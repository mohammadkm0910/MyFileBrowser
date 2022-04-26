package com.mohammadkk.myfilebrowser.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.file.DocumentFileCompat
import com.mohammadkk.myfilebrowser.BaseConfig
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.adapter.FileAdapter
import com.mohammadkk.myfilebrowser.adapter.FileListener
import com.mohammadkk.myfilebrowser.adapter.StorageAdapter
import com.mohammadkk.myfilebrowser.databinding.FragmentInternalBinding
import com.mohammadkk.myfilebrowser.extension.*
import com.mohammadkk.myfilebrowser.helper.DialogCreator
import com.mohammadkk.myfilebrowser.helper.SystemNewApi
import com.mohammadkk.myfilebrowser.helper.ensureBackgroundThread
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File
import java.io.IOException

class InternalFragment : BaseFragment(), FileListener {
    private lateinit var dialogCreator: DialogCreator
    private lateinit var binding: FragmentInternalBinding
    private var pathArgument: String = ""
    private lateinit var storage: File
    private lateinit var launcherIntent: ActivityResultLauncher<Intent>
    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        fileAdapter = FileAdapter(mActivity, listener = this)
        dialogCreator = DialogCreator(requireActivity())
        launcherIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { treeUri ->
                    val pathUri = treeUri.path ?: ""
                    val baseConfig = BaseConfig.newInstance(mContext)
                    if (pathUri.endsWith("Android/data") || pathUri.endsWith("Android/obb")) {
                        if (systemNewApi != null) {
                            baseConfig.setUriPath(systemNewApi!!, treeUri)
                            mContext.contentResolver.takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                            val documentFile = DocumentFile.fromTreeUri(mContext, treeUri)
                            displayDocumentFiles(documentFile)
                        }
                    }
                }
            }
        }
        arguments?.let { pathArgument = it.getString(PATH_ARG) ?: "" }
        pathArgument.let { if (it.isNotEmpty()) storage = File(it) }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInternalBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSplitStorage()
        runTimePermission(binding.root) {
            displayFiles()
            savedLayoutManager()
        }
    }
    private fun initSplitStorage() {
        val items = pathArgument.trim('/').split('/')
        val storageAdapter = StorageAdapter(mContext, items)
        storageAdapter.setOnItemClickListener { position ->
            val storageBuilder = StringBuilder()
            for (index in 0..position) {
                storageBuilder.append("/${items[index]}")
            }
            mActivity.navigate(newInstance(storageBuilder.toString()))
        }
        val layout = LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)
        binding.splitRecycler.layoutManager = layout
        binding.splitRecycler.adapter = storageAdapter
    }
    fun refreshFragment() {
        ensureBackgroundThread {
            try {
                val fileList = findFiles(storage)
                mActivity.runOnUiThread {
                    fileAdapter.clear()
                    fileAdapter.addAll(fileList)
                    fileAdapter.refresh()
                    scrollState?.also {
                        getLayoutList(binding.storageRecycler).onRestoreInstanceState(it)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val dc = DialogCreator(requireActivity())
        when (item.itemId) {
            R.id.createFileOption -> {
                dc.createNewInStorageDialog(true) { output, dialog ->
                    if (output.isEmpty || !output.isAValidFilename()) {
                        mContext.toast(if (output.isEmpty) R.string.empty_name else R.string.invalid_name)
                    } else {
                        val newPath = "${storage.absolutePath.trimEnd('/')}/$output"
                        val newFile = File(newPath)
                        if (newFile.exists()) {
                            mActivity.toast("This file already exists")
                        } else {
                            if (createFile(newFile)) {
                                mActivity.toast("New file created")
                                fileAdapter.add(newFile.toFileItem())
                                fileAdapter.refresh()
                            } else {
                                mActivity.toast("It is not possible to create a file")
                            }
                        }
                        dialog.dismiss()
                    }
                }
            }
            R.id.createFolderOption -> {
                dc.createNewInStorageDialog(false) { output, dialog ->
                    if (output.isEmpty || !output.isAValidFilename()) {
                        mContext.toast(if (output.isEmpty) R.string.empty_name else R.string.invalid_name)
                    } else {
                        val newPath = "${storage.absolutePath.trimEnd('/')}/$output"
                        val newFolder = File(newPath)
                        if (newFolder.exists()) {
                            mActivity.toast("This folder already exists")
                        } else {
                            if (createDir(newFolder)) {
                                mActivity.toast("New folder created")
                                fileAdapter.add(newFolder.toFileItem())
                                fileAdapter.refresh()
                            } else {
                                mActivity.toast("It is not possible to create a folder")
                            }
                        }
                        dialog.dismiss()
                    }
                }
            }
            R.id.exitOption -> {
                mContext.toast("Closing Application...")
                mActivity.moveTaskToBack(true)
                mActivity.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun findFiles(file: File): ArrayList<FileItem> {
        val list = arrayListOf<FileItem>()
        val files = file.listFiles()?.filter { !it.isHidden }
        files?.forEach { list.add(it.toFileItem()) }
        return list
    }
    private fun findFiles(documentFile: DocumentFile?): List<FileItem> {
        val list = ArrayList<FileItem>()
        val listDoc = documentFile?.listFiles() ?: return list
        listDoc.forEach { file ->
            val name = file?.name ?: return@forEach
            val item = FileItem(
                name,
                "${storage.absolutePath}/$name",
                file.isDirectory,
                file.length()
            )
            list.add(item)
        }
        return list
    }
    private fun displayDocumentFiles(documentFile: DocumentFile?) {
        binding.restrictionLayout.isVisible = documentFile?.listFiles().isNullOrEmpty()
        ensureBackgroundThread {
            runCatching {
                val items = findFiles(documentFile)
                Handler(Looper.getMainLooper()).post {
                    fileAdapter.addAll(items)
                }
            }
        }
    }
    override fun displayFiles() {
        val current = storage.toFileItem()
        val baseConfig = BaseConfig.newInstance(mContext)
        val currentEnum = mContext.getEnumSystemNewApi(current)
        binding.storageRecycler.setHasFixedSize(true)
        binding.storageRecycler.layoutManager = GridLayoutManager(mContext, 1)
        fileAdapter.setOnItemClick { item -> onFileClicked(item) }
        binding.storageRecycler.adapter = fileAdapter
        if (isQPlus() && current.systemDir().isNotEmpty()) {
            val intent = mContext.askPermission(current.systemDir(), current.path)
            val uri = baseConfig.getUriPath(currentEnum)
            if (uri != null && mContext.isPathPermission(uri)) {
                val doc = DocumentFile.fromTreeUri(mContext, uri)
                displayDocumentFiles(doc)
            } else {
                systemNewApi = currentEnum
                launcherIntent.launch(intent)
            }
            return
        }
        if (current.isAndroidData() || current.isAndroidObb()) {
            val documentFile = DocumentFileCompat.fromFile(mContext, storage)
            displayDocumentFiles(documentFile)
            return
        }
        binding.restrictionLayout.isVisible = storage.getCountChild() == 0
        ensureBackgroundThread {
            runCatching {
                val items = findFiles(storage)
                mActivity.runOnUiThread {
                    fileAdapter.addAll(items)
                }
            }
        }
    }
    private fun savedLayoutManager() {
        binding.storageRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                scrollState = getLayoutList(recyclerView).onSaveInstanceState()
            }
        })
    }
    private fun getLayoutList(recyclerView: RecyclerView): GridLayoutManager {
        return (recyclerView.layoutManager as GridLayoutManager)
    }
    private fun onFileClicked(item: FileItem) {
        if (item.isDirectory) {
            val internalFragment = newInstance(item.path)
            mActivity.navigate(internalFragment, true)
        } else {
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uriByFileItem(item), item.path.mimetype)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                mContext.launchRequireIntent(this)
            }
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
                if (it) {
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
            putExtra(Intent.EXTRA_STREAM, uriByFileItem(item))
            mContext.launchRequireIntent(
                Intent.createChooser(this,"share ${item.name}")
            )
        }
    }
    private fun createDir(folder: File): Boolean {
        if (folder.exists()) return false
        if (folder.mkdir()) {
            mActivity.rescanPaths(arrayOf(folder.absolutePath))
            return true
        } else {
            val parent = folder.parentFile ?: return false
            val document = DocumentFileCompat.fromFile(mContext, parent)
            if (document?.createDirectory(folder.name) != null) {
                mActivity.rescanPaths(arrayOf(folder.absolutePath))
                return true
            }
        }
        return false
    }
    private fun createFile(file: File): Boolean {
        var result = false
        if (file.exists()) result = !file.isDirectory
        try {
            if (file.createNewFile()) {
                if (file.exists()) {
                    mActivity.rescanPaths(arrayOf(file.absolutePath))
                    result = true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (!result) {
            val parent = file.parentFile ?: return false
            val document = DocumentFileCompat.fromFile(mContext, parent)
            try {
                val de = document?.createFile(file.mimetype, file.name)
                mActivity.rescanPaths(arrayOf(file.absolutePath))
                result = de != null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }
    companion object {
        private var scrollState: Parcelable? = null
        private const val PATH_ARG = "path_arg"
        private var systemNewApi: SystemNewApi? = null
        fun newInstance(path: String): InternalFragment {
            return InternalFragment().apply {
                arguments = Bundle().apply {
                    putString(PATH_ARG, path)
                }
            }
        }
    }
}