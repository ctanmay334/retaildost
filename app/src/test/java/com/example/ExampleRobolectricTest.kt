package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.KiranaViewModel
import com.example.ui.auth.WelcomeScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.inventory.AddProductScreen
import com.example.ui.khata.CustomerLedgerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.khata.KhataViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RetailDost", appName)
  }

  private fun getViewModel(): KiranaViewModel {
    val application = ApplicationProvider.getApplicationContext<Application>()
    return TestViewModelFactory.create(application)
  }

  @Test
  fun verify_welcome_screen_renders() {
    val viewModel = getViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        WelcomeScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }



  @Test
  fun verify_onboarding_shop_details_screen_renders() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val onboardingViewModel = OnboardingViewModel(application)
    composeTestRule.setContent {
      MyApplicationTheme {
        OnboardingScreen(
          viewModel = onboardingViewModel,
          onOnboardingSuccess = {},
          onNavigateBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun verify_dashboard_screen_renders() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val khataViewModel = KhataViewModel(application)
    val viewModel = getViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel, khataViewModel = khataViewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun verify_settings_screen_renders() {
    val viewModel = getViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        SettingsScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun verify_add_product_screen_renders() {
    val viewModel = getViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        AddProductScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  @org.junit.Ignore("Robolectric infinite animation loop crash")
  fun verify_customer_ledger_screen_renders() {
    val viewModel = getViewModel()
    val application = ApplicationProvider.getApplicationContext<Application>()
    val khataViewModel = KhataViewModel(application)
    composeTestRule.setContent {
      MyApplicationTheme {
        CustomerLedgerScreen(
          viewModel = viewModel,
          khataViewModel = khataViewModel,
          customerId = "1"
        )
      }
    }
    composeTestRule.waitForIdle()
  }
}


