package com.example.webdownloader

import android.webkit.WebView
import android.util.Log
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebArchiver {

    suspend fun archiveCurrentPage(webView: WebView, outputFile: File): ArchiveResult = suspendCoroutine { continuation ->
        val url = webView.url ?: ""
        val title = webView.title ?: "No Title"
        Log.d("WebDownloader", "Starting saveWebArchive for: $url")
        
        webView.saveWebArchive(outputFile.absolutePath, false) { path ->
            Log.d("WebDownloader", "saveWebArchive callback reached. Path: $path")
            if (path != null) {
                continuation.resume(ArchiveResult.Success(title, path))
            } else {
                continuation.resume(ArchiveResult.Error("Failed to save archive"))
            }
        }
    }

    sealed class ArchiveResult {
        data class Success(val title: String, val filePath: String) : ArchiveResult()
        data class Error(val message: String) : ArchiveResult()
    }
}

