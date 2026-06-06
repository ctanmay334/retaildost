package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val type: String, // "debit" (udhar) or "credit" (deposit)
    val amount: Double,
    val balanceAfter: Double,
    val rawInput: String = "", // original phrase like "Ramesh ka 500 ka udhar" or "voice"
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val isSettled: Boolean = false
)
