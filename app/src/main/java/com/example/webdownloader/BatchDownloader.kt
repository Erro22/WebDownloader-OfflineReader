package com.example.webdownloader

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class BatchDownloader(private val context: Context) {
    
    fun enqueue(links: List<LinkItem>, groupName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        
        val workRequests = links.map { link ->
            val data = Data.Builder()
                .putString("url", link.url)
                .putString("title", link.title)
                .putString("category", link.category)
                .putString("groupName", groupName)
                .build()

            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }

        workManager.enqueue(workRequests)
    }
}
