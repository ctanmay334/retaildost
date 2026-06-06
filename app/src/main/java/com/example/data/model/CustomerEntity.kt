package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val balance: Double = 0.0, // Positive value: client owes us. Negative: advance.
    val lastTransaction: Long = System.currentTimeMillis()
)
