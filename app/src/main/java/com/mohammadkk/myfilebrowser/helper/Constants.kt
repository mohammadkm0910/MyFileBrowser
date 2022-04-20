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

val extraDocsMimeType = arrayListOf(
    "application/msword",
    "application/vnd.ms-word.document.macroEnabled.12",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword",
    "application/vnd.ms-word.template.macroEnabled.12",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template"
)

const val INTENT_EXTRA_URI_NEW_API = "android.provider.extra.INITIAL_URI"
const val IMAGES = "images"
const val VIDEOS = "videos"
const val AUDIOS = "audios"
const val DOCUMENTS = "documents"
const val ARCHIVES = "archives"
const val OTHERS = "others"

enum class SystemNewApi {
    DATA, OBB, DATA_SD, OBB_SD;
    companion object {
        fun getKey(enum: SystemNewApi) = when (enum) {
            DATA -> "android_data_key"
            OBB -> "android_obb_key"
            DATA_SD -> "android_data_sd_key"
            OBB_SD -> "android_obb_sd_key"
        }
    }
}