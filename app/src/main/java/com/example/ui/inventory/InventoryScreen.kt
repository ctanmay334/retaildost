package com.example.ui.inventory

import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.utils.AutoResizingText
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.data.model.InventoryEntity
import coil.compose.AsyncImage
import com.example.ui.KiranaViewModel
import com.example.ui.Screen

/**
 * InventoryScreen
 * ───────────────
 * Premium stock management center supporting search, category chips, alerts,
 * quick stock deductions, and editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryId: String? = null,
    onNavigateBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    viewModel: InventoryViewModel = hiltViewModel(),
    kiranaViewModel: KiranaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<InventoryEntity?>(null) }
    val alerts by kiranaViewModel.alerts.collectAsState()
    val unreadCount = alerts.filter { !it.isRead }.size

    LaunchedEffect(inventoryId) {
        if (!inventoryId.isNullOrBlank()) {
            viewModel.filterById(inventoryId)
        }
    }

    // Stitch Design System Colors
    val primaryColor = Color(0xFF0B1A7D)
    val backgroundBg = Color(0xFFFBF8FF)
    val onSurface = Color(0xFF1B1B21)
    val onSurfaceVariant = Color(0xFF454652)
    val outlineVariant = Color(0xFFC6C5D4)
    val surfaceContainerLow = Color(0xFFF5F2FB)
    val surfaceContainerHighest = Color(0xFFE4E1EA)
    val primaryContainer = Color(0xFF283593)
    val onPrimaryContainer = Color(0xFF9AA5FF)
    val errorColor = Color(0xFFBA1A1A)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)
    val secondaryContainer = Color(0xFF94F0DF)
    val onSecondaryContainer = Color(0xFF006F62)

    // Dynamic stats computation from live dataset
    val lowStockCount = allItems.count { it.quantity <= it.minThreshold && it.quantity > 0.0 && it.deletedAt == null }
    val outOfStockCount = allItems.count { it.quantity == 0.0 && it.deletedAt == null }

    var parentWidth by remember { mutableStateOf(0f) }
    var parentHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                parentWidth = coordinates.size.width.toFloat()
                parentHeight = coordinates.size.height.toFloat()
            }
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Suresh Bhai Circular Avatar Profile Image
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCLfo2R644A59ByH0UPnlELEMmRWZv-vb2HeC7V581AgRLTJEl1W17ZBT8XhPabYIo8Nbq774vGJQJq6xBuF9GQACmoq54Eyp1AbMOK9nvWHS5DAuC_My72G4HynORu-fSkNEYsxgEBSDu8PuH00-cj4oFdtfsRSq9VAbMQbpGuJ4OUWN_uDFC9zEg-ouu_9t5n6n4CKkrSlaDzCKYbx1HtU4EB1EE1H0OmHayojueNf4SzY9vOkINjUXVR6Jp3PtPeqCZhJ7-sjoU",
                            contentDescription = "Suresh Bhai Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primaryContainer)
                        )
                        Text(
                            text = "Inventory Stock & Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryColor
                        )
                    }
                },
                navigationIcon = {
                    if (showNavigationIcon) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close screen",
                                tint = onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .clickable { kiranaViewModel.navigateTo(Screen.Notifications) }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Stats Grid Row (Stitch Specifications) ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric Card 1: Low Stock Items
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                    border = BorderStroke(1.dp, outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Low Stock Items",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format("%02d", lowStockCount),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            if (lowStockCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = errorColor,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "ALERT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Metric Card 2: Out of Stock
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                    border = BorderStroke(1.dp, outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Out of Stock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariant
                        )
                        Text(
                            text = String.format("%02d", outOfStockCount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (outOfStockCount > 0) errorColor else onSurface
                        )
                    }
                }
            }

            // ── AI STOCK SYNC Premium Banner Card (Stitch Specifications) ────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor),
                border = BorderStroke(1.dp, primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI STOCK SYNC",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = onPrimaryContainer,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Text(
                        text = "Upload Invoice or Bills to Update Stock Instantly",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 26.sp
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stock In Button (Scan Receipt from Distributor)
                        Button(
                            onClick = {
                                kiranaViewModel.navigateTo(Screen.OcrReview(isStockOut = false))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Stock In (Scan Receipt)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // Stock Out Button (Scan Diary of Sold Items)
                        OutlinedButton(
                            onClick = {
                                kiranaViewModel.navigateTo(Screen.OcrReview(isStockOut = true))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, onPrimaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Stock Out (Scan Diary)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ── Search & Filters Section ─────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Input with leading icon search and grey background
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search Products", fontSize = 15.sp, color = outlineVariant) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = outlineVariant
                        )
                    },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surfaceContainerHighest,
                        unfocusedContainerColor = surfaceContainerHighest,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("inventory_search_field")
                )

                // Filter tabs LazyRow
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterTabButton(
                            text = "All",
                            selected = !uiState.showLowStockOnly && !uiState.showOutOfStockOnly && !uiState.showExpiringSoonOnly,
                            onClick = {
                                viewModel.clearAllFilters()
                            }
                        )
                    }
                    item {
                        FilterTabButton(
                            text = "Low Stock",
                            selected = uiState.showLowStockOnly,
                            onClick = viewModel::toggleLowStockFilter
                        )
                    }
                    item {
                        FilterTabButton(
                            text = "Out of Stock",
                            selected = uiState.showOutOfStockOnly,
                            onClick = viewModel::toggleOutOfStockFilter
                        )
                    }
                }
            }

            // ── Product List Audit View ──────────────────────────────────────
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
                        text = "RECENT INVENTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.clickable { kiranaViewModel.navigateTo(Screen.AllProducts) }
                    )
                }

                if (uiState.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items match your criteria",
                            color = outlineVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.items.forEach { item ->
                            InventoryCard(
                                item = item,
                                onEdit = { editingItem = item },
                                onDelete = { itemToDelete = item },
                                onDecrement = { viewModel.decrementStock(item.id, 1.0) },
                                onIncrement = { viewModel.incrementStock(item.id, 1.0) }
                            )
                        }
                    }
                }
            }
        }

        // ── Dialogs: Add & Edit popups ───────────────────────────────────────
        if (showAddDialog) {
            AddEditInventoryDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, cat, unit, qty, threshold, cost, mrp, batch, expiry ->
                    viewModel.addItem(name, cat, unit, qty, threshold, cost, mrp, batch, expiry)
                    showAddDialog = false
                }
            )
        }

        if (editingItem != null) {
            AddEditInventoryDialog(
                item = editingItem,
                onDismiss = { editingItem = null },
                onSave = { name, cat, unit, qty, threshold, cost, mrp, batch, expiry ->
                    val updated = editingItem!!.copy(
                        itemName = name,
                        category = cat,
                        unitLabel = unit,
                        quantity = qty,
                        minThreshold = threshold,
                        costPrice = cost,
                        mrp = mrp,
                        batchNo = batch,
                        expiryDate = expiry
                    )
                    viewModel.updateItem(updated)
                    editingItem = null
                }
            )
        }

        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = {
                    Text(
                        text = "Delete Product?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA1A1A)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete \"${itemToDelete!!.itemName}\" from your inventory? This action cannot be undone.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(itemToDelete!!.id)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel", color = Color(0xFF454652))
                    }
                }
            )
        }
        }

        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        
        var buttonWidth by remember { mutableStateOf(0f) }
        var buttonHeight by remember { mutableStateOf(0f) }
        
        val animX = remember { Animatable(0f) }
        val animY = remember { Animatable(0f) }
        var isInitialized by remember { mutableStateOf(false) }
        
        val paddingPx = with(density) { 16.dp.toPx() }
        val topBoundPx = with(density) { 80.dp.toPx() }
        
        LaunchedEffect(parentWidth, parentHeight, buttonWidth, buttonHeight) {
            if (parentWidth > 0f && parentHeight > 0f && buttonWidth > 0f && buttonHeight > 0f && !isInitialized) {
                val initialX = parentWidth - buttonWidth - paddingPx
                val initialY = parentHeight - buttonHeight - paddingPx
                animX.snapTo(initialX)
                animY.snapTo(initialY)
                isInitialized = true
            }
        }
        
        var dragDisplacement by remember { mutableStateOf(0f) }

        // Add Product FAB aligned at bottom end of the screen Box
        ExtendedFloatingActionButton(
            onClick = { kiranaViewModel.navigateTo(Screen.AddProduct) },
            containerColor = primaryColor,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .onGloballyPositioned { coords ->
                    buttonWidth = coords.size.width.toFloat()
                    buttonHeight = coords.size.height.toFloat()
                }
                .offset {
                    if (isInitialized) {
                        IntOffset(animX.value.roundToInt(), animY.value.roundToInt())
                    } else {
                        val x = (parentWidth - buttonWidth - paddingPx).coerceAtLeast(0f)
                        val y = (parentHeight - buttonHeight - paddingPx).coerceAtLeast(0f)
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                }
                .pointerInput(parentWidth, parentHeight, buttonWidth, buttonHeight, isInitialized) {
                    if (!isInitialized) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            dragDisplacement = 0f
                        },
                        onDragEnd = {
                            if (dragDisplacement < 15f) {
                                kiranaViewModel.navigateTo(Screen.AddProduct)
                            } else {
                                val bottomBound = parentHeight - buttonHeight - paddingPx
                                val leftBound = paddingPx
                                val rightBound = parentWidth - buttonWidth - paddingPx
                                
                                val corners = listOf(
                                    Offset(leftBound, topBoundPx),
                                    Offset(rightBound, topBoundPx),
                                    Offset(leftBound, bottomBound),
                                    Offset(rightBound, bottomBound)
                                )
                                val currentPos = Offset(animX.value, animY.value)
                                val closestCorner = corners.minByOrNull { (it - currentPos).getDistanceSquared() } 
                                    ?: Offset(rightBound, bottomBound)
                                    
                                coroutineScope.launch {
                                    animX.animateTo(
                                        targetValue = closestCorner.x,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                coroutineScope.launch {
                                    animY.animateTo(
                                        targetValue = closestCorner.y,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            val bottomBound = parentHeight - buttonHeight - paddingPx
                            val leftBound = paddingPx
                            val rightBound = parentWidth - buttonWidth - paddingPx
                            
                            val corners = listOf(
                                Offset(leftBound, topBoundPx),
                                Offset(rightBound, topBoundPx),
                                Offset(leftBound, bottomBound),
                                Offset(rightBound, bottomBound)
                            )
                            val currentPos = Offset(animX.value, animY.value)
                            val closestCorner = corners.minByOrNull { (it - currentPos).getDistanceSquared() } 
                                ?: Offset(rightBound, bottomBound)
                                
                            coroutineScope.launch {
                                animX.animateTo(closestCorner.x, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                            }
                            coroutineScope.launch {
                                animY.animateTo(closestCorner.y, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDisplacement += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                            
                            val bottomBound = parentHeight - buttonHeight - paddingPx
                            val leftBound = paddingPx
                            val rightBound = parentWidth - buttonWidth - paddingPx
                            
                            val nextX = (animX.value + dragAmount.x).coerceIn(leftBound, rightBound)
                            val nextY = (animY.value + dragAmount.y).coerceIn(topBoundPx, bottomBound)
                            
                            coroutineScope.launch {
                                animX.snapTo(nextX)
                            }
                            coroutineScope.launch {
                                animY.snapTo(nextY)
                            }
                        }
                    )
                }
                .testTag("add_inventory_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Product",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ── Stitch Styled Filter Tab Button ──────────────────────────────────────────

@Composable
fun FilterTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = Color(0xFF0B1A7D)
    val surfaceContainerHigh = Color(0xFFE9E7F0)
    val outlineVariant = Color(0xFFC6C5D4)
    val onSurfaceVariant = Color(0xFF454652)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) primaryColor else surfaceContainerHigh,
        border = BorderStroke(1.dp, if (selected) Color.Transparent else outlineVariant),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Product Inventory Card (Exact Stitch Specifications) ─────────────────────

@Composable
fun InventoryCard(
    item: InventoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val primaryColor = Color(0xFF0B1A7D)
    val onSurface = Color(0xFF1B1B21)
    val outlineVariant = Color(0xFFC6C5D4)
    val outline = Color(0xFF767683)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)
    val secondaryContainer = Color(0xFF94F0DF)
    val onSecondaryContainer = Color(0xFF006F62)
    val errorColor = Color(0xFFBA1A1A)
    val surfaceContainerLow = Color(0xFFF5F2FB)

    val lowStock = item.quantity <= item.minThreshold

    // Pixel-perfect image loading corresponding to the Stitch layout
    val imageUrl = when {
        item.itemName.contains("Atta", ignoreCase = true) || item.itemName.contains("Aashirvaad", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAXhId5LCjBS6V2K1z67Ai8snqcSk3YPNWP8ryb2hGHwasr-FMqYijn1WgUHLORqJqOMhOyNUME4c_ZdFpEK2NWs7jBl28ZHq2kUq6dvrGN8AsNKl9scGGUD-lRgnb6ByXxGwjcKe8c7SKmplsqjVFxyh4_FUKBmKCqiOJv-qt-Pg_59dm-A9MbGbvQ_bF4DTng5ma1_JvF1v3jQ99vyQjS9pTy15IE6Xj1XMoNOiI3RpulG0FFKLzmhis_tFZCDdmSvhUX78mH93U"
        item.itemName.contains("Milk", ignoreCase = true) || item.itemName.contains("Amul", ignoreCase = true) || item.itemName.contains("Taaza", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuD3qvMtNooWUqVvt_xRKjCtDqWPNLKVkgYfMfPHuWz0kNAZyO0BiMdKT7anPm067X2GY0x6__ld-IyRVbM3n9_3jl2Jb8CoOIeabzsyN4hBrEJL2LonWtvAOELGfP5mSQGMuwqS37-0HrwIlb1p2K9jHJmDmYSJmYc06Da2iImYd3MjDZ7SMEV3Eaj0tGle0KKBLoQjq5M1kbN2hpwQ69yK-2blSVJ9Py8o9oXUuuLc7PrQkwvlwfOu8MVh2eD-ixC60mrqI_j2ngA"
        item.itemName.contains("Rice", ignoreCase = true) || item.itemName.contains("Basmati", ignoreCase = true) || item.itemName.contains("Gate", ignoreCase = true) ->
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA50-ZFWXYUM-qLkPHcPgi8HyH6htpsa9weVlrvoRF80IwOoCQKvZ3cNUSnf87oMT7PeWBigqwuijFMNthvb_JHf6kmgr6FLSDYpSfJHMf_ZovCUSYxHrvs15bzLNcaUZhpsfTufn5U3UYv3c2Nov7Pe87ohdAP60taFInv4UmWQWZElXB9Cp5RdJnCBYyMDqKS1oGUYXQd5SGvayCHZdpHuemavS2oa0VzyL-9uG8ownGniA3n8wVH5nNGXH3Y6gHVnXgGQR500x4"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Image Container on the Left
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = item.itemName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = outline,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Product Details and Controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AutoResizingText(
                            text = item.itemName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        AutoResizingText(
                            text = "${item.batchNo ?: "FMCG"} • ${item.category ?: "General"}",
                            fontSize = 12.sp,
                            color = outline
                        )
                    }
                    AutoResizingText(
                        text = "₹${item.mrp ?: 0.0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stock Badge with dynamic styling based on stock level
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (lowStock) errorContainer else secondaryContainer,
                        contentColor = if (lowStock) onErrorContainer else onSecondaryContainer
                    ) {
                        AutoResizingText(
                            text = "Stock: " + String.format("%02d", item.quantity.toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Decrement quick Sell Action Button (circular Stitch specification)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = outline, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = errorColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDecrement,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Sell 1 unit",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onIncrement,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Restock 1 unit",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Add/Edit Inventory Form Popup Dialog ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInventoryDialog(
    item: InventoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        itemName: String,
        category: String,
        unitLabel: String,
        quantity: Double,
        minThreshold: Double,
        costPrice: Double?,
        mrp: Double?,
        batchNo: String?,
        expiryDate: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(item?.itemName ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Staples") }
    var unitLabel by remember { mutableStateOf(item?.unitLabel ?: "pcs") }
    var quantityStr by remember { mutableStateOf(item?.quantity?.toString() ?: "10") }
    var thresholdStr by remember { mutableStateOf(item?.minThreshold?.toString() ?: "5") }
    var costStr by remember { mutableStateOf(item?.costPrice?.toString() ?: "") }
    var mrpStr by remember { mutableStateOf(item?.mrp?.toString() ?: "") }
    var batchNo by remember { mutableStateOf(item?.batchNo ?: "") }
    var expiryDate by remember { mutableStateOf(item?.expiryDate ?: "") }

    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Staples", "Dairy", "Snacks", "Personal Care", "Cleaning")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (item == null) "Add New Product" else "Edit Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_product_name")
                )

                // Category exposed dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dialog_category_select")
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text("Unit (e.g. kg, pcs)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("dialog_quantity")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("MRP *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("dialog_mrp")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Min Threshold") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchNo,
                        onValueChange = { batchNo = it },
                        label = { Text("Batch No") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && quantityStr.isNotEmpty() && mrpStr.isNotEmpty()) {
                        onSave(
                            name,
                            category,
                            unitLabel,
                            quantityStr.toDoubleOrNull() ?: 0.0,
                            thresholdStr.toDoubleOrNull() ?: 5.0,
                            costStr.toDoubleOrNull(),
                            mrpStr.toDoubleOrNull() ?: 0.0,
                            batchNo.ifBlank { null },
                            expiryDate.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank() && quantityStr.isNotEmpty() && mrpStr.isNotEmpty(),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
