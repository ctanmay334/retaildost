package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderHelper {
    /**
     * Generates a polite payment reminder template for the credit customer.
     */
    fun getReminderMessage(
        customerName: String,
        amount: Double,
        storeName: String,
        dueDateMs: Long? = null
    ): String {
        val amountInt = amount.toInt()
        val baseMessage = "Dear $customerName, your outstanding balance is ₹$amountInt at $storeName."
        
        val dueDateStr = if (dueDateMs != null) {
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(dueDateMs))
            } catch (e: Exception) {
                null
            }
        } else null

        return if (dueDateStr != null) {
            "Namaste $customerName ji 🙏\n\nThis is a friendly reminder that ₹$amountInt is due by $dueDateStr at $storeName. Please settle it via cash or UPI. Thank you! 🙏"
        } else {
            "Namaste $customerName ji 🙏\n\nA friendly reminder that ₹$amountInt is outstanding at $storeName. Kripya jaldi settle karein. Dhanyavaad! 🙏"
        }
    }
}
