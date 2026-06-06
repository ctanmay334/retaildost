package com.example.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventoryEntity
import com.example.ui.KiranaViewModel
import com.example.ui.inventory.InventoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: KiranaViewModel,
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by inventoryViewModel.uiState.collectAsStateWithLifecycle()
    var product by remember { mutableStateOf<InventoryEntity?>(null) }
    var adjustQtyStr by remember { mutableStateOf("1") }

    // Query sales history from the sales log
    val salesHistory by viewModel.getSaleItemsForProduct(productId).collectAsState(initial = emptyList())

    LaunchedEffect(productId, uiState.items) {
        product = uiState.items.find { it.id == productId }
    }

    // Modern color palette
    val primaryIndigo = Color(0xFF0B1A7D)
    val secondaryTeal = Color(0xFF006B5E)
    val errorCrimson = Color(0xFFBA1A1A)
    val textDark = Color(0xFF1B1B21)
    val textMuted = Color(0xFF64748B)
    val bgLight = Color(0xFFF8FAFC)
    val cardBg = Color.White
    val borderOutline = Color(0xFFE2E8F0)

    if (product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryIndigo)
        }
    } else {
        val item = product!!

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Product Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textDark
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgLight)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Basic details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Product Name & Category
                        Column {
                            Text(
                                text = item.itemName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = primaryIndigo
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE0F2F1))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = (item.category ?: "General").ifBlank { "General" },
                                    color = secondaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = borderOutline)

                        // Purchase & Selling Price Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Purchase Price",
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${String.format("%.2f", item.costPrice ?: 0.0)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(borderOutline)
                                    .align(Alignment.CenterVertically)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selling Price / MRP",
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${String.format("%.2f", item.mrp ?: 0.0)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                            }
                        }
                    }
                }

                // 2. Current Stock Count & Action controls card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Stock display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Stock",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${item.quantity.toInt()}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryIndigo
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.unitLabel ?: "pcs",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMuted,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = borderOutline)

                        // Action Controls to Add or Remove stock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = adjustQtyStr,
                                onValueChange = { adjustQtyStr = it },
                                label = { Text("Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Column(
                                modifier = Modifier.weight(1.5f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val qty = adjustQtyStr.toDoubleOrNull() ?: 0.0
                                        if (qty > 0) {
                                            inventoryViewModel.incrementStock(item.id, qty)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Stock", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val qty = adjustQtyStr.toDoubleOrNull() ?: 0.0
                                        if (qty > 0) {
                                            inventoryViewModel.decrementStock(item.id, qty)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = errorCrimson),
                                    border = BorderStroke(1.5.dp, errorCrimson),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove Stock", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. Sales history for that product from the sales log
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALES HISTORY LOG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryIndigo,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Sales history",
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderOutline)
                    ) {
                        if (salesHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No sales logged yet for this product.",
                                    color = textMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                salesHistory.forEachIndexed { index, saleItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFF1F2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Sale Record",
                                                tint = errorCrimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Customer Sale",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textDark
                                                )
                                                Text(
                                                    text = "-${saleItem.quantitySold.toInt()} ${saleItem.unitLabel ?: "pcs"}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = errorCrimson
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Rate: ₹${String.format("%.2f", saleItem.salePrice ?: 0.0)}",
                                                        fontSize = 12.sp,
                                                        color = textMuted,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "Total Price: ₹${String.format("%.2f", saleItem.quantitySold * (saleItem.salePrice ?: 0.0))}",
                                                        fontSize = 11.sp,
                                                        color = textMuted
                                                    )
                                                }
                                                Text(
                                                    text = dateFormat.format(Date(saleItem.createdAt)),
                                                    fontSize = 11.sp,
                                                    color = textMuted,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (index < salesHistory.lastIndex) {
                                        HorizontalDivider(color = borderOutline, modifier = Modifier.padding(top = 12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
