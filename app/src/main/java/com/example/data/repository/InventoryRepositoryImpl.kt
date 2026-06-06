package com.example.data.repository

import android.util.Log
import com.example.data.dao.InventoryDao
import com.example.data.model.InventoryDto
import com.example.data.model.InventoryEntity
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.toInventoryDto
import com.example.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "InventoryRepositoryImpl"

/**
 * InventoryRepositoryImpl
 * ───────────────────────
 * Offline-first repository executing inventory changes on Room and syncing with Supabase.
 * Uses OfflineQueueRepository to queue failed updates and triggers SyncScheduler to process them.
 *
 * All Supabase operations use InventoryDto (with @SerialName snake_case mappings)
 * to ensure proper column name mapping between Kotlin camelCase and PostgreSQL snake_case.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val supabaseClient: SupabaseClient,
    private val offlineQueueRepository: OfflineQueueRepository,
    private val syncScheduler: SyncScheduler
) : InventoryRepository {

    override val allItems: Flow<List<InventoryEntity>> = inventoryDao.getAllInventoryFlow()

    override fun searchItems(query: String): Flow<List<InventoryEntity>> =
        inventoryDao.searchInventoryFlow("%$query%")

    override suspend fun getItemsPaged(limit: Int, offset: Int): List<InventoryEntity> =
        withContext(Dispatchers.IO) {
            inventoryDao.getInventoryPaged(limit, offset)
        }

    override suspend fun getItemById(id: String): InventoryEntity? =
        withContext(Dispatchers.IO) {
            inventoryDao.getItemById(id)
        }

    override suspend fun insertItem(item: InventoryEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Insert to Room DB cache immediately (Optimistic UI update)
                inventoryDao.insertItem(item)

                // 2. Synchronize to Supabase if network is present
                try {
                    val dto = item.toInventoryDto()
                    val postgrest = supabaseClient.postgrest
                    postgrest["inventory"].upsert(dto)
                    Log.i(TAG, "Item synced to Supabase: ${item.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase upsert failed, queueing offline task: ${e.message}")
                    // 3. Queue the task offline
                    val payload = JSONObject().apply {
                        put("id", item.id)
                        put("storeId", item.storeId)
                        put("itemName", item.itemName)
                        put("category", if (item.category == null) JSONObject.NULL else item.category)
                        put("unitLabel", if (item.unitLabel == null) JSONObject.NULL else item.unitLabel)
                        put("quantity", item.quantity)
                        put("minThreshold", item.minThreshold)
                        put("costPrice", if (item.costPrice == null) JSONObject.NULL else item.costPrice)
                        put("mrp", if (item.mrp == null) JSONObject.NULL else item.mrp)
                        put("batchNo", if (item.batchNo == null) JSONObject.NULL else item.batchNo)
                        put("expiryDate", if (item.expiryDate == null) JSONObject.NULL else item.expiryDate)
                        put("ocrConfidence", if (item.ocrConfidence == null) JSONObject.NULL else item.ocrConfidence)
                        put("source", item.source)
                        put("createdAt", item.createdAt)
                        put("updatedAt", item.updatedAt)
                        put("deletedAt", if (item.deletedAt == null) JSONObject.NULL else item.deletedAt)
                    }.toString()

                    offlineQueueRepository.enqueue(
                        OfflineQueueEntity(
                            storeId = item.storeId,
                            actionType = "inventory_add",
                            idempotencyKey = item.id,
                            payload = payload
                        )
                    )
                    // Trigger dynamic sync task
                    syncScheduler.triggerImmediateSync()
                }
                Unit
            }
        }

    override suspend fun deleteItem(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Fetch the item first to resolve its store ID
                val item = inventoryDao.getItemById(id)
                val storeId = item?.storeId ?: "00000000-0000-0000-0000-000000000000"

                // 1. Delete from Room DB cache
                inventoryDao.deleteItemById(id)

                // 2. Delete from Supabase
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["inventory"].delete {
                        filter {
                            eq("id", id)
                        }
                    }
                    Log.i(TAG, "Item deleted from Supabase: $id")
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase delete failed, queueing offline task: ${e.message}")
                    // 3. Queue the task offline
                    offlineQueueRepository.enqueue(
                        OfflineQueueEntity(
                            storeId = storeId,
                            actionType = "inventory_delete",
                            idempotencyKey = id,
                            payload = JSONObject().apply { put("id", id) }.toString()
                        )
                    )
                    // Trigger dynamic sync task
                    syncScheduler.triggerImmediateSync()
                }
                Unit
            }
        }

    override suspend fun syncPendingItems(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pending = offlineQueueRepository.getPendingActions()
                val inventoryTasks = pending.filter {
                    it.actionType == "inventory_add" || it.actionType == "inventory_edit" || it.actionType == "inventory_delete"
                }
                Log.i(TAG, "Found ${inventoryTasks.size} pending offline inventory sync actions.")
                for (action in inventoryTasks) {
                    try {
                        when (action.actionType) {
                            "inventory_add", "inventory_edit" -> {
                                val json = JSONObject(action.payload)
                                val item = InventoryEntity(
                                    id = json.getString("id"),
                                    storeId = json.getString("storeId"),
                                    itemName = json.getString("itemName"),
                                    category = if (json.isNull("category")) null else json.getString("category"),
                                    unitLabel = if (json.isNull("unitLabel")) null else json.getString("unitLabel"),
                                    quantity = json.getDouble("quantity"),
                                    minThreshold = json.getDouble("minThreshold"),
                                    costPrice = if (json.isNull("costPrice")) null else json.getDouble("costPrice"),
                                    mrp = if (json.isNull("mrp")) null else json.getDouble("mrp"),
                                    batchNo = if (json.isNull("batchNo")) null else json.getString("batchNo"),
                                    expiryDate = if (json.isNull("expiryDate")) null else json.getString("expiryDate"),
                                    ocrConfidence = if (json.isNull("ocrConfidence")) null else json.optDouble("ocrConfidence", 0.0),
                                    source = json.optString("source", "manual"),
                                    createdAt = json.getLong("createdAt"),
                                    updatedAt = json.getLong("updatedAt"),
                                    deletedAt = if (json.isNull("deletedAt")) null else json.getLong("deletedAt")
                                )
                                // Convert to DTO with proper snake_case @SerialName for Supabase
                                val dto = item.toInventoryDto()
                                supabaseClient.postgrest["inventory"].upsert(dto)
                            }
                            "inventory_delete" -> {
                                val json = JSONObject(action.payload)
                                val id = json.getString("id")
                                supabaseClient.postgrest["inventory"].delete {
                                    filter {
                                        eq("id", id)
                                    }
                                }
                            }
                        }
                        offlineQueueRepository.markCompleted(action.id)
                        Log.i(TAG, "Successfully synced queued action: ${action.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync queued action ${action.id}: ${e.message}")
                        offlineQueueRepository.markFailed(action.id, e.message ?: "Unknown sync error")
                        // Propagate network exception to retry
                        if (isNetworkError(e)) {
                            throw e
                        }
                    }
                }
            }
        }

    private fun isNetworkError(e: Exception): Boolean {
        val name = e.javaClass.name.lowercase()
        val msg = e.message?.lowercase() ?: ""
        return e is java.io.IOException || 
               name.contains("ktor") || 
               name.contains("socket") || 
               name.contains("httprequest") || 
               msg.contains("timeout") || 
               msg.contains("connection") || 
               msg.contains("host") || 
               msg.contains("address")
    }
}
