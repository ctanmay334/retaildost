package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * InventoryEntity
 * ───────────────
 * Room Database entity representing the 'inventory' table.
 * Houses product catalog details per tenant store.
 * Upgraded to support soft deletion and client-side idempotency keys.
 */
@Entity(
    tableName = "inventory",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["itemName"]),
        Index(value = ["expiryDate"])
    ]
)
data class InventoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "storeId")
    val storeId: String,

    @ColumnInfo(name = "itemName")
    val itemName: String,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "unitLabel")
    val unitLabel: String? = null,

    @ColumnInfo(name = "quantity")
    val quantity: Double = 0.0,

    @ColumnInfo(name = "minThreshold")
    val minThreshold: Double = 5.0,

    @ColumnInfo(name = "costPrice")
    val costPrice: Double? = null,

    @ColumnInfo(name = "mrp")
    val mrp: Double? = null,

    @ColumnInfo(name = "batchNo")
    val batchNo: String? = null,

    @ColumnInfo(name = "expiryDate")
    val expiryDate: String? = null, // "YYYY-MM-DD"

    @ColumnInfo(name = "ocrConfidence")
    val ocrConfidence: Double? = null,

    @ColumnInfo(name = "source")
    val source: String = "manual",

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    // --- Production-Grade Additions ---
    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long? = null, // null = active, timestamp = soft deleted

    @ColumnInfo(name = "requestId")
    val requestId: String? = id // matches PostgreSQL request_id to prevent duplicate insert
)
