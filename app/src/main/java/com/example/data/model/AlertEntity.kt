package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * AlertEntity
 * ───────────
 * Room Database entity representing the 'alerts' table.
 * Supports offline-first architecture by caching local notifications:
 * - LOW_STOCK
 * - EXPIRY_WARNING
 * - KHATA_REMINDER
 * - OCR_RETRY
 * - SYNC_FAILURE
 */
@Entity(
    tableName = "alerts",
    indices = [
        Index(value = ["alert_type"]),
        Index(value = ["is_read"]),
        Index(value = ["created_at"])
    ]
)
data class AlertEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "alert_type")
    val alertType: AlertType,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "deep_link")
    val deepLink: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null
)
