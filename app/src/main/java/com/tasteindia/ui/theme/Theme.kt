package com.tasteindia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed saffron/spice brand palette (consistent look on every device; light + dark supported).
private val LightColors = lightColorScheme(
    primary = Color(0xFFB34700),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3A1300),
    secondary = Color(0xFF8A5100),
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF4F6600),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF5DED3),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF5B2200),
    primaryContainer = Color(0xFF833400),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFFFB95C),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = Color(0xFFB2D24D),
    background = Color(0xFF201A17),
    surface = Color(0xFF201A17),
    surfaceVariant = Color(0xFF52443C),
    error = Color(0xFFFFB4AB),
)

@Composable
fun TasteIndiaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
