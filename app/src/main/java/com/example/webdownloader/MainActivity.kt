package com.example.webdownloader

import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var fabBackToInspector: View
    private lateinit var highlightOverlay: View

    private lateinit var db: AppDatabase
    private lateinit var adapter: PagesAdapter
    private val archiver = WebArchiver()
    private lateinit var batchDownloader: BatchDownloader

    private var currentMode = Mode.DOWNLOADER
    private var lastExtractedLinksJson: String? = null
    private val selectedUrls = mutableSetOf<String>()
    private val expandedGroups = mutableSetOf<String>()
    private var isInspectorActive = false

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
        fabBackToInspector = findViewById(R.id.fabBackToInspector)
        highlightOverlay = findViewById(R.id.highlightOverlay)

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
        batchDownloader = BatchDownloader(this)
    }

    private fun setupWebView() {
        @Suppress("DEPRECATION")
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            javaScriptCanOpenWindowsAutomatically = true
        }
        webView.isNestedScrollingEnabled = true
        
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onLinksExtracted(json: String) {
                runOnUiThread {
                    showLinksInspector(json)
                }
            }

            @android.webkit.JavascriptInterface
            fun onLinkToggledOnPage(url: String, isSelected: Boolean) {
                runOnUiThread {
                    if (isSelected) selectedUrls.add(url) else selectedUrls.remove(url)
                    // If BottomSheet is visible, we should ideally update it, 
                    // but for now we'll just keep the set in sync.
                }
            }

            @android.webkit.JavascriptInterface
            fun showNativeHighlight(x: Float, y: Float, width: Float, height: Float) {
                runOnUiThread {
                    positionAndShowHighlight(x, y, width, height)
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
                    
                    // Reset inspector state on new page load
                    isInspectorActive = false
                    fabBackToInspector.visibility = View.GONE
                    btnDownload.clearColorFilter()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Return false to allow WebView to handle the URL loading normally.
                // We only override if we need special handling (e.g. custom schemes), 
                // but for articles, the Library already handles opening local files.
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                // Remove interception for now to avoid deadlocks and rendering issues
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PagesAdapter(
            emptyList(),
            onItemClick = { page -> openPage(page) },
            onItemLongClick = { page -> showDeleteConfirmation(page) },
            onGroupToggle = { groupName ->
                if (expandedGroups.contains(groupName)) {
                    expandedGroups.remove(groupName)
                } else {
                    expandedGroups.add(groupName)
                }
                loadSavedPages()
            }
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

        btnDownload.setOnClickListener { 
            if (isInspectorActive) {
                isInspectorActive = false
                webView.reload()
                fabBackToInspector.visibility = View.GONE
                btnDownload.clearColorFilter()
            } else {
                if (webView.url != null) {
                    isInspectorActive = true
                    extractLinks()
                    fabBackToInspector.visibility = View.VISIBLE
                    btnDownload.setColorFilter(android.graphics.Color.parseColor("#BB86FC"))
                } else {
                    loadUrlFromInput()
                }
            }
        }
        fabSave.setOnClickListener { saveCurrentPage() }

        fabBackToInspector.setOnClickListener {
            lastExtractedLinksJson?.let { json ->
                showLinksInspector(json)
                fabBackToInspector.visibility = View.GONE
            }
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
        Log.d("WebDownloader", "STEP 1: saveCurrentPage() called")
        lifecycleScope.launch {
            Log.d("WebDownloader", "STEP 2: launch started")
            progressBar.visibility = View.VISIBLE
            val archiveDir = File(filesDir, "archives")
            Log.d("WebDownloader", "STEP 3: archiveDir = ${archiveDir.absolutePath}")
            if (!archiveDir.exists()) archiveDir.mkdirs()
            
            val fileName = "${UUID.randomUUID()}.mhtml"
            val outputFile = File(archiveDir, fileName)
            Log.d("WebDownloader", "STEP 4: outputFile = ${outputFile.absolutePath}")
            
            Log.d("WebDownloader", "STEP 5: calling archiver.archiveCurrentPage...")
            val result = archiver.archiveCurrentPage(webView, outputFile)
            Log.d("WebDownloader", "STEP 6: Archive result received: $result")
            
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
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val groups = db.pageDao().getAllGroupNames()
            val libraryItems = mutableListOf<LibraryItem>()

            // Add grouped items
            groups.forEach { groupName ->
                val pagesInGroup = db.pageDao().getPagesByGroup(groupName)
                val isExpanded = expandedGroups.contains(groupName)
                libraryItems.add(LibraryItem.GroupHeader(groupName, pagesInGroup.size, isExpanded))
                if (isExpanded) {
                    pagesInGroup.forEach { libraryItems.add(LibraryItem.PageItem(it)) }
                }
            }

            // Add ungrouped items
            val ungrouped = db.pageDao().getUngroupedPages()
            ungrouped.forEach { libraryItems.add(LibraryItem.PageItem(it)) }

            withContext(Dispatchers.Main) {
                adapter.updateItems(libraryItems)
                // Update empty state if needed
            }
        }
    }

    private fun filterPages(query: String) {
        lifecycleScope.launch {
            val pages = if (query.isEmpty()) db.pageDao().getAllPages() else db.pageDao().searchPages(query)
            adapter.updateItems(pages.map { LibraryItem.PageItem(it) })
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
        Log.d("WebDownloader", "extractLinks() called")
        Toast.makeText(this, "Анализ страницы...", Toast.LENGTH_SHORT).show()
        val script = assets.open("links_inspector.js").bufferedReader().use { it.readText() }
        webView.evaluateJavascript(script, null)
    }

    private fun showLinksInspector(json: String) {
        if (selectedUrls.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(webView, "Выберите ссылки на странице для скачивания", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
        }
        lastExtractedLinksJson = json
        val bottomSheet = LinksBottomSheet.newInstance(json, selectedUrls.toList())
        bottomSheet.setListeners(
            onDownload = { selectedLinks ->
                showGroupNameDialog(selectedLinks)
            },
            onPreview = { link ->
                val escapedUrl = link.url.replace("'", "\\'")
                webView.evaluateJavascript("highlightElement('$escapedUrl')", null)
                bottomSheet.dismiss()
                fabBackToInspector.visibility = View.VISIBLE
            },
            onSelectionChanged = { url, isSelected ->
                if (isSelected) selectedUrls.add(url) else selectedUrls.remove(url)
                val escapedUrl = url.replace("'", "\\'")
                webView.evaluateJavascript("updateLinkSelection('$escapedUrl', $isSelected)", null)
            }
        )
        bottomSheet.show(supportFragmentManager, "LinksInspector")
    }

    private fun queueBatchDownload(selectedLinks: List<LinkItem>, groupName: String? = null) {
        batchDownloader.enqueue(selectedLinks, groupName)
        Toast.makeText(this, "Добавлено в очередь: ${selectedLinks.size}", Toast.LENGTH_SHORT).show()
    }

    private fun showGroupNameDialog(selectedLinks: List<LinkItem>) {
        val input = EditText(this)
        input.hint = "Напр. Материалы по Kotlin"
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(48, 24, 48, 24)
        input.layoutParams = params
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Название группы")
            .setMessage("Введите название для этой группы страниц:")
            .setView(container)
            .setPositiveButton("Скачать") { _, _ ->
                val groupName = input.text.toString().takeIf { it.isNotBlank() }
                queueBatchDownload(selectedLinks, groupName)
                switchMode(Mode.LIBRARY)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun positionAndShowHighlight(x: Float, y: Float, w: Float, h: Float) {
        val density = resources.displayMetrics.density
        // Convert CSS pixels to physical pixels
        val pxX = (x * density).toInt()
        val pxY = (y * density).toInt()
        val pxW = (w * density).toInt()
        val pxH = (h * density).toInt()

        val params = highlightOverlay.layoutParams as RelativeLayout.LayoutParams
        params.leftMargin = pxX
        params.topMargin = pxY
        params.width = pxW
        params.height = pxH
        highlightOverlay.layoutParams = params
        
        highlightOverlay.visibility = View.VISIBLE
        highlightOverlay.alpha = 0f
        highlightOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                highlightOverlay.animate()
                    .alpha(0f)
                    .setStartDelay(3000)
                    .setDuration(500)
                    .withEndAction {
                        highlightOverlay.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    override fun onBackPressed() {
        if (isInspectorActive) {
            isInspectorActive = false
            webView.reload()
            fabBackToInspector.visibility = View.GONE
            btnDownload.clearColorFilter()
        } else if (webView.canGoBack() && currentMode == Mode.DOWNLOADER) {
            webView.goBack()
        } else if (currentMode != Mode.DOWNLOADER) {
            switchMode(Mode.DOWNLOADER)
        } else {
            super.onBackPressed()
        }
    }
}

