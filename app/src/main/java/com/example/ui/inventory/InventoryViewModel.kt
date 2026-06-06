package com.example.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.InventoryEntity
import com.example.data.repository.ProfileRepository
import com.example.domain.inventory.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * InventoryUiState
 * ────────────────
 * Represents the presentation state of the inventory catalog screen.
 */
data class InventoryUiState(
    val items: List<InventoryEntity> = emptyList(),
    val categories: List<String> = listOf("All", "Staples", "Dairy", "Snacks", "Personal Care", "Cleaning"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val showLowStockOnly: Boolean = false,
    val showOutOfStockOnly: Boolean = false,
    val showExpiringSoonOnly: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val inventoryRepository: com.example.data.repository.InventoryRepository,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val addInventoryUseCase: AddInventoryUseCase,
    private val updateInventoryUseCase: UpdateInventoryUseCase,
    private val deleteInventoryUseCase: DeleteInventoryUseCase,
    private val decrementStockUseCase: DecrementStockUseCase
) : ViewModel() {

    fun filterById(id: String) {
        viewModelScope.launch {
            val item = inventoryRepository.getItemById(id)
            if (item != null) {
                _searchQuery.value = item.itemName
            }
        }
    }

    // Filter flows backing the reactive catalog query
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _showLowStockOnly = MutableStateFlow(false)
    private val _showOutOfStockOnly = MutableStateFlow(false)
    private val _showExpiringSoonOnly = MutableStateFlow(false)

    val allItems: StateFlow<List<InventoryEntity>> = inventoryRepository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Spinner and popup states
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // Combine filters and invoke GetInventoryUseCase reactively (Flow support)
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InventoryUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _showLowStockOnly,
        _showOutOfStockOnly,
        _showExpiringSoonOnly,
        _isLoading,
        _errorMessage,
        _successMessage
    ) { args ->
        val query = args[0] as String
        val cat = args[1] as String
        val low = args[2] as Boolean
        val outOfStock = args[3] as Boolean
        val exp = args[4] as Boolean
        val loading = args[5] as Boolean
        val err = args[6] as String?
        val success = args[7] as String?
        Tuple(query, cat, low, outOfStock, exp, loading, err, success)
    }.flatMapLatest { tuple ->
        getInventoryUseCase(
            searchQuery = tuple.query,
            categoryFilter = tuple.cat,
            showLowStockOnly = tuple.low,
            showOutOfStockOnly = tuple.outOfStock,
            showExpiringSoonOnly = tuple.exp
        ).map { itemsList ->
            InventoryUiState(
                items = itemsList,
                searchQuery = tuple.query,
                selectedCategory = tuple.cat,
                showLowStockOnly = tuple.low,
                showOutOfStockOnly = tuple.outOfStock,
                showExpiringSoonOnly = tuple.exp,
                isLoading = tuple.loading,
                errorMessage = tuple.err,
                successMessage = tuple.success
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    // ── Filter Controls ──────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun toggleLowStockFilter() {
        if (!_showLowStockOnly.value) {
            _showExpiringSoonOnly.value = false
            _showOutOfStockOnly.value = false
        }
        _showLowStockOnly.value = !_showLowStockOnly.value
    }

    fun toggleExpiryFilter() {
        if (!_showExpiringSoonOnly.value) {
            _showLowStockOnly.value = false
            _showOutOfStockOnly.value = false
        }
        _showExpiringSoonOnly.value = !_showExpiringSoonOnly.value
    }

    fun toggleOutOfStockFilter() {
        if (!_showOutOfStockOnly.value) {
            _showLowStockOnly.value = false
            _showExpiringSoonOnly.value = false
        }
        _showOutOfStockOnly.value = !_showOutOfStockOnly.value
    }

    fun clearAllFilters() {
        _showLowStockOnly.value = false
        _showExpiringSoonOnly.value = false
        _showOutOfStockOnly.value = false
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ── CRUD Actions (Optimistic Updates) ────────────────────────────────────

    /**
     * Creates a new product. Updates Room database immediately,
     * allowing the state flow to emit instantly (optimistic update),
     * and syncs with Supabase in the background.
     */
    fun addItem(
        itemName: String,
        category: String?,
        unitLabel: String?,
        quantity: Double,
        minThreshold: Double,
        costPrice: Double?,
        mrp: Double?,
        batchNo: String?,
        expiryDate: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _isLoading.value = false
                _errorMessage.value = "User session expired. Please log in again."
                return@launch
            }

            // Resolve cached store ID
            val profile = profileRepository.getProfile(currentUser.id).getOrNull()
            val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

            addInventoryUseCase(
                storeId = storeId,
                itemName = itemName,
                category = category,
                unitLabel = unitLabel,
                quantity = quantity,
                minThreshold = minThreshold,
                costPrice = costPrice,
                mrp = mrp,
                batchNo = batchNo,
                expiryDate = expiryDate
            ).onSuccess {
                _isLoading.value = false
                _successMessage.value = "Product added successfully!"
            }.onFailure { error ->
                _isLoading.value = false
                _errorMessage.value = error.message ?: "Failed to add product"
            }
        }
    }

    fun updateItem(item: InventoryEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            updateInventoryUseCase(item)
                .onSuccess {
                    _isLoading.value = false
                    _successMessage.value = "Product updated successfully!"
                }.onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to update product"
                }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            deleteInventoryUseCase(id)
                .onSuccess {
                    _isLoading.value = false
                    _successMessage.value = "Product deleted successfully"
                }.onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to delete product"
                }
        }
    }

    fun decrementStock(id: String, count: Double) {
        viewModelScope.launch {
            decrementStockUseCase(id, count)
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to decrement stock level"
                }
        }
    }

    fun incrementStock(id: String, count: Double) {
        viewModelScope.launch {
            val item = inventoryRepository.getItemById(id)
            if (item != null) {
                val updated = item.copy(
                    quantity = item.quantity + count,
                    updatedAt = System.currentTimeMillis()
                )
                inventoryRepository.insertItem(updated)
                    .onFailure { error ->
                        _errorMessage.value = error.message ?: "Failed to increment stock level"
                    }
            } else {
                _errorMessage.value = "Product not found"
            }
        }
    }


    private data class Tuple(
        val query: String,
        val cat: String,
        val low: Boolean,
        val outOfStock: Boolean,
        val exp: Boolean,
        val loading: Boolean,
        val err: String?,
        val success: String?
    )
}
