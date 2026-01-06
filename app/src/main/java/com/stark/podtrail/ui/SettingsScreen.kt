package com.stark.podtrail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.AppSettings
import com.stark.podtrail.data.SettingsRepository
import com.stark.podtrail.data.ThemeMode
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.stark.podtrail.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: SettingsRepository,
    currentSettings: AppSettings,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Appearance Section
            item { PreferenceCategory("Appearance") }
            
            // Theme Mode
            item {
                var showThemeDialog by remember { mutableStateOf(false) }
                ClickablePreference(
                    title = "Theme",
                    subtitle = currentSettings.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.DarkMode,
                    onClick = { showThemeDialog = true }
                )

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Choose Theme") },
                        text = {
                            Column {
                                ThemeMode.values().forEach { mode ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { 
                                                scope.launch { repo.setThemeMode(mode) }
                                                showThemeDialog = false 
                                            }
                                    ) {
                                        RadioButton(
                                            selected = (mode == currentSettings.themeMode),
                                            onClick = null // handled by row
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }

            item {
                SwitchPreference(
                    title = "Dynamic Color",
                    subtitle = "Use wallpaper colors (Android 12+)",
                    icon = Icons.Default.Palette,
                    checked = currentSettings.useDynamicColor,
                    onCheckedChange = { scope.launch { repo.setDynamicColor(it) } }
                )
            }

            if (currentSettings.themeMode != ThemeMode.LIGHT) {
                item {
                    SwitchPreference(
                        title = "AMOLED Dark",
                        subtitle = "Use pure black background",
                        icon = Icons.Default.Brightness2,
                        checked = currentSettings.useAmoled,
                        onCheckedChange = { scope.launch { repo.setAmoled(it) } }
                    )
                }
            }

            if (!currentSettings.useDynamicColor) {
                item {
                    Text(
                        "Custom Color",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    // Custom Color Picker Grid
                     val colors = listOf(
                        0xFF6200EE.toInt(), 0xFF3700B3.toInt(), 0xFFBB86FC.toInt(), 0xFFB71C1C.toInt(),
                        0xFFD32F2F.toInt(), 0xFF1B5E20.toInt(), 0xFF4CAF50.toInt(), 0xFF0D47A1.toInt(),
                        0xFF2196F3.toInt(), 0xFF006064.toInt(), 0xFF00BCD4.toInt(), 0xFFE65100.toInt(),
                        0xFFFF9800.toInt(), 0xFF3E2723.toInt(), 0xFF795548.toInt(), 0xFF212121.toInt()
                    )
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.take(8).forEach { color ->
                                ColorItem(color, currentSettings.customColor, scope, repo)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.drop(8).take(7).forEach { color -> // Take 7 to make room for custom button
                                ColorItem(color, currentSettings.customColor, scope, repo)
                            }
                            
                            // Custom Color Button
                            var showColorPicker by remember { mutableStateOf(false) }
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .clickable { showColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (showColorPicker) {
                                ColorPickerDialog(
                                    initialColor = currentSettings.customColor,
                                    onDismiss = { showColorPicker = false },
                                    onColorSelected = { color -> 
                                        scope.launch { repo.setCustomColor(color) }
                                        showColorPicker = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            // Storage & Data Section
            item { PreferenceCategory("Storage & Data") }

            item {
                 val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/gzip")
                ) { uri ->
                    if (uri != null) {
                        scope.launch { repo.exportDatabase(uri) }
                    }
                }
                ClickablePreference(
                    title = "Export Backup (GZIP)",
                    subtitle = "Backup your data to a compressed JSON file",
                    icon = Icons.Default.Upload,
                    onClick = { exportLauncher.launch("podtrail_backup.json.gz") }
                )
            }

            item {
                val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            if (repo.importDatabase(uri)) {
                                // For full restore, it's often safer to restart to ensure all in-memory caches (like ViewModels) are cleared.
                                // Although Flows should update, global state or non-reactive components might be stale.
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                context.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        }
                    }
                }
                ClickablePreference(
                    title = "Import Backup (GZIP)",
                    subtitle = "Restore data (Overwrites current!)",
                    icon = Icons.Default.Download,
                    onClick = { importLauncher.launch("application/gzip") }
                )
            }

             item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            // About Section
            item { PreferenceCategory("About") }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Icon
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            text = "PodTrail",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val version = remember {
                             try {
                                 context.packageManager.getPackageInfo(context.packageName, 0).versionName
                             } catch (e: Exception) { "1.0" }
                        }
                        
                        Text(
                            text = "Version $version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Action Rows
                        AboutActionRow(
                            icon = Icons.Default.Code, 
                            label = "Source Code", 
                            onClick = { uriHandler.openUri("https://github.com/SV-stark/PodTrail") }
                        )
                        AboutActionRow(
                            icon = Icons.Default.Person, 
                            label = "Developed by SV-stark", 
                            onClick = { uriHandler.openUri("https://github.com/SV-stark") }
                        )
                         AboutActionRow(
                            icon = Icons.Default.Description, 
                            label = "License (GPL v3)", 
                            onClick = { uriHandler.openUri("https://github.com/SV-stark/PodTrail/blob/main/LICENSE") }
                        )
                    }
                }
            }
            
            item {
                 Box(
                     modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
                     contentAlignment = Alignment.Center
                 ) {
                     Text(
                         text = "Made with ❤️ in Kotlin and Jetpack Compose",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                     )
                 }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun AboutActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
