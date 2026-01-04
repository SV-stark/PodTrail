package com.example.podtrail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.podtrail.data.AppSettings
import com.example.podtrail.data.SettingsRepository
import com.example.podtrail.data.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: SettingsRepository,
    currentSettings: AppSettings,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            // Theme Mode
            Text("Theme Mode", style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                    FilterChip(
                        selected = currentSettings.themeMode == mode,
                        onClick = { scope.launch { repo.setThemeMode(mode) } },
                        label = { Text(mode.name.lowercase().capitalize()) },
                        leadingIcon = if (currentSettings.themeMode == mode) {
                            { Icon(Icons.Default.Check, null) }
                        } else null
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // Dynamic Color
            Row(
                Modifier.fillMaxWidth().clickable { scope.launch { repo.setDynamicColor(!currentSettings.useDynamicColor) } },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dynamic Color", style = MaterialTheme.typography.bodyLarge)
                    Text("Use wallpaper colors (Android 12+)", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = currentSettings.useDynamicColor,
                    onCheckedChange = { scope.launch { repo.setDynamicColor(it) } }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // AMOLED Mode (Only visible/relevant if Dark or System+Dark)
            Row(
                Modifier.fillMaxWidth().clickable { scope.launch { repo.setAmoled(!currentSettings.useAmoled) } },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("AMOLED Dark", style = MaterialTheme.typography.bodyLarge)
                    Text("Use pure black background", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = currentSettings.useAmoled,
                    onCheckedChange = { scope.launch { repo.setAmoled(it) } }
                )
            }
            
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            
            // Custom Color Picker (Simple predefined list)
            if (!currentSettings.useDynamicColor) {
                Text("Custom Theme Color", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val colors = listOf(
                        0xFF6200EE.toInt(), // Purple
                        0xFFB71C1C.toInt(), // Red
                        0xFF1B5E20.toInt(), // Green
                        0xFF0D47A1.toInt(), // Blue
                        0xFFE65100.toInt()  // Orange
                    )
                    
                    colors.forEach { color ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(color), androidx.compose.foundation.shape.CircleShape)
                                .clickable { scope.launch { repo.setCustomColor(color) } }
                        ) {
                            if (currentSettings.customColor == color) {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = null, 
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
