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
import com.mohammadkk.myfilebrowser.helper.MimetypeUtils.archivesMimetype
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
        fileAdapter = FileAdapter(requireActivity(), true, this)
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
        requireContext().queryCursor(uri, projection) { cursor ->
            try {
                val fullMimeType = cursor.getStringValue(Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault()) ?:return@queryCursor
                val name = cursor.getStringValue(Files.FileColumns.DISPLAY_NAME)
                if (name.startsWith(".")) return@queryCursor
                val size = cursor.getLongValue(Files.FileColumns.SIZE)
                if (size == 0L) return@queryCursor
                val path = cursor.getStringValue(Files.FileColumns.DATA)
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
                        if (mimeType == "audio" || extraAudioMimeType.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    DOCUMENTS -> {
                        if (mimeType == "text" || extraDocumentMimeTypes.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    ARCHIVES -> {
                        if (archivesMimetype.contains(fullMimeType)) {
                            fileItems.add(FileItem(name, path, false, size))
                        }
                    }
                    OTHERS -> {
                        if (mimeType != "image" && mimeType != "video" && mimeType != "audio" && mimeType != "text" &&
                            !extraAudioMimeType.contains(fullMimeType) && !extraDocumentMimeTypes.contains(fullMimeType) &&
                            !archivesMimetype.contains(fullMimeType)
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
        binding.mimetypeRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        ensureBackgroundThread {
            try {
                val fileList = findFiles()
                fileAdapter.addAll(fileList)
                requireActivity().runOnUiThread {
                    fileAdapter.setOnItemClick { item -> onFileClicked(item) }
                    binding.mimetypeRecycler.adapter = fileAdapter
                }
            } catch (e: Exception) { }
        }
    }
    private fun onFileClicked(item: FileItem) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uriByFileItem(item), item.path.mimetype)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            requireContext().launchRequireIntent(this)
        }
    }
    override fun onOpenDetailDialog(item: FileItem, position: Int) {
        dialogCreator.detailDialog(item)
    }
    override fun onRenameFile(file: File, position: Int) {
        dialogCreator.renameDialog(file.name, false) { output, dialog ->
            var newName = output
            val edtExtension = output.substringAfterLast('.')
            val extension = file.extension
            if (edtExtension.isEmpty()) newName += if (extension.isNotEmpty()) ".$extension" else ""
            val newFile = File(file.parent?:return@renameDialog, output)
            if (file.renameTo(requireContext(), newFile)) {
                fileAdapter.updateItemAt(position, newFile)
                requireContext().rescanPaths(arrayListOf(file.absolutePath, newFile.absolutePath)) {
                    requireContext().updateFileMediaStore(file.absolutePath, newFile.absolutePath)
                }
                fileAdapter.refresh()
                requireContext().toast("renamed!!")
            } else {
                requireContext().toast("Couldn't Renamed!")
            }
            dialog.dismiss()
        }
    }
    override fun onDeleteFile(item: FileItem, position: Int) {
        dialogCreator.deleteDialog(item.name) {
            if (deleteFileItem(item)) {
                requireContext().deleteFileMediaStore(item.path)
                fileAdapter.removeItemAt(position)
                fileAdapter.refresh()
                requireContext().toast("Deleted!")
            } else requireContext().toast("Couldn't Deleted!")
        }
    }
    override fun onShareFile(item: FileItem, position: Int) {
        Intent().apply {
            action = Intent.ACTION_SEND
            type = item.path.mimetype
            putExtra(Intent.EXTRA_STREAM, uriByFileItem(item))
            requireContext().launchRequireIntent(
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