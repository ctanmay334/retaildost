package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * InventoryDto
 * ────────────
 * Data Transfer Object for the Supabase `public.inventory` table.
 * Bridges Kotlin camelCase ↔ PostgreSQL snake_case via @SerialName.
 */
@Serializable
data class InventoryDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("category") val category: String? = null,
    @SerialName("unit_label") val unitLabel: String? = null,
    @SerialName("quantity") val quantity: Double = 0.0,
    @SerialName("min_threshold") val minThreshold: Double = 5.0,
    @SerialName("cost_price") val costPrice: Double? = null,
    @SerialName("mrp") val mrp: Double? = null,
    @SerialName("batch_no") val batchNo: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("ocr_confidence") val ocrConfidence: Double? = null,
    @SerialName("source") val source: String = "manual",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
) {
    /** Converts this Supabase DTO to a local Room entity. */
    fun toEntity(): InventoryEntity = InventoryEntity(
        id = id,
        storeId = storeId,
        itemName = itemName,
        category = category,
        unitLabel = unitLabel,
        quantity = quantity,
        minThreshold = minThreshold,
        costPrice = costPrice,
        mrp = mrp,
        batchNo = batchNo,
        expiryDate = expiryDate,
        ocrConfidence = ocrConfidence,
        source = source,
        createdAt = parseIsoToMillis(createdAt),
        updatedAt = parseIsoToMillis(updatedAt),
        deletedAt = deletedAt?.let { parseIsoToMillis(it) }
    )
}

/** Extension: convert a Room InventoryEntity to a Supabase-ready DTO. */
fun InventoryEntity.toInventoryDto(): InventoryDto = InventoryDto(
    id = id,
    storeId = storeId,
    itemName = itemName,
    category = category,
    unitLabel = unitLabel,
    quantity = quantity,
    minThreshold = minThreshold,
    costPrice = costPrice,
    mrp = mrp,
    batchNo = batchNo,
    expiryDate = expiryDate,
    ocrConfidence = ocrConfidence,
    source = source,
    createdAt = millisToIso(createdAt),
    updatedAt = millisToIso(updatedAt),
    deletedAt = deletedAt?.let { millisToIso(it) }
)

// ── Shared timestamp utilities ─────────────────────────────────────────────────

private val isoFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

fun millisToIso(millis: Long): String =
    isoFormat.get()!!.format(Date(millis))

fun parseIsoToMillis(iso: String?): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        isoFormat.get()!!.parse(iso)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        try {
            // Fallback: try parsing with OffsetDateTime (Java 8+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }
}
