package com.example.data.model

/**
 * SyncState
 * ─────────
 * Declares the structured synchronization states for the RetailDost offline-first data pipeline.
 */
enum class SyncState {
    PENDING,      // waiting in queue
    PROCESSING,   // active synchronization transmission
    SUCCESS,      // synced successfully
    FAILED,       // terminal failure (invalid payload or format error)
    RETRYING      // transient retry scheduled
}
