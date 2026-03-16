package com.stark.podtrail.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.EpisodeListItem
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEpisodeListScreen(
    vm: com.stark.podtrail.ui.PodcastViewModel,
    podcastId: Long,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onBack: () -> Unit,
    onDetails: (EpisodeListItem) -> Unit
) {
    val pagingItems = vm.episodesForPaging(podcastId).collectAsLazyPagingItems()
    val sortOption by vm.sortOption.collectAsState()
    
    // Selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedEpisodes by remember { mutableStateOf(setOf<Long>()) }
    
    // Show filters panel
    var showFilters by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Episodes",
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {
                    // Selection mode toggle
                    IconButton(onClick = { 
                        isSelectionMode = !isSelectionMode
                        selectedEpisodes = emptySet()
                    }) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = if (isSelectionMode) "Exit selection" else "Select episodes"
                        )
                    }
                    
                    // Filters toggle
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters"
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter panels (animated)
                AnimatedVisibility(
                    visible = showFilters,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    ) {
                        // Sort dropdown
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort by:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            EpisodeSortMenu(
                                selectedSort = sortOption,
                                onSortChanged = { vm.setSortOption(it) },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Episode list
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            bottom = if (isSelectionMode) 80.dp else 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.id },
                            contentType = pagingItems.itemContentType { "episode" }
                        ) { index ->
                            val episode = pagingItems[index]
                            if (episode != null) {
                                EnhancedEpisodeCard(
                                    episode = episode,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = episode.id in selectedEpisodes,
                                    showNewBadge = false,
                                    onToggle = { vm.setListened(episode, !episode.listened) },
                                    onSelect = { 
                                        selectedEpisodes = if (episode.id in selectedEpisodes) {
                                            selectedEpisodes - episode.id
                                        } else {
                                            selectedEpisodes + episode.id
                                        }
                                    },
                                    onDetails = { onDetails(episode) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Batch actions bar (animated)
            AnimatedVisibility(
                visible = isSelectionMode && selectedEpisodes.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BatchActionBar(
                    selectedCount = selectedEpisodes.size,
                    onMarkListened = { 
                        // Batch mark listened logic
                        selectedEpisodes = emptySet()
                    },
                    onMarkUnlistened = { 
                        // Batch mark unlistened logic
                        selectedEpisodes = emptySet()
                    },
                    onDelete = { 
                        selectedEpisodes = emptySet()
                    },
                    onAddToPlaylist = { 
                        selectedEpisodes = emptySet()
                    },
                    onClearSelection = { selectedEpisodes = emptySet() },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
