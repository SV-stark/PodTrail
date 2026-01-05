package com.stark.podtrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.shape.CircleShape

@Composable
fun ColorItem(color: Int, selectedColor: Int, scope: CoroutineScope, repo: SettingsRepository) {
    Box(
        Modifier
            .size(36.dp)
            .background(Color(color), CircleShape)
            .clickable { scope.launch { repo.setCustomColor(color) } }
    ) {
        if (selectedColor == color) {
            Icon(
                Icons.Default.Check, 
                contentDescription = null, 
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

