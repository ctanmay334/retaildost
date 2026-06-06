package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KhataTransactionDto
 * ───────────────────
 * Data Transfer Object for the Supabase `public.khata_transactions` table.
 * Bridges Kotlin camelCase ↔ PostgreSQL snake_case via @SerialName.
 *
 * Note: `dueDate` exists in the Room entity but NOT in the Supabase schema,
 * so it is excluded from this DTO (local-only field).
 */
@Serializable
data class KhataTransactionDto(
    @SerialName("id") val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("tx_type") val txType: String,
    @SerialName("amount") val amount: Double,
    @SerialName("notes") val notes: String? = null,
    @SerialName("nlp_intent") val nlpIntent: String? = null,
    @SerialName("nlp_confidence") val nlpConfidence: Double? = null,
    @SerialName("raw_input") val rawInput: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    @SerialName("sale_record_id") val saleRecordId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** Converts this Supabase DTO to a local Room entity. */
    fun toEntity(): KhataTransactionEntity = KhataTransactionEntity(
        id = id,
        storeId = storeId,
        customerId = customerId,
        txType = txType,
        amount = amount,
        notes = notes,
        nlpIntent = nlpIntent,
        nlpConfidence = nlpConfidence,
        rawInput = rawInput,
        idempotencyKey = idempotencyKey,
        saleRecordId = saleRecordId,
        dueDate = null, // local-only field
        createdAt = parseIsoToMillis(createdAt),
        deletedAt = null // local-only field
    )
}

/** Extension: convert a Room KhataTransactionEntity to a Supabase-ready DTO. */
fun KhataTransactionEntity.toKhataTransactionDto(): KhataTransactionDto = KhataTransactionDto(
    id = id,
    storeId = storeId,
    customerId = customerId,
    txType = txType,
    amount = amount,
    notes = notes,
    nlpIntent = nlpIntent,
    nlpConfidence = nlpConfidence,
    rawInput = rawInput,
    idempotencyKey = idempotencyKey,
    saleRecordId = saleRecordId,
    createdAt = millisToIso(createdAt)
)
