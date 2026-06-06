package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = OnPrimaryContainerBlue,
    onPrimary = PrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    inversePrimary = InversePrimary,
    secondary = MintTeal,
    onSecondary = OnSecondaryContainerTeal,
    secondaryContainer = OnSecondaryContainerTeal,
    onSecondaryContainer = MintTeal,
    tertiary = OnTertiaryContainer,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    surface = DarkBackgroundAccent,
    surfaceDim = Color(0xFF13131A),
    surfaceBright = Color(0xFF39393F),
    surfaceContainerLowest = Color(0xFF0E0E14),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A2A30),
    surfaceContainerHighest = Color(0xFF35353B),
    surfaceVariant = SurfaceVariant,
    surfaceTint = SurfaceTint,
    background = DarkBackgroundAccent,
    onSurface = LightSurface,
    onSurfaceVariant = Color(0xFFC6C5D4),
    onBackground = LightSurface,
    inverseSurface = InverseOnSurface,
    inverseOnSurface = InverseSurface,
    outline = OutlineColor,
    outlineVariant = Color(0xFF454652),
    error = ExpiryRed,
    onError = OnError,
    errorContainer = ExpiryContainerRed,
    onErrorContainer = OnExpiryContainerRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    inversePrimary = InversePrimary,
    secondary = SecondaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainerTeal,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    surface = SurfaceColor,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceVariant = SurfaceVariant,
    surfaceTint = SurfaceTint,
    background = SurfaceColor,
    onSurface = OnSurfaceText,
    onSurfaceVariant = OnSurfaceVariantText,
    onBackground = OnBackground,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    error = ExpiryRed,
    onError = OnError,
    errorContainer = ExpiryContainerRed,
    onErrorContainer = OnExpiryContainerRed
  )


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
