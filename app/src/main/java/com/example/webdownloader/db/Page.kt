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
    val iconPath: String? = null,
    val faviconUrl: String? = null,
    val fileSize: Long = 0,
    val tags: String? = null,
    val status: String = "COMPLETED"
)
