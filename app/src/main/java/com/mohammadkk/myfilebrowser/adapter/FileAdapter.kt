package com.mohammadkk.myfilebrowser.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.anggrayudi.storage.file.DocumentFileCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.*
import com.mohammadkk.myfilebrowser.helper.MimetypeUtils
import com.mohammadkk.myfilebrowser.helper.extraDocsMimeType
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.*
import java.io.File


class FileAdapter(private val activity: Activity, private val isGrid: Boolean = false, private val listener: FileListener) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    private val mItems = SortedList(FileItem::class.java, object : SortedList.Callback<FileItem>() {
        override fun compare(o1: FileItem?, o2: FileItem?): Int {
            if (o1 == null || o2 == null) return 0
            if (o1.isDirectory != o2.isDirectory) {
                return if (o1.isDirectory) -1 else +1
            }
            return o1.name.compareTo(o2.name, true)
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
        val file = mItems[position]
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
                val placeholder = getPlaceholder(file)
                val imageOf = getImage(file, placeholder)
                withContext(uiDispatcher) {
                    val glide = Glide.with(activity)
                        .load(imageOf)
                        .apply(RequestOptions())
                        .placeholder(placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                    if (imageOf is String || imageOf is Uri) {
                        glide.centerCrop().centerCrop().into(holder.imgFileType)
                    } else {
                        glide.fitCenter().override(Target.SIZE_ORIGINAL).into(holder.imgFileType)
                    }
                }
            }
        }
        holder.itemView.setOnClickListener {
            itemClick?.invoke(file)
        }
        holder.itemView.setOnLongClickListener {
            createPopup(holder.itemView, position)
            true
        }
    }
    @SuppressLint("RestrictedApi")
    private fun createPopup(view: View, position: Int) {
        val currItem = mItems[position]
        val currFile = File(currItem.path)
        val popup = PopupMenu(activity, view)
        popup.inflate(R.menu.options_menu)
        val menuHelper = MenuPopupHelper(activity, popup.menu as MenuBuilder, view)
        menuHelper.setForceShowIcon(true)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.detailOption -> listener.onOpenDetailDialog(currItem, position)
                R.id.renameOption -> listener.onRenameFile(currFile, position)
                R.id.deleteOption -> listener.onDeleteFile(currItem, position)
                R.id.shareOption -> listener.onShareFile(currItem, position)
            }
            true
        }
        menuHelper.show()
    }
    fun setOnItemClick(listener: (item: FileItem) -> Unit) {
        itemClick = listener
    }
    private fun getImage(item: FileItem, placeholder: Int): Any {
        if (item.path.endsWith(".apk", true)) {
            return getApkIcon(item.path) ?: R.drawable.ic_android
        } else if (item.path.isImageSlow() || item.path.isVideoSlow()) {
            if (isQPlus() && (item.path.isAndroidData() || item.path.isAndroidObb())) {
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
            return art ?: MimetypeUtils.getMusicIcon(item.path)
        }
        return placeholder
    }
    private fun getApkIcon(path: String): Bitmap? {
        var bitmap: Bitmap? = null
        val pi = activity.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
        if (pi != null) {
            val ai = pi.applicationInfo
            ai.sourceDir = path
            ai.publicSourceDir = path
            val drawable = ai.loadIcon(activity.packageManager)
            if (drawable != null) bitmap = drawable.toBitmap()
        }
        return bitmap
    }
    private fun getPlaceholder(item: FileItem): Int {
        if (item.isDirectory) return R.drawable.ic_folder
        val path = item.path
        val images = MimetypeUtils.getImageResource(path.extension)
        return if (images != null) {
            return images
        } else {
            val mime = item.path.mimetype.lowercase()
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
    override fun getItemCount(): Int {
        return mItems.size()
    }
    override fun getSectionName(position: Int): String {
        return mItems[position].name.substring(0, 1)
    }
}