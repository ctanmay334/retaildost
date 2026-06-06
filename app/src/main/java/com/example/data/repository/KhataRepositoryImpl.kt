package com.example.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.data.dao.KhataDao
import com.example.data.dao.KiranaDao
import com.example.data.db.AppDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.KhataCustomerDto
import com.example.data.model.KhataTransactionDto
import com.example.data.model.KhataCustomerEntity
import com.example.data.model.KhataTransactionEntity
import com.example.data.model.OfflineQueueEntity
import com.example.data.model.toKhataCustomerDto
import com.example.data.model.toKhataTransactionDto
import com.example.data.model.parseIsoToMillis
import com.example.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "KhataRepositoryImpl"

/**
 * KhataRepositoryImpl
 * ───────────────────
 * Offline-first repository executing ledger changes on Room and syncing with Supabase.
 * Integrates OfflineQueueRepository to serialize offline transactions/customers and triggers SyncScheduler to process them.
 *
 * All Supabase operations use KhataCustomerDto / KhataTransactionDto (with @SerialName snake_case mappings)
 * to ensure proper column name mapping between Kotlin camelCase and PostgreSQL snake_case.
 */
class KhataRepositoryImpl @Inject constructor(
    private val khataDao: KhataDao,
    private val supabaseClient: SupabaseClient,
    private val offlineQueueRepository: OfflineQueueRepository,
    private val syncScheduler: SyncScheduler,
    private val kiranaDao: KiranaDao,
    private val appDatabase: AppDatabase
) : KhataRepository {

    override val allCustomers: Flow<List<KhataCustomerEntity>> = khataDao.getAllCustomersFlow()

    override val allTransactions: Flow<List<KhataTransactionEntity>> = khataDao.getAllTransactionsFlow()

    override suspend fun getCustomersPaged(limit: Int, offset: Int): List<KhataCustomerEntity> =
        withContext(Dispatchers.IO) {
            khataDao.getCustomersPaged(limit, offset)
        }

    override suspend fun getCustomerById(id: String): KhataCustomerEntity? =
        withContext(Dispatchers.IO) {
            khataDao.getCustomerById(id)
        }

    override suspend fun getCustomerByName(name: String): KhataCustomerEntity? =
        withContext(Dispatchers.IO) {
            khataDao.getCustomerByName("%$name%")
        }

    override suspend fun searchCustomersByName(name: String): List<KhataCustomerEntity> =
        withContext(Dispatchers.IO) {
            khataDao.searchCustomersByName("%$name%")
        }

    override suspend fun insertCustomer(customer: KhataCustomerEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dto = customer.toKhataCustomerDto()
                val postgrest = supabaseClient.postgrest
                try {
                    // 1. Try to sync to Supabase first
                    postgrest["khata_customers"].upsert(dto)
                    Log.i(TAG, "Customer synced to Supabase successfully: ${customer.id}")
                    // 2. Cache in local modern Room DB on success
                    khataDao.insertCustomer(customer)
                } catch (e: Exception) {
                    if (isNetworkError(e)) {
                        Log.w(TAG, "Network error during customer sync, caching locally and enqueuing offline task: ${e.message}", e)
                        // Cache in local Room DB first
                        khataDao.insertCustomer(customer)
                        // Enqueue offline action
                        enqueueCustomerOffline(customer)
                    } else {
                        Log.e(TAG, "Permanent database/RLS error during customer insert: ${e.message}", e)
                        throw e
                    }
                }
                Unit
            }
        }

    private suspend fun enqueueCustomerOffline(customer: KhataCustomerEntity) {
        val payload = JSONObject().apply {
            put("entryType", "customer")
            put("customer", JSONObject().apply {
                put("id", customer.id)
                put("storeId", customer.storeId)
                put("name", customer.name)
                put("phone", if (customer.phone == null) JSONObject.NULL else customer.phone)
                put("email", if (customer.email == null) JSONObject.NULL else customer.email)
                put("notes", if (customer.notes == null) JSONObject.NULL else customer.notes)
                put("runningBalance", customer.runningBalance)
                put("lastActivity", if (customer.lastActivity == null) JSONObject.NULL else customer.lastActivity)
                put("createdAt", customer.createdAt)
                put("updatedAt", customer.updatedAt)
                put("deletedAt", if (customer.deletedAt == null) JSONObject.NULL else customer.deletedAt)
            })
        }.toString()

        offlineQueueRepository.enqueue(
            OfflineQueueEntity(
                storeId = customer.storeId,
                actionType = "khata_entry",
                idempotencyKey = customer.id,
                payload = payload
            )
        )
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun addTransaction(transaction: KhataTransactionEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Retrieve customer to compute new balance locally (Optimistic state tracking)
                val customer = khataDao.getCustomerById(transaction.customerId)
                    ?: error("Customer not found with id=${transaction.customerId}")

                val updatedBalance = when (transaction.txType) {
                    "debit" -> customer.runningBalance + transaction.amount
                    "credit" -> customer.runningBalance - transaction.amount
                    else -> customer.runningBalance
                }

                val updatedCustomer = customer.copy(
                    runningBalance = updatedBalance,
                    lastActivity = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val customerDto = updatedCustomer.toKhataCustomerDto()
                val transactionDto = transaction.toKhataTransactionDto()
                val postgrest = supabaseClient.postgrest

                try {
                    // 1. Try to sync to Supabase first (source of truth)
                    postgrest["khata_customers"].upsert(customerDto)
                    postgrest["khata_transactions"].upsert(transactionDto)
                    Log.i(TAG, "Ledger transaction synced to Supabase: ${transaction.id}")
                    
                    // 2. Cache in local Room DB on success
                    khataDao.insertTransaction(transaction)
                    khataDao.updateCustomer(updatedCustomer)
                } catch (e: Exception) {
                    if (isNetworkError(e)) {
                        Log.w(TAG, "Network error during transaction sync, caching locally and enqueuing offline task: ${e.message}", e)
                        // Cache in local Room DB first
                        khataDao.insertTransaction(transaction)
                        khataDao.updateCustomer(updatedCustomer)
                        // Enqueue offline action
                        enqueueTransactionOffline(updatedCustomer, transaction)
                    } else {
                        Log.e(TAG, "Permanent database/RLS error during transaction insert: ${e.message}", e)
                        throw e
                    }
                }
                Unit
            }
        }

    private suspend fun enqueueTransactionOffline(updatedCustomer: KhataCustomerEntity, transaction: KhataTransactionEntity) {
        val payload = JSONObject().apply {
            put("entryType", "transaction")
            put("customer", JSONObject().apply {
                put("id", updatedCustomer.id)
                put("storeId", updatedCustomer.storeId)
                put("name", updatedCustomer.name)
                put("phone", if (updatedCustomer.phone == null) JSONObject.NULL else updatedCustomer.phone)
                put("email", if (updatedCustomer.email == null) JSONObject.NULL else updatedCustomer.email)
                put("notes", if (updatedCustomer.notes == null) JSONObject.NULL else updatedCustomer.notes)
                put("runningBalance", updatedCustomer.runningBalance)
                put("lastActivity", if (updatedCustomer.lastActivity == null) JSONObject.NULL else updatedCustomer.lastActivity)
                put("createdAt", updatedCustomer.createdAt)
                put("updatedAt", updatedCustomer.updatedAt)
                put("deletedAt", if (updatedCustomer.deletedAt == null) JSONObject.NULL else updatedCustomer.deletedAt)
            })
            put("transaction", JSONObject().apply {
                put("id", transaction.id)
                put("storeId", transaction.storeId)
                put("customerId", transaction.customerId)
                put("txType", transaction.txType)
                put("amount", transaction.amount)
                put("notes", if (transaction.notes == null) JSONObject.NULL else transaction.notes)
                put("nlpIntent", if (transaction.nlpIntent == null) JSONObject.NULL else transaction.nlpIntent)
                put("nlpConfidence", if (transaction.nlpConfidence == null) JSONObject.NULL else transaction.nlpConfidence)
                put("rawInput", if (transaction.rawInput == null) JSONObject.NULL else transaction.rawInput)
                put("idempotencyKey", if (transaction.idempotencyKey == null) JSONObject.NULL else transaction.idempotencyKey)
                put("saleRecordId", if (transaction.saleRecordId == null) JSONObject.NULL else transaction.saleRecordId)
                put("dueDate", if (transaction.dueDate == null) JSONObject.NULL else transaction.dueDate)
                put("createdAt", transaction.createdAt)
                put("deletedAt", if (transaction.deletedAt == null) JSONObject.NULL else transaction.deletedAt)
            })
        }.toString()

        offlineQueueRepository.enqueue(
            OfflineQueueEntity(
                storeId = transaction.storeId,
                actionType = "khata_entry",
                idempotencyKey = transaction.id,
                payload = payload
            )
        )
        syncScheduler.triggerImmediateSync()
    }

    override fun getTransactionsForCustomerFlow(customerId: String): Flow<List<KhataTransactionEntity>> =
        khataDao.getTransactionsForCustomerFlow(customerId)

    override suspend fun getTransactionsForCustomerPaged(
        customerId: String,
        limit: Int,
        offset: Int
    ): List<KhataTransactionEntity> =
        withContext(Dispatchers.IO) {
            khataDao.getTransactionsForCustomerPaged(customerId, limit, offset)
        }

    override suspend fun syncPendingKhata(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pending = offlineQueueRepository.getPendingActions()
                val khataTasks = pending.filter { it.actionType == "khata_entry" }
                Log.i(TAG, "Found ${khataTasks.size} pending offline khata actions.")

                for (action in khataTasks) {
                    try {
                        val json = JSONObject(action.payload)
                        val entryType = json.getString("entryType")
                        val postgrest = supabaseClient.postgrest

                        if (entryType == "customer") {
                            val custJson = json.getJSONObject("customer")
                            val customer = KhataCustomerEntity(
                                id = custJson.getString("id"),
                                storeId = custJson.getString("storeId"),
                                name = custJson.getString("name"),
                                phone = if (custJson.isNull("phone")) null else custJson.getString("phone"),
                                email = if (custJson.isNull("email")) null else custJson.getString("email"),
                                notes = if (custJson.isNull("notes")) null else custJson.getString("notes"),
                                runningBalance = custJson.getDouble("runningBalance"),
                                lastActivity = if (custJson.isNull("lastActivity")) null else custJson.getLong("lastActivity"),
                                createdAt = custJson.getLong("createdAt"),
                                updatedAt = custJson.getLong("updatedAt"),
                                deletedAt = if (custJson.isNull("deletedAt")) null else custJson.getLong("deletedAt")
                            )
                            val dto = customer.toKhataCustomerDto()
                            postgrest["khata_customers"].upsert(dto)
                        } else if (entryType == "transaction") {
                            val custJson = json.getJSONObject("customer")
                            val customer = KhataCustomerEntity(
                                id = custJson.getString("id"),
                                storeId = custJson.getString("storeId"),
                                name = custJson.getString("name"),
                                phone = if (custJson.isNull("phone")) null else custJson.getString("phone"),
                                email = if (custJson.isNull("email")) null else custJson.getString("email"),
                                notes = if (custJson.isNull("notes")) null else custJson.getString("notes"),
                                runningBalance = custJson.getDouble("runningBalance"),
                                lastActivity = if (custJson.isNull("lastActivity")) null else custJson.getLong("lastActivity"),
                                createdAt = custJson.getLong("createdAt"),
                                updatedAt = custJson.getLong("updatedAt"),
                                deletedAt = if (custJson.isNull("deletedAt")) null else custJson.getLong("deletedAt")
                            )

                            val txJson = json.getJSONObject("transaction")
                            val transaction = KhataTransactionEntity(
                                id = txJson.getString("id"),
                                storeId = txJson.getString("storeId"),
                                customerId = txJson.getString("customerId"),
                                txType = txJson.getString("txType"),
                                amount = txJson.getDouble("amount"),
                                notes = if (txJson.isNull("notes")) null else txJson.getString("notes"),
                                nlpIntent = if (txJson.isNull("nlpIntent")) null else txJson.getString("nlpIntent"),
                                nlpConfidence = if (txJson.isNull("nlpConfidence")) null else txJson.getDouble("nlpConfidence"),
                                rawInput = if (txJson.isNull("rawInput")) null else txJson.getString("rawInput"),
                                idempotencyKey = if (txJson.isNull("idempotencyKey")) null else txJson.getString("idempotencyKey"),
                                saleRecordId = if (txJson.isNull("saleRecordId")) null else txJson.getString("saleRecordId"),
                                dueDate = if (txJson.isNull("dueDate")) null else txJson.getLong("dueDate"),
                                createdAt = txJson.getLong("createdAt"),
                                deletedAt = if (txJson.isNull("deletedAt")) null else txJson.getLong("deletedAt")
                            )

                            val customerDto = customer.toKhataCustomerDto()
                            val transactionDto = transaction.toKhataTransactionDto()

                            postgrest["khata_customers"].upsert(customerDto)
                            postgrest["khata_transactions"].upsert(transactionDto)
                        } else if (entryType == "delete_customer") {
                            val idToDelete = json.getString("customerId")
                            postgrest["khata_customers"].delete {
                                filter {
                                    eq("id", idToDelete)
                                }
                            }
                        }

                        offlineQueueRepository.markCompleted(action.id)
                        Log.i(TAG, "Successfully synced queued khata action: ${action.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync queued khata action ${action.id}: ${e.message}")
                        offlineQueueRepository.markFailed(action.id, e.message ?: "Unknown sync error")
                        if (isNetworkError(e)) {
                            throw e
                        }
                    }
                }
            }
        }

    override suspend fun fetchKhataFromRemote(storeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val postgrest = supabaseClient.postgrest

                // 1. Fetch remote customers
                val customerResponse = postgrest["khata_customers"].select {
                    filter { eq("store_id", storeId) }
                }
                val remoteCustomers = if (customerResponse.data != "[]" && customerResponse.data.isNotBlank()) {
                    customerResponse.decodeList<KhataCustomerDto>()
                } else emptyList()

                // 2. Fetch remote transactions
                val transactionResponse = postgrest["khata_transactions"].select {
                    filter { eq("store_id", storeId) }
                }
                val remoteTransactions = if (transactionResponse.data != "[]" && transactionResponse.data.isNotBlank()) {
                    transactionResponse.decodeList<KhataTransactionDto>()
                } else emptyList()

                // 3. Write to local Room tables in transaction
                appDatabase.withTransaction {
                    khataDao.clearAllCustomers()
                    khataDao.clearAllTransactions()

                    kiranaDao.clearAllCustomers()
                    kiranaDao.clearAllTransactions()

                    remoteCustomers.forEach { dto ->
                        val txsForCust = remoteTransactions.filter { it.customerId == dto.id }
                        val calculatedBalance = txsForCust.sumOf { tx ->
                            when (tx.txType) {
                                "debit" -> tx.amount
                                "credit", "reversal" -> -tx.amount
                                else -> 0.0
                            }
                        }

                        val entity = dto.toEntity().copy(runningBalance = calculatedBalance)
                        khataDao.insertCustomer(entity)

                        val legacyCustomer = CustomerEntity(
                            id = dto.id.hashCode(),
                            name = dto.name,
                            phone = dto.phone ?: "",
                            balance = calculatedBalance,
                            lastTransaction = dto.lastActivity?.let { parseIsoToMillis(it) } ?: parseIsoToMillis(dto.createdAt)
                        )
                        kiranaDao.insertCustomer(legacyCustomer)
                    }

                    remoteTransactions.forEach { dto ->
                        val entity = dto.toEntity()
                        khataDao.insertTransaction(entity)

                        val customer = remoteCustomers.firstOrNull { it.id == dto.customerId }
                        if (customer != null) {
                            val runningBalance = customer.runningBalance
                            val legacyTx = TransactionEntity(
                                id = dto.id.hashCode(),
                                customerId = dto.customerId.hashCode(),
                                type = dto.txType,
                                amount = dto.amount,
                                balanceAfter = runningBalance,
                                rawInput = dto.rawInput ?: (dto.notes ?: ""),
                                date = parseIsoToMillis(dto.createdAt),
                                dueDate = null,
                                isSettled = dto.notes == "Account Settled"
                            )
                            kiranaDao.insertTransaction(legacyTx)
                        }
                    }
                }
                Log.i(TAG, "Successfully downloaded and cached ${remoteCustomers.size} customers and ${remoteTransactions.size} transactions.")
                Unit
            }
        }

    override suspend fun deleteCustomer(customerId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val customer = khataDao.getCustomerById(customerId)
                if (customer == null) {
                    Log.w(TAG, "Customer not found in local DB for deletion: $customerId")
                    return@runCatching
                }
                val storeId = customer.storeId

                // 1. Soft delete customer locally
                khataDao.softDeleteCustomer(customerId, System.currentTimeMillis())
                // 2. Soft delete their transactions locally
                khataDao.softDeleteTransactionsForCustomer(customerId, System.currentTimeMillis())

                // 3. Try to delete from remote Supabase
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["khata_customers"].delete {
                        filter {
                            eq("id", customerId)
                        }
                    }
                    Log.i(TAG, "Customer deleted from Supabase: $customerId")
                } catch (e: Exception) {
                    if (isNetworkError(e)) {
                        Log.w(TAG, "Supabase delete failed due to network, queueing offline: ${e.message}")
                        val payload = JSONObject().apply {
                            put("entryType", "delete_customer")
                            put("customerId", customerId)
                        }.toString()
                        offlineQueueRepository.enqueue(
                            OfflineQueueEntity(
                                storeId = storeId,
                                actionType = "khata_entry",
                                idempotencyKey = customerId,
                                payload = payload
                            )
                        )
                        syncScheduler.triggerImmediateSync()
                    } else {
                        Log.e(TAG, "Permanent database/RLS error during customer delete: ${e.message}", e)
                        throw e
                    }
                }
                Unit
            }
        }

    private fun isNetworkError(e: Throwable): Boolean {
        val name = e.javaClass.name.lowercase()
        val msg = e.message?.lowercase() ?: ""
        return e is java.io.IOException || 
               name.contains("ktor") || 
               name.contains("socket") || 
               name.contains("httprequest") || 
               msg.contains("timeout") || 
               msg.contains("connection") || 
               msg.contains("host") || 
               msg.contains("address")
    }
}
