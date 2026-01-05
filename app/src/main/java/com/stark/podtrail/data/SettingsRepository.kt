package com.stark.podtrail.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.room.withTransaction

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val useAmoled: Boolean = false,
    val customColor: Int = 0xFF6200EE.toInt(), // Purple default
    val profileImageUri: String? = null,
    val profileBgUri: String? = null
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC = booleanPreferencesKey("use_dynamic")
        val USE_AMOLED = booleanPreferencesKey("use_amoled")
        val CUSTOM_COLOR = intPreferencesKey("custom_color")
        val PROFILE_IMAGE = stringPreferencesKey("profile_image")
        val PROFILE_BG = stringPreferencesKey("profile_bg")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            useDynamicColor = prefs[Keys.USE_DYNAMIC] ?: true,
            useAmoled = prefs[Keys.USE_AMOLED] ?: false,
            customColor = prefs[Keys.CUSTOM_COLOR] ?: 0xFF6200EE.toInt(),
            profileImageUri = prefs[Keys.PROFILE_IMAGE],
            profileBgUri = prefs[Keys.PROFILE_BG]
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

    fun getDatabasePath(): java.io.File {
        return context.getDatabasePath("podtrack.db")
    }

    suspend fun importDatabase(uri: android.net.Uri): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString == null) return@withContext false

                val gson = com.google.gson.Gson()
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                // Restore
                val db = PodcastDatabase.getInstance(context)
                val dao = db.podcastDao()
                
                // To ensure atomicity:
                androidx.room.withTransaction(db) {
                    dao.deleteAllEpisodes()
                    dao.deleteAllPodcasts()
                    dao.insertPodcasts(backupData.podcasts)
                    dao.insertAllEpisodes(backupData.episodes)
                }

                // Restart app to refresh data? Or just let flows update?
                // The flows should update automatically. 
                // But the original code restarted the app.
                // Let's keep the restart logic in the UI if needed, but here we just return true.
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
                val db = PodcastDatabase.getInstance(context)
                val dao = db.podcastDao()
                
                val podcasts = dao.getAllPodcastsSync()
                val episodes = dao.getAllEpisodesSync()
                
                val backupData = BackupData(
                    podcasts = podcasts,
                    episodes = episodes
                )
                
                val gson = com.google.gson.Gson()
                val jsonString = gson.toJson(backupData)
                
                context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { it?.write(jsonString) }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

