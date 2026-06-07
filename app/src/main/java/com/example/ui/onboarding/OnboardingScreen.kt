package com.example.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.auth.AmbientBackground
import com.example.ui.auth.ErrorBanner

/**
 * OnboardingScreen
 * ───────────────
 * Multi-step onboarding flow designed to match high-fidelity screenshots.
 * Integrates Hilt-injected [OnboardingViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val deepNavy = Color(0xFF0F1B85)
    val softLavenderBg = Color(0xFFF8F7FD)
    val activeColor = Color(0xFF3F51B5)

    // Back button behavior
    BackHandler(enabled = uiState.currentStep > 0) {
        viewModel.previousStep()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softLavenderBg)
    ) {
        AmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Header Navigation ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (uiState.currentStep > 0) {
                            viewModel.previousStep()
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.testTag("onboarding_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = deepNavy
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Profile Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = deepNavy
                )

                Spacer(modifier = Modifier.weight(1f))

                // Progress Step Pill Indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8EAF6),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Step ${uiState.currentStep + 1} of 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            val animatedProgress by animateFloatAsState(
                targetValue = (uiState.currentStep + 1) / 2.0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "OnboardingProgress"
            )

            // Simple line progress indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = activeColor,
                trackColor = Color(0xFFE2E0EE)
            )

            // ── Main Content Container ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "OnboardingStepAnimation"
                ) { step ->
                    when (step) {
                        0 -> StoreDetailsStep(uiState, viewModel, focusManager)
                        1 -> LocationStep(uiState, viewModel, onOnboardingSuccess, focusManager)
                    }
                }
            }
        }
    }
}

// ── Step 1: Store Basics ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailsStep(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    val businessTypes = listOf("Kirana Store", "General Store", "Wholesale Mart", "Pharmacy", "Supermarket")
    val deepNavy = Color(0xFF0F1B85)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "PulsingNextButton")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NextButtonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 0)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tell us about your business",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set up your digital ledger identities to start managing items and credits.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Owner Name Field
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 100)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 100)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = uiState.ownerName,
                    onValueChange = viewModel::onOwnerNameChanged,
                    label = { Text("Owner's Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = uiState.ownerNameError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("owner_name_input")
                )
                if (uiState.ownerNameError != null) {
                    Text(
                        text = uiState.ownerNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Store Name Field
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 200)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 200)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = uiState.storeName,
                    onValueChange = viewModel::onStoreNameChanged,
                    label = { Text("Store Name *") },
                    placeholder = { Text("e.g. Laxmi Traders") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    isError = uiState.storeNameError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { expanded = true }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("store_name_input")
                )
                if (uiState.storeNameError != null) {
                    Text(
                        text = uiState.storeNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Business Type Dropdown
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 300)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.businessType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Business Type *") },
                            placeholder = { Text("Select Type") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            isError = uiState.businessTypeError != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("business_type_select")
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            businessTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        viewModel.onBusinessTypeChanged(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (uiState.businessTypeError != null) {
                    Text(
                        text = uiState.businessTypeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 400)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 400)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = viewModel::nextStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .testTag("onboarding_step_zero_next_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = deepNavy)
            ) {
                Text("Next: Location Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// ── Step 2: Location Details ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationStep(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onSuccess: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val scrollState = rememberScrollState()
    val activeColor = Color(0xFF0F1B85)
    var stateExpanded by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "PulsingCompleteButton")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CompleteButtonScale"
    )

    val indianStates = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
        "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
        "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
        "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
        "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh",
        "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir",
        "Ladakh", "Lakshadweep", "Puducherry"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 0)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Where is your store?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter location details to complete configuration.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // State Dropdown (Indian States)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 100)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 100)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = stateExpanded,
                        onExpandedChange = { stateExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.state,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("State *") },
                            placeholder = { Text("Select State") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                            isError = uiState.stateError != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("state_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = stateExpanded,
                            onDismissRequest = { stateExpanded = false }
                        ) {
                            indianStates.forEach { stateName ->
                                DropdownMenuItem(
                                    text = { Text(stateName) },
                                    onClick = {
                                        viewModel.onStateChanged(stateName)
                                        stateExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (uiState.stateError != null) {
                    Text(
                        text = uiState.stateError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City (Manual Input below State dropdown)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 200)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 200)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = viewModel::onCityChanged,
                    label = { Text("City *") },
                    placeholder = { Text("e.g. Mumbai") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    isError = uiState.cityError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("city_input")
                )
                if (uiState.cityError != null) {
                    Text(
                        text = uiState.cityError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pincode (Manual Input below City field)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 300)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = uiState.pincode,
                    onValueChange = viewModel::onPincodeChanged,
                    label = { Text("Pincode *") },
                    placeholder = { Text("6-digit Code (e.g. 400001)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    isError = uiState.pincodeError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pincode_input")
                )
                if (uiState.pincodeError != null) {
                    Text(
                        text = uiState.pincodeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Global Error Banner
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let {
                ErrorBanner(message = it, onDismiss = viewModel::clearError)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(500, delayMillis = 400)) { it / 2 } + fadeIn(animationSpec = tween(500, delayMillis = 400)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { viewModel.completeOnboarding(onSuccess) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .testTag("onboarding_step_one_complete_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Complete Profile Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Done, contentDescription = null)
                    }
                }
            }
        }
    }
}
