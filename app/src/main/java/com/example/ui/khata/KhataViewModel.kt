package com.example.ui.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.KhataCustomerEntity
import com.example.data.model.KhataTransactionEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.KhataRepository
import com.example.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class KhataUiState(
    val customers: List<KhataCustomerEntity> = emptyList(),
    val filteredCustomers: List<KhataCustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: String = "All", // "All", "Get" (balance > 0), "Give" (balance < 0)
    val sortingOption: String = "High Balance First", // "High Balance First", "Low Balance First", "Name A-Z", "Name Z-A", "Last Active"
    val selectedCustomer: KhataCustomerEntity? = null,
    val selectedCustomerTransactions: List<KhataTransactionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class KhataViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val khataRepository: KhataRepository,
    private val kiranaDao: com.example.data.dao.KiranaDao,
    private val syncScheduler: com.example.sync.SyncScheduler? = null
) : ViewModel() {

    companion object {
        private inline fun <reified T : Any> createFake(): T {
            return java.lang.reflect.Proxy.newProxyInstance(
                T::class.java.classLoader,
                arrayOf(T::class.java)
            ) { _, method, _ ->
                val returnType = method.returnType
                val genericReturnType = method.genericReturnType
                val genericString = genericReturnType.toString()
                if (returnType.isAssignableFrom(kotlinx.coroutines.flow.Flow::class.java) ||
                    returnType.isAssignableFrom(kotlinx.coroutines.flow.StateFlow::class.java)) {
                    if (genericString.contains("Int") || genericString.contains("Integer")) {
                        kotlinx.coroutines.flow.MutableStateFlow(0)
                    } else if (genericString.contains("Boolean")) {
                        kotlinx.coroutines.flow.MutableStateFlow(false)
                    } else {
                        kotlinx.coroutines.flow.MutableStateFlow(emptyList<Any>())
                    }
                } else if (returnType == Result::class.java) {
                    Result.success(Unit)
                } else if (returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.java) {
                    false
                } else if (returnType == Int::class.javaPrimitiveType || returnType == Int::class.java) {
                    0
                } else if (returnType == Long::class.javaPrimitiveType || returnType == Long::class.java) {
                    0L
                } else if (returnType == List::class.java) {
                    emptyList<Any>()
                } else {
                    null
                }
            } as T
        }
    }

    constructor(application: android.app.Application) : this(
        authRepository = createFake(),
        profileRepository = createFake(),
        khataRepository = createFake(),
        kiranaDao = createFake(),
        syncScheduler = null
    )


    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow("All")
    private val _sortingOption = MutableStateFlow("High Balance First")
    
    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // Flow of selected customer details
    val selectedCustomer: Flow<KhataCustomerEntity?> = combine(
        _selectedCustomerId,
        khataRepository.allCustomers
    ) { id, allCustomers ->
        if (id == null) {
            null
        } else {
            var found = allCustomers.find { it.id == id || it.id.hashCode().toString() == id }
            if (found == null) {
                val legacyId = id.toIntOrNull()
                if (legacyId != null) {
                    val legacyCust = kiranaDao.getCustomerById(legacyId)
                    if (legacyCust != null) {
                        found = allCustomers.find { it.name.equals(legacyCust.name, ignoreCase = true) }
                    }
                }
            }
            found
        }
    }

    // Flow of selected customer transactions
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCustomerTransactions: Flow<List<KhataTransactionEntity>> = selectedCustomer
        .flatMapLatest { customer ->
            if (customer == null) flowOf(emptyList())
            else khataRepository.getTransactionsForCustomerFlow(customer.id)
        }

    // Flow of sorted and filtered customers list
    private val _filteredCustomersFlow: Flow<List<KhataCustomerEntity>> = combine(
        khataRepository.allCustomers,
        _searchQuery,
        _activeFilter,
        _sortingOption
    ) { customerList, query, filter, sort ->
        var list = customerList

        // 1. Search filter
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.phone?.contains(query) ?: false)
            }
        }

        // 2. Chip filter
        list = when (filter) {
            "Get" -> list.filter { it.runningBalance > 0 }
            "Give" -> list.filter { it.runningBalance < 0 }
            else -> list
        }

        // 3. Sorting
        list = when (sort) {
            "High Balance First" -> list.sortedByDescending { it.runningBalance }
            "Low Balance First" -> list.sortedBy { it.runningBalance }
            "Name A-Z" -> list.sortedBy { it.name }
            "Name Z-A" -> list.sortedByDescending { it.name }
            "Last Active" -> list.sortedByDescending { it.lastActivity ?: 0L }
            else -> list
        }

        list
    }

    // Combined screen UI state
    val uiState: StateFlow<KhataUiState> = combine(
        khataRepository.allCustomers,
        _filteredCustomersFlow,
        _searchQuery,
        _activeFilter,
        _sortingOption,
        selectedCustomer,
        selectedCustomerTransactions,
        _isLoading,
        _errorMessage,
        _successMessage
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        KhataUiState(
            customers = flows[0] as List<KhataCustomerEntity>,
            filteredCustomers = flows[1] as List<KhataCustomerEntity>,
            searchQuery = flows[2] as String,
            activeFilter = flows[3] as String,
            sortingOption = flows[4] as String,
            selectedCustomer = flows[5] as? KhataCustomerEntity,
            selectedCustomerTransactions = flows[6] as List<KhataTransactionEntity>,
            isLoading = flows[7] as Boolean,
            errorMessage = flows[8] as? String,
            successMessage = flows[9] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KhataUiState()
    )

    // ── Controls ─────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: String) {
        _activeFilter.value = filter
    }

    fun onSortingChanged(sort: String) {
        _sortingOption.value = sort
    }

    fun setSelectedCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
        if (customerId != null) {
            viewModelScope.launch {
                val legacyId = customerId.toIntOrNull()
                if (legacyId != null) {
                    val legacyCust = kiranaDao.getCustomerById(legacyId)
                    if (legacyCust != null) {
                        // Use a DB-level query to check existence, avoiding race condition
                        // where allCustomers.first() returned empty list before first emission.
                        val existingKhataCust = khataRepository.getCustomerByName(legacyCust.name)
                        val alreadyExists = existingKhataCust != null

                        if (!alreadyExists) {
                            val currentUser = authRepository.getCurrentUser()
                            val profile = currentUser?.let { profileRepository.getProfile(it.id).getOrNull() }
                            val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

                            val newUuid = UUID.randomUUID().toString()
                            // CRITICAL FIX: Start with runningBalance = 0.0, NOT legacyCust.balance.
                            // khataRepository.addTransaction() recalculates balance from the current
                            // value on each call. If we set runningBalance = 5500 first and then
                            // replay the same transactions (+7000, -1500), we'd get 5500+7000-1500=11000.
                            // Starting from 0 and replaying gives the correct 0+7000-1500=5500.
                            val newKhataCustomer = KhataCustomerEntity(
                                id = newUuid,
                                storeId = storeId,
                                name = legacyCust.name,
                                phone = if (legacyCust.phone.isEmpty()) null else legacyCust.phone,
                                notes = "Migrated from legacy database",
                                runningBalance = 0.0,
                                lastActivity = legacyCust.lastTransaction,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            khataRepository.insertCustomer(newKhataCustomer)

                            val legacyTxs = kiranaDao.getTransactionsForCustomer(legacyId).first()
                            for (tx in legacyTxs) {
                                val khataTx = KhataTransactionEntity(
                                    id = UUID.randomUUID().toString(),
                                    storeId = storeId,
                                    customerId = newUuid,
                                    txType = tx.type,
                                    amount = tx.amount,
                                    notes = if (tx.rawInput.isEmpty()) null else tx.rawInput,
                                    rawInput = tx.rawInput,
                                    createdAt = tx.date,
                                    dueDate = tx.dueDate
                                )
                                khataRepository.addTransaction(khataTx)
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun createCustomer(
        name: String,
        phone: String,
        email: String?,
        openingBalance: Double = 0.0,
        notes: String?,
        onSuccess: (String) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Customer name is required"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "Session expired. Please log in again."
                    return@launch
                }
                val profile = profileRepository.getProfile(currentUser.id).getOrNull()
                val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

                val customerId = UUID.randomUUID().toString()
                val newCustomer = KhataCustomerEntity(
                    id = customerId,
                    storeId = storeId,
                    name = name.trim(),
                    phone = phone.trim().ifEmpty { null },
                    email = email?.trim()?.ifEmpty { null },
                    notes = notes?.trim()?.ifEmpty { null },
                    runningBalance = 0.0,
                    lastActivity = System.currentTimeMillis()
                )

                khataRepository.insertCustomer(newCustomer).onSuccess {
                    val legacyCustomer = CustomerEntity(
                        name = newCustomer.name,
                        phone = newCustomer.phone ?: "+91 90000 00000",
                        balance = newCustomer.runningBalance,
                        lastTransaction = newCustomer.lastActivity ?: System.currentTimeMillis()
                    )
                    var insertedLegacyId = -1L
                    try {
                        insertedLegacyId = kiranaDao.insertCustomer(legacyCustomer)
                    } catch (e: Exception) {
                        android.util.Log.e("KhataViewModel", "Failed to sync legacy customer", e)
                    }

                    if (openingBalance != 0.0) {
                        val type = if (openingBalance > 0.0) "debit" else "credit"
                        val absAmount = kotlin.math.abs(openingBalance)
                        val openingTx = KhataTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = storeId,
                            customerId = customerId,
                            txType = type,
                            amount = absAmount,
                            notes = "Opening ledger balance",
                            dueDate = null
                        )
                        khataRepository.addTransaction(openingTx).onSuccess {
                            if (insertedLegacyId != -1L) {
                                try {
                                    kiranaDao.insertTransaction(
                                        TransactionEntity(
                                            customerId = insertedLegacyId.toInt(),
                                            type = type,
                                            amount = absAmount,
                                            balanceAfter = openingBalance,
                                            rawInput = "Opening ledger balance",
                                            date = System.currentTimeMillis(),
                                            dueDate = null
                                        )
                                    )
                                    val legacyCust = kiranaDao.getCustomerById(insertedLegacyId.toInt())
                                    if (legacyCust != null) {
                                        kiranaDao.updateCustomer(
                                            legacyCust.copy(
                                                balance = openingBalance,
                                                lastTransaction = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("KhataViewModel", "Failed to sync legacy opening transaction", e)
                                }
                            }
                        }.onFailure { error ->
                            android.util.Log.e("KhataViewModel", "Failed to record local opening transaction: ${error.message}")
                        }
                    }

                    _successMessage.value = "Customer ledger created!"
                    onSuccess(customerId)
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to add customer"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error creating customer"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTransaction(
        customerId: String,
        type: String, // "debit" (Maine Diya) or "credit" (Maine Mila)
        amount: Double,
        notes: String?,
        dueDate: Long? = null
    ) {
        if (amount <= 0) {
            _errorMessage.value = "Amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "Session expired. Please log in again."
                    return@launch
                }
                val profile = profileRepository.getProfile(currentUser.id).getOrNull()
                val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

                val transaction = KhataTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = storeId,
                    customerId = customerId,
                    txType = type,
                    amount = amount,
                    notes = notes?.trim()?.ifEmpty { null },
                    dueDate = dueDate
                )

                khataRepository.addTransaction(transaction).onSuccess {
                    // Dual write to legacy database
                    val khataCust = khataRepository.getCustomerById(customerId)
                    if (khataCust != null) {
                        val legacyCust = kiranaDao.getCustomerByName(khataCust.name)
                        if (legacyCust != null) {
                            val newBalance = if (type == "debit") {
                                legacyCust.balance + amount
                            } else {
                                legacyCust.balance - amount
                            }
                            
                            try {
                                kiranaDao.insertTransaction(
                                    TransactionEntity(
                                        customerId = legacyCust.id,
                                        type = type,
                                        amount = amount,
                                        balanceAfter = newBalance,
                                        rawInput = notes ?: "",
                                        date = System.currentTimeMillis(),
                                        dueDate = dueDate
                                    )
                                )
                                kiranaDao.updateCustomer(
                                    legacyCust.copy(
                                        balance = newBalance,
                                        lastTransaction = System.currentTimeMillis()
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("KhataViewModel", "Failed to sync legacy transaction", e)
                            }
                        }
                    }
                    _successMessage.value = "Ledger updated successfully!"
                    syncScheduler?.triggerImmediateInventoryCheck()
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to record transaction"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error recording transaction"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun settleCustomer(customerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val customer = khataRepository.getCustomerById(customerId)
                if (customer == null) {
                    _errorMessage.value = "Customer not found"
                    return@launch
                }
                val currentBalance = customer.runningBalance
                if (currentBalance == 0.0) {
                    _successMessage.value = "Balance is already settled"
                    return@launch
                }

                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "Session expired"
                    return@launch
                }
                val profile = profileRepository.getProfile(currentUser.id).getOrNull()
                val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

                // To settle: if balance is positive (we get money), customer settles by paying us (credit)
                // If balance is negative (we owe money), we settle by paying customer (debit)
                val type = if (currentBalance > 0) "credit" else "debit"
                val transaction = KhataTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = storeId,
                    customerId = customerId,
                    txType = type,
                    amount = kotlin.math.abs(currentBalance),
                    notes = "Account Settle Adjustment"
                )

                khataRepository.addTransaction(transaction).onSuccess {
                    // Dual write to legacy database for settlement
                    val khataCust = khataRepository.getCustomerById(customerId)
                    if (khataCust != null) {
                        val legacyCust = kiranaDao.getCustomerByName(khataCust.name)
                        if (legacyCust != null) {
                            try {
                                kiranaDao.settleAllTransactionsForCustomer(legacyCust.id)
                                
                                val settlementTx = TransactionEntity(
                                    customerId = legacyCust.id,
                                    type = if (legacyCust.balance >= 0) "credit" else "debit",
                                    amount = kotlin.math.abs(legacyCust.balance),
                                    balanceAfter = 0.0,
                                    rawInput = "Account Settled",
                                    date = System.currentTimeMillis(),
                                    isSettled = true
                                )
                                kiranaDao.insertTransaction(settlementTx)
                                
                                kiranaDao.updateCustomer(
                                    legacyCust.copy(
                                        balance = 0.0,
                                        lastTransaction = System.currentTimeMillis()
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("KhataViewModel", "Failed to sync legacy settlement", e)
                            }
                        }
                    }
                    _successMessage.value = "Ledger account settled to ₹0.00"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to settle ledger"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error settling account"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val customer = khataRepository.getCustomerById(customerId)
                if (customer == null) {
                    _errorMessage.value = "Customer not found"
                    return@launch
                }
                khataRepository.deleteCustomer(customerId).onSuccess {
                    // Dual write to legacy database for deletion
                    val legacyCust = kiranaDao.getCustomerByName(customer.name)
                    if (legacyCust != null) {
                        try {
                            kiranaDao.deleteCustomerById(legacyCust.id)
                            kiranaDao.deleteTransactionsByCustomerId(legacyCust.id)
                        } catch (e: Exception) {
                            android.util.Log.e("KhataViewModel", "Failed to sync legacy deletion", e)
                        }
                    }
                    _successMessage.value = "Customer ledger deleted successfully!"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to delete customer ledger"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error deleting customer"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
