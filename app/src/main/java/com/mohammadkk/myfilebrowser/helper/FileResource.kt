package com.mohammadkk.myfilebrowser.helper

import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.extension

class FileResource(private val path: String) {
    private fun imageResource(): HashMap<String, Int> {
        return hashMapOf(
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
    }
    fun getMusicIcon(): Int {
        return if (path.extension == "mp3") R.drawable.ic_mp3 else R.drawable.ic_music
    }
    fun getImages(): Int? {
        val map = imageResource()
        return map[path.extension]
    }
}