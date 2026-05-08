package com.example.webdownloader

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webdownloader.db.AppDatabase
import com.example.webdownloader.db.Page
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var urlBarContainer: View
    private lateinit var toolbarTitle: TextView
    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var downloaderContainer: View
    private lateinit var libraryContainer: View
    private lateinit var emptyPreviewState: View
    private lateinit var rvSavedPages: RecyclerView
    private lateinit var searchArchive: EditText
    private lateinit var fabSave: FloatingActionButton
    private lateinit var btnDownload: ImageButton

    private lateinit var db: AppDatabase
    private lateinit var adapter: PagesAdapter
    private val archiver = WebArchiver()

    private var currentMode = Mode.DOWNLOADER

    enum class Mode { DOWNLOADER, LIBRARY, READING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupLogic()
        switchMode(Mode.DOWNLOADER)
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)
        urlBarContainer = findViewById(R.id.urlBarContainer)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        progressBar = findViewById(R.id.progressBar)
        downloaderContainer = findViewById(R.id.downloaderContainer)
        libraryContainer = findViewById(R.id.libraryContainer)
        emptyPreviewState = findViewById(R.id.emptyPreviewState)
        rvSavedPages = findViewById(R.id.rvSavedPages)
        searchArchive = findViewById(R.id.searchArchive)
        fabSave = findViewById(R.id.fabSave)
        btnDownload = findViewById(R.id.btnDownload)

        setSupportActionBar(toolbar)
    }

    private fun setupLogic() {
        db = AppDatabase.getDatabase(this)
        
        setupWebView()
        setupRecyclerView()
        setupDrawer()
        setupListeners()
        setupBatchDownloader()
    }

    private fun setupBatchDownloader() {
        // Trigger background processing on start if any items are queued
        val workRequest = OneTimeWorkRequestBuilder<BatchDownloadWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.isNestedScrollingEnabled = true
        
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onLinksExtracted(json: String) {
                runOnUiThread {
                    showLinksInspector(json)
                }
            }
        }, "AndroidInterface")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                if (currentMode == Mode.DOWNLOADER) {
                    urlInput.setText(url)
                    emptyPreviewState.visibility = View.GONE
                    fabSave.visibility = View.VISIBLE
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                
                // Only intercept actual page loads (not assets for now to avoid complexity)
                if (request.isForMainFrame) {
                    val localPage = runBlocking { db.pageDao().getPageByUrl(url) }
                    if (localPage != null && localPage.filePath.isNotEmpty()) {
                        val file = File(localPage.filePath)
                        if (file.exists()) {
                            return WebResourceResponse(
                                "text/html",
                                "UTF-8",
                                file.inputStream()
                            )
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PagesAdapter(
            pages = emptyList(),
            onItemClick = { openPage(it) },
            onItemLongClick = { showDeleteConfirmation(it) }
        )
        rvSavedPages.layoutManager = LinearLayoutManager(this)
        rvSavedPages.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    val pages = db.pageDao().getAllPages()
                    if (position < pages.size) {
                        deletePage(pages[position])
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(rvSavedPages)
    }

    private fun setupDrawer() {
        toolbar.setNavigationOnClickListener {
            if (currentMode == Mode.READING) {
                switchMode(Mode.LIBRARY)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_download -> switchMode(Mode.DOWNLOADER)
                R.id.nav_archive -> switchMode(Mode.LIBRARY)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupListeners() {
        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromInput()
                true
            } else false
        }

        btnDownload.setOnClickListener { loadUrlFromInput() }
        fabSave.setOnClickListener { saveCurrentPage() }
        fabSave.setOnLongClickListener {
            extractLinks()
            true
        }

        searchArchive.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterPages(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun switchMode(mode: Mode) {
        currentMode = mode
        when (mode) {
            Mode.DOWNLOADER -> {
                downloaderContainer.visibility = View.VISIBLE
                libraryContainer.visibility = View.GONE
                urlBarContainer.visibility = View.VISIBLE
                toolbarTitle.visibility = View.GONE
                fabSave.visibility = if (webView.url != null) View.VISIBLE else View.GONE
                toolbar.setNavigationIcon(R.drawable.ic_menu)
            }
            Mode.LIBRARY -> {
                downloaderContainer.visibility = View.GONE
                libraryContainer.visibility = View.VISIBLE
                urlBarContainer.visibility = View.GONE
                toolbarTitle.visibility = View.VISIBLE
                toolbarTitle.text = "Библиотека"
                fabSave.visibility = View.GONE
                toolbar.setNavigationIcon(R.drawable.ic_menu)
                loadSavedPages()
            }
            Mode.READING -> {
                downloaderContainer.visibility = View.VISIBLE
                libraryContainer.visibility = View.GONE
                urlBarContainer.visibility = View.GONE
                toolbarTitle.visibility = View.VISIBLE
                fabSave.visibility = View.GONE
                toolbar.setNavigationIcon(R.drawable.ic_back)
                emptyPreviewState.visibility = View.GONE
            }
        }
    }

    private fun loadUrlFromInput() {
        val urlStr = urlInput.text.toString()
        if (urlStr.isNotEmpty()) {
            val finalUrl = if (!urlStr.startsWith("http")) "https://$urlStr" else urlStr
            webView.loadUrl(finalUrl)
        }
    }

    private fun saveCurrentPage() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val archiveDir = File(filesDir, "archives")
            if (!archiveDir.exists()) archiveDir.mkdirs()
            
            val fileName = "${UUID.randomUUID()}.mht"
            val outputFile = File(archiveDir, fileName)
            
            val result = archiver.archiveCurrentPage(webView, outputFile)
            
            when (result) {
                is WebArchiver.ArchiveResult.Success -> {
                    val page = Page(
                        title = result.title,
                        url = webView.url ?: "",
                        filePath = outputFile.absolutePath,
                        fileSize = outputFile.length(),
                        faviconUrl = "https://www.google.com/s2/favicons?domain=${webView.url}&sz=128"
                    )
                    db.pageDao().insertPage(page)
                    Toast.makeText(this@MainActivity, "Сохранено в архив", Toast.LENGTH_SHORT).show()
                    switchMode(Mode.LIBRARY)
                }
                is WebArchiver.ArchiveResult.Error -> {
                    Toast.makeText(this@MainActivity, "Ошибка: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun openPage(page: Page) {
        switchMode(Mode.READING)
        toolbarTitle.text = page.title
        webView.loadUrl("file://${page.filePath}")
    }

    private fun loadSavedPages() {
        lifecycleScope.launch {
            val pages = db.pageDao().getAllPages()
            adapter.updatePages(pages)
        }
    }

    private fun filterPages(query: String) {
        lifecycleScope.launch {
            val pages = if (query.isEmpty()) db.pageDao().getAllPages() else db.pageDao().searchPages(query)
            adapter.updatePages(pages)
        }
    }

    private fun deletePage(page: Page) {
        lifecycleScope.launch {
            db.pageDao().deletePage(page)
            File(page.filePath).delete()
            loadSavedPages()
            Toast.makeText(this@MainActivity, "Удалено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(page: Page) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удалить страницу?")
            .setMessage("Вы уверены, что хотите удалить \"${page.title}\" из архива?")
            .setPositiveButton("Удалить") { _, _ -> deletePage(page) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun extractLinks() {
        val script = assets.open("links_inspector.js").bufferedReader().use { it.readText() }
        webView.evaluateJavascript(script, null)
    }

    private fun showLinksInspector(json: String) {
        val bottomSheet = LinksBottomSheet.newInstance(json)
        bottomSheet.setListeners(
            onDownload = { selectedLinks ->
                queueBatchDownload(selectedLinks)
            },
            onPreview = { link ->
                webView.evaluateJavascript("highlightElement('${link.url}')", null)
            }
        )
        bottomSheet.show(supportFragmentManager, "LinksInspector")
    }

    private fun queueBatchDownload(selectedLinks: List<LinkItem>) {
        val currentUrl = webView.url ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            // Find or create a parent entry for grouping if needed
            val parent = db.pageDao().getPageByUrl(currentUrl)
            val parentId = parent?.id

            selectedLinks.take(50).forEach { link ->
                val existing = db.pageDao().getPageByUrl(link.url)
                if (existing == null) {
                    val page = Page(
                        title = link.title,
                        url = link.url,
                        filePath = "",
                        status = "QUEUED",
                        parentId = parentId,
                        category = link.category,
                        estimatedWeight = link.estimatedWeight,
                        faviconUrl = "https://www.google.com/s2/favicons?domain=${link.domain}&sz=128"
                    )
                    db.pageDao().insertPage(page)
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Добавлено в очередь: ${selectedLinks.size}", Toast.LENGTH_SHORT).show()
                val workRequest = OneTimeWorkRequestBuilder<BatchDownloadWorker>().build()
                WorkManager.getInstance(this@MainActivity).enqueue(workRequest)
            }
        }
    }
}

