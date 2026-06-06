package com.example.data.repository

import com.example.data.dao.OfflineQueueDao
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * OfflineQueueRepositoryImpl
 * ──────────────────────────
 * Concrete repository managing local offline queue operations.
 * Upgraded with support for default metrics parameters and backward compatibility delegates.
 */
class OfflineQueueRepositoryImpl @Inject constructor(
    private val offlineQueueDao: OfflineQueueDao
) : OfflineQueueRepository {

    override val pendingCount: Flow<Int> = offlineQueueDao.getPendingCountFlow()

    override suspend fun enqueue(action: OfflineQueueEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pendingAction = action.copy(status = SyncState.PENDING)
                offlineQueueDao.enqueueAction(pendingAction)
                Unit
            }
        }

    override suspend fun getPendingActions(): List<OfflineQueueEntity> =
        withContext(Dispatchers.IO) {
            offlineQueueDao.getPendingActions()
        }

    override suspend fun markProcessing(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = offlineQueueDao.getActionById(id)
                    ?: error("Action with ID $id not found in queue.")

                offlineQueueDao.updateActionStatus(
                    id = id,
                    status = SyncState.PROCESSING,
                    attemptCount = action.attemptCount,
                    lastAttemptedAt = System.currentTimeMillis(),
                    processedAt = null,
                    errorMessage = null,
                    processingTimestamp = System.currentTimeMillis(),
                    retryTimestamp = null,
                    syncAnalytics = action.syncAnalytics
                )
                Unit
            }
        }

    override suspend fun markRetrying(
        id: String,
        error: String,
        nextAttemptDelayMs: Long
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = offlineQueueDao.getActionById(id)
                    ?: error("Action with ID $id not found in queue.")

                val newAttemptCount = action.attemptCount + 1
                val now = System.currentTimeMillis()

                val analyticsObj = try {
                    if (action.syncAnalytics != null) JSONObject(action.syncAnalytics) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }
                analyticsObj.put("last_error", error)
                analyticsObj.put("last_attempt_ts", now)
                analyticsObj.put("attempts_so_far", newAttemptCount)

                offlineQueueDao.updateActionStatus(
                    id = id,
                    status = SyncState.RETRYING,
                    attemptCount = newAttemptCount,
                    lastAttemptedAt = now,
                    processedAt = null,
                    errorMessage = error,
                    processingTimestamp = action.processingTimestamp,
                    retryTimestamp = now + nextAttemptDelayMs,
                    syncAnalytics = analyticsObj.toString()
                )
                Unit
            }
        }

    override suspend fun markFailedPermanently(id: String, error: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = offlineQueueDao.getActionById(id)
                    ?: error("Action with ID $id not found in queue.")

                val newAttemptCount = action.attemptCount + 1
                val now = System.currentTimeMillis()

                val analyticsObj = try {
                    if (action.syncAnalytics != null) JSONObject(action.syncAnalytics) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }
                analyticsObj.put("terminal_error", error)
                analyticsObj.put("failed_at_ts", now)
                analyticsObj.put("total_attempts", newAttemptCount)
                analyticsObj.put("sync_outcome", "PERMANENT_FAILURE")

                offlineQueueDao.updateActionStatus(
                    id = id,
                    status = SyncState.FAILED,
                    attemptCount = newAttemptCount,
                    lastAttemptedAt = now,
                    processedAt = null,
                    errorMessage = error,
                    processingTimestamp = action.processingTimestamp,
                    retryTimestamp = null,
                    syncAnalytics = analyticsObj.toString()
                )
                Unit
            }
        }

    override suspend fun markCompleted(
        id: String,
        durationMs: Long,
        networkType: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = offlineQueueDao.getActionById(id)
                    ?: error("Action with ID $id not found in queue.")

                val newAttemptCount = action.attemptCount + 1
                val now = System.currentTimeMillis()

                val analyticsObj = try {
                    if (action.syncAnalytics != null) JSONObject(action.syncAnalytics) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }
                analyticsObj.put("sync_duration_ms", durationMs)
                analyticsObj.put("network_carrier", networkType)
                analyticsObj.put("synced_successfully_at", now)
                analyticsObj.put("total_attempts_taken", newAttemptCount)
                analyticsObj.put("sync_outcome", "SUCCESS")

                offlineQueueDao.updateActionStatus(
                    id = id,
                    status = SyncState.SUCCESS,
                    attemptCount = newAttemptCount,
                    lastAttemptedAt = now,
                    processedAt = now,
                    errorMessage = null,
                    processingTimestamp = action.processingTimestamp,
                    retryTimestamp = null,
                    syncAnalytics = analyticsObj.toString()
                )

                // Delete synced task to keep database pruned
                offlineQueueDao.deleteAction(id)
                Unit
            }
        }

    override suspend fun markFailed(id: String, error: String): Result<Unit> {
        val lower = error.lowercase()
        val isTransient = lower.contains("timeout") ||
                lower.contains("connect") ||
                lower.contains("host") ||
                lower.contains("network") ||
                lower.contains("online") ||
                lower.contains("ioexception") ||
                lower.contains("socket") ||
                lower.contains("ktor") ||
                lower.contains("ssl") ||
                lower.contains("502") ||
                lower.contains("503") ||
                lower.contains("504") ||
                lower.contains("429") // rate-limited retries allowed

        return if (isTransient) {
            val attempt = withContext(Dispatchers.IO) {
                offlineQueueDao.getActionById(id)?.attemptCount ?: 0
            }
            // Exponential backoff: 10s, 20s, 40s, 80s... maxing out at 5 minutes
            val backoffMs = (Math.pow(2.0, attempt.toDouble()) * 10000L).toLong().coerceAtMost(300000L)
            markRetrying(id, error, backoffMs)
        } else {
            markFailedPermanently(id, error)
        }
    }
}
