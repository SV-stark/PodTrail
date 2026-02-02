package com.stark.podtrail.storage

import com.stark.podtrail.data.PodcastDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class StorageStats(
    val totalEpisodes: Int,
    val totalSizeMB: Double,
    val episodesWithoutDescription: Int,
    val oldUnlistenedEpisodes: Int,
    val podcastsLastUpdated: Map<String, Long>
)

enum class CleanupOption {
    OLD_UNLISTENED_EPISODES,
    TRUNCATE_DESCRIPTIONS,
    REMOVE_INACTIVE_PODCASTS,
    COMPACT_DATABASE
}

data class CleanupResult(
    val option: CleanupOption,
    val itemsAffected: Int,
    val spaceSavedMB: Double
)

class StorageManager(private val dao: PodcastDao) {
    
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val allEpisodes = dao.getAllEpisodesSync()
        val totalEpisodes = allEpisodes.size
        
        // Estimate size (rough calculation)
        val avgEpisodeSize = 1024.0 // 1KB average per episode record
        val totalSizeMB = (totalEpisodes * avgEpisodeSize) / 1024 / 1024
        
        val episodesWithoutDescription = allEpisodes.count { 
            it.description.isNullOrBlank() || it.description.length < 50 
        }
        
        // Episodes older than 6 months and unlistened
        val sixMonthsAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -6)
        }.timeInMillis
        
        val oldUnlistenedEpisodes = allEpisodes.count { 
            !it.listened && it.pubDate < sixMonthsAgo 
        }
        
        // Get podcast last updated info
        val podcastsLastUpdated = dao.getAllPodcastsSync().associate { 
            podcast -> podcast.title to (podcast.lastUpdated ?: 0L)
        }
        
        StorageStats(
            totalEpisodes = totalEpisodes,
            totalSizeMB = totalSizeMB,
            episodesWithoutDescription = episodesWithoutDescription,
            oldUnlistenedEpisodes = oldUnlistenedEpisodes,
            podcastsLastUpdated = podcastsLastUpdated
        )
    }
    
    suspend fun performCleanup(option: CleanupOption): CleanupResult = withContext(Dispatchers.IO) {
        when (option) {
            CleanupOption.OLD_UNLISTENED_EPISODES -> {
                val sixMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }.timeInMillis
                
                val oldEpisodes = dao.getAllEpisodesSync().filter { 
                    !it.listened && it.pubDate < sixMonthsAgo 
                }
                
                // You would need to implement deleteEpisodes in DAO
                // dao.deleteEpisodes(oldEpisodes.map { it.id })
                
                CleanupResult(
                    option = option,
                    itemsAffected = oldEpisodes.size,
                    spaceSavedMB = oldEpisodes.size * 0.001 // Estimate
                )
            }
            
            CleanupOption.TRUNCATE_DESCRIPTIONS -> {
                val episodes = dao.getAllEpisodesSync()
                val episodesWithLongDesc = episodes.filter { 
                    (it.description?.length ?: 0) > 200 
                }
                
                episodesWithLongDesc.forEach { episode ->
                    val updated = episode.copy(
                        description = episode.description?.take(200)
                    )
                    dao.updateEpisode(updated)
                }
                
                CleanupResult(
                    option = option,
                    itemsAffected = episodesWithLongDesc.size,
                    spaceSavedMB = episodesWithLongDesc.size * 0.0005 // Estimate
                )
            }
            
            CleanupOption.REMOVE_INACTIVE_PODCASTS -> {
                // Remove podcasts not updated in last year
                val oneYearAgo = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                }.timeInMillis
                
                val inactivePodcasts = dao.getAllPodcastsSync().filter { 
                    (it.lastUpdated ?: 0L) < oneYearAgo 
                }
                
                inactivePodcasts.forEach { podcast ->
                    dao.deletePodcast(podcast.id)
                }
                
                CleanupResult(
                    option = option,
                    itemsAffected = inactivePodcasts.size,
                    spaceSavedMB = inactivePodcasts.size * 0.01 // Estimate
                )
            }
            
            CleanupOption.COMPACT_DATABASE -> {
                // This would trigger SQLite VACUUM command
                // You would need to add this to DAO
                // dao.compactDatabase()
                
                CleanupResult(
                    option = option,
                    itemsAffected = 1,
                    spaceSavedMB = 0.5 // Estimate
                )
            }
        }
    }
    
    suspend fun autoCleanup() = withContext(Dispatchers.IO) {
        val stats = getStorageStats()
        val cleanupResults = mutableListOf<CleanupResult>()
        
        // Auto-cleanup thresholds
        if (stats.oldUnlistenedEpisodes > 100) {
            cleanupResults.add(performCleanup(CleanupOption.OLD_UNLISTENED_EPISODES))
        }
        
        if (stats.episodesWithoutDescription > 50) {
            cleanupResults.add(performCleanup(CleanupOption.TRUNCATE_DESCRIPTIONS))
        }
        
        if (stats.totalSizeMB > 100) {
            cleanupResults.add(performCleanup(CleanupOption.COMPACT_DATABASE))
        }
        
        cleanupResults
    }
}