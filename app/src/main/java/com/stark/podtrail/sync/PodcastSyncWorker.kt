package com.stark.podtrail.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.stark.podtrail.data.PodcastRepository
import java.util.concurrent.TimeUnit

class PodcastSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get repository instance (you'll need to handle dependency injection here)
            // For now, this is a placeholder that would refresh all podcast feeds
            // repo.refreshAllPodcasts()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "PodcastSyncWork"
        
        fun scheduleSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
                
            val syncRequest = PeriodicWorkRequestBuilder<PodcastSyncWorker>(
                6, TimeUnit.HOURS, // repeatInterval, repeatIntervalTimeUnit
                1, TimeUnit.HOURS  // flexTimeInterval, flexIntervalTimeUnit
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
                
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncRequest
                )
        }
        
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }
}