package com.example.data.dao

import androidx.room.*
import com.example.data.model.InventoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * InventoryDao
 * ────────────
 * Data Access Object for the 'inventory' table.
 * Supports reactive Flows, paged retrieval, and full CRUD operations.
 * Upgraded to filter out soft-deleted products.
 */
@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory WHERE deletedAt IS NULL ORDER BY itemName ASC")
    fun getAllInventoryFlow(): Flow<List<InventoryEntity>>

    /** Paged query for large catalogues to optimize performance */
    @Query("SELECT * FROM inventory WHERE deletedAt IS NULL ORDER BY itemName ASC LIMIT :limit OFFSET :offset")
    suspend fun getInventoryPaged(limit: Int, offset: Int): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE itemName LIKE :query AND deletedAt IS NULL ORDER BY itemName ASC")
    fun searchInventoryFlow(query: String): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryEntity): Long

    @Update
    suspend fun updateItem(item: InventoryEntity)

    @Query("UPDATE inventory SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("SELECT * FROM inventory WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getItemById(id: String): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE quantity <= minThreshold AND deletedAt IS NULL")
    suspend fun getLowStockItems(): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE expiryDate IS NOT NULL AND expiryDate != '' AND deletedAt IS NULL")
    suspend fun getItemsWithExpiry(): List<InventoryEntity>

    @Query("SELECT COUNT(*) FROM inventory WHERE deletedAt IS NULL")
    suspend fun getItemsCount(): Int

    @Query("DELETE FROM inventory")
    suspend fun clearAllInventory()
}
