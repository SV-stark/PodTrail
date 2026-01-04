package com.example.podtrail.network

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
    val imageUrl: String?,
    val episodeNumber: Int?,
    val durationMillis: Long?
)

data class ParsedPodcast(
    val title: String?,
    val imageUrl: String?
)

class FeedParser {
    private val client = OkHttpClient()

    suspend fun fetchFeed(url: String): Pair<ParsedPodcast?, List<ParsedEpisode>> {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return Pair(null, emptyList())
            val body = resp.body?.string() ?: return Pair(null, emptyList())
            val podcastInfo = parseFeedInfo(body)
            val episodes = parseEpisodes(body)
            return Pair(podcastInfo, episodes)
        }
    }

    private fun parseFeedInfo(xml: String): ParsedPodcast? {
        val parser = xmlPullParser(xml)
        try {
            var event = parser.eventType
            var title: String? = null
            var imageUrl: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name.equals("title", ignoreCase = true) && title == null) {
                        parser.next()
                        title = parser.text
                    } else if (name.equals("image", ignoreCase = true)) {
                        // Standard RSS image
                        imageUrl = parseImageTag(parser) ?: imageUrl
                    } else if (name.equals("itunes:image", ignoreCase = true) && imageUrl == null) {
                         imageUrl = parser.getAttributeValue(null, "href")
                    }
                }
                 // naive stop if we have both, or break after first item... 
                 // but better to scan a bit more? usually channel comes first.
                 // let's break if we see "item"
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                    break
                }
                event = parser.next()
            }
            return ParsedPodcast(title, imageUrl)
        } catch (e: Exception) { }
        return null
    }

    private fun parseImageTag(parser: XmlPullParser): String? {
        // parser is at <image>, looking for <url> inside
        var url: String? = null
        var event = parser.next()
        while (event != XmlPullParser.END_TAG || parser.name?.equals("image", ignoreCase = true) == false) {
             if (event == XmlPullParser.START_TAG && parser.name.equals("url", ignoreCase = true)) {
                 parser.next()
                 url = parser.text
             }
             event = parser.next()
        }
        return url
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
            var imageUrl: String? = null
            var episodeNumber: Int? = null
            var durationMillis: Long? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val rawName = parser.name ?: ""
                    val name = rawName.lowercase(Locale.ROOT)
                    if (name == "item") {
                        insideItem = true
                        title = null; guid = null; pubDate = 0; audioUrl = null; imageUrl = null; episodeNumber = null; durationMillis = null
                    } else if (insideItem) {
                        if (name == "title") {
                            parser.next()
                            title = parser.text
                        } else if (name == "guid") {
                            parser.next()
                            guid = parser.text
                        } else if (name == "pubdate") {
                            parser.next()
                            pubDate = parseDateToMillis(parser.text)
                        } else if (name.contains("enclosure")) {
                            audioUrl = parser.getAttributeValue(null, "url") ?: audioUrl
                        } else if (name == "link" && audioUrl == null) {
                            parser.next()
                            audioUrl = parser.text
                        } else if (name == "itunes:image" || name.endsWith(":image")) {
                            imageUrl = parser.getAttributeValue(null, "href")
                        } else if (name == "itunes:episode" || rawName.equals("episode", ignoreCase = true) || name.endsWith(":episode")) {
                            parser.next()
                            episodeNumber = parser.text?.toIntOrNull()
                        } else if (name == "itunes:duration" || name.endsWith(":duration") || rawName.equals("duration", ignoreCase = true)) {
                            parser.next()
                            durationMillis = parseDurationToMillis(parser.text)
                        }
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
                            imageUrl = imageUrl,
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
        if (trimmed.matches(Regex("^\\d+$"))) {
            return trimmed.toLong() * 1000L
        }
        val parts = trimmed.split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> (parts[0]*3600 + parts[1]*60 + parts[2]) * 1000
            2 -> (parts[0]*60 + parts[1]) * 1000
            1 -> (parts[0]) * 1000
            else -> null
        }
    }
}