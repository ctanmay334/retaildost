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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KiranaViewModel
import com.example.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(viewModel: KiranaViewModel) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var skuStr by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var mrpStr by remember { mutableStateOf("") }
    var openingStockStr by remember { mutableStateOf("") }
    var lowStockAlertStr by remember { mutableStateOf("") }

    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Staples", "Dairy", "Snacks", "Personal Care", "Cleaning")

    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedPhotoUri = it.toString() }
    }

    // Premium Color Palette from Stitch Mockups
    val primaryIndigo = Color(0xFF0B1A7D)
    val textDark = Color(0xFF1B1B21)
    val onSurfaceVariant = Color(0xFF454652)
    val backgroundLight = Color(0xFFFBF8FF)
    val outlineVariant = Color(0xFFC6C5D4)
    val surfaceContainerLowest = Color(0xFFFFFFFF)

    val inputFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF5F2FB),
        unfocusedContainerColor = Color(0xFFF5F2FB),
        focusedBorderColor = primaryIndigo,
        unfocusedBorderColor = outlineVariant,
        focusedLabelColor = primaryIndigo,
        unfocusedLabelColor = onSurfaceVariant,
        focusedPrefixColor = onSurfaceVariant,
        unfocusedPrefixColor = onSurfaceVariant,
        focusedTextColor = textDark,
        unfocusedTextColor = textDark
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Product",
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Dashboard) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = textDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundLight)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = backgroundLight,
                tonalElevation = 8.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.Dashboard) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = onSurfaceVariant) },
                    label = { Text("Home", color = onSurfaceVariant, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryIndigo.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = "Stock", tint = primaryIndigo)
                        }
                    },
                    label = { Text("Stock", color = primaryIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.Dashboard); viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Khata", tint = onSurfaceVariant) },
                    label = { Text("Khata", color = onSurfaceVariant, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.Marketplace) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Market", tint = onSurfaceVariant) },
                    label = { Text("Market", color = onSurfaceVariant, fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        if (isOcrProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = primaryIndigo)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI Optical OCR parsing...",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reading items description, MRP, batches...",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Exact Screenshot 5 Dotted Photo Container ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .drawBehind {
                            val stroke = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                            drawRoundRect(
                                color = outlineVariant,
                                style = stroke,
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                        }
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUri != null) {
                        coil.compose.AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(primaryIndigo.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera icon",
                                    modifier = Modifier.size(28.dp),
                                    tint = primaryIndigo
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Product Image",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = primaryIndigo
                            )
                            Text(
                                text = "Supports JPG, PNG (Max 5MB)",
                                fontSize = 12.sp,
                                color = onSurfaceVariant
                            )
                        }
                    }
                }

                // ── BASIC INFO Section ──
                Text(
                    text = "BASIC INFO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryIndigo,
                    letterSpacing = 0.5.sp
                )

                // Product Name input box
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name*") },
                    singleLine = true,
                    colors = inputFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown selection & SKU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            colors = inputFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = skuStr,
                        onValueChange = { skuStr = it },
                        label = { Text("SKU / ID") },
                        singleLine = true,
                        colors = inputFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── PRICING Section ──
                Text(
                    text = "PRICING",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryIndigo,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("Purchase Price") },
                        prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = inputFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Selling Price") },
                        prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = inputFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = mrpStr,
                    onValueChange = { mrpStr = it },
                    label = { Text("MRP") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = inputFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── INVENTORY SETTINGS Section ──
                Text(
                    text = "INVENTORY SETTINGS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryIndigo,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = openingStockStr,
                        onValueChange = { openingStockStr = it },
                        label = { Text("Opening Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = inputFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lowStockAlertStr,
                        onValueChange = { lowStockAlertStr = it },
                        label = { Text("Low Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = inputFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Fields marked with * are mandatory.",
                    fontSize = 12.sp,
                    color = onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save Product Button floating at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(backgroundLight)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val cost = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        val selling = sellingPriceStr.toDoubleOrNull() ?: mrpStr.toDoubleOrNull() ?: 0.0
                        val mrpVal = mrpStr.toDoubleOrNull() ?: selling
                        val qty = openingStockStr.toIntOrNull() ?: 0
                        val lowStock = lowStockAlertStr.toIntOrNull() ?: 5

                        if (name.isNotEmpty() && selectedCategory.isNotEmpty()) {
                            viewModel.onAddOrUpdateProduct(
                                name = name,
                                category = selectedCategory,
                                brand = "",
                                quantity = qty,
                                minQty = lowStock,
                                costPrice = cost,
                                mrp = mrpVal,
                                expiryDate = "",
                                batchNo = skuStr,
                                imageUri = selectedPhotoUri
                            )
                            viewModel.navigateTo(Screen.Dashboard)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Save Product",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
