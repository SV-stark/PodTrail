package com.stark.podtrail.data

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("podcasts") val podcasts: List<Podcast>,
    @SerializedName("episodes") val episodes: List<Episode>
)

data class MinimalBackupData(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("podcasts") val podcasts: List<MinimalPodcast>,
    @SerializedName("episodes") val episodes: List<MinimalEpisode>
)

data class MinimalPodcast(
    val feedUrl: String,
    val title: String,
    val isFavorite: Boolean
)

data class MinimalEpisode(
    val feedUrl: String, // To link back to podcast without relying on IDs
    val guid: String,
    val listened: Boolean,
    val playbackPosition: Long,
    val lastPlayedTimestamp: Long
)
