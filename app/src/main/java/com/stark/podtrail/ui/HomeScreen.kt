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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.clickable
import com.stark.podtrail.PodcastCard
import com.stark.podtrail.data.PodcastWithStats

@Composable
fun HomeScreen(
    vm: PodcastViewModel, 
    onOpenPodcast: (Podcast) -> Unit,
    onOpenEpisode: (Episode) -> Unit
) {
    val podcasts by vm.podcasts.collectAsState()
    val upNext by vm.upNext.collectAsState()
    
    // Split UP NEXT into "Continue Watching" (started) and "On Deck" (new) if needed.
    // For now, vm.upNext returns next unplayed.
    // We can filter `upNext` for playbackPosition > 0 for "Continue".
    
    val continueListening = upNext.filter { it.playbackPosition > 0 }
    val onDeck = upNext.filter { it.playbackPosition == 0L }

    if (podcasts.isEmpty()) {
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
            Text(
                "Your Library", 
                style = MaterialTheme.typography.titleLarge, 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        items(podcasts) { pStats ->
             PodcastCard(
                 podcast = pStats.podcast,
                 stats = pStats,
                 onClick = { onOpenPodcast(pStats.podcast) },
                 onInfoClick = { /* Handle info click via callback if needed, or pass lambda */ }
             )
        }
    }
}
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
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(8.dp))
                Text("Left: ${formatDuration((episode.durationMillis ?: 0) - episode.playbackPosition)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun UpNextCard(episode: Episode, onClick: () -> Unit) {
     Card(
        modifier = Modifier.width(160.dp).height(200.dp),
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
                Text(episode.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, minLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(formatDate(episode.pubDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

