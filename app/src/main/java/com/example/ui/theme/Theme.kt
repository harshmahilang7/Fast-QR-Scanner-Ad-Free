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

private val StitchDarkColorScheme = darkColorScheme(
    primary = GoogleBlueDark,
    onPrimary = Color(0xFF002F6C),
    primaryContainer = GoogleBlueContainer,
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = StitchLaserCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFA6EEFF),
    tertiary = GoogleGreenDark,
    onTertiary = Color(0xFF00391A),
    background = StitchDarkBackground,
    onBackground = StitchTextPrimaryDark,
    surface = StitchDarkSurface,
    onSurface = StitchTextPrimaryDark,
    surfaceVariant = StitchDarkSurfaceContainer,
    onSurfaceVariant = StitchTextSecondaryDark,
    surfaceContainer = StitchDarkSurfaceContainer,
    surfaceContainerHigh = StitchDarkSurfaceContainerHigh,
    surfaceContainerHighest = StitchDarkSurfaceContainerHighest,
    outline = Color(0xFF484F58)
)

private val StitchLightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueLightContainer,
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00677D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5E4F8),
    onSecondaryContainer = Color(0xFF001F28),
    tertiary = GoogleGreen,
    onTertiary = Color.White,
    background = StitchLightBackground,
    onBackground = StitchTextPrimaryLight,
    surface = StitchLightSurface,
    onSurface = StitchTextPrimaryLight,
    surfaceVariant = StitchLightSurfaceContainer,
    onSurfaceVariant = StitchTextSecondaryLight,
    surfaceContainer = StitchLightSurfaceContainer,
    surfaceContainerHigh = StitchLightSurfaceContainerHigh,
    surfaceContainerHighest = StitchLightSurfaceContainerHighest,
    outline = Color(0xFFBDC1C6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StitchDarkColorScheme
        else -> StitchLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
