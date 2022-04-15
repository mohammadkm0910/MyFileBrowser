package com.mohammadkk.myfilebrowser.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.anggrayudi.storage.file.DocumentFileCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.*
import com.mohammadkk.myfilebrowser.helper.FileResource
import com.mohammadkk.myfilebrowser.helper.extraDocsMimeType
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Method


class FileAdapter(private val activity: Activity, private val isGrid: Boolean = false, private val listener: FileListener) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    private val mItems = SortedList(FileItem::class.java, object : SortedList.Callback<FileItem>() {
        override fun compare(o1: FileItem?, o2: FileItem?): Int {
            if (o1 != null && o2 != null) {
                if (o1.isDirectory != o2.isDirectory) {
                    return if (o1.isDirectory) -1 else +1
                }
                return o1.name.compareTo(o2.name, true)
            }
            return 0
        }
        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }
        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }
        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }
        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }
        override fun areContentsTheSame(oldItem: FileItem?, newItem: FileItem?): Boolean {
            return oldItem?.equals(newItem) ?: false
        }
        override fun areItemsTheSame(item1: FileItem?, item2: FileItem?): Boolean {
            return item1?.equals(item2) ?: false
        }
    })
    private var itemClick: ((item: FileItem) -> Unit)? = null

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFileType: ImageView = itemView.findViewById(R.id.imgFileType)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        fun setSize(size: String) {
            val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
            tvFileSize.fixSetText(size)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val inflate = if (!isGrid) {
            LayoutInflater.from(activity).inflate(R.layout.list_items_file, parent, false)
        } else {
            LayoutInflater.from(activity).inflate(R.layout.list_items_file_grid, parent, false)
        }
        return FileViewHolder(inflate)
    }
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val executorJOB = SupervisorJob()
        val handlerExc = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }
        val uiDispatcher = Dispatchers.Main
        val ioDispatcher = Dispatchers.IO + executorJOB + handlerExc
        val uiScope = CoroutineScope(uiDispatcher)
        val file = get(position)
        holder.tvFileName.fixSetText(file.name)
        if (!isGrid) {
            uiScope.launch {
                withContext(ioDispatcher) {
                    val fileSize = file.getSize(activity)
                    withContext(uiDispatcher) {
                        holder.setSize(fileSize)
                    }
                }
            }
        }
        uiScope.launch {
            withContext(ioDispatcher) {
                val placeholder = getPlaceholder(file.path)
                val imageOf = getImage(position, placeholder)
                withContext(uiDispatcher) {
                    Glide.with(activity)
                        .asDrawable()
                        .load(imageOf)
                        .placeholder(placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .centerCrop()
                        .into(holder.imgFileType)
                }
            }
        }
        holder.itemView.setOnClickListener {
            itemClick?.invoke(file)
        }
        holder.itemView.setOnLongClickListener {
            val currFile = File(get(position).path)
            val popup = PopupMenu(activity, holder.itemView)
            popup.menuInflater.inflate(R.menu.options_menu,popup.menu)
            try {
                val classPopupMenu = Class.forName(popup.javaClass.name)
                val mPopup = classPopupMenu.getDeclaredField("mPopup")
                mPopup.isAccessible = true
                val menuPopupHelper = mPopup.get(popup)
                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons: Method = classPopupHelper.getMethod(
                    "setForceShowIcon", Boolean::class.javaPrimitiveType
                )
                setForceIcons.invoke(menuPopupHelper, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.detailOption -> listener.onOpenDetailDialog(get(position), position)
                    R.id.renameOption -> listener.onRenameFile(currFile, position)
                    R.id.deleteOption -> listener.onDeleteFile(get(position), position)
                    R.id.shareOption -> listener.onShareFile(get(position), position)
                }
                true
            }
            popup.show()
            true
        }
    }
    fun setOnItemClick(listener: (item: FileItem) -> Unit) {
        itemClick = listener
    }
    private fun getImage(position: Int, placeholder: Int): Any {
        val item = get(position)
        if (item.isDirectory) return R.drawable.ic_folder
        if (item.path.endsWith(".apk", true)) {
            val pi = activity.packageManager.getPackageArchiveInfo(
                item.path,
                PackageManager.GET_ACTIVITIES
            ) ?: return R.drawable.ic_android
            val ai = pi.applicationInfo
            ai.sourceDir = item.path
            ai.publicSourceDir = item.path
            return ai.loadIcon(activity.packageManager) ?: R.drawable.ic_android
        } else if (item.path.isImageSlow() || item.path.isVideoSlow()) {
            if (isQPlus() && (item.isAndroidData() || item.isAndroidObb())) {
                val docFile = DocumentFileCompat.fromFullPath(activity, item.path)
                if (docFile != null) return docFile.uri
            } else return item.path
        } else if (item.path.isAudioSlow()) {
            var art: ByteArray? = null
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(item.path)
                art = mmr.embeddedPicture
                mmr.release()
            } catch (e: Exception) {
            }
            return art ?: FileResource(item.path).getMusicIcon()
        }
        return placeholder
    }
    private fun getPlaceholder(path: String): Int {
        val fileResource = FileResource(path)
        val images = fileResource.getImages()
        return if (images != null) {
            return images
        } else {
            val mime = path.mimetype.lowercase()
            when {
                path.isImageSlow() -> R.drawable.ic_image
                path.isAudioSlow() -> R.drawable.ic_music
                path.isVideoSlow() -> R.drawable.ic_play
                path.endsWith(".apk", true) -> R.drawable.ic_android
                extraDocsMimeType.contains(mime) -> R.drawable.ic_docs
                else -> R.drawable.ic_misc_file
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        for (i in 0 until mItems.size())
            notifyItemChanged(i)
    }
    fun clear() {
        mItems.beginBatchedUpdates()
        while (mItems.size() > 0) mItems.removeItemAt(mItems.size() - 1)
        mItems.endBatchedUpdates()
    }
    fun updateItemAt(index: Int, item: File) {
        val toItem = item.toFileItem()
        mItems.updateItemAt(index, toItem)
    }
    fun add(item: FileItem): Int {
        return mItems.add(item)
    }
    fun addAll(items: List<FileItem>) {
        mItems.beginBatchedUpdates()
        items.map { add(it) }
        mItems.endBatchedUpdates()
    }
    fun removeItemAt(index: Int): FileItem {
        return mItems.removeItemAt(index)
    }
    fun get(position: Int): FileItem = mItems[position]
    override fun getItemCount(): Int {
        return mItems.size()
    }
    override fun getSectionName(position: Int): String {
        return mItems[position].name.substring(0, 1)
    }
}