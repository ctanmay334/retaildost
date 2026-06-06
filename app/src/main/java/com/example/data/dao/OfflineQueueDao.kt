package com.example.data.dao

import androidx.room.*
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * OfflineQueueDao
 * ──────────────
 * Room DAO managing offline sync jobs queued to be pushed to Supabase.
 * Upgraded to support granular SyncState and advanced metrics.
 */
@Dao
interface OfflineQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAction(action: OfflineQueueEntity): Long

    @Query("SELECT * FROM offline_queue WHERE id = :id")
    suspend fun getActionById(id: String): OfflineQueueEntity?

    @Query("SELECT * FROM offline_queue WHERE status = 'PENDING' ORDER BY clientTs ASC")
    suspend fun getPendingActions(): List<OfflineQueueEntity>

    @Query("SELECT COUNT(*) FROM offline_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("UPDATE offline_queue SET status = :status, attemptCount = :attemptCount, lastAttemptedAt = :lastAttemptedAt, processedAt = :processedAt, errorMessage = :errorMessage, processingTimestamp = :processingTimestamp, retryTimestamp = :retryTimestamp, syncAnalytics = :syncAnalytics WHERE id = :id")
    suspend fun updateActionStatus(
        id: String,
        status: SyncState,
        attemptCount: Int,
        lastAttemptedAt: Long?,
        processedAt: Long?,
        errorMessage: String?,
        processingTimestamp: Long?,
        retryTimestamp: Long?,
        syncAnalytics: String?
    )

    @Query("DELETE FROM offline_queue WHERE id = :id")
    suspend fun deleteAction(id: String)

    @Query("DELETE FROM offline_queue WHERE status = 'SUCCESS'")
    suspend fun clearSyncedActions()

    @Query("SELECT * FROM offline_queue WHERE status = 'FAILED' ORDER BY clientTs DESC")
    suspend fun getFailedActions(): List<OfflineQueueEntity>
}
