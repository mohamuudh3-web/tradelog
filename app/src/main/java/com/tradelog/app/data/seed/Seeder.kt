package com.tradelog.app.data.seed

import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.GoalMetric
import com.tradelog.app.data.entity.GoalType
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PayoutStatus
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.PositionPreset
import com.tradelog.app.data.entity.TaskCategory
import com.tradelog.app.data.entity.TaskFrequency
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils

/** Populates a few example records so the UI is alive on first launch. */
object Seeder {

    suspend fun seed(repo: TradeLogRepository) {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        // Accounts
        val ftmoId = repo.saveAccount(
            Account(name = "FTMO Challenge", broker = "FTMO", balance = 100_000.0, currency = "USD", isPropFirm = true, createdAt = now)
        )
        val liveId = repo.saveAccount(
            Account(name = "Personal Live", broker = "IC Markets", balance = 5_000.0, currency = "USD", isPropFirm = false, createdAt = now)
        )

        // Setup tags
        listOf("Breakout", "Pullback", "Liquidity Sweep", "London Open", "FVG").forEach { repo.addSetupTag(it) }

        // Trades (spread over last ~10 days)
        val trades = listOf(
            Trade(accountId = ftmoId, instrument = "EURUSD", direction = Direction.LONG, entryPrice = 1.0820, exitPrice = 1.0875, lotSize = 0.5, riskPercent = 1.0, rMultiple = 2.1, result = TradeResult.WIN, pnl = 275.0, setupTag = "London Open", notes = "Clean break of Asian high.", openedAt = now - 9 * day),
            Trade(accountId = ftmoId, instrument = "GBPUSD", direction = Direction.SHORT, entryPrice = 1.2740, exitPrice = 1.2705, lotSize = 0.4, riskPercent = 1.0, rMultiple = 1.4, result = TradeResult.WIN, pnl = 140.0, setupTag = "Liquidity Sweep", notes = "Swept prior day high then rejected.", openedAt = now - 8 * day),
            Trade(accountId = ftmoId, instrument = "XAUUSD", direction = Direction.LONG, entryPrice = 2315.0, exitPrice = 2308.0, lotSize = 0.1, riskPercent = 1.0, rMultiple = -1.0, result = TradeResult.LOSS, pnl = -120.0, setupTag = "Breakout", notes = "Faked out, stopped at structure.", openedAt = now - 6 * day),
            Trade(accountId = liveId, instrument = "EURUSD", direction = Direction.LONG, entryPrice = 1.0901, exitPrice = 1.0901, lotSize = 0.2, riskPercent = 0.5, rMultiple = 0.0, result = TradeResult.BREAKEVEN, pnl = 0.0, setupTag = "Pullback", notes = "Moved to BE, no follow-through.", openedAt = now - 4 * day),
            Trade(accountId = ftmoId, instrument = "US30", direction = Direction.LONG, entryPrice = 38800.0, exitPrice = 39010.0, lotSize = 0.3, riskPercent = 1.0, rMultiple = 2.6, result = TradeResult.WIN, pnl = 315.0, setupTag = "FVG", notes = "Filled FVG, ran to target.", openedAt = now - 2 * day),
            Trade(accountId = liveId, instrument = "GBPJPY", direction = Direction.SHORT, entryPrice = 198.40, exitPrice = 198.95, lotSize = 0.1, riskPercent = 1.0, rMultiple = -1.0, result = TradeResult.LOSS, pnl = -90.0, setupTag = "Pullback", notes = "Counter-trend, cut quickly.", openedAt = now - 1 * day)
        )
        trades.forEach { repo.saveTrade(it) }

        // Journal entries
        val today = DateUtils.today()
        repo.saveJournal(
            JournalEntry(
                date = DateUtils.dateKey(today.minusDays(1)),
                mindset = "Calm and focused. Slept well.",
                routine = "Reviewed calendar, marked HTF levels.",
                reflection = "Took only A+ setups. Stuck to the plan.",
                mood = 4, discipline = 5, createdAt = now
            )
        )
        repo.saveJournal(
            JournalEntry(
                date = DateUtils.dateKey(today),
                mindset = "Slightly impatient at the open.",
                routine = "Backtested 2 setups before session.",
                reflection = "Need to wait for confirmation, not anticipate.",
                mood = 3, discipline = 4, createdAt = now
            )
        )

        // Notebook notes
        repo.saveNote(NotebookNote(title = "London Open Playbook", body = "Wait for the Asian range to be swept, then look for displacement and a FVG entry on the 5m.", tags = "strategy, london, fvg"))
        repo.saveNote(NotebookNote(title = "Risk Rules", body = "Max 1% per trade. Max 3 trades/day. Stop after 2 losses. No revenge trading.", tags = "rules, risk"))
        repo.saveNote(NotebookNote(title = "Lesson: Patience", body = "Most of my losses come from entering before confirmation. Let the setup come to me.", tags = "lesson, psychology"))

        // Payouts
        repo.savePayout(PayoutRecord(date = DateUtils.dateKey(today.minusDays(20)), accountName = "FTMO", amount = 1200.0, currency = "USD", status = PayoutStatus.PAID, notes = "First payout."))
        repo.savePayout(PayoutRecord(date = DateUtils.dateKey(today.minusDays(2)), accountName = "FTMO", amount = 1800.0, currency = "USD", status = PayoutStatus.PENDING, notes = "Requested, awaiting processing."))

        // Goals
        repo.saveGoal(Goal(title = "Complete backtests", type = GoalType.DAILY, metric = GoalMetric.MANUAL, target = 2, unit = "backtests", createdAt = now))
        repo.saveGoal(Goal(title = "Take quality trades this week", type = GoalType.WEEKLY, metric = GoalMetric.TRADES, target = 5, unit = "trades", createdAt = now))
        repo.saveGoal(Goal(title = "Journal entries this week", type = GoalType.WEEKLY, metric = GoalMetric.JOURNAL_ENTRIES, target = 5, unit = "entries", createdAt = now))
        repo.saveGoal(Goal(title = "Daily tasks done", type = GoalType.DAILY, metric = GoalMetric.TASKS_COMPLETED, target = 4, unit = "tasks", createdAt = now))

        // Daily tasks
        listOf(
            "Review economic calendar",
            "Mark HTF key levels",
            "Backtest 2 setups",
            "Journal yesterday's trades",
            "Check open positions"
        ).forEachIndexed { i, t ->
            repo.saveTask(TaskItem(title = t, frequency = TaskFrequency.DAILY, sortOrder = i, createdAt = now))
        }

        // Position preset
        repo.savePreset(PositionPreset(name = "FTMO EURUSD 1%", balance = 100_000.0, riskPercent = 1.0, stopLoss = 15.0, pipValuePerLot = 10.0, instrument = "EURUSD"))

        seedV2(repo)
        seedV3(repo)
        seedV4(repo)
    }

    /** Incremental seed for features added after v1 (saved pairs, morning routine). Idempotent-guarded by caller. */
    suspend fun seedV2(repo: TradeLogRepository) {
        val now = System.currentTimeMillis()

        // Saved instruments / pairs with typical pip/point value per 1.0 lot (USD-quoted approximations).
        val pairs = listOf(
            "EURUSD" to 10.0, "GBPUSD" to 10.0, "AUDUSD" to 10.0, "NZDUSD" to 10.0,
            "USDJPY" to 9.0, "USDCAD" to 7.5, "USDCHF" to 11.0,
            "GBPJPY" to 9.0, "EURJPY" to 9.0,
            "XAUUSD" to 10.0, "XAGUSD" to 50.0,
            "US30" to 1.0, "NAS100" to 1.0, "SPX500" to 1.0,
            "BTCUSD" to 1.0
        )
        pairs.forEachIndexed { i, (name, pip) ->
            repo.saveInstrument(Instrument(name = name, pipValuePerLot = pip, sortOrder = i))
        }

        // Morning routine checklist
        listOf(
            "Woke up early",
            "Exercise / workout",
            "Clean room",
            "Meditate / breathe",
            "Hydrate",
            "Review trading plan"
        ).forEachIndexed { i, t ->
            repo.saveTask(TaskItem(title = t, frequency = TaskFrequency.DAILY, category = TaskCategory.ROUTINE, sortOrder = i, createdAt = now))
        }
    }

    /** Default confirmation checklist rules. */
    suspend fun seedV4(repo: TradeLogRepository) {
        listOf(
            "Find the trend (H1/H4 for S1, 15M for S2/S3, 5M for S4)",
            "Find the institutional order-flow zone",
            "Wait for the liquidity grab with reversal volume",
            "Confirm volume drives the countertrend break",
            "Confirm momentum aligns with the direction"
        ).forEach { repo.addChecklistRule(it) }
    }

    /** Example backtests so the gallery shows its layout on first run. */
    suspend fun seedV3(repo: TradeLogRepository) {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        repo.saveBacktest(
            Backtest(
                title = "EURUSD London sweep", instrument = "EURUSD", direction = "Buy",
                result = "WIN", session = "S2", bias = "Bullish",
                notes = "Swept Asian low, displaced up, entered the 5m FVG. Clean 1:3 to the prior high.",
                dateMillis = now - 2 * day
            )
        )
        repo.saveBacktest(
            Backtest(
                title = "GBPUSD failed breakout", instrument = "GBPUSD", direction = "Sell",
                result = "LOSS", session = "S3", bias = "Bearish",
                notes = "Anticipated the breakout instead of waiting for the candle close. Lesson: wait for confirmation.",
                dateMillis = now - 1 * day
            )
        )
    }
}
