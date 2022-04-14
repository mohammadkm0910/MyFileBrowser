package com.mohammadkk.myfilebrowser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.myfilebrowser.databinding.ListItemsSplitBinding

class StorageAdapter(private val context: Context, private val items: List<String>) : RecyclerView.Adapter<StorageAdapter.StorageHolder>() {
    private var onItemClick: ((position: Int)->Unit)? = null

    class StorageHolder(binding: ListItemsSplitBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvSplit = binding.tvSplit
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageHolder {
        return StorageHolder(ListItemsSplitBinding.inflate(LayoutInflater.from(context), parent, false))
    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (items.isNotEmpty()) {
            recyclerView.layoutManager?.also {
                it.scrollToPosition(items.size - 1)
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }
    override fun onBindViewHolder(holder: StorageHolder, position: Int) {
        holder.tvSplit.text = items[position]
        holder.tvSplit.setOnClickListener {
            onItemClick?.invoke(position)
        }
    }
    fun setOnItemClickListener(listener: (position: Int)->Unit) {
        onItemClick = listener
    }
    override fun getItemCount(): Int {
        return items.size
    }
}