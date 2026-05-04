package com.stark.podtrail.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [Podcast::class, Episode::class, Playlist::class, PlaylistCollection::class], version = 9, exportSchema = false)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        @Volatile private var INSTANCE: PodcastDatabase? = null
        fun getInstance(context: Context): PodcastDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PodcastDatabase::class.java,
                    "podtrack.db"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .build().also { INSTANCE = it }
            }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    fun checkpoint() {
        // checkpoint() logic might need update for Room 3
        // For now, leaving it as is or removing if not critical
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE episodes ADD COLUMN playbackPosition INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedTimestamp INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE podcasts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // Add indices for podcastId and listened/lastPlayedTimestamp
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_podcastId ON episodes(podcastId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_listened_lastPlayedTimestamp ON episodes(listened, lastPlayedTimestamp)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `episodeId` INTEGER NOT NULL, `position` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`episodeId`) REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `playlist_collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `position` INTEGER NOT NULL DEFAULT 0)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_episodeId ON playlists(episodeId)")
    }
}
