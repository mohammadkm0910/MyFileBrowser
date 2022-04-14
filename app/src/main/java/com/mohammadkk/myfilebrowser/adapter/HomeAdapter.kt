package com.mohammadkk.myfilebrowser.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.myfilebrowser.databinding.ListItemsHomeBinding
import com.mohammadkk.myfilebrowser.extension.getColorOrAlpha
import com.mohammadkk.myfilebrowser.extension.navigate
import com.mohammadkk.myfilebrowser.fragment.MimeTypeFragment
import com.mohammadkk.myfilebrowser.model.HomeItems

class HomeAdapter(private val fragmentActivity: FragmentActivity, private val items: MutableSet<HomeItems>) : RecyclerView.Adapter<HomeAdapter.HomeHolder>() {
    class HomeHolder(binding: ListItemsHomeBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageHome = binding.imgHome
        val titleHome = binding.tvHome
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeHolder {
        return HomeHolder(ListItemsHomeBinding.inflate(LayoutInflater.from(fragmentActivity), parent, false))
    }
    override fun onBindViewHolder(holder: HomeHolder, position: Int) {
        val item = items.elementAt(position)
        holder.imageHome.setImageResource(item.icon)
        holder.imageHome.setBackgroundColor(fragmentActivity.getColorOrAlpha(item.fillColor, 0.5f))
        holder.titleHome.text = fragmentActivity.getString(item.name)
        holder.itemView.setOnClickListener {
            fragmentActivity.navigate(MimeTypeFragment.newInstance(item.tag), true)
        }
    }
    override fun getItemCount(): Int {
        return items.size
    }
}