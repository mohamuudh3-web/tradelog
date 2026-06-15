package com.tradelog.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val DASHBOARD = "dashboard"
    const val JOURNAL = "journal"
    const val ANALYTICS = "analytics"
    const val GOALS = "goals"
    const val MORE = "more"

    const val TRADE_EDIT = "trade_edit" // ?id=
    const val TRADE_DETAIL = "trade_detail" // /{id}
    const val DAILY_EDIT = "daily_edit" // /{date}

    const val PORTFOLIO = "portfolio"
    const val ACCOUNT_EDIT = "account_edit" // ?id=
    const val NOTEBOOK = "notebook"
    const val NOTE_EDIT = "note_edit" // ?id=
    const val PAYOUTS = "payouts"
    const val PAYOUT_EDIT = "payout_edit" // ?id=
    const val TOOLS = "tools"
    const val POSITION_CALC = "position_calc"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val INSTRUMENTS = "instruments"
    const val BACKTESTS = "backtests"
    const val BACKTEST_EDIT = "backtest_edit" // ?id=
    const val BACKTEST_STATS = "backtest_stats"
    const val COUNTDOWN = "countdown"
    const val SYNC = "sync"

    fun tradeEdit(id: Long = 0L) = "$TRADE_EDIT?id=$id"
    fun tradeDetail(id: Long) = "$TRADE_DETAIL/$id"
    fun dailyEdit(date: String) = "$DAILY_EDIT/$date"
    fun accountEdit(id: Long = 0L) = "$ACCOUNT_EDIT?id=$id"
    fun noteEdit(id: Long = 0L) = "$NOTE_EDIT?id=$id"
    fun payoutEdit(id: Long = 0L) = "$PAYOUT_EDIT?id=$id"
    fun backtestEdit(id: Long = 0L) = "$BACKTEST_EDIT?id=$id"
}

data class BottomDest(val route: String, val label: String, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDest(Routes.DASHBOARD, "Home", Icons.Filled.Dashboard),
    BottomDest(Routes.JOURNAL, "Journal", Icons.Filled.Edit),
    BottomDest(Routes.ANALYTICS, "Analytics", Icons.Filled.BarChart),
    BottomDest(Routes.GOALS, "Goals", Icons.Filled.TrackChanges),
    BottomDest(Routes.MORE, "More", Icons.Filled.MoreHoriz)
)
