package com.mohammadkk.myfilebrowser.model

import android.content.Context
import com.anggrayudi.storage.file.DocumentFileCompat
import com.mohammadkk.myfilebrowser.extension.formatSize
import com.mohammadkk.myfilebrowser.extension.getCountChild
import com.mohammadkk.myfilebrowser.extension.isAndroidData
import com.mohammadkk.myfilebrowser.extension.isAndroidObb
import com.mohammadkk.myfilebrowser.helper.isQPlus
import java.io.File
import java.io.Serializable

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
) : Serializable {
    fun isExists() = File(path).exists()
    fun getSize(context: Context): String {
        val isSystem = (path.isAndroidData() || path.isAndroidObb()) && isQPlus()
        val file = File(path)
        return if (isSystem) {
            val documentFile = DocumentFileCompat.fromFile(context, file)
            if (isDirectory) {
                String.format("%d items", documentFile?.listFiles()?.size ?: 0)
            } else size.formatSize()
        } else {
            if (isDirectory) {
                String.format("%d items", file.getCountChild())
            } else file.length().formatSize()
        }
    }
    override fun toString(): String {
        return "FileItem(name= $name, path=$path, isDirectory=$isDirectory, size: $size)"
    }
}