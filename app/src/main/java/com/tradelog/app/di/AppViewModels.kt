package com.tradelog.app.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.analytics.AnalyticsViewModel
import com.tradelog.app.ui.backtest.BacktestEditViewModel
import com.tradelog.app.ui.backtest.BacktestListViewModel
import com.tradelog.app.ui.backtest.BacktestStatsViewModel
import com.tradelog.app.ui.calendar.CalendarViewModel
import com.tradelog.app.ui.dashboard.DashboardViewModel
import com.tradelog.app.ui.goals.GoalsViewModel
import com.tradelog.app.ui.journal.DailyEntryViewModel
import com.tradelog.app.ui.journal.JournalViewModel
import com.tradelog.app.ui.notebook.NoteEditViewModel
import com.tradelog.app.ui.notebook.NotebookViewModel
import com.tradelog.app.ui.onboarding.OnboardingViewModel
import com.tradelog.app.ui.payouts.PayoutEditViewModel
import com.tradelog.app.ui.payouts.PayoutViewModel
import com.tradelog.app.ui.portfolio.AccountEditViewModel
import com.tradelog.app.ui.portfolio.PortfolioViewModel
import com.tradelog.app.ui.settings.SettingsViewModel
import com.tradelog.app.ui.tools.InstrumentViewModel
import com.tradelog.app.ui.tools.PositionCalcViewModel
import com.tradelog.app.ui.trades.TradeDetailViewModel
import com.tradelog.app.ui.trades.TradeEditViewModel

class AppViewModelFactory(private val repo: TradeLogRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repo)
            modelClass.isAssignableFrom(JournalViewModel::class.java) -> JournalViewModel(repo)
            modelClass.isAssignableFrom(TradeEditViewModel::class.java) -> TradeEditViewModel(repo)
            modelClass.isAssignableFrom(TradeDetailViewModel::class.java) -> TradeDetailViewModel(repo)
            modelClass.isAssignableFrom(DailyEntryViewModel::class.java) -> DailyEntryViewModel(repo)
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> AnalyticsViewModel(repo)
            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> GoalsViewModel(repo)
            modelClass.isAssignableFrom(PortfolioViewModel::class.java) -> PortfolioViewModel(repo)
            modelClass.isAssignableFrom(AccountEditViewModel::class.java) -> AccountEditViewModel(repo)
            modelClass.isAssignableFrom(NotebookViewModel::class.java) -> NotebookViewModel(repo)
            modelClass.isAssignableFrom(NoteEditViewModel::class.java) -> NoteEditViewModel(repo)
            modelClass.isAssignableFrom(PayoutViewModel::class.java) -> PayoutViewModel(repo)
            modelClass.isAssignableFrom(PayoutEditViewModel::class.java) -> PayoutEditViewModel(repo)
            modelClass.isAssignableFrom(PositionCalcViewModel::class.java) -> PositionCalcViewModel(repo)
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(repo)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repo)
            modelClass.isAssignableFrom(InstrumentViewModel::class.java) -> InstrumentViewModel(repo)
            modelClass.isAssignableFrom(BacktestListViewModel::class.java) -> BacktestListViewModel(repo)
            modelClass.isAssignableFrom(BacktestEditViewModel::class.java) -> BacktestEditViewModel(repo)
            modelClass.isAssignableFrom(BacktestStatsViewModel::class.java) -> BacktestStatsViewModel(repo)
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(repo)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return vm as T
    }
}

@Composable
inline fun <reified VM : ViewModel> appViewModel(): VM {
    val context = LocalContext.current
    val repo = ServiceLocator.repository(context)
    return viewModel(factory = AppViewModelFactory(repo))
}
