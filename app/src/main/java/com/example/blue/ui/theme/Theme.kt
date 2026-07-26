package com.example.blue.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Slate80,
    tertiary = Rose80,
    background = Color(0xFF101820),
    surface = Color(0xFF16232C),
    surfaceVariant = Color(0xFF263A48),
    onBackground = Color(0xFFE4EEF5),
    onSurface = Color(0xFFE4EEF5),
    onSurfaceVariant = Color(0xFFC1D2DE),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Slate40,
    tertiary = Rose40,
    primaryContainer = Color(0xFFD4E2ED),
    onPrimaryContainer = Color(0xFF29485F),
    secondaryContainer = Color(0xFFE0EBF5),
    onSecondaryContainer = Color(0xFF29485F),
    background = Color(0xFFE0EBF5),
    onBackground = Color(0xFF243845),
    surface = Color(0xFFF6FAFD),
    onSurface = Color(0xFF243845),
    surfaceVariant = Color(0xFFD4E2ED),
    onSurfaceVariant = Color(0xFF587184),
    outline = Color(0xFF8198A8),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun BlueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
