package com.example.webdownloader

data class LinkItem(
    val title: String,
    val url: String,
    val displayUrl: String = url,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    var isSelected: Boolean = false,
    val category: String = "EXTERNAL",
    val estimatedWeight: Long = 0,
    var downloadStatus: String? = null // PENDING, DOWNLOADING, COMPLETED, FAILED
) {
    val domain: String
        get() = try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
}
