package com.example.data.dao

import androidx.room.*
import com.example.data.model.KhataCustomerEntity
import com.example.data.model.KhataTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * KhataDao
 * ────────
 * Room DAO managing credit customers and append-only ledger transaction histories.
 * Upgraded to filter out soft-deleted records.
 */
@Dao
interface KhataDao {

    // ── Customers ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: KhataCustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: KhataCustomerEntity)

    @Query("SELECT * FROM khata_customers WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<KhataCustomerEntity>>

    @Query("SELECT * FROM khata_customers WHERE deletedAt IS NULL ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getCustomersPaged(limit: Int, offset: Int): List<KhataCustomerEntity>

    @Query("SELECT * FROM khata_customers WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getCustomerById(id: String): KhataCustomerEntity?

    @Query("SELECT * FROM khata_customers WHERE name LIKE :query AND deletedAt IS NULL LIMIT 1")
    suspend fun getCustomerByName(query: String): KhataCustomerEntity?

    @Query("SELECT * FROM khata_customers WHERE name LIKE :query AND deletedAt IS NULL")
    suspend fun searchCustomersByName(query: String): List<KhataCustomerEntity>

    // ── Transactions ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: KhataTransactionEntity): Long

    @Query("SELECT * FROM khata_transactions WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<KhataTransactionEntity>>

    @Query("SELECT * FROM khata_transactions WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<KhataTransactionEntity>

    @Query("SELECT * FROM khata_transactions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<KhataTransactionEntity>>

    @Query("SELECT * FROM khata_transactions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllTransactions(): List<KhataTransactionEntity>

    @Query("SELECT * FROM khata_customers WHERE runningBalance > 0.0 AND deletedAt IS NULL ORDER BY name ASC")
    suspend fun getDebtors(): List<KhataCustomerEntity>

    @Query("SELECT * FROM khata_transactions WHERE dueDate IS NOT NULL AND deletedAt IS NULL")
    suspend fun getTransactionsWithDueDate(): List<KhataTransactionEntity>

    @Query("UPDATE khata_customers SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, deletedAt: Long)

    @Query("UPDATE khata_transactions SET deletedAt = :deletedAt WHERE customerId = :customerId")
    suspend fun softDeleteTransactionsForCustomer(customerId: String, deletedAt: Long)

    @Query("DELETE FROM khata_customers")
    suspend fun clearAllCustomers()

    @Query("DELETE FROM khata_transactions")
    suspend fun clearAllTransactions()
}
