package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ProfileDto
 * ──────────
 * Data Transfer Object representing a row in the public.profiles table in Supabase.
 * Includes @SerialName annotations to bridge database snake_case with Kotlin camelCase.
 */
@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("pincode") val pincode: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    @SerialName("plan") val plan: String = "pro",
    @SerialName("onboarded_at") val onboardedAt: String? = null
)
