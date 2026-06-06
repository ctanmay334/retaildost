package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ProfileUpdate
 * ─────────────
 * Update payload class passed to Supabase Postgrest updates.
 * Sends user onboarding inputs to the database.
 */
@Serializable
data class ProfileUpdate(
    @SerialName("owner_name") val ownerName: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("pincode") val pincode: String,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    @SerialName("plan") val plan: String? = null,
    @SerialName("onboarded_at") val onboardedAt: String? = null
)
