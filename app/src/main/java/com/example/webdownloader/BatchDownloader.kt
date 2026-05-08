package com.example.webdownloader

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.webdownloader.db.AppDatabase
import com.example.webdownloader.db.Page
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class BatchDownloader(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessing = false
    private val queue: Queue<LinkItem> = LinkedList()
    
    // Headless WebView for background processing
    private val headlessWebView: WebView by lazy {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    fun enqueue(links: List<LinkItem>) {
        queue.addAll(links)
        if (!isProcessing) {
            processNext()
        }
    }

    private fun processNext() {
        if (queue.isEmpty()) {
            isProcessing = false
            Log.d("BatchDownloader", "Queue empty, stopping.")
            return
        }

        if (isSystemOverloaded()) {
            Log.w("BatchDownloader", "System overloaded, waiting 30s...")
            scope.launch {
                delay(30000)
                processNext()
            }
            return
        }

        isProcessing = true
        val link = queue.poll() ?: return
        downloadLink(link)
    }

    private fun downloadLink(link: LinkItem) {
        Log.d("BatchDownloader", "Starting download: ${link.url}")
        
        headlessWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                scope.launch {
                    saveArchive(link, view)
                }
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                Log.e("BatchDownloader", "Error loading ${link.url}: ${error?.description}")
                processNext()
            }
        }

        headlessWebView.loadUrl(link.url)
    }

    private suspend fun saveArchive(link: LinkItem, view: WebView?) {
        if (view == null) return
        
        val archiveDir = File(context.filesDir, "archives")
        if (!archiveDir.exists()) archiveDir.mkdirs()
        
        val fileName = "${UUID.randomUUID()}.mhtml"
        val outputFile = File(archiveDir, fileName)
        
        // Use the actual page title if available, otherwise fallback to link title
        val finalTitle = view.title ?: link.title

        view.saveWebArchive(outputFile.absolutePath, false) { path ->
            scope.launch(Dispatchers.IO) {
                if (path != null) {
                    val page = Page(
                        title = finalTitle,
                        url = link.url,
                        filePath = path,
                        fileSize = outputFile.length(),
                        faviconUrl = "https://www.google.com/s2/favicons?domain=${link.url}&sz=128",
                        category = link.category
                    )
                    db.pageDao().insertPage(page)
                    Log.d("BatchDownloader", "Successfully saved: $finalTitle")
                } else {
                    Log.e("BatchDownloader", "Failed to save archive for ${link.url}")
                }
                
                withContext(Dispatchers.Main) {
                    processNext()
                }
            }
        }
    }

    private fun isSystemOverloaded(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val temp = 30 // Placeholder for temperature if needed
        return level < 10 || temp > 50
    }
}
