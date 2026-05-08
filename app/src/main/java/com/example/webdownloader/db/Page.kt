package com.example.webdownloader.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val fileSize: Long = 0,
    val status: String = "COMPLETED",
    val category: String = "INTERNAL",
    val groupName: String? = null
)
