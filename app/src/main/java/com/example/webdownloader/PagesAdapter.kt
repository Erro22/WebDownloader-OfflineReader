package com.example.webdownloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.webdownloader.db.Page
import java.text.SimpleDateFormat
import java.util.*

class PagesAdapter(
    private var pages: List<Page>,
    private val onItemClick: (Page) -> Unit,
    private val onItemLongClick: (Page) -> Unit
) : RecyclerView.Adapter<PagesAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSize: TextView = view.findViewById(R.id.tvSize)
        val ivFavicon: ImageView = view.findViewById(R.id.ivFavicon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.tvTitle.text = page.title
        holder.tvUrl.text = page.url
        
        val date = Date(page.timestamp)
        val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        holder.tvDate.text = format.format(date)
        
        holder.tvSize.text = formatFileSize(page.fileSize)

        val faviconUrl = page.faviconUrl ?: "https://www.google.com/s2/favicons?domain=${page.url}&sz=128"
        Glide.with(holder.itemView.context)
            .load(faviconUrl)
            .placeholder(R.drawable.ic_download)
            .into(holder.ivFavicon)

        holder.itemView.setOnClickListener { onItemClick(page) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(page)
            true
        }
    }

    override fun getItemCount() = pages.size

    fun updatePages(newPages: List<Page>) {
        val diffResult = DiffUtil.calculateDiff(PageDiffCallback(pages, newPages))
        pages = newPages
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    class PageDiffCallback(
        private val oldList: List<Page>,
        private val newList: List<Page>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newList[newPos]
    }
}

