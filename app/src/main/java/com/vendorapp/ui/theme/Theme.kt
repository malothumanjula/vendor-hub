package com.vendorapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmLightColors = lightColorScheme(
    primary = Color(0xFFE4572E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCCF),
    onPrimaryContainer = Color(0xFF3A0A00),
    secondary = Color(0xFF2A9D8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F2E9),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFF4A261),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF29231F),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF29231F),
    surfaceVariant = Color(0xFFF3E5D8),
    onSurfaceVariant = Color(0xFF59463A),
    outline = Color(0xFF9A8170)
)

private val WarmDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59E),
    secondary = Color(0xFF83D5C9),
    tertiary = Color(0xFFFFB77B)
)

@Composable
fun VendorAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) WarmDarkColors else WarmLightColors,
        content = content
    )
}
