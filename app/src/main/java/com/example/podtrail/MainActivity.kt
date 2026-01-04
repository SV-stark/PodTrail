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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape



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
    val context = LocalContext.current
    val settingsRepo = remember { com.example.podtrail.data.SettingsRepository(context) }
    val appSettings by settingsRepo.settings.collectAsState(initial = com.example.podtrail.data.AppSettings())

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    
    // 0 = Home, 1 = Discover, 2 = Profile
    var selectedTab by remember { mutableIntStateOf(0) }

    com.example.podtrail.ui.theme.PodTrailTheme(
        darkTheme = when(appSettings.themeMode) {
            com.example.podtrail.data.ThemeMode.LIGHT -> false
            com.example.podtrail.data.ThemeMode.DARK -> true
            else -> isSystemInDarkTheme()
        },
        dynamicColor = false, // Enforce our custom theme for the request
        amoled = appSettings.useAmoled,
        customColor = appSettings.customColor
    ) {
        val topAppBarColors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )

        Scaffold(
            topBar = {
                if (selectedPodcast == null && selectedEpisode == null && !showSearch && !showSettings) {
                    TopAppBar(
                        title = { Text("PodTrack", style = MaterialTheme.typography.headlineMedium) },
                        actions = {
                            IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Add, contentDescription = "Add") }
                             // Stats hidden for now as per mockup, or moved to profile?
                            IconButton(onClick = { }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } // Hamburger placeholder
                            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                        },
                        colors = topAppBarColors
                    )
                }
            },
            bottomBar = {
                if (selectedPodcast == null && selectedEpisode == null && !showSearch && !showSettings) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("HOME") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                            label = { Text("DISCOVER") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("PROFILE") },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 } // Profile placeholder
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (showSettings) {
                    com.example.podtrail.ui.SettingsScreen(settingsRepo, appSettings, onBack = { showSettings = false })
                } else if (selectedEpisode != null) {
                    EpisodeDetailScreen(episode = selectedEpisode!!, vm = vm, onClose = { selectedEpisode = null })
                } else if (showSearch) {
                    SearchScreen(vm, onBack = { showSearch = false }, onPodcastAdded = { showSearch = false })
                } else if (selectedPodcast != null) {
                    EpisodeListScreen(vm, selectedPodcast!!.id,
                        onBack = { selectedPodcast = null },
                        onDetails = { ep -> selectedEpisode = ep }
                    )
                } else {
                    // Main Tabs
                    when (selectedTab) {
                        0 -> PodcastListScreen(vm) { podcast -> selectedPodcast = podcast }
                        1 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Discover Coming Soon") }
                        2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile Coming Soon") }
                    }
                }
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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(podcasts) { pStats ->
            val p = pStats.podcast
            PodcastCard(p, pStats, onClick = { onOpen(p) })
        }
    }
}

@Composable
fun PodcastCard(
    podcast: Podcast, 
    stats: com.example.podtrail.data.PodcastWithStats, 
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                
                if (stats.totalEpisodes > 0) {
                    LinearProgressIndicator(
                        progress = { stats.listenedEpisodes.toFloat() / stats.totalEpisodes.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${stats.listenedEpisodes}/${stats.totalEpisodes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val percentage = (stats.listenedEpisodes.toFloat() / stats.totalEpisodes.toFloat() * 100).toInt()
                        Text(
                            "$percentage% completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Icon(
                Icons.Default.Info, 
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(vm: PodcastViewModel, podcastId: Long, onBack: () -> Unit, onDetails: (Episode) -> Unit) {
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
                EpisodeRow(ep, onToggle = { vm.setListened(ep, !ep.listened) }, onDetails = { onDetails(ep) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EpisodeRow(ep: Episode, onToggle: () -> Unit, onDetails: () -> Unit) {
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
        
        Column(Modifier.weight(1f).clickable { onDetails() }) {
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
fun EpisodeDetailScreen(episode: Episode, vm: PodcastViewModel, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Close") }
            Text("Episode Details", style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
            Spacer(Modifier.height(24.dp))
            Text(episode.title, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            if (episode.episodeNumber != null) {
                Text("Episode ${episode.episodeNumber}", style = MaterialTheme.typography.titleMedium)
            }
            if (episode.durationMillis != null) {
                Text("Duration: ${formatMillis(episode.durationMillis)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (episode.pubDate > 0) {
                 Text("Published: ${java.text.SimpleDateFormat.getDateInstance().format(java.util.Date(episode.pubDate))}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            // Description might be HTML, but focusing on plain text or simple display. 
            // For now, simple text display. Ideally use AndroidView for WebView or Html.fromHtml.
            if (!episode.description.isNullOrBlank()) {
                val decodedDescription = try {
                    android.text.Html.fromHtml(episode.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
                } catch (e: Exception) { episode.description }
                
                Text(
                    text = decodedDescription, 
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { vm.setListened(episode, !episode.listened) },
            modifier = Modifier.fillMaxWidth()
        ) {
             Text(if (episode.listened) "Mark Unlistened" else "Mark Listened")
        }
        Spacer(Modifier.height(16.dp))
    }
}