package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * OfflineQueueEntity
 * ──────────────────
 * Room Database entity representing the 'offline_queue' table.
 * Manages actions created while offline and tracks their detailed synchronization lifecycle.
 */
@Entity(
    tableName = "offline_queue",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["status"])
    ]
)
data class OfflineQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "storeId")
    val storeId: String,

    @ColumnInfo(name = "actionType")
    val actionType: String, // "inventory_add" | "inventory_edit" | "sale_record" | "khata_entry"

    @ColumnInfo(name = "idempotencyKey")
    val idempotencyKey: String,

    @ColumnInfo(name = "payload")
    val payload: String, // JSON payload string

    @ColumnInfo(name = "status")
    val status: SyncState = SyncState.PENDING,

    @ColumnInfo(name = "attemptCount")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "lastAttemptedAt")
    val lastAttemptedAt: Long? = null,

    @ColumnInfo(name = "processedAt")
    val processedAt: Long? = null,

    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = null,

    @ColumnInfo(name = "clientTs")
    val clientTs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    // --- Production-Grade Additions for Advanced Sync Tracking ---
    @ColumnInfo(name = "processingTimestamp")
    val processingTimestamp: Long? = null,

    @ColumnInfo(name = "retryTimestamp")
    val retryTimestamp: Long? = null,

    @ColumnInfo(name = "syncAnalytics")
    val syncAnalytics: String? = null // Holds JSON metadata (e.g. latency, bandwidth metrics)
)
