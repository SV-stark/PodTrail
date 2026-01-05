package com.example.podtrail.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Podcast::class, Episode::class], version = 5, exportSchema = false)
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