package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val brand: String,
    val quantity: Int,
    val minThreshold: Int = 5,
    val costPrice: Double,
    val mrp: Double,
    val batchNo: String = "",
    val expiryDate: String = "", // "YYYY-MM-DD"
    val predictedExpiry: Boolean = false,
    val imageUri: String? = null
)
