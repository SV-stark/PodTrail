package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stark.podtrail.storage.*
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(
    storageManager: StorageManager,
    onBack: () -> Unit
) {
    var storageStats by remember { mutableStateOf<StorageStats?>(null) }
    
    // Helper data class for UI
    data class CleanupDisplayInfo(
        val title: String,
        val description: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val available: Boolean
    )
    var isLoading by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf<CleanupOption?>(null) }
    var cleanupResults by remember { mutableStateOf<List<CleanupResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isLoading = true
        storageStats = storageManager.getStorageStats()
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ResponsiveDimensions.spacingSmall()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Storage Management",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (isLoading) {
            LoadingFullScreen("Loading storage information...")
            return@Column
        }
        
        storageStats?.let { stats ->
            PullToRefreshWrapper(
                isRefreshing = false,
                onRefresh = {
                    scope.launch {
                        storageStats = storageManager.getStorageStats()
                    }
                }
            ) { padding ->
                val layoutDirection = LocalLayoutDirection.current
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection) + ResponsiveDimensions.spacingSmall(),
                        top = padding.calculateTopPadding() + ResponsiveDimensions.spacingSmall(),
                        end = padding.calculateEndPadding(layoutDirection) + ResponsiveDimensions.spacingSmall(),
                        bottom = padding.calculateBottomPadding() + ResponsiveDimensions.spacingSmall()
                    ),
                    verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingMedium())
                ) {
                    // Storage Overview
                    item {
                        StorageOverviewCard(stats = stats)
                    }
                    
                    // Cleanup Options
                    item {
                        Text(
                            text = "Cleanup Options",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = ResponsiveDimensions.spacingTiny())
                        )
                    }
                    
                    items(getCleanupOptions(stats)) { option ->
                        CleanupOptionCard(
                            option = option,
                            stats = stats,
                            onClick = { showCleanupDialog = option }
                        )
                    }
                    
                    // Auto Cleanup
                    item {
                        AutoCleanupCard(
                            onPerformAutoCleanup = {
                                scope.launch {
                                    isLoading = true
                                    cleanupResults = storageManager.autoCleanup()
                                    isLoading = false
                                }
                            }
                        )
                    }
                    
                    // Cleanup Results
                    if (cleanupResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Cleanup Results",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = ResponsiveDimensions.spacingTiny())
                            )
                        }
                        items(cleanupResults) { result ->
                            CleanupResultCard(result)
                        }
                    }
                }
            }
        }
        
        // Cleanup Confirmation Dialog
        showCleanupDialog?.let { option ->
            CleanupConfirmationDialog(
                option = option,
                stats = storageStats!!,
                onConfirm = {
                    scope.launch {
                        isLoading = true
                        cleanupResults = cleanupResults + storageManager.performCleanup(option)
                        isLoading = false
                        // refresh stats
                        storageStats = storageManager.getStorageStats()
                        showCleanupDialog = null
                    }
                },
                onDismiss = { showCleanupDialog = null }
            )
        }
    }
}

@Composable
fun StorageOverviewCard(stats: StorageStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(ResponsiveDimensions.spacingMedium())
        ) {
            Text(
                text = "Storage Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${stats.totalEpisodes}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Episodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f MB".format(stats.totalSizeMB),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Estimated Size",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (stats.oldUnlistenedEpisodes > 0 || stats.episodesWithoutDescription > 0) {
                Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                
                if (stats.oldUnlistenedEpisodes > 0) {
                    Text(
                        text = "⚠️ ${stats.oldUnlistenedEpisodes} old unlistened episodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (stats.episodesWithoutDescription > 0) {
                    Text(
                        text = "ℹ️ ${stats.episodesWithoutDescription} episodes with minimal descriptions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun CleanupOptionCard(
    option: CleanupOption,
    stats: StorageStats,
    onClick: () -> Unit
) {
    val info = when (option) {
        CleanupOption.OLD_UNLISTENED_EPISODES -> CleanupDisplayInfo(
            "Remove Old Unlistened Episodes",
            "Delete episodes older than 6 months that haven't been listened to",
            Icons.Default.Delete,
            stats.oldUnlistenedEpisodes > 0
        )
        CleanupOption.TRUNCATE_DESCRIPTIONS -> CleanupDisplayInfo(
            "Truncate Long Descriptions",
            "Limit episode descriptions to 200 characters to save space",
            Icons.Default.TextSnippet,
            true
        )
        CleanupOption.REMOVE_INACTIVE_PODCASTS -> CleanupDisplayInfo(
            "Remove Inactive Podcasts",
            "Delete podcasts not updated in the last year",
            Icons.Default.Podcasts,
            stats.podcastsLastUpdated.values.any { it < System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000 }
        )
        CleanupOption.COMPACT_DATABASE -> CleanupDisplayInfo(
            "Compact Database",
            "Optimize database file size and improve performance",
            Icons.Default.Storage,
            true
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        enabled = info.available,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingMedium()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                modifier = Modifier.size(ResponsiveDimensions.iconSizeMedium()),
                tint = if (info.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingMedium()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (info.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (info.available) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!info.available) {
                Text(
                    text = "Not Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
// Define data class at top level or use the one defined in StorageManagementScreen if accessible?
// Better to define it outside.
data class CleanupDisplayInfo(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val available: Boolean
)

@Composable
fun AutoCleanupCard(
    onPerformAutoCleanup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(ResponsiveDimensions.spacingMedium())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingSmall()))
                Text(
                    text = "Auto Cleanup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
            Text(
                text = "Automatically clean up old data and optimize storage based on predefined thresholds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
            Button(
                onClick = onPerformAutoCleanup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Perform Auto Cleanup")
            }
        }
    }
}

@Composable
fun CleanupResultCard(result: CleanupResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingSmall()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingSmall()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (result.option) {
                        CleanupOption.OLD_UNLISTENED_EPISODES -> "Old Episodes Removed"
                        CleanupOption.TRUNCATE_DESCRIPTIONS -> "Descriptions Truncated"
                        CleanupOption.REMOVE_INACTIVE_PODCASTS -> "Inactive Podcasts Removed"
                        CleanupOption.COMPACT_DATABASE -> "Database Compacted"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${result.itemsAffected} items affected, %.2f MB saved".format(result.spaceSavedMB),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CleanupConfirmationDialog(
    option: CleanupOption,
    stats: StorageStats,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (option) {
        CleanupOption.OLD_UNLISTENED_EPISODES -> Pair(
            "Remove Old Unlistened Episodes",
            "This will permanently delete ${stats.oldUnlistenedEpisodes} episodes older than 6 months that haven't been listened to. This action cannot be undone."
        )
        CleanupOption.TRUNCATE_DESCRIPTIONS -> Pair(
            "Truncate Long Descriptions",
            "This will reduce all episode descriptions to 200 characters to save storage space. This action cannot be undone."
        )
        CleanupOption.REMOVE_INACTIVE_PODCASTS -> Pair(
            "Remove Inactive Podcasts",
            "This will permanently remove podcasts that haven't been updated in the last year. This action cannot be undone."
        )
        CleanupOption.COMPACT_DATABASE -> Pair(
            "Compact Database",
            "This will optimize the database file to reduce size and improve performance. This may take a few moments."
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getCleanupOptions(stats: StorageStats): List<CleanupOption> {
    return listOf(
        CleanupOption.OLD_UNLISTENED_EPISODES,
        CleanupOption.TRUNCATE_DESCRIPTIONS,
        CleanupOption.REMOVE_INACTIVE_PODCASTS,
        CleanupOption.COMPACT_DATABASE
    )
}