package com.example.ui.marketplace

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(viewModel: KiranaViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("Nearby") }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val isEmbedded = currentScreen is Screen.Dashboard

    // Premium Color Palette from Stitch Mockups
    val primaryIndigo = Color(0xFF0B1A7D)
    val primaryContainer = Color(0xFF283593)
    val onPrimaryContainer = Color(0xFF9AA5FF)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerHighest = Color(0xFFE4E1EA)
    val onSurfaceVariant = Color(0xFF454652)
    val secondary = Color(0xFF006B5E)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFF94F0DF)
    val onSecondaryContainer = Color(0xFF006F62)
    val tertiaryFixed = Color(0xFFFFDBCC)
    val onTertiaryFixed = Color(0xFF351000)
    val tertiary = Color(0xFF4C1A00)
    val outlineVariant = Color(0xFFC6C5D4)
    val textDark = Color(0xFF1B1B21)
    val backgroundLight = Color(0xFFFBF8FF)

    val dbDistributors by viewModel.distributors.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchDistributors()
    }

    val distributorsList = dbDistributors.map { dist ->
        val idVal = dist.id.toIntOrNull() ?: when(dist.businessName) {
            "Vikram Seth Enterprises", "Sharma Staples & Grains Wholesalers" -> 1
            "Balaram Spices & Flour Mills" -> 2
            "Apex Beverage Logistics" -> 3
            "Royal Dairy Distributors", "Mehta Dairy & Beverages" -> 4
            "Krishna Oil & Ghee Traders" -> 5
            "Golden Harvest Pulses" -> 6
            "National Confectionery Wholesalers" -> 7
            "Supreme Hygiene & Detergents", "Gupta Cleaning & Personal Care Agencies" -> 8
            "Mumbai Premium Dry Fruits" -> 9
            "Modern Packaged Foods" -> 10
            "Super Fresh Tea & Coffee Co." -> 11
            "Gourmet Bakery & Snacks", "Patel Packaged Snacks & Confectionery" -> 12
            "Balaji Agro & Staples" -> 13
            "Narayana Beverage & Snacks" -> 14
            "Pooja Cosmetics & Toiletries" -> 15
            "Shree Ganesha Dairy & Sweets" -> 16
            "National Spices & Condiments" -> 17
            "Hindustan Premium Packaging" -> 18
            else -> 1
        }
        val rating = when(idVal.toString()) {
            "1" -> "4.9"
            "2" -> "4.6"
            "3" -> "4.7"
            "4" -> "4.8"
            "5" -> "4.5"
            "6" -> "4.4"
            "7" -> "4.3"
            "8" -> "4.6"
            "9" -> "4.9"
            "10" -> "4.7"
            "11" -> "4.5"
            "12" -> "4.6"
            "13" -> "4.8"
            "14" -> "4.5"
            "15" -> "4.7"
            "16" -> "4.6"
            "17" -> "4.9"
            "18" -> "4.4"
            else -> "4.5"
        }
        val promo = when(idVal.toString()) {
            "1" -> "5% extra cash margin on top of raw Basmati bags!"
            "2" -> "Extra 50 sachet tea masala sample on every bulk spices box!"
            "3" -> "Buy 10 crates of Sprite, get 1 crate of Kinley Soda free!"
            "4" -> "Next-day delivery guaranteed or flat 2% off invoice value!"
            "5" -> "Slab scheme: Orders above ₹15,000 get 3% instant cash discount!"
            "6" -> "Free custom shop branding banner on ordering 500kg of mixed dals!"
            "7" -> "12% high margins on new Cadbury assortments!"
            "8" -> "Get 1 dozen free Vim bars with every case of Surf Excel liquid!"
            "9" -> "Premium vacuum packaging included free for festive boxes!"
            "10" -> "Scratch card inside every case of Maggi noodles - win up to ₹500!"
            "11" -> "Free 1kg Red Label packet on orders above ₹4,000!"
            "12" -> "Flat 8% margin on all Lay's & Kurkure party packs!"
            "13" -> "Free doorstep unloading for orders above 20 bags!"
            "14" -> "Get 2 cases of Lay's free on purchasing 5 cases of Pepsi!"
            "15" -> "Flat 10% wholesale margin on all Dove range!"
            "16" -> "Extra 2% margin on Gowardhan Desi Ghee bulk tins!"
            "17" -> "Next-day shipping for all orders within Mumbai suburbs!"
            "18" -> "Get 500 free customized logo paper bags on orders above ₹10,000!"
            else -> "Flat wholesale margins available!"
        }
        val image = when(idVal.toString()) {
            "1" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuAHgHdUGrTq7Ih9FVYs2x04F3OINmFhZzL6QQwiM298yzlCN2H7wPL89tI7KZcm04URHhi7PwspRVS-C305cP1XIWZLA0wDGqiqQJhvaYXUd25XiO5weK_YKgDC9-NvL0XN6_okvSJ3qFhwSp6IKbH2GhAVQ6LdyCs96iHL3Es89QOfy4hSYHKv8UuT7D52_YaHpr24uvWHonT6wIbO7y-Wly8_LWPZBkbJC3m2iffJgWsPRGeezHIg8FWZyjgv-gkc-V2D0bSBoaU"
            "2" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCxbjfqqU8EEdR43WmVK3tP9fLppr9GPddFvqNTxXRq5Hy4himN-kDni3RHPpbnT_tO8mb8jprG3Hn99vhUit1We1lJDK6Fqqbx4LMQPzohyoozVnL0DUWHy24a1wFP-9plQWrBuISuNpQLZZdC6hYILj11NGpEHp-oxTL8uJ_ynt8qC12Mmm65bFIePKcTdcsuBT7VppiTqZhsr2UrVItupAcRIn6LL5waedknGSZN8IwswnrBdWWCNHOw_Ydp7s5T1VaipnIIfAc"
            "3" -> "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=600&q=80"
            "4" -> "https://images.unsplash.com/photo-1528750951167-a558973b8042?auto=format&fit=crop&w=600&q=80"
            "5" -> "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80"
            "6" -> "https://images.unsplash.com/photo-1574316071802-0d684efa7bf5?auto=format&fit=crop&w=600&q=80"
            "7" -> "https://images.unsplash.com/photo-1581798459219-318e76aecc7b?auto=format&fit=crop&w=600&q=80"
            "8" -> "https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=600&q=80"
            "9" -> "https://images.unsplash.com/photo-1596560548464-f01068e6020a?auto=format&fit=crop&w=600&q=80"
            "10" -> "https://images.unsplash.com/photo-1612966608967-312ba599102e?auto=format&fit=crop&w=600&q=80"
            "11" -> "https://images.unsplash.com/photo-1554980291-c277f2405021?auto=format&fit=crop&w=600&q=80"
            "12" -> "https://images.unsplash.com/photo-1599490659223-9372241688b6?auto=format&fit=crop&w=600&q=80"
            "13" -> "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=600&q=80"
            "14" -> "https://images.unsplash.com/photo-1553413719-87587121d72b?auto=format&fit=crop&w=600&q=80"
            "15" -> "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=600&q=80"
            "16" -> "https://images.unsplash.com/photo-1506084868230-bb9d95c24759?auto=format&fit=crop&w=600&q=80"
            "17" -> "https://images.unsplash.com/photo-1607344645866-009c320c5ab8?auto=format&fit=crop&w=600&q=80"
            "18" -> "https://images.unsplash.com/photo-1569003339405-ea396a5a8a90?auto=format&fit=crop&w=600&q=80"
            else -> "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=600&q=80"
        }
        val isNearby = idVal % 2 == 1
        val isBestMargin = idVal in listOf(1, 2, 5, 6, 7, 9, 12, 13, 15, 17)
        val isFastDelivery = idVal in listOf(2, 3, 4, 8, 10, 11, 12, 14, 16, 17, 18)
 
        Distributor(
            id = idVal,
            name = dist.businessName,
            category = dist.category,
            moq = "₹${dist.minOrderValue.toInt()}",
            rating = rating,
            location = dist.address ?: "Mumbai",
            brands = dist.serviceRegions.ifEmpty { listOf("General Wholesaler") },
            promo = promo,
            phone = dist.phone,
            image = image,
            isNearby = isNearby,
            isBestMargin = isBestMargin,
            isFastDelivery = isFastDelivery
        )
    }

    val filteredDistributors = distributorsList.filter { dist ->
        val matchesSearch = searchQuery.isEmpty() ||
                dist.name.contains(searchQuery, ignoreCase = true) ||
                dist.category.contains(searchQuery, ignoreCase = true) ||
                dist.brands.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesChip = when (selectedFilterChip) {
            "Nearby" -> dist.isNearby
            "Best Margin" -> dist.isBestMargin
            "Fast Delivery" -> dist.isFastDelivery
            else -> true
        }

        matchesSearch && matchesChip
    }

    Scaffold(
        topBar = {
            if (!isEmbedded) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(primaryContainer.copy(alpha = 0.15f))
                                    .border(1.dp, outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Shop logo",
                                    tint = primaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "RetailDost",
                                fontWeight = FontWeight.Bold,
                                color = primaryIndigo,
                                fontSize = 20.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryIndigo
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Sync complete",
                                tint = primaryIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundLight)
                )
            }
        },
        bottomBar = {
            if (!isEmbedded) {
                NavigationBar(
                    containerColor = backgroundLight,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { viewModel.navigateTo(Screen.Dashboard); viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = onSurfaceVariant) },
                        label = { Text("Home", fontSize = 11.sp, color = onSurfaceVariant) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { viewModel.navigateTo(Screen.Dashboard); viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "Stock", tint = onSurfaceVariant) },
                        label = { Text("Stock", fontSize = 11.sp, color = onSurfaceVariant) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { viewModel.navigateTo(Screen.Dashboard); viewModel.selectTab(2) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Khata", tint = onSurfaceVariant) },
                        label = { Text("Khata", fontSize = 11.sp, color = onSurfaceVariant) }
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = "Market", tint = primaryIndigo)
                            }
                        },
                        label = { Text("Market", color = primaryIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .then(
                    if (isEmbedded) Modifier.statusBarsPadding() else Modifier.padding(innerPadding)
                )
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // ── 1. Search Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search brands (e.g. Atta, Amul, Maggi)", fontSize = 14.sp, color = onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = onSurfaceVariant) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = textDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surfaceContainerLowest,
                        unfocusedContainerColor = surfaceContainerLowest,
                        focusedBorderColor = primaryIndigo,
                        unfocusedBorderColor = outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 2. Horizontally Scrollable Filter Chips Row ──
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = selectedFilterChip == "Nearby"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) primaryContainer else surfaceContainerHighest)
                            .clickable { selectedFilterChip = "Nearby" }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Nearby",
                            color = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    val isSelected = selectedFilterChip == "Best Margin"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) primaryContainer else surfaceContainerHighest)
                            .clickable { selectedFilterChip = "Best Margin" }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Best Margin",
                            color = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    val isSelected = selectedFilterChip == "Fast Delivery"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) primaryContainer else surfaceContainerHighest)
                            .clickable { selectedFilterChip = "Fast Delivery" }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Fast Delivery",
                            color = if (isSelected) onPrimaryContainer else onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Verified Counter Indicator with Pulse ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(secondary.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${filteredDistributors.size} VERIFIED DISTRIBUTORS IN MUMBAI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 4. Wholesaler Distributors List ──
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (filteredDistributors.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No distributors found matching search/filter.",
                                color = onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredDistributors) { dist ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                            border = BorderStroke(1.dp, outlineVariant)
                        ) {
                            Column {
                                // Warehouse image container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                ) {
                                    AsyncImage(
                                        model = dist.image,
                                        contentDescription = dist.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Translucent glassmorphic MOQ Badge on top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.85f))
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dist.moq,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = secondary
                                            )
                                            Text(
                                                text = "MOQ",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Verified Tag bottom left
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(secondary)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "VERIFIED",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Details area
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dist.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = primaryIndigo
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(backgroundLight)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Star",
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = dist.rating,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = textDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dist.category,
                                        fontSize = 14.sp,
                                        color = onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Location",
                                            tint = onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dist.location,
                                            fontSize = 12.sp,
                                            color = onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Supplier list row
                                    Text(
                                        text = "OFFICIAL SUPPLIER FOR:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceVariant,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(dist.brands) { brand ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(surfaceContainerHighest)
                                                    .border(1.dp, outlineVariant, RoundedCornerShape(20.dp))
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = brand,
                                                    color = onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Alternating Margin Banner style
                                    val isEven = dist.id % 2 == 0
                                    val promoBg = if (isEven) tertiaryFixed else secondaryContainer.copy(alpha = 0.3f)
                                    val promoBorder = if (isEven) outlineVariant else secondaryContainer
                                    val promoText = if (isEven) onTertiaryFixed else onSecondaryContainer
                                    val promoIcon = if (isEven) Icons.Default.Celebration else Icons.Default.Bolt
                                    val promoIconTint = if (isEven) tertiary else onSecondaryContainer

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(promoBg)
                                            .border(1.dp, promoBorder, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = promoIcon,
                                                contentDescription = "Flash promotion margin bonus",
                                                tint = promoIconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = dist.promo,
                                                fontSize = 12.sp,
                                                color = promoText,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Chat wholesale button (WhatsApp or Direct Call action depending on even/odd ID to separate them)
                                    Button(
                                        onClick = {
                                            if (isEven) {
                                                // WhatsApp Chat action
                                                val msg = "Hello, I found your wholesale business ${dist.name} on RetailDost. I want to inquire about stocking prices."
                                                val encodedMsg = URLEncoder.encode(msg, "UTF-8")
                                                val url = "https://api.whatsapp.com/send?phone=${dist.phone}&text=$encodedMsg"
                                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(url)
                                                }
                                                context.startActivity(waIntent)
                                            } else {
                                                // Direct Phone Dial action
                                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${dist.phone}")
                                                }
                                                context.startActivity(dialIntent)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = secondary)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (isEven) "Chat & Ask Wholesale Margin" else "Call Wholesaler Shop Direct",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
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
}

data class Distributor(
    val id: Int,
    val name: String,
    val category: String,
    val moq: String,
    val rating: String,
    val location: String,
    val brands: List<String>,
    val promo: String,
    val phone: String,
    val image: String,
    val isNearby: Boolean,
    val isBestMargin: Boolean,
    val isFastDelivery: Boolean
)

val defaultDistributors = listOf(
    Distributor(
        id = 1,
        name = "Vikram Seth Enterprises",
        category = "ITC Staples, Basmati Rice & Grocery",
        moq = "₹1,500",
        rating = "4.9",
        location = "CST Wholesaler Market, Shop #23, Mumbai",
        brands = listOf("Aashirvaad", "Sunfeast", "Savlon"),
        promo = "5% extra cash margin on top of raw Basmati bags!",
        phone = "9876543210",
        image = "https://lh3.googleusercontent.com/aida-public/AB6AXuAHgHdUGrTq7Ih9FVYs2x04F3OINmFhZzL6QQwiM298yzlCN2H7wPL89tI7KZcm04URHhi7PwspRVS-C305cP1XIWZLA0wDGqiqQJhvaYXUd25XiO5weK_YKgDC9-NvL0XN6_okvSJ3qFhwSp6IKbH2GhAVQ6LdyCs96iHL3Es89QOfy4hSYHKv8UuT7D52_YaHpr24uvWHonT6wIbO7y-Wly8_LWPZBkbJC3m2iffJgWsPRGeezHIg8FWZyjgv-gkc-V2D0bSBoaU",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 2,
        name = "Balaram Spices & Flour Mills",
        category = "Flour/Atta, Local Spices & Masalas",
        moq = "₹2,000",
        rating = "4.6",
        location = "Mandvi Spices Yard, Galas Shop #18, Mumbai",
        brands = listOf("Fortune", "Kitchen King", "Haldi"),
        promo = "Extra 50 sachet tea masala sample on every bulk spices box!",
        phone = "9876543210",
        image = "https://lh3.googleusercontent.com/aida-public/AB6AXuCxbjfqqU8EEdR43WmVK3tP9fLppr9GPddFvqNTxXRq5Hy4himN-kDni3RHPpbnT_tO8mb8jprG3Hn99vhUit1We1lJDK6Fqqbx4LMQPzohyoozVnL0DUWHy24a1wFP-9plQWrBuISuNpQLZZdC6hYILj11NGpEHp-oxTL8uJ_ynt8qC12Mmm65bFIePKcTdcsuBT7VppiTqZhsr2UrVItupAcRIn6LL5waedknGSZN8IwswnrBdWWCNHOw_Ydp7s5T1VaipnIIfAc",
        isNearby = false,
        isBestMargin = true,
        isFastDelivery = true
    ),
    Distributor(
        id = 3,
        name = "Apex Beverage Logistics",
        category = "Soft Drinks, Energy Drinks & Mineral Water",
        moq = "₹3,500",
        rating = "4.7",
        location = "Andheri East Industrial Estate, Plot #12, Mumbai",
        brands = listOf("Coca-Cola", "Sprite", "Thums Up", "Kinley"),
        promo = "Buy 10 crates of Sprite, get 1 crate of Kinley Soda free!",
        phone = "9812345670",
        image = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 4,
        name = "Royal Dairy Distributors",
        category = "Cheese, Butter, Milk & Fresh Cream",
        moq = "₹1,000",
        rating = "4.8",
        location = "Goregaon East Milk Colony, Hub #4, Mumbai",
        brands = listOf("Amul", "Mother Dairy", "Go Cheese"),
        promo = "Next-day delivery guaranteed or flat 2% off invoice value!",
        phone = "9823456781",
        image = "https://images.unsplash.com/photo-1528750951167-a558973b8042?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 5,
        name = "Krishna Oil & Ghee Traders",
        category = "Refined Oils, Mustard Oil & Pure Desi Ghee",
        moq = "₹5,000",
        rating = "4.5",
        location = "Masjid Bunder Oil Market, Shop #5A, Mumbai",
        brands = listOf("Saffola", "Dhara", "Fortune Oils"),
        promo = "Slab scheme: Orders above ₹15,000 get 3% instant cash discount!",
        phone = "9834567892",
        image = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 6,
        name = "Golden Harvest Pulses",
        category = "Premium Dals, Pulses & Organic Grains",
        moq = "₹2,500",
        rating = "4.4",
        location = "Vashi APMC Market II, Gate #3, Navi Mumbai",
        brands = listOf("Tata Sampann", "Organic India", "Golden"),
        promo = "Free custom shop branding banner on ordering 500kg of mixed dals!",
        phone = "9845678903",
        image = "https://images.unsplash.com/photo-1574316071802-0d684efa7bf5?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 7,
        name = "National Confectionery Wholesalers",
        category = "Chocolates, Biscuits, Wafers & Candy Boxes",
        moq = "₹3,000",
        rating = "4.3",
        location = "Crawford Market, Shop #88, Mumbai",
        brands = listOf("Cadbury", "Nestle", "Parle-G", "Britannia"),
        promo = "12% high margins on new Cadbury assortments!",
        phone = "9856789014",
        image = "https://images.unsplash.com/photo-1581798459219-318e76aecc7b?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 8,
        name = "Supreme Hygiene & Detergents",
        category = "Soaps, Dishwash, Toilet Cleaners & Detergents",
        moq = "₹1,200",
        rating = "4.6",
        location = "Kurla West Depot Road, Building #2, Mumbai",
        brands = listOf("Dettol", "Vim", "Surf Excel", "Harpic"),
        promo = "Get 1 dozen free Vim bars with every case of Surf Excel liquid!",
        phone = "9867890125",
        image = "https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 9,
        name = "Mumbai Premium Dry Fruits",
        category = "Almonds, Cashews, Raisins & Exotic Nuts",
        moq = "₹8,000",
        rating = "4.9",
        location = "Byculla Dry Fruit Hub, Shop #12B, Mumbai",
        brands = listOf("Del Monte", "Tulsi", "Happilo"),
        promo = "Premium vacuum packaging included free for festive boxes!",
        phone = "9878901236",
        image = "https://images.unsplash.com/photo-1596560548464-f01068e6020a?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 10,
        name = "Modern Packaged Foods",
        category = "Noodles, Pasta, Sauces & Instant Soups",
        moq = "₹2,200",
        rating = "4.7",
        location = "Thane Wagle Estate, Gali #4, Mumbai",
        brands = listOf("Maggi", "Chings Secret", "Yippee", "Knorr"),
        promo = "Scratch card inside every case of Maggi noodles - win up to ₹500!",
        phone = "9889012347",
        image = "https://images.unsplash.com/photo-1612966608967-312ba599102e?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 11,
        name = "Super Fresh Tea & Coffee Co.",
        category = "Loose Tea Dust, CTC Packets & Instant Coffee",
        moq = "₹1,800",
        rating = "4.5",
        location = "Mulund West Commercial Street, Shop #9, Mumbai",
        brands = listOf("Red Label", "Tata Tea", "Nescafe", "Bru"),
        promo = "Free 1kg Red Label packet on orders above ₹4,000!",
        phone = "9890123458",
        image = "https://images.unsplash.com/photo-1554980291-c277f2405021?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 12,
        name = "Gourmet Bakery & Snacks",
        category = "Toast, Khari, Rusk, Chips & Namkeen packets",
        moq = "₹1,500",
        rating = "4.6",
        location = "Bandra Linking Road, Extension Block #7, Mumbai",
        brands = listOf("Haldiram", "Lay's", "Kurkure", "Monaco"),
        promo = "Flat 8% margin on all Lay's & Kurkure party packs!",
        phone = "9901234569",
        image = "https://images.unsplash.com/photo-1599490659223-9372241688b6?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = true
    ),
    Distributor(
        id = 13,
        name = "Balaji Agro & Staples",
        category = "Premium Basmati, Wheat & Bulk Grains",
        moq = "₹3,000",
        rating = "4.8",
        location = "Byculla Grain Market, Shop #45, Mumbai",
        brands = listOf("India Gate", "Aashirvaad", "Fortune Basmati"),
        promo = "Free doorstep unloading for orders above 20 bags!",
        phone = "9911223344",
        image = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 14,
        name = "Narayana Beverage & Snacks",
        category = "Juices, Sodas, Chips & Party Packs",
        moq = "₹1,500",
        rating = "4.5",
        location = "Linking Road Snack Hub, Basement #2, Mumbai",
        brands = listOf("Pepsi", "Tropicana", "Kurkure", "Lays"),
        promo = "Get 2 cases of Lay's free on purchasing 5 cases of Pepsi!",
        phone = "9922334455",
        image = "https://images.unsplash.com/photo-1553413719-87587121d72b?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 15,
        name = "Pooja Cosmetics & Toiletries",
        category = "Soaps, Shampoos, Toothpaste & Cosmetics",
        moq = "₹2,500",
        rating = "4.7",
        location = "Dharavi Commercial Lane, Gala #12, Mumbai",
        brands = listOf("Colgate", "Dove", "Lifebuoy", "Pepsodent"),
        promo = "Flat 10% wholesale margin on all Dove range!",
        phone = "9933445566",
        image = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = false
    ),
    Distributor(
        id = 16,
        name = "Shree Ganesha Dairy & Sweets",
        category = "Paneer, Ghee, Milk & Curd Packs",
        moq = "₹1,000",
        rating = "4.6",
        location = "Kurla East Milk Depot, Shop #3, Mumbai",
        brands = listOf("Gowardhan", "Amul Ghee", "Mahi Milk"),
        promo = "Extra 2% margin on Gowardhan Desi Ghee bulk tins!",
        phone = "9944556677",
        image = "https://images.unsplash.com/photo-1506084868230-bb9d95c24759?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = false,
        isFastDelivery = true
    ),
    Distributor(
        id = 17,
        name = "National Spices & Condiments",
        category = "Bulk Spices, Dry Red Chillies & Seeds",
        moq = "₹4,000",
        rating = "4.9",
        location = "Masjid Bunder Gali #3, Spice Block, Mumbai",
        brands = listOf("MDH", "Everest", "Badshah Spices"),
        promo = "Next-day shipping for all orders within Mumbai suburbs!",
        phone = "9955667788",
        image = "https://images.unsplash.com/photo-1607344645866-009c320c5ab8?auto=format&fit=crop&w=600&q=80",
        isNearby = true,
        isBestMargin = true,
        isFastDelivery = true
    ),
    Distributor(
        id = 18,
        name = "Hindustan Premium Packaging",
        category = "Disposables, Paper Bags & Packaging Material",
        moq = "₹2,000",
        rating = "4.4",
        location = "Andheri West Industrial Estate, Hub #1B, Mumbai",
        brands = listOf("EcoPack", "BioWare", "CarryClean"),
        promo = "Get 500 free customized logo paper bags on orders above ₹10,000!",
        phone = "9966778899",
        image = "https://images.unsplash.com/photo-1569003339405-ea396a5a8a90?auto=format&fit=crop&w=600&q=80",
        isNearby = false,
        isBestMargin = false,
        isFastDelivery = true
    )
)

