package com.example.ssmapplication3.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SsmBlueLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = SsmBlueContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = SsmGreenSuccess,
    background = SsmBackgroundDark,
    onBackground = SsmTextPrimaryDark,
    surface = SsmSurfaceDark,
    onSurface = SsmTextPrimaryDark,
    surfaceVariant = SsmSurfaceVariantDark,
    onSurfaceVariant = SsmTextSecondaryDark,
    outline = SsmOutlineDark,
    error = SsmRedError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SsmBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SsmBlueContainerLight,
    onPrimaryContainer = Color(0xFF062E73),
    secondary = SsmTextSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = SsmGreenSuccess,
    background = SsmBackgroundLight,
    onBackground = SsmTextPrimaryLight,
    surface = SsmSurfaceLight,
    onSurface = SsmTextPrimaryLight,
    surfaceVariant = SsmSurfaceVariantLight,
    onSurfaceVariant = SsmTextSecondaryLight,
    outline = SsmOutlineLight,
    error = SsmRedError,
    onError = Color.White
)

@Composable
fun SSMApplication3Theme(
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
        typography = AppTypography,
        content = content
    )
}
