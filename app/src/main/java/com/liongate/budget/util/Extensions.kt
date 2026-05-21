package com.liongate.budget.util

import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import java.text.NumberFormat
import java.util.Locale

fun Double.formatCurrency(): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return "NT$ ${format.format(this)}"
}

fun Double.formatAmount(): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 2
    return format.format(this)
}

fun String.toComposeColor(): ComposeColor {
    return try {
        ComposeColor(Color.parseColor(this))
    } catch (e: Exception) {
        ComposeColor.Gray
    }
}

fun String.toAndroidColor(): Int {
    return try {
        Color.parseColor(this)
    } catch (e: Exception) {
        Color.GRAY
    }
}

fun List<*>.isNotEmpty() = this.isNotEmpty()

fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}
