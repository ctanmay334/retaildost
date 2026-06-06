package com.example.data.repository

import android.util.Log
import com.example.data.dao.SaleDao
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.SaleRecordEntity
import com.example.data.model.SaleRecordItemEntity
import com.example.data.model.toSaleRecordDto
import com.example.data.model.toSaleRecordItemDto
import com.example.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "SaleRepositoryImpl"

/**
 * SaleRepositoryImpl
 * ──────────────────
 * Implements SaleRepository coordinating Room DB caching and Supabase remote syncing.
 * Integrates OfflineQueueRepository to cache offline sales and triggers SyncScheduler to process them.
 *
 * All Supabase operations use SaleRecordDto / SaleRecordItemDto (with @SerialName snake_case mappings)
 * to ensure proper column name mapping between Kotlin camelCase and PostgreSQL snake_case.
 */
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val supabaseClient: SupabaseClient,
    private val offlineQueueRepository: OfflineQueueRepository,
    private val syncScheduler: SyncScheduler
) : SaleRepository {

    override val allSales: Flow<List<SaleRecordEntity>> = saleDao.getAllSalesFlow()

    override suspend fun getSalesPaged(limit: Int, offset: Int): List<SaleRecordEntity> =
        withContext(Dispatchers.IO) {
            saleDao.getSalesPaged(limit, offset)
        }

    override suspend fun getSaleWithItems(saleId: String): Pair<SaleRecordEntity?, List<SaleRecordItemEntity>> =
        withContext(Dispatchers.IO) {
            val record = saleDao.getSaleRecordById(saleId)
            val items = saleDao.getItemsForSale(saleId)
            record to items
        }

    override suspend fun createSale(
        sale: SaleRecordEntity,
        items: List<SaleRecordItemEntity>
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Insert header & lines to Room database (offline-first execution)
                saleDao.insertSaleRecord(sale)
                saleDao.insertSaleItems(items)

                // 2. Synchronize to Supabase remote server using DTOs
                try {
                    val postgrest = supabaseClient.postgrest
                    
                    // Convert to DTOs with proper snake_case @SerialName for Supabase
                    val saleDto = sale.toSaleRecordDto()
                    val itemDtos = items.map { it.toSaleRecordItemDto() }
                    
                    // Upsert header
                    postgrest["sale_records"].upsert(saleDto)
                    
                    // Upsert line items
                    postgrest["sale_record_items"].upsert(itemDtos)
                    Log.i(TAG, "SaleRecord and line items synced to Supabase: ${sale.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync sales transaction, queueing offline task: ${e.message}")
                    // 3. Serialize and enqueue offline task
                    val payload = JSONObject().apply {
                        put("sale", JSONObject().apply {
                            put("id", sale.id)
                            put("storeId", sale.storeId)
                            put("customerName", if (sale.customerName == null) JSONObject.NULL else sale.customerName)
                            put("source", sale.source)
                            put("notes", if (sale.notes == null) JSONObject.NULL else sale.notes)
                            put("totalAmount", sale.totalAmount)
                            put("itemsCount", sale.itemsCount)
                            put("saleDate", sale.saleDate)
                            put("createdAt", sale.createdAt)
                            put("updatedAt", sale.updatedAt)
                            put("deletedAt", if (sale.deletedAt == null) JSONObject.NULL else sale.deletedAt)
                        })
                        put("items", JSONArray().apply {
                            items.forEach { item ->
                                put(JSONObject().apply {
                                    put("id", item.id)
                                    put("storeId", item.storeId)
                                    put("saleRecordId", item.saleRecordId)
                                    put("inventoryId", if (item.inventoryId == null) JSONObject.NULL else item.inventoryId)
                                    put("itemName", item.itemName)
                                    put("unitLabel", if (item.unitLabel == null) JSONObject.NULL else item.unitLabel)
                                    put("quantitySold", item.quantitySold)
                                    put("salePrice", if (item.salePrice == null) JSONObject.NULL else item.salePrice)
                                    put("costPrice", if (item.costPrice == null) JSONObject.NULL else item.costPrice)
                                    put("createdAt", item.createdAt)
                                })
                            }
                        })
                    }.toString()

                    offlineQueueRepository.enqueue(
                        OfflineQueueEntity(
                            storeId = sale.storeId,
                            actionType = "sale_record",
                            idempotencyKey = sale.id,
                            payload = payload
                        )
                    )
                    // Trigger dynamic sync task
                    syncScheduler.triggerImmediateSync()
                }
                Unit
            }
        }

    override suspend fun syncPendingSales(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pending = offlineQueueRepository.getPendingActions()
                val salesTasks = pending.filter { it.actionType == "sale_record" }
                Log.i(TAG, "Found ${salesTasks.size} pending offline sale actions.")
                
                for (action in salesTasks) {
                    try {
                        val json = JSONObject(action.payload)
                        val saleJson = json.getJSONObject("sale")
                        val sale = SaleRecordEntity(
                            id = saleJson.getString("id"),
                            storeId = saleJson.getString("storeId"),
                            customerName = if (saleJson.isNull("customerName")) null else saleJson.getString("customerName"),
                            source = saleJson.getString("source"),
                            notes = if (saleJson.isNull("notes")) null else saleJson.getString("notes"),
                            totalAmount = saleJson.getDouble("totalAmount"),
                            itemsCount = saleJson.getInt("itemsCount"),
                            saleDate = saleJson.getString("saleDate"),
                            createdAt = saleJson.getLong("createdAt"),
                            updatedAt = saleJson.getLong("updatedAt"),
                            deletedAt = if (saleJson.isNull("deletedAt")) null else saleJson.getLong("deletedAt")
                        )

                        val itemsArray = json.getJSONArray("items")
                        val items = (0 until itemsArray.length()).map { i ->
                            val itemJson = itemsArray.getJSONObject(i)
                            SaleRecordItemEntity(
                                id = itemJson.getString("id"),
                                storeId = itemJson.getString("storeId"),
                                saleRecordId = itemJson.getString("saleRecordId"),
                                inventoryId = if (itemJson.isNull("inventoryId")) null else itemJson.getString("inventoryId"),
                                itemName = itemJson.getString("itemName"),
                                unitLabel = if (itemJson.isNull("unitLabel")) null else itemJson.getString("unitLabel"),
                                quantitySold = itemJson.getDouble("quantitySold"),
                                salePrice = if (itemJson.isNull("salePrice")) null else itemJson.getDouble("salePrice"),
                                costPrice = if (itemJson.isNull("costPrice")) null else itemJson.getDouble("costPrice"),
                                createdAt = itemJson.getLong("createdAt")
                            )
                        }

                        // Convert to DTOs with proper snake_case @SerialName for Supabase
                        val saleDto = sale.toSaleRecordDto()
                        val itemDtos = items.map { it.toSaleRecordItemDto() }

                        val postgrest = supabaseClient.postgrest
                        postgrest["sale_records"].upsert(saleDto)
                        postgrest["sale_record_items"].upsert(itemDtos)
                        
                        offlineQueueRepository.markCompleted(action.id)
                        Log.i(TAG, "Successfully synced queued sales: ${action.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync queued sales ${action.id}: ${e.message}")
                        offlineQueueRepository.markFailed(action.id, e.message ?: "Unknown sync error")
                        // Propagate network exception to retry
                        if (isNetworkError(e)) {
                            throw e
                        }
                    }
                }
            }
        }

    override fun getSaleItemsForProductFlow(inventoryId: String): Flow<List<SaleRecordItemEntity>> {
        return saleDao.getSaleItemsForProductFlow(inventoryId)
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
