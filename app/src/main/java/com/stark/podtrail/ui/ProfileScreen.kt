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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val favoritePodcasts by vm.favoritePodcasts.collectAsState()

    // Image Pickers
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { settingsRepo.setProfileImage(uri.toString()) }
        }
    }

    val bgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { settingsRepo.setProfileBg(uri.toString()) }
        }
    }

    // State for background selection
    var showBgSelectionDialog by remember { mutableStateOf(false) }
    var showPodcastPicker by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    if (showBgSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showBgSelectionDialog = false },
            title = { Text("Change Cover") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Choose from Podcast Art") },
                        leadingContent = { Icon(Icons.Default.Collections, null) },
                        modifier = Modifier.clickable { 
                            showBgSelectionDialog = false
                            showPodcastPicker = true
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Upload Image") },
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
            title = { Text("Select Podcast Art") },
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

    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        vm.setUserName(tempName)
                        showNameEditDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNameEditDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header Section ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.TopCenter)
            ) {
                if (appSettings.profileBgUri != null) {
                    AsyncImage(
                        model = appSettings.profileBgUri,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showBgSelectionDialog = true }
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .clickable { showBgSelectionDialog = true }
                    )
                }
                
                // Edit Cover Button
                SmallFloatingActionButton(
                    onClick = { showBgSelectionDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Edit, "Edit Cover", modifier = Modifier.size(20.dp))
                }
            }

            // Profile Picture (Overlapping)
            Box(
                modifier = Modifier
                    .offset(y = 50.dp) // Push down to overlap half out
                    .size(120.dp)
            ) {
                 AsyncImage(
                     model = appSettings.profileImageUri ?: "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y",
                     contentDescription = "Profile Picture",
                     contentScale = ContentScale.Crop,
                     modifier = Modifier
                         .fillMaxSize()
                         .clip(CircleShape)
                         .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                         .clickable { 
                             profileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                         }
                 )
                 // Edit Profile Pic Badge
                 Box(
                     modifier = Modifier
                         .align(Alignment.BottomEnd)
                         .padding(4.dp)
                         .size(32.dp)
                         .background(MaterialTheme.colorScheme.primary, CircleShape)
                         .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         Icons.Default.PhotoCamera, 
                         contentDescription = null, 
                         tint = MaterialTheme.colorScheme.onPrimary,
                         modifier = Modifier.size(16.dp)
                     )
                 }
            }
        }

        Spacer(Modifier.height(60.dp)) // Clearance for profile pic

        // User Greeting
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    tempName = appSettings.userName ?: "PodTrail User"
                    showNameEditDialog = true 
                }
            ) {
                Text(
                    text = appSettings.userName ?: "PodTrail User",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Edit, "Edit Name", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- Statistics Section ---
        SectionHeader("Statistics")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Episodes",
                value = totalEpisodesListened.toString(),
                icon = Icons.Default.Headphones,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Time",
                value = formatTimeListenedShort(totalTimeListened),
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
         Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Subscribed",
                value = totalPodcasts.toString(),
                icon = Icons.Default.RssFeed,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Streak",
                value = "$currentStreak days",
                icon = Icons.Default.LocalFireDepartment,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        // --- Achievements Section ---
        SectionHeader("Achievements")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                 BadgeCard(badge)
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- Insights Section ---
        SectionHeader("Listening Insights")
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Genre Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                if (totalPodcasts == 0) {
                     Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                         Text("Subscribe to podcasts to see insights.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
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
                                Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107),
                                Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFF00BCD4)
                            )
                            
                            var colorIndex = 0
                            genreMap.entries.sortedByDescending { it.value }.take(5).forEach { entry ->
                                val color = colors[colorIndex % colors.size]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(12.dp).background(color, CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${entry.value}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                colorIndex++
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        // Chart
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                             Canvas(modifier = Modifier.size(120.dp)) {
                                 val total = totalPodcasts.toFloat()
                                 var startAngle = -90f
                                 val colors = listOf(
                                    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107),
                                    Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFF00BCD4)
                                )
                                var colorIndex = 0
                                
                                val gapAngle = 4f
                                genreMap.entries.sortedByDescending { it.value }.forEach { entry ->
                                    val sweepAngle = (entry.value / total) * 360f
                                    val drawSweep = if (totalPodcasts > 1) sweepAngle - gapAngle else sweepAngle
                                    
                                    if (drawSweep > 0) {
                                        drawArc(
                                            color = colors[colorIndex % colors.size],
                                            startAngle = startAngle + (if (totalPodcasts > 1) gapAngle / 2 else 0f),
                                            sweepAngle = drawSweep,
                                            useCenter = false,
                                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    startAngle += sweepAngle
                                    colorIndex++
                                }
                             }
                             // Center text
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                  Text(totalPodcasts.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                  Text("Subs", style = MaterialTheme.typography.labelSmall)
                             }
                        }
                    }
                }
            }
        }
        
        
        Spacer(Modifier.height(32.dp))

        // --- Favorites Section ---
        if (favoritePodcasts.isNotEmpty()) {
            SectionHeader("Favorites")
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(160.dp) // Fixed height for a row or small grid
            ) {
                items(favoritePodcasts) { p ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(100.dp)
                    ) {
                        AsyncImage(
                            model = p.imageUrl,
                            contentDescription = p.title,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun StatCard(
    title: String, 
    value: String, 
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha=0.4f)), // Lighter shade
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween, // Icon top, Content bottom
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            
            Column {
                Text(
                    value, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    title, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BadgeCard(badge: com.stark.podtrail.ui.Badge) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (badge.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(110.dp).height(130.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (badge.unlocked) MaterialTheme.colorScheme.primary.copy(alpha=0.2f) else MaterialTheme.colorScheme.surface.copy(alpha=0.5f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (badge.unlocked) badge.icon else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (badge.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                badge.name, 
                style = MaterialTheme.typography.labelMedium, 
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = if (badge.unlocked) FontWeight.Bold else FontWeight.Normal,
                color = if (badge.unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeListenedShort(millis: Long): String {
    val seconds = millis / 1000
    val days = seconds / (24 * 3600)
    val hours = (seconds % (24 * 3600)) / 3600
    val minutes = (seconds % 3600) / 60
    
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

