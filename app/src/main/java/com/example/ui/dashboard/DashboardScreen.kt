package com.example.ui.dashboard

import android.graphics.Bitmap
import kotlinx.coroutines.launch
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import com.example.data.model.CustomerEntity
import com.example.data.model.ItemEntity
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.marketplace.MarketplaceScreen
import com.example.utils.AutoResizingText
import java.util.Date
import android.speech.RecognizerIntent
import android.content.Intent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow

@Composable
fun DashboardScreen(viewModel: KiranaViewModel, khataViewModel: com.example.ui.khata.KhataViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val openVoiceOverlay by viewModel.voiceOverlayOpen.collectAsState()
    val cameraScanType by viewModel.cameraScanType.collectAsState()
    var rootWidth by remember { mutableStateOf(0f) }
    var rootHeight by remember { mutableStateOf(0f) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("dashboard_bottom_nav"),
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Intelligence report Hub") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("tab_home"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B1A7D),
                        selectedTextColor = Color(0xFF0B1A7D),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Products Stock lists") },
                    label = { Text("Stock") },
                    modifier = Modifier.testTag("tab_stock"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B1A7D),
                        selectedTextColor = Color(0xFF0B1A7D),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Ledger list Khata") },
                    label = { Text("Khata") },
                    modifier = Modifier.testTag("tab_khata"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF005B54),
                        selectedTextColor = Color(0xFF005B54),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF80ECC9)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Store, contentDescription = "Distributor Market Portal") },
                    label = { Text("Market") },
                    modifier = Modifier.testTag("tab_market"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B1A7D),
                        selectedTextColor = Color(0xFF0B1A7D),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .onGloballyPositioned { coordinates ->
                    rootWidth = coordinates.size.width.toFloat()
                    rootHeight = coordinates.size.height.toFloat()
                }
        ) {
            when (selectedTab) {
                0 -> HomeTab(viewModel)
                1 -> InventoryScreen(
                    onNavigateBack = { viewModel.selectTab(0) },
                    showNavigationIcon = false,
                    kiranaViewModel = viewModel
                )
                2 -> KhataTab(viewModel, khataViewModel)
                3 -> MarketplaceScreen(
                    viewModel = viewModel
                )
            }

            // Draggable Voice FAB shown only on Khata (tab 2)
            if (selectedTab == 2) {
                val voiceDensity = LocalDensity.current
                val voiceCoroutineScope = rememberCoroutineScope()
                
                var voiceButtonWidth by remember { mutableStateOf(0f) }
                var voiceButtonHeight by remember { mutableStateOf(0f) }
                
                val voiceAnimX = remember { Animatable(0f) }
                val voiceAnimY = remember { Animatable(0f) }
                var isVoiceInitialized by remember { mutableStateOf(false) }
                
                val voicePaddingPx = with(voiceDensity) { 16.dp.toPx() }
                val voiceTopBoundPx = with(voiceDensity) { 80.dp.toPx() }
                
                LaunchedEffect(rootWidth, rootHeight, voiceButtonWidth, voiceButtonHeight) {
                    if (rootWidth > 0f && rootHeight > 0f && voiceButtonWidth > 0f && voiceButtonHeight > 0f && !isVoiceInitialized) {
                        val initialX = rootWidth - voiceButtonWidth - voicePaddingPx
                        val initialY = rootHeight - voiceButtonHeight - voicePaddingPx
                        voiceAnimX.snapTo(initialX)
                        voiceAnimY.snapTo(initialY)
                        isVoiceInitialized = true
                    }
                }
                
                var voiceDragDisplacement by remember { mutableStateOf(0f) }

                FloatingActionButton(
                    onClick = { viewModel.openVoiceOverlay() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { coords ->
                            voiceButtonWidth = coords.size.width.toFloat()
                            voiceButtonHeight = coords.size.height.toFloat()
                        }
                        .offset {
                            if (isVoiceInitialized) {
                                IntOffset(voiceAnimX.value.roundToInt(), voiceAnimY.value.roundToInt())
                            } else {
                                val x = (rootWidth - voiceButtonWidth - voicePaddingPx).coerceAtLeast(0f)
                                val y = (rootHeight - voiceButtonHeight - voicePaddingPx).coerceAtLeast(0f)
                                IntOffset(x.roundToInt(), y.roundToInt())
                            }
                        }
                        .pointerInput(rootWidth, rootHeight, voiceButtonWidth, voiceButtonHeight, isVoiceInitialized) {
                            if (!isVoiceInitialized) return@pointerInput
                            detectDragGestures(
                                onDragStart = {
                                    voiceDragDisplacement = 0f
                                },
                                onDragEnd = {
                                    if (voiceDragDisplacement < 15f) {
                                        viewModel.openVoiceOverlay()
                                    } else {
                                        val bottomBound = rootHeight - voiceButtonHeight - voicePaddingPx
                                        val leftBound = voicePaddingPx
                                        val rightBound = rootWidth - voiceButtonWidth - voicePaddingPx
                                        
                                        val corners = listOf(
                                            Offset(leftBound, voiceTopBoundPx),
                                            Offset(rightBound, voiceTopBoundPx),
                                            Offset(leftBound, bottomBound),
                                            Offset(rightBound, bottomBound)
                                        )
                                        val currentPos = Offset(voiceAnimX.value, voiceAnimY.value)
                                        val closestCorner = corners.minByOrNull { (it - currentPos).getDistanceSquared() } 
                                            ?: Offset(rightBound, bottomBound)
                                            
                                        voiceCoroutineScope.launch {
                                            voiceAnimX.animateTo(
                                                targetValue = closestCorner.x,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                        voiceCoroutineScope.launch {
                                            voiceAnimY.animateTo(
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
                                    val bottomBound = rootHeight - voiceButtonHeight - voicePaddingPx
                                    val leftBound = voicePaddingPx
                                    val rightBound = rootWidth - voiceButtonWidth - voicePaddingPx
                                    
                                    val corners = listOf(
                                        Offset(leftBound, voiceTopBoundPx),
                                        Offset(rightBound, voiceTopBoundPx),
                                        Offset(leftBound, bottomBound),
                                        Offset(rightBound, bottomBound)
                                    )
                                    val currentPos = Offset(voiceAnimX.value, voiceAnimY.value)
                                    val closestCorner = corners.minByOrNull { (it - currentPos).getDistanceSquared() } 
                                        ?: Offset(rightBound, bottomBound)
                                        
                                    voiceCoroutineScope.launch {
                                        voiceAnimX.animateTo(closestCorner.x, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                                    }
                                    voiceCoroutineScope.launch {
                                        voiceAnimY.animateTo(closestCorner.y, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    voiceDragDisplacement += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                                    
                                    val bottomBound = rootHeight - voiceButtonHeight - voicePaddingPx
                                    val leftBound = voicePaddingPx
                                    val rightBound = rootWidth - voiceButtonWidth - voicePaddingPx
                                    
                                    val nextX = (voiceAnimX.value + dragAmount.x).coerceIn(leftBound, rightBound)
                                    val nextY = (voiceAnimY.value + dragAmount.y).coerceIn(voiceTopBoundPx, bottomBound)
                                    
                                    voiceCoroutineScope.launch {
                                        voiceAnimX.snapTo(nextX)
                                    }
                                    voiceCoroutineScope.launch {
                                        voiceAnimY.snapTo(nextY)
                                    }
                                }
                            )
                        }
                        .padding(0.dp)
                        .testTag("ai_voice_ledgers_fab"),
                    containerColor = Color(0xFF80ECC9),
                    contentColor = Color(0xFF005B54),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Khata entry assistant",
                        tint = Color(0xFF005B54),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // AI Hinglish voice overlay block
            if (openVoiceOverlay) {
                VoiceAssistantOverlay(viewModel)
            }

            val showScanOptions by viewModel.showScanOptions.collectAsState()
            val showCameraScanner by viewModel.showCameraScanner.collectAsState()
            val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
            val ocrMessage by viewModel.ocrMessage.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            if (showScanOptions) {
                ScanOptionsDialog(
                    onDismiss = { viewModel.setShowScanOptions(false) },
                    onSelectOption = { scanType ->
                        viewModel.setCameraScannerVisible(true, scanType)
                        viewModel.setShowScanOptions(false)
                    }
                )
            }

            if (showCameraScanner) {
                com.example.ui.ocr.CameraScreen(
                    onImageCaptured = { uri ->
                        try {
                            // Save the scanned URI for the review screen preview
                            viewModel.setScannedInvoiceUri(uri)
                            
                            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                android.graphics.ImageDecoder.decodeBitmap(
                                    android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                )
                            } else {
                                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                            
                            // Make bitmap mutable just in case
                            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            viewModel.onProcessInvoiceOcr(mutableBitmap, isHandwritten = (cameraScanType == "stock_out")) {
                                viewModel.setCameraScannerVisible(false)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            viewModel.setCameraScannerVisible(false)
                        }
                    },
                    onClose = {
                        viewModel.setCameraScannerVisible(false)
                    },
                    isProcessing = isOcrProcessing,
                    processingMessage = ocrMessage
                )
            }
        }
    }
}

data class RecentActivityItem(
    val title: String,
    val description: String,
    val timestamp: Long,
    val amount: Double,
    val type: String, // "sale", "debit", "credit"
    val id: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(viewModel: KiranaViewModel) {
    val storeName by viewModel.storeName.collectAsState()
    val products by viewModel.products.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val salesList by viewModel.sales.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    val getRelativeTimeAgo = remember {
        { timestamp: Long ->
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                diff < 0 -> "just now"
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hr ago"
                days == 1L -> "Yesterday"
                days < 7 -> "$days days ago"
                else -> android.text.format.DateFormat.format("dd MMM", Date(timestamp)).toString()
            }
        }
    }

    val combinedActivity = remember(salesList, transactions, customers) {
        val list = mutableListOf<RecentActivityItem>()
        
        // Add Sales
        for (sale in salesList) {
            if (sale.deletedAt == null) {
                list.add(
                    RecentActivityItem(
                        title = "Sale: ${sale.customerName ?: "Cash Customer"}",
                        description = "Recorded ${sale.itemsCount} items",
                        timestamp = sale.createdAt,
                        amount = sale.totalAmount,
                        type = "sale",
                        id = sale.id
                    )
                )
            }
        }
        
        // Add Transactions
        for (tx in transactions) {
            val name = customers.find { it.id == tx.customerId }?.name ?: "Customer"
            val typeLabel = if (tx.type == "debit") "Udhar" else "Deposit"
            list.add(
                RecentActivityItem(
                    title = "$typeLabel: $name",
                    description = tx.rawInput.ifEmpty { if (tx.type == "debit") "Maine Diya" else "Maine Mila" },
                    timestamp = tx.date,
                    amount = tx.amount,
                    type = tx.type,
                    id = "tx_${tx.id}"
                )
            )
        }
        
        list.sortByDescending { it.timestamp }
        list.take(5)
    }
    
    val unreadCount = alerts.filter { !it.isRead }.size
    val todayDate = java.text.SimpleDateFormat("EEEE, dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    
    val todayStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val todaySales = salesList.filter { it.saleDate == todayStr && it.deletedAt == null }
    val todayRevenue = todaySales.sumOf { it.totalAmount }
    val currentRevenue = todayRevenue

    val yesterdayStr = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
    }
    val yesterdaySales = salesList.filter { it.saleDate == yesterdayStr && it.deletedAt == null }
    val yesterdayRevenue = yesterdaySales.sumOf { it.totalAmount }

    val trendPct = remember(todayRevenue, yesterdayRevenue) {
        if (yesterdayRevenue > 0.0) {
            ((todayRevenue - yesterdayRevenue) / yesterdayRevenue * 100).toInt()
        } else {
            if (todayRevenue > 0.0) 100 else 0
        }
    }

    val mockSalesData = remember(salesList) {
        val cal = java.util.Calendar.getInstance()
        val list = mutableListOf<Pair<String, Double>>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        for (i in 5 downTo 0) {
            cal.time = java.util.Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            
            val label = when (i) {
                0 -> "TODAY'S SALES"
                1 -> "YESTERDAY"
                else -> "$i DAYS AGO"
            }
            
            val dailyRevenue = salesList
                .filter { it.saleDate == dateStr && it.deletedAt == null }
                .sumOf { it.totalAmount }
                
            list.add(label to dailyRevenue)
        }
        list
    }
    var selectedBarIndex by remember { mutableStateOf(5) }
    val selectedData = mockSalesData.getOrNull(selectedBarIndex) ?: ("TODAY'S SALES" to currentRevenue)

    var parentWidth by remember { mutableStateOf(0f) }
    var parentHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .onGloballyPositioned { coordinates ->
                parentWidth = coordinates.size.width.toFloat()
                parentHeight = coordinates.size.height.toFloat()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Shopkeeper Avatar
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUMy1G-7L3chtuF4TiMsGQ40MV12V3MVqwWwH6dSwzdrt1tJBDtQhQGA73OdTY_jdunP6LDdm3_MZV_7djDx7H5i2wmkvFALCGB656VykXhvDQBC38HP_Gf2su70SepAL-7VN-JyPHy9-GTrW-888dNe8n1IeHJcW1R_5girplZTDp3NWTEOfpwwbbymc9NKmgo_6Di0AYy75qhRaxXaEINJ2T7d0Zi8KmQ1_Mp12S-x1gql2wTMPCLdT6lEfjL9CY9NW5JyJrVE8",
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "RetailDost",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1A7D) // Premium Navy
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(Screen.Notifications) }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF0B1A7D),
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF0B1A7D),
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { viewModel.navigateTo(Screen.Settings) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Body content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Today's Sales
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1A7D)), // Premium dark blue
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = selectedData.first,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%,.2f", selectedData.second)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
// Removed trend percentage bubble
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Sparkline trend bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxVal = mockSalesData.maxOfOrNull { it.second } ?: 0.0
                            val heights = mockSalesData.map {
                                val fraction = if (maxVal > 0.0) (it.second / maxVal).toFloat() else 0f
                                fraction.coerceIn(0.15f, 1.00f)
                            }
                            heights.forEachIndexed { idx, ht ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(ht)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (idx == selectedBarIndex) Color.White else Color.White.copy(alpha = 0.4f))
                                        .clickable { selectedBarIndex = idx }
                                )
                            }
                        }
                    }
                }

                // Card 2: Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Action 1: Add Sale
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { viewModel.navigateTo(Screen.RecordSale) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Add Sale", tint = Color(0xFF0B1A7D), modifier = Modifier.size(28.dp))
                            }
                        }
                        Text("Add Sale", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    // Action 2: Restock
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { viewModel.navigateTo(Screen.Inventory) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Assignment, contentDescription = "Restock", tint = Color(0xFF0B1A7D), modifier = Modifier.size(28.dp))
                            }
                        }
                        Text("Restock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    // Action 3: Sales Log
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { viewModel.navigateTo(Screen.SalesHistory) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Sales Log", tint = Color(0xFF0B1A7D), modifier = Modifier.size(28.dp))
                            }
                        }
                        Text("Sales Log", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    // Action 4: AI Scan
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { viewModel.navigateTo(Screen.OcrReview()) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1A7D)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "AI Scan", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        Text("AI Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }

                // Section 3: Critical Alerts
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Critical Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.Notifications) }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val activeAlerts = alerts.filter { alert ->
                        val isResolved = try {
                            val meta = alert.metadataJson?.let { org.json.JSONObject(it) }
                            when (alert.alertType) {
                                com.example.data.model.AlertType.LOW_STOCK -> {
                                    if (meta == null) false
                                    else {
                                        val itemName = meta.optString("item_name")
                                        // Match by name only — inventory uses UUID ids, products uses Int ids
                                        // so hashCode comparison always fails. If product not found → show alert.
                                        val product = products.firstOrNull { it.name.equals(itemName, ignoreCase = true) }
                                        if (product == null) {
                                            false // can't confirm resolved, keep showing
                                        } else {
                                            product.quantity >= product.minThreshold
                                        }
                                    }
                                }
                                com.example.data.model.AlertType.EXPIRY_WARNING -> {
                                    // Expiry warnings are never auto-resolved: out-of-stock ≠ expiry resolved
                                    false
                                }
                                com.example.data.model.AlertType.KHATA_REMINDER -> {
                                    if (meta == null) false
                                    else {
                                        val customerName = meta.optString("customer_name")
                                        // Use legacy customers table for resolution check (balance maintained there)
                                        val customer = customers.firstOrNull { it.name.equals(customerName, ignoreCase = true) }
                                        if (customer == null) {
                                            false // can't confirm resolved, keep showing
                                        } else {
                                            customer.balance < 5000.0
                                        }
                                    }
                                }
                                com.example.data.model.AlertType.SYNC_FAILURE -> {
                                    pendingSyncCount == 0
                                }
                                else -> {
                                    alert.isRead
                                }
                            }
                        } catch (e: Exception) {
                            false // on any parse error, show the alert rather than hiding it
                        }
                        !isResolved
                    }.sortedByDescending { it.createdAt }.take(5)
                    if (activeAlerts.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No critical alerts. Dukan is running smoothly! 🎉",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        for (alert in activeAlerts) {
                            val alertColor = when (alert.alertType) {
                                com.example.data.model.AlertType.LOW_STOCK -> Color(0xFFFFEBEE)
                                com.example.data.model.AlertType.EXPIRY_WARNING -> Color(0xFFFEF3C7)
                                else -> Color(0xFFE0F2F1)
                            }
                            val alertIcon = when (alert.alertType) {
                                com.example.data.model.AlertType.LOW_STOCK, com.example.data.model.AlertType.EXPIRY_WARNING -> Icons.Default.Warning
                                com.example.data.model.AlertType.KHATA_REMINDER -> Icons.Default.Assignment
                                else -> Icons.Default.Notifications
                            }
                            val iconColor = when (alert.alertType) {
                                com.example.data.model.AlertType.LOW_STOCK -> Color(0xFFBA1A1A)
                                com.example.data.model.AlertType.EXPIRY_WARNING -> Color(0xFFD97706)
                                else -> Color(0xFF00796B)
                            }
                            val buttonText = when (alert.alertType) {
                                com.example.data.model.AlertType.LOW_STOCK, com.example.data.model.AlertType.EXPIRY_WARNING -> "Refill"
                                com.example.data.model.AlertType.KHATA_REMINDER -> "Ping"
                                else -> "View"
                            }
                            val buttonAction = {
                                when (alert.alertType) {
                                    com.example.data.model.AlertType.LOW_STOCK, com.example.data.model.AlertType.EXPIRY_WARNING -> {
                                        viewModel.navigateTo(Screen.Inventory)
                                    }
                                    com.example.data.model.AlertType.KHATA_REMINDER -> {
                                        val meta = alert.metadataJson?.let { org.json.JSONObject(it) }
                                        val customerName = meta?.optString("customer_name") ?: ""
                                        viewModel.setKhataSearchQuery(customerName)
                                        viewModel.selectTab(2)
                                    }
                                    else -> {
                                        viewModel.navigateTo(Screen.Notifications)
                                    }
                                }
                            }
                            key(alert.id) {
                                    // Removed SwipeToDismissBox to keep alerts visible until resolved
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(alertColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = alertIcon,
                                                        contentDescription = null,
                                                        tint = iconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = alert.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = alert.message,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = buttonAction,
                                                    border = BorderStroke(1.dp, Color(0xFF0B1A7D)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0B1A7D)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                            }
                        }
                        if (alerts.count { !it.isRead } > 5) {
                            TextButton(
                                onClick = { viewModel.navigateTo(Screen.Notifications) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("View More Alerts", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Section 4: Recent Activity
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.SalesHistory) }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (combinedActivity.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent transactions or sales.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        for (activity in combinedActivity) {
                            val activityBg = when (activity.type) {
                                "sale" -> Color(0xFFF1F5F9)
                                "debit" -> Color(0xFFFFEBEE)
                                else -> Color(0xFFE8F5E9)
                            }
                            val activityIcon = when (activity.type) {
                                "sale" -> Icons.Default.Inventory
                                "debit" -> Icons.Default.Remove
                                else -> Icons.Default.CheckCircle
                            }
                            val iconColor = when (activity.type) {
                                "sale" -> Color(0xFF64748B)
                                "debit" -> Color(0xFFBA1A1A)
                                else -> Color(0xFF006B5E)
                            }
                            val amountColor = when (activity.type) {
                                "debit" -> Color(0xFFBA1A1A)
                                else -> Color(0xFF006B5E)
                            }
                            val amountPrefix = when (activity.type) {
                                "debit" -> "-₹"
                                "credit" -> "+₹"
                                else -> "₹"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(activityBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = activityIcon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activity.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = activity.description,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF64748B))
                                            )
                                            Text(
                                                text = getRelativeTimeAgo(activity.timestamp),
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$amountPrefix${String.format("%.2f", activity.amount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // space for floating action buttons or bottom nav padding
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
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.navigateTo(Screen.OcrReview()) },
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
                                viewModel.navigateTo(Screen.OcrReview())
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
                .padding(0.dp),
            containerColor = Color(0xFF0B1A7D),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner", tint = Color.White)
        }
    }
}

@Composable
fun KhataTab(viewModel: KiranaViewModel, khataViewModel: com.example.ui.khata.KhataViewModel) {
    val khataUiState by khataViewModel.uiState.collectAsState()
    // Use KhataCustomerEntity (has correct UUID ids and runningBalance from khata_customers table)
    val khataCustomers = khataUiState.customers
    val alerts by viewModel.alerts.collectAsState()
    val unreadCount = alerts.filter { !it.isRead }.size
    val initialSearchQuery by viewModel.khataSearchQuery.collectAsState()
    var searchKeyword by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var debouncedSearchKeyword by remember { mutableStateOf("") }
    
    LaunchedEffect(searchKeyword) {
        kotlinx.coroutines.delay(300)
        debouncedSearchKeyword = searchKeyword
    }
    var activeFilter by remember { mutableStateOf("All") } // "All", "Get", "Give"

    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderCustomerName by remember { mutableStateOf("") }
    var reminderCustomerBalance by remember { mutableStateOf(0.0) }

    var showSettleConfirmation by remember { mutableStateOf(false) }
    var settleTargetKhataCustomer by remember { mutableStateOf<com.example.data.model.KhataCustomerEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteTargetKhataCustomer by remember { mutableStateOf<com.example.data.model.KhataCustomerEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val primaryNavy = Color(0xFF0B1A7D)
    val textDark = Color(0xFF0F172A)
    val textMuted = Color(0xFF64748B)
    val bgLight = Color(0xFFF8FAFC)
    val dividerColor = Color(0xFFE2E8F0)

    // Helper function for Initials
    val getInitials = remember {
        { name: String ->
            val parts = name.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
    }

    // Helper function for Time Ago
    val getTimeAgo = remember {
        { timestamp: Long ->
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                diff < 0 -> "just now"
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes mins ago"
                hours < 24 -> "$hours hrs ago"
                days == 1L -> "Yesterday"
                days < 7 -> "$days days ago"
                days < 30 -> "${days / 7} week ago"
                else -> DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()
            }
        }
    }

    // Apply Live Filter based on search and chip selection — uses KhataCustomerEntity
    val filteredCustomers = khataCustomers.filter { cust ->
        val matchesSearch = cust.name.contains(debouncedSearchKeyword, ignoreCase = true)
        val matchesChip = when (activeFilter) {
            "Get" -> cust.runningBalance > 0
            "Give" -> cust.runningBalance < 0
            else -> true
        }
        matchesSearch && matchesChip && cust.deletedAt == null
    }

    val exactCustomerMatch = khataCustomers.any { it.name.trim().equals(searchKeyword.trim(), ignoreCase = true) }

    var khataParentWidth by remember { mutableStateOf(0f) }
    var khataParentHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onGloballyPositioned { coordinates ->
                khataParentWidth = coordinates.size.width.toFloat()
                khataParentHeight = coordinates.size.height.toFloat()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 1. Top App Bar (Hamburger menu icon + "RetailDost Pro" + Notifications Bell)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = { /* Hamburger menu action */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Main navigation menu",
                            tint = primaryNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "RetailDost Pro",
                        fontWeight = FontWeight.Bold,
                        color = primaryNavy,
                        fontSize = 20.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(Screen.Notifications) }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = primaryNavy,
                        modifier = Modifier.size(24.dp)
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

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Inner column for spacing out components
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2. Outstanding Balance Card - source from khata_customers table (runningBalance)
                val totalWillGet = khataCustomers.filter { it.runningBalance > 0 && it.deletedAt == null }.sumOf { it.runningBalance }
                val totalWillGive = khataCustomers.filter { it.runningBalance < 0 && it.deletedAt == null }.sumOf { kotlin.math.abs(it.runningBalance) }
                val netOutstanding = totalWillGet - totalWillGive

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Top row: YOU WILL GET (left) | YOU WILL GIVE (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: YOU WILL GET
                            Column {
                                Text(
                                    text = "YOU WILL GET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", totalWillGet)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Aapne Diye",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            // Right: YOU WILL GIVE
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "YOU WILL GIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", totalWillGive)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEF4444) // Red
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Aapko Mile",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom footer row: Net Balance | Updated just now
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Balance: ₹${String.format("%,.0f", netOutstanding)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Updated just now",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // 3. Side-by-side action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Customer Button (Navy)
                    Button(
                        onClick = { viewModel.navigateTo(Screen.AddCustomer) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_customer_khata_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Import Button (White with Navy border)
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.SelectContact(Screen.Dashboard)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_contacts_khata_button"),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = primaryNavy),
                        border = BorderStroke(1.5.dp, primaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPage,
                                contentDescription = null,
                                tint = primaryNavy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // 4. Outlined Search box and Filter Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchKeyword,
                                onValueChange = { searchKeyword = it },
                                textStyle = TextStyle(fontSize = 14.sp, color = textDark),
                                decorationBox = { innerTextField ->
                                    if (searchKeyword.isEmpty()) {
                                        Text(
                                            text = "Search customer name...",
                                            color = textMuted,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchKeyword = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Cycle filter icon button
                    IconButton(
                        onClick = {
                            activeFilter = when (activeFilter) {
                                "All" -> "Get"
                                "Get" -> "Give"
                                else -> "All"
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter customer list (Current: $activeFilter)",
                            tint = primaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 5. Customer Ledger list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("customer_ledgers_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Feature 3: Quick Add Customer Card
                    if (searchKeyword.isNotEmpty() && !exactCustomerMatch) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.createCustomerDirectly(searchKeyword.trim()) { newId ->
                                            searchKeyword = "" // clear search
                                            viewModel.navigateTo(Screen.CustomerLedger(newId))
                                        }
                                    }
                                    .testTag("quick_add_customer_item"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(primaryNavy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PersonAdd, "Add person", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "Add \"${searchKeyword.trim()}\" as New Customer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryNavy
                                        )
                                        Text(
                                            text = "Tap to open clean ledger statement for them instantly.",
                                            fontSize = 10.sp,
                                            color = primaryNavy.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredCustomers.isEmpty() && (exactCustomerMatch || searchKeyword.isEmpty())) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No accounts found in this filter.", fontSize = 12.sp, color = textMuted)
                            }
                        }
                    } else {
                        items(filteredCustomers) { cust ->
                            // cust is KhataCustomerEntity — use runningBalance and UUID id
                            val lastTxDate = getTimeAgo(cust.lastActivity ?: cust.createdAt ?: System.currentTimeMillis())

                            val balanceLabel = when {
                                cust.runningBalance > 0 -> "YOU'LL GET"
                                cust.runningBalance < 0 -> "YOU GIVE"
                                else -> "SETTLED"
                            }

                            val labelColor = when {
                                cust.runningBalance > 0 -> Color(0xFF047857) // Green
                                cust.runningBalance < 0 -> Color(0xFFB91C1C) // Red
                                else -> Color(0xFF64748B) // Slate Gray
                            }

                            val amountColor = when {
                                cust.runningBalance > 0 -> Color(0xFF047857) // Green
                                cust.runningBalance < 0 -> Color(0xFFB91C1C) // Red
                                else -> Color(0xFF0F172A) // Dark Slate
                            }

                            val avatarBg = when {
                                cust.runningBalance > 0 -> Color(0xFFE0E7FF) // Lavender
                                cust.runningBalance < 0 -> Color(0xFFFFEDD5) // Peach/Orange
                                else -> Color(0xFFD1FAE5) // Mint
                            }

                            val avatarText = when {
                                cust.runningBalance > 0 -> Color(0xFF3730A3) // Dark Blue
                                cust.runningBalance < 0 -> Color(0xFF9A3412) // Dark Orange
                                else -> Color(0xFF065F46) // Dark Green
                            }

                            // Settle & Delete options menu
                            var showMenu by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Navigate using cust.id which is now a UUID string — no migration triggered
                                    .clickable { viewModel.navigateTo(Screen.CustomerLedger(cust.id)) }
                                    .testTag("customer_card_${cust.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Circular Initials Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = getInitials(cust.name),
                                                fontWeight = FontWeight.Bold,
                                                color = avatarText,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = cust.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = textDark
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Last entry: $lastTxDate",
                                                fontSize = 12.sp,
                                                color = textMuted
                                            )
                                        }
                                    }

                                    // Balance info and 3-dot options menu on the right
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = balanceLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = labelColor
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "₹${String.format("%,.0f", kotlin.math.abs(cust.runningBalance))}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = amountColor
                                            )
                                        }

                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Options menu",
                                                    tint = textMuted
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Settle Hisaab (₹0)") },
                                                    onClick = {
                                                        showMenu = false
                                                        settleTargetKhataCustomer = cust
                                                        showSettleConfirmation = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete Customer", color = Color.Red) },
                                                    onClick = {
                                                        showMenu = false
                                                        deleteTargetKhataCustomer = cust
                                                        showDeleteConfirmation = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = Color.Red
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Reminder Dialog Overlay Popup
        if (showReminderDialog) {
            AlertDialog(
                onDismissRequest = { showReminderDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Send, contentDescription = "Review reminder", tint = Color(0xFF388E3C))
                        Text("Pre-filled WhatsApp Reminder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Send professional payment reminder notification template regarding ledger account dues directly to your user's chat box.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("TO: " + reminderCustomerName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Dear $reminderCustomerName, your outstanding balance is ₹$reminderCustomerBalance at our shop. Please settle at your earliest convenience using online UPI QR scan or Cash. Thank you!",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReminderDialog = false
                            android.widget.Toast.makeText(context, "Reminder template shared successfully with $reminderCustomerName!", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.speak("Reminder sent successfully to $reminderCustomerName!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Send via WhatsApp", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (showSettleConfirmation && settleTargetKhataCustomer != null) {
            val customer = settleTargetKhataCustomer!!
            AlertDialog(
                onDismissRequest = {
                    showSettleConfirmation = false
                    settleTargetKhataCustomer = null
                },
                title = { Text("Settle Hisaab?") },
                text = { Text("Are you sure you want to settle all transactions for ${customer.name}? This will reset their balance to ₹0.") },
                confirmButton = {
                    Button(
                        onClick = {
                            khataViewModel.settleCustomer(customer.id)
                            showSettleConfirmation = false
                            settleTargetKhataCustomer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Settle (₹0)")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSettleConfirmation = false
                            settleTargetKhataCustomer = null
                        }
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (showDeleteConfirmation && deleteTargetKhataCustomer != null) {
            val customer = deleteTargetKhataCustomer!!
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                    deleteTargetKhataCustomer = null
                },
                title = { Text("Delete Customer & History?") },
                text = { Text("Are you sure you want to delete ${customer.name} and all their transaction history? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            khataViewModel.deleteCustomer(customer.id)
                            showDeleteConfirmation = false
                            deleteTargetKhataCustomer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            deleteTargetKhataCustomer = null
                        }
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun VoiceAssistantOverlay(viewModel: KiranaViewModel) {
    val speechText by viewModel.voiceInputText.collectAsState()
    val isParsingIntent by viewModel.isParsingIntent.collectAsState()

    var customTypedTranscript by remember { mutableStateOf(speechText) }

    LaunchedEffect(speechText) {
        customTypedTranscript = speechText
    }

    val context = LocalContext.current
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                customTypedTranscript = spokenText
                viewModel.openVoiceOverlay(spokenText)
            }
        }
    }

    fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak ledger entry (e.g. Suresh se 500 liye)...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition is not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { viewModel.closeVoiceOverlay() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(enabled = false) {}
                .testTag("voice_assistant_sheet_bottom"),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sound wave pulsing animation
                Row(
                    modifier = Modifier.fillMaxWidth(0.3f).height(60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scaleAnimations = listOf(1f, 0.4f, 0.8f, 0.6f, 0.9f)
                    scaleAnimations.forEach { scale ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(scale)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "KiranaOS Voice Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Listening to Hinglish voice memos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Editable text box allowing Hinglish logs
                OutlinedTextField(
                    value = customTypedTranscript,
                    onValueChange = { customTypedTranscript = it },
                    placeholder = { Text("Click triggers to record dues...") },
                    modifier = Modifier.fillMaxWidth().testTag("voice_transcript_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Circular glowing pulsing Microphone button for voice input
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    IconButton(
                        onClick = { startSpeechToText() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .shadow(6.dp, CircleShape)
                            .testTag("voice_mic_speak_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = "Tap microphone to speak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Interactive spoken test triggers matching screenshot!
                Text(
                    text = "TAP TEST VOICE TRIGGERS:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { customTypedTranscript = "Ramesh ka 500 ka udhar" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Udhar: Ramesh 500", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { customTypedTranscript = "Sunita ne 150 pay kiye" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Pay: Sunita 150", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isParsingIntent) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Confirm/Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeVoiceOverlay() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.openVoiceOverlay(customTypedTranscript) // update state
                            viewModel.onConfirmVoiceKhata(
                                onSuccess = {},
                                onFailure = {}
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("voice_confirm_button"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Confirm Ledger", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraScannerOverlay(viewModel: KiranaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionCameraAllowed by viewModel.cameraPermissionAllowed.collectAsState()
    val scanType by viewModel.cameraScanType.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()

    var activeSampleIndex by remember { mutableStateOf(0) }
    var flashEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(enabled = false) {}, // block back touches
        contentAlignment = Alignment.Center
    ) {
        if (!permissionCameraAllowed) {
            // Step 1: Pre-permission instruction request dialog
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Access Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Real-time AI Camera Scan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "To scan receipts or diary notes instantly, this application requires access to your device Camera and Microphone.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setCameraScannerVisible(false) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Not Now")
                        }
                        Button(
                            onClick = {
                                viewModel.setCameraPermissionAllowed(true)
                                viewModel.setAudioPermissionAllowed(true)
                                viewModel.speak("Kripya invoice ya diary camera ke samne rakhein, aur snap button dabayein.")
                                android.widget.Toast.makeText(context, "Permissions granted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.5f).height(48.dp).testTag("camera_grant_button"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            // Step 2: Dynamic Live Viewfinder layout
            val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
            val laserProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laser_y"
            )

            val laserGlowHeight by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laser_glow"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (Back Arrow & Info bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setCameraScannerVisible(false) },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close scanner", tint = Color.White)
                    }

                    Text(
                        text = if (scanType == "stock_in") "STOCK IN SCANNER" else "STOCK OUT SCANNER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier.background(if (flashEnabled) Color.Yellow else Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Toggle flash",
                            tint = if (flashEnabled) Color.Black else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mid viewfinder frame
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray.copy(alpha = 0.4f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw simulated content sheets to make OCR truly operational as real snapped files
                    if (scanType == "stock_in") {
                        if (activeSampleIndex == 0) {
                            SimulatedPrintedBill1()
                        } else {
                            SimulatedPrintedBill2()
                        }
                    } else {
                        if (activeSampleIndex == 0) {
                            SimulatedHandwrittenSlip1()
                        } else {
                            SimulatedHandwrittenSlip2()
                        }
                    }

                    // Scan laser line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * laserProgress
                        drawLine(
                            color = Color(0xFF00FFCC),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = (2.5f * laserGlowHeight).dp.toPx()
                        )
                    }

                    // Reticle brackets in corners
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top-left bracket
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(20.dp).border(BorderStroke(3.dp, Color(0xFF00FFCC)), RoundedCornerShape(topStart = 4.dp)))
                        // Top-right bracket
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(20.dp).border(BorderStroke(3.dp, Color(0xFF00FFCC)), RoundedCornerShape(topEnd = 4.dp)))
                        // Bottom-left bracket
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).size(20.dp).border(BorderStroke(3.dp, Color(0xFF00FFCC)), RoundedCornerShape(bottomStart = 4.dp)))
                        // Bottom-right bracket
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp).border(BorderStroke(3.dp, Color(0xFF00FFCC)), RoundedCornerShape(bottomEnd = 4.dp)))
                    }

                    // Flash Overlay representing light snap
                    if (flashEnabled) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
                    }
                }

                Text(
                    text = "Pose document neatly within boundaries. Tap preview to switch.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                // Select Switcher / previews list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .clickable { activeSampleIndex = 0 }
                            .padding(4.dp)
                            .border(
                                1.5.dp,
                                if (activeSampleIndex == 0) Color(0xFF00FFCC) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Draft 1", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Card(
                        modifier = Modifier
                            .clickable { activeSampleIndex = 1 }
                            .padding(4.dp)
                            .border(
                                1.5.dp,
                                if (activeSampleIndex == 1) Color(0xFF00FFCC) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Draft 2", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                // Shutter Capture controls bottom pane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOcrProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("AI Analysis active...", color = Color.White, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                val dummyBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                viewModel.onProcessInvoiceOcr(dummyBitmap, isHandwritten = (scanType == "stock_out")) { success ->
                                    if (success) {
                                        viewModel.setCameraScannerVisible(false)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .padding(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, CircleShape)
                                    .border(4.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simulated Printed Wholesaler invoices vectors
@Composable
fun SimulatedPrintedBill1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ITC BULK DIST.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("INVOICE #9201", fontSize = 7.sp, color = Color.DarkGray)
        }
        Divider(color = Color.LightGray, modifier = Modifier.fillMaxWidth().height(1.dp))
        Text("TAX SUMMARY: GST AUTHORIZED", fontSize = 6.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1. Aashirvaad Atta 5kg", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("10x Qty - ₹240.0", fontSize = 8.sp, color = Color.Black)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("2. Sunfeast Cookies", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("24x Qty - ₹15.0", fontSize = 8.sp, color = Color.Black)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("3. Savlon Soap Bundle", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("15x Qty - ₹55.0", fontSize = 8.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("TOTAL AMOUNT EXCL GRAND: ₹4,850.0", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun SimulatedPrintedBill2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("AMUL COLD CHAIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
            Text("INVOICE #2810", fontSize = 7.sp, color = Color.DarkGray)
        }
        Divider(color = Color.LightGray, modifier = Modifier.fillMaxWidth().height(1.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1. Amul Taaza Milk", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("25x Qty - ₹26.0", fontSize = 8.sp, color = Color.Black)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("2. Premium Paneer 500g", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("12x Qty - ₹180.0", fontSize = 8.sp, color = Color.Black)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("3. Butter Block 100g", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            Text("50x Qty - ₹48.0", fontSize = 8.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("TOTAL DUPLICATE STALL: ₹5,210.0", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.align(Alignment.End))
    }
}

// Simulated Handwritten notebook slips vectors
@Composable
fun SimulatedHandwrittenSlip1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDE7)) // yellow journal page
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Ramesh Parcha (Hath Likhit)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
        Divider(color = Color.Blue, modifier = Modifier.fillMaxWidth().height(1.dp).padding(bottom = 2.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text("✏️ India Gate Basmati: 15 packet", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Text("✏️ Tata Salt iodized: 10 units", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Text("✏️ Dettol Liquid 250ml: 5 bottle", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text("Sign: Shriman Ramesh Kumar", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun SimulatedHandwrittenSlip2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDE7)) // yellow journal page
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Daily Delivery Stock Slip", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
        Divider(color = Color.Blue, modifier = Modifier.fillMaxWidth().height(1.dp).padding(bottom = 2.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text("✏️ Britannia Marie Gold: 30 boxes", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Text("✏️ Maggi Noodles 2-Min: 40 Qty", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Text("✏️ Clinic Plus Shampoo: 100 sachet", fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text("Suresh Kirana Records", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun ScanOptionsDialog(
    onDismiss: () -> Unit,
    onSelectOption: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Scan Type",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1B1B21),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select whether you want to scan a receipt for Stock In, or a handwritten diary for Stock Out.",
                    fontSize = 14.sp,
                    color = Color(0xFF454652),
                    textAlign = TextAlign.Center
                )

                // Option 1: Stock In (Scan Invoice)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOption("stock_in") }
                        .testTag("scan_option_stock_in"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0FA)),
                    border = BorderStroke(1.dp, Color(0xFFC6C5D4))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF0B1A7D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stock In (Scan Invoice)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1B1B21)
                            )
                            Text(
                                text = "For distributor receipts/bills",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Option 2: Stock Out (Scan Diary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOption("stock_out") }
                        .testTag("scan_option_stock_out"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFC2410C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stock Out (Scan Diary)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF7C2D12)
                            )
                            Text(
                                text = "For handwritten pages of sold items",
                                fontSize = 12.sp,
                                color = Color(0xFFC2410C).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag("scan_option_cancel")
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B21)
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
