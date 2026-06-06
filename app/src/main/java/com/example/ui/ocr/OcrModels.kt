package com.example.ui.ocr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScannedInvoice(
    @SerialName("supplier_name")   val supplierName: String?   = null,
    @SerialName("invoice_number")  val invoiceNumber: String?  = null,
    @SerialName("invoice_date")    val invoiceDate: String?    = null,
    @SerialName("currency")        val currency: String?       = "INR",
    @SerialName("items")           val items: List<ScannedItem> = emptyList(),
    @SerialName("subtotal")        val subtotal: Double?       = null,
    @SerialName("tax_amount")      val taxAmount: Double?      = null,
    @SerialName("total_amount")    val totalAmount: Double?    = null
)

@Serializable
data class ScannedItem(
    @SerialName("id")          val id: String  = java.util.UUID.randomUUID().toString(),
    @SerialName("name")        val name: String,
    @SerialName("quantity")    val quantity: Double,
    @SerialName("unit")        val unit: String?        = null,   // kg, pcs, box, litre, dozen
    @SerialName("unit_price")  val unitPrice: Double?  = null,
    @SerialName("total_price") val totalPrice: Double? = null,
    @SerialName("sku")         val sku: String?         = null,
    @SerialName("hsn_code")    val hsnCode: String?     = null,   // Indian GST invoices
    @SerialName("brand")       val brand: String?       = null,
    @SerialName("description") val description: String? = null
)

data class ReviewableItem(
    val scannedItem: ScannedItem,
    val isEdited: Boolean = false,
    val matchedProductId: String? = null,   // null = create new product
    val matchedProductName: String? = null
)
