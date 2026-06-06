package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import coil.compose.AsyncImage
import com.example.ui.KiranaViewModel
import com.example.ui.Screen

/**
 * WelcomeScreen
 * ─────────────
 * Serves as the landing / launch screen (Image 3) when unauthenticated.
 * Integrates language switching, overview cards, and navigation hooks to auth.
 */
@Composable
fun WelcomeScreen(
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignup: (() -> Unit)? = null,
    viewModel: KiranaViewModel
) {
    val language by viewModel.currentLanguage.collectAsState()

    val deepNavy = Color(0xFF0F1B85)
    val softLavenderBg = Color(0xFFF7F6FC)
    val cardLavender = Color(0xFFF3F1FA)
    val badgeTeal = Color(0xFF007E65)
    val badgeBlue = Color(0xFF283AC2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softLavenderBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Header ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = "RetailDost Logo",
                tint = deepNavy,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "RetailDost",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = deepNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The AI-Powered OS for your Store",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Feature Card 1: B&W Image + Badges + Overlay ─────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("onboarding_illustration_card"),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBTQkTMXnib_qVPnCwVRHyps5y2h_GcjEZeUmii1eUtGwYFhXo9aBt4xtLq2QhzQPGSlOsswqZnfwSRRSqmMk-bahKeZEn9d6ymrOM5AzTAo87ZVT08o_Sh4q5uzy9sTtL1VbYXuWufg4kEcblgfPOp-1e5mYRF6MpqueZ-P8sGYhL8GCIlSe7vHTyDO7jfGNv7J2R1kBi8FYyF2lU96zTSFklHq7U6w0fPvO1X0GCwicMrDDiY1WiOV0blwSGeSrMioMdyzoV8xO8",
                        contentDescription = "Store Aisle Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Dark overlay gradient to make text readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 100f
                                )
                            )
                    )

                    // Badges (top left)
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = badgeTeal,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RELIABLE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = badgeBlue,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FAST",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Text (bottom left)
                    Text(
                        text = "Manage Inventory with\n99.9% Accuracy",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Feature Card 2: Smart Ledger ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardLavender),
                border = BorderStroke(1.dp, Color(0xFFE2E0EE))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Smart Ledger Icon",
                        tint = deepNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Ledger",
                            color = deepNavy,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automated credit tracking and settlement reminders for your customers.",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Feature Card 3: Quick Billing ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardLavender),
                border = BorderStroke(1.dp, Color(0xFFE2E0EE))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Quick Billing Icon",
                        tint = badgeTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Quick Billing",
                            color = badgeTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fastest barcode scanning engine optimized for budget hardware.",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Actions: Sign In / Create Account ────────────────────
            Button(
                onClick = { onNavigateToLogin?.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("get_started_button"),
                colors = ButtonDefaults.buttonColors(containerColor = deepNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Sign In Arrow",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onNavigateToSignup?.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, deepNavy),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = deepNavy)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Copyright / Policy Footer Links ──────────────────────
            Text(
                text = "© 2024 RetailDost. All rights reserved.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "Terms",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "Support",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
