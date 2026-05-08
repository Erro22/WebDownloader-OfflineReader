package com.example.webdownloader

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray

class LinksBottomSheet : BottomSheetDialogFragment() {

    private var links: MutableList<LinkItem> = mutableListOf()
    private lateinit var adapter: LinksAdapter
    private var onDownloadSelected: (List<LinkItem>) -> Unit = {}
    private var onPreviewLink: (LinkItem) -> Unit = {}
    private var onSelectionChanged: (String, Boolean) -> Unit = { _, _ -> }

    companion object {
        fun newInstance(linksJson: String, selectedUrls: List<String>): LinksBottomSheet {
            val fragment = LinksBottomSheet()
            val args = Bundle()
            args.putString("links_json", linksJson)
            args.putStringArrayList("selected_urls", ArrayList(selectedUrls))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString("links_json") ?: "[]"
        val selected = arguments?.getStringArrayList("selected_urls") ?: emptyList<String>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val url = obj.getString("url")
            links.add(LinkItem(
                title = obj.getString("title"),
                url = url,
                displayUrl = obj.optString("displayUrl", url),
                x = obj.getInt("x"),
                y = obj.getInt("y"),
                width = obj.getInt("width"),
                height = obj.getInt("height"),
                category = obj.optString("category", "EXTERNAL"),
                estimatedWeight = obj.optLong("estimatedWeight", 0),
                isSelected = selected.contains(url)
            ))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_links_inspector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvLinks: RecyclerView = view.findViewById(R.id.rvLinks)
        val tvCount: TextView = view.findViewById(R.id.tvSelectedCount)
        val cbSelectAll: CheckBox = view.findViewById(R.id.cbSelectAll)
        val btnInvert: MaterialButton = view.findViewById(R.id.btnInvertSelection)
        val tabCategories: TabLayout = view.findViewById(R.id.tabCategories)
        val etSearch: EditText = view.findViewById(R.id.etSearchLinks)
        val btnDownload: MaterialButton = view.findViewById(R.id.btnBatchDownload)

        adapter = LinksAdapter(
            onLinkClick = { onPreviewLink(it) },
            onSelectionChanged = { item ->
                onSelectionChanged(item.url, item.isSelected)
                updateCounter(tvCount, btnDownload)
            }
        )

        rvLinks.layoutManager = LinearLayoutManager(context)
        rvLinks.adapter = adapter
        adapter.submitList(links)

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val currentList = adapter.currentList
            currentList.forEach { it.isSelected = isChecked }
            adapter.notifyDataSetChanged()
            updateCounter(tvCount, btnDownload)
        }

        btnInvert.setOnClickListener {
            val currentList = adapter.currentList
            currentList.forEach { it.isSelected = !it.isSelected }
            adapter.notifyDataSetChanged()
            updateCounter(tvCount, btnDownload)
        }

        tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterLinks(etSearch.text.toString(), tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterLinks(s.toString(), tabCategories.selectedTabPosition)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnDownload.setOnClickListener {
            val selected = links.filter { it.isSelected }
            if (selected.isNotEmpty()) {
                onDownloadSelected(selected)
                dismiss()
            }
        }
    }

    private fun filterLinks(query: String, tabPosition: Int) {
        val q = query.lowercase()
        val category = when(tabPosition) {
            1 -> "СТАТЬИ"
            2 -> "ВНУТРЕННИЕ"
            3 -> "МЕДИА"
            4 -> "ВНЕШНИЕ"
            5 -> "PROCHEЕ"
            else -> null
        }
        
        val filtered = links.filter {
            (category == null || it.category == category) &&
            (it.title.lowercase().contains(q) || it.url.lowercase().contains(q))
        }
        adapter.submitList(filtered)
    }

    private fun updateCounter(tvCount: TextView, btnDownload: MaterialButton) {
        val selectedCount = links.count { it.isSelected }
        tvCount.text = "Выбрано: $selectedCount"
        btnDownload.text = if (selectedCount > 0) "Скачать выбранное ($selectedCount)" else "Скачать выбранное"
    }

    fun setListeners(
        onDownload: (List<LinkItem>) -> Unit, 
        onPreview: (LinkItem) -> Unit,
        onSelectionChanged: (String, Boolean) -> Unit
    ) {
        this.onDownloadSelected = onDownload
        this.onPreviewLink = onPreview
        this.onSelectionChanged = onSelectionChanged
    }
}
