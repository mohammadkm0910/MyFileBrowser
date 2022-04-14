package com.mohammadkk.myfilebrowser.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

data class HomeItems(
    @StringRes val name: Int,
    val icon: Int,
    @ColorRes val fillColor: Int,
    val tag: String
)
