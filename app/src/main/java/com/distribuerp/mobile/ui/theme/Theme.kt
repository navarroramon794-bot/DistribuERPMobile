package com.distribuerp.mobile.ui.theme

import android.app.Activity
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
    primary = AzulPrincipal,
    onPrimary = Color.White,
    primaryContainer = InfoContainer,
    onPrimaryContainer = AzulOscuro,
    secondary = Naranja,
    onSecondary = Color.White,
    secondaryContainer = WarningContainer,
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = NaranjaBrillante,
    onTertiary = Color.White,
    background = FondoClaro,
    onBackground = TextoPrimario,
    surface = SurfaceClaro,
    onSurface = TextoPrimario,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextoSecundario,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF3E0000),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = AzulClaro,
    onPrimary = AzulOscuro,
    primaryContainer = InfoContainerOscuro,
    onPrimaryContainer = InfoOscuro,
    secondary = WarningOscuro,
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = WarningContainerOscuro,
    onSecondaryContainer = WarningOscuro,
    tertiary = NaranjaBrillante,
    onTertiary = Color(0xFF3E2723),
    background = FondoOscuro,
    onBackground = TextoPrimarioOscuro,
    surface = SurfaceOscuro,
    onSurface = TextoPrimarioOscuro,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextoSecundarioOscuro,
    error = ErrorOscuro,
    onError = Color(0xFF3E0000),
    errorContainer = ErrorContainerOscuro,
    onErrorContainer = ErrorOscuro,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

@Composable
fun DistribuERPMobileTheme(
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
        content = content
    )
}
