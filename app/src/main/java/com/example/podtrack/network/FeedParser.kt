package com.example.podtrack.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

data class ParsedEpisode(
    val title: String,
    val guid: String,
    val pubDateMillis: Long,
    val audioUrl: String?,
    val episodeNumber: Int?,
    val durationMillis: Long?
)

class FeedParser {
    private val client = OkHttpClient()

    suspend fun fetchFeed(url: String): Pair<String?, List<ParsedEpisode>> {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return Pair(null, emptyList())
            val body = resp.body?.string() ?: return Pair(null, emptyList())
            val title = parseFeedTitle(body)
            val episodes = parseEpisodes(body)
            return Pair(title, episodes)
        }
    }

    private fun parseFeedTitle(xml: String): String? {
        val parser = xmlPullParser(xml)
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("title", ignoreCase = true)) {
                    // ensure this is channel/title, but we accept first reasonable title
                    parser.next()
                    return parser.text
                }
                event = parser.next()
            }
        } catch (e: Exception) { }
        return null
    }

    private fun parseEpisodes(xml: String): List<ParsedEpisode> {
        val list = mutableListOf<ParsedEpisode>()
        val parser = xmlPullParser(xml)
        try {
            var event = parser.eventType
            var insideItem = false
            var title: String? = null
            var guid: String? = null
            var pubDate: Long = 0
            var audioUrl: String? = null
            var episodeNumber: Int? = null
            var durationMillis: Long? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val rawName = parser.name ?: ""
                    val name = rawName.lowercase(Locale.ROOT)
                    if (name == "item") {
                        insideItem = true
                        title = null; guid = null; pubDate = 0; audioUrl = null; episodeNumber = null; durationMillis = null
                    } else if (insideItem && name == "title") {
                        parser.next()
                        title = parser.text
                    } else if (insideItem && name == "guid") {
                        parser.next()
                        guid = parser.text
                    } else if (insideItem && name == "pubdate") {
                        parser.next()
                        pubDate = parseDateToMillis(parser.text)
                    } else if (insideItem && name.contains("enclosure")) {
                        // enclosure often contains url attribute
                        audioUrl = parser.getAttributeValue(null, "url") ?: audioUrl
                    } else if (insideItem && name == "link" && audioUrl == null) {
                        parser.next()
                        audioUrl = parser.text
                    } else if (insideItem && (name == "itunes:episode" || rawName.equals("episode", ignoreCase = true) || name.endsWith(":episode"))) {
                        parser.next()
                        episodeNumber = parser.text?.toIntOrNull()
                    } else if (insideItem && (name == "itunes:duration" || name.endsWith(":duration") || rawName.equals("duration", ignoreCase = true))) {
                        parser.next()
                        durationMillis = parseDurationToMillis(parser.text)
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name.equals("item", ignoreCase = true)) {
                    insideItem = false
                    if (title != null) {
                        val finalGuid = guid ?: title
                        list += ParsedEpisode(
                            title = title,
                            guid = finalGuid,
                            pubDateMillis = pubDate,
                            audioUrl = audioUrl,
                            episodeNumber = episodeNumber,
                            durationMillis = durationMillis
                        )
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) { }
        return list
    }

    private fun xmlPullParser(xml: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        return parser
    }

    private fun parseDateToMillis(text: String?): Long {
        if (text == null) return 0
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                return sdf.parse(text)?.time ?: 0L
            } catch (e: Exception) { }
        }
        return 0L
    }

    private fun parseDurationToMillis(text: String?): Long? {
        if (text == null) return null
        val trimmed = text.trim()
        // If it's just seconds:
        if (trimmed.matches(Regex("^\d+$"))) {
            return trimmed.toLong() * 1000L
        }
        // If it's HH:MM:SS or MM:SS
        val parts = trimmed.split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> (parts[0]*3600 + parts[1]*60 + parts[2]) * 1000
            2 -> (parts[0]*60 + parts[1]) * 1000
            1 -> (parts[0]) * 1000
            else -> null
        }
    }
}