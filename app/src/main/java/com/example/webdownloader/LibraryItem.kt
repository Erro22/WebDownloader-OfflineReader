package com.example.webdownloader

import com.example.webdownloader.db.Page

sealed class LibraryItem {
    data class GroupHeader(
        val name: String, 
        val itemCount: Int, 
        var isExpanded: Boolean = false
    ) : LibraryItem()
    
    data class PageItem(val page: Page) : LibraryItem()
}
