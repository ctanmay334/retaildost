package com.example.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.InventoryEntity
import com.example.data.model.KhataCustomerEntity
import com.example.data.model.SaleRecordEntity
import com.example.data.model.SaleRecordItemEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.KhataRepository
import com.example.data.repository.ProfileRepository
import com.example.domain.sale.CreateSaleUseCase
import com.example.domain.sale.GetSalesHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartItem(
    val product: InventoryEntity,
    val quantity: Double
) {
    val subtotal: Double
        get() = (product.mrp ?: 0.0) * quantity
}

data class SaleUiState(
    val cart: List<CartItem> = emptyList(),
    val catalog: List<InventoryEntity> = emptyList(),
    val customers: List<KhataCustomerEntity> = emptyList(),
    val filteredCustomers: List<KhataCustomerEntity> = emptyList(),
    val selectedCustomer: KhataCustomerEntity? = null,
    val paymentMode: String = "Cash", // "Cash" or "Credit"
    val notes: String = "",
    val salesHistory: List<SaleRecordEntity> = emptyList(),
    val catalogSearchQuery: String = "",
    val customerSearchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val inventoryRepository: InventoryRepository,
    private val khataRepository: KhataRepository,
    private val createSaleUseCase: CreateSaleUseCase,
    private val getSalesHistoryUseCase: GetSalesHistoryUseCase
) : ViewModel() {

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    private val _catalogSearchQuery = MutableStateFlow("")
    private val _customerSearchQuery = MutableStateFlow("")
    private val _selectedCustomer = MutableStateFlow<KhataCustomerEntity?>(null)
    private val _paymentMode = MutableStateFlow("Cash")
    private val _notes = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // Flow of the search results from the catalog
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _catalogFlow: Flow<List<InventoryEntity>> = _catalogSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                inventoryRepository.allItems
            } else {
                inventoryRepository.searchItems(query)
            }
        }

    // Flow of filtered customers
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _filteredCustomersFlow: Flow<List<KhataCustomerEntity>> = combine(
        khataRepository.allCustomers,
        _customerSearchQuery
    ) { customerList, query ->
        if (query.isBlank()) {
            customerList
        } else {
            customerList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.phone?.contains(query) ?: false)
            }
        }
    }

    // Expose combined UI state
    val uiState: StateFlow<SaleUiState> = combine(
        _cart,
        _catalogFlow,
        khataRepository.allCustomers,
        _filteredCustomersFlow,
        _selectedCustomer,
        _paymentMode,
        _notes,
        getSalesHistoryUseCase(),
        _catalogSearchQuery,
        _customerSearchQuery,
        _isLoading,
        _errorMessage,
        _successMessage
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        SaleUiState(
            cart = flows[0] as List<CartItem>,
            catalog = flows[1] as List<InventoryEntity>,
            customers = flows[2] as List<KhataCustomerEntity>,
            filteredCustomers = flows[3] as List<KhataCustomerEntity>,
            selectedCustomer = flows[4] as? KhataCustomerEntity,
            paymentMode = flows[5] as String,
            notes = flows[6] as String,
            salesHistory = flows[7] as List<SaleRecordEntity>,
            catalogSearchQuery = flows[8] as String,
            customerSearchQuery = flows[9] as String,
            isLoading = flows[10] as Boolean,
            errorMessage = flows[11] as? String,
            successMessage = flows[12] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SaleUiState()
    )

    // ── Cart State Actions ───────────────────────────────────────────────────

    fun addToCart(product: InventoryEntity, quantity: Double) {
        if (quantity <= 0) return
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(product, quantity))
        }
        _cart.value = currentList
    }

    fun updateCartItemQuantity(productId: String, quantity: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = currentList[index].copy(quantity = quantity)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
        _paymentMode.value = "Cash"
        _notes.value = ""
        _customerSearchQuery.value = ""
        _catalogSearchQuery.value = ""
    }

    // ── Form State Actions ───────────────────────────────────────────────────

    fun onCatalogSearchChanged(query: String) {
        _catalogSearchQuery.value = query
    }

    fun onCustomerSearchChanged(query: String) {
        _customerSearchQuery.value = query
    }

    fun selectCustomer(customer: KhataCustomerEntity?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            _paymentMode.value = "Credit"
        }
    }

    fun setPaymentMode(mode: String) {
        _paymentMode.value = mode
        if (mode == "Cash") {
            _selectedCustomer.value = null
        }
    }

    fun setNotes(notesText: String) {
        _notes.value = notesText
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ── Checkout / Checkout Execution ────────────────────────────────────────

    fun checkoutSale(onSuccess: () -> Unit) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            _errorMessage.value = "Cart is empty"
            return
        }

        val mode = _paymentMode.value
        val customer = _selectedCustomer.value
        if (mode == "Credit" && customer == null) {
            _errorMessage.value = "Please select a customer for Udhaar/Credit sale"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "User session expired. Please log in again."
                    _isLoading.value = false
                    return@launch
                }

                // Retrieve user profile to resolve store ID
                val profile = profileRepository.getProfile(currentUser.id).getOrNull()
                val storeId = profile?.storeId ?: "00000000-0000-0000-0000-000000000000"

                val total = cartItems.sumOf { it.subtotal }
                
                // Map CartItems to SaleRecordItemEntity structures
                val saleItems = cartItems.map { cart ->
                    SaleRecordItemEntity(
                        storeId = storeId,
                        saleRecordId = "", // set by UseCase
                        inventoryId = cart.product.id,
                        itemName = cart.product.itemName,
                        unitLabel = cart.product.unitLabel,
                        quantitySold = cart.quantity,
                        salePrice = cart.product.mrp ?: 0.0,
                        costPrice = cart.product.costPrice
                    )
                }

                createSaleUseCase(
                    storeId = storeId,
                    customerName = customer?.name,
                    customerId = customer?.id,
                    paymentMode = mode,
                    totalAmount = total,
                    notes = _notes.value,
                    items = saleItems
                ).onSuccess {
                    _successMessage.value = "Sale recorded successfully!"
                    clearCart()
                    onSuccess()
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to checkout sale"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred during checkout"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
