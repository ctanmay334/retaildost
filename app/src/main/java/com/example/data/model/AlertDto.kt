package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AlertDto
 * ────────
 * Serialization data transfer object matching the remote public.alerts Supabase table.
 * Used for fetching alerts from the cloud database and synchronizing with local cache.
 */
@Serializable
data class AlertDto(
    @SerialName("id") val id: String? = null,
    @SerialName("store_id") val storeId: String,
    @SerialName("alert_type") val alertType: String,
    @SerialName("inventory_id") val inventoryId: String? = null,
    @SerialName("item_name") val itemName: String,
    @SerialName("message") val message: String,
    @SerialName("days_to_expiry") val daysToExpiry: Int? = null,
    @SerialName("current_qty") val currentQty: Double? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
