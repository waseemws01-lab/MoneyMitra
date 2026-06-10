package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = MitraTextMain,
    surface = LightSurface,
    onSurface = MitraTextMain,
    surfaceVariant = LightBorder,
    onSurfaceVariant = MitraTextSecondary,
    outline = MitraBorder,
    error = MitraErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force false to prevent dark theme per user request
    dynamicColor: Boolean = false, // Disable dynamic colors to keep MoneyMitra signature branding!
    content: @Composable () -> Unit,
) {
    // We strictly use our custom LightColorScheme for clean fintech green & white branding
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
