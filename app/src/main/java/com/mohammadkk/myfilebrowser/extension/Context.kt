package com.mohammadkk.myfilebrowser.extension

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.anggrayudi.storage.file.DocumentFileCompat
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.helper.*
import java.io.File

fun FragmentActivity.navigate(fragment: Fragment, isBack: Boolean = false) {
    val ft = supportFragmentManager.beginTransaction()
    val tag = fragment.tag ?: fragment.toString()
    ft.replace(R.id.fragmentContainer, fragment, tag)
    if (isBack) ft.addToBackStack(tag)
    ft.commit()
}
val Context.externalStorage: File
    get() {
        var external = Environment.getExternalStorageDirectory()
        if (external == null) {
            val dir = getExternalFilesDirs(null)[0]
            var path = dir.absolutePath
            path = path.substring(0, path.indexOf("Android/data"))
            path = path.trimEnd('/')
            external = File(path)
        }
        return external
    }
val Context.sdcardStorage: File?
    get() {
        var base: String? = null
        for (file in externalCacheDirs) {
            if (Environment.isExternalStorageRemovable(file)) {
                base = file.path.split("/Android")[0]
                break
            }
        }
        return if (base != null) File(base) else null
    }
fun Context.isPathPermission(uri: Uri?): Boolean {
    val listUri = arrayListOf<Uri>()
    contentResolver.persistedUriPermissions.forEach {
        if (it.isWritePermission && it.isReadPermission) {
            listUri.add(it.uri)
        }
    }
    return uri != null && !listUri.isNullOrEmpty() && listUri.contains(uri)
}
fun Context.getEnumSystemNewApi(path: String): SystemNewApi? {
    val root = compareStorage(path)[1]
    if (path.isAndroidDataEnded()) {
        if (root == "0") {
            return SystemNewApi.DATA
        } else if (root == "1") {
            return SystemNewApi.DATA_SD
        }
    } else if (path.isAndroidObbEnded()) {
        if (root == "0") {
            return SystemNewApi.OBB
        } else if (root == "1") {
            return SystemNewApi.OBB_SD
        }
    }
    return null
}
@RequiresApi(Build.VERSION_CODES.Q)
fun Context.askPermission(target: String, fullPath: String): Intent {
    val storageId = DocumentFileCompat.getStorageId(this, fullPath)
    val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
    var targetDirectory = target.trim('/')
    targetDirectory = targetDirectory.replace("/", "%2F")
    val uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI")
    var scheme = uri.toString()
    scheme = scheme.replace("/root/", "/document/")
    scheme = scheme.replace("/document/primary", "/document/$storageId")
    scheme += "%3A$targetDirectory"
    intent.putExtra(INTENT_EXTRA_URI_NEW_API, Uri.parse(scheme))
    return intent
}
fun Context.compareStorage(path: String): Array<String> {
    val internal = externalStorage.absolutePath
    val external = sdcardStorage?.absolutePath ?: return arrayOf(internal, "0")
    if (path.startsWith(internal)) {
        return arrayOf(internal, "0")
    } else if (path.startsWith(external)) {
        return arrayOf(external, "1")
    }
    return arrayOf("", "")
}
fun Context.getInternalStoragePublicDirectory(type: String): File {
    val internal = externalStorage
    return File(internal.absolutePath + "/" + type)
}
fun Context.hasPermission(permission: String): Boolean {
    if (!isMarshmallowPlus()) return true
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
fun Context.isExternalStorageManager(): Boolean {
    return if (isRPlus()) {
        Environment.isExternalStorageManager()
    } else {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
fun Context.toast(@StringRes id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}
fun Context.toast(msg: String?, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg ?: "null", length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg ?: "null", length)
            }
        }
    } catch (e: Exception) {}
}
private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else Toast.makeText(context, message, length).show()
}
fun Context.mColor(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}
fun Context.launchRequireIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showError: Boolean = false,
    callback:(cursor:Cursor)->Unit
) {
    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
    try {
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showError) {
            e.printStackTrace()
        }
    }
}
fun Context.deleteFileMediaStore(path: String) {
    try {
        MediaScannerConnection.scanFile(this, arrayOf(path), null
        ) { _, uri ->
            if (uri != null) {
                val where = "${MediaColumns.DATA} = ?"
                val args = arrayOf(path)
                contentResolver.delete(uri, where, args)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.updateFileMediaStore(oldPath: String, newPath: String) {
    ensureBackgroundThread {
        val values = ContentValues().apply {
            put(MediaColumns.DATA, newPath)
            put(MediaColumns.DISPLAY_NAME, newPath.substringAfterLast("/"))
            put(MediaColumns.TITLE, newPath.substringAfterLast("/"))
        }
        val uri = getFileUri(oldPath)
        val selection = "${MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(oldPath)

        try {
            contentResolver.update(uri, values, selection, selectionArgs)
        } catch (ignored: Exception) {
        }
    }
}
fun Context.rescanPaths(paths: Array<String>, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) {
        callback?.invoke()
        return
    }
    var cnt = paths.size
    MediaScannerConnection.scanFile(applicationContext, paths, null) { _, _ ->
        if (--cnt == 0) callback?.invoke()
    }
}
private fun getFileUri(path: String) = when {
    path.isImageSlow() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    path.isVideoSlow() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    path.isAudioSlow() -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Files.getContentUri("external")
}