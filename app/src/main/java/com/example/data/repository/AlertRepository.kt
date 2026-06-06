package com.example.data.repository

import com.example.data.model.AlertEntity
import com.example.data.model.AlertType
import kotlinx.coroutines.flow.Flow

/**
 * AlertRepository
 * ───────────────
 * Repository managing local system alerts (stock notifications, expiry warnings,
 * khata reminders, OCR retries, and sync failures) in an offline-first architecture.
 */
interface AlertRepository {

    val allAlerts: Flow<List<AlertEntity>>

    val unreadAlerts: Flow<List<AlertEntity>>

    val unreadCount: Flow<Int>

    suspend fun insertAlert(alert: AlertEntity): Result<Long>

    suspend fun insertAlerts(alerts: List<AlertEntity>): Result<Unit>

    suspend fun getAlertsPaged(limit: Int, offset: Int): List<AlertEntity>

    fun getAlertsByTypeFlow(type: AlertType): Flow<List<AlertEntity>>

    suspend fun markAsRead(id: String): Result<Unit>

    /**
     * Synchronizes and pulls alerts from remote Supabase DB, updates Room cache.
     * Implements offline-first reconciliation.
     */
    suspend fun fetchRemoteAlerts(storeId: String): Result<Unit>

    /**
     * Clears all alerts locally and deletes them from Supabase remote database.
     */
    suspend fun clearAllAlerts(storeId: String): Result<Unit>

    /**
     * Marks all unread alerts as read in both local Room cache and remote Supabase.
     */
    suspend fun markAllAsRead(storeId: String): Result<Unit>
}
