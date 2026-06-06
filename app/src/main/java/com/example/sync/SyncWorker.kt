package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "SyncWorker"

/**
 * SyncWorker
 * ──────────
 * WorkManager CoroutineWorker that processes all pending offline transactions
 * (inventory edits, sales, khata transactions) in the queue.
 * Integrates partial failure handling, transient connection checks, and robust retry logic.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting background offline queue synchronization...")

        // 1. Fetch repositories using Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncEntryPoint::class.java
        )
        val offlineQueueRepo = entryPoint.offlineQueueRepository()
        val inventoryRepo = entryPoint.inventoryRepository()
        val saleRepo = entryPoint.saleRepository()
        val khataRepo = entryPoint.khataRepository()
        val ocrScannerRepo = entryPoint.ocrScannerRepository()

        // 2. Fetch pending tasks to see if work is required
        val pendingCount = offlineQueueRepo.getPendingActions().size
        if (pendingCount == 0) {
            Log.i(TAG, "No pending actions found in queue. Synchronization complete.")
            return@withContext Result.success()
        }

        Log.i(TAG, "Found $pendingCount actions pending sync. Processing...")

        // Track if a transient failure occurs to trigger a WorkManager retry
        var hasTransientError = false

        // 3. Process inventory pending items
        try {
            val res = inventoryRepo.syncPendingItems()
            if (res.isFailure) {
                val exception = res.exceptionOrNull()
                if (isTransientNetworkError(exception)) {
                    hasTransientError = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during inventory sync: ${e.message}", e)
            if (isTransientNetworkError(e)) {
                hasTransientError = true
            }
        }

        // 3b. Process pending offline OCR scans
        try {
            val res = ocrScannerRepo.syncPendingOcrScans(applicationContext)
            if (res.isFailure) {
                val exception = res.exceptionOrNull()
                if (isTransientNetworkError(exception)) {
                    hasTransientError = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during offline OCR scan sync: ${e.message}", e)
            if (isTransientNetworkError(e)) {
                hasTransientError = true
            }
        }

        // 4. Process sales records
        try {
            val res = saleRepo.syncPendingSales()
            if (res.isFailure) {
                val exception = res.exceptionOrNull()
                if (isTransientNetworkError(exception)) {
                    hasTransientError = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sales sync: ${e.message}", e)
            if (isTransientNetworkError(e)) {
                hasTransientError = true
            }
        }

        // 5. Process khata entries
        try {
            val res = khataRepo.syncPendingKhata()
            if (res.isFailure) {
                val exception = res.exceptionOrNull()
                if (isTransientNetworkError(exception)) {
                    hasTransientError = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during khata sync: ${e.message}", e)
            if (isTransientNetworkError(e)) {
                hasTransientError = true
            }
        }

        // 6. Return retry or success based on execution outcomes
        if (hasTransientError) {
            val attempt = runAttemptCount
            Log.w(TAG, "Sync failed due to transient connection issue (Attempt $attempt). Rescheduling with WorkManager retry.")
            return@withContext Result.retry()
        }

        // Re-query count to see if we successfully drained the queue
        val remainingCount = offlineQueueRepo.getPendingActions().size
        Log.i(TAG, "Draining finished. Remaining pending sync jobs in database: $remainingCount")

        return@withContext Result.success()
    }

    /**
     * Determines if an error is transient (e.g. network disconnect, timeout, DNS failure)
     * and should trigger an exponential backup retry, versus a permanent format error.
     */
    private fun isTransientNetworkError(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        val message = throwable.message?.lowercase() ?: ""
        return throwable is IOException ||
                throwable.javaClass.name.contains("ktor", ignoreCase = true) ||
                throwable.javaClass.name.contains("socket", ignoreCase = true) ||
                throwable.javaClass.name.contains("timeout", ignoreCase = true) ||
                message.contains("timeout") ||
                message.contains("connect") ||
                message.contains("host") ||
                message.contains("network") ||
                message.contains("online")
    }
}
