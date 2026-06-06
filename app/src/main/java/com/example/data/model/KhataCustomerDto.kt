package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KhataCustomerDto
 * ────────────────
 * Data Transfer Object for the Supabase `public.khata_customers` table.
 * Bridges Kotlin camelCase ↔ PostgreSQL snake_case via @SerialName.
 */
@Serializable
data class KhataCustomerDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("running_balance") val runningBalance: Double = 0.0,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /** Converts this Supabase DTO to a local Room entity. */
    fun toEntity(): KhataCustomerEntity = KhataCustomerEntity(
        id = id,
        storeId = storeId,
        name = name,
        phone = phone,
        email = null, // local-only field
        notes = notes,
        runningBalance = runningBalance,
        lastActivity = parseIsoToMillis(lastActivity).takeIf { lastActivity != null },
        createdAt = parseIsoToMillis(createdAt),
        updatedAt = parseIsoToMillis(updatedAt),
        deletedAt = null // local-only field
    )
}

/** Extension: convert a Room KhataCustomerEntity to a Supabase-ready DTO. */
fun KhataCustomerEntity.toKhataCustomerDto(): KhataCustomerDto = KhataCustomerDto(
    id = id,
    storeId = storeId,
    name = name,
    phone = phone,
    notes = notes,
    runningBalance = runningBalance,
    lastActivity = lastActivity?.let { millisToIso(it) },
    createdAt = millisToIso(createdAt),
    updatedAt = millisToIso(updatedAt)
)
