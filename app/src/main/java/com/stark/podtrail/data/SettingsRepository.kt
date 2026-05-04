package com.stark.podtrail.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room3.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import com.stark.podtrail.data.OpmlManager
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PodcastDatabase
) {
    private val dataStore = context.dataStore
    private val dao = database.podcastDao()

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC = booleanPreferencesKey("use_dynamic")
        val USE_AMOLED = booleanPreferencesKey("use_amoled")
        val CUSTOM_COLOR = intPreferencesKey("custom_color")
        val PROFILE_IMAGE = stringPreferencesKey("profile_image")
        val PROFILE_BG = stringPreferencesKey("profile_bg")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            useDynamicColor = prefs[Keys.USE_DYNAMIC] ?: true,
            useAmoled = prefs[Keys.USE_AMOLED] ?: false,
            customColor = prefs[Keys.CUSTOM_COLOR] ?: 0xFF6200EE.toInt(),
            profileImageUri = prefs[Keys.PROFILE_IMAGE],
            profileBgUri = prefs[Keys.PROFILE_BG],
            userName = prefs[Keys.USER_NAME]
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enable: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC] = enable }
    }

    suspend fun setAmoled(enable: Boolean) {
        dataStore.edit { it[Keys.USE_AMOLED] = enable }
    }

    suspend fun setCustomColor(color: Int) {
        dataStore.edit { it[Keys.CUSTOM_COLOR] = color }
    }

    suspend fun setProfileImage(uri: String?) {
        dataStore.edit { 
            if (uri != null) it[Keys.PROFILE_IMAGE] = uri else it.remove(Keys.PROFILE_IMAGE)
        }
    }

    suspend fun setProfileBg(uri: String?) {
        dataStore.edit {
            if (uri != null) it[Keys.PROFILE_BG] = uri else it.remove(Keys.PROFILE_BG)
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[Keys.USER_NAME] = name }
    }

    fun getDatabasePath(): java.io.File {
        return context.getDatabasePath("podtrack.db")
    }

    suspend fun importDatabase(uri: android.net.Uri): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) return@withContext false

                val jsonString = GZIPInputStream(inputStream).bufferedReader().use { it.readText() }

                val gson = com.google.gson.Gson()
                // Try parsing as MinimalBackupData first (version 2)
                try {
                    val backupData = gson.fromJson(jsonString, MinimalBackupData::class.java)
                    if (backupData.podcasts != null) { // Simple check
                        database.useWriterConnection { transactor ->
                            transactor.immediateTransaction {
                                dao.deleteAllEpisodes()
                                dao.deleteAllPodcasts()
                                
                                // 1. Insert Podcasts
                                val urlToIdMap = mutableMapOf<String, Long>()
                                backupData.podcasts.forEach { mp ->
                                    val p = Podcast(
                                        title = mp.title,
                                        feedUrl = mp.feedUrl,
                                        isFavorite = mp.isFavorite,
                                        imageUrl = null,
                                        description = null,
                                        primaryGenre = null
                                    )
                                    val id = dao.insertPodcast(p)
                                    urlToIdMap[mp.feedUrl] = id
                                }
                                
                                // 2. Insert Episodes (Stubs)
                                val episodeStubs = backupData.episodes?.mapNotNull { me ->
                                    val pid = urlToIdMap[me.feedUrl]
                                    if (pid != null) {
                                        Episode(
                                            podcastId = pid,
                                            guid = me.guid,
                                            title = "Restoring...",
                                            listened = me.listened,
                                            playbackPosition = me.playbackPosition,
                                            lastPlayedTimestamp = me.lastPlayedTimestamp,
                                            pubDate = 0,
                                            audioUrl = null,
                                            imageUrl = null,
                                            description = null
                                        )
                                    } else null
                                }
                                dao.insertAllEpisodes(episodeStubs ?: emptyList())
                            }
                        }
                        return@withContext true
                    }
                } catch (e: Exception) {
                    // Fallback to legacy
                }

                val legacyBackup = gson.fromJson(jsonString, BackupData::class.java)
                database.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        dao.deleteAllEpisodes()
                        dao.deleteAllPodcasts()
                        dao.insertPodcasts(legacyBackup.podcasts)
                        dao.insertAllEpisodes(legacyBackup.episodes)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportDatabase(uri: android.net.Uri): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val podcasts = dao.getAllPodcastsSync()
                val episodes = dao.getAllEpisodesSync()
                
                val minimalPodcasts = podcasts.map { p ->
                    MinimalPodcast(
                        feedUrl = p.feedUrl,
                        title = p.title,
                        isFavorite = p.isFavorite
                    )
                }
                
                val podcastIdToUrl = podcasts.associate { it.id to it.feedUrl }
                
                val minimalEpisodes = episodes.filter { it.listened || it.playbackPosition > 0 }
                    .mapNotNull { ep ->
                        val url = podcastIdToUrl[ep.podcastId]
                        if (url != null) {
                            MinimalEpisode(
                                feedUrl = url,
                                guid = ep.guid,
                                listened = ep.listened,
                                playbackPosition = ep.playbackPosition,
                                lastPlayedTimestamp = ep.lastPlayedTimestamp
                            )
                        } else null
                    }

                val backupData = MinimalBackupData(
                    podcasts = minimalPodcasts,
                    episodes = minimalEpisodes
                )
                
                val gson = com.google.gson.Gson()
                val jsonString = gson.toJson(backupData)
                
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    GZIPOutputStream(outputStream).bufferedWriter().use { it.write(jsonString) }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportOpml(uri: android.net.Uri): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val podcasts = dao.getAllPodcastsSync()
                val opmlManager = OpmlManager()
                val opmlString = opmlManager.generateOpml(podcasts)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(opmlString) }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importOpml(uri: android.net.Uri, podcastRepository: PodcastRepository): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var count = 0
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
                val opmlManager = OpmlManager()
                val outlines = opmlManager.parseOpml(inputStream)
                
                val existingPodcasts = podcastRepository.allPodcastsDirect()
                val existingUrls = existingPodcasts.map { it.feedUrl }.toSet()
                
                outlines.forEach { outline ->
                    if (!existingUrls.contains(outline.xmlUrl)) {
                        val result = podcastRepository.addPodcast(outline.xmlUrl)
                        if (result.isSuccess) {
                            count++
                        }
                    }
                }
                count
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }
}
