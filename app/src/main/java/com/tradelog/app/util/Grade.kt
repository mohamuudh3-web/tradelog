package com.tradelog.app.util

/** Letter grade for a trade/backtest based on how many confirmation rules were met. */
object Grade {
    fun of(checked: Int, total: Int): String {
        if (total <= 0) return "—"
        val ratio = checked.toFloat() / total
        return when {
            ratio >= 1.0f -> "A+"
            ratio >= 0.8f -> "A"
            ratio >= 0.6f -> "B"
            ratio >= 0.4f -> "C"
            ratio >= 0.2f -> "D"
            else -> "F"
        }
    }
}
