package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElectricCyan,
    secondary = NeonGreen,
    tertiary = LuxuryGold,
    background = ObsidianBlack,
    surface = DarkSlate,
    onPrimary = ObsidianBlack,
    onSecondary = ObsidianBlack,
    onBackground = CleanWhite,
    onSurface = CleanWhite
  )

private val LightColorScheme = DarkColorScheme // Keep it consistently dark for premium render styling according to user guidelines

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark theme for sleek watch styling
  dynamicColor: Boolean = false, // Use our handcrafted rich palette rather than dynamic system hues
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
