package com.mohammadkk.myfilebrowser.model

import android.content.Context
import com.anggrayudi.storage.file.DocumentFileCompat
import com.mohammadkk.myfilebrowser.extension.formatSize
import com.mohammadkk.myfilebrowser.extension.getCountChild
import com.mohammadkk.myfilebrowser.extension.providerUri
import com.mohammadkk.myfilebrowser.helper.isQPlus
import java.io.File
import java.io.Serializable

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
) : Serializable {
    fun parsUri(context: Context) = File(path).providerUri(context)
    fun isAndroidDataEnded() = path.endsWith("/Android/data")
    fun isAndroidObbEnded() = path.endsWith("/Android/obb")
    fun isAndroidData(): Boolean {
        val basePath = path.substringBefore( "/Android/data")
        return path.startsWith("$basePath/Android/data")
    }
    fun isAndroidObb(): Boolean {
        val basePath = path.substringBefore( "/Android/obb")
        return path.startsWith("$basePath/Android/obb")
    }
    fun systemDir() = when {
        isAndroidDataEnded() -> "Android/data"
        isAndroidObbEnded() -> "Android/obb"
        else -> ""
    }
    fun getSize(context: Context): String {
        val isSystem = (isAndroidData() || isAndroidObb()) && isQPlus()
        val file = File(path)
        return if (isSystem) {
            val documentFile = DocumentFileCompat.fromFile(context, file)
            if (documentFile?.isDirectory == true) {
                String.format("%d items", documentFile.listFiles().size)
            } else (documentFile?.length() ?: 0L).formatSize()
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