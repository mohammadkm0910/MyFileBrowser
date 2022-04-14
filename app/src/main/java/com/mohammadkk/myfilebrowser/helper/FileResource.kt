package com.mohammadkk.myfilebrowser.helper

import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.extension.extension

class FileResource(private val path: String) {
    private fun imageResource(): HashMap<String, Int> {
        return hashMapOf<String, Int>().apply {
            put("ai", R.drawable.ic_ai)
            put("avi", R.drawable.ic_avi)
            put("css", R.drawable.ic_css)
            put("csv", R.drawable.ic_csv)
            put("dbf", R.drawable.ic_dbf)
            put("dll", R.drawable.ic_dll)
            put("doc", R.drawable.ic_doc)
            put("docx", R.drawable.ic_docx)
            put("dwg", R.drawable.ic_dwg)
            put("exe", R.drawable.ic_exe)
            put("fla", R.drawable.ic_fla)
            put("html", R.drawable.ic_html)
            put("iso", R.drawable.ic_iso)
            put("jar", R.drawable.ic_jar)
            put("jpg", R.drawable.ic_jpg)
            put("jpeg", R.drawable.ic_jpeg)
            put("js", R.drawable.ic_js)
            put("json", R.drawable.ic_json)
            put("mp3", R.drawable.ic_mp3)
            put("mp4", R.drawable.ic_mp4)
            put("pdf", R.drawable.ic_pdf)
            put("php", R.drawable.ic_php)
            put("png", R.drawable.ic_png)
            put("ppt", R.drawable.ic_ppt)
            put("psd", R.drawable.ic_psd)
            put("rar", R.drawable.ic_rar)
            put("rtf", R.drawable.ic_rtf)
            put("svg", R.drawable.ic_svg)
            put("txt", R.drawable.ic_txt)
            put("xls", R.drawable.ic_xls)
            put("xlsx", R.drawable.ic_xlsx)
            put("xml", R.drawable.ic_xml)
            put("zip", R.drawable.ic_zip)
        }
    }
    fun getMusicIcon(): Int {
        return if (path.extension == "mp3") R.drawable.ic_mp3 else R.drawable.ic_music
    }
    fun invalidExtension(): Boolean {
        val map = imageResource()
        val key = map.keys
        return key.any { it ==  path.substringAfterLast('.') }
    }
    fun getImages(): Int? {
        val map = imageResource()
        return map[path.substringAfterLast('.')]
    }
}