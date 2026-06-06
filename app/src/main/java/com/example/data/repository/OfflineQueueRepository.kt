package com.example.data.repository

import com.example.data.model.OfflineQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * OfflineQueueRepository
 * ──────────────────────
 * Repository interface governing the local sync queue operations.
 * Upgraded with default arguments and backward compatibility mappings.
 */
interface OfflineQueueRepository {

    val pendingCount: Flow<Int>

    suspend fun enqueue(action: OfflineQueueEntity): Result<Unit>

    suspend fun getPendingActions(): List<OfflineQueueEntity>

    suspend fun markProcessing(id: String): Result<Unit>

    suspend fun markRetrying(id: String, error: String, nextAttemptDelayMs: Long): Result<Unit>

    suspend fun markFailedPermanently(id: String, error: String): Result<Unit>

    suspend fun markCompleted(id: String, durationMs: Long = 0L, networkType: String = "unknown"): Result<Unit>

    // Backward-compatible mapping helper
    suspend fun markFailed(id: String, error: String): Result<Unit>
}
