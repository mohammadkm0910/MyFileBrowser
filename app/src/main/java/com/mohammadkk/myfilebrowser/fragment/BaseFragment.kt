package com.mohammadkk.myfilebrowser.fragment

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.anggrayudi.storage.file.DocumentFileCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mohammadkk.myfilebrowser.BuildConfig
import com.mohammadkk.myfilebrowser.databinding.DialogPermissionBinding
import com.mohammadkk.myfilebrowser.extension.deleteDocument
import com.mohammadkk.myfilebrowser.extension.hasPermission
import com.mohammadkk.myfilebrowser.extension.toast
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.helper.isRPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File

abstract class BaseFragment : Fragment() {
    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private lateinit var resultActivity : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRPlus() && Environment.isExternalStorageManager()) {
                displayFiles()
            } else if (storagePermissions.any { requireContext().hasPermission(it) }) {
                displayFiles()
            }
        }
    }
    protected fun runTimePermission(rootView: View, callback:()->Unit) {
        if (isRPlus()) {
            if (Environment.isExternalStorageManager()) {
                callback()
            } else {
                val dialogView = DialogPermissionBinding.inflate(layoutInflater)
                val dialogPermission = MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogView.root)
                    .setCancelable(false)
                    .create()
                dialogPermission.show()
                dialogView.btnAllowed.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse("package:${requireContext().packageName}")
                        resultActivity.launch(intent)
                    } catch (e: Exception) {
                        requireContext().toast(e.message.toString())
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        resultActivity.launch(intent)
                    }
                    dialogPermission.dismiss()
                }
                dialogView.btnDenied.setOnClickListener { dialogPermission.dismiss() }
            }
        } else if (storagePermissions.any { requireContext().hasPermission(it) }) {
            callback()
        } else {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val results = arrayListOf<Boolean>()
                permissions.entries.forEach { results.add(it.value) }
                if (results[0] && results[1]) {
                    callback()
                } else {
                    Snackbar.make(rootView, "Permission Require" , Snackbar.LENGTH_SHORT).setAction("Settings") {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            resultActivity.launch(this)
                        }
                    }.show()
                }
            }.launch(storagePermissions)
        }
    }
    protected fun uriByFileItem(item: FileItem): Uri {
        var uri = item.parsUri(requireContext())
        if (isQPlus() && (item.isAndroidData() || item.isAndroidObb())) {
            val docFile = DocumentFileCompat.fromFile(requireContext(), File(item.path))
            if (docFile != null) uri = docFile.uri
        }
        return uri
    }
    protected fun deleteFileItem(item: FileItem): Boolean {
        val file = File(item.path)
        return try {
            if (isQPlus() && (item.isAndroidData() || item.isAndroidObb())) {
                val documentFile = DocumentFileCompat.fromFile(requireContext(), file)
                requireContext().deleteDocument(documentFile)
            } else {
                deleteRecursively(file)
            }
        } catch (e: Exception) {
            false
        }
    }
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            val files = file.listFiles() ?: return file.delete()
            for (child in files) {
                deleteRecursively(child)
            }
        }
        return file.delete()
    }
    protected abstract fun displayFiles()
}