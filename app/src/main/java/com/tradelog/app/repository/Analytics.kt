package com.tradelog.app.repository

import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import kotlin.math.abs

data class TagStat(
    val name: String,
    val trades: Int,
    val wins: Int,
    val winRate: Double,
    val netPnl: Double,
    val avgR: Double
)

data class AnalyticsResult(
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val breakevens: Int = 0,
    val winRate: Double = 0.0,
    val netPnl: Double = 0.0,
    val avgR: Double = 0.0,
    val profitFactor: Double = 0.0,
    val expectancy: Double = 0.0,
    val byStrategy: List<TagStat> = emptyList(),
    val byInstrument: List<TagStat> = emptyList(),
    /** Cumulative P&L points in chronological order, starting at 0. */
    val equityCurve: List<Double> = emptyList(),
    val bestSetup: TagStat? = null,
    val worstSetup: TagStat? = null
)

object AnalyticsCalculator {

    fun compute(trades: List<Trade>): AnalyticsResult {
        if (trades.isEmpty()) return AnalyticsResult()

        val total = trades.size
        val wins = trades.count { it.result == TradeResult.WIN }
        val losses = trades.count { it.result == TradeResult.LOSS }
        val be = trades.count { it.result == TradeResult.BREAKEVEN }
        val decisive = wins + losses
        val winRate = if (decisive > 0) wins.toDouble() / decisive else 0.0

        val netPnl = trades.sumOf { it.pnl }
        val grossProfit = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = abs(trades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) Double.POSITIVE_INFINITY else 0.0

        val rValues = trades.mapNotNull { it.rMultiple }
        val avgR = if (rValues.isNotEmpty()) rValues.average() else 0.0
        val expectancy = if (total > 0) netPnl / total else 0.0

        val sortedByTime = trades.sortedBy { it.openedAt }
        val equity = ArrayList<Double>(sortedByTime.size + 1)
        var running = 0.0
        equity.add(0.0)
        for (t in sortedByTime) {
            running += t.pnl
            equity.add(running)
        }

        val byStrategy = groupStats(trades) { it.setupTag?.takeIf { s -> s.isNotBlank() } ?: "Untagged" }
        val byInstrument = groupStats(trades) { it.instrument.ifBlank { "Unknown" } }

        val ranked = byStrategy.filter { it.trades >= 1 }.sortedByDescending { it.netPnl }
        val best = ranked.firstOrNull()
        val worst = ranked.lastOrNull()?.takeIf { it != best }

        return AnalyticsResult(
            totalTrades = total,
            wins = wins,
            losses = losses,
            breakevens = be,
            winRate = winRate,
            netPnl = netPnl,
            avgR = avgR,
            profitFactor = profitFactor,
            expectancy = expectancy,
            byStrategy = byStrategy,
            byInstrument = byInstrument,
            equityCurve = equity,
            bestSetup = best,
            worstSetup = worst
        )
    }

    private fun groupStats(trades: List<Trade>, key: (Trade) -> String): List<TagStat> {
        return trades.groupBy(key).map { (name, list) ->
            val w = list.count { it.result == TradeResult.WIN }
            val l = list.count { it.result == TradeResult.LOSS }
            val decisive = w + l
            val rs = list.mapNotNull { it.rMultiple }
            TagStat(
                name = name,
                trades = list.size,
                wins = w,
                winRate = if (decisive > 0) w.toDouble() / decisive else 0.0,
                netPnl = list.sumOf { it.pnl },
                avgR = if (rs.isNotEmpty()) rs.average() else 0.0
            )
        }.sortedByDescending { it.trades }
    }
}
