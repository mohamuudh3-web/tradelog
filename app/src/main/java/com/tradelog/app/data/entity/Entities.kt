package com.tradelog.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val broker: String = "",
    val balance: Double = 0.0,
    val currency: String = "USD",
    val isPropFirm: Boolean = false,
    // RyzeLog prop-firm challenge fields
    @ColumnInfo(defaultValue = "") val challengePhase: String = "",
    @ColumnInfo(defaultValue = "") val status: String = "",
    @ColumnInfo(defaultValue = "") val website: String = "",
    val startingBalance: Double? = null,
    val splitPercent: Double? = null,
    val drawdownPercent: Double? = null,
    val targetPercent: Double? = null,
    val createdAt: Long = 0L
)

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long? = null,
    val instrument: String,
    val direction: Direction = Direction.LONG,
    val entryPrice: Double = 0.0,
    val exitPrice: Double? = null,
    val lotSize: Double = 0.0,
    val riskPercent: Double? = null,
    val rMultiple: Double? = null,
    val result: TradeResult = TradeResult.BREAKEVEN,
    /** Realized money result, positive for profit, negative for loss. Used for net P&L. */
    val pnl: Double = 0.0,
    val setupTag: String? = null,
    @ColumnInfo(defaultValue = "") val session: String = "",
    val slPips: Double? = null,
    val tpPips: Double? = null,
    /** Comma-separated psychology tags chosen before entry. */
    @ColumnInfo(defaultValue = "") val psychology: String = "",
    /** Comma-separated ids of confirmation rules that were checked. */
    @ColumnInfo(defaultValue = "") val checkedRules: String = "",
    /** Comma-separated remote image URLs (in addition to the local screenshot). */
    @ColumnInfo(defaultValue = "") val imageUrls: String = "",
    val notes: String = "",
    /** content:// or file:// uri to a screenshot, if any. */
    val screenshotUri: String? = null,
    /** When the trade was taken (epoch millis). */
    val openedAt: Long = 0L,
    val createdAt: Long = 0L
)

@Entity(tableName = "journal_entries", indices = [Index(value = ["date"], unique = true)])
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** yyyy-MM-dd, one entry per day. */
    val date: String,
    val mindset: String = "",
    val routine: String = "",
    val reflection: String = "",
    val mood: Int = 3,
    val discipline: Int = 3,
    // RyzeLog-style daily battle plan
    @ColumnInfo(defaultValue = "") val title: String = "",
    @ColumnInfo(defaultValue = "") val gratitude: String = "",
    @ColumnInfo(defaultValue = "") val battlePlan: String = "",
    @ColumnInfo(defaultValue = "") val affirmation: String = "",
    @ColumnInfo(defaultValue = "") val tags: String = "",
    @ColumnInfo(defaultValue = "") val moodLabel: String = "",
    /** Newline-separated focus tasks for the day. */
    @ColumnInfo(defaultValue = "") val focusTasks: String = "",
    val accountBalance: Double? = null,
    val tradesTarget: Int? = null,
    val pipsTarget: Double? = null,
    val riskPercent: Double? = null,
    val riskAmount: Double? = null,
    val createdAt: Long = 0L
)

@Entity(tableName = "notes")
data class NotebookNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String = "",
    /** comma-separated tags */
    val tags: String = "",
    val updatedAt: Long = 0L
)

@Entity(tableName = "setup_tags", indices = [Index(value = ["name"], unique = true)])
data class SetupTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "payouts")
data class PayoutRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** yyyy-MM-dd */
    val date: String,
    val accountName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: PayoutStatus = PayoutStatus.PENDING,
    val notes: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "economic_events")
data class EconomicEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** currency code, e.g. USD, EUR */
    val country: String,
    val impact: Impact = Impact.LOW,
    /** event time, epoch millis (UTC) */
    val dateTimeUtc: Long,
    val forecast: String = "",
    val previous: String = "",
    val actual: String = ""
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: GoalType = GoalType.DAILY,
    val metric: GoalMetric = GoalMetric.MANUAL,
    val target: Int = 1,
    /** Manual increments for the current period. */
    val manualProgress: Int = 0,
    /** Period key the manualProgress belongs to (yyyy-MM-dd for daily, yyyy-'W'ww for weekly). */
    val manualPeriodKey: String = "",
    val unit: String = "",
    val archived: Boolean = false,
    /** Daily reminder time; hour -1 means no reminder. */
    @ColumnInfo(defaultValue = "-1") val reminderHour: Int = -1,
    @ColumnInfo(defaultValue = "0") val reminderMinute: Int = 0,
    val createdAt: Long = 0L
)

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val frequency: TaskFrequency = TaskFrequency.DAILY,
    @ColumnInfo(defaultValue = "TASK") val category: TaskCategory = TaskCategory.TASK,
    /** Last date this task was completed (yyyy-MM-dd). A DAILY task counts as done today iff this == today. */
    val lastCompletedDate: String? = null,
    /** For ONCE tasks only. */
    val doneOnce: Boolean = false,
    val sortOrder: Int = 0,
    /** Daily reminder time; hour -1 means no reminder. */
    @ColumnInfo(defaultValue = "-1") val reminderHour: Int = -1,
    @ColumnInfo(defaultValue = "0") val reminderMinute: Int = 0,
    val createdAt: Long = 0L
)

@Entity(tableName = "task_completions", indices = [Index(value = ["date"])])
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** yyyy-MM-dd */
    val date: String
)

@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDateMillis: Long,
    val motivation: String = "",
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val reviewDone: Boolean = false,
    val reachedIt: Boolean = false,
    val wentWrong: String = "",
    val improveNext: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "checklist_rules")
data class ChecklistRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "currencies", indices = [Index(value = ["code"], unique = true)])
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO-style code, e.g. USD, EUR, GBP. Uppercase. */
    val code: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "instruments", indices = [Index(value = ["name"], unique = true)])
data class Instrument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Money value of 1 pip/point per 1.0 lot. */
    val pipValuePerLot: Double = 10.0,
    val sortOrder: Int = 0
)

@Entity(tableName = "backtests")
data class Backtest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val instrument: String = "",
    val dateMillis: Long = 0L,
    /** Free-form bias note, e.g. "Bullish", "A+ setup". */
    val bias: String = "",
    /** "Buy" / "Sell" / "" */
    @ColumnInfo(defaultValue = "") val direction: String = "",
    /** "WIN" / "LOSS" / "BE" / "" */
    @ColumnInfo(defaultValue = "") val result: String = "",
    /** Session / scenario tag, e.g. "S2", "London". */
    @ColumnInfo(defaultValue = "") val session: String = "",
    val slPips: Double? = null,
    val tpPips: Double? = null,
    /** Comma-separated ids of confirmation rules that were checked. */
    @ColumnInfo(defaultValue = "") val checkedRules: String = "",
    /** Comma-separated remote image URLs (in addition to local screenshots). */
    @ColumnInfo(defaultValue = "") val imageUrls: String = "",
    /** Labeled chart snapshot URLs (RyzeLog style). */
    @ColumnInfo(defaultValue = "") val chart5Url: String = "",
    @ColumnInfo(defaultValue = "") val chart15Url: String = "",
    val notes: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "backtest_images")
data class BacktestImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val backtestId: Long,
    val path: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "position_presets")
data class PositionPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    val riskPercent: Double = 1.0,
    val stopLoss: Double = 10.0,
    /** Money value of 1 pip/point per 1.0 lot for the instrument. */
    val pipValuePerLot: Double = 10.0,
    val instrument: String = ""
)
