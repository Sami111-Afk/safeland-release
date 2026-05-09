package com.sol.dopaminetrap.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SafelandColorScheme = lightColorScheme(
    primary              = SfDark,
    onPrimary            = SfCream,
    primaryContainer     = SfBg2,
    onPrimaryContainer   = SfDark,
    secondary            = StatusGreen,
    onSecondary          = Color.White,
    secondaryContainer   = SfWarnBg,
    onSecondaryContainer = SfDark,
    background           = SfCream,
    onBackground         = SfDark,
    surface              = Color.White,
    onSurface            = SfDark,
    surfaceVariant       = SfBg2,
    onSurfaceVariant     = SfGray,
    outline              = SfBorder,
    error                = StatusRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFEDED),
    onErrorContainer     = Color(0xFFB71C1C),
)

@Composable
fun SafelandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SafelandColorScheme,
        typography  = Typography,
        content     = content
    )
}
