package com.example.ui.marketplace

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KiranaViewModel
import com.example.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorRegistrationScreen(viewModel: KiranaViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Staples") }
    var phone by remember { mutableStateOf("") }
    var whatsappNo by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var serviceRegionsStr by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var minOrderValueStr by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }

    val categories = listOf("Staples", "Dairy", "Snacks", "Cleaning", "Beverages", "Personal Care")
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register as Supplier", fontWeight = FontWeight.Bold, color = Color(0xFF0F1B85)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Marketplace) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F1B85)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F7FD)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Join our Distributor Directory",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F1B85),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect with thousands of local Kirana stores. Provide your business details below to get registered.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Business name field
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name *") },
                        placeholder = { Text("e.g. Balaji Wholesalers") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Owner name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Owner / Representative Name *") },
                        placeholder = { Text("e.g. Rajesh Kumar") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category picker
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Product Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Contact phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone *") },
                        placeholder = { Text("e.g. +919876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // WhatsApp number
                    OutlinedTextField(
                        value = whatsappNo,
                        onValueChange = { whatsappNo = it },
                        label = { Text("WhatsApp Number *") },
                        placeholder = { Text("e.g. +919876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Headquarter Pincode
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = { Text("Headquarter Pincode *") },
                        placeholder = { Text("e.g. 400001") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Service areas / regions
                    OutlinedTextField(
                        value = serviceRegionsStr,
                        onValueChange = { serviceRegionsStr = it },
                        label = { Text("Service Regions (Comma Separated Pincodes) *") },
                        placeholder = { Text("e.g. 400001, 400002, 400003") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Business Address") },
                        placeholder = { Text("Shop or warehouse address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    // Min Order Value
                    OutlinedTextField(
                        value = minOrderValueStr,
                        onValueChange = { minOrderValueStr = it },
                        label = { Text("Minimum Order Value (INR)") },
                        placeholder = { Text("e.g. 2500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A green verified badge will be awarded post administrative check.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    // Validations
                    if (businessName.isBlank() || name.isBlank() || phone.isBlank() || whatsappNo.isBlank() || pincode.isBlank() || serviceRegionsStr.isBlank()) {
                        Toast.makeText(context, "Please fill in all required (*) fields.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val serviceList = serviceRegionsStr.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (serviceList.isEmpty()) {
                        Toast.makeText(context, "Please provide at least one valid service region.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val minOrder = minOrderValueStr.toDoubleOrNull() ?: 0.0

                    isLoading = true
                    viewModel.registerDistributor(
                        name = name,
                        businessName = businessName,
                        category = category,
                        phone = phone,
                        whatsappNo = whatsappNo,
                        pincode = pincode,
                        serviceRegions = serviceList,
                        address = address.ifBlank { null },
                        minOrderValue = minOrder,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Supplier Registration Submitted successfully!", Toast.LENGTH_LONG).show()
                            viewModel.navigateTo(Screen.Marketplace)
                        },
                        onFailure = { err ->
                            isLoading = false
                            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B85)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
