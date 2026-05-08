package com.example.webdownloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatchDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        try {
            val downloader = BatchDownloader(applicationContext)
            downloader.startBatch()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
