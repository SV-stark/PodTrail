package com.stark.podtrail.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.stark.podtrail.data.EpisodeListItem
import com.stark.podtrail.data.Podcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEpisodeCard(
    episode: EpisodeListItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isSwipeInProgress: Boolean = false,
    showNewBadge: Boolean = false,
    onToggle: () -> Unit = {},
    onSelect: () -> Unit = {},
    onDetails: () -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val cardBackgroundColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primaryContainer
            isSwipeInProgress -> MaterialTheme.colorScheme.tertiaryContainer
            episode.listened -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200), label = "cardBackground"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primary
            episode.listened -> MaterialTheme.colorScheme.outlineVariant
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(200), label = "borderColor"
    )

    Card(
        onClick = if (isSelectionMode) onSelect else onDetails,
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(ResponsiveDimensions.cornerRadiusMedium()),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelectionMode) 0.dp else 1.dp,
            pressedElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelectionMode && isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(ResponsiveDimensions.cornerRadiusMedium())
            )
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingMedium()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox or artwork
            if (isSelectionMode) {
                SelectionCheckbox(
                    isSelected = isSelected,
                    onSelect = onSelect,
                    modifier = Modifier.size(ResponsiveDimensions.iconSizeLarge())
                )
            } else {
                EpisodeArtwork(
                    imageUrl = episode.imageUrl,
                    modifier = Modifier.size(ResponsiveDimensions.iconSizeLarge() + ResponsiveDimensions.spacingSmall()),
                    isListened = episode.listened
                )
            }
            
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingMedium()))
            
            // Episode info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall())
                ) {
                    EpisodeTitle(
                        title = episode.title,
                        isListened = episode.listened,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // New badge
                    if (showNewBadge && !episode.listened) {
                        NewEpisodeBadge()
                    }
                }
                
                Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingTiny()))
                
                EpisodeMetadata(
                    pubDate = episode.pubDate,
                    durationMillis = episode.durationMillis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Playback progress
                if (episode.playbackPosition > 0 && !episode.listened && episode.durationMillis != null) {
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                    PlaybackProgressIndicator(
                        progress = episode.playbackPosition.toFloat() / episode.durationMillis.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingMedium()))
            
            // Action button
            EpisodeActionButton(
                isListened = episode.listened,
                isSelectionMode = isSelectionMode,
                onToggle = onToggle,
                modifier = Modifier.size(ResponsiveDimensions.iconSizeMedium() + ResponsiveDimensions.spacingTiny())
            )
        }
    }
}

@Composable
fun SelectionCheckbox(
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onSelect)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(ResponsiveDimensions.iconSizeSmall())
            )
        }
    }
}

@Composable
fun EpisodeArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    isListened: Boolean = false
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall())),
            contentScale = ContentScale.Crop,
            error = rememberAsyncImagePainter(Icons.Default.Podcasts),
            placeholder = rememberAsyncImagePainter(Icons.Default.Podcasts)
        )
        
        // Overlay for listened episodes
        if (isListened) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall())),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Listened",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ResponsiveDimensions.iconSizeMedium())
                )
            }
        }
    }
}

@Composable
fun EpisodeTitle(
    title: String,
    isListened: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isListened) 
        MaterialTheme.colorScheme.onSurfaceVariant 
    else 
        MaterialTheme.colorScheme.onSurface
    
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isListened) FontWeight.Normal else FontWeight.Bold,
        color = textColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun EpisodeMetadata(
    pubDate: Long,
    durationMillis: Long?,
    modifier: Modifier = Modifier
) {
    val dateStr = if (pubDate > 0) 
        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(pubDate)) 
    else ""
    
    val durStr = if (durationMillis != null) 
        formatMillis(durationMillis) 
    else ""
    
    val metadata = listOfNotNull(
        dateStr.takeIf { it.isNotEmpty() },
        durStr.takeIf { it.isNotEmpty() }
    ).joinToString(" • ")
    
    Text(
        text = metadata,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun PlaybackProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun EpisodeActionButton(
    isListened: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelectionMode) {
        // Show more options button in selection mode
        IconButton(onClick = { }, modifier = modifier) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // Show listened toggle
        Box(
            modifier = modifier
                .clip(CircleShape)
                .clickable(onClick = onToggle)
                .background(
                    color = if (isListened) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
                .border(
                    width = 1.5.dp,
                    color = if (isListened) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListened) Icons.Default.Check else Icons.Default.Check,
                contentDescription = if (isListened) "Mark as unlistened" else "Mark as listened",
                tint = if (isListened) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ResponsiveDimensions.iconSizeSmall())
            )
        }
    }
}

@Composable
fun NewEpisodeBadge(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall())
    ) {
        Text(
            text = "New",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(
                horizontal = ResponsiveDimensions.spacingTiny(),
                vertical = 1.dp
            )
        )
    }
}

@Composable
fun EpisodeEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Default.Podcast,
            title = "No episodes found",
            message = "Try adjusting your filters or check back later for new episodes.",
            action = null
        )
    }
}

private fun formatMillis(ms: Long): String {
    val s = ms / 1000
    val hh = s / 3600
    val mm = (s % 3600) / 60
    val ss = s % 60
    return if (hh > 0) String.format("%d:%02d:%02d", hh, mm, ss) else String.format("%d:%02d", mm, ss)
}