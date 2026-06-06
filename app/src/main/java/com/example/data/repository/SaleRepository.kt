package com.example.data.repository

import com.example.data.model.SaleRecordEntity
import com.example.data.model.SaleRecordItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * SaleRepository
 * ──────────────
 * Interface governing invoice and invoice line-item actions.
 */
interface SaleRepository {

    val allSales: Flow<List<SaleRecordEntity>>

    suspend fun getSalesPaged(limit: Int, offset: Int): List<SaleRecordEntity>

    suspend fun getSaleWithItems(saleId: String): Pair<SaleRecordEntity?, List<SaleRecordItemEntity>>

    /**
     * Creates a new sale record. Writes header & lines to Room,
     * then attempts sync with Supabase tables.
     */
    suspend fun createSale(sale: SaleRecordEntity, items: List<SaleRecordItemEntity>): Result<Unit>

    suspend fun syncPendingSales(): Result<Unit>

    fun getSaleItemsForProductFlow(inventoryId: String): Flow<List<SaleRecordItemEntity>>
}
