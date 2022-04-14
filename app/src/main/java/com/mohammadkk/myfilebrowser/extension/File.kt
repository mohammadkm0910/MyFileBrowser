package com.mohammadkk.myfilebrowser.extension

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.mohammadkk.myfilebrowser.BuildConfig
import com.mohammadkk.myfilebrowser.helper.isNougatPlus
import com.mohammadkk.myfilebrowser.helper.isQPlus
import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File
import java.util.*

fun File.getCountChild(): Int {
    val listFiles = listFiles()?.filter {
        !it.name.startsWith('.')
    }
    return listFiles?.size ?: 0
}
fun File.getPropSize(context: Context): Long {
    val isAndroid = isQPlus() && absolutePath.endsWith("/Android")
    val isSystem = isQPlus() && (absolutePath.contains("/Android/data") || absolutePath.contains("/Android/obb"))
    return when {
        isAndroid -> {
            val androidDataSize = getFileSize(DocumentFileCompat.fromFile(context, File(absolutePath, "data")))
            val androidDataObb = getFileSize(DocumentFileCompat.fromFile(context, File(absolutePath, "obb")))
            getFileSize(this) + androidDataObb + androidDataSize
        }
        isSystem -> getFileSize(DocumentFileCompat.fromFile(context, this))
        else -> getFileSize(this)
    }
}
private fun getFileSize(file: File?): Long {
    var result = 0L
    if (file == null || !file.exists()) return 0L
    if (!file.isDirectory) return file.length()
    val dirList: Stack<File> = Stack()
    dirList.clear()
    dirList.push(file)
    while (!dirList.isEmpty()) {
        val dirCurrent = dirList.pop()
        dirCurrent.listFiles()?.mapNotNull { f ->
            if (f.isDirectory) {
                dirList.push(f)
            } else result += f.length()
        }
    }
    return result
}
private fun getFileSize(file: DocumentFile?): Long {
    var result = 0L
    if (file == null || !file.exists()) return 0L
    if (!file.isDirectory) return file.length()
    val dirList: Stack<DocumentFile> = Stack()
    dirList.clear()
    dirList.push(file)
    while (!dirList.isEmpty()) {
        val dirCurrent = dirList.pop()
        dirCurrent.listFiles().mapNotNull { f ->
            if (f.isDirectory) {
                dirList.push(f)
            } else result += f.length()
        }
    }
    return result
}
val File.mimetype : String
    get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

fun File.toFileItem(): FileItem {
    return FileItem(this.name, this.absolutePath, this.isDirectory, this.length())
}
fun File.providerUri(context: Context): Uri {
    return if (isNougatPlus()) {
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", this)
    } else Uri.fromFile(this)
}
fun File.renameTo(context: Context, newFile: File): Boolean {
    var rename = renameTo(newFile)
    if (!rename) {
        val docFile = DocumentFileCompat.fromFile(context, this)
        rename = docFile?.renameTo(newFile.name) == true
    }
    return rename
}