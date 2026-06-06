package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ProfileEntity
 * ─────────────
 * Room Database entity representing the user's store profile.
 * Provides offline-first caching of tenant details.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,         // auth.users.id
    val storeId: String,                // tenant ID used in all sync records
    val ownerName: String,
    val storeName: String,
    val phone: String? = null,
    val pincode: String,
    val city: String,
    val state: String,
    val businessType: String,
    val plan: String = "pro",          // "free" | "pro"
    val onboardedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
