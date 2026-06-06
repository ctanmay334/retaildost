package com.example.data.repository

import android.util.Log
import com.example.data.dao.AlertDao
import com.example.data.model.AlertDto
import com.example.data.model.AlertEntity
import com.example.data.model.AlertType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

private const val TAG = "AlertRepositoryImpl"

/**
 * AlertRepositoryImpl
 * ───────────────────
 * Production-ready implementation of [AlertRepository] that enforces an offline-first strategy.
 * Instantly updates Room database cache to ensure reliable offline operations, and synchronizes
 * read status updates and pull requests with Supabase in a non-blocking manner.
 */
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val supabaseClient: SupabaseClient
) : AlertRepository {

    override val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()

    override val unreadAlerts: Flow<List<AlertEntity>> = alertDao.getUnreadAlertsFlow()

    override val unreadCount: Flow<Int> = alertDao.getUnreadAlertsCountFlow()

    override suspend fun insertAlert(alert: AlertEntity): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                alertDao.insertAlert(alert)
            }
        }

    override suspend fun insertAlerts(alerts: List<AlertEntity>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                alertDao.insertAlerts(alerts)
            }
        }

    override suspend fun getAlertsPaged(limit: Int, offset: Int): List<AlertEntity> =
        withContext(Dispatchers.IO) {
            alertDao.getAlertsPaged(limit, offset)
        }

    override fun getAlertsByTypeFlow(type: AlertType): Flow<List<AlertEntity>> =
        alertDao.getAlertsByTypeFlow(type)

    override suspend fun markAsRead(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Immediately update Room DB cache (offline-first validation)
                alertDao.markAsRead(id)

                // 2. Optimistically push changes to remote Supabase DB
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["alerts"].update(
                        mapOf("is_read" to true)
                    ) {
                        filter {
                            eq("id", id)
                        }
                    }
                    Log.i(TAG, "Alert marked read on Supabase: $id")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update alert status remote on Supabase (cached locally): ${e.message}")
                }
                Unit
            }
        }

    override suspend fun fetchRemoteAlerts(storeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val postgrest = supabaseClient.postgrest
                try {
                    val response = postgrest["alerts"].select {
                        filter {
                            eq("store_id", storeId)
                        }
                    }
                    if (response.data != "[]" && response.data.isNotBlank()) {
                        val remoteList = response.decodeList<AlertDto>()
                        val entities = remoteList.map { dto ->
                            val localType = when (dto.alertType) {
                                "low_stock" -> AlertType.LOW_STOCK
                                "expiry_warning", "expiry_critical" -> AlertType.EXPIRY_WARNING
                                "khata_reminder" -> AlertType.KHATA_REMINDER
                                "ocr_retry" -> AlertType.OCR_RETRY
                                "sync_failure" -> AlertType.SYNC_FAILURE
                                else -> AlertType.LOW_STOCK
                            }

                            val derivedTitle = when (localType) {
                                AlertType.LOW_STOCK -> "⚠️ Low Stock Alarm"
                                AlertType.EXPIRY_WARNING -> "⏰ Expiry Warning"
                                AlertType.KHATA_REMINDER -> "💰 Khata Ledger Reminder"
                                AlertType.OCR_RETRY -> "📸 Invoice OCR Failed"
                                AlertType.SYNC_FAILURE -> "🔄 Cloud Sync Error"
                            }

                            val metadata = JSONObject().apply {
                                put("store_id", dto.storeId)
                                dto.inventoryId?.let { put("inventory_id", it) }
                                put("item_name", dto.itemName)
                                dto.daysToExpiry?.let { put("days_to_expiry", it) }
                                dto.currentQty?.let { put("current_qty", it) }
                            }.toString()

                            val deepLink = if (dto.inventoryId != null) {
                                "retaildost://inventory_detail?inventory_id=${dto.inventoryId}"
                            } else null

                            AlertEntity(
                                id = dto.id ?: UUID.randomUUID().toString(),
                                title = derivedTitle,
                                message = dto.message,
                                alertType = localType,
                                createdAt = parseCreatedAt(dto.createdAt),
                                isRead = dto.isRead,
                                deepLink = deepLink,
                                metadataJson = metadata
                            )
                        }

                        if (entities.isNotEmpty()) {
                            alertDao.insertAlerts(entities)
                            Log.i(TAG, "Pulled and cached ${entities.size} alerts from Supabase")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pull alerts from Supabase: ${e.message}", e)
                    throw e
                }
                Unit
            }
        }

    override suspend fun clearAllAlerts(storeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Clear local Room DB cache
                alertDao.clearAllAlerts()

                // 2. Clear remote Supabase DB
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["alerts"].delete {
                        filter {
                            eq("store_id", storeId)
                        }
                    }
                    Log.i(TAG, "Cleared all alerts on Supabase for store: $storeId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear alerts remote on Supabase: ${e.message}")
                }
                Unit
            }
        }

    override suspend fun markAllAsRead(storeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Mark all unread alerts as read in local Room cache
                alertDao.markAllAsRead()

                // 2. Batch update on Supabase
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["alerts"].update(
                        mapOf("is_read" to true)
                    ) {
                        filter {
                            eq("store_id", storeId)
                            eq("is_read", false)
                        }
                    }
                    Log.i(TAG, "All alerts marked read on Supabase for store: $storeId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to batch-update alert status on Supabase: ${e.message}")
                }
                Unit
            }
        }

    private fun parseCreatedAt(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
            } else {
                java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateStr) { temp ->
                    java.time.Instant.from(temp).toEpochMilli()
                }
            }
        } catch (e: Exception) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
                sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
