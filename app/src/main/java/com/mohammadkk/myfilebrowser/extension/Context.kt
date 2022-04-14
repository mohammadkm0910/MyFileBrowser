package com.mohammadkk.myfilebrowser.extension

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
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
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.deleteRecursively
import com.mohammadkk.myfilebrowser.BaseConfig
import com.mohammadkk.myfilebrowser.R
import com.mohammadkk.myfilebrowser.helper.INTENT_EXTRA_URI_NEW_API
import com.mohammadkk.myfilebrowser.helper.ensureBackgroundThread
import com.mohammadkk.myfilebrowser.helper.isMarshmallowPlus
import com.mohammadkk.myfilebrowser.helper.isOnMainThread
import java.io.File
import kotlin.math.roundToInt

fun FragmentActivity.navigate(fragment: Fragment, isBack: Boolean = false) {
    val ft = supportFragmentManager.beginTransaction()
    ft.replace(R.id.fragmentContainer, fragment, fragment.tag ?: fragment.toString())
    if (isBack) ft.addToBackStack(fragment.toString())
    ft.commit()
}
fun Context.getInternalStorage(): File {
    val dir = getExternalFilesDirs(null)[0]
    var path = dir.absolutePath
    path = path.substring(0, path.indexOf("Android/data"))
    path = path.trimEnd('/')
    return File(path)
}
fun Context.getExternalStorage(): File? {
    val selected = arrayListOf<String?>()
    selected.add(0, null)
    for (file in externalCacheDirs) {
        if (Environment.isExternalStorageRemovable(file)) {
            selected[0] = file.path.split("/Android")[0]
            break
        }
    }
    val path = selected[0]
    return if (path != null) File(path) else null
}
val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)
fun Context.setPermissionTreeUri(treeUri: Uri) {
    contentResolver.takePersistableUriPermission(
        treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
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
    val internal = getInternalStorage().absolutePath
    val external = getExternalStorage()?.absolutePath ?: return arrayOf(internal, "0")
    if (path.startsWith(internal)) {
        return arrayOf(internal, "0")
    } else if (path.startsWith(external)) {
        return arrayOf(external, "1")
    }
    return arrayOf("", "")
}
fun Context.deleteDocument(documentFile: DocumentFile?): Boolean {
    if (documentFile?.isDirectory == true) {
        return documentFile.deleteRecursively(this)
    } else if (documentFile?.isFile == true) {
        return documentFile.delete()
    }
    return false
}
fun Context.getInternalStoragePublicDirectory(type: String): File {
    val internal = getInternalStorage()
    val path = internal.absolutePath + "/" + type
    return File(path)
}
fun Context.hasPermission(permission: String): Boolean {
    if (!isMarshmallowPlus()) return true
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
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
@ColorInt
fun Context.getColorOrAlpha(@ColorRes id: Int,  factor: Float = 0.5f): Int {
    val color = ContextCompat.getColor(this, id)
    val alpha = (Color.alpha(color) * factor).roundToInt()
    val red: Int = Color.red(color)
    val green: Int = Color.green(color)
    val blue: Int = Color.blue(color)
    return Color.argb(alpha, red, green, blue)
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
fun Context.rescanPaths(paths: List<String>, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) {
        callback?.invoke()
        return
    }
    var cnt = paths.size
    MediaScannerConnection.scanFile(applicationContext, paths.toTypedArray(), null) { _, _ ->
        if (--cnt == 0) {
            callback?.invoke()
        }
    }
}
@Suppress("HasPlatformType")
private fun getFileUri(path: String) = when {
    path.isImageSlow() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    path.isVideoSlow() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    path.isAudioSlow() -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Files.getContentUri("external")
}