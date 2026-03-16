package com.stark.podtrail.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stark.podtrail.data.Episode
import com.stark.podtrail.network.SearchResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SearchScreen(vm: PodcastViewModel, onBack: () -> Unit, onPodcastAdded: () -> Unit) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()
    var showUrlDialog by remember { mutableStateOf(false) }
    var directUrl by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUrlDialog = false 
                urlError = null
            },
            title = { Text("Add by URL") },
            text = {
                Column {
                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { 
                            directUrl = it 
                            urlError = null
                        },
                        label = { Text("Feed URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (directUrl.isBlank()) {
                        urlError = "URL cannot be empty"
                    } else if (!directUrl.startsWith("http")) {
                        urlError = "URL must start with http:// or https://"
                    } else {
                        vm.addPodcast(directUrl, null) { }
                        onPodcastAdded()
                        showUrlDialog = false
                        urlError = null
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUrlDialog = false 
                    urlError = null
                }) { Text("Cancel") }
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
        
        TextField(
            value = query,
            onValueChange = { vm.search(it) },
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
                            vm.subscribeToSearchResult(result)
                            onPodcastAdded()
                        }) { Text("Add") }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: PodcastViewModel) {
    val discoverPodcasts by vm.discoverPodcasts.collectAsState()
    val title by vm.discoverTitle.collectAsState()
    var showPreviewPodcast by remember { mutableStateOf<SearchResult?>(null) }
    
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
        if (discoverPodcasts.isEmpty()) vm.refreshDiscover()
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

@Composable
fun EpisodeDetailScreen(episode: Episode, vm: PodcastViewModel, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.35f)) {
            AsyncImage(
                 model = episode.imageUrl,
                 contentDescription = null,
                 modifier = Modifier.fillMaxSize(),
                 contentScale = ContentScale.Crop,
                 alpha = 0.4f
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)))
            
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }

            AsyncImage(
                model = episode.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(8.dp),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Podcasts),
                placeholder = rememberVectorPainter(Icons.Default.Podcasts)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
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
                     Text(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(episode.pubDate)), style = MaterialTheme.typography.labelMedium)
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

             Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
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
             
             Button(onClick = { vm.setListened(episode, !episode.listened) }, modifier = Modifier.fillMaxWidth()) {
                 Text(if (episode.listened) "Remove Listened" else "Mark Listened")
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
