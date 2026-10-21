package com.example.domino_game.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    secondary = DarkGrayText,
    tertiary = Pink80,
    background = PureBlack,
    surface = PureBlack,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = PureWhite,
    onSurface = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PureWhite,
    secondary = DarkGrayText,
    tertiary = Pink40,
    background = PureBlack,     // Forzamos negro también aquí para que siempre sea oscuro
    surface = PureBlack,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = PureWhite,
    onSurface = PureWhite
)


@Composable
fun Domino_GameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cambia 'true' por 'false' aquí:
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