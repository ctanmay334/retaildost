package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncScheduler"
private const val UNIQUE_PERIODIC_WORK_NAME = "RetailDostPeriodicSyncWork"
private const val UNIQUE_ONE_TIME_WORK_NAME = "RetailDostOneTimeSyncWork"
private const val UNIQUE_DAILY_INVENTORY_WORK_NAME = "RetailDostDailyInventoryCheckWork"
private const val UNIQUE_IMMEDIATE_INVENTORY_WORK_NAME = "RetailDostImmediateInventoryCheckWork"

/**
 * SyncScheduler
 * ─────────────
 * Central orchestrator handling WorkManager task configurations for RetailDost offline synchronization.
 * Schedules periodic health check syncs, daily inventory audits, and triggers instant manual scans.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager? by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (e: Throwable) {
            Log.e(TAG, "WorkManager initialization failed", e)
            null
        }
    }

    /**
     * Schedules a unique periodic background worker task for syncing.
     * Guarantees periodic alignment (e.g., every 1 hour) to push any offline records,
     * restricted exclusively to situations where active internet access exists.
     */
    fun schedulePeriodicSync() {
        Log.i(TAG, "Configuring periodic sync worker (every 1 hour)...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Battery-optimized: Avoid pushing background sync on dead batteries
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS, // Execute once per hour
            15, TimeUnit.MINUTES // Flex interval of 15 minutes
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager?.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid resetting the hour counter
            periodicRequest
        )
    }

    /**
     * Enqueues an immediate, one-time sync task to drain the pending queue.
     * Can be invoked after locally modifying tables or when connectivity is restored.
     */
    fun triggerImmediateSync() {
        Log.i(TAG, "Enqueuing immediate one-time sync task request...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager?.enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace active so any new queue insertions trigger sync instantly
            oneTimeRequest
        )
    }

    /**
     * Schedules a unique periodic background task to perform daily inventory and alert checks.
     * Checks low stock levels, expiration parameters, outstanding ledger balances, and failed offline queues.
     * Battery and Storage Optimized: Runs once every 24 hours only when the battery is not low.
     */
    fun scheduleDailyInventoryChecks() {
        Log.i(TAG, "Configuring periodic daily inventory and database health checker worker (every 24 hours)...")

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)   // High battery optimization
            .setRequiresStorageNotLow(true)   // Storage optimization safety
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<AlertCheckingWorker>(
            24, TimeUnit.HOURS, // Execute once every 24 hours for daily checks
            2, TimeUnit.HOURS   // Flex interval of 2 hours to let Android schedule during idle time
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager?.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_INVENTORY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Prevent resetting timers when application restarts
            periodicRequest
        )
    }

    /**
     * Enqueues an immediate on-demand inventory check, usually triggered manually
     * by the user or key onboarding/synchronization lifecycle events.
     */
    fun triggerImmediateInventoryCheck() {
        Log.i(TAG, "Enqueuing immediate database alert scan...")

        val oneTimeRequest = OneTimeWorkRequestBuilder<AlertCheckingWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager?.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_INVENTORY_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace active to trigger immediate scan instantly
            oneTimeRequest
        )
    }

    /**
     * Cancels any active scheduled work requests.
     */
    fun cancelAllScheduledSyncs() {
        Log.w(TAG, "Cancelling all active synchronization and inventory background worker tasks.")
        workManager?.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        workManager?.cancelUniqueWork(UNIQUE_ONE_TIME_WORK_NAME)
        workManager?.cancelUniqueWork(UNIQUE_DAILY_INVENTORY_WORK_NAME)
        workManager?.cancelUniqueWork(UNIQUE_IMMEDIATE_INVENTORY_WORK_NAME)
    }
}
