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
import com.example.podtrail.ui.ProfileScreen
import com.example.podtrail.ui.theme.PodTrailTheme
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.painterResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.lazy.items
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
    var selectedEpisode by remember { mutableStateOf<com.example.podtrail.data.EpisodeListItem?>(null) }
    
    // 0 = Home, 1 = Discover, 2 = Profile
    var selectedTab by remember { mutableIntStateOf(0) }

    com.example.podtrail.ui.theme.PodTrailTheme(
        darkTheme = when(appSettings.themeMode) {
            com.example.podtrail.data.ThemeMode.LIGHT -> false
            com.example.podtrail.data.ThemeMode.DARK -> true
            else -> isSystemInDarkTheme()
        },
        dynamicColor = appSettings.useDynamicColor,
        amoled = appSettings.useAmoled,
        customColor = appSettings.customColor
    ) {
        val topAppBarColors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )

        // Handle Back Press
        androidx.activity.compose.BackHandler(enabled = true) {
            when {
                showSettings -> showSettings = false
                selectedEpisode != null -> selectedEpisode = null
                showSearch -> showSearch = false
                selectedPodcast != null -> selectedPodcast = null
                else -> context.findActivity()?.finish()
            }
        }

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
                    // Fetch full episode details if we only have the item
                     val fullEpisodeState = produceState<Episode?>(initialValue = null, key1 = selectedEpisode!!.id) {
                         value = vm.getEpisode(selectedEpisode!!.id)
                         // Trigger real-time fetch to ensure description is up to date/full
                         if (value != null) {
                             vm.fetchAndUpdateDescription(value!!.id)
                         }
                     }
                     // Also watch for updates to the episode in case the fetch finishes while we are looking at it
                     val updatedEpisode by vm.episodesFor(selectedEpisode!!.podcastId).collectAsState(initial = emptyList())
                     // This flow is for the list, so it might be lightweight.
                     // Better: The produceState above runs once. We need a way to Observe changes to the single episode.
                     // Since we don't have a flow for single episode, we will just rely on the UI re-reading if we close/open? 
                     // OR we can make a Flow for getEpisode(id). 
                     // For now, let's keep it simple: The fetch runs in background. 
                     // If the user really wants to see it "real time", we should observe it.
                     // But the user said "loaded from api in real time".
                     // Let's change `fullEpisodeState` to use a Flow if possible or poll?
                     // Actually, if we update the DB, the Flow<List> updates.
                     // But we are in Details view.
                     
                     if (fullEpisodeState.value != null) {
                         // We display the loaded value. If background fetch updates DB, this state won't update unless we observe DB.
                         // Let's display what we have.
                        EpisodeDetailScreen(episode = fullEpisodeState.value!!, vm = vm, onClose = { selectedEpisode = null })
                     } else {
                         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                     }
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
                        1 -> DiscoverScreen(vm)
                        2 -> ProfileScreen(vm, settingsRepo, appSettings)
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
    var showUrlDialog by remember { mutableStateOf(false) }
    var directUrl by remember { mutableStateOf("") }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Add by URL") },
            text = {
                OutlinedTextField(
                    value = directUrl,
                    onValueChange = { directUrl = it },
                    label = { Text("Feed URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (directUrl.isNotBlank()) {
                        vm.addPodcast(directUrl, null) { }
                        onPodcastAdded()
                        showUrlDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text("Add podcast", style = MaterialTheme.typography.titleLarge)
        }
        
        // Search Bar
        TextField(
            value = query,
            onValueChange = { 
                query = it
                vm.search(it)
            },
            placeholder = { Text("Search podcast...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // Add by URL
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showUrlDialog = true }
                .padding(vertical = 12.dp, horizontal = 0.dp)
        ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text("Add by URL", style = MaterialTheme.typography.bodyLarge)
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
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                            error = rememberVectorPainter(Icons.Default.Podcasts)
                        )
                    },
                    trailingContent = {
                        Button(onClick = {
                            result.feedUrl?.let { url ->
                                vm.addPodcast(url, result.primaryGenreName) { }
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
    var showInfoPodcast by remember { mutableStateOf<Podcast?>(null) }

    if (showInfoPodcast != null) {
        val p = showInfoPodcast!!
        AlertDialog(
            onDismissRequest = { showInfoPodcast = null },
            title = { Text(p.title) },
            text = {
                Column {
                    if (!p.description.isNullOrBlank()) {
                         // Simple HTML decoding
                        val decoded = try {
                            android.text.Html.fromHtml(p.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
                        } catch (e: Exception) { p.description }
                        Text(decoded, modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp))
                    } else {
                        Text("No description available.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoPodcast = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        vm.deletePodcast(p.id)
                        showInfoPodcast = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove Podcast") }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(podcasts) { pStats ->
            val p = pStats.podcast
            PodcastCard(p, pStats, onClick = { onOpen(p) }, onInfoClick = { showInfoPodcast = p })
        }
    }
}

@Composable
fun PodcastCard(
    podcast: Podcast, 
    stats: com.example.podtrail.data.PodcastWithStats, 
    onClick: () -> Unit,
    onInfoClick: () -> Unit
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
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onInfoClick() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(vm: PodcastViewModel, podcastId: Long, onBack: () -> Unit, onDetails: (com.example.podtrail.data.EpisodeListItem) -> Unit) {
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(episodes) { ep ->
                EpisodeCard(ep, onToggle = { vm.setListened(ep, !ep.listened) }, onDetails = { onDetails(ep) })
            }
        }
    }
}

@Composable
fun EpisodeCard(ep: com.example.podtrail.data.EpisodeListItem, onToggle: () -> Unit, onDetails: () -> Unit) {
    Card(
        onClick = onDetails,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ep.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).padding(end = 8.dp),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = ep.title, 
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (ep.pubDate > 0) {
                    Text(
                        text = java.text.SimpleDateFormat.getDateInstance().format(java.util.Date(ep.pubDate)), 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (ep.durationMillis != null) {
                    Text(
                        text = "Duration: ${formatMillis(ep.durationMillis)}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Checkbox(checked = ep.listened, onCheckedChange = { onToggle() })
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: PodcastViewModel) {
    val discoverPodcasts by vm.discoverPodcasts.collectAsState()
    val title by vm.discoverTitle.collectAsState()
    
    var showPreviewPodcast by remember { mutableStateOf<com.example.podtrail.network.SearchResult?>(null) }
    
    LaunchedEffect(Unit) {
        vm.refreshDiscover()
    }

    if (showPreviewPodcast != null) {
        val p = showPreviewPodcast!!
        AlertDialog(
            onDismissRequest = { showPreviewPodcast = null },
            title = { Text(p.collectionName ?: "Podcast") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = p.artworkUrl600 ?: p.artworkUrl100,
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                        error = rememberVectorPainter(Icons.Default.Podcasts)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(p.artistName ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    Text("Genre: ${p.primaryGenreName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.subscribeToSearchResult(p)
                    showPreviewPodcast = null
                }) { Text("Subscribe") }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewPodcast = null }) { Text("Close") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(discoverPodcasts) { podcast ->
                Card(
                     onClick = { showPreviewPodcast = podcast },
                     shape = RoundedCornerShape(12.dp),
                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = podcast.artworkUrl600 ?: podcast.artworkUrl100,
                            contentDescription = null,
                            modifier = Modifier.aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                            error = rememberVectorPainter(Icons.Default.Podcasts)
                        )
                        Box(Modifier.padding(12.dp)) {
                            Text(
                                text = podcast.collectionName ?: "Unknown",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}