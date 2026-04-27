package com.sol.dopaminetrap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = BrandIndigo,
    onPrimary            = Color.White,
    primaryContainer     = BrandIndigoContainer,
    onPrimaryContainer   = BrandIndigoDark,
    secondary            = BrandEmerald,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD1FAE5),
    onSecondaryContainer = BrandEmeraldDark,
    background           = Color(0xFFF4F5FB),
    onBackground         = Color(0xFF1A1A2E),
    surface              = Color.White,
    onSurface            = Color(0xFF1A1A2E),
    surfaceVariant       = Color(0xFFEEEFF8),
    onSurfaceVariant     = Color(0xFF5A5A72),
    outline              = Color(0xFFCBCBE0),
    error                = StatusRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFEDED),
    onErrorContainer     = Color(0xFFB71C1C)
)

private val DarkColorScheme = darkColorScheme(
    primary              = BrandIndigoNight,
    onPrimary            = BrandIndigoDark,
    primaryContainer     = BrandIndigo,
    onPrimaryContainer   = Color.White,
    secondary            = BrandEmerald,
    onSecondary          = Color.White,
    background           = BackgroundDark,
    onBackground         = Color(0xFFE8E8FF),
    surface              = SurfaceDark,
    onSurface            = Color(0xFFE8E8FF),
    surfaceVariant       = Color(0xFF252438),
    onSurfaceVariant     = Color(0xFFAAAAAC),
    outline              = Color(0xFF3A3A55),
    error                = Color(0xFFFF6B6B),
    onError              = Color.White,
    errorContainer       = Color(0xFF4A1515),
    onErrorContainer     = Color(0xFFFFCDD2)
)

@Composable
fun SafelandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
