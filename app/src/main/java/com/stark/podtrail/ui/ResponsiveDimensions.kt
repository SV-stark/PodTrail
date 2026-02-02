package com.stark.podtrail.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ResponsiveDimensions {
    // Screen size categories
    enum class ScreenSize { SMALL, MEDIUM, LARGE }
    
    @Composable
    fun getScreenSize(): ScreenSize {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        return when {
            screenWidthDp < 600 -> ScreenSize.SMALL
            screenWidthDp < 840 -> ScreenSize.MEDIUM  
            else -> ScreenSize.LARGE
        }
    }
    
    // Responsive spacing
    @Composable
    fun spacingTiny() = when (getScreenSize()) {
        ScreenSize.SMALL -> 4.dp
        ScreenSize.MEDIUM -> 6.dp
        ScreenSize.LARGE -> 8.dp
    }
    
    @Composable
    fun spacingSmall() = when (getScreenSize()) {
        ScreenSize.SMALL -> 8.dp
        ScreenSize.MEDIUM -> 12.dp
        ScreenSize.LARGE -> 16.dp
    }
    
    @Composable
    fun spacingMedium() = when (getScreenSize()) {
        ScreenSize.SMALL -> 12.dp
        ScreenSize.MEDIUM -> 16.dp
        ScreenSize.LARGE -> 24.dp
    }
    
    @Composable
    fun spacingLarge() = when (getScreenSize()) {
        ScreenSize.SMALL -> 16.dp
        ScreenSize.MEDIUM -> 24.dp
        ScreenSize.LARGE -> 32.dp
    }
    
    @Composable
    fun spacingXLarge() = when (getScreenSize()) {
        ScreenSize.SMALL -> 24.dp
        ScreenSize.MEDIUM -> 32.dp
        ScreenSize.LARGE -> 48.dp
    }
    
    // Responsive sizes
    @Composable
    fun iconSizeSmall() = when (getScreenSize()) {
        ScreenSize.SMALL -> 16.dp
        ScreenSize.MEDIUM -> 20.dp
        ScreenSize.LARGE -> 24.dp
    }
    
    @Composable
    fun iconSizeMedium() = when (getScreenSize()) {
        ScreenSize.SMALL -> 20.dp
        ScreenSize.MEDIUM -> 24.dp
        ScreenSize.LARGE -> 32.dp
    }
    
    @Composable
    fun iconSizeLarge() = when (getScreenSize()) {
        ScreenSize.SMALL -> 32.dp
        ScreenSize.MEDIUM -> 40.dp
        ScreenSize.LARGE -> 48.dp
    }
    
    @Composable
    fun cornerRadiusSmall() = when (getScreenSize()) {
        ScreenSize.SMALL -> 4.dp
        ScreenSize.MEDIUM -> 6.dp
        ScreenSize.LARGE -> 8.dp
    }
    
    @Composable
    fun cornerRadiusMedium() = when (getScreenSize()) {
        ScreenSize.SMALL -> 8.dp
        ScreenSize.MEDIUM -> 12.dp
        ScreenSize.LARGE -> 16.dp
    }
    
    @Composable
    fun cornerRadiusLarge() = when (getScreenSize()) {
        ScreenSize.SMALL -> 12.dp
        ScreenSize.MEDIUM -> 16.dp
        ScreenSize.LARGE -> 24.dp
    }
    
    // Grid columns based on screen size
    @Composable
    fun getGridColumns(minItemWidth: Dp = 200.dp): Int {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        return (screenWidth / minItemWidth).toInt().coerceAtMost(4)
    }
    
    // Safe area insets
    @Composable
    fun getSafeAreaPadding() = with(LocalDensity.current) {
        WindowInsets.systemBars.getTop(this).toDp()
    }
}

// Extension functions for easier usage
@Composable
fun Dp.responsive() = this

@Composable
fun Int.dpResponsive() = this.dp