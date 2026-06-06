package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.auth.AuthRepository
import com.example.data.model.InventoryEntity
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.SyncState
import com.example.data.supabase.SupabaseManager
import com.example.data.api.GeminiClient
import com.example.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OcrScannerRepository"

/**
 * Custom Exception thrown when an invoice scan is queued for offline synchronization.
 */
class OfflineQueueException(val filePath: String, message: String) : Exception(message)

@Singleton
class OcrScannerRepositoryImpl @Inject constructor(
    private val supabaseManager: SupabaseManager,
    private val offlineQueueRepo: OfflineQueueRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val inventoryRepository: InventoryRepository
) : OcrScannerRepository {

    private suspend fun checkImageHasText(context: Context, imageUri: Uri): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val hasText = visionText.text.isNotBlank()
                    Log.d(TAG, "ML Kit Text detection finished. Found text: $hasText")
                    continuation.resume(hasText)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit Text detection failed", e)
                    continuation.resume(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ML Kit text recognition for image", e)
            continuation.resume(false)
        }
    }

    override suspend fun processInvoiceScan(
        context: Context,
        imageUri: Uri,
        isHandwritten: Boolean
    ): Result<List<InventoryEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting processInvoiceScan execution for Uri: $imageUri")

            // 1. Fetch current Store ID context
            val currentUser = authRepository.getCurrentUser()
            val storeId = if (currentUser != null) {
                profileRepository.getProfile(currentUser.id).getOrNull()?.storeId ?: "00000000-0000-0000-0000-000000000000"
            } else {
                "00000000-0000-0000-0000-000000000000"
            }

            // 2. Perform progressive down-scaling and resizing compression
            Log.d(TAG, "Resizing and compressing invoice scan...")
            val compressedBytes = ImageCompressor.compressUriToBytes(context, imageUri)
            Log.d(TAG, "Compression finished. Size: ${compressedBytes.size / 1024} KB")

            // 3. Inspect active internet connectivity
            val online = isDeviceOnline(context)
            Log.d(TAG, "Device internet status check: Online=$online")

            if (!online) {
                // Device is offline: Cache file on persistent local disk and queue action
                Log.w(TAG, "Device is OFFLINE. Initializing offline invoice caching and queueing.")
                val localInvoiceDir = File(context.filesDir, "pending_invoices").apply {
                    if (!exists()) mkdirs()
                }
                val localFile = File(localInvoiceDir, "scan_${System.currentTimeMillis()}.jpg")
                FileOutputStream(localFile).use { fos ->
                    fos.write(compressedBytes)
                }
                
                val queuePayload = JSONObject().apply {
                    put("localFilePath", localFile.absolutePath)
                    put("isHandwritten", isHandwritten)
                }.toString()

                val queueEntity = OfflineQueueEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = storeId,
                    actionType = "invoice_ocr_sync",
                    idempotencyKey = UUID.randomUUID().toString(),
                    payload = queuePayload,
                    status = SyncState.PENDING
                )

                offlineQueueRepo.enqueue(queueEntity)
                Log.i(TAG, "Offline sync job registered successfully: ${queueEntity.id}")
                
                throw OfflineQueueException(
                    filePath = localFile.absolutePath,
                    message = "Scanned bill stored offline! We will upload and scan it automatically once your network restores."
                )
            }

            // Device is online: Execute live Supabase storage upload and call OCR API
            Log.i(TAG, "Device is ONLINE. Proceeding with immediate cloud upload & parsing.")
            
            // Perform upload with 3 standard network retries
            val bucketName = if (isHandwritten) "retaildost-diary-images" else "retaildost-invoice-images"
            val bucketFileName = "$storeId/${UUID.randomUUID()}.jpg"
            var uploadResult: Result<String>? = null
            var retryCount = 0
            val maxRetries = 3

            while (retryCount < maxRetries) {
                Log.d(TAG, "Uploading invoice scan to Supabase bucket $bucketName (Attempt ${retryCount + 1})...")
                uploadResult = supabaseManager.uploadFile(
                    bucketId = bucketName,
                    path = bucketFileName,
                    bytes = compressedBytes,
                    contentType = "image/jpeg"
                )
                if (uploadResult.isSuccess) {
                    break
                }
                retryCount++
                if (retryCount < maxRetries) {
                    // Small exponential delay back-off
                    kotlinx.coroutines.delay(1000L * retryCount)
                }
            }

            val publicUrl = uploadResult?.getOrNull()
                ?: throw uploadResult?.exceptionOrNull() ?: IOException("Failed to upload compressed invoice scan to Supabase Storage after $maxRetries attempts")

            Log.i(TAG, "Invoice uploaded successfully. Storage Path: $publicUrl")

            // 4. Trigger Supabase Edge Function (ocr-invoice or ocr-diary)
            val functionName = if (isHandwritten) "ocr-diary" else "ocr-invoice"
            Log.d(TAG, "Calling Supabase Edge Function $functionName...")
            
            val payload = JSONObject().apply {
                put("image_path", bucketFileName)
                put("bucket_name", bucketName)
                put("idempotency_key", UUID.randomUUID().toString())
            }.toString()

            val functionResult = supabaseManager.invokeFunction(functionName, payload)
            val jsonResult = functionResult.getOrThrow()

            val cleanJson = jsonResult.replace("```json", "").replace("```", "").trim()
            Log.d(TAG, "OCR parsed clean JSON from Edge Function $functionName: $cleanJson")

            // 5. De-serialize and map results to domain entities matching validators.ts
            val rootObj = JSONObject(cleanJson)
            val itemsArray = rootObj.optJSONArray("items") ?: JSONArray()
            val parsedList = mutableListOf<InventoryEntity>()
            
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                val name = if (isHandwritten) {
                    val canonicalName = obj.optString("canonical_name", "")
                    if (canonicalName.isNotBlank() && canonicalName != "null") {
                        canonicalName
                    } else {
                        obj.optString("product_name", "Unknown Item")
                    }
                } else {
                    obj.optString("item_name", "Unknown Item")
                }
                val category = "Staples"
                val quantity = if (isHandwritten) {
                    obj.optDouble("quantity_sold", 1.0)
                } else {
                    obj.optDouble("quantity", 10.0)
                }
                val unitLabel = if (isHandwritten) "pcs" else obj.optString("unit_label", "pcs")
                val price = if (isHandwritten) obj.optDouble("price", 0.0) else 0.0
                val costPrice = if (isHandwritten) price else obj.optDouble("cost_price", 10.0)
                val mrp = if (isHandwritten) price else obj.optDouble("mrp", 15.0)
                val expiryDate = if (isHandwritten) "" else obj.optString("expiry_date", "")
                val confidence = obj.optDouble("confidence", 1.0)
                val batchNo = if (isHandwritten) "" else obj.optString("batch_no", "")
                val source = if (isHandwritten) "ocr_diary" else "ocr"

                parsedList.add(
                    InventoryEntity(
                        id = UUID.randomUUID().toString(),
                        storeId = storeId,
                        itemName = name,
                        category = category,
                        unitLabel = unitLabel,
                        quantity = quantity,
                        minThreshold = 5.0,
                        costPrice = costPrice,
                        mrp = mrp,
                        batchNo = batchNo.ifBlank { null },
                        expiryDate = expiryDate.ifBlank { null },
                        ocrConfidence = confidence,
                        source = source
                    )
                )
            }

            Log.i(TAG, "Successfully parsed ${parsedList.size} invoice items.")
            parsedList
        }
    }

    override suspend fun syncPendingOcrScans(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val pending = offlineQueueRepo.getPendingActions()
            val ocrActions = pending.filter { it.actionType == "invoice_ocr_sync" }
            Log.i(TAG, "Found ${ocrActions.size} pending offline invoice OCR sync jobs.")

            for (action in ocrActions) {
                try {
                    val payload = JSONObject(action.payload)
                    val filePath = payload.getString("localFilePath")
                    val isHandwritten = payload.optBoolean("isHandwritten", false)
                    val file = File(filePath)

                    if (!file.exists()) {
                        Log.e(TAG, "Offline scan file not found: $filePath. Deleting broken queue job.")
                        offlineQueueRepo.markFailed(action.id, "Image file not found on device.")
                        continue
                    }

                    // Process invoice scan using standard online upload + OCR call!
                    Log.d(TAG, "Processing queued offline scan file: $filePath")
                    val items = processInvoiceScan(context, Uri.fromFile(file), isHandwritten).getOrThrow()

                    // Insert parsed items locally (which also triggers their upsert to Supabase)
                    for (item in items) {
                        inventoryRepository.insertItem(item)
                    }

                    // Mark sync task as successfully synchronized and clean up cached disk image
                    offlineQueueRepo.markCompleted(action.id)
                    if (file.exists()) {
                        file.delete()
                        Log.d(TAG, "Cleaned up local cached image: $filePath")
                    }
                    Log.i(TAG, "Successfully synced queued invoice scan action: ${action.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process queued offline scan ${action.id}: ${e.message}", e)
                    offlineQueueRepo.markFailed(action.id, e.message ?: "Unknown offline sync error")
                    if (isNetworkError(e)) {
                        throw e // Propagate to trigger exponential WorkManager retry
                    }
                }
            }
        }
    }

    private fun isNetworkError(t: Throwable): Boolean {
        val name = t.javaClass.name.lowercase()
        val msg = t.message?.lowercase() ?: ""
        return t is IOException || 
               name.contains("ktor") || 
               name.contains("socket") || 
               name.contains("httprequest") || 
               msg.contains("timeout") || 
               msg.contains("connection") || 
               msg.contains("host") || 
               msg.contains("address")
    }

    /**
     * Inspects active network capabilities to determine offline status.
     */
    private fun isDeviceOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
