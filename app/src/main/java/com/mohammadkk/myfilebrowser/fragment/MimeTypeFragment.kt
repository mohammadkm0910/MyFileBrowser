package com.mohammadkk.myfilebrowser.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore.Files
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.mohammadkk.myfilebrowser.adapter.FileAdapter
import com.mohammadkk.myfilebrowser.adapter.FileListener
import com.mohammadkk.myfilebrowser.databinding.FragmentMimTypeBinding
import com.mohammadkk.myfilebrowser.extension.*
import com.mohammadkk.myfilebrowser.helper.*
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File
import java.util.*

class MimeTypeFragment: BaseFragment(), FileListener {
    private lateinit var binding: FragmentMimTypeBinding
    private lateinit var dialogCreator: DialogCreator
    private var typeArgument: String? = null
    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogCreator = DialogCreator(requireActivity())
        arguments?.let { typeArgument = it.getString(TYPE_ARG) }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMimTypeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileAdapter = FileAdapter(mActivity, true, this)
        runTimePermission(binding.root) {
            displayFiles()
        }
    }
    private fun findFiles(): List<FileItem> {
        val fileItems = arrayListOf<FileItem>()
        val uri = Files.getContentUri("external")
        val projection = arrayOf(
            Files.FileColumns.MIME_TYPE,
            Files.FileColumns.DATA,
            Files.FileColumns.DISPLAY_NAME,
            Files.FileColumns.SIZE
        )
        mContext.queryCursor(uri, projection) { cursor ->
            try {
                val fullMimeType = cursor.getStringOrNullVal(Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault()) ?:return@queryCursor
                val name = cursor.getStringVal(Files.FileColumns.DISPLAY_NAME)
                if (name.startsWith(".")) return@queryCursor
                val size = cursor.getLongVal(Files.FileColumns.SIZE)
                if (size == 0L) return@queryCursor
                val path = cursor.getStringVal(Files.FileColumns.DATA)
                val mimeType = fullMimeType.substringBefore("/")

                when (typeArgument) {
                    IMAGES -> {
                        if (mimeType == "image") {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    VIDEOS -> {
                        if (mimeType == "video") {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    AUDIOS -> {
                        if (mimeType == "audio" || MimetypeUtils.extraAudioMimeType.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    DOCUMENTS -> {
                        if (mimeType == "text" || MimetypeUtils.extraDocumentMimeTypes.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    ARCHIVES -> {
                        if (MimetypeUtils.archivesMimetype.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    OTHERS -> {
                        if (mimeType != "image" && mimeType != "video" && mimeType != "audio" && mimeType != "text" &&
                            !MimetypeUtils.extraAudioMimeType.contains(fullMimeType) && !MimetypeUtils.archivesMimetype.contains(fullMimeType) &&
                            !MimetypeUtils.extraDocumentMimeTypes.contains(fullMimeType)
                        ) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
        return fileItems
    }
    override fun displayFiles() {
        binding.mimetypeRecycler.setHasFixedSize(true)
        binding.mimetypeRecycler.layoutManager = GridLayoutManager(mContext, 3)
        ensureBackgroundThread {
            try {
                val fileList = findFiles()
                fileAdapter.addAll(fileList)
                mActivity.runOnUiThread {
                    fileAdapter.setOnItemClick { item -> onFileClicked(item) }
                    binding.mimetypeRecycler.adapter = fileAdapter
                }
            } catch (e: Exception) { }
        }
    }
    private fun onFileClicked(item: FileItem) {
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
        private const val TYPE_ARG = "type_arg"
        fun newInstance(mimeType: String): MimeTypeFragment {
            return MimeTypeFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE_ARG, mimeType)
                }
            }
        }
    }
}