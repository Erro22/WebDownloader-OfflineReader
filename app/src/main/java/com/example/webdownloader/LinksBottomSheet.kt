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

    companion object {
        fun newInstance(linksJson: String): LinksBottomSheet {
            val fragment = LinksBottomSheet()
            val args = Bundle()
            args.putString("links_json", linksJson)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString("links_json") ?: "[]"
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            links.add(LinkItem(
                title = obj.getString("title"),
                url = obj.getString("url"),
                x = obj.getInt("x"),
                y = obj.getInt("y"),
                width = obj.getInt("width"),
                height = obj.getInt("height"),
                category = obj.optString("category", "EXTERNAL"),
                estimatedWeight = obj.optLong("estimatedWeight", 0)
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
            onSelectionChanged = {
                val selectedCount = links.count { it.isSelected }
                tvCount.text = "Выбрано: $selectedCount"
            }
        )

        rvLinks.layoutManager = LinearLayoutManager(context)
        rvLinks.adapter = adapter
        adapter.submitList(links)

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val currentList = adapter.currentList
            currentList.forEach { it.isSelected = isChecked }
            adapter.notifyDataSetChanged()
            updateCounter(tvCount)
        }

        btnInvert.setOnClickListener {
            val currentList = adapter.currentList
            currentList.forEach { it.isSelected = !it.isSelected }
            adapter.notifyDataSetChanged()
            updateCounter(tvCount)
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
            1 -> "INTERNAL"
            2 -> "EXTERNAL"
            3 -> "MEDIA"
            else -> null
        }
        
        val filtered = links.filter {
            (category == null || it.category == category) &&
            (it.title.lowercase().contains(q) || it.url.lowercase().contains(q))
        }
        adapter.submitList(filtered)
    }

    private fun updateCounter(tvCount: TextView) {
        val selectedCount = links.count { it.isSelected }
        tvCount.text = "Выбрано: $selectedCount"
    }

    fun setListeners(onDownload: (List<LinkItem>) -> Unit, onPreview: (LinkItem) -> Unit) {
        this.onDownloadSelected = onDownload
        this.onPreviewLink = onPreview
    }
}
