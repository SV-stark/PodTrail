package com.example.podtrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.podtrail.data.Episode
import com.example.podtrail.data.Podcast
import com.example.podtrail.ui.PodcastViewModel
import com.example.podtrail.ui.theme.PodTrailTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.MediaItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.filled.Sort
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.vector.rememberVectorPainter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PodTrailTheme {
                PodTrackApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodTrackApp(vm: PodcastViewModel = viewModel()) {
    var showSearch by remember { mutableStateOf(false) }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var playingEpisode by remember { mutableStateOf<Episode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PodTrack") }, actions = {
                IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Add, contentDescription = "Add") }
            })
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (playingEpisode != null) {
                PlayerScreen(episode = playingEpisode!!, vm = vm, onClose = { playingEpisode = null })
            } else if (showSearch) {
                SearchScreen(vm, onBack = { showSearch = false }, onPodcastAdded = { showSearch = false })
            } else if (selectedPodcast == null) {
                PodcastListScreen(vm) { podcast -> selectedPodcast = podcast }
            } else {
                EpisodeListScreen(vm, selectedPodcast!!.id,
                    onBack = { selectedPodcast = null },
                    onPlay = { ep -> playingEpisode = ep }
                )
            }
        }
    }
}

@Composable
fun SearchScreen(vm: PodcastViewModel, onBack: () -> Unit, onPodcastAdded: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    var directUrl by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Add Podcast", style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                vm.search(it)
            },
            label = { Text("Search Podcasts") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Direct URL Input (Fallback)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = directUrl,
                onValueChange = { directUrl = it },
                label = { Text("Or enter Feed URL directly") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (directUrl.isNotBlank()) {
                    vm.addPodcast(directUrl) { }
                    onPodcastAdded()
                }
            }) { Text("Add") }
        }

        Spacer(Modifier.height(16.dp))

        // Results List
        LazyColumn {
            items(results) { result ->
                ListItem(
                    headlineContent = { Text(result.collectionName ?: "Unknown Title") },
                    supportingContent = { Text(result.artistName ?: "") },
                    leadingContent = {
                        AsyncImage(
                            model = result.artworkUrl600 ?: result.artworkUrl100,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                            error = rememberVectorPainter(Icons.Default.Podcasts)
                        )
                    },
                    trailingContent = {
                        Button(onClick = {
                            result.feedUrl?.let { url ->
                                vm.addPodcast(url) { }
                                onPodcastAdded()
                            }
                        }) { Text("Add") }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PodcastListScreen(vm: PodcastViewModel, onOpen: (Podcast) -> Unit) {
    val podcasts by vm.podcasts.collectAsState()
    LazyColumn {
        items(podcasts) { p ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(p) },
                headlineContent = { Text(p.title) },
                supportingContent = { Text(p.feedUrl) },
                leadingContent = {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.Podcasts),
                        placeholder = rememberVectorPainter(Icons.Default.Podcasts)
                    )
                }
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(vm: PodcastViewModel, podcastId: Long, onBack: () -> Unit, onPlay: (Episode) -> Unit) {
    val episodes by vm.episodesFor(podcastId).collectAsState(initial = emptyList())
    val sortOrder by vm.sortOrder.collectAsState()

    Column {
        TopAppBar(
            title = { Text("Episodes") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = { vm.toggleSortOrder() }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
            }
        )
        LazyColumn {
            items(episodes) { ep ->
                EpisodeRow(ep, onToggle = { vm.setListened(ep, !ep.listened) }, onPlay = { onPlay(ep) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EpisodeRow(ep: Episode, onToggle: () -> Unit, onPlay: () -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        
        AsyncImage(
            model = ep.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).padding(end = 8.dp),
            contentScale = ContentScale.Crop,
            error = rememberVectorPainter(Icons.Default.Podcasts),
            placeholder = rememberVectorPainter(Icons.Default.Podcasts)
        )
        
        Column(Modifier.weight(1f).clickable { onPlay() }) {
            Text(ep.title, style = MaterialTheme.typography.bodyLarge)
            if (ep.pubDate > 0) Text(java.text.SimpleDateFormat.getDateInstance().format(java.util.Date(ep.pubDate)), style = MaterialTheme.typography.bodySmall)
            if (ep.episodeNumber != null) Text("Episode ${ep.episodeNumber}", style = MaterialTheme.typography.bodySmall)
            if (ep.durationMillis != null) Text("Duration ${formatMillis(ep.durationMillis)}", style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Checkbox(checked = ep.listened, onCheckedChange = { onToggle() })
        }
    }
}

private fun formatMillis(ms: Long): String {
    val s = ms / 1000
    val hh = s / 3600
    val mm = (s % 3600) / 60
    val ss = s % 60
    return if (hh > 0) String.format("%d:%02d:%02d", hh, mm, ss) else String.format("%02d:%02d", mm, ss)
}

@Composable
fun PlayerScreen(episode: Episode, vm: PodcastViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember {
        SimpleExoPlayer.Builder(context).build().also { p ->
            val media = episode.audioUrl?.let { MediaItem.fromUri(it) }
            if (media != null) p.setMediaItem(media)
            p.prepare()
        }
    }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    LaunchedEffect(player) {
        while (true) {
            if (!this.isActive) break
            isPlaying = player.isPlaying
            val pos = player.currentPosition
            val dur = if (player.duration > 0) player.duration else episode.durationMillis ?: 0L
            // Report progress to ViewModel (saves to DB and marks listened if threshold reached)
            vm.reportPlaybackProgress(episode.id, pos, if (dur <= 0) null else dur)
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    player.playWhenReady = false
                    onClose()
                }) { Icon(Icons.Default.ArrowBack, contentDescription = "Close") }
                Text(episode.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
            Spacer(Modifier.height(16.dp))
            Text(if (episode.episodeNumber != null) "Episode ${episode.episodeNumber}" else "", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Position: ${formatMillis(player.currentPosition)} / ${formatMillis(if (player.duration > 0) player.duration else (episode.durationMillis ?: 0L))}", style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = {
                player.seekTo((player.currentPosition - 15000).coerceAtLeast(0L))
            }) { Text("-15s") }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = {
                player.playWhenReady = !player.playWhenReady
            }) { Text(if (player.isPlaying) "Pause" else "Play") }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = {
                player.seekTo((player.currentPosition + 30000).coerceAtMost(if (player.duration > 0) player.duration else Long.MAX_VALUE))
            }) { Text("+30s") }
        }
    }
}