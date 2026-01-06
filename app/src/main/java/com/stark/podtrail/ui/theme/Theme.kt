package com.stark.podtrail.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0F5A56) // Deep Teal
val CreamBackground = Color(0xFFF7F4EB) // Cream/Off-white
val SurfaceCream = Color(0xFFFFFDF5) // Lighter Cream for cards
val DarkText = Color(0xFF1A1C19)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = TealPrimary,
    onSecondary = Color.White,
    tertiary = Color(0xFFD0BCFF),
    background = CreamBackground,
    onBackground = DarkText,
    surface = SurfaceCream,
    onSurface = DarkText,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = TealPrimary,
    onSecondary = Color.White,
    background = Color(0xFF101412), // Darker fallback for pure dark mode
    surface = Color(0xFF1E2321)
)

@Composable
fun PodTrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }


    val context = LocalContext.current
    // Ideally this should be injected or passed from MainActivity, but for simplicity we instantiate here or use a CompositionLocal.
    // However, recomposition issues might occur.
    // Better pattern: Pass the settings State to this function.
    // For now, let's assume the caller passes the necessary flags or we read them (but that requires a coroutine scope/flow collection inside a composable which is fine).
    
    // Changing signature to accept generic params would break call sites. 
    // Let's rely on the caller (MainActivity) to collect settings and pass them, 
    // OR we change this function to observe the repo. 
    // Given the prompt constraints, checking MainActivity shows we wrap content in PodTrailTheme.
    // I will overload or modify this one to take parameters that MainActivity will pass.
    
    // ACTUALLY, I will keep the signature compatible if possible or update MainActivity.
    // Let's update MainActivity to collect settings and pass them here.
    // So I will update this signature to take 'appSettings'.
    
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

@Composable
fun PodTrailTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    amoled: Boolean,
    customColor: Int,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
             // Generate a simple scheme from custom color
             val seed = androidx.compose.ui.graphics.Color(customColor)
             if (darkTheme) darkColorScheme(primary = seed) else lightColorScheme(primary = seed)
        }
    }
    
    // Apply AMOLED black if needed
    val finalScheme = if (darkTheme && amoled) {
        colorScheme.copy(
            background = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color.Black
        )
    } else colorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = finalScheme.background.toArgb() // Use background color for cleaner look or primary
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

