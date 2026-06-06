package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * DistributorEntity
 * ─────────────────
 * Room Database entity representing a distributor.
 * Caches marketplace entries locally for offline browsing.
 */
@Entity(
    tableName = "distributors",
    indices = [
        Index(value = ["pincode"]),
        Index(value = ["category"])
    ]
)
data class DistributorEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val businessName: String,
    val category: String,
    val phone: String,
    val whatsappNo: String,
    val pincode: String,
    val serviceRegions: List<String> = emptyList(),
    val address: String? = null,
    val minOrderValue: Double = 0.0,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
