package com.example

import android.app.Application
import com.example.sync.NetworkObserver
import com.example.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * RetailDostApplication
 * ─────────────────────
 * Extends Android Application, marked with Hilt's @HiltAndroidApp.
 * Performs critical application bootstrapping by scheduling background sync cycles
 * and binding dynamic internet change listeners.
 */
@HiltAndroidApp
class RetailDostApplication : Application() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var networkObserver: NetworkObserver

    init {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("RetailDostApp", "Severe uncaught exception intercepted on startup", throwable)
            
            // Try to store the crash diagnostic info to a local text file
            try {
                val file = java.io.File(filesDir, "last_crash.txt")
                file.writeText(android.util.Log.getStackTraceString(throwable))
            } catch (e: Throwable) {
                // filesDir or baseContext might not be ready yet
            }

            // Attempt to launch the standalone, non-Hilt CrashReportActivity
            try {
                val intent = android.content.Intent().apply {
                    action = "com.example.action.SHOW_CRASH"
                    setClassName(packageName, "com.example.CrashReportActivity")
                    putExtra("error_message", throwable.localizedMessage ?: throwable.message ?: "Unknown severe exception")
                    putExtra("stack_trace", android.util.Log.getStackTraceString(throwable))
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Throwable) {
                // If anything fails (e.g. context is not fully ready), pass it back to OS
                defaultHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }

            // Terminate the crashed process clean
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(10)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize all standard premium notification channels
        try {
            com.example.utils.NotificationHelper.createNotificationChannels(this)
        } catch (e: Throwable) {
            android.util.Log.e("RetailDostApp", "Failed to create notification channels: ${e.message}", e)
        }
        
        // Triggers scheduling of the WorkManager tasks
        try {
            syncScheduler.schedulePeriodicSync()
            syncScheduler.scheduleDailyInventoryChecks()
            
            // Trigger an immediate check on startup for real-time notifications
            syncScheduler.triggerImmediateInventoryCheck()
        } catch (e: Throwable) {
            android.util.Log.e("RetailDostApp", "Failed to schedule workers: ${e.message}", e)
        }
        
        // Starts observing system-wide connectivity changes
        try {
            networkObserver.startObserving()
        } catch (e: Throwable) {
            android.util.Log.e("RetailDostApp", "Failed to start network observer: ${e.message}", e)
        }
    }
}
