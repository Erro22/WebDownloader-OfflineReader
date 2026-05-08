package com.example.webdownloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.webdownloader.db.Page
import java.text.SimpleDateFormat
import java.util.*

class PagesAdapter(
    private var allItems: List<LibraryItem> = emptyList(),
    private val onItemClick: (Page) -> Unit,
    private val onItemLongClick: (Page) -> Unit,
    private val onGroupToggle: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (allItems[position]) {
            is LibraryItem.GroupHeader -> TYPE_HEADER
            is LibraryItem.PageItem -> TYPE_PAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            GroupViewHolder(inflater.inflate(R.layout.item_group_header, parent, false))
        } else {
            PageViewHolder(inflater.inflate(R.layout.item_page, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = allItems[position]
        if (holder is GroupViewHolder && item is LibraryItem.GroupHeader) {
            holder.bind(item)
        } else if (holder is PageViewHolder && item is LibraryItem.PageItem) {
            holder.bind(item.page)
        }
    }

    override fun getItemCount() = allItems.size

    fun updateItems(newItems: List<LibraryItem>) {
        allItems = newItems
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvGroupName)
        private val tvCount: TextView = view.findViewById(R.id.tvGroupCount)
        private val ivExpand: ImageView = view.findViewById(R.id.ivExpand)

        fun bind(item: LibraryItem.GroupHeader) {
            tvName.text = item.name
            tvCount.text = "${item.itemCount} стр."
            ivExpand.rotation = if (item.isExpanded) 180f else 0f
            itemView.setOnClickListener { onGroupToggle(item.name) }
        }
    }

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val tvSize: TextView = view.findViewById(R.id.tvSize)
        private val ivFavicon: ImageView = view.findViewById(R.id.ivFavicon)

        fun bind(page: Page) {
            tvTitle.text = page.title
            tvUrl.text = page.url
            val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            tvDate.text = format.format(Date(page.timestamp))
            tvSize.text = formatFileSize(page.fileSize)
            
            Glide.with(itemView.context)
                .load(page.faviconUrl)
                .placeholder(R.drawable.ic_download)
                .into(ivFavicon)

            itemView.setOnClickListener { onItemClick(page) }
            itemView.setOnLongClickListener {
                onItemLongClick(page)
                true
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
