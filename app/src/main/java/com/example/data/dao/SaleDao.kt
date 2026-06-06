package com.example.data.dao

import androidx.room.*
import com.example.data.model.SaleRecordEntity
import com.example.data.model.SaleRecordItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * SaleDao
 * ───────
 * Room DAO managing invoices and matching sales items.
 * Upgraded to filter out soft-deleted records.
 */
@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecord(sale: SaleRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleRecordItemEntity>)

    @Query("SELECT * FROM sale_records WHERE deletedAt IS NULL ORDER BY saleDate DESC, createdAt DESC")
    fun getAllSalesFlow(): Flow<List<SaleRecordEntity>>

    @Query("SELECT * FROM sale_records WHERE deletedAt IS NULL ORDER BY saleDate DESC, createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesPaged(limit: Int, offset: Int): List<SaleRecordEntity>

    @Query("SELECT * FROM sale_record_items WHERE saleRecordId = :saleRecordId")
    suspend fun getItemsForSale(saleRecordId: String): List<SaleRecordItemEntity>

    @Query("SELECT * FROM sale_records WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getSaleRecordById(id: String): SaleRecordEntity?

    @Query("UPDATE sale_records SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteSale(id: String, deletedAt: Long)

    @Query("SELECT * FROM sale_record_items WHERE inventoryId = :inventoryId ORDER BY createdAt DESC")
    fun getSaleItemsForProductFlow(inventoryId: String): Flow<List<SaleRecordItemEntity>>

    @Query("DELETE FROM sale_records")
    suspend fun clearAllSaleRecords()

    @Query("DELETE FROM sale_record_items")
    suspend fun clearAllSaleItems()
}
