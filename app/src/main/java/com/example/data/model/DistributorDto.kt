package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DistributorDto
 * ──────────────
 * Serialization data transfer object matching the public.distributors Supabase table.
 */
@Serializable
data class DistributorDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("category") val category: String,
    @SerialName("phone") val phone: String,
    @SerialName("whatsapp_no") val whatsappNo: String,
    @SerialName("pincode") val pincode: String,
    @SerialName("service_regions") val serviceRegions: List<String> = emptyList(),
    @SerialName("address") val address: String? = null,
    @SerialName("min_order_value") val minOrderValue: Double = 0.0,
    @SerialName("is_verified") val isVerified: Boolean = false
)
