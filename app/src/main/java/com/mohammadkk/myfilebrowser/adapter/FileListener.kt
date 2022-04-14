package com.mohammadkk.myfilebrowser.adapter

import com.mohammadkk.myfilebrowser.model.FileItem
import java.io.File

interface FileListener {
    fun onOpenDetailDialog(item: FileItem, position: Int)
    fun onRenameFile(file: File, position: Int)
    fun onDeleteFile(item: FileItem, position: Int)
    fun onShareFile(item: FileItem, position: Int)
}