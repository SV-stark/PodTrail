package com.stark.podtrail.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        if (!isOpen) return
        val db = openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").close()
    }
}

val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE episodes ADD COLUMN playbackPosition INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedTimestamp INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE podcasts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Add indices for podcastId and listened/lastPlayedTimestamp
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_podcastId ON episodes(podcastId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_listened_lastPlayedTimestamp ON episodes(listened, lastPlayedTimestamp)")
    }
}

val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `episodeId` INTEGER NOT NULL, `position` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`episodeId`) REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `position` INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_episodeId ON playlists(episodeId)")
    }
}
