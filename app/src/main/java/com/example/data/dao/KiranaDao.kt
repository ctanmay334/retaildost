package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomerEntity
import com.example.data.model.ItemEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KiranaDao {
    // Inventory Items
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Int): ItemEntity?

    @Query("SELECT * FROM items WHERE name = :name LIMIT 1")
    suspend fun getItemByName(name: String): ItemEntity?

    // Customers (Khata)
    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomersCount(): Int

    @Query("SELECT * FROM customers ORDER BY lastTransaction DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE :name LIMIT 1")
    suspend fun getCustomerByName(name: String): CustomerEntity?

    // Transactions
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE transactions SET isSettled = 1 WHERE customerId = :customerId AND isSettled = 0")
    suspend fun settleAllTransactionsForCustomer(customerId: Int)

    // User Profile caching
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: com.example.data.model.ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): com.example.data.model.ProfileEntity?

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomerId(customerId: Int)

    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
