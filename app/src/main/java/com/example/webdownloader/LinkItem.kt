package com.example.webdownloader

data class LinkItem(
    val title: String,
    val url: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    var isSelected: Boolean = false
) {
    val domain: String
        get() = try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
}
