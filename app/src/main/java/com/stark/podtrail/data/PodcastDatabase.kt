package com.stark.podtrail.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Podcast::class, Episode::class], version = 7, exportSchema = false)
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
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
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
