package com.example.domain.sale

import com.example.data.model.SaleRecordEntity
import com.example.data.model.SaleRecordItemEntity
import com.example.data.model.KhataTransactionEntity
import com.example.data.repository.SaleRepository
import com.example.data.repository.KhataRepository
import com.example.domain.inventory.DecrementStockUseCase
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * CreateSaleUseCase
 * ──────────────────
 * Use case to manage the transactional process of checking out a sale:
 * 1. Saves the SaleRecordEntity and list of SaleRecordItemEntity via SaleRepository.
 * 2. Decrements the stock quantity of all line items in the inventory.
 * 3. If credit payment is selected, posts a debit transaction to the customer's ledger.
 */
class CreateSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val khataRepository: KhataRepository,
    private val decrementStockUseCase: DecrementStockUseCase
) {
    suspend operator fun invoke(
        storeId: String,
        customerName: String?,
        customerId: String?, // Linked customer if payment is Credit/Udhaar
        paymentMode: String, // "Cash" or "Credit"
        totalAmount: Double,
        notes: String?,
        items: List<SaleRecordItemEntity>
    ): Result<Unit> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart cannot be empty"))
        }
        if (paymentMode == "Credit" && customerId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Customer must be selected for Udhaar/Credit sale"))
        }

        val saleId = UUID.randomUUID().toString()
        val saleDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val saleRecord = SaleRecordEntity(
            id = saleId,
            storeId = storeId,
            customerName = customerName,
            source = "manual",
            notes = notes,
            totalAmount = totalAmount,
            itemsCount = items.size,
            saleDate = saleDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val preparedItems = items.map { item ->
            item.copy(
                id = UUID.randomUUID().toString(),
                saleRecordId = saleId,
                storeId = storeId,
                createdAt = System.currentTimeMillis()
            )
        }

        // 1. Create sale record in DB/Remote
        val saleResult = saleRepository.createSale(saleRecord, preparedItems)
        if (saleResult.isFailure) {
            return saleResult
        }

        // 2. Decrement inventory stock levels
        for (item in preparedItems) {
            val invId = item.inventoryId
            if (!invId.isNullOrBlank()) {
                val decResult = decrementStockUseCase(invId, item.quantitySold)
                if (decResult.isFailure) {
                    // Log warning but don't abort, since database sale is already written
                    android.util.Log.w("CreateSaleUseCase", "Failed to decrement stock for item ${item.itemName}: ${decResult.exceptionOrNull()?.message}")
                }
            }
        }

        // 3. Post a debit transaction to Customer Ledger if payment is credit
        if (paymentMode == "Credit" && !customerId.isNullOrBlank()) {
            val khataTx = KhataTransactionEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                customerId = customerId,
                txType = "debit",
                amount = totalAmount,
                notes = notes ?: "Udhaar for Sale #${saleId.take(8)}",
                saleRecordId = saleId,
                createdAt = System.currentTimeMillis()
            )
            val khataResult = khataRepository.addTransaction(khataTx)
            if (khataResult.isFailure) {
                android.util.Log.e("CreateSaleUseCase", "Failed to post ledger debit for customer $customerId: ${khataResult.exceptionOrNull()?.message}")
            }
        }

        return Result.success(Unit)
    }
}

/**
 * GetSalesHistoryUseCase
 * ──────────────────────
 * Obtains the flow of sale record history from the SaleRepository.
 */
class GetSalesHistoryUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    operator fun invoke(): Flow<List<SaleRecordEntity>> {
        return saleRepository.allSales
    }
}
