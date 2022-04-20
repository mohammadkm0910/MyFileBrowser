package com.mohammadkk.myfilebrowser.helper

object MimetypeUtils {
    val archivesMimetype = arrayListOf(
        "application/x-7z-compressed",
        "application/x-bzip2",
        "application/gzip",
        "application/java-archive",
        "application/rar",
        "application/x-tar",
        "application/x-xz",
        "application/zip"
    )
    val extraAudioMimeType = arrayListOf("application/ogg")
    val extraDocumentMimeTypes = arrayListOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/javascript"
    )
}