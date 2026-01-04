package com.example.podtrail

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.podtrail.data.Episode
import com.example.podtrail.data.Podcast
import com.example.podtrail.data.PodcastDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class GenerateScreenshotsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureScreenshots() {
        val context = composeTestRule.activity.applicationContext
        val db = PodcastDatabase.getInstance(context)

        // 1. Populate Dummy Data
        runBlocking {
            db.podcastDao().deleteAllEpisodes()
            db.podcastDao().deleteAllPodcasts()

            val p = Podcast(
                id = 1,
                title = "Tech Talk Daily",
                feedUrl = "https://example.com/feed",
                imageUrl = "https://picsum.photos/200" // Dummy image, might not load in emulator without net, but placeholder will show
            )
            db.podcastDao().insertPodcast(p)

            val eps = listOf(
                Episode(podcastId = 1, title = "The Future of AI", guid = "1", pubDate = System.currentTimeMillis(), durationMillis = 1200000, episodeNumber = 101),
                Episode(podcastId = 1, title = "Kotlin Coroutines", guid = "2", pubDate = System.currentTimeMillis() - 86400000, durationMillis = 1500000, episodeNumber = 100),
                Episode(podcastId = 1, title = "Jetpack Compose 1.6", guid = "3", pubDate = System.currentTimeMillis() - 172800000, durationMillis = 1800000, episodeNumber = 99)
            )
            db.podcastDao().insertEpisodes(eps)
        }
        
        // Wait for UI to stabilize
        composeTestRule.waitForIdle()
        saveScreenshot("1_home")

        // 2. Open Search
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.waitForIdle()
        saveScreenshot("2_search")
        
        // Back to Home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 3. Open Podcast (Episode List)
        composeTestRule.onNodeWithText("Tech Talk Daily").performClick()
        composeTestRule.waitForIdle()
        saveScreenshot("3_episode_list")

        // 4. Open Episode Details
        composeTestRule.onNodeWithText("The Future of AI").performClick()
        composeTestRule.waitForIdle()
        saveScreenshot("4_episode_details")
        
        // 5. Mark Listened
         composeTestRule.onNodeWithText("Mark Listened").performClick()
         composeTestRule.waitForIdle()
         saveScreenshot("5_details_listened")
    }

    private fun saveScreenshot(name: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        try {
            val path = "/sdcard/Pictures/screenshots/$name.png"
            // Ensure directory exists (usually done via adb shell mkdir beforehand, but good practice to check logic if possible, though standard java IO might check permission)
            // In instrumented test, writing to sdcard requires grant rule or generic setup. Emulator runner usually handles permissions.
            FileOutputStream(path).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
