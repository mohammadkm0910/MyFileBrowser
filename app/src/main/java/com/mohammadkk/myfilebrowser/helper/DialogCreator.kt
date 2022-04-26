package com.mohammadkk.myfilebrowser.helper

import android.app.Activity
import android.app.Dialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.databinding.DialogCreateBinding
import com.mohammadkk.myfilebrowser.databinding.DialogDetailsBinding
import com.mohammadkk.myfilebrowser.databinding.DialogRenameBinding
import com.mohammadkk.myfilebrowser.extension.formatDate
import com.mohammadkk.myfilebrowser.extension.formatSize
import com.mohammadkk.myfilebrowser.extension.getPropSize
import com.mohammadkk.myfilebrowser.extension.setDisableKeyboard
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class DialogCreator(private val activity: Activity) {
    fun detailDialog(item: FileItem) {
        val file = File(item.path)
        val detailDialog = MaterialAlertDialogBuilder(activity)
        detailDialog.setTitle("Details: ")
        val view = DialogDetailsBinding.inflate(activity.layoutInflater)
        view.displayFileName.setDisableKeyboard()
        view.displayParentFolder.setDisableKeyboard()
        view.displayPathFile.setDisableKeyboard()
        view.displayFileSize.setDisableKeyboard()
        view.displayFileLastModified.setDisableKeyboard()
        view.displayFileName.setText(file.name)
        view.displayParentFolder.setText(file.parent ?: "unknown parent")
        view.displayPathFile.setText(file.absolutePath)
        view.displayFileLastModified.setText(file.lastModified().formatDate(Locale.ENGLISH))
        val executor = Executors.newSingleThreadExecutor()
        view.displayFileSize.setText("...")
        view.displayFileSize.setTextColor(ContextCompat.getColor(activity, R.color.blue_500))
        executor.execute {
            runCatching {
                val size = file.getPropSize(activity)
                activity.runOnUiThread {
                    view.displayFileSize.setText(size.formatSize())
                    view.displayFileSize.setTextColor(ContextCompat.getColor(activity, R.color.grey_900))
                    executor.shutdown()
                }
            }
        }
        detailDialog.setView(view.root)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
        detailDialog.create().show()
    }
    fun deleteDialog(fileName: String, callback: ()->Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Delete $fileName ?")
            .setPositiveButton("Yes") { dialog, _ ->
                callback()
                dialog.dismiss()
            }
            .setNegativeButton("No") {dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    fun renameDialog(name: String, callback: (output: String, dialog: Dialog) -> Unit) {
        val alert = MaterialAlertDialogBuilder(activity)
        val binding = DialogRenameBinding.inflate(activity.layoutInflater)
        binding.editName.setHint(R.string.enter_name)
        binding.editName.setText(name)
        binding.btnOkDialog.setText(android.R.string.ok)
        alert.setView(binding.root)
        val dialog = alert.create()
        binding.editName.addTextChangedListener {
            val newName = it.toString()
            if (name != newName && newName.isNotEmpty()) {
                binding.btnOkDialog.isEnabled = true
                binding.btnOkDialog.alpha = 1.0f
            } else {
                binding.btnOkDialog.isEnabled = false
                binding.btnOkDialog.alpha = 0.5f
            }
        }
        binding.btnOkDialog.setOnClickListener {
            callback(binding.editName.editableText.toString(), dialog)
        }
        binding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
    fun createNewInStorageDialog(isFile: Boolean, callback: (output: String, dialog: Dialog) -> Unit) {
        val alert = MaterialAlertDialogBuilder(activity)
        val binding = DialogCreateBinding.inflate(activity.layoutInflater)
        binding.editName.setHint(R.string.enter_name)
        if (isFile) {
            alert.setTitle(R.string.create_new_file)
        } else {
            alert.setTitle(R.string.create_new_folder)
        }
        alert.setView(binding.root)
        val dialog = alert.create()
        binding.btnOkDialog.setOnClickListener {
            callback(binding.editName.editableText.toString(), dialog)
        }
        binding.btnCancelDialog.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}