package com.example.data.model

/**
 * AlertType
 * ─────────
 * Enum defining the specific categories of local alert notifications
 * in the RetailDost offline-first architecture.
 */
enum class AlertType {
    LOW_STOCK,
    EXPIRY_WARNING,
    KHATA_REMINDER,
    OCR_RETRY,
    SYNC_FAILURE
}
