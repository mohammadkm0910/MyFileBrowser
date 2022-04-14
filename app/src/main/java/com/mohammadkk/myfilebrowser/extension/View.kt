package com.mohammadkk.myfilebrowser.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.*

@SuppressLint("RtlHardcoded")
fun TextView.fixGravity() {
    val isRTL = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL
    gravity = if (isRTL) Gravity.RIGHT else Gravity.LEFT
}
fun TextView.fixSetText(text: String) {
    this.fixGravity()
    this.text = text
}
fun TextInputEditText.setDisableKeyboard() {
    showSoftInputOnFocus = false
    isCursorVisible = false
}
fun Activity.snackBar(message: String?, time: Int = Snackbar.LENGTH_SHORT) {
    val view = window?.decorView?.findViewById<View>(android.R.id.content)
    Snackbar.make(view ?: return, message ?: "null", time).show()
}