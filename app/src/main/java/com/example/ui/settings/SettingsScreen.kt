package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.KiranaViewModel
import com.example.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: KiranaViewModel, onLogout: () -> Unit = {}) {
    val storeName by viewModel.storeName.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val pincode by viewModel.pincode.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    val storeLocation by viewModel.storeLocation.collectAsState()
    val businessHours by viewModel.businessHours.collectAsState()
    val gstDetails by viewModel.gstDetails.collectAsState()

    var showSyncWarningDialog by remember { mutableStateOf(false) }

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationInput by remember(storeLocation) { mutableStateOf(storeLocation) }

    var showHoursDialog by remember { mutableStateOf(false) }
    var hoursInput by remember(businessHours) { mutableStateOf(businessHours) }

    var showGstDialog by remember { mutableStateOf(false) }
    var gstInput by remember(gstDetails) { mutableStateOf(gstDetails) }

    var showFaqDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced status info icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // User Bento-style profile card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUMy1G-7L3chtuF4TiMsGQ40MV12V3MVqwWwH6dSwzdrt1tJBDtQhQGA73OdTY_jdunP6LDdm3_MZV_7djDx7H5i2wmkvFALCGB656VykXhvDQBC38HP_Gf2su70SepAL-7VN-JyPHy9-GTrW-888dNe8n1IeHJcW1R_5girplZTDp3NWTEOfpwwbbymc9NKmgo_6Di0AYy75qhRaxXaEINJ2T7d0Zi8KmQ1_Mp12S-x1gql2wTMPCLdT6lEfjL9CY9NW5JyJrVE8",
                        contentDescription = "Suresh Bhai portrait photo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ownerName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }

            // Cloud Sync Status & Manual Sync Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (pendingSyncCount > 0) 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) 
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (pendingSyncCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (pendingSyncCount > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = "Sync status icon",
                            tint = if (pendingSyncCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Cloud Backup & Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (pendingSyncCount > 0) "$pendingSyncCount changes pending sync" else "All changes saved to cloud",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pendingSyncCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.triggerSync() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pendingSyncCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sync Now", fontSize = 12.sp)
                    }
                }
            }



            // Section 1: STORE SETUP
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STORE SETUP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Default.LocationOn,
                            title = "Store Location",
                            subtitle = if (storeLocation.isEmpty()) ("Pincode: " + if (pincode.isEmpty()) "400001" else pincode) else storeLocation,
                            onClick = { showLocationDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRowItem(
                            icon = Icons.Default.Schedule,
                            title = "Business Hours",
                            subtitle = businessHours,
                            onClick = { showHoursDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRowItem(
                            icon = Icons.Default.ReceiptLong,
                            title = "GST Details",
                            subtitle = if (gstDetails.isEmpty()) "Add GSTIN number" else gstDetails,
                            onClick = { showGstDialog = true }
                        )
                    }
                }
            }

            // Section 2: PREFERENCES
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // Notifications toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Expiry & Low Stock updates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications() }
                            )
                        }
                    }
                }
            }

            // Section 3: SUPPORT
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUPPORT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Default.SupportAgent,
                            title = "Contact Support",
                            subtitle = "Call +91 9082496721",
                            onClick = { showSupportDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRowItem(
                            icon = Icons.Default.HelpOutline,
                            title = "FAQ",
                            subtitle = "Read setup help docs",
                            onClick = { showFaqDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Logout action (revert back to splash onboarding)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (pendingSyncCount > 0) {
                                        showSyncWarningDialog = true
                                    } else {
                                        onLogout()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Exit to onboarding logout",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSyncWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSyncWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Cannot Logout", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "You have $pendingSyncCount unsynced changes. Logging out will result in permanent loss of this data.\n\nPlease sync your changes to the cloud before logging out.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerSync()
                        showSyncWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSyncWarningDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Edit Store Location", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location Address / Pincode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStoreLocation(locationInput)
                        showLocationDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHoursDialog) {
        AlertDialog(
            onDismissRequest = { showHoursDialog = false },
            title = { Text("Edit Business Hours", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Hours (e.g. 08:00 AM - 10:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessHours(hoursInput)
                        showHoursDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHoursDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGstDialog) {
        AlertDialog(
            onDismissRequest = { showGstDialog = false },
            title = { Text("Edit GST Details", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = gstInput,
                    onValueChange = { gstInput = it },
                    label = { Text("GSTIN Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateGstDetails(gstInput)
                        showGstDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGstDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Contact Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Need help with RetailDost? Reach out directly to our support assistants:")
                    Text(
                        text = "Phone: +91 9082496721",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSupportDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Frequently Asked Questions", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val faqs = listOf(
                        "How do I scan an invoice?" to "Go to the Stock tab, click 'Stock In (Scan Receipt)', and select a photo or take one. Our AI model will automatically read the items, quantities, and prices.",
                        "Can I record sales offline?" to "Yes! RetailDost works completely offline. All sales records are saved locally and synced automatically when you reconnect to the internet.",
                        "How do I manage customer credit limits?" to "Track credit and repayments under the Khata tab. Reminder alerts are displayed automatically when a customer's total outstanding balance exceeds ₹5,000.",
                        "How do I add GST Details?" to "Navigate to Settings -> Store Setup -> GST Details, click on it, and enter your store's valid GSTIN number to update your profile.",
                        "Where can I view store business analytics?" to "Under the Home tab dashboard, select the 'Analytics' section to view visual trends of sales, profits, and customer credit activity."
                    )
                    faqs.forEach { (question, answer) ->
                        Column {
                            Text(text = "Q: $question", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFaqDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = "$title Icon",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Chevron icon indicator",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
