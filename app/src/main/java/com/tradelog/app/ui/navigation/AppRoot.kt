package com.tradelog.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tradelog.app.ui.analytics.AnalyticsScreen
import com.tradelog.app.ui.backtest.BacktestEditScreen
import com.tradelog.app.ui.backtest.BacktestGalleryScreen
import com.tradelog.app.ui.backtest.BacktestStatsScreen
import com.tradelog.app.ui.calendar.CalendarScreen
import com.tradelog.app.ui.countdown.CountdownScreen
import com.tradelog.app.ui.dashboard.DashboardScreen
import com.tradelog.app.ui.goals.GoalsScreen
import com.tradelog.app.ui.journal.DailyEntryScreen
import com.tradelog.app.ui.journal.JournalScreen
import com.tradelog.app.ui.more.MoreScreen
import com.tradelog.app.ui.notebook.NoteEditScreen
import com.tradelog.app.ui.notebook.NotebookScreen
import com.tradelog.app.ui.payouts.PayoutEditScreen
import com.tradelog.app.ui.payouts.PayoutsScreen
import com.tradelog.app.ui.portfolio.AccountEditScreen
import com.tradelog.app.ui.portfolio.PortfolioScreen
import com.tradelog.app.ui.settings.SettingsScreen
import com.tradelog.app.ui.tools.InstrumentsScreen
import com.tradelog.app.ui.tools.PositionCalcScreen
import com.tradelog.app.ui.tools.ToolsScreen
import com.tradelog.app.ui.trades.TradeDetailScreen
import com.tradelog.app.ui.trades.TradeEditScreen
import com.tradelog.app.util.DateUtils

@Composable
fun AppRoot(openCalendar: Boolean, onCalendarConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(openCalendar) {
        if (openCalendar) {
            navController.navigate(Routes.CALENDAR)
            onCalendarConsumed()
        }
    }

    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAddTrade = { navController.navigate(Routes.tradeEdit()) },
                    onOpenTrade = { id -> navController.navigate(Routes.tradeDetail(id)) },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Routes.JOURNAL) {
                JournalScreen(
                    onAddTrade = { navController.navigate(Routes.tradeEdit()) },
                    onOpenTrade = { id -> navController.navigate(Routes.tradeDetail(id)) },
                    onOpenDaily = { date -> navController.navigate(Routes.dailyEdit(date)) }
                )
            }
            composable(Routes.ANALYTICS) { AnalyticsScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
            composable(Routes.MORE) {
                MoreScreen(onNavigate = { route -> navController.navigate(route) })
            }

            composable(
                route = "${Routes.TRADE_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                TradeEditScreen(
                    tradeId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.TRADE_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TradeDetailScreen(
                    tradeId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.tradeEdit(id)) }
                )
            }
            composable(
                route = "${Routes.DAILY_EDIT}/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { entry ->
                DailyEntryScreen(
                    date = entry.arguments?.getString("date") ?: DateUtils.todayKey(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PORTFOLIO) {
                PortfolioScreen(
                    onAdd = { navController.navigate(Routes.accountEdit()) },
                    onEdit = { id -> navController.navigate(Routes.accountEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.ACCOUNT_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                AccountEditScreen(
                    accountId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.NOTEBOOK) {
                NotebookScreen(
                    onAdd = { navController.navigate(Routes.noteEdit()) },
                    onOpen = { id -> navController.navigate(Routes.noteEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.NOTE_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                NoteEditScreen(
                    noteId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PAYOUTS) {
                PayoutsScreen(
                    onAdd = { navController.navigate(Routes.payoutEdit()) },
                    onEdit = { id -> navController.navigate(Routes.payoutEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.PAYOUT_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                PayoutEditScreen(
                    payoutId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.TOOLS) {
                ToolsScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.POSITION_CALC) {
                PositionCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.INSTRUMENTS) {
                InstrumentsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BACKTESTS) {
                BacktestGalleryScreen(
                    onAdd = { navController.navigate(Routes.backtestEdit()) },
                    onOpen = { id -> navController.navigate(Routes.backtestEdit(id)) },
                    onStats = { navController.navigate(Routes.BACKTEST_STATS) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BACKTEST_STATS) {
                BacktestStatsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COUNTDOWN) {
                CountdownScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.BACKTEST_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                BacktestEditScreen(
                    backtestId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
