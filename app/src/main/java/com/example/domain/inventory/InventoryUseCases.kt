package com.example.domain.inventory

import com.example.data.model.InventoryEntity
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// ── 1. Fetch / Query Use Case ────────────────────────────────────────────────

class GetInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    /**
     * Obtains Flow of inventory items matching search query, category, and status filters.
     * Enforces domain rules for low stock and expiry warnings.
     */
    operator fun invoke(
        searchQuery: String = "",
        categoryFilter: String? = null,
        showLowStockOnly: Boolean = false,
        showOutOfStockOnly: Boolean = false,
        showExpiringSoonOnly: Boolean = false
    ): Flow<List<InventoryEntity>> {
        return repository.allItems.map { list ->
            var filtered = list

            // 1. Search Query Filter (case insensitive)
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter {
                    it.itemName.contains(searchQuery, ignoreCase = true) ||
                            (it.category?.contains(searchQuery, ignoreCase = true) ?: false)
                }
            }

            // 2. Category Filter
            if (categoryFilter != null && categoryFilter.isNotBlank() && categoryFilter != "All") {
                filtered = filtered.filter { it.category == categoryFilter }
            }

            // 3. Low Stock detection (quantity <= minThreshold && quantity > 0)
            if (showLowStockOnly) {
                filtered = filtered.filter { it.quantity <= it.minThreshold && it.quantity > 0.0 }
            }

            // 3b. Out of Stock detection (quantity == 0)
            if (showOutOfStockOnly) {
                filtered = filtered.filter { it.quantity == 0.0 }
            }

            // 4. Expiry tracking filter (expiring within the next 30 days)
            if (showExpiringSoonOnly) {
                val now = System.currentTimeMillis()
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                filtered = filtered.filter { item ->
                    val expiryDateStr = item.expiryDate
                    if (!expiryDateStr.isNullOrBlank()) {
                        try {
                            val expiryTime = sdf.parse(expiryDateStr)?.time ?: 0L
                            expiryTime in now..(now + thirtyDaysMs)
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                }
            }

            filtered
        }
    }
}

// ── 2. Add Item Use Case ─────────────────────────────────────────────────────

class AddInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(
        storeId: String,
        itemName: String,
        category: String?,
        unitLabel: String?,
        quantity: Double,
        minThreshold: Double,
        costPrice: Double?,
        mrp: Double?,
        batchNo: String?,
        expiryDate: String?
    ): Result<Unit> {
        require(itemName.isNotBlank()) { "Item name cannot be empty" }
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(minThreshold >= 0) { "Min threshold cannot be negative" }

        val item = InventoryEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            itemName = itemName.trim(),
            category = category?.trim(),
            unitLabel = unitLabel?.trim(),
            quantity = quantity,
            minThreshold = minThreshold,
            costPrice = costPrice,
            mrp = mrp,
            batchNo = batchNo?.trim(),
            expiryDate = expiryDate?.trim(),
            source = "manual"
        )
        return repository.insertItem(item)
    }
}

// ── 3. Update Item Use Case ──────────────────────────────────────────────────

class UpdateInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(item: InventoryEntity): Result<Unit> {
        require(item.itemName.isNotBlank()) { "Item name cannot be empty" }
        require(item.quantity >= 0) { "Quantity cannot be negative" }

        val updated = item.copy(
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertItem(updated)
    }
}

// ── 4. Delete Item Use Case ──────────────────────────────────────────────────

class DeleteInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        require(id.isNotBlank()) { "Item ID cannot be empty" }
        return repository.deleteItem(id)
    }
}

// ── 5. Decrement Stock Use Case ──────────────────────────────────────────────

class DecrementStockUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    /**
     * Safely decrements stock levels of a product (e.g., during checkout).
     * Prevents negative stock amounts unless configured.
     */
    suspend operator fun invoke(id: String, count: Double): Result<Unit> {
        require(count > 0) { "Decrement count must be positive" }

        val item = repository.getItemById(id)
            ?: return Result.failure(IllegalArgumentException("Inventory item not found"))

        val newQuantity = (item.quantity - count).coerceAtLeast(0.0)
        val updated = item.copy(
            quantity = newQuantity,
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertItem(updated)
    }
}
