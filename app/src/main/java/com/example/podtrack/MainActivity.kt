package com.example.podtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.podtrack.data.Episode
import com.example.podtrack.data.Podcast
import com.example.podtrack.ui.PodcastViewModel

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PodTrackApp()
        }
    }
}

@Composable
fun PodTrackApp(vm: PodcastViewModel = viewModel()) {
    var showAdd by remember { mutableStateOf(false) }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var playingEpisode by remember { mutableStateOf<Episode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PodTrack") }, actions = {
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Add") }
            })
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (playingEpisode != null) {
                PlayerScreen(episode = playingEpisode!!, vm = vm, onClose = { playingEpisode = null })
            } else if (selectedPodcast == null) {
                PodcastListScreen(vm) { podcast -> selectedPodcast = podcast }
            } else {
                EpisodeListScreen(vm, selectedPodcast!!.id,
                    onBack = { selectedPodcast = null },
                    onPlay = { ep -> playingEpisode = ep }
                )
            }

            if (showAdd) {
                AddPodcastDialog(onAdd = { feedUrl ->
                    vm.addPodcast(feedUrl) { res ->
                        // TODO: show feedback
                    }
                    showAdd = false
                }, onDismiss = { showAdd = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PodcastListScreen(vm: PodcastViewModel, onOpen: (Podcast) -> Unit) {
    val podcasts by vm.podcasts.collectAsState()
    LazyColumn {
        items(podcasts) { p ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(p) },
                text = { Text(p.title) },
                secondaryText = { Text(p.feedUrl) }
            )
            Divider()
        }
    }
}

@Composable
fun AddPodcastDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add podcast feed") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Feed URL") })
                Spacer(Modifier.height(4.dp))
                Text("Example: https://feeds.simplecast.com/abcd", style = MaterialTheme.typography.caption)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EpisodeListScreen(vm: PodcastViewModel, podcastId: Long, onBack: () -> Unit, onPlay: (Episode) -> Unit) {
    val episodes by vm.episodesFor(podcastId).collectAsState(initial = emptyList())
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        }
        LazyColumn {
            items(episodes) { ep ->
                EpisodeRow(ep, onToggle = { vm.setListened(ep, !ep.listened) }, onPlay = { onPlay(ep) })
                Divider()
            }
        }
    }
}

@Composable
fun EpisodeRow(ep: Episode, onToggle: () -> Unit, onPlay: () -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable { onPlay() }) {
            Text(ep.title)
            if (ep.pubDate > 0) Text(java.text.SimpleDateFormat.getDateInstance().format(java.util.Date(ep.pubDate)), style = MaterialTheme.typography.caption)
            if (ep.episodeNumber != null) Text("Episode ${ep.episodeNumber}", style = MaterialTheme.typography.caption)
            if (ep.durationMillis != null) Text("Duration ${formatMillis(ep.durationMillis)}", style = MaterialTheme.typography.caption)
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
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    player.playWhenReady = false
                    onClose()
                }) { Icon(Icons.Default.ArrowBack, contentDescription = "Close") }
                Text(episode.title, style = MaterialTheme.typography.h6)
            }
            Spacer(Modifier.height(8.dp))
            Text(if (episode.episodeNumber != null) "Episode ${episode.episodeNumber}" else "", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(4.dp))
            Text("Position: ${formatMillis(player.currentPosition)} / ${formatMillis(if (player.duration > 0) player.duration else (episode.durationMillis ?: 0L))}", style = MaterialTheme.typography.caption)
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