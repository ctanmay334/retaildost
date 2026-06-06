package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.AlertType
import com.example.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "AlertCheckingWorker"

/**
 * AlertCheckingWorker
 * ───────────────────
 * Periodic WorkManager worker that runs background checks directly on the local Room cache
 * for low-stock inventory, expiring products, pending khata reminders, and failed sync events.
 * Implements fatigue protection deduplication and is highly battery-optimized.
 */
class AlertCheckingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting periodic local database alert scan...")

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val inventoryDao = db.inventoryDao()
            val khataDao = db.khataDao()
            val offlineQueueDao = db.offlineQueueDao()
            val alertDao = db.alertDao()

            // 1. Check Low Stock Items
            checkLowStock(inventoryDao, alertDao)

            // 2. Check Expiry Dates
            checkExpiry(inventoryDao, alertDao)

            // 3. Check Pending Khata Balances
            checkKhataReminders(khataDao, alertDao)

            // 4. Check Failed Offline Actions
            checkFailedSyncs(offlineQueueDao, alertDao)

            Log.i(TAG, "Local database alert scan finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred during local database alert scan", e)
            Result.retry()
        }
    }

    private suspend fun checkLowStock(
        inventoryDao: com.example.data.dao.InventoryDao,
        alertDao: com.example.data.dao.AlertDao
    ) {
        val items = inventoryDao.getLowStockItems()
        Log.d(TAG, "Low stock check scanned ${items.size} matching records.")

        // 3-day cooldown window: only suppress if an alert was already created for this item in the last 3 days
        val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L

        for (item in items) {
            // Deduplication: only check alerts created within the last 3 days to allow re-alerting
            val exists = alertDao.getAlertCountByTypeAndMessage(
                alertType = AlertType.LOW_STOCK,
                query = "%${item.itemName}%",
                sinceTimestamp = threeDaysAgo
            ) > 0

            if (!exists) {
                val alertEntity = AlertEntity(
                    id = UUID.randomUUID().toString(),
                    title = "⚠️ Low Stock Alert",
                    message = "${item.itemName} is running low! Only ${item.quantity.toInt()} ${item.unitLabel ?: "pcs"} left.",
                    alertType = AlertType.LOW_STOCK,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    deepLink = "retaildost://inventory_detail?inventory_id=${item.id}",
                    metadataJson = "{\"store_id\":\"${item.storeId}\",\"inventory_id\":\"${item.id}\",\"item_name\":\"${item.itemName}\"}"
                )
                alertDao.insertAlert(alertEntity)

                NotificationHelper.showLowStockNotification(
                    context = applicationContext,
                    itemName = item.itemName,
                    currentQty = item.quantity,
                    unit = item.unitLabel ?: "pcs",
                    inventoryId = item.id
                )
                Log.i(TAG, "Dispatched Low Stock notification for: ${item.itemName}")
            }
        }
    }

    private suspend fun checkExpiry(
        inventoryDao: com.example.data.dao.InventoryDao,
        alertDao: com.example.data.dao.AlertDao
    ) {
        val items = inventoryDao.getItemsWithExpiry()
        Log.d(TAG, "Expiry warning check scanned ${items.size} items with expiry parameters.")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Date()

        for (item in items) {
            val dateStr = item.expiryDate ?: continue
            try {
                val expiryDate = sdf.parse(dateStr) ?: continue
                val diffMs = expiryDate.time - today.time
                val daysToExpiry = (diffMs / (1000 * 60 * 60 * 24)).toInt()

                // Warning zone: expiring within 30 days and not yet fully expired (daysToExpiry >= 0)
                if (daysToExpiry in 0..30) {
                    val exists = alertDao.getAlertCountByTypeAndMessage(
                        alertType = AlertType.EXPIRY_WARNING,
                        query = "%${item.itemName}%",
                        sinceTimestamp = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L // 7-day window for expiry warnings
                    ) > 0

                    if (!exists) {
                        val alertEntity = AlertEntity(
                            id = UUID.randomUUID().toString(),
                            title = "⏰ Expiry Warning",
                            message = "${item.itemName} is expiring in $daysToExpiry days! Please review stock.",
                            alertType = AlertType.EXPIRY_WARNING,
                            isRead = false,
                            createdAt = System.currentTimeMillis(),
                            deepLink = "retaildost://inventory_detail?inventory_id=${item.id}",
                            metadataJson = "{\"store_id\":\"${item.storeId}\",\"inventory_id\":\"${item.id}\",\"item_name\":\"${item.itemName}\",\"days_to_expiry\":$daysToExpiry}"
                        )
                        alertDao.insertAlert(alertEntity)

                        NotificationHelper.showExpiryWarningNotification(
                            context = applicationContext,
                            itemName = item.itemName,
                            daysToExpiry = daysToExpiry,
                            inventoryId = item.id
                        )
                        Log.i(TAG, "Dispatched Expiry Warning notification for: ${item.itemName}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not parse expiry date string: '$dateStr' for ${item.itemName}", e)
            }
        }
    }

    private suspend fun checkKhataReminders(
        khataDao: com.example.data.dao.KhataDao,
        alertDao: com.example.data.dao.AlertDao
    ) {
        // 1. General outstanding balance collection check
        val debtors = khataDao.getDebtors()
        Log.d(TAG, "Khata reminder check scanned ${debtors.size} customer records with positive balance.")
        
        val debtorMap = debtors.associateBy { it.id }

        for (debtor in debtors) {
            // Only alert when the amount of udhari is >= 5000 rupees
            if (debtor.runningBalance < 5000.0) continue
            val exists = alertDao.getAlertCountByTypeAndMessage(
                alertType = AlertType.KHATA_REMINDER,
                query = "%outstanding balance for ${debtor.name}%",
                sinceTimestamp = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L // 3-day window
            ) > 0

            if (!exists) {
                val alertEntity = AlertEntity(
                    id = UUID.randomUUID().toString(),
                    title = "💰 Khata Payment Collection",
                    message = "Reminder: ₹${debtor.runningBalance.toInt()} outstanding balance for ${debtor.name}.",
                    alertType = AlertType.KHATA_REMINDER,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    deepLink = "retaildost://customer_ledger?customer_id=${debtor.id}",
                    metadataJson = "{\"store_id\":\"${debtor.storeId}\",\"customer_id\":\"${debtor.id}\",\"customer_name\":\"${debtor.name}\",\"amount\":${debtor.runningBalance}}"
                )
                alertDao.insertAlert(alertEntity)

                NotificationHelper.showKhataReminderNotification(
                    context = applicationContext,
                    customerName = debtor.name,
                    amount = debtor.runningBalance,
                    customerId = debtor.id
                )
                Log.i(TAG, "Dispatched Khata Reminder notification for: ${debtor.name}")
            }
        }

        // 2. Specific due date alerts
        val transactionsWithDueDate = khataDao.getTransactionsWithDueDate()
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)

        for (tx in transactionsWithDueDate) {
            if (tx.txType != "debit") continue
            val customer = debtorMap[tx.customerId] ?: khataDao.getCustomerById(tx.customerId) ?: continue
            // Only alert when the customer's total outstanding balance (udhari) is >= 5000 rupees
            if (customer.runningBalance < 5000.0) continue

            val dueDateVal = tx.dueDate ?: continue
            val diffMs = dueDateVal - currentTime
            val dueFormatted = sdf.format(Date(dueDateVal))

            if (diffMs in 0..oneDayMs) {
                // 1 Day or less left before due date
                val exists = alertDao.getAlertCountByTypeAndMessage(
                    alertType = AlertType.KHATA_REMINDER,
                    query = "%due%${tx.id}%",
                    sinceTimestamp = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L // 1-day window for due alerts
                ) > 0

                if (!exists) {
                    val alertEntity = AlertEntity(
                        id = UUID.randomUUID().toString(),
                        title = "⏰ Payment Due Tomorrow",
                        message = "Upcoming payment of ₹${tx.amount.toInt()} from ${customer.name} is due on $dueFormatted. (Ref: ${tx.id})",
                        alertType = AlertType.KHATA_REMINDER,
                        isRead = false,
                        createdAt = System.currentTimeMillis(),
                        deepLink = "retaildost://customer_ledger?customer_id=${customer.id}",
                        metadataJson = "{\"store_id\":\"${tx.storeId}\",\"customer_id\":\"${customer.id}\",\"transaction_id\":\"${tx.id}\",\"due_date\":$dueDateVal}"
                    )
                    alertDao.insertAlert(alertEntity)

                    NotificationHelper.showKhataReminderNotification(
                        context = applicationContext,
                        customerName = customer.name,
                        amount = tx.amount,
                        customerId = customer.id,
                        title = "⏰ Payment Due Tomorrow",
                        message = "Upcoming payment of ₹${tx.amount.toInt()} from ${customer.name} is due on $dueFormatted."
                    )
                    Log.i(TAG, "Dispatched Upcoming Khata Reminder for customer: ${customer.name}, Tx: ${tx.id}")
                }
            } else if (diffMs < 0) {
                // Overdue
                val exists = alertDao.getAlertCountByTypeAndMessage(
                    alertType = AlertType.KHATA_REMINDER,
                    query = "%OVERDUE%${tx.id}%",
                    sinceTimestamp = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L // 1-day window for overdue alerts
                ) > 0

                if (!exists) {
                    val alertEntity = AlertEntity(
                        id = UUID.randomUUID().toString(),
                        title = "⚠️ Payment Overdue",
                        message = "Payment of ₹${tx.amount.toInt()} from ${customer.name} was due on $dueFormatted and is now OVERDUE! (Ref: ${tx.id})",
                        alertType = AlertType.KHATA_REMINDER,
                        isRead = false,
                        createdAt = System.currentTimeMillis(),
                        deepLink = "retaildost://customer_ledger?customer_id=${customer.id}",
                        metadataJson = "{\"store_id\":\"${tx.storeId}\",\"customer_id\":\"${customer.id}\",\"transaction_id\":\"${tx.id}\",\"due_date\":$dueDateVal}"
                    )
                    alertDao.insertAlert(alertEntity)

                    NotificationHelper.showKhataReminderNotification(
                        context = applicationContext,
                        customerName = customer.name,
                        amount = tx.amount,
                        customerId = customer.id,
                        title = "⚠️ Payment Overdue",
                        message = "Payment of ₹${tx.amount.toInt()} from ${customer.name} was due on $dueFormatted and is now OVERDUE!"
                    )
                    Log.i(TAG, "Dispatched Overdue Khata Reminder for customer: ${customer.name}, Tx: ${tx.id}")
                }
            }
        }
    }

    private suspend fun checkFailedSyncs(
        offlineQueueDao: com.example.data.dao.OfflineQueueDao,
        alertDao: com.example.data.dao.AlertDao
    ) {
        val failed = offlineQueueDao.getFailedActions()
        Log.d(TAG, "Offline sync queue check found ${failed.size} failed actions.")

        if (failed.isNotEmpty()) {
            val count = failed.size
            val exists = alertDao.getAlertCountByTypeAndMessage(
                alertType = AlertType.SYNC_FAILURE,
                query = "%failed to sync%"
            ) > 0

            if (!exists) {
                val alertEntity = AlertEntity(
                    id = UUID.randomUUID().toString(),
                    title = "🔄 Cloud Sync Failure",
                    message = "Failed to sync $count transaction(s). Tap to review and retry.",
                    alertType = AlertType.SYNC_FAILURE,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    deepLink = "retaildost://settings",
                    metadataJson = "{\"failed_items_count\":$count}"
                )
                alertDao.insertAlert(alertEntity)

                val errorMsg = failed.firstOrNull()?.errorMessage ?: "Network timeout during upload."
                NotificationHelper.showSyncFailureNotification(
                    context = applicationContext,
                    failedItemsCount = count,
                    errorMessage = errorMsg
                )
                Log.i(TAG, "Dispatched Cloud Sync Failure notification for $count actions.")
            }
        }
    }
}
