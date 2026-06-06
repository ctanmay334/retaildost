package com.example.data.repository

import com.example.data.model.KhataCustomerEntity
import com.example.data.model.KhataTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * KhataRepository
 * ───────────────
 * Repository interface governing credit customers and ledger ledger transactions.
 */
interface KhataRepository {

    val allCustomers: Flow<List<KhataCustomerEntity>>

    val allTransactions: Flow<List<KhataTransactionEntity>>

    suspend fun getCustomersPaged(limit: Int, offset: Int): List<KhataCustomerEntity>

    suspend fun getCustomerById(id: String): KhataCustomerEntity?

    suspend fun getCustomerByName(name: String): KhataCustomerEntity?

    suspend fun searchCustomersByName(name: String): List<KhataCustomerEntity>

    suspend fun insertCustomer(customer: KhataCustomerEntity): Result<Unit>

    /**
     * Records a ledger transaction. Writes to Room DB and adjusts the customer's balance
     * atomically, then attempts to push updates to Supabase tables.
     */
    suspend fun addTransaction(transaction: KhataTransactionEntity): Result<Unit>

    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<KhataTransactionEntity>>

    suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<KhataTransactionEntity>

    /**
     * Synchronizes pending offline khata actions to Supabase.
     */
    suspend fun syncPendingKhata(): Result<Unit>

    /**
     * Fetches all remote customers and transactions from Supabase for the given storeId and caches them.
     */
    suspend fun fetchKhataFromRemote(storeId: String): Result<Unit>

    /**
     * Deletes a credit customer and their ledger transactions locally and remotely on Supabase.
     */
    suspend fun deleteCustomer(customerId: String): Result<Unit>
}
