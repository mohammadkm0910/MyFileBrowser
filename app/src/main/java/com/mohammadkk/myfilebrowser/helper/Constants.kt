@file:Suppress("SpellCheckingInspection")

package com.mohammadkk.myfilebrowser.helper

import android.os.Build
import android.os.Looper

fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
fun isQPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback:()->Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

val photoExtensions: Array<String> get() = arrayOf(".jpg", ".png", ".jpeg", ".bmp", ".webp", ".heic", ".heif", ".apng")
val videoExtensions: Array<String> get() = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".3gpp")
val audioExtensions: Array<String> get() = arrayOf(".mp3", ".wav", ".wma", ".ogg", ".m4a", ".opus", ".flac", ".aac")

val extraAudioMimeType = arrayListOf("application/ogg")

val extraDocsMimeType = arrayListOf(
    "application/msword",
    "application/vnd.ms-word.document.macroEnabled.12",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword",
    "application/vnd.ms-word.template.macroEnabled.12",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template"
)
val extraDocumentMimeTypes = arrayListOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/javascript"
)
const val INTENT_EXTRA_URI_NEW_API = "android.provider.extra.INITIAL_URI"
const val IMAGES = "images"
const val VIDEOS = "videos"
const val AUDIOS = "audios"
const val DOCUMENTS = "documents"
const val ARCHIVES = "archives"
const val OTHERS = "others"

enum class SystemNewApi {
    DATA, OBB, DATA_SD, OBB_SD
}