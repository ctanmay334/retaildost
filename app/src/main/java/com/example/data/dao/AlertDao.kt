package com.example.data.dao

import androidx.room.*
import com.example.data.model.AlertEntity
import com.example.data.model.AlertType
import kotlinx.coroutines.flow.Flow

/**
 * AlertDao
 * ────────
 * Room DAO managing local and remote synchronised system alerts.
 * Features full reactive Flow support, pagination, and transactional bulk insertion.
 */
@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("SELECT * FROM alerts ORDER BY created_at DESC")
    fun getAllAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAlertsPaged(limit: Int, offset: Int): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE is_read = 0 ORDER BY created_at DESC")
    fun getUnreadAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM alerts WHERE is_read = 0")
    fun getUnreadAlertsCountFlow(): Flow<Int>

    @Query("UPDATE alerts SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE alerts SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllAsRead()

    @Query("SELECT * FROM alerts WHERE alert_type = :alertType ORDER BY created_at DESC")
    fun getAlertsByTypeFlow(alertType: AlertType): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()

    @Query("SELECT COUNT(*) FROM alerts WHERE alert_type = :alertType AND message LIKE :query AND is_read = 0")
    suspend fun getUnreadAlertCountByTypeAndMessage(alertType: AlertType, query: String): Int

    /**
     * Checks if a recent alert (within last 3 days) exists for this item+type.
     * Using a 3-day cooldown window instead of checking all-time alerts prevents
     * permanent deduplication from blocking re-alerts after the user reads them.
     */
    @Query("SELECT COUNT(*) FROM alerts WHERE alert_type = :alertType AND message LIKE :query AND created_at > :sinceTimestamp")
    suspend fun getAlertCountByTypeAndMessage(alertType: AlertType, query: String, sinceTimestamp: Long = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L): Int
}
