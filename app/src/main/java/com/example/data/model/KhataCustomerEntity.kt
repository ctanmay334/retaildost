package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * KhataCustomerEntity
 * ──────────────────
 * Room Database entity representing the 'khata_customers' table.
 * Upgraded to support soft deletion and client-side idempotency keys.
 */
@Entity(
    tableName = "khata_customers",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["name"])
    ]
)
data class KhataCustomerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "storeId")
    val storeId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "runningBalance")
    val runningBalance: Double = 0.0,

    @ColumnInfo(name = "lastActivity")
    val lastActivity: Long? = null,

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
