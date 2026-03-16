package com.stark.podtrail.network

import android.util.Log
import com.stark.podtrail.data.Episode
import com.stark.podtrail.data.Podcast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedPodcast(
    val title: String?,
    val imageUrl: String?,
    val description: String?,
    val genre: String? = null
)

data class ParsedEpisode(
    val title: String,
    val guid: String,
    val audioUrl: String,
    val pubDateMillis: Long,
    val imageUrl: String? = null,
    val durationMillis: Long? = null,
    val description: String? = null,
    val episodeNumber: Int? = null
)

@Singleton
class FeedParser @Inject constructor() {
    private val client = OkHttpClient()

    suspend fun fetchFeed(url: String): Pair<ParsedPodcast?, List<ParsedEpisode>> {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty body")
            return parseFeed(body)
        }
    }

    @VisibleForTesting
    internal fun parseFeed(xml: String): Pair<ParsedPodcast?, List<ParsedEpisode>> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var podcast: ParsedPodcast? = null
        val episodes = mutableListOf<ParsedEpisode>()

        var currentTag: String? = null
        var inItem = false

        // Podcast level
        var pTitle: String? = null
        var pImage: String? = null
        var pDesc: String? = null
        var pGenre: String? = null

        // Episode level
        var eTitle: String? = null
        var eGuid: String? = null
        var eAudio: String? = null
        var ePubDate: String? = null
        var eImage: String? = null
        var eDuration: String? = null
        var eDesc: String? = null
        var eNumber: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                    } else if (currentTag == "itunes:image" || currentTag == "image") {
                        val url = parser.getAttributeValue(null, "href") ?: ""
                        if (inItem) {
                            if (url.isNotEmpty()) eImage = url
                        } else {
                            if (url.isNotEmpty()) pImage = url
                        }
                    } else if (currentTag == "enclosure" && inItem) {
                        eAudio = parser.getAttributeValue(null, "url")
                    } else if (currentTag == "itunes:category" && !inItem) {
                         pGenre = parser.getAttributeValue(null, "text")
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        if (inItem) {
                            when (currentTag) {
                                "title" -> eTitle = text
                                "guid" -> eGuid = text
                                "pubDate" -> ePubDate = text
                                "itunes:duration" -> eDuration = text
                                "description", "itunes:summary", "content:encoded" -> {
                                     // Prefer longer content if we already have a snippet, but for now just pick one
                                     if (eDesc == null || currentTag == "content:encoded") eDesc = text
                                }
                                "itunes:episode" -> eNumber = text
                            }
                        } else {
                            when (currentTag) {
                                "title" -> pTitle = text
                                "description", "itunes:summary" -> pDesc = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        if (eTitle != null && eAudio != null) {
                            episodes.add(ParsedEpisode(
                                title = eTitle ?: "Untitled",
                                guid = eGuid ?: eAudio ?: eTitle ?: "",
                                audioUrl = eAudio ?: "",
                                pubDateMillis = parseDateToMillis(ePubDate),
                                imageUrl = eImage,
                                durationMillis = parseDurationToMillis(eDuration),
                                description = eDesc,
                                episodeNumber = eNumber?.toIntOrNull()
                            ))
                        }
                        // Reset episode vars
                        eTitle = null; eGuid = null; eAudio = null; ePubDate = null
                        eImage = null; eDuration = null; eDesc = null; eNumber = null
                        inItem = false
                    } else if (parser.name == "channel") {
                        podcast = ParsedPodcast(pTitle, pImage, pDesc, pGenre)
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        return Pair(podcast, episodes)
    }

    private fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        val formats = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (e: Exception) { /* continue */ }
        }
        return 0L
    }

    private fun parseDurationToMillis(duration: String?): Long? {
        if (duration == null) return null
        try {
            if (duration.contains(":")) {
                val parts = duration.split(":").map { it.trim().toLongOrNull() ?: 0L }
                return when (parts.size) {
                    1 -> parts[0] * 1000
                    2 -> (parts[0] * 60 + parts[1]) * 1000
                    3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                    else -> null
                }
            } else {
                return duration.toLongOrNull()?.let { it * 1000 }
            }
        } catch (e: Exception) {
            return null
        }
    }
}
