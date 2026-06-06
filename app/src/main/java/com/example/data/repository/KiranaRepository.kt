package com.example.data.repository

import com.example.data.dao.KiranaDao
import com.example.data.auth.AuthRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class KiranaRepository(
    private val kiranaDao: KiranaDao,
    private val khataRepository: KhataRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {

    val allItems: Flow<List<ItemEntity>> = kiranaDao.getAllItems()
    val allCustomers: Flow<List<CustomerEntity>> = kiranaDao.getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = kiranaDao.getAllTransactions()

    suspend fun insertItem(item: ItemEntity): Long {
        return kiranaDao.insertItem(item)
    }

    suspend fun getItemsCount(): Int {
        return kiranaDao.getItemsCount()
    }

    suspend fun getCustomersCount(): Int {
        return kiranaDao.getCustomersCount()
    }

    suspend fun deleteItem(id: Int) {
        kiranaDao.deleteItemById(id)
    }

    suspend fun getItemById(id: Int): ItemEntity? {
        return kiranaDao.getItemById(id)
    }

    suspend fun getItemByName(name: String): ItemEntity? {
        return kiranaDao.getItemByName(name)
    }

    suspend fun getCustomerByName(name: String): CustomerEntity? {
        return kiranaDao.getCustomerByName(name)
    }

    suspend fun insertCustomer(customer: CustomerEntity): Long {
        val currentUser = authRepository.getCurrentUser()
        val profile = currentUser?.let { profileRepository.getProfile(it.id).getOrNull() }
        val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

        val customerId = UUID.randomUUID().toString()
        val newKhataCustomer = KhataCustomerEntity(
            id = customerId,
            storeId = storeId,
            name = customer.name,
            phone = customer.phone.takeIf { it.isNotEmpty() },
            notes = "Created from legacy repository wrapper",
            runningBalance = customer.balance,
            lastActivity = customer.lastTransaction
        )

        // Sync modern customer to Supabase/modern Room DB
        khataRepository.insertCustomer(newKhataCustomer)

        val legacyId = customerId.hashCode()
        val legacyCustomer = customer.copy(id = legacyId)
        kiranaDao.insertCustomer(legacyCustomer)
        return legacyId.toLong()
    }

    suspend fun getCustomerById(id: Int): CustomerEntity? {
        return kiranaDao.getCustomerById(id)
    }

    suspend fun getTransactionsForCustomer(customerId: Int): Flow<List<TransactionEntity>> {
        return kiranaDao.getTransactionsForCustomer(customerId)
    }

    // Atomic transaction booking system inspired by Khatabook (TRD)
    suspend fun addTransaction(
        customerId: Int,
        type: String, // "debit" (udhar) or "credit" (deposit)
        amount: Double,
        rawInput: String = "",
        dueDate: Long? = null,
        isSettled: Boolean = false
    ): Boolean {
        val customer = kiranaDao.getCustomerById(customerId) ?: return false
        val newBalance = if (type == "debit") {
            customer.balance + amount
        } else {
            customer.balance - amount
        }

        val currentUser = authRepository.getCurrentUser()
        val profile = currentUser?.let { profileRepository.getProfile(it.id).getOrNull() }
        val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

        var modernCustomer = khataRepository.getCustomerByName(customer.name)
        if (modernCustomer == null) {
            val modernId = UUID.randomUUID().toString()
            modernCustomer = KhataCustomerEntity(
                id = modernId,
                storeId = storeId,
                name = customer.name,
                phone = customer.phone.takeIf { it.isNotEmpty() },
                runningBalance = 0.0,
                lastActivity = System.currentTimeMillis()
            )
            khataRepository.insertCustomer(modernCustomer)
        }

        val modernTxId = UUID.randomUUID().toString()
        val khataTx = KhataTransactionEntity(
            id = modernTxId,
            storeId = storeId,
            customerId = modernCustomer.id,
            txType = type,
            amount = amount,
            notes = rawInput.takeIf { it.isNotEmpty() },
            rawInput = rawInput,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis()
        )

        // Sync modern transaction to Supabase/Room
        khataRepository.addTransaction(khataTx)

        // Insert legacy transaction
        val tx = TransactionEntity(
            id = modernTxId.hashCode(),
            customerId = customerId,
            type = type,
            amount = amount,
            balanceAfter = newBalance,
            rawInput = rawInput,
            date = System.currentTimeMillis(),
            dueDate = dueDate,
            isSettled = isSettled
        )
        kiranaDao.insertTransaction(tx)

        // Update legacy customer balance
        val updatedCustomer = customer.copy(
            balance = newBalance,
            lastTransaction = System.currentTimeMillis()
        )
        kiranaDao.updateCustomer(updatedCustomer)
        return true
    }

    suspend fun settleAllTransactionsForCustomer(customerId: Int): Boolean {
        val customer = kiranaDao.getCustomerById(customerId) ?: return false
        
        // Settle all tx in legacy local DB
        kiranaDao.settleAllTransactionsForCustomer(customerId)
        
        val currentUser = authRepository.getCurrentUser()
        val profile = currentUser?.let { profileRepository.getProfile(it.id).getOrNull() }
        val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

        val modernCustomer = khataRepository.getCustomerByName(customer.name)
        val settlementTxId = UUID.randomUUID().toString()
        if (modernCustomer != null) {
            val currentBalance = modernCustomer.runningBalance
            if (currentBalance != 0.0) {
                val type = if (currentBalance > 0) "credit" else "debit"
                val khataTx = KhataTransactionEntity(
                    id = settlementTxId,
                    storeId = storeId,
                    customerId = modernCustomer.id,
                    txType = type,
                    amount = kotlin.math.abs(currentBalance),
                    notes = "Account Settle Adjustment",
                    createdAt = System.currentTimeMillis()
                )
                khataRepository.addTransaction(khataTx)
            }
        }

        // Reset balance to 0 and record a settlement tx in legacy Room
        val settlementTx = TransactionEntity(
            id = settlementTxId.hashCode(),
            customerId = customerId,
            type = if (customer.balance >= 0) "credit" else "debit",
            amount = kotlin.math.abs(customer.balance),
            balanceAfter = 0.0,
            rawInput = "Account Settled",
            date = System.currentTimeMillis(),
            isSettled = true
        )
        kiranaDao.insertTransaction(settlementTx)
        
        val updatedCustomer = customer.copy(
            balance = 0.0,
            lastTransaction = System.currentTimeMillis()
        )
        kiranaDao.updateCustomer(updatedCustomer)
        return true
    }

    suspend fun deleteCustomer(customerId: Int) {
        val customer = kiranaDao.getCustomerById(customerId)
        if (customer != null) {
            val modernCustomer = khataRepository.getCustomerByName(customer.name)
            if (modernCustomer != null) {
                khataRepository.deleteCustomer(modernCustomer.id)
            }
        }
        kiranaDao.deleteCustomerById(customerId)
        kiranaDao.deleteTransactionsByCustomerId(customerId)
    }
}
