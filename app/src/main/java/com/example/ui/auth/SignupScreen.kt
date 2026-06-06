package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * SignupScreen
 * ────────────
 * Replicates the signup page (Image 4) with:
 *  • Back arrow navigation header and "RetailDost" branding
 *  • Central card with Full Name, Email, and Password fields
 *  • Password strength indicator and checkbox agreement
 *  • SECURE, SYNC, LEDGER feature badges at the bottom
 */
@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess:   () -> Unit,
    onNavigateBack:    () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val deepNavy = Color(0xFF0F1B85)
    val softLavenderBg = Color(0xFFF8F7FD)
    val cardOutline = Color(0xFFE2E0EE)

    var fullName by remember { mutableStateOf("") }
    var agreeChecked by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onSignupSuccess()
    }

    // Dynamic password strength evaluation
    val passwordStrength = remember(uiState.signupPassword) {
        val pass = uiState.signupPassword
        when {
            pass.isEmpty() -> 0.0f
            pass.length < 4 -> 0.25f
            pass.length < 6 -> 0.5f
            pass.length < 8 -> 0.75f
            else -> 1.0f
        }
    }
    val strengthColor = when (passwordStrength) {
        0.25f -> Color.Red
        0.5f -> Color(0xFFFFA500) // Orange
        0.75f -> Color.Yellow
        1.0f -> Color(0xFF4CAF50) // Green
        else -> Color.LightGray
    }

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
                    text = "RetailDost",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = deepNavy
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Main Card Form Container ─────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardOutline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Join RetailDost",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The professional digital ledger for Indian retail merchants.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Name Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Full Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = { Text("Enter your full name") },
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.LightGray,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Address Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email Address",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.signupEmail,
                            onValueChange = viewModel::onSignupEmailChanged,
                            placeholder = { Text("name@store.com") },
                            isError = uiState.signupEmailError != null,
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.LightGray,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        if (uiState.signupEmailError != null) {
                            Text(
                                text = uiState.signupEmailError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Password",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.signupPassword,
                            onValueChange = {
                                viewModel.onSignupPasswordChanged(it)
                                viewModel.onSignupConfirmPasswordChanged(it) // Bind confirm password automatically to bypass dual inputs
                            },
                            placeholder = { Text("Create a strong password") },
                            isError = uiState.signupPasswordError != null,
                            enabled = !uiState.isLoading,
                            visualTransformation = if (uiState.signupPasswordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = viewModel::onSignupPasswordVisibilityToggle) {
                                    Icon(
                                        imageVector = if (uiState.signupPasswordVisible) Icons.Default.VisibilityOff
                                                      else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (agreeChecked) viewModel.signup()
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.LightGray,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        if (uiState.signupPasswordError != null) {
                            Text(
                                text = uiState.signupPasswordError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Password Strength Bar
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { passwordStrength },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = strengthColor,
                            trackColor = Color(0xFFEEEEEE)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Password Strength",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Min. 8 characters",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeChecked,
                            onCheckedChange = { agreeChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = deepNavy)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(style = SpanStyle(color = deepNavy, fontWeight = FontWeight.Bold)) {
                                    append("Terms of Service")
                                }
                                append(" and ")
                                withStyle(style = SpanStyle(color = deepNavy, fontWeight = FontWeight.Bold)) {
                                    append("Privacy Policy")
                                }
                                append(".")
                            },
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.clickable { agreeChecked = !agreeChecked }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Global Error Banner
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        uiState.errorMessage?.let {
                            ErrorBanner(message = it, onDismiss = viewModel::clearMessages)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Create Account Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.signup()
                        },
                        enabled = !uiState.isLoading && agreeChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("signup_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = deepNavy,
                            disabledContainerColor = deepNavy.copy(alpha = 0.5f)
                        )
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
                                    text = "Create Account",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Arrow Forward Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Already have an account link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Login",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = deepNavy,
                            modifier = Modifier
                                .clickable { onNavigateToLogin() }
                                .testTag("go_to_login_link")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECURE / SYNC / LEDGER Badges ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple(Icons.Default.Shield, "SECURE", "Secure shield"),
                    Triple(Icons.Default.Sync, "SYNC", "Sync database"),
                    Triple(Icons.Default.Book, "LEDGER", "Ledger book")
                ).forEach { (icon, text, desc) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2EFFD)),
                        border = BorderStroke(1.dp, Color(0xFFE2E0EE))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = desc,
                                tint = deepNavy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = deepNavy
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
