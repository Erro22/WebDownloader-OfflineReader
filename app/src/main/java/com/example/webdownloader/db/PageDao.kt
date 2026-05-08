package com.example.webdownloader.db

import androidx.room.*

@Dao
interface PageDao {
    @Query("SELECT * FROM pages ORDER BY timestamp DESC")
    suspend fun getAllPages(): List<Page>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page)

    @Delete
    suspend fun deletePage(page: Page)

    @Update
    suspend fun updatePage(page: Page)

    @Query("SELECT * FROM pages WHERE url = :url LIMIT 1")
    suspend fun getPageByUrl(url: String): Page?

    @Query("SELECT * FROM pages WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchPages(query: String): List<Page>

    @Query("SELECT DISTINCT groupName FROM pages WHERE groupName IS NOT NULL")
    suspend fun getAllGroupNames(): List<String>

    @Query("SELECT * FROM pages WHERE groupName = :groupName ORDER BY timestamp DESC")
    suspend fun getPagesByGroup(groupName: String): List<Page>

    @Query("SELECT * FROM pages WHERE groupName IS NULL ORDER BY timestamp DESC")
    suspend fun getUngroupedPages(): List<Page>
}
