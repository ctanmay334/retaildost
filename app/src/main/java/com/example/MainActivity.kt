package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.ForgotPasswordScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.SignupScreen
import com.example.ui.khata.KhataViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.sale.RecordSaleScreen
import com.example.ui.sale.SalesHistoryScreen
import com.example.ui.auth.WelcomeScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.inventory.AddProductScreen
import com.example.ui.inventory.AllProductsScreen
import com.example.ui.inventory.ProductDetailsScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.khata.CustomerLedgerScreen
import com.example.ui.marketplace.MarketplaceScreen
import com.example.ui.marketplace.DistributorRegistrationScreen
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.khata.AddCustomerScreen
import com.example.ui.khata.SelectContactScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 * ─────────────
 * Single-activity host with Hilt injection.
 *
 * Auth flow:
 *  • AuthViewModel.isSessionLoading blocks navigation until the session
 *    restore attempt completes (shows a centered spinner).
 *  • If isAuthenticated=true → go straight to the app.
 *  • Otherwise → show Login screen with routes to Signup and ForgotPassword.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentState = mutableStateOf<android.content.Intent?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val appViewModel:  KiranaViewModel = hiltViewModel()
            val khataViewModel: KhataViewModel = hiltViewModel()
            val analyticsViewModel: com.example.ui.analytics.AnalyticsViewModel = hiltViewModel()

            val authState       by authViewModel.uiState.collectAsStateWithLifecycle()
            val isSessionLoading by authViewModel.isSessionLoading.collectAsStateWithLifecycle()
            val darkMode        by appViewModel.darkMode.collectAsStateWithLifecycle()
            val currentScreen   by appViewModel.currentScreen.collectAsStateWithLifecycle()
            val isAppLoading    by appViewModel.isSessionLoading.collectAsStateWithLifecycle()
            val selectedTab     by appViewModel.selectedTab.collectAsStateWithLifecycle()

            // Auth screen sub-nav state (local to MainActivity; no NavController overhead)
            var authScreen by remember { mutableStateOf<AuthNav>(AuthNav.Welcome) }

            // Observe intent deep links reactively
            val activeIntent by intentState
            LaunchedEffect(activeIntent) {
                activeIntent?.let { intent ->
                    val navigateTo = intent.getStringExtra("navigate_to")
                    val inventoryId = intent.getStringExtra("inventory_id")
                    val customerId = intent.getStringExtra("customer_id")
                    
                    if (navigateTo == "inventory_detail" && !inventoryId.isNullOrBlank()) {
                        appViewModel.navigateTo(Screen.InventoryDetail(inventoryId))
                        intentState.value = null
                    } else if (navigateTo == "customer_ledger" && !customerId.isNullOrBlank()) {
                        appViewModel.navigateTo(Screen.CustomerLedger(customerId))
                        intentState.value = null
                    } else if (navigateTo == "settings") {
                        appViewModel.navigateTo(Screen.Settings)
                        intentState.value = null
                    } else {
                        // Support raw custom URI deep linking (e.g. retaildost://...)
                        intent.data?.let { uri ->
                            if (uri.scheme == "retaildost") {
                                when (uri.host) {
                                    "inventory_detail" -> {
                                        uri.getQueryParameter("inventory_id")?.let { invId ->
                                            appViewModel.navigateTo(Screen.InventoryDetail(invId))
                                        }
                                    }
                                    "customer_ledger" -> {
                                        uri.getQueryParameter("customer_id")?.let { custId ->
                                            appViewModel.navigateTo(Screen.CustomerLedger(custId))
                                        }
                                    }
                                    "settings" -> {
                                        appViewModel.navigateTo(Screen.Settings)
                                    }
                                }
                                intentState.value = null
                            }
                        }
                    }
                }
            }

            // Trigger onboarding verification upon authentication success
            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    appViewModel.checkOnboardingAndSync(isSignUp = authState.isNewSignup)
                }
            }

            // Handle system back press when not authenticated (auth screens navigation)
            BackHandler(
                enabled = !authState.isAuthenticated && authScreen != AuthNav.Welcome
            ) {
                authScreen = when (authScreen) {
                    AuthNav.Login, AuthNav.Signup -> AuthNav.Welcome
                    AuthNav.ForgotPassword -> AuthNav.Login
                    AuthNav.Welcome -> AuthNav.Welcome
                }
            }

            // Handle system back press when authenticated (main navigation flow)
            BackHandler(
                enabled = authState.isAuthenticated && (
                    (currentScreen == Screen.Dashboard && selectedTab != 0) ||
                    (currentScreen != Screen.Dashboard && currentScreen != Screen.Splash &&
                     currentScreen != Screen.OnboardingHighlights && currentScreen != Screen.OnboardingShopDetails)
                )
            ) {
                if (currentScreen == Screen.Dashboard) {
                    appViewModel.selectTab(0)
                } else {
                    appViewModel.navigateBack()
                }
            }

            MyApplicationTheme(darkTheme = darkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        when {
                            // ── Loading: session restore or logout in progress ──
                            isSessionLoading || (authState.isAuthenticated && authState.isLoading) -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            // ── Not authenticated → show auth screens ────────────
                            !authState.isAuthenticated -> {
                                AnimatedContent(
                                    targetState = authScreen,
                                    transitionSpec = {
                                        val isForward = when (targetState) {
                                            AuthNav.Login -> initialState == AuthNav.Welcome
                                            AuthNav.Signup -> initialState == AuthNav.Welcome || initialState == AuthNav.Login
                                            AuthNav.ForgotPassword -> initialState == AuthNav.Login
                                            AuthNav.Welcome -> false
                                        }
                                        if (isForward) {
                                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                    slideOutHorizontally { width -> -width } + fadeOut()
                                        } else {
                                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                    slideOutHorizontally { width -> width } + fadeOut()
                                        }
                                    },
                                    label = "AuthScreenTransition",
                                    modifier = Modifier.fillMaxSize()
                                ) { targetAuthScreen ->
                                    when (targetAuthScreen) {
                                        AuthNav.Welcome -> WelcomeScreen(
                                            onNavigateToLogin  = { authScreen = AuthNav.Login },
                                            onNavigateToSignup = { authScreen = AuthNav.Signup },
                                            viewModel          = appViewModel
                                        )
                                        AuthNav.Login -> LoginScreen(
                                            onNavigateToSignup         = { authScreen = AuthNav.Signup },
                                            onNavigateToForgotPassword = { authScreen = AuthNav.ForgotPassword },
                                            onLoginSuccess             = { /* isAuthenticated flips → recompose */ },
                                            onNavigateBack             = { authScreen = AuthNav.Welcome },
                                            viewModel                  = authViewModel
                                        )
                                        AuthNav.Signup -> SignupScreen(
                                            onNavigateToLogin = { authScreen = AuthNav.Login },
                                            onSignupSuccess   = { /* isAuthenticated flips → recompose */ },
                                            onNavigateBack    = { authScreen = AuthNav.Welcome },
                                            viewModel         = authViewModel
                                        )
                                        AuthNav.ForgotPassword -> ForgotPasswordScreen(
                                            onNavigateBack = { authScreen = AuthNav.Login },
                                            viewModel      = authViewModel
                                        )
                                    }
                                }
                            }

                            // ── App loading after auth ───────────────────────────
                            isAppLoading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            // ── Authenticated → show the main app ────────────────
                            else -> {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                                        fadeOut(animationSpec = tween(90))
                                    },
                                    label = "MainScreenTransition",
                                    modifier = Modifier.fillMaxSize()
                                ) { screen ->
                                    when (screen) {
                                        is Screen.Splash             -> OnboardingScreen(
                                            onOnboardingSuccess = { appViewModel.checkOnboardingAndSync() },
                                            onNavigateBack = {
                                                authViewModel.logout()
                                                appViewModel.logout()
                                            }
                                        )
                                        is Screen.OnboardingHighlights -> OnboardingScreen(
                                            onOnboardingSuccess = { appViewModel.completeOnboardingLocally() },
                                            onNavigateBack = {
                                                authViewModel.logout()
                                                appViewModel.logout()
                                            }
                                        )
                                        is Screen.OnboardingShopDetails -> OnboardingScreen(
                                            onOnboardingSuccess = {
                                                appViewModel.completeOnboardingLocally()
                                            },
                                            onNavigateBack = {
                                                authViewModel.logout()
                                                appViewModel.logout()
                                            }
                                        )
                                        is Screen.Dashboard          -> DashboardScreen(appViewModel, khataViewModel)
                                        is Screen.Inventory          -> InventoryScreen(
                                            onNavigateBack = {
                                                appViewModel.navigateTo(Screen.Dashboard)
                                            },
                                            kiranaViewModel = appViewModel
                                        )
                                        is Screen.AllProducts        -> AllProductsScreen(
                                            onNavigateBack = {
                                                appViewModel.navigateTo(Screen.Inventory)
                                            }
                                        )
                                        is Screen.InventoryDetail    -> ProductDetailsScreen(
                                            productId = screen.inventoryId,
                                            onNavigateBack = {
                                                appViewModel.navigateTo(Screen.Dashboard)
                                            },
                                            viewModel = appViewModel
                                        )
                                        is Screen.OcrReview          -> com.example.ui.ocr.OcrOrchestratorScreen(
                                            isStockOut = screen.isStockOut,
                                            onComplete = {
                                                appViewModel.navigateTo(Screen.Inventory)
                                            },
                                            onNavigateBack = {
                                                appViewModel.navigateTo(Screen.Inventory)
                                            }
                                        )
                                        is Screen.Settings           -> SettingsScreen(
                                            viewModel = appViewModel,
                                            onLogout = {
                                                authViewModel.logout()
                                                appViewModel.logout()
                                            }
                                        )
                                        is Screen.AddProduct         -> AddProductScreen(appViewModel)
                                        is Screen.Notifications      -> NotificationsScreen(appViewModel)
                                        is Screen.CustomerLedger     -> CustomerLedgerScreen(appViewModel, khataViewModel, screen.customerId)
                                        is Screen.Marketplace        -> MarketplaceScreen(appViewModel)
                                        is Screen.DistributorRegistration -> DistributorRegistrationScreen(appViewModel)
                                        is Screen.RecordSale         -> RecordSaleScreen(
                                            onNavigateBack = { appViewModel.navigateTo(Screen.Dashboard) }
                                        )
                                        is Screen.SalesHistory       -> SalesHistoryScreen(
                                            onNavigateBack = { appViewModel.navigateTo(Screen.Dashboard) }
                                        )
                                        is Screen.Analytics          -> AnalyticsScreen(
                                            viewModel = analyticsViewModel,
                                            onBack = { appViewModel.navigateTo(Screen.Dashboard) }
                                        )
                                        is Screen.AddCustomer        -> AddCustomerScreen(
                                            viewModel = appViewModel,
                                            onNavigateBack = { appViewModel.navigateTo(Screen.Dashboard) }
                                        )
                                        is Screen.SelectContact      -> SelectContactScreen(
                                            viewModel = appViewModel,
                                            onNavigateBack = { appViewModel.navigateTo(screen.fromScreen) }
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

/** Simple local navigation sealed class for the auth sub-flow. */
private sealed class AuthNav {
    object Welcome        : AuthNav()
    object Login          : AuthNav()
    object Signup         : AuthNav()
    object ForgotPassword : AuthNav()
}
