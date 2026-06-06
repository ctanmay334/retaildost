package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * ForgotPasswordScreen
 * ─────────────────────
 * Replicates the forgot password screen (Image 2) with:
 *  • Back arrow navigation header and "RetailDost Pro" branding
 *  • White central card containing form fields
 *  • Icon representing reset password
 *  • Navy blue "Send Reset Link" button with plane/send icon
 *  • "Back to Login" text button below
 */
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val deepNavy = Color(0xFF0F1B85)
    val softLavenderBg = Color(0xFFF8F7FD)
    val cardOutline = Color(0xFFE2E0EE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softLavenderBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header Row with Back Button ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back",
                    tint = deepNavy,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNavigateBack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "RetailDost Pro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = deepNavy
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Success state ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.resetEmailSent,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Email sent",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                              )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Check your inbox",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "We sent a password reset link to\n${uiState.resetEmail}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("back_to_login_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, deepNavy),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = deepNavy)
                    ) {
                        Text("Back to Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Input State card (Image 2 design) ──────────────────────────
            AnimatedVisibility(
                visible = !uiState.resetEmailSent,
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardOutline, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Padlock refresh icon container
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE9E7FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Lock Reset Icon",
                                    tint = deepNavy,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Forgot Password?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Enter your email to receive a secure reset link.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Email text field
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Email Address",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = uiState.resetEmail,
                                    onValueChange = viewModel::onResetEmailChanged,
                                    placeholder = { Text("name@store.com") },
                                    isError = uiState.resetEmailError != null,
                                    enabled = !uiState.isLoading,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Mail,
                                            contentDescription = "Mail Icon",
                                            tint = Color.Gray
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            viewModel.sendPasswordReset()
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_email_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.LightGray,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                                if (uiState.resetEmailError != null) {
                                    Text(
                                        text = uiState.resetEmailError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Send Reset Link button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.sendPasswordReset()
                                },
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("send_reset_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = deepNavy)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Send Reset Link",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send Arrow Icon",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Back to Login text button
                    Row(
                        modifier = Modifier
                            .clickable { onNavigateBack() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Arrow Back Icon",
                            tint = deepNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back to Login",
                            color = deepNavy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Copyright / Policy Footer Links ──────────────────────
            Text(
                text = "© 2024 RetailDost Pro. All rights reserved.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "Terms of Service",
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
