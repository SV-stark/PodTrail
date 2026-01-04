package com.example.podtrail.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class SearchResponse(
    val resultCount: Int,
    val results: List<SearchResult>
)

data class SearchResult(
    val collectionName: String?,
    val feedUrl: String?,
    val artworkUrl600: String?,
    val artworkUrl100: String?,
    val artistName: String?
)

class ItunesPodcastSearcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val url = "https://itunes.apple.com/search?media=podcast&term=${query}"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val searchResponse = gson.fromJson(body, SearchResponse::class.java)
                return@withContext searchResponse.results
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}
