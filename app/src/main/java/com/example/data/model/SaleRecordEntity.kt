package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * SaleRecordEntity
 * ────────────────
 * Room Database entity representing the header level 'sale_records' table.
 * Upgraded to support soft deletion and client-side idempotency keys.
 */
@Entity(
    tableName = "sale_records",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["saleDate"])
    ]
)
data class SaleRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "storeId")
    val storeId: String,

    @ColumnInfo(name = "customerName")
    val customerName: String? = null,

    @ColumnInfo(name = "source")
    val source: String = "manual",

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "totalAmount")
    val totalAmount: Double = 0.0,

    @ColumnInfo(name = "itemsCount")
    val itemsCount: Int = 0,

    @ColumnInfo(name = "saleDate")
    val saleDate: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    // --- Production-Grade Additions ---
    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "requestId")
    val requestId: String? = id
)
