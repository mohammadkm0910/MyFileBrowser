package com.mohammadkk.myfilebrowser.fragment

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.anggrayudi.storage.file.DocumentFileCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mohammadkk.myfilebrowser.BuildConfig
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.isExternalStorageManager
import com.mohammadkk.myfilebrowser.extension.toast
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.helper.isRPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import com.mohammadkk.myfilebrowser.service.FileOperand
import java.io.File

abstract class BaseFragment : Fragment(), ServiceConnection {
    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var fileOperand: FileOperand? = null
    private lateinit var resultActivity : ActivityResultLauncher<Intent>

    protected val mContext: Context get() = requireContext()
    protected val mActivity: FragmentActivity get() = requireActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (requireContext().isExternalStorageManager()) {
                displayFiles()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val intentService = Intent(requireContext(), FileOperand::class.java)
        requireContext().startService(intentService)
        requireContext().bindService(intentService, this, Context.BIND_AUTO_CREATE)
    }
    override fun onStop() {
        super.onStop()
        requireContext().unbindService(this)
    }
    protected fun runTimePermission(rootView: View, callback:()->Unit) {
        if (requireContext().isExternalStorageManager()) {
            callback()
        } else {
            if (isRPlus()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.message_permission)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
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
                        dialog.dismiss()
                    }
                    .setCancelable(false).create().show()
            } else {
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                    val results = arrayListOf<Boolean>()
                    permissions.entries.forEach { results.add(it.value) }
                    if (results[0] && results[1]) {
                        callback()
                    } else {
                        Snackbar.make(rootView, R.string.message_permission_small , Snackbar.LENGTH_SHORT).setAction("Settings") {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                resultActivity.launch(this)
                            }
                        }.show()
                    }
                }.launch(storagePermissions)
            }
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
    protected fun deleteFileItem(item: FileItem, callback: (Boolean) -> Unit) {
        fileOperand?.deleteForceFile(item) { force ->
            callback(force)
        }
    }
    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        fileOperand = (p1 as FileOperand.LocalService).getService()
    }
    override fun onServiceDisconnected(p0: ComponentName?) {
        fileOperand = null
    }
    protected abstract fun displayFiles()
}