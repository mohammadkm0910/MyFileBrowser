package com.mohammadkk.myfilebrowser.helper

import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.extension

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
    fun getImageResource(extension: String): Int? {
        val mapImages = hashMapOf(
            "ai" to R.drawable.ic_ai,
            "avi" to R.drawable.ic_avi,
            "css" to R.drawable.ic_css,
            "csv" to R.drawable.ic_csv,
            "dbf" to R.drawable.ic_dbf,
            "dll" to R.drawable.ic_dll,
            "doc" to R.drawable.ic_doc,
            "docx" to R.drawable.ic_docx,
            "dwg" to R.drawable.ic_dwg,
            "exe" to R.drawable.ic_exe,
            "file" to R.drawable.ic_file,
            "fla" to R.drawable.ic_fla,
            "html" to R.drawable.ic_html,
            "iso" to R.drawable.ic_iso,
            "jar" to R.drawable.ic_jar,
            "jpeg" to R.drawable.ic_jpeg,
            "jpg" to R.drawable.ic_jpg,
            "js" to R.drawable.ic_js,
            "json" to R.drawable.ic_json,
            "mp3" to R.drawable.ic_mp3,
            "mp4" to R.drawable.ic_mp4,
            "pdf" to R.drawable.ic_pdf,
            "php" to R.drawable.ic_php,
            "png" to R.drawable.ic_png,
            "ppt" to R.drawable.ic_ppt,
            "psd" to R.drawable.ic_psd,
            "rar" to R.drawable.ic_rar,
            "rtf" to R.drawable.ic_rtf,
            "svg" to R.drawable.ic_svg,
            "txt" to R.drawable.ic_txt,
            "xls" to R.drawable.ic_xls,
            "xlsx" to R.drawable.ic_xlsx,
            "xml" to R.drawable.ic_xml,
            "zip" to R.drawable.ic_zip
        )
        return mapImages[extension]
    }
    fun getMusicIcon(path: String): Int {
        return if (path.extension == "mp3") R.drawable.ic_mp3 else R.drawable.ic_music
    }
}