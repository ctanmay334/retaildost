package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SaleRecordItemDto
 * ─────────────────
 * Data Transfer Object for the Supabase `public.sale_record_items` table.
 * Bridges Kotlin camelCase ↔ PostgreSQL snake_case via @SerialName.
 */
@Serializable
data class SaleRecordItemDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("sale_record_id") val saleRecordId: String,
    @SerialName("inventory_id") val inventoryId: String? = null,
    @SerialName("item_name") val itemName: String,
    @SerialName("unit_label") val unitLabel: String? = null,
    @SerialName("quantity_sold") val quantitySold: Double,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("cost_price") val costPrice: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** Converts this Supabase DTO to a local Room entity. */
    fun toEntity(): SaleRecordItemEntity = SaleRecordItemEntity(
        id = id,
        storeId = storeId,
        saleRecordId = saleRecordId,
        inventoryId = inventoryId,
        itemName = itemName,
        unitLabel = unitLabel,
        quantitySold = quantitySold,
        salePrice = salePrice,
        costPrice = costPrice,
        createdAt = parseIsoToMillis(createdAt)
    )
}

/** Extension: convert a Room SaleRecordItemEntity to a Supabase-ready DTO. */
fun SaleRecordItemEntity.toSaleRecordItemDto(): SaleRecordItemDto = SaleRecordItemDto(
    id = id,
    storeId = storeId,
    saleRecordId = saleRecordId,
    inventoryId = inventoryId,
    itemName = itemName,
    unitLabel = unitLabel,
    quantitySold = quantitySold,
    salePrice = salePrice,
    costPrice = costPrice,
    createdAt = millisToIso(createdAt)
)
