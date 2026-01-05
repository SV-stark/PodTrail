package com.stark.podtrail.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Collections
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stark.podtrail.data.AppSettings
import com.stark.podtrail.data.PodcastWithStats
import com.stark.podtrail.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    vm: PodcastViewModel,
    settingsRepo: SettingsRepository,
    appSettings: AppSettings
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Stats from VM
    val podcasts by vm.podcasts.collectAsState()
    val totalPodcasts = podcasts.size
    val totalEpisodesListened = podcasts.sumOf { it.listenedEpisodes }
    val totalTimeListened by vm.totalTimeListened.collectAsState()
    val currentStreak by vm.currentStreak.collectAsState()
    val badges by vm.badges.collectAsState()
    
    // Genre Breakdown
    val genreMap = remember(podcasts) {
        podcasts.groupBy { it.podcast.primaryGenre ?: "Other" }
            .mapValues { entry -> entry.value.size }
    }

    // Image Pickers
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                settingsRepo.setProfileImage(uri.toString())
            }
        }
    }

    val bgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                settingsRepo.setProfileBg(uri.toString())
            }
        }
    }

    // State for background selection
    var showBgSelectionDialog by remember { mutableStateOf(false) }
    var showPodcastPicker by remember { mutableStateOf(false) }

    if (showBgSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showBgSelectionDialog = false },
            title = { Text("Change Background") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Choose from Library") },
                        leadingContent = { Icon(Icons.Default.Collections, null) },
                        modifier = Modifier.clickable { 
                            showBgSelectionDialog = false
                            showPodcastPicker = true
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Custom Image") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable { 
                            showBgSelectionDialog = false
                            bgImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBgSelectionDialog = false }) { Text("Cancel") } }
        )
    }

    if (showPodcastPicker) {
        AlertDialog(
            onDismissRequest = { showPodcastPicker = false },
            title = { Text("Select Podcast") },
            text = {
                Box(Modifier.height(300.dp)) {
                    if (podcasts.isEmpty()) {
                        Text("No podcasts subscribed.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(podcasts) { pStats ->
                                AsyncImage(
                                    model = pStats.podcast.imageUrl,
                                    contentDescription = pStats.podcast.title,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch { settingsRepo.setProfileBg(pStats.podcast.imageUrl ?: "") }
                                            showPodcastPicker = false
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPodcastPicker = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Background Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { showBgSelectionDialog = true }
            ) {
                if (appSettings.profileBgUri != null) {
                    AsyncImage(
                        model = appSettings.profileBgUri,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Overlay for contrast
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                } else {
                    // Dynamic Gradient Fallback (using primary color)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                }
            }

            // Profile Picture Layer
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(120.dp)
                    .offset(y = 0.dp) // Sits right on the edge of the Box context, but we want it overlapping
            ) {
                 AsyncImage(
                     model = appSettings.profileImageUri ?: "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y", // Generic default
                     contentDescription = "Profile Picture",
                     contentScale = ContentScale.Crop,
                     modifier = Modifier
                         .fillMaxSize()
                         .clip(CircleShape)
                         .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                         .clickable { 
                             profileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                         }
                 )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Username (Static for now, could be added to settings)
        Text(
            text = "User Profile",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))

        // Stats Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Episodes\nListened",
                value = totalEpisodesListened.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Podcasts\nAdded",
                value = totalPodcasts.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))

        // Time & Streak Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Time\nListened",
                value = formatTimeListened(totalTimeListened),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Current\nStreak",
                value = "$currentStreak days",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Badges Section
        Text(
            text = "Badges",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                 BadgeCard(badge)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Donut Chart Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                if (totalPodcasts == 0) {
                     Text("No podcasts added yet.", modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Legend
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val colors = listOf(
                                Color(0xFF4CAF50), // Green
                                Color(0xFF2196F3), // Blue
                                Color(0xFFFFC107), // Amber
                                Color(0xFFF44336), // Red
                                Color(0xFF9C27B0), // Purple
                                Color(0xFF00BCD4)  // Cyan
                            )
                            
                            var colorIndex = 0
                            genreMap.entries.sortedByDescending { it.value }.take(5).forEach { entry ->
                                val color = colors[colorIndex % colors.size]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(12.dp).background(color, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                colorIndex++
                            }
                        }

                        // Chart
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                             Canvas(modifier = Modifier.size(120.dp)) {
                                 val total = totalPodcasts.toFloat()
                                 var startAngle = -90f
                                 val colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF2196F3),
                                    Color(0xFFFFC107),
                                    Color(0xFFF44336),
                                    Color(0xFF9C27B0),
                                    Color(0xFF00BCD4)
                                )
                                var colorIndex = 0
                                
                                genreMap.entries.sortedByDescending { it.value }.forEach { entry ->
                                    val sweepAngle = (entry.value / total) * 360f
                                    drawArc(
                                        color = colors[colorIndex % colors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                    colorIndex++
                                }
                             }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun BadgeCard(badge: com.stark.podtrail.ui.Badge) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (badge.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(100.dp, 120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (badge.unlocked) badge.icon else Icons.Default.Lock,
                contentDescription = null,
                tint = if (badge.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                badge.name, 
                style = MaterialTheme.typography.labelMedium, 
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (badge.unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeListened(millis: Long): String {
    val seconds = millis / 1000
    val days = seconds / (24 * 3600)
    val hours = (seconds % (24 * 3600)) / 3600
    val minutes = (seconds % 3600) / 60
    
    return if (days > 0) "${days}d ${hours}h" else "${hours}h ${minutes}m"
}

