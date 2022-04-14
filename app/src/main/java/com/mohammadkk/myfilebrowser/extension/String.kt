package com.mohammadkk.myfilebrowser.extension

import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.mohammadkk.myfilebrowser.helper.audioExtensions
import com.mohammadkk.myfilebrowser.helper.photoExtensions
import com.mohammadkk.myfilebrowser.helper.videoExtensions

val String.extension: String
    get() = substringAfterLast('.')

val String.mimetype: String
    get() {
        val extension = substringAfterLast('.')
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
val String.isEmpty: Boolean get() = trim().isEmpty()
fun String.isAValidFilename() : Boolean {
    val chars = charArrayOf('/', '\n', '\r', '\t', '\u0000', '`', '?', '*', '\\', '<', '>', '|', '\"', ':')
    chars.forEach {
        if (contains(it))
            return false
    }
    return true
}
fun String.isImageFast() = photoExtensions.any { endsWith(it, true) }
fun String.isVideoFast() = videoExtensions.any { endsWith(it, true) }
fun String.isAudioFast() = audioExtensions.any { endsWith(it, true) }

fun String.isImageSlow() = isImageFast() || mimetype.startsWith("image") || startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())
fun String.isVideoSlow() = isVideoFast() || mimetype.startsWith("video") || startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())
fun String.isAudioSlow() = isAudioFast() || mimetype.startsWith("audio") || startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())