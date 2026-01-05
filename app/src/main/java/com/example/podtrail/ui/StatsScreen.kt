package com.example.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Podcasts
import com.example.podtrail.data.Episode
import com.example.podtrail.data.EpisodeListItem
import com.example.podtrail.EpisodeCard

private fun Episode.toListItem() = EpisodeListItem(
    id = id,
    podcastId = podcastId,
    title = title,
    pubDate = pubDate,
    imageUrl = imageUrl,
    episodeNumber = episodeNumber,
    durationMillis = durationMillis,
    listened = listened,
    listenedAt = listenedAt
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: PodcastViewModel, onBack: () -> Unit, onEpisodeClick: (Episode) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val history by vm.history.collectAsState()
    val upNext by vm.upNext.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Tracking Stats") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            }
        )
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Up Next") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") })
        }

        when (selectedTab) {
            0 -> UpNextList(upNext, vm, onEpisodeClick)
            1 -> HistoryList(history, vm, onEpisodeClick)
        }
    }
}

@Composable
fun UpNextList(episodes: List<Episode>, vm: PodcastViewModel, onEpisodeClick: (Episode) -> Unit) {
    if (episodes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No episodes up next. Subscribe to podcasts or check for new episodes!")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(episodes) { ep ->
                EpisodeCard(ep.toListItem(), onToggle = { vm.setListened(ep, !ep.listened) }, onDetails = { onEpisodeClick(ep) })
            }
        }
    }
}

@Composable
fun HistoryList(episodes: List<Episode>, vm: PodcastViewModel, onEpisodeClick: (Episode) -> Unit) {
    if (episodes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No history yet.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(episodes) { ep ->
                Column {
                    EpisodeCard(ep.toListItem(), onToggle = { vm.setListened(ep, !ep.listened) }, onDetails = { onEpisodeClick(ep) })
                    if (ep.listenedAt != null && ep.listenedAt > 0) {
                         Text(
                             text = "Listened on: ${java.text.SimpleDateFormat.getDateInstance().format(java.util.Date(ep.listenedAt))}",
                             style = MaterialTheme.typography.bodySmall,
                             modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp),
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                }
            }
        }
    }
}
