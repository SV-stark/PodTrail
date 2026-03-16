package com.stark.podtrail.network

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class FeedParserTest {
    private val parser = FeedParser()

    @Test
    fun `test parsing simple RSS feed`() = runBlocking {
        val xml = """
            <rss version="2.0">
                <channel>
                    <title>Test Podcast</title>
                    <description>A test podcast description</description>
                    <image>
                        <url>http://example.com/image.jpg</url>
                    </image>
                    <item>
                        <title>Episode 1</title>
                        <guid>ep1</guid>
                        <enclosure url="http://example.com/ep1.mp3" type="audio/mpeg" />
                        <pubDate>Mon, 01 Jan 2024 12:00:00 +0000</pubDate>
                        <description>First episode</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val (podcast, episodes) = parser.parseFeed(xml)

        assertNotNull(podcast)
        assertEquals("Test Podcast", podcast?.title)
        assertEquals("http://example.com/image.jpg", podcast?.imageUrl)

        assertEquals(1, episodes.size)
        assertEquals("Episode 1", episodes[0].title)
        assertEquals("ep1", episodes[0].guid)
        assertEquals("http://example.com/ep1.mp3", episodes[0].audioUrl)
    }

    @Test
    fun `test duration parsing`() {
        // Since parseDurationToMillis is private, I'll test it via parseFeed or make it internal too.
        // For now, I'll just check if it's correctly used in parseFeed.
        val xml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Ep 1</title>
                        <enclosure url="url" type="audio/mpeg" />
                        <itunes:duration>01:00:00</itunes:duration>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val (_, episodes) = parser.parseFeed(xml)
        assertEquals(3600000L, episodes[0].durationMillis)
    }
}
