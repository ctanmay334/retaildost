package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.UUID

/**
 * NotificationHelper
 * ──────────────────
 * Reusable utility that coordinates Android 13+ styled notification channels, deep linking,
 * grouped notification cards, and automatically managed summary alerts.
 * Exposes feature-specific APIs to support low stock, expiry, khata ledger, and sync warnings.
 */
object NotificationHelper {

    const val CHANNEL_INVENTORY = "inventory_alerts"
    const val CHANNEL_EXPIRY = "expiry_alerts"
    const val CHANNEL_KHATA = "khata_alerts"
    const val CHANNEL_SYNC = "sync_alerts"

    private const val GROUP_INVENTORY = "com.example.retaildost.INVENTORY_ALERTS"
    private const val GROUP_EXPIRY = "com.example.retaildost.EXPIRY_ALERTS"
    private const val GROUP_KHATA = "com.example.retaildost.KHATA_ALERTS"
    private const val GROUP_SYNC = "com.example.retaildost.SYNC_ALERTS"

    private const val SUMMARY_ID_INVENTORY = 1001
    private const val SUMMARY_ID_EXPIRY = 1002
    private const val SUMMARY_ID_KHATA = 1003
    private const val SUMMARY_ID_SYNC = 1004

    /**
     * Initializes all standard notification channels for RetailDost.
     * Safe to invoke on application bootstrapping (Oreo API 26+ compatible).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_INVENTORY,
                    "Inventory Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts regarding low stock limits, out-of-stock items, and automated restocking suggestions."
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_EXPIRY,
                    "Expiry Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical alerts for stock items nearing expiry within 7 to 30 days."
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_KHATA,
                    "Khata Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates regarding ledger balances, payment reminder dispatches, and customer settlements."
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_SYNC,
                    "Sync Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Updates on offline queue operations and local background database synchronization status."
                    setShowBadge(false)
                }
            )

            notificationManager.createNotificationChannels(channels)
            Log.i("NotificationHelper", "Initialized RetailDost Notification Channels successfully")
        }
    }

    /**
     * Triggers a strongly-typed low-stock alert notification.
     * Routes directly to the inventory detail screen.
     */
    fun showLowStockNotification(
        context: Context,
        itemName: String,
        currentQty: Double,
        unit: String,
        inventoryId: String
    ) {
        val title = "⚠️ Low Stock Alert"
        val message = "$itemName is running low! Only ${currentQty.toInt()} $unit left."
        val deepLink = "retaildost://inventory_detail?inventory_id=$inventoryId"
        showNotification(context, CHANNEL_INVENTORY, title, message, deepLink)
    }

    /**
     * Triggers a strongly-typed expiry warning notification.
     * Routes directly to the inventory detail screen.
     */
    fun showExpiryWarningNotification(
        context: Context,
        itemName: String,
        daysToExpiry: Int,
        inventoryId: String
    ) {
        val title = "⏰ Expiry Warning"
        val message = "$itemName is expiring in $daysToExpiry days! Please review stock."
        val deepLink = "retaildost://inventory_detail?inventory_id=$inventoryId"
        showNotification(context, CHANNEL_EXPIRY, title, message, deepLink)
    }

    /**
     * Triggers a strongly-typed khata payment collection reminder notification.
     * Routes directly to the customer ledger screen.
     */
    fun showKhataReminderNotification(
        context: Context,
        customerName: String,
        amount: Double,
        customerId: String,
        title: String = "💰 Khata Payment Collection",
        message: String? = null
    ) {
        val finalTitle = title
        val finalMessage = message ?: "Reminder: ₹${amount.toInt()} outstanding balance for $customerName."
        val deepLink = "retaildost://customer_ledger?customer_id=$customerId"
        showNotification(context, CHANNEL_KHATA, finalTitle, finalMessage, deepLink)
    }

    /**
     * Triggers a strongly-typed cloud sync failure notification.
     * Routes directly to Settings screen where sync can be manually retried.
     */
    fun showSyncFailureNotification(
        context: Context,
        failedItemsCount: Int,
        errorMessage: String
    ) {
        val title = "🔄 Cloud Sync Failure"
        val message = "Failed to sync $failedItemsCount transaction(s): $errorMessage"
        val deepLink = "retaildost://settings"
        showNotification(context, CHANNEL_SYNC, title, message, deepLink)
    }

    /**
     * Shows a styled notification under the specified channel, handling deep linking,
     * grouping, and automatic summary updates.
     */
    fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        deepLink: String? = null,
        notificationId: Int = UUID.randomUUID().hashCode()
    ) {
        // 1. Android 13+ POST_NOTIFICATIONS runtime check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationHelper", "Cannot show notification: POST_NOTIFICATIONS permission not granted.")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channels are created
        createNotificationChannels(context)

        // 2. Build deep-link intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!deepLink.isNullOrBlank()) {
                data = Uri.parse(deepLink)
                // Add query parameters as intent extras for backwards compatibility
                val uri = Uri.parse(deepLink)
                uri.getQueryParameter("inventory_id")?.let { invId ->
                    putExtra("inventory_id", invId)
                    putExtra("navigate_to", "inventory_detail")
                }
                uri.getQueryParameter("customer_id")?.let { custId ->
                    putExtra("customer_id", custId)
                    putExtra("navigate_to", "customer_ledger")
                }
                if (uri.host == "settings") {
                    putExtra("navigate_to", "settings")
                }
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val groupKey = getGroupKey(channelId)
        val smallIcon = getSmallIcon()

        // 3. Build Individual Grouped Notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(groupKey)
            .setPriority(getPriority(channelId))
            .setCategory(getCategory(channelId))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        // 4. Build Group Summary Notification
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(getGroupTitle(channelId))
            .setContentText(getGroupSummaryText(channelId))
            .setStyle(NotificationCompat.InboxStyle()
                .setBigContentTitle(getGroupTitle(channelId))
                .setSummaryText(getGroupSummaryText(channelId))
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        // 5. Post notification and update its group summary
        notificationManager.notify(notificationId, notification)
        notificationManager.notify(getSummaryId(channelId), summaryNotification)
    }

    private fun getGroupKey(channelId: String): String {
        return when (channelId) {
            CHANNEL_INVENTORY -> GROUP_INVENTORY
            CHANNEL_EXPIRY -> GROUP_EXPIRY
            CHANNEL_KHATA -> GROUP_KHATA
            CHANNEL_SYNC -> GROUP_SYNC
            else -> "com.example.retaildost.DEFAULT_ALERTS"
        }
    }

    private fun getGroupTitle(channelId: String): String {
        return when (channelId) {
            CHANNEL_INVENTORY -> "Inventory Alerts"
            CHANNEL_EXPIRY -> "Expiry Warnings"
            CHANNEL_KHATA -> "Khata Ledger Updates"
            CHANNEL_SYNC -> "Cloud Synchronizations"
            else -> "RetailDost System Alerts"
        }
    }

    private fun getGroupSummaryText(channelId: String): String {
        return when (channelId) {
            CHANNEL_INVENTORY -> "Stock updates"
            CHANNEL_EXPIRY -> "Expiry warnings"
            CHANNEL_KHATA -> "Khata balances"
            CHANNEL_SYNC -> "Sync processes"
            else -> "Notifications"
        }
    }

    private fun getSmallIcon(): Int {
        return android.R.drawable.stat_notify_chat
    }

    private fun getPriority(channelId: String): Int {
        return when (channelId) {
            CHANNEL_INVENTORY, CHANNEL_EXPIRY -> NotificationCompat.PRIORITY_HIGH
            CHANNEL_KHATA -> NotificationCompat.PRIORITY_DEFAULT
            CHANNEL_SYNC -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun getCategory(channelId: String): String {
        return when (channelId) {
            CHANNEL_INVENTORY, CHANNEL_EXPIRY -> NotificationCompat.CATEGORY_ALARM
            CHANNEL_KHATA, CHANNEL_SYNC -> NotificationCompat.CATEGORY_STATUS
            else -> NotificationCompat.CATEGORY_MESSAGE
        }
    }

    private fun getSummaryId(channelId: String): Int {
        return when (channelId) {
            CHANNEL_INVENTORY -> SUMMARY_ID_INVENTORY
            CHANNEL_EXPIRY -> SUMMARY_ID_EXPIRY
            CHANNEL_KHATA -> SUMMARY_ID_KHATA
            CHANNEL_SYNC -> SUMMARY_ID_SYNC
            else -> 1000
        }
    }
}
