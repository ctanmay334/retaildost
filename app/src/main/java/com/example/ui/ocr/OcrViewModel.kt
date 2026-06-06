package com.example.ui.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.InventoryEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OcrUiState {
    object Idle : OcrUiState()
    object ShowScanner : OcrUiState()
    object ProcessingImage : OcrUiState()
    data class ReviewItems(
        val invoice: ScannedInvoice,
        val reviewableItems: List<ReviewableItem>
    ) : OcrUiState()
    object SavingToStock : OcrUiState()
    data class Success(val createdCount: Int, val updatedCount: Int) : OcrUiState()
    data class Error(val message: String, val isNotInvoice: Boolean = false) : OcrUiState()
}

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val geminiService = GeminiVisionService(com.example.BuildConfig.GEMINI_API_KEY)

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private val _isStockOutMode = MutableStateFlow(false)
    val isStockOutMode: StateFlow<Boolean> = _isStockOutMode.asStateFlow()

    fun setStockOutMode(isStockOut: Boolean) {
        _isStockOutMode.value = isStockOut
    }

    fun onScannerButtonClicked() { _uiState.value = OcrUiState.ShowScanner }
    fun onScannerDismissed() { _uiState.value = OcrUiState.Idle }
    fun retryFromScanner() { _uiState.value = OcrUiState.ShowScanner }
    fun resetToIdle() { _uiState.value = OcrUiState.Idle }

    fun processImage(context: Context, imageUri: Uri) {
        _uiState.value = OcrUiState.ProcessingImage

        viewModelScope.launch {
            try {
                // Step 1: Compress + base64 encode
                val base64 = OcrImageUtils.uriToBase64(context, imageUri)

                // Step 2: Call Gemini 3.1 Pro Preview
                val result = geminiService.extractInvoiceData(base64)

                result.fold(
                    onSuccess = { invoice ->
                        if (invoice.items.isEmpty()) {
                            _uiState.value = OcrUiState.Error(
                                "No products found in this invoice. Please try a clearer image."
                            )
                            return@launch
                        }
                        // Step 3: Match against existing stock
                        val reviewableItems = matchItemsWithStock(invoice.items)
                        _uiState.value = OcrUiState.ReviewItems(invoice, reviewableItems)
                    },
                    onFailure = { error ->
                        _uiState.value = OcrUiState.Error(
                            message = error.message ?: "Scan failed. Please try again.",
                            isNotInvoice = error is NotAnInvoiceException
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = OcrUiState.Error("Image processing failed: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun matchItemsWithStock(items: List<ScannedItem>): List<ReviewableItem> {
        val allProducts = try {
            inventoryRepository.allItems.first()
        } catch (e: Exception) {
            emptyList()
        }

        return items.map { scannedItem ->
            val match = allProducts.firstOrNull { product ->
                val productNorm = product.itemName.trim().lowercase()
                val scannedNorm = scannedItem.name.trim().lowercase()

                // Priority 1: Exact name match (case-insensitive)
                productNorm == scannedNorm ||
                // Priority 2: Contains match (one-way)
                productNorm.contains(scannedNorm) || scannedNorm.contains(productNorm)
            }

            ReviewableItem(
                scannedItem = scannedItem,
                matchedProductId = match?.id,
                matchedProductName = match?.itemName
            )
        }
    }

    fun updateReviewItem(itemId: String, updatedItem: ScannedItem) {
        val state = _uiState.value as? OcrUiState.ReviewItems ?: return
        _uiState.update {
            OcrUiState.ReviewItems(
                invoice = state.invoice,
                reviewableItems = state.reviewableItems.map { r ->
                    if (r.scannedItem.id == itemId) r.copy(scannedItem = updatedItem, isEdited = true) else r
                }
            )
        }
    }

    fun removeReviewItem(itemId: String) {
        val state = _uiState.value as? OcrUiState.ReviewItems ?: return
        val updated = state.reviewableItems.filter { it.scannedItem.id != itemId }
        if (updated.isEmpty()) {
            _uiState.value = OcrUiState.Error("All items removed. Nothing to save.")
            return
        }
        _uiState.update { OcrUiState.ReviewItems(state.invoice, updated) }
    }

    fun confirmAndSaveToStock() {
        val state = _uiState.value as? OcrUiState.ReviewItems ?: return
        _uiState.value = OcrUiState.SavingToStock

        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                val storeId = if (currentUser != null) {
                    profileRepository.getProfile(currentUser.id).getOrNull()?.storeId ?: "00000000-0000-0000-0000-000000000000"
                } else {
                    "00000000-0000-0000-0000-000000000000"
                }

                var createdCount = 0
                var updatedCount = 0
                val isStockOut = _isStockOutMode.value

                state.reviewableItems.forEach { reviewable ->
                    val item = reviewable.scannedItem
                    if (reviewable.matchedProductId != null) {
                        // Product EXISTS — update stock level
                        val existingItem = inventoryRepository.getItemById(reviewable.matchedProductId)
                        if (existingItem != null) {
                            val newQuantity = if (isStockOut) {
                                (existingItem.quantity - item.quantity).coerceAtLeast(0.0)
                            } else {
                                existingItem.quantity + item.quantity
                            }
                            val mergedItem = existingItem.copy(
                                quantity = newQuantity,
                                costPrice = item.unitPrice ?: existingItem.costPrice,
                                mrp = item.unitPrice ?: existingItem.mrp ?: 0.0,
                                source = "ocr",
                                updatedAt = System.currentTimeMillis()
                            )
                            inventoryRepository.insertItem(mergedItem)
                            updatedCount++
                        }
                    } else {
                        // Product DOES NOT EXIST — create it
                        val initialQuantity = if (isStockOut) 0.0 else item.quantity
                        val newEntity = InventoryEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            storeId = storeId,
                            itemName = item.name,
                            category = "Staples", // Default category
                            unitLabel = item.unit ?: "pcs",
                            quantity = initialQuantity,
                            minThreshold = 5.0,
                            costPrice = item.unitPrice,
                            mrp = item.unitPrice ?: 0.0,
                            batchNo = item.hsnCode,
                            ocrConfidence = 1.0,
                            source = "ocr"
                        )
                        inventoryRepository.insertItem(newEntity)
                        createdCount++
                    }
                }

                _uiState.value = OcrUiState.Success(createdCount, updatedCount)
            } catch (e: Exception) {
                Log.e("OcrViewModel", "Failed to save products to stock", e)
                _uiState.value = OcrUiState.Error("Failed to save products: ${e.localizedMessage}")
            }
        }
    }
}
