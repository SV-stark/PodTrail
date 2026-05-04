package com.stark.podtrail.di

import android.content.Context
import androidx.room3.Room
import com.stark.podtrail.data.PodcastDao
import com.stark.podtrail.data.PodcastDatabase
import com.stark.podtrail.data.MIGRATION_5_6
import com.stark.podtrail.data.MIGRATION_6_7
import com.stark.podtrail.data.MIGRATION_7_8
import com.stark.podtrail.data.MIGRATION_8_9
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PodcastDatabase {
        return Room.databaseBuilder(
            context,
            PodcastDatabase::class.java,
            "podtrack.db"
        )
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .build()
    }

    @Provides
    fun providePodcastDao(database: PodcastDatabase): PodcastDao {
        return database.podcastDao()
    }
}
