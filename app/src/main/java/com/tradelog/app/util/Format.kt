package com.tradelog.app.util

import kotlin.math.abs

object Format {
    fun money(amount: Double, currency: String = "USD"): String {
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> ""
        }
        val sign = if (amount < 0) "-" else ""
        val abs = abs(amount)
        val body = if (symbol.isNotEmpty()) "$symbol${twoDp(abs)}" else "${twoDp(abs)} ${currency.uppercase()}"
        return "$sign$body"
    }

    fun signedMoney(amount: Double, currency: String = "USD"): String {
        val s = money(amount, currency)
        return if (amount > 0) "+$s" else s
    }

    fun twoDp(v: Double): String = String.format("%,.2f", v)

    fun percent(fraction: Double): String = "${String.format("%.1f", fraction * 100)}%"

    fun rMultiple(r: Double?): String = if (r == null) "—" else "${if (r >= 0) "+" else ""}${String.format("%.2f", r)}R"

    fun profitFactor(pf: Double): String = when {
        pf.isInfinite() -> "∞"
        pf == 0.0 -> "—"
        else -> String.format("%.2f", pf)
    }
}
