package com.example.data.repository

import com.example.data.model.InventoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * InventoryRepository
 * ───────────────────
 * Repository interface governing inventory operations.
 */
interface InventoryRepository {

    val allItems: Flow<List<InventoryEntity>>

    fun searchItems(query: String): Flow<List<InventoryEntity>>

    suspend fun getItemsPaged(limit: Int, offset: Int): List<InventoryEntity>

    suspend fun getItemById(id: String): InventoryEntity?

    suspend fun insertItem(item: InventoryEntity): Result<Unit>

    suspend fun deleteItem(id: String): Result<Unit>

    suspend fun syncPendingItems(): Result<Unit>
}
