package com.example.webdownloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class LinksAdapter(
    private val onLinkClick: (LinkItem) -> Unit,
    private val onSelectionChanged: () -> Unit
) : ListAdapter<LinkItem, LinksAdapter.LinkViewHolder>(LinkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvLinkTitle)
        private val domain: TextView = itemView.findViewById(R.id.tvLinkDomain)
        private val checkBox: CheckBox = itemView.findViewById(R.id.cbLink)
        private val previewBtn: ImageView = itemView.findViewById(R.id.ivPreviewLink)

        fun bind(item: LinkItem) {
            title.text = item.title
            domain.text = "${item.displayUrl} • ${item.category}"
            
            // Highlight already saved items
            if (item.downloadStatus == "COMPLETED") {
                domain.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Material Green
            } else {
                domain.setTextColor(android.graphics.Color.GRAY)
            }
            
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.isSelected
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onSelectionChanged()
            }

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }

            previewBtn.setOnClickListener {
                onLinkClick(item)
            }
        }
    }

    class LinkDiffCallback : DiffUtil.ItemCallback<LinkItem>() {
        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem) = oldItem == newItem
    }
}
