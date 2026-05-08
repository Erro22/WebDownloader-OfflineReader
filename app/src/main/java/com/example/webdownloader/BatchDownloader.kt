package com.example.webdownloader

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.webdownloader.db.AppDatabase
import com.example.webdownloader.db.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class BatchDownloader(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val archiver = WebArchiver()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var isProcessing = false
    private var hiddenWebView: WebView? = null

    fun startBatch() {
        if (isProcessing) return
        processNext()
    }

    private fun processNext() {
        scope.launch {
            val nextItem = withContext(Dispatchers.IO) {
                db.pageDao().getAllPages().find { it.status == "QUEUED" }
            }

            if (nextItem == null) {
                isProcessing = false
                hiddenWebView?.destroy()
                hiddenWebView = null
                return
            }

            isProcessing = true
            downloadPage(nextItem)
        }
    }

    private fun downloadPage(page: Page) {
        if (hiddenWebView == null) {
            hiddenWebView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        }

        hiddenWebView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                saveArchive(page)
            }
            
            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                updateStatus(page, "FAILED")
                processNext()
            }
        }

        hiddenWebView?.loadUrl(page.url)
    }

    private fun saveArchive(page: Page) {
        scope.launch {
            val archiveDir = File(context.filesDir, "archives")
            if (!archiveDir.exists()) archiveDir.mkdirs()
            
            val fileName = "${UUID.randomUUID()}.mht"
            val outputFile = File(archiveDir, fileName)
            
            val result = archiver.archiveCurrentPage(hiddenWebView!!, outputFile)
            
            if (result is WebArchiver.ArchiveResult.Success) {
                val updatedPage = page.copy(
                    title = result.title,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    status = "COMPLETED"
                )
                withContext(Dispatchers.IO) {
                    db.pageDao().updatePage(updatedPage)
                }
            } else {
                updateStatus(page, "FAILED")
            }
            
            processNext()
        }
    }

    private fun updateStatus(page: Page, status: String) {
        scope.launch(Dispatchers.IO) {
            db.pageDao().updatePage(page.copy(status = status))
        }
    }
}
