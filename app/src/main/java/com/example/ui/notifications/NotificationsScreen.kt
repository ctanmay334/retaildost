package com.example.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import com.example.data.model.AlertEntity
import com.example.data.model.AlertType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: KiranaViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isReloading by viewModel.isReloadingAlerts.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Alerts, 2: Business

    // Color definitions
    val primaryIndigo = Color(0xFF0B1A7D)
    val textDark = Color(0xFF1B1B21)
    val onSurfaceVariant = Color(0xFF454652)
    val backgroundLight = Color(0xFFFBF8FF)
    val outlineVariant = Color(0xFFC6C5D4)
    val surfaceContainerLowest = Color(0xFFFFFFFF)

    val errorColor = Color(0xFFBA1A1A)
    val errorContainer = Color(0xFFFFDAD6)
    val secondaryTeal = Color(0xFF006B5E)
    val secondaryContainer = Color(0xFF94F0DF)
    val tertiaryContainer = Color(0xFF702A00)
    val tertiaryFixed = Color(0xFFFFDBCC)

    // Alerts are NOT auto-marked-as-read on screen open.
    // The user must explicitly press the "Mark all as read" (✓✓) button in the top bar.
    // Auto-reading was causing the unread badge to vanish immediately and alerts to disappear.

    // Filter logic based on tab choice and resolved status
    val filteredAlerts = remember(alerts, selectedTab, products, customers, pendingSyncCount) {
        val active = alerts.filter { alert ->
            val isResolved = try {
                val meta = alert.metadataJson?.let { org.json.JSONObject(it) }
                when (alert.alertType) {
                    AlertType.LOW_STOCK -> {
                        if (meta == null) false
                        else {
                            // Match by item_name only (products uses legacy ItemEntity with Int id,
                            // while alerts store InventoryEntity's UUID — hashCode comparison is unreliable)
                            val itemName = meta.optString("item_name")
                            val product = products.firstOrNull { it.name.equals(itemName, ignoreCase = true) }
                            // Resolved = product restocked above threshold
                            product != null && product.quantity >= product.minThreshold
                        }
                    }
                    AlertType.EXPIRY_WARNING -> {
                        // Expiry alerts are never "auto-resolved" in the UI — the item still needs to be sold/removed
                        false
                    }
                    AlertType.KHATA_REMINDER -> {
                        if (meta == null) false
                        else {
                            val customerName = meta.optString("customer_name")
                            val customer = customers.firstOrNull { it.name.equals(customerName, ignoreCase = true) }
                            // Resolved = customer's balance dropped below 5000
                            customer != null && customer.balance < 5000.0
                        }
                    }
                    AlertType.SYNC_FAILURE -> {
                        pendingSyncCount == 0
                    }
                    else -> {
                        alert.isRead
                    }
                }
            } catch (e: Exception) {
                false // On parse error, show the alert rather than hiding it
            }
            !isResolved
        }

        when (selectedTab) {
            1 -> active.filter { it.alertType == AlertType.LOW_STOCK || it.alertType == AlertType.EXPIRY_WARNING }
            2 -> active.filter { it.alertType == AlertType.KHATA_REMINDER || it.alertType == AlertType.SYNC_FAILURE || it.alertType == AlertType.OCR_RETRY }
            else -> active
        }
    }

    // Date grouping logic
    val now = System.currentTimeMillis()
    val dayInMillis = 24 * 60 * 60 * 1000L

    val todayAlerts = filteredAlerts.filter { now - it.createdAt < dayInMillis }
    val yesterdayAlerts = filteredAlerts.filter { now - it.createdAt >= dayInMillis && now - it.createdAt < 2 * dayInMillis }
    val olderAlerts = filteredAlerts.filter { now - it.createdAt >= 2 * dayInMillis }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = textDark, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textDark)
                    }
                },
                actions = {
                    if (isReloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            color = primaryIndigo,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.reloadAlerts() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload notifications", tint = primaryIndigo)
                        }
                    }
                    IconButton(onClick = { viewModel.markAllAlertsAsRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read", tint = primaryIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundLight)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(innerPadding)
        ) {
            // Tab Navigation header matching mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundLight)
                    .border(BorderStroke(1.dp, outlineVariant.copy(alpha = 0.5f)))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val tabs = listOf("All", "Alerts", "Business")
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Column(
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) primaryIndigo else onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(28.dp)
                                .background(if (isSelected) primaryIndigo else Color.Transparent)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredAlerts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No recent notifications.",
                                    color = onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Today section
                    if (todayAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        items(todayAlerts, key = { it.id }) { alert ->
                            NotificationAlertCard(
                                alert = alert,
                                viewModel = viewModel,
                                errorColor = errorColor,
                                errorContainer = errorContainer,
                                secondaryTeal = secondaryTeal,
                                secondaryContainer = secondaryContainer,
                                tertiaryContainer = tertiaryContainer,
                                tertiaryFixed = tertiaryFixed,
                                primaryIndigo = primaryIndigo
                            )
                        }
                    }

                    // Yesterday section
                    if (yesterdayAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Yesterday",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        items(yesterdayAlerts, key = { it.id }) { alert ->
                            NotificationAlertCard(
                                alert = alert,
                                viewModel = viewModel,
                                errorColor = errorColor,
                                errorContainer = errorContainer,
                                secondaryTeal = secondaryTeal,
                                secondaryContainer = secondaryContainer,
                                tertiaryContainer = tertiaryContainer,
                                tertiaryFixed = tertiaryFixed,
                                primaryIndigo = primaryIndigo
                            )
                        }
                    }

                    // Older section
                    if (olderAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Older",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        items(olderAlerts, key = { it.id }) { alert ->
                            NotificationAlertCard(
                                alert = alert,
                                viewModel = viewModel,
                                errorColor = errorColor,
                                errorContainer = errorContainer,
                                secondaryTeal = secondaryTeal,
                                secondaryContainer = secondaryContainer,
                                tertiaryContainer = tertiaryContainer,
                                tertiaryFixed = tertiaryFixed,
                                primaryIndigo = primaryIndigo
                            )
                        }
                    }
                }

                // Marketplace Promo Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(primaryIndigo, Color(0xFF1A237E))
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                                Text(
                                    text = "Grow your business",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Stock up on premium pulses. Trending in your locality right now.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFDFE0FF),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        viewModel.navigateTo(Screen.Marketplace)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Marketplace", color = primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = primaryIndigo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Vector background icon overlay
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationAlertCard(
    alert: AlertEntity,
    viewModel: KiranaViewModel,
    errorColor: Color,
    errorContainer: Color,
    secondaryTeal: Color,
    secondaryContainer: Color,
    tertiaryContainer: Color,
    tertiaryFixed: Color,
    primaryIndigo: Color
) {
    val dateString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(alert.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC6C5D4).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon customized by AlertType
            val iconBg = when (alert.alertType) {
                AlertType.LOW_STOCK -> errorContainer
                AlertType.EXPIRY_WARNING -> tertiaryFixed
                AlertType.KHATA_REMINDER -> secondaryContainer
                else -> Color(0xFFEEF2F6)
            }
            val iconTint = when (alert.alertType) {
                AlertType.LOW_STOCK -> errorColor
                AlertType.EXPIRY_WARNING -> Color(0xFF793105)
                AlertType.KHATA_REMINDER -> secondaryTeal
                else -> primaryIndigo
            }
            val iconVector = when (alert.alertType) {
                AlertType.LOW_STOCK -> Icons.Default.Warning
                AlertType.EXPIRY_WARNING -> Icons.Default.HourglassEmpty
                AlertType.KHATA_REMINDER -> Icons.Default.AccountBalanceWallet
                AlertType.SYNC_FAILURE -> Icons.Default.SyncProblem
                AlertType.OCR_RETRY -> Icons.Default.CameraAlt
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeText = when (alert.alertType) {
                        AlertType.LOW_STOCK -> "LOW STOCK"
                        AlertType.EXPIRY_WARNING -> "EXPIRY WARNING"
                        AlertType.KHATA_REMINDER -> "PENDING KHATA"
                        AlertType.OCR_RETRY -> "OCR RETRY"
                        AlertType.SYNC_FAILURE -> "SYNC FAILURE"
                    }
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = iconTint,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )

                // Contextual CTA button matching mockup
                Spacer(modifier = Modifier.height(10.dp))
                when (alert.alertType) {
                    AlertType.LOW_STOCK -> {
                        Button(
                            onClick = {
                                viewModel.selectTab(1) // Navigate to Stock tab
                                viewModel.navigateTo(Screen.Dashboard)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Restock Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    AlertType.EXPIRY_WARNING -> {
                        Button(
                            onClick = {
                                viewModel.selectTab(1)
                                viewModel.navigateTo(Screen.Dashboard)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("View Inventory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    AlertType.KHATA_REMINDER -> {
                        Button(
                            onClick = {
                                val meta = alert.metadataJson?.let { org.json.JSONObject(it) }
                                val customerName = meta?.optString("customer_name") ?: ""
                                viewModel.setKhataSearchQuery(customerName)
                                viewModel.selectTab(2) // Go to Khata
                                viewModel.navigateTo(Screen.Dashboard)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Send Reminder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    AlertType.SYNC_FAILURE -> {
                        Button(
                            onClick = {
                                viewModel.triggerSync()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Sync Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
