package com.mohammadkk.myfilebrowser.extension

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

fun Long.formatSize(isThousand: Boolean = true): String {
    if (this <= 0) return String.format("%d B", 0)
    val base = if (isThousand) 1000.0 else 1024.0
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val index = (log10(toDouble()) / log10(base)).toInt()
    val df = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.getDefault()))
    return "${df.format(this / base.pow(index.toDouble()))} ${units[index]}"
}
fun Long.formatDate(locale: Locale = Locale.getDefault()): String {
    val simpleFormatter = SimpleDateFormat("dd/mm/yyyy HH:mm:ss", locale)
    return simpleFormatter.format(Date(this))
}