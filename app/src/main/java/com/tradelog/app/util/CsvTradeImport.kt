package com.tradelog.app.util

import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Parses a broker trade-history CSV (cTrader / MT5 deal export and similar) into [Trade]s.
 * Columns are matched by header keyword so it tolerates different broker layouts.
 */
object CsvTradeImport {

    data class Result(val trades: List<Trade>, val skipped: Int, val error: String?)

    fun parse(text: String): Result {
        val lines = text.split(Regex("\r?\n")).filter { it.isNotBlank() }
        if (lines.size < 2) return Result(emptyList(), 0, "File has no data rows.")
        val delim = detectDelimiter(lines[0])
        val header = splitLine(lines[0], delim).map { it.trim().trim('"').lowercase() }

        fun col(vararg keys: String) = header.indexOfFirst { h -> keys.any { h.contains(it) } }
        val iSym = col("symbol", "instrument", "pair")
        val iType = col("type", "direction", "side", "action")
        val iLots = col("lots", "volume", "size", "quantity", "units")
        val iOpen = col("open price", "openprice", "entry", "open")
        val iClose = col("close price", "closeprice", "exit", "close")
        val iPnl = col("profit", "p/l", "pnl", "net", "gross")
        val iTime = col("open time", "opentime", "time", "date")

        if (iSym < 0 || iPnl < 0) {
            return Result(emptyList(), 0, "Couldn't find a Symbol and a Profit/PnL column. Header was: ${header.joinToString(", ")}")
        }

        val trades = mutableListOf<Trade>()
        var skipped = 0
        for (i in 1 until lines.size) {
            val cells = splitLine(lines[i], delim)
            val sym = cells.getOrNull(iSym)?.trim()?.trim('"')?.uppercase().orEmpty()
            if (sym.isBlank() || sym == "BALANCE" || sym == "CREDIT") { skipped++; continue }
            val pnl = cells.getOrNull(iPnl).num()
            val dir = cells.getOrNull(iType)?.lowercase().orEmpty()
            val direction = if (dir.contains("sell") || dir.contains("short")) Direction.SHORT else Direction.LONG
            val result = when {
                pnl > 0.0 -> TradeResult.WIN
                pnl < 0.0 -> TradeResult.LOSS
                else -> TradeResult.BREAKEVEN
            }
            val opened = if (iTime >= 0) parseTime(cells.getOrNull(iTime)) else 0L
            trades += Trade(
                instrument = sym,
                direction = direction,
                entryPrice = if (iOpen >= 0) cells.getOrNull(iOpen).num() else 0.0,
                exitPrice = if (iClose >= 0) cells.getOrNull(iClose).num() else null,
                lotSize = if (iLots >= 0) cells.getOrNull(iLots).num() else 0.0,
                result = result,
                pnl = pnl,
                notes = "Imported from CSV",
                openedAt = if (opened > 0) opened else System.currentTimeMillis()
            )
        }
        return Result(trades, skipped, if (trades.isEmpty()) "No trade rows found." else null)
    }

    private fun detectDelimiter(headerLine: String): Char = when {
        headerLine.count { it == '\t' } > headerLine.count { it == ',' } -> '\t'
        headerLine.count { it == ';' } > headerLine.count { it == ',' } -> ';'
        else -> ','
    }

    /** Split a CSV line honoring simple double-quoted fields. */
    private fun splitLine(line: String, delim: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delim && !inQuotes -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
        }
        out.add(sb.toString())
        return out
    }

    private fun String?.num(): Double {
        if (this.isNullOrBlank()) return 0.0
        // Strip currency symbols, spaces, thousands separators; keep sign, digits, decimal.
        val cleaned = this.trim().trim('"').replace(" ", "").replace(",", "")
            .replace(Regex("[^0-9.\\-]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private val timeFormats = listOf(
        "yyyy.MM.dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss",
        "yyyy.MM.dd", "yyyy-MM-dd"
    )

    private fun parseTime(raw: String?): Long {
        val s = raw?.trim()?.trim('"') ?: return 0L
        if (s.isBlank()) return 0L
        runCatching { return OffsetDateTime.parse(s).toInstant().toEpochMilli() }
        for (f in timeFormats) {
            runCatching {
                val fmt = DateTimeFormatter.ofPattern(f)
                val ldt = if (f.contains("HH")) LocalDateTime.parse(s, fmt)
                else java.time.LocalDate.parse(s, fmt).atStartOfDay()
                return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return 0L
    }
}
