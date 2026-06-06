package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SaleRecordDto
 * ─────────────
 * Data Transfer Object for the Supabase `public.sale_records` table.
 * Bridges Kotlin camelCase ↔ PostgreSQL snake_case via @SerialName.
 */
@Serializable
data class SaleRecordDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("source") val source: String = "manual",
    @SerialName("notes") val notes: String? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("items_count") val itemsCount: Int = 0,
    @SerialName("sale_date") val saleDate: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
) {
    /** Converts this Supabase DTO to a local Room entity. */
    fun toEntity(): SaleRecordEntity = SaleRecordEntity(
        id = id,
        storeId = storeId,
        customerName = customerName,
        source = source,
        notes = notes,
        totalAmount = totalAmount,
        itemsCount = itemsCount,
        saleDate = saleDate,
        createdAt = parseIsoToMillis(createdAt),
        updatedAt = parseIsoToMillis(updatedAt),
        deletedAt = deletedAt?.let { parseIsoToMillis(it) }
    )
}

/** Extension: convert a Room SaleRecordEntity to a Supabase-ready DTO. */
fun SaleRecordEntity.toSaleRecordDto(): SaleRecordDto = SaleRecordDto(
    id = id,
    storeId = storeId,
    customerName = customerName,
    source = source,
    notes = notes,
    totalAmount = totalAmount,
    itemsCount = itemsCount,
    saleDate = saleDate,
    createdAt = millisToIso(createdAt),
    updatedAt = millisToIso(updatedAt),
    deletedAt = deletedAt?.let { millisToIso(it) }
)
