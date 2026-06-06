package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * SaleRecordItemEntity
 * ────────────────────
 * Room Database entity representing the line items inside the 'sale_record_items' table.
 */
@Entity(
    tableName = "sale_record_items",
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["saleRecordId"]),
        Index(value = ["inventoryId"])
    ]
)
data class SaleRecordItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val saleRecordId: String,
    val inventoryId: String? = null,
    val itemName: String,
    val unitLabel: String? = null,
    val quantitySold: Double,
    val salePrice: Double? = null,
    val costPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
