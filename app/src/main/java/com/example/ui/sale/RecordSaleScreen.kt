package com.example.ui.sale

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.data.model.InventoryEntity
import com.example.ui.sale.SaleViewModel
import com.example.ui.sale.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSaleScreen(
    onNavigateBack: () -> Unit,
    viewModel: SaleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val primaryNavy = Color(0xFF0B1A7D)
    val textDark = Color(0xFF0F172A)
    val textMuted = Color(0xFF64748B)
    val dividerColor = Color(0xFFE2E8F0)

    var discountPercent by remember { mutableStateOf(0.0) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showInsufficientStockDialog by remember { mutableStateOf(false) }
    var showSuccessFeedback by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "New Sale",
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            fontSize = 20.sp
                        )
                        val totalItems = uiState.cart.sumOf { it.quantity }.toInt()
                        Text(
                            text = "$totalItems items added",
                            color = textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textDark)
                    }
                },
                actions = {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUMy1G-7L3chtuF4TiMsGQ40MV12V3MVqwWwH6dSwzdrt1tJBDtQhQGA73OdTY_jdunP6LDdm3_MZV_7djDx7H5i2wmkvFALCGB656VykXhvDQBC38HP_Gf2su70SepAL-7VN-JyPHy9-GTrW-888dNe8n1IeHJcW1R_5girplZTDp3NWTEOfpwwbbymc9NKmgo_6Di0AYy75qhRaxXaEINJ2T7d0Zi8KmQ1_Mp12S-x1gql2wTMPCLdT6lEfjL9CY9NW5JyJrVE8",
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F6FB))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp) // Leave space for bottom checkout card
            ) {
                // Top Barcode Button & Search Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Barcode scan button
                    Button(
                        onClick = {
                            Toast.makeText(context, "Scanning barcode...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scan_barcode_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scan Barcode",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search catalog bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = uiState.catalogSearchQuery,
                                onValueChange = viewModel::onCatalogSearchChanged,
                                textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1E293B)),
                                decorationBox = { innerTextField ->
                                    if (uiState.catalogSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search products (e.g. Atta, Milk)",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.catalogSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onCatalogSearchChanged("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    if (uiState.catalogSearchQuery.isNotEmpty()) {
                        // SEARCH RESULTS CATALOG VIEW
                        item {
                            Text(
                                text = "SEARCH RESULTS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = primaryNavy,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (uiState.catalog.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No products found in inventory.", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        } else {
                            items(uiState.catalog) { product ->
                                val cartItem = uiState.cart.find { it.product.id == product.id }
                                SearchResultProductCard(
                                    product = product,
                                    cartItem = cartItem,
                                    onAdd = { viewModel.addToCart(product, 1.0) },
                                    onIncrease = {
                                        val nextQty = (cartItem?.quantity ?: 0.0) + 1
                                        if (nextQty > product.quantity) {
                                            showInsufficientStockDialog = true
                                        } else {
                                            viewModel.updateCartItemQuantity(product.id, nextQty)
                                        }
                                    },
                                    onDecrease = {
                                        val nextQty = (cartItem?.quantity ?: 0.0) - 1
                                        viewModel.updateCartItemQuantity(product.id, nextQty)
                                    }
                                )
                            }
                        }
                    } else {
                        // STANDARD SALE VIEW (CART ITEMS LIST)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT ITEMS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryNavy,
                                    letterSpacing = 0.5.sp
                                )
                                if (uiState.cart.isNotEmpty()) {
                                    Text(
                                        text = "Clear All",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable { viewModel.clearCart() }
                                    )
                                }
                            }
                        }

                        if (uiState.cart.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Empty cart",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Your cart is empty.",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Search products above to add items to this sale.",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.cart) { cartItem ->
                                CartItemRedesignedCard(
                                    cartItem = cartItem,
                                    onIncrease = {
                                        val nextQty = cartItem.quantity + 1
                                        if (nextQty > cartItem.product.quantity) {
                                            showInsufficientStockDialog = true
                                        } else {
                                            viewModel.updateCartItemQuantity(cartItem.product.id, nextQty)
                                        }
                                    },
                                    onDecrease = {
                                        viewModel.updateCartItemQuantity(cartItem.product.id, cartItem.quantity - 1)
                                    }
                                )
                            }

                            // Payment mode selector & linked customer
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "PAYMENT MODE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = primaryNavy,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.setPaymentMode("Cash") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (uiState.paymentMode == "Cash") primaryNavy else Color(0xFFEFF2F6),
                                                contentColor = if (uiState.paymentMode == "Cash") Color.White else textDark
                                            )
                                        ) {
                                            Text("Cash Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.setPaymentMode("Credit") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (uiState.paymentMode == "Credit") primaryNavy else Color(0xFFEFF2F6),
                                                contentColor = if (uiState.paymentMode == "Credit") Color.White else textDark
                                            )
                                        ) {
                                            Text("Udhaar / Credit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    if (uiState.paymentMode == "Credit") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "LINK TO CUSTOMER",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = primaryNavy,
                                            letterSpacing = 0.5.sp
                                        )

                                        if (uiState.selectedCustomer != null) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = uiState.selectedCustomer!!.name,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1E3A8A),
                                                            fontSize = 14.sp
                                                        )
                                                        uiState.selectedCustomer!!.phone?.let {
                                                            Text(text = it, color = Color(0xFF3B82F6), fontSize = 12.sp)
                                                        }
                                                        Text(
                                                            text = "Outstanding: ₹${uiState.selectedCustomer!!.runningBalance.toInt()}",
                                                            color = Color(0xFFDC2626),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "Change",
                                                        color = Color(0xFF2563EB),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.clickable { viewModel.selectCustomer(null) }
                                                    )
                                                }
                                            }
                                        } else {
                                            // Customer search block
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(8.dp))
                                                    .background(Color.White, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    BasicTextField(
                                                        value = uiState.customerSearchQuery,
                                                        onValueChange = viewModel::onCustomerSearchChanged,
                                                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1E293B)),
                                                        decorationBox = { innerTextField ->
                                                            if (uiState.customerSearchQuery.isEmpty()) {
                                                                Text(
                                                                    text = "Search customer by name or phone...",
                                                                    color = Color(0xFF94A3B8),
                                                                    fontSize = 14.sp
                                                                )
                                                            }
                                                            innerTextField()
                                                        },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                if (uiState.filteredCustomers.isNotEmpty()) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 160.dp)
                                                            .verticalScroll(rememberScrollState())
                                                    ) {
                                                        uiState.filteredCustomers.forEach { customer ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable { viewModel.selectCustomer(customer) }
                                                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                    customer.phone?.let { Text(it, color = Color.Gray, fontSize = 11.sp) }
                                                                }
                                                                Text(
                                                                    text = "Select",
                                                                    color = Color(0xFF2563EB),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            HorizontalDivider(color = dividerColor)
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = "No customers found. Please add customer in Khata book.",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(16.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SALE NOTES",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = primaryNavy,
                                        letterSpacing = 0.5.sp
                                    )
                                    OutlinedTextField(
                                        value = uiState.notes,
                                        onValueChange = viewModel::setNotes,
                                        placeholder = { Text("Add custom comments or notes (optional)...", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // Deduction Warning Box
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2FB)),
                                    border = BorderStroke(1.dp, Color(0xFFC6C5D4).copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info icon",
                                            tint = primaryNavy,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Stock will be deducted from your inventory automatically upon confirmation of this sale.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Grand Total & Checkout Footer Card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 8.dp,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val subTotal = uiState.cart.sumOf { it.subtotal }
                    val discountedTotal = subTotal - (subTotal * (discountPercent / 100.0))

                    // Grand total block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GRAND TOTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "₹${"%.2f".format(discountedTotal)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryNavy
                            )
                        }

                        // Add Discount Link
                        Text(
                            text = if (discountPercent > 0) "${discountPercent.toInt()}% Off (₹${"%.2f".format(subTotal * (discountPercent / 100.0))}) ✎" else "Add Discount ✎",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { showDiscountDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Complete Sale Button
                    Button(
                        onClick = {
                            viewModel.checkoutSale {
                                showSuccessFeedback = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("checkout_sale_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryNavy),
                        enabled = !uiState.isLoading && uiState.cart.isNotEmpty()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Sale",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Dialogs
            if (showDiscountDialog) {
                var tempDisc by remember { mutableStateOf(discountPercent.toString()) }
                AlertDialog(
                    onDismissRequest = { showDiscountDialog = false },
                    title = { Text("Apply Discount", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Enter discount percentage to apply to entire bill subtotal:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = tempDisc,
                                onValueChange = { tempDisc = it },
                                label = { Text("Discount (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                discountPercent = tempDisc.toDoubleOrNull() ?: 0.0
                                showDiscountDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryNavy)
                        ) {
                            Text("Apply")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscountDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showInsufficientStockDialog) {
                AlertDialog(
                    onDismissRequest = { showInsufficientStockDialog = false },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFDAD6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Low Stock Warning icon",
                                tint = Color(0xFFBA1A1A),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Insufficient Stock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = "You cannot sell more than available stock for this item.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showInsufficientStockDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                )
            }

            if (showSuccessFeedback) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = primaryNavy
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Sale Recorded!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stock levels updated & entry added to database.",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        Button(
                            onClick = {
                                showSuccessFeedback = false
                                viewModel.clearCart()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Go Back",
                                color = primaryNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultProductCard(
    product: InventoryEntity,
    cartItem: CartItem?,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val imageUrl = when {
        product.itemName.contains("Atta", ignoreCase = true) || product.itemName.contains("Aashirvaad", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAXhId5LCjBS6V2K1z67Ai8snqcSk3YPNWP8ryb2hGHwasr-FMqYijn1WgUHLORqJqOMhOyNUME4c_ZdFpEK2NWs7jBl28ZHq2kUq6dvrGN8AsNKl9scGGUD-lRgnb6ByXxGwjcKe8c7SKmplsqjVFxyh4_FUKBmKCqiOJv-qt-Pg_59dm-A9MbGbvQ_bF4DTng5ma1_JvF1v3jQ99vyQjS9pTy15IE6Xj1XMoNOiI3RpulG0FFKLzmhis_tFZCDdmSvhUX78mH93U"
        product.itemName.contains("Milk", ignoreCase = true) || product.itemName.contains("Amul", ignoreCase = true) || product.itemName.contains("Taaza", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuD3qvMtNooWUqVvt_xRKjCtDqWPNLKVkgYfMfPHuWz0kNAZyO0BiMdKT7anPm067X2GY0x6__ld-IyRVbM3n9_3jl2Jb8CoOIeabzsyN4hBrEJL2LonWtvAOELGfP5mSQGMuwqS37-0HrwIlb1p2K9jHJmDmYSJmYc06Da2iImYd3MjDZ7SMEV3Eaj0tGle0KKBLoQjq5M1kbN2hpwQ69yK-2blSVJ9Py8o9oXUuuLc7PrQkwvlwfOu8MVh2eD-ixC60mrqI_j2ngA"
        product.itemName.contains("Rice", ignoreCase = true) || product.itemName.contains("Basmati", ignoreCase = true) || product.itemName.contains("Gate", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA50-ZFWXYUM-qLkPHcPgi8HyH6htpsa9weVlrvoRF80IwOoCQKvZ3cNUSnf87oMT7PeWBigqwuijFMNthvb_JHf6kmgr6FLSDYpSfJHMf_ZovCUSYxHrvs15bzLNcaUZhpsfTufn5U3UYv3c2Nov7Pe87ohdAP60taFInv4UmWQWZElXB9Cp5RdJnCBYyMDqKS1oGUYXQd5SGvayCHZdpHuemavS2oa0VzyL-9uG8ownGniA3n8wVH5nNGXH3Y6gHVnXgGQR500x4"
        else -> null
    }

    val primaryNavy = Color(0xFF0B1A7D)
    val textDark = Color(0xFF0F172A)
    val textMuted = Color(0xFF64748B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEF2F6)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.itemName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹${product.mrp?.toInt() ?: 0}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFF2563EB)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${product.quantity.toInt()} in stock",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (cartItem != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color(0xFFEEF2F6), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp), tint = Color(0xFF475569))
                    }
                    Text(text = cartItem.quantity.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textDark)
                    IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp), tint = Color(0xFF475569))
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    enabled = product.quantity > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryNavy),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CartItemRedesignedCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val imageUrl = when {
        cartItem.product.itemName.contains("Atta", ignoreCase = true) || cartItem.product.itemName.contains("Aashirvaad", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAXhId5LCjBS6V2K1z67Ai8snqcSk3YPNWP8ryb2hGHwasr-FMqYijn1WgUHLORqJqOMhOyNUME4c_ZdFpEK2NWs7jBl28ZHq2kUq6dvrGN8AsNKl9scGGUD-lRgnb6ByXxGwjcKe8c7SKmplsqjVFxyh4_FUKBmKCqiOJv-qt-Pg_59dm-A9MbGbvQ_bF4DTng5ma1_JvF1v3jQ99vyQjS9pTy15IE6Xj1XMoNOiI3RpulG0FFKLzmhis_tFZCDdmSvhUX78mH93U"
        cartItem.product.itemName.contains("Milk", ignoreCase = true) || cartItem.product.itemName.contains("Amul", ignoreCase = true) || cartItem.product.itemName.contains("Taaza", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuD3qvMtNooWUqVvt_xRKjCtDqWPNLKVkgYfMfPHuWz0kNAZyO0BiMdKT7anPm067X2GY0x6__ld-IyRVbM3n9_3jl2Jb8CoOIeabzsyN4hBrEJL2LonWtvAOELGfP5mSQGMuwqS37-0HrwIlb1p2K9jHJmDmYSJmYc06Da2iImYd3MjDZ7SMEV3Eaj0tGle0KKBLoQjq5M1kbN2hpwQ69yK-2blSVJ9Py8o9oXUuuLc7PrQkwvlwfOu8MVh2eD-ixC60mrqI_j2ngA"
        cartItem.product.itemName.contains("Rice", ignoreCase = true) || cartItem.product.itemName.contains("Basmati", ignoreCase = true) || cartItem.product.itemName.contains("Gate", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA50-ZFWXYUM-qLkPHcPgi8HyH6htpsa9weVlrvoRF80IwOoCQKvZ3cNUSnf87oMT7PeWBigqwuijFMNthvb_JHf6kmgr6FLSDYpSfJHMf_ZovCUSYxHrvs15bzLNcaUZhpsfTufn5U3UYv3c2Nov7Pe87ohdAP60taFInv4UmWQWZElXB9Cp5RdJnCBYyMDqKS1oGUYXQd5SGvayCHZdpHuemavS2oa0VzyL-9uG8ownGniA3n8wVH5nNGXH3Y6gHVnXgGQR500x4"
        else -> null
    }

    val textDark = Color(0xFF0F172A)
    val textMuted = Color(0xFF64748B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEF2F6)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = cartItem.product.itemName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${cartItem.product.quantity.toInt()} in stock",
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quantity",
                        fontSize = 12.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0xFFEEF2F6), RoundedCornerShape(16.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp), tint = Color(0xFF475569))
                        }
                        Text(text = cartItem.quantity.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textDark)
                        IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp), tint = Color(0xFF475569))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.2f".format(cartItem.subtotal)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val unitPrice = cartItem.product.mrp ?: 0.0
                Text(
                    text = "₹${unitPrice.toInt()}/unit",
                    color = textMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
