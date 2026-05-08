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
                height = obj.getInt("height")
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
            links.forEach { it.isSelected = isChecked }
            adapter.notifyDataSetChanged()
            val selectedCount = if (isChecked) links.size else 0
            tvCount.text = "Выбрано: $selectedCount"
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = links.filter { 
                    it.title.lowercase().contains(query) || it.url.lowercase().contains(query) 
                }
                adapter.submitList(filtered)
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

    fun setListeners(onDownload: (List<LinkItem>) -> Unit, onPreview: (LinkItem) -> Unit) {
        this.onDownloadSelected = onDownload
        this.onPreviewLink = onPreview
    }
}
