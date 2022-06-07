package com.mohammadkk.myfilebrowser.fragment

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.adapter.FileAdapter
import com.mohammadkk.myfilebrowser.adapter.FileListener
import com.mohammadkk.myfilebrowser.adapter.HomeAdapter
import com.mohammadkk.myfilebrowser.databinding.FragmentHomeBinding
import com.mohammadkk.myfilebrowser.extension.*
import com.mohammadkk.myfilebrowser.helper.*
import com.mohammadkk.myfilebrowser.model.FileItem
import com.mohammadkk.myfilebrowser.model.HomeItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeFragment : BaseFragment(), FileListener {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var fileAdapter: FileAdapter
    private lateinit var dialogCreator: DialogCreator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileAdapter = FileAdapter(mActivity, true, this)
        dialogCreator = DialogCreator(requireActivity())
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayGridHome()
        runTimePermission(binding.root) {
            displayFiles()
        }
    }
    private fun displayGridHome() {
        val span = if (mContext.isLandscape || mContext.isTablet) 6 else 3
        binding.rvHomeGrid.layoutManager = GridLayoutManager(mContext, span)
        val items = mutableSetOf(
            HomeItems(R.string.images, R.drawable.ic_image, R.color.image_holder, IMAGES),
            HomeItems(R.string.videos, R.drawable.ic_play, R.color.play_holder, VIDEOS),
            HomeItems(R.string.audios, R.drawable.ic_music, R.color.music_holder, AUDIOS),
            HomeItems(R.string.documents, R.drawable.ic_docs, R.color.docs_holder, DOCUMENTS),
            HomeItems(R.string.archives, R.drawable.ic_archives, R.color.archives_holder, ARCHIVES),
            HomeItems(R.string.others, R.drawable.ic_others, R.color.others_holder, OTHERS)
        )
        binding.rvHomeGrid.adapter = HomeAdapter(mActivity, items)
    }
    private fun findRecentFiles(): List<FileItem> {
        val fileItems = arrayListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )
        try {
            if (isRPlus()) {
                val queryArgs = bundleOf(
                    ContentResolver.QUERY_ARG_LIMIT to RECENT_LIMIT,
                    ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED),
                    ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                mContext.contentResolver.query(uri, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $RECENT_LIMIT"
                mContext.contentResolver.query(uri, projection, null, null, sortOrder)
            }?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val path = cursor.getStringVal( MediaStore.Files.FileColumns.DATA)
                        val name = cursor.getStringOrNullVal( MediaStore.Files.FileColumns.DISPLAY_NAME) ?: path.substringAfterLast('/')
                        val size = cursor.getLongVal(MediaStore.Files.FileColumns.SIZE)
                        if (!name.startsWith('.')) {
                            fileItems.add(
                                FileItem(name, path, File(path).isDirectory, size)
                            )
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {}
        return fileItems
    }
    override fun displayFiles() {
        (binding.recentRV.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.recentRV.setHasFixedSize(false)
        binding.recentRV.isNestedScrollingEnabled = false
        binding.recentRV.layoutManager = GridLayoutManager(mContext, 3)
        fileAdapter.setOnItemClick { item -> onFileClicked(item) }
        binding.recentRV.adapter = fileAdapter
        ensureBackgroundThread {
            runCatching {
                val items = findRecentFiles()
                mActivity.runOnUiThread {
                    fileAdapter.addAll(items)
                }
            }
        }
    }
    fun refreshFragment() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val items = findRecentFiles()
                withContext(Dispatchers.Main) {
                    fileAdapter.clear()
                    items.forEach { fileAdapter.add(it) }
                }
            }
        }
    }
    private fun onFileClicked(item: FileItem) {
        if (item.isDirectory) {
            mActivity.navigate(InternalFragment.newInstance(item.path), true)
            return
        }
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uriByFilePath(item.path), item.path.mimetype)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            mContext.launchRequireIntent(this)
        }
    }
    override fun onOpenDetailDialog(item: FileItem, position: Int) {
        dialogCreator.detailDialog(item)
    }
    override fun onRenameFile(file: File, position: Int) {
        dialogCreator.renameDialog(file.name) { output, dialog ->
            var newName = output
            val edtExtension = output.substringAfterLast('.')
            val extension = file.extension
            if (edtExtension.isEmpty()) newName += if (extension.isNotEmpty()) ".$extension" else ""
            val newFile = File(file.parent?:return@renameDialog, output)
            if (file.renameTo(mContext, newFile)) {
                fileAdapter.updateItemAt(position, newFile)
                mContext.rescanPaths(arrayOf(file.absolutePath, newFile.absolutePath)) {
                    mContext.updateFileMediaStore(file.absolutePath, newFile.absolutePath)
                }
                fileAdapter.refresh()
                mContext.toast("renamed!!")
            } else {
                mContext.toast("Couldn't Renamed!")
            }
            dialog.dismiss()
        }
    }
    override fun onDeleteFile(item: FileItem, position: Int) {
        dialogCreator.deleteDialog(item.name) {
            deleteFileItem(item) {
                if (it || !item.isExists()) {
                    mContext.deleteFileMediaStore(item.path)
                    fileAdapter.removeItemAt(position)
                    fileAdapter.refresh()
                    mContext.toast("Deleted!")
                } else mContext.toast("Couldn't Deleted!")
            }
        }
    }
    override fun onShareFile(item: FileItem, position: Int) {
        Intent().apply {
            action = Intent.ACTION_SEND
            type = item.path.mimetype
            putExtra(Intent.EXTRA_STREAM, uriByFilePath(item.path))
            mContext.launchRequireIntent(
                Intent.createChooser(this,"share ${item.name}")
            )
        }
    }
    companion object {
        private const val RECENT_LIMIT = 60
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}