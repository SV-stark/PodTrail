package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.AppSettings
import com.stark.podtrail.data.SettingsRepository
import com.stark.podtrail.data.ThemeMode
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
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
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
                            colors.drop(8).take(8).forEach { color ->
                                ColorItem(color, currentSettings.customColor, scope, repo)
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
                    androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/x-sqlite3")
                ) { uri ->
                    if (uri != null) {
                        scope.launch { repo.exportDatabase(uri) }
                    }
                }
                ClickablePreference(
                    title = "Export Database",
                    subtitle = "Backup your data to a file",
                    icon = Icons.Default.Upload,
                    onClick = { exportLauncher.launch("backup.db") }
                )
            }

            item {
                val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            if (repo.importDatabase(uri)) {
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                context.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        }
                    }
                }
                ClickablePreference(
                    title = "Import Database",
                    subtitle = "Restore data (Overwrites current!)",
                    icon = Icons.Default.Download,
                    onClick = { importLauncher.launch("*/*") }
                )
            }

             item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            // About Section
            item { PreferenceCategory("About") }
            
            item {
                ClickablePreference(
                    title = "Source Code",
                    subtitle = "github.com/SV-stark/PodTrail",
                    icon = Icons.Default.Code,
                    onClick = { uriHandler.openUri("https://github.com/SV-stark/PodTrail") }
                )
            }
            
            item {
                ClickablePreference(
                    title = "Developer",
                    subtitle = "SV-stark",
                    icon = Icons.Default.Person,
                    onClick = { uriHandler.openUri("https://github.com/SV-stark") }
                )
            }

            item {
                val version = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Exception) { "Unknown" }
                }
                ClickablePreference(
                    title = "Version",
                    subtitle = version,
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
             item {
                ClickablePreference(
                    title = "License",
                    subtitle = "GPL v3 License",
                    icon = Icons.Default.Description,
                    onClick = { uriHandler.openUri("https://github.com/SV-stark/PodTrail/blob/main/LICENSE") }
                )
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
