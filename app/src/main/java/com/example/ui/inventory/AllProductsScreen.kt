package com.example.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.data.model.InventoryEntity
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import com.example.ui.inventory.InventoryViewModel
import com.example.utils.AutoResizingText

/**
 * AllProductsScreen
 * ─────────────────
 * Professional product catalog screen displaying all stocks in a grid of card components.
 * Matches Stitch specifications: 1/3rd top image, 2/3rds product details, premium borders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllProductsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
    kiranaViewModel: KiranaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Stitch Design Colors
    val primaryColor = Color(0xFF0B1A7D)
    val backgroundBg = Color(0xFFFBF8FF)
    val onSurface = Color(0xFF1B1B21)
    val onSurfaceVariant = Color(0xFF454652)
    val outlineVariant = Color(0xFFC6C5D4)
    val surfaceContainerLow = Color(0xFFF5F2FB)
    val surfaceContainerHighest = Color(0xFFE4E1EA)
    val errorColor = Color(0xFFBA1A1A)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)
    val secondaryContainer = Color(0xFF94F0DF)
    val onSecondaryContainer = Color(0xFF006F62)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Stock Section",
                            tint = onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBg)
                .padding(innerPadding)
        ) {
            // Search Input with premium grey background
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search all products", fontSize = 15.sp, color = outlineVariant) },
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp)
            )

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No products in stock",
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items) { item ->
                        ProductGridCard(
                            item = item,
                            primaryColor = primaryColor,
                            onSurface = onSurface,
                            onSurfaceVariant = onSurfaceVariant,
                            outlineVariant = outlineVariant,
                            surfaceContainerLow = surfaceContainerLow,
                            errorContainer = errorContainer,
                            onErrorContainer = onErrorContainer,
                            secondaryContainer = secondaryContainer,
                            onSecondaryContainer = onSecondaryContainer,
                            onCardClick = {
                                kiranaViewModel.navigateTo(Screen.InventoryDetail(item.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    item: InventoryEntity,
    primaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    outlineVariant: Color,
    surfaceContainerLow: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    onCardClick: () -> Unit
) {
    val lowStock = item.quantity <= item.minThreshold

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
            .height(260.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top 1/3rd is the image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = item.itemName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = outlineVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom 2/3rds is the product details
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AutoResizingText(
                    text = item.itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    modifier = Modifier.weight(1f)
                )

                AutoResizingText(
                    text = "${item.category ?: "General"} • ${item.batchNo ?: "FMCG"}",
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                )

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (lowStock) errorContainer else secondaryContainer,
                    contentColor = if (lowStock) onErrorContainer else onSecondaryContainer,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    AutoResizingText(
                        text = "Stock: ${item.quantity.toInt()} ${item.unitLabel ?: "pcs"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoResizingText(
                        text = "₹${item.mrp ?: 0.0}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}
