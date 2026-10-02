package com.distribuerp.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncManager {

    const val TRABAJO_GENERAL = "sync_general"

    private const val BACKOFF_BASE_SEG = 30L

    fun encolarSincronizacion(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncGeneralWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_BASE_SEG,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                TRABAJO_GENERAL,
                ExistingWorkPolicy.KEEP,
                request
            )
    }
}