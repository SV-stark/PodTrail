package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.stark.podtrail.data.Podcast
import com.stark.podtrail.data.Episode
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.clickable
import com.stark.podtrail.PodcastCard
import com.stark.podtrail.data.PodcastWithStats

@Composable
fun HomeScreen(
    vm: PodcastViewModel, 
    onOpenPodcast: (Podcast) -> Unit,
    onOpenEpisode: (Episode) -> Unit,
    onOpenPodcastInfo: (Podcast) -> Unit
) {
    val podcasts by vm.podcasts.collectAsState()
    val upNext by vm.upNext.collectAsState()
    
    // Split UP NEXT into "Continue Watching" (started) and "On Deck" (new) if needed.
    // For now, vm.upNext returns next unplayed.
    // We can filter `upNext` for playbackPosition > 0 for "Continue".
    
    val continueListening = upNext.filter { it.playbackPosition > 0 }
    val onDeck = upNext.filter { it.playbackPosition == 0L }

    val isRefreshing by vm.isRefreshing.collectAsState()
    // var showInfoPodcast by remember { mutableStateOf<Podcast?>(null) } // Removed local state
    var isGridView by rememberSaveable { mutableStateOf(false) }

    if (podcasts.isEmpty() && !isRefreshing) {
        com.stark.podtrail.ui.EmptyState(
             icon = Icons.Default.PlayArrow,
             title = "Your library is empty",
             message = "Subscribe to podcasts to see episodes here.",
             action = null
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Refresh Indicator
        if (isRefreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // 1. Continue Listening (Shelf)
        if (continueListening.isNotEmpty()) {
            item {
                Text(
                    "Continue Listening", 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(continueListening) { episode ->
                        ContinueListeningCard(episode, onClick = { onOpenEpisode(episode) })
                    }
                }
            }
        }
        
        // 2. Up Next (Deck/Row) - episodes from subscribed podcasts
        if (onDeck.isNotEmpty()) {
            item {
                Text(
                    "Up Next", 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(onDeck) { episode ->
                        UpNextCard(episode, onClick = { onOpenEpisode(episode) })
                    }
                }
            }
        }
        
        // 3. All Podcasts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Library", 
                    style = MaterialTheme.typography.titleLarge
                )
                
                // View Toggle
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Switch View"
                    )
                }
            }
        }
        
        if (isGridView) {
            items(podcasts.chunked(3)) { rowPodcasts ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (pStats in rowPodcasts) {
                        Box(Modifier.weight(1f)) {
                            PodcastGridCard(
                                podcast = pStats.podcast,
                                stats = pStats,
                                onClick = { onOpenPodcast(pStats.podcast) }
                            )
                        }
                    }
                    // Fill empty space if row has fewer than 3 items
                    if (rowPodcasts.size < 3) {
                        repeat(3 - rowPodcasts.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            items(podcasts) { pStats ->
                 PodcastCard(
                     podcast = pStats.podcast,
                     stats = pStats,
                     onClick = { onOpenPodcast(pStats.podcast) },
                     onInfoClick = { onOpenPodcastInfo(pStats.podcast) }
                 )
            }
        }
    }
    
    // Removed PodcastInfoDialog call
}

@Composable
fun PodcastInfoDialog(
    podcast: Podcast,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    if (podcast.id == 0L) return // Sanity check

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = podcast.title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                if (!podcast.imageUrl.isNullOrEmpty()) {
                     AsyncImage(
                        model = podcast.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = podcast.description ?: "No description available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            IconButton(onClick = {
                onToggleFavorite()
            }) {
                Icon(
                    imageVector = if (podcast.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (podcast.isFavorite) "Unfavorite" else "Favorite",
                    tint = if (podcast.isFavorite) androidx.compose.ui.graphics.Color.Red else LocalContentColor.current
                )
            }
        }
    )
}

@Composable
fun ContinueListeningCard(episode: Episode, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).height(100.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxSize()) {
           AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.width(100.dp).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                Text(episode.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                // Progress Bar
                val progress = if (episode.durationMillis != null && episode.durationMillis > 0) 
                    episode.playbackPosition.toFloat() / episode.durationMillis.toFloat()
                else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(8.dp))
                Text("Left: ${formatDuration((episode.durationMillis ?: 0) - episode.playbackPosition)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun UpNextCard(episode: Episode, onClick: () -> Unit) {
     Card(
        modifier = Modifier.width(160.dp).height(240.dp), // Increased height to prevent clipping
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(8.dp)) {
                Text(
                    episode.title, 
                    style = MaterialTheme.typography.labelMedium, 
                    maxLines = 2, 
                    minLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDate(episode.pubDate), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (episode.durationMillis != null && episode.durationMillis > 0) {
                         Text(
                            formatDuration(episode.durationMillis), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val mins = millis / 60000
    return "${mins}m"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}

@Composable
fun PodcastGridCard(
    podcast: Podcast,
    stats: PodcastWithStats,
    onClick: () -> Unit
) {
    val remaining = stats.totalEpisodes - stats.listenedEpisodes
    val percentage = if (stats.totalEpisodes > 0) (stats.listenedEpisodes.toFloat() / stats.totalEpisodes.toFloat() * 100).toInt() else 0
    val progress = if (stats.totalEpisodes > 0) stats.listenedEpisodes.toFloat() / stats.totalEpisodes.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f), // Slightly taller than square to accommodate title if needed, or just square
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Overlays
                // Top Left: Percentage
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp
                    )
                }

                // Top Right: Remaining Episodes
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$remaining left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp
                    )
                }
                
                // Bottom: Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                podcast.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

