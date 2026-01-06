package com.stark.podtrail.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.StringWriter

data class OpmlOutline(
    val title: String,
    val xmlUrl: String
)

class OpmlManager {

    fun generateOpml(podcasts: List<Podcast>): String {
        val serializer: XmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        
        try {
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag(null, "opml")
            serializer.attribute(null, "version", "1.0")
            
            serializer.startTag(null, "head")
            serializer.startTag(null, "title")
            serializer.text("PodTrail Subscriptions")
            serializer.endTag(null, "title")
            serializer.endTag(null, "head")
            
            serializer.startTag(null, "body")
            
            podcasts.forEach { podcast ->
                serializer.startTag(null, "outline")
                serializer.attribute(null, "type", "rss")
                serializer.attribute(null, "text", podcast.title)
                serializer.attribute(null, "title", podcast.title)
                serializer.attribute(null, "xmlUrl", podcast.feedUrl)
                serializer.endTag(null, "outline")
            }
            
            serializer.endTag(null, "body")
            serializer.endTag(null, "opml")
            serializer.endDocument()
            return writer.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun parseOpml(inputStream: InputStream): List<OpmlOutline> {
        val outlines = mutableListOf<OpmlOutline>()
        val parser: XmlPullParser = Xml.newPullParser()
        
        try {
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "outline") {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                    val text = parser.getAttributeValue(null, "text") ?: parser.getAttributeValue(null, "title") ?: "Unknown Podcast"
                    
                    if (!xmlUrl.isNullOrBlank()) {
                        outlines.add(OpmlOutline(text, xmlUrl))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return outlines
    }
}
