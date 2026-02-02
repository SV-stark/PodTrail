package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.Podcast
import com.stark.podtrail.data.SortOption
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeFilterChips(
    selectedFilter: EpisodeFilter,
    onFilterChanged: (EpisodeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall()),
        contentPadding = PaddingValues(horizontal = ResponsiveDimensions.spacingSmall())
    ) {
        items(EpisodeFilter.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = { 
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilter(
    selectedRange: DateRange,
    onRangeChanged: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall()),
        contentPadding = PaddingValues(horizontal = ResponsiveDimensions.spacingSmall())
    ) {
        items(DateRange.values()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeChanged(range) },
                label = { Text(range.displayName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationFilter(
    selectedDuration: DurationFilter,
    onDurationChanged: (DurationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall()),
        contentPadding = PaddingValues(horizontal = ResponsiveDimensions.spacingSmall())
    ) {
        items(DurationFilter.values()) { duration ->
            FilterChip(
                selected = selectedDuration == duration,
                onClick = { onDurationChanged(duration) },
                label = { Text(duration.displayName) }
            )
        }
    }
}

@Composable
fun BatchActionBar(
    selectedCount: Int,
    onMarkListened: () -> Unit,
    onMarkUnlistened: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingSmall()),
            horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onMarkListened) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Mark as listened",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            IconButton(onClick = onMarkUnlistened) {
                Icon(
                    Icons.Default.CheckCircleOutline,
                    contentDescription = "Mark as unlistened",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    AppIcons.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            IconButton(onClick = onClearSelection) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeSortMenu(
    selectedSort: SortOption,
    onSortChanged: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSort.displayName,
            onValueChange = { },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSortChanged(option)
                        expanded = false
                    },
                    leadingIcon = if (selectedSort == option) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

enum class EpisodeFilter(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("All", Icons.Default.Podcast),
    UNLISTENED("Unlistened", Icons.Default.RadioButtonUnchecked),
    LISTENED("Listened", Icons.Default.CheckCircle),
    IN_PROGRESS("In Progress", Icons.Default.PlayArrow),
    DOWNLOADED("Downloaded", Icons.Default.Download)
}

enum class DateRange(val displayName: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    PAST_3_MONTHS("Past 3 Months"),
    PAST_6_MONTHS("Past 6 Months"),
    PAST_YEAR("Past Year")
}

enum class DurationFilter(val displayName: String) {
    ALL("All"),
    UNDER_15("Under 15 min"),
    UNDER_30("Under 30 min"),
    UNDER_60("Under 1 hour"),
    OVER_30("Over 30 min"),
    OVER_60("Over 1 hour")
}

// Extension properties for better display names
val SortOption.displayName: String
    get() = when (this) {
        SortOption.DATE_NEWEST -> "Newest First"
        SortOption.DATE_OLDEST -> "Oldest First"
        SortOption.DURATION_SHORTEST -> "Shortest First"
        SortOption.DURATION_LONGEST -> "Longest First"
    }