package com.example.webdownloader

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.webdownloader.db.AppDatabase
import com.example.webdownloader.db.Page
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Без названия"
        val category = inputData.getString("category") ?: "INTERNAL"
        val groupName = inputData.getString("groupName")

        Log.d("DownloadWorker", "Starting background download for: $url")

        return try {
            val success = downloadPage(url, title, category, groupName)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error downloading $url", e)
            Result.retry()
        }
    }

    private suspend fun downloadPage(url: String, title: String, category: String, groupName: String?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        withContext(Dispatchers.Main) {
            val webView = WebView(applicationContext)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    saveArchive(view, title, url ?: "", category, groupName, deferred)
                }

                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    Log.e("DownloadWorker", "WebView error: ${error?.description}")
                    deferred.complete(false)
                }
            }
            webView.loadUrl(url)
        }

        return deferred.await()
    }

    private fun saveArchive(
        view: WebView?, 
        title: String, 
        url: String, 
        category: String, 
        groupName: String?,
        deferred: CompletableDeferred<Boolean>
    ) {
        if (view == null) {
            deferred.complete(false)
            return
        }

        val archiveDir = File(applicationContext.filesDir, "archives")
        if (!archiveDir.exists()) archiveDir.mkdirs()

        val fileName = "${UUID.randomUUID()}.mhtml"
        val outputFile = File(archiveDir, fileName)
        val finalTitle = view.title ?: title

        view.saveWebArchive(outputFile.absolutePath, false) { path ->
            if (path != null) {
                val page = Page(
                    title = finalTitle,
                    url = url,
                    filePath = path,
                    fileSize = outputFile.length(),
                    faviconUrl = "https://www.google.com/s2/favicons?domain=$url&sz=128",
                    category = category,
                    groupName = groupName
                )
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    db.pageDao().insertPage(page)
                    deferred.complete(true)
                }
            } else {
                deferred.complete(false)
            }
        }
    }
}
