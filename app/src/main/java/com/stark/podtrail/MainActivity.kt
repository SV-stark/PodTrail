package com.stark.podtrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stark.podtrail.data.Episode
import com.stark.podtrail.data.Podcast
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.HorizontalDivider
import com.stark.podtrail.ui.PodcastViewModel
import com.stark.podtrail.ui.ProfileScreen
import com.stark.podtrail.ui.theme.PodTrailTheme
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.painterResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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

import kotlinx.coroutines.isActive
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.filled.Sort
import coil.compose.AsyncImage

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff


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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodTrackApp(vm: PodcastViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsRepo = remember { com.stark.podtrail.data.SettingsRepository(context) }
    val podcastRepository = remember { 
        com.stark.podtrail.data.PodcastRepository(
            com.stark.podtrail.data.PodcastDatabase.getInstance(context).podcastDao()
        ) 
    }
    val appSettings by settingsRepo.settings.collectAsState(initial = com.stark.podtrail.data.AppSettings())

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var selectedEpisode by remember { mutableStateOf<com.stark.podtrail.data.EpisodeListItem?>(null) }
    
    // Podcast Info Screen State
    var selectedPodcastInfo by remember { mutableStateOf<Podcast?>(null) }
    val podcastListState = androidx.compose.foundation.lazy.LazyListState() // Hoisted state for Home
    val episodeListState = remember { androidx.compose.foundation.lazy.LazyListState() } // Hoisted for Episode List

    // Reset scroll when podcast changes
    LaunchedEffect(selectedPodcast?.id) {
        episodeListState.scrollToItem(0)
    }
    
    // 0 = Home, 1 = Discover, 2 = Profile (Calendar removed from bottom bar, moved to drawer?)
    // Plan: Home, Discover, Calendar, Profile in Drawer. Bottom Bar optional or synced?
    // Let's keep Bottom Bar for main nav, Drawer for utils/extra.
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    com.stark.podtrail.ui.theme.PodTrailTheme(
        darkTheme = when(appSettings.themeMode) {
            com.stark.podtrail.data.ThemeMode.LIGHT -> false
            com.stark.podtrail.data.ThemeMode.DARK -> true
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
                drawerState.isOpen -> scope.launch { drawerState.close() }
                showSettings -> showSettings = false
                selectedEpisode != null -> selectedEpisode = null
                selectedPodcastInfo != null -> selectedPodcastInfo = null
                showSearch -> showSearch = false
                selectedPodcast != null -> selectedPodcast = null
                else -> context.findActivity()?.finish()
            }
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                com.stark.podtrail.ui.SidebarDrawer(
                    onNavigate = { 
                        selectedTab = it
                        // Close other "screens"
                        selectedPodcast = null
                        selectedEpisode = null
                        selectedPodcastInfo = null
                        showSearch = false
                        showSettings = false
                    },
                    onClose = { scope.launch { drawerState.close() } },
                    onSettings = { showSettings = true }
                )
            }
        ) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    // Hide TopBar if on specific full screens like Settings or PodcastInfo or EpisodeDetail
                    // But we want it for Home/Discover logic.
                    // Actually, PodcastInfo has its own internal back button in my design? 
                    // No, I put Scaffold in PodcastInfoScreen. So we should hide this one.
                    val hideMainTopBar = showSettings || selectedPodcastInfo != null || selectedEpisode != null
                    
                    if (!hideMainTopBar) {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    when(selectedTab) {
                                        0 -> "PodTrack"
                                        1 -> "Discover"
                                        2 -> "Calendar"
                                        3 -> "Profile"
                                        else -> "PodTrack"
                                    }, 
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                if (selectedTab == 0 && selectedPodcast == null) {
                                    IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Add, contentDescription = "Add") }
                                     // Refresh button
                                    val isRefreshing by vm.isRefreshing.collectAsState()
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(12.dp).size(24.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = { vm.refreshAllPodcasts() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                                    } 
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    }
                },
                bottomBar = {
                    val hideBottomBar = showSettings || selectedPodcastInfo != null || selectedEpisode != null || selectedPodcast != null || showSearch
                    if (!hideBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                                label = { Text("Discover") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                                label = { Text("Calendar") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    if (showSettings) {
                        com.stark.podtrail.ui.SettingsScreen(settingsRepo, podcastRepository, appSettings, onBack = { showSettings = false })
                    } else if (selectedPodcastInfo != null) {
                         // Find the stats for this podcast
                         val podcasts by vm.podcasts.collectAsState()
                         val pStats = podcasts.find { it.podcast.id == selectedPodcastInfo!!.id }
                         com.stark.podtrail.ui.PodcastInfoScreen(
                             podcast = selectedPodcastInfo!!,
                             stats = pStats,
                             vm = vm,
                             onBack = { selectedPodcastInfo = null }
                         )
                    } else if (selectedEpisode != null) {
                         val fullEpisode by vm.getEpisodeFlow(selectedEpisode!!.id).collectAsState(initial = null)
                         LaunchedEffect(selectedEpisode!!.id) { vm.fetchAndUpdateDescription(selectedEpisode!!.id) }
                         
                         if (fullEpisode != null) {
                            EpisodeDetailScreen(episode = fullEpisode!!, vm = vm, onClose = { selectedEpisode = null })
                         } else {
                             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                         }
                    } else if (showSearch) {
                        SearchScreen(vm, onBack = { showSearch = false }, onPodcastAdded = { showSearch = false })
                    } else if (selectedPodcast != null) {
                        EpisodeListScreen(vm, selectedPodcast!!.id,
                            listState = episodeListState,
                            onBack = { selectedPodcast = null },
                            onDetails = { ep -> selectedEpisode = ep }
                        )
                    } else {
                        // Main Tabs
                        when (selectedTab) {
                            0 -> com.stark.podtrail.ui.HomeScreen(
                                vm, 
                                onOpenPodcast = { podcast -> selectedPodcast = podcast },
                                onOpenEpisode = { episode -> 
                                    selectedEpisode = com.stark.podtrail.data.EpisodeListItem(
                                        id = episode.id,
                                        podcastId = episode.podcastId,
                                        title = episode.title,
                                        pubDate = episode.pubDate,
                                        imageUrl = episode.imageUrl,
                                        episodeNumber = episode.episodeNumber,
                                        durationMillis = episode.durationMillis,
                                        listened = episode.listened,
                                        listenedAt = episode.listenedAt,
                                        playbackPosition = episode.playbackPosition,
                                        lastPlayedTimestamp = episode.lastPlayedTimestamp
                                    )
                                },
                                onOpenPodcastInfo = { p -> selectedPodcastInfo = p }
                            )
                            1 -> DiscoverScreen(vm)
                            2 -> com.stark.podtrail.ui.CalendarScreen(vm, onEpisodeClick = { ep -> selectedEpisode = ep })
                            3 -> ProfileScreen(vm, settingsRepo, appSettings)
                        }
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
fun PodcastListScreen(vm: PodcastViewModel, onOpen: (Podcast) -> Unit, onInfo: (Podcast) -> Unit) {
    val podcasts by vm.podcasts.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(podcasts) { pStats ->
            val p = pStats.podcast
            PodcastCard(p, pStats, onClick = { onOpen(p) }, onInfoClick = { onInfo(p) })
        }
    }
}

@Composable
fun PodcastCard(
    podcast: Podcast, 
    stats: com.stark.podtrail.data.PodcastWithStats, 
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
fun EpisodeListScreen(
    vm: PodcastViewModel, 
    podcastId: Long, 
    listState: androidx.compose.foundation.lazy.LazyListState, 
    onBack: () -> Unit, 
    onDetails: (com.stark.podtrail.data.EpisodeListItem) -> Unit
) {
    val episodes by vm.episodesFor(podcastId).collectAsState(initial = emptyList())
    val sortOption by vm.sortOption.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var hideListened by rememberSaveable { mutableStateOf(false) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredEpisodes = remember(episodes, hideListened) {
        if (hideListened) episodes.filter { !it.listened } else episodes
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Episodes",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {

                        // Hide/Show Listened Toggle
                        IconButton(onClick = { hideListened = !hideListened }) {
                            Icon(
                                imageVector = if (hideListened) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideListened) "Show Listened" else "Hide Listened"
                            )
                        }

                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = { vm.setSortOption(com.stark.podtrail.ui.SortOption.DATE_NEWEST); showSortMenu = false },
                                leadingIcon = { if (sortOption == com.stark.podtrail.ui.SortOption.DATE_NEWEST) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = { vm.setSortOption(com.stark.podtrail.ui.SortOption.DATE_OLDEST); showSortMenu = false },
                                leadingIcon = { if (sortOption == com.stark.podtrail.ui.SortOption.DATE_OLDEST) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Shortest First") },
                                onClick = { vm.setSortOption(com.stark.podtrail.ui.SortOption.DURATION_SHORTEST); showSortMenu = false },
                                leadingIcon = { if (sortOption == com.stark.podtrail.ui.SortOption.DURATION_SHORTEST) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Longest First") },
                                onClick = { vm.setSortOption(com.stark.podtrail.ui.SortOption.DURATION_LONGEST); showSortMenu = false },
                                leadingIcon = { if (sortOption == com.stark.podtrail.ui.SortOption.DURATION_LONGEST) Icon(Icons.Default.Check, null) }
                            )
                        }

                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredEpisodes) { ep ->
                EpisodeCard(ep, onToggle = { vm.setListened(ep, !ep.listened) }, onDetails = { onDetails(ep) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeCard(ep: com.stark.podtrail.data.EpisodeListItem, onToggle: () -> Unit, onDetails: () -> Unit) {
    Card(
        onClick = onDetails,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ep.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = ep.title, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                
                val dateStr = if (ep.pubDate > 0) java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(ep.pubDate)) else ""
                val durStr = if (ep.durationMillis != null) "Duration: ${formatMillis(ep.durationMillis)}" else ""
                
                Text(
                    text = if (dateStr.isNotEmpty()) "$dateStr\n$durStr" else durStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (ep.playbackPosition > 0 && !ep.listened && ep.durationMillis != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ep.playbackPosition.toFloat() / ep.durationMillis.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Action Button (Checkmark)
            Box(
                modifier = Modifier
                    .size(48.dp) // Accessible touch target
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp) // Visual size (optimized as requested)
                        .background(
                            color = if (ep.listened) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (ep.listened) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (ep.listened) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Listened",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
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
    Column(Modifier.fillMaxSize()) {
        // Top Section: Image and Backdrop (~35% of screen)
        Box(
             modifier = Modifier
                 .fillMaxWidth()
                 .weight(0.35f)
        ) {
            // Blurred/Dimmed Backdrop
            AsyncImage(
                 model = episode.imageUrl,
                 contentDescription = null,
                 modifier = Modifier.fillMaxSize(),
                 contentScale = ContentScale.Crop,
                 alpha = 0.4f
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)))
            
            // Back Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
            ) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) 
            }

            // Central Image
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.85f) // maximize height within the 35% box
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(8.dp),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
        }

        // Bottom Section: Details and Description (~65% of screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Fixed Title and Metadata
            Text(episode.title, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (episode.episodeNumber != null) {
                    Text("Ep ${episode.episodeNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("•", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                }
                if (episode.pubDate > 0) {
                     Text(java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(episode.pubDate)), style = MaterialTheme.typography.labelMedium)
                     Spacer(Modifier.width(8.dp))
                     Text("•", style = MaterialTheme.typography.labelMedium)
                     Spacer(Modifier.width(8.dp))
                }
                if (episode.durationMillis != null) {
                    Text(formatMillis(episode.durationMillis), style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

             // Scrollable Description Area
             Column(
                 modifier = Modifier
                     .weight(1f)
                     .verticalScroll(rememberScrollState())
             ) {
                if (!episode.description.isNullOrBlank()) {
                    val decodedDescription = try {
                        android.text.Html.fromHtml(episode.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
                    } catch (e: Exception) { episode.description }
                    
                    Text(
                        text = decodedDescription, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                } else {
                    Text("No description available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
             }

             Spacer(Modifier.height(16.dp))
             
             Button(
                onClick = { vm.setListened(episode, !episode.listened) },
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text(if (episode.listened) "Remove Listened" else "Mark Listened")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: PodcastViewModel) {
    val discoverPodcasts by vm.discoverPodcasts.collectAsState()
    val title by vm.discoverTitle.collectAsState()
    
    var showPreviewPodcast by remember { mutableStateOf<com.stark.podtrail.network.SearchResult?>(null) }
    
    // Genres
    val genres = remember {
        listOf(
            "Top" to null,
            "Comedy" to 1303L,
            "News" to 1489L,
            "Tech" to 1310L,
            "True Crime" to 1488L,
            "History" to 1487L,
            "Science" to 1315L,
            "Business" to 1321L
        )
    }
    
    var selectedGenreIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (discoverPodcasts.isEmpty()) vm.refreshDiscover() // loads default
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

    Column(Modifier.fillMaxSize()) {
        Text("Discover", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        
        // Genre Chips
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres.size) { index ->
                val (name, id) = genres[index]
                FilterChip(
                    selected = selectedGenreIndex == index,
                    onClick = {
                        selectedGenreIndex = index
                        vm.selectDiscoverGenre(id, name)
                    },
                    label = { Text(name) },
                    leadingIcon = if (selectedGenreIndex == index) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        
        Spacer(Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
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
