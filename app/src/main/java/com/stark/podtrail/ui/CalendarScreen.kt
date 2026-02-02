package com.stark.podtrail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.*
import com.stark.podtrail.data.EpisodeListItem
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight

@Composable
fun CalendarScreen(vm: PodcastViewModel, onEpisodeClick: (EpisodeListItem) -> Unit) {
    // Performance optimized: Load only episodes for the current month instead of all episodes
    // This significantly reduces memory usage and database load
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val episodes by vm.getEpisodesForMonth(
        currentMonth.get(Calendar.YEAR), 
        currentMonth.get(Calendar.MONTH)
    ).collectAsState(initial = emptyList())
    
    // Loading state for month navigation
    val isLoading by remember { derivedStateOf { episodes.isEmpty() } }
    
    val episodesByDay = remember(episodes, currentMonth) {
        episodes.groupBy { ep ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = ep.pubDate
            // Key: Day of month (1-31)
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    Column(Modifier.fillMaxSize()) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(ResponsiveDimensions.spacingSmall()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                val new =  currentMonth.clone() as Calendar
                new.add(Calendar.MONTH, -1)
                currentMonth = new
                selectedDay = null
            }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Prev") }
            
            Text(
                text = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                style = MaterialTheme.typography.titleLarge
            )
            
            IconButton(onClick = { 
                val new =  currentMonth.clone() as Calendar
                new.add(Calendar.MONTH, 1)
                currentMonth = new
                selectedDay = null
            }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
        }

        // Days Grid
        // 7 columns
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = currentMonth.clone() as Calendar
        firstDayOfWeek.set(Calendar.DAY_OF_MONTH, 1)
        val offset = firstDayOfWeek.get(Calendar.DAY_OF_WEEK) - 1 // 0-based offset
        
        // Headers (S M T W T F S)
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { 
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))

        LoadingOverlay(isLoading = isLoading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().padding(horizontal = ResponsiveDimensions.spacingSmall())
            ) {
                // Empty slots
                items(offset) { Spacer(Modifier.height(ResponsiveDimensions.spacingLarge())) }
                
                // Days
                items(daysInMonth) { i ->
                    val day = i + 1
                    val hasEpisodes = episodesByDay.containsKey(day)
                    val isSelected = selectedDay == day
                    val isToday = isToday(currentMonth, day)
                    
                    Box(
                        modifier = Modifier
                            .height(ResponsiveDimensions.spacingLarge() + ResponsiveDimensions.spacingTiny())
                            .padding(ResponsiveDimensions.spacingTiny())
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f)
                                else Color.Transparent, 
                                CircleShape
                            )
                            .clickable { selectedDay = day },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            if (hasEpisodes) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    Modifier.size(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(Modifier.padding(vertical = ResponsiveDimensions.spacingSmall()))
        
        // List for selected day
        if (selectedDay != null) {
            val daysEpisodes = episodesByDay[selectedDay] ?: emptyList()
            if (daysEpisodes.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No releases on this day.", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(ResponsiveDimensions.spacingSmall())
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(ResponsiveDimensions.spacingSmall())) {
                    item { 
                        Text(
                            "Releases on ${java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(currentMonth.time)} $selectedDay", 
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = ResponsiveDimensions.spacingTiny())
                        ) 
                    }
                    items(daysEpisodes) { ep ->
                        EpisodesListItemSmall(ep, onClick = { onEpisodeClick(ep) })
                    }
                }
            }
        } else {
             Box(
                Modifier.fillMaxSize(), 
                contentAlignment = Alignment.Center
             ) {
                Text(
                    "Select a day to view releases", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ResponsiveDimensions.spacingSmall())
                )
            }
        }
    }
}

private fun isToday(cal: Calendar, day: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           today.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
           today.get(Calendar.DAY_OF_MONTH) == day
}

@Composable
fun EpisodesListItemSmall(ep: EpisodeListItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(ep.title, maxLines = 1) },
        supportingContent = { Text(formatTime(ep.pubDate)) },
        leadingContent = {
             coil.compose.AsyncImage(
                model = ep.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(ResponsiveDimensions.iconSizeLarge())
                    .clip(RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall()))
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private fun formatTime(millis: Long): String {
    return java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
}

