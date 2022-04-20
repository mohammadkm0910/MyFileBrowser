package com.mohammadkk.myfilebrowser.service

import android.app.Service
import android.content.Intent
import android.os.*
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.forceDelete
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File

class FileOperand : Service() {
    private val bindService: IBinder = LocalService()

    override fun onBind(p0: Intent?): IBinder {
        return bindService
    }
    fun deleteForceFile(item: FileItem, callback: (force: Boolean) -> Unit) {
        val ht = HandlerThread("delete_item")
        ht.start()
        val looper = ht.looper
        Handler(looper).post {
            runCatching {
                val force = deleteFileItem(item)
                Handler(Looper.getMainLooper()).post {
                    callback(force)
                    ht.quit()
                }
            }
        }
    }
    private fun deleteFileItem(item: FileItem): Boolean {
        val file = File(item.path)
        return if (isQPlus() && (item.isAndroidData() || item.isAndroidObb())) {
            val documentFile = DocumentFileCompat.fromFile(baseContext, file)
            documentFile?.forceDelete(baseContext) == true
        } else {
            deleteRecursively(file)
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
    inner class LocalService : Binder() {
        fun getService(): FileOperand {
            return this@FileOperand
        }
    }
}