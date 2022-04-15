package com.mohammadkk.myfilebrowser

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.mohammadkk.myfilebrowser.helper.SystemNewApi

class BaseConfig(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getUriPath(enum: SystemNewApi?): Uri? {
        val key = SystemNewApi.getKey(enum ?: return null)
        return Uri.parse(prefs.getString(key, ""))
    }
    fun setUriPath(enum: SystemNewApi, uri: Uri) {
        val key = SystemNewApi.getKey(enum)
        prefs.edit().putString(key, uri.toString()).apply()
    }
    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }
}