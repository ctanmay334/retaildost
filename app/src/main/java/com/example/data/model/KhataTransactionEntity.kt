package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * KhataTransactionEntity
 * ──────────────────────
 * Room Database entity representing the 'khata_transactions' table.
 * Upgraded to support soft deletion and client-side idempotency keys.
 */
@Entity(
    tableName = "khata_transactions",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["customerId"]),
        Index(value = ["saleRecordId"])
    ]
)
data class KhataTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "storeId")
    val storeId: String,

    @ColumnInfo(name = "customerId")
    val customerId: String,

    @ColumnInfo(name = "txType")
    val txType: String, // "debit" | "credit" | "reversal"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "nlpIntent")
    val nlpIntent: String? = null,

    @ColumnInfo(name = "nlpConfidence")
    val nlpConfidence: Double? = null,

    @ColumnInfo(name = "rawInput")
    val rawInput: String? = null,

    @ColumnInfo(name = "idempotencyKey")
    val idempotencyKey: String? = null,

    @ColumnInfo(name = "saleRecordId")
    val saleRecordId: String? = null,

    @ColumnInfo(name = "dueDate")
    val dueDate: Long? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    // --- Production-Grade Additions ---
    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "requestId")
    val requestId: String? = id
)
