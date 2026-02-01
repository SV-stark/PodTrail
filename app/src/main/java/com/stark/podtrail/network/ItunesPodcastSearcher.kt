package com.stark.podtrail.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchResponse(
    val resultCount: Int,
    val results: List<SearchResult>
)

data class SearchResult(
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("feedUrl") val feedUrl: String?,
    @SerializedName("artworkUrl600") val artworkUrl600: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("collectionId") val collectionId: Long?,
    @SerializedName("primaryGenreName") val primaryGenreName: String? = null,
    @SerializedName("primaryGenreId") val primaryGenreId: Long? = null
)

// RSS Feed Data Models
data class RssFeedResponse(val feed: RssFeed)
data class RssFeed(val entry: List<RssEntry>)
data class RssEntry(
    @SerializedName("im:name") val name: Label,
    @SerializedName("im:image") val images: List<RssImage>,
    val summary: Label?,
    val id: RssId,
    @SerializedName("im:artist") val artist: Label?
)
data class Label(val label: String)
data class RssImage(val label: String, val attributes: RssImageAttr)
data class RssImageAttr(val height: String)
data class RssId(val attributes: RssIdAttr)
data class RssIdAttr(@SerializedName("im:id") val id: String)

class ItunesPodcastSearcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?media=podcast&term=${encodedQuery}"
        return@withContext fetchAndParse(url)
    }

    suspend fun lookup(id: Long): SearchResult? = withContext(Dispatchers.IO) {
         val url = "https://itunes.apple.com/lookup?id=$id"
         return@withContext fetchAndParse(url).firstOrNull()
    }

    suspend fun getTopPodcasts(limit: Int = 20, genreId: Long? = null): List<SearchResult> = withContext(Dispatchers.IO) {
        val baseUrl = "https://itunes.apple.com/us/rss/toppodcasts/limit=$limit"
        val url = if (genreId != null) "$baseUrl/genre=$genreId/json" else "$baseUrl/json"
        
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val rssResponse = gson.fromJson(body, RssFeedResponse::class.java)
                
                // Map RSS Entry to SearchResult
                rssResponse.feed.entry.map { entry ->
                     val largeImage = entry.images.lastOrNull()?.label
                     SearchResult(
                         collectionName = entry.name.label,
                         feedUrl = null, // RSS feed doesn't have the feedUrl, needs lookup if selected
                         artworkUrl600 = largeImage,
                         artworkUrl100 = largeImage,
                         artistName = entry.artist?.label,
                         collectionId = entry.id.attributes.id.toLongOrNull(),
                         primaryGenreName = null
                     )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun fetchAndParse(url: String): List<SearchResult> {
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val searchResponse = gson.fromJson(body, SearchResponse::class.java)
                return searchResponse.results
            }
        } catch (e:IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

