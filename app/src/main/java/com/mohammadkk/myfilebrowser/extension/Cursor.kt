package com.mohammadkk.myfilebrowser.extension

import android.database.Cursor

@Suppress("HasPlatformType")
fun Cursor.getStringValue(key: String) = getString(getColumnIndexOrThrow(key))
fun Cursor.getLongValue(key: String) = getLong(getColumnIndexOrThrow(key))