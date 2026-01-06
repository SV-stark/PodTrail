package com.stark.podtrail.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), // More rounded for cards/dialogs
    large = RoundedCornerShape(24.dp),  // Very rounded for specific large containers
    extraLarge = RoundedCornerShape(32.dp) // Fully rounded for bottom sheets/large surfaces
)
