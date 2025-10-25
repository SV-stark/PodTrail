package com.example.podtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Podcast::class, Episode::class], version = 1, exportSchema = false)
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
                ).build().also { INSTANCE = it }
            }
    }
}