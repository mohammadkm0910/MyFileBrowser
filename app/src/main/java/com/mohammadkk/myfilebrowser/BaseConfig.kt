package com.mohammadkk.myfilebrowser

import android.content.Context
import androidx.preference.PreferenceManager

class BaseConfig(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var androidData: String
        get() = prefs.getString("android_data_key", "") ?: ""
        set(value) = prefs.edit().putString("android_data_key", value).apply()
    var androidObb: String
        get() = prefs.getString("android_obb_key", "") ?: ""
        set(value) = prefs.edit().putString("android_obb_key", value).apply()
    var androidDataSd: String
        get() = prefs.getString("android_data_sd_key", "") ?: ""
        set(value) = prefs.edit().putString("android_data_sd_key", value).apply()
    var androidObbSd: String
        get() = prefs.getString("android_obb_sd_key", "") ?: ""
        set(value) = prefs.edit().putString("android_obb_sd_key", value).apply()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }
}