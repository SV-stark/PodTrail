package com.stark.podtrail.data

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("podcasts") val podcasts: List<Podcast>,
    @SerializedName("episodes") val episodes: List<Episode>
)
