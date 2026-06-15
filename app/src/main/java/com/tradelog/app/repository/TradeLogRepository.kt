package com.tradelog.app.repository

import com.tradelog.app.data.SettingsStore
import com.tradelog.app.data.db.AppDatabase
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.GoalMetric
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PositionPreset
import com.tradelog.app.data.entity.SetupTag
import com.tradelog.app.data.entity.TaskCompletion
import com.tradelog.app.data.entity.TaskFrequency
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.SyncMeta
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.network.FFEvent
import com.tradelog.app.network.ForexFactoryApi
import com.tradelog.app.sync.SyncTables
import com.tradelog.app.util.DateUtils
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime
import kotlin.math.max

class TradeLogRepository(
    private val db: AppDatabase,
    val settings: SettingsStore,
    private val api: ForexFactoryApi
) {
    private val tradeDao = db.tradeDao()
    private val accountDao = db.accountDao()
    private val journalDao = db.journalDao()
    private val notebookDao = db.notebookDao()
    private val setupTagDao = db.setupTagDao()
    private val payoutDao = db.payoutDao()
    private val eventDao = db.economicEventDao()
    private val goalDao = db.goalDao()
    private val taskDao = db.taskDao()
    private val presetDao = db.positionPresetDao()
    private val instrumentDao = db.instrumentDao()
    private val backtestDao = db.backtestDao()
    private val checklistRuleDao = db.checklistRuleDao()
    private val countdownDao = db.countdownDao()
    private val syncMetaDao = db.syncMetaDao()

    // ---- Cloud-sync bookkeeping ----
    /** Mark a local row as changed so the next sync pushes it to the cloud. */
    private suspend fun touchSync(table: String, localId: Long) {
        if (localId <= 0L) return
        val now = System.currentTimeMillis()
        val existing = syncMetaDao.byLocal(table, localId)
        if (existing == null) {
            syncMetaDao.insert(
                SyncMeta(
                    tableName = table, localId = localId, uid = UUID.randomUUID().toString(),
                    updatedAt = now, deleted = false, pending = true
                )
            )
        } else {
            syncMetaDao.update(existing.copy(updatedAt = now, deleted = false, pending = true))
        }
    }

    /** Tombstone a deleted local row so the deletion propagates to the cloud. */
    private suspend fun tombstoneSync(table: String, localId: Long) {
        val existing = syncMetaDao.byLocal(table, localId) ?: return
        syncMetaDao.update(existing.copy(updatedAt = System.currentTimeMillis(), deleted = true, pending = true))
    }

    // ---- Streams ----
    val trades: Flow<List<Trade>> = tradeDao.observeAll()
    val accounts: Flow<List<Account>> = accountDao.observeAll()
    val journals: Flow<List<JournalEntry>> = journalDao.observeAll()
    val notes: Flow<List<NotebookNote>> = notebookDao.observeAll()
    val setupTags: Flow<List<SetupTag>> = setupTagDao.observeAll()
    val payouts: Flow<List<PayoutRecord>> = payoutDao.observeAll()
    val events: Flow<List<EconomicEvent>> = eventDao.observeAll()
    val activeGoals: Flow<List<Goal>> = goalDao.observeActive()
    val allGoals: Flow<List<Goal>> = goalDao.observeAll()
    val tasks: Flow<List<TaskItem>> = taskDao.observeAll()
    val presets: Flow<List<PositionPreset>> = presetDao.observeAll()
    val instruments: Flow<List<Instrument>> = instrumentDao.observeAll()
    val checklistRules: Flow<List<ChecklistRule>> = checklistRuleDao.observeAll()
    val countdowns: Flow<List<Countdown>> = countdownDao.observeAll()
    val backtests: Flow<List<Backtest>> = backtestDao.observeAll()
    val backtestImages: Flow<List<BacktestImage>> = backtestDao.observeAllImages()

    // ---- Trades ----
    fun observeTrade(id: Long): Flow<Trade?> = tradeDao.observeById(id)
    suspend fun getTrade(id: Long): Trade? = tradeDao.getById(id)
    suspend fun saveTrade(trade: Trade): Long {
        val stamped = if (trade.createdAt == 0L) trade.copy(createdAt = System.currentTimeMillis()) else trade
        val id = if (stamped.id == 0L) tradeDao.insert(stamped) else { tradeDao.update(stamped); stamped.id }
        touchSync(SyncTables.TRADES, id)
        return id
    }
    suspend fun deleteTrade(trade: Trade) {
        tombstoneSync(SyncTables.TRADES, trade.id)
        tradeDao.delete(trade)
    }

    // ---- Accounts ----
    suspend fun getAccount(id: Long): Account? = accountDao.getById(id)
    suspend fun saveAccount(account: Account): Long {
        val stamped = if (account.createdAt == 0L) account.copy(createdAt = System.currentTimeMillis()) else account
        val id = accountDao.upsert(stamped)
        touchSync(SyncTables.ACCOUNTS, id)
        return id
    }
    suspend fun deleteAccount(account: Account) {
        tombstoneSync(SyncTables.ACCOUNTS, account.id)
        accountDao.delete(account)
    }

    /** Net realized P&L per account, computed from trades. */
    suspend fun accountPnl(): Map<Long, Double> =
        tradeDao.getAllAsc().filter { it.accountId != null }.groupBy { it.accountId!! }
            .mapValues { (_, list) -> list.sumOf { it.pnl } }

    // ---- Journal ----
    suspend fun getJournal(date: String): JournalEntry? = journalDao.getByDate(date)
    fun observeJournal(date: String): Flow<JournalEntry?> = journalDao.observeByDate(date)
    suspend fun saveJournal(entry: JournalEntry): Long {
        val stamped = if (entry.createdAt == 0L) entry.copy(createdAt = System.currentTimeMillis()) else entry
        val id = journalDao.insert(stamped)
        touchSync(SyncTables.JOURNAL, id)
        return id
    }
    suspend fun deleteJournal(entry: JournalEntry) {
        tombstoneSync(SyncTables.JOURNAL, entry.id)
        journalDao.delete(entry)
    }

    // ---- Notebook ----
    fun searchNotes(q: String): Flow<List<NotebookNote>> =
        if (q.isBlank()) notebookDao.observeAll() else notebookDao.search(q.trim())
    suspend fun getNote(id: Long): NotebookNote? = notebookDao.getById(id)
    suspend fun saveNote(note: NotebookNote): Long {
        val id = notebookDao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
        touchSync(SyncTables.NOTES, id)
        return id
    }
    suspend fun deleteNote(note: NotebookNote) {
        tombstoneSync(SyncTables.NOTES, note.id)
        notebookDao.delete(note)
    }

    // ---- Setup tags ----
    suspend fun addSetupTag(name: String) { if (name.isNotBlank()) setupTagDao.insert(SetupTag(name = name.trim())) }
    suspend fun deleteSetupTag(tag: SetupTag) = setupTagDao.delete(tag)

    // ---- Payouts ----
    suspend fun getPayout(id: Long): PayoutRecord? = payoutDao.getById(id)
    suspend fun savePayout(payout: PayoutRecord): Long {
        val stamped = if (payout.createdAt == 0L) payout.copy(createdAt = System.currentTimeMillis()) else payout
        val id = payoutDao.upsert(stamped)
        touchSync(SyncTables.PAYOUTS, id)
        return id
    }
    suspend fun deletePayout(payout: PayoutRecord) {
        tombstoneSync(SyncTables.PAYOUTS, payout.id)
        payoutDao.delete(payout)
    }

    // ---- Goals ----
    suspend fun getGoal(id: Long): Goal? = goalDao.getById(id)
    suspend fun saveGoal(goal: Goal): Long {
        val stamped = if (goal.createdAt == 0L) goal.copy(createdAt = System.currentTimeMillis()) else goal
        return goalDao.upsert(stamped)
    }
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal)
    suspend fun setGoalArchived(goal: Goal, archived: Boolean) = goalDao.upsert(goal.copy(archived = archived))

    suspend fun incrementGoal(goal: Goal, delta: Int) {
        val key = DateUtils.periodKey(goal.type)
        val base = if (goal.manualPeriodKey == key) goal.manualProgress else 0
        goalDao.upsert(goal.copy(manualProgress = max(0, base + delta), manualPeriodKey = key))
    }

    /** Current progress for a goal in its current (resetting) period. */
    suspend fun goalProgress(goal: Goal): Int {
        val auto = when (goal.metric) {
            GoalMetric.TRADES -> {
                val (s, e) = DateUtils.periodEpochRange(goal.type); tradeDao.countBetween(s, e)
            }
            GoalMetric.JOURNAL_ENTRIES -> {
                val (s, e) = DateUtils.periodDateRange(goal.type); journalDao.countBetween(s, e)
            }
            GoalMetric.TASKS_COMPLETED -> {
                val (s, e) = DateUtils.periodDateRange(goal.type); taskDao.countCompletionsBetween(s, e)
            }
            GoalMetric.MANUAL -> 0
        }
        val manual = if (goal.metric == GoalMetric.MANUAL &&
            goal.manualPeriodKey == DateUtils.periodKey(goal.type)
        ) goal.manualProgress else 0
        return auto + manual
    }

    // ---- Tasks ----
    suspend fun getTask(id: Long): TaskItem? = taskDao.getById(id)
    suspend fun saveTask(task: TaskItem): Long {
        val stamped = if (task.createdAt == 0L) task.copy(createdAt = System.currentTimeMillis()) else task
        return taskDao.upsert(stamped)
    }
    suspend fun deleteTask(task: TaskItem) = taskDao.delete(task)

    fun isTaskDoneToday(task: TaskItem): Boolean =
        if (task.frequency == TaskFrequency.ONCE) task.doneOnce
        else task.lastCompletedDate == DateUtils.todayKey()

    suspend fun setTaskDone(task: TaskItem, done: Boolean) {
        val today = DateUtils.todayKey()
        taskDao.removeCompletion(task.id, today)
        if (done) {
            taskDao.upsert(task.copy(lastCompletedDate = today, doneOnce = task.frequency == TaskFrequency.ONCE))
            taskDao.insertCompletion(TaskCompletion(taskId = task.id, date = today))
        } else {
            taskDao.upsert(task.copy(lastCompletedDate = null, doneOnce = false))
        }
    }

    // ---- Position presets ----
    suspend fun savePreset(preset: PositionPreset): Long = presetDao.upsert(preset)
    suspend fun deletePreset(preset: PositionPreset) = presetDao.delete(preset)

    // ---- Instruments (saved pairs) ----
    suspend fun saveInstrument(instrument: Instrument): Long = instrumentDao.upsert(instrument)
    suspend fun addInstrument(name: String, pipValuePerLot: Double) {
        if (name.isNotBlank()) instrumentDao.insert(Instrument(name = name.trim().uppercase(), pipValuePerLot = pipValuePerLot))
    }
    suspend fun deleteInstrument(instrument: Instrument) = instrumentDao.delete(instrument)
    suspend fun instrumentCount(): Int = instrumentDao.count()

    // ---- Checklist rules ----
    suspend fun addChecklistRule(text: String) {
        if (text.isNotBlank()) checklistRuleDao.insert(ChecklistRule(text = text.trim()))
    }
    suspend fun deleteChecklistRule(rule: ChecklistRule) = checklistRuleDao.delete(rule)
    suspend fun checklistRuleCount(): Int = checklistRuleDao.count()

    // ---- Goal countdowns ----
    suspend fun getCountdown(id: Long): Countdown? = countdownDao.getById(id)
    suspend fun saveCountdown(c: Countdown): Long {
        val stamped = if (c.createdAt == 0L) c.copy(createdAt = System.currentTimeMillis()) else c
        return countdownDao.upsert(stamped)
    }
    suspend fun deleteCountdown(c: Countdown) = countdownDao.delete(c)
    suspend fun allCountdowns(): List<Countdown> = countdownDao.getAll()

    /** Did the user log any backtest yesterday (local day)? */
    suspend fun backtestedYesterday(): Boolean {
        val (start, end) = DateUtils.dayEpochBounds(DateUtils.today().minusDays(1))
        return backtestDao.countBetween(start, end) > 0
    }

    // ---- Backtests ----
    suspend fun getBacktest(id: Long): Backtest? = backtestDao.getById(id)
    fun observeBacktestImages(id: Long): Flow<List<BacktestImage>> = backtestDao.observeImages(id)
    suspend fun saveBacktest(backtest: Backtest): Long {
        val stamped = if (backtest.createdAt == 0L) backtest.copy(createdAt = System.currentTimeMillis()) else backtest
        val id = backtestDao.upsert(stamped)
        touchSync(SyncTables.BACKTESTS, id)
        return id
    }
    suspend fun addBacktestImage(backtestId: Long, path: String) {
        backtestDao.insertImage(BacktestImage(backtestId = backtestId, path = path))
    }
    suspend fun deleteBacktestImage(image: BacktestImage) = backtestDao.deleteImage(image)
    suspend fun deleteBacktest(backtest: Backtest) {
        tombstoneSync(SyncTables.BACKTESTS, backtest.id)
        backtestDao.imagesOf(backtest.id).forEach { com.tradelog.app.util.ImageStorage.delete(it.path) }
        backtestDao.deleteImagesFor(backtest.id)
        backtestDao.delete(backtest)
    }

    // ---- Economic calendar ----
    suspend fun refreshCalendar(): Result<Int> = runCatching {
        val raw = api.thisWeek()
        val mapped = raw.mapNotNull { it.toEntity() }
        eventDao.clear()
        eventDao.insertAll(mapped)
        settings.setLastSync(System.currentTimeMillis())
        mapped.size
    }

    suspend fun hasCachedEvents(): Boolean = eventDao.count() > 0

    suspend fun highImpactToday(): List<EconomicEvent> {
        val (start, end) = DateUtils.dayEpochBounds()
        return eventDao.highImpactBetween(start, end)
    }

    /** Today's high + medium impact events (for the morning briefing). */
    suspend fun importantToday(): List<EconomicEvent> {
        val (start, end) = DateUtils.dayEpochBounds()
        return eventDao.between(start, end).filter { it.impact == Impact.HIGH || it.impact == Impact.MEDIUM }
    }

    /** High-impact events strictly after today, up to [withinMillis] ahead (e.g. tomorrow's big releases). */
    suspend fun highImpactSoon(withinMillis: Long = 2L * 24 * 60 * 60 * 1000): List<EconomicEvent> {
        val (_, todayEnd) = DateUtils.dayEpochBounds()
        return eventDao.highImpactBetween(todayEnd + 1, System.currentTimeMillis() + withinMillis)
    }

    /** Upcoming high-impact events from now up to [withinMillis] ahead (default 7 days). */
    suspend fun upcomingHighImpact(withinMillis: Long = 7L * 24 * 60 * 60 * 1000): List<EconomicEvent> {
        val now = System.currentTimeMillis()
        return eventDao.highImpactBetween(now, now + withinMillis)
    }

    private fun FFEvent.toEntity(): EconomicEvent? {
        val impact = when (impact.trim().lowercase()) {
            "high" -> Impact.HIGH
            "medium" -> Impact.MEDIUM
            "low" -> Impact.LOW
            "holiday" -> Impact.HOLIDAY
            else -> return null // skip Non-Economic / unknown
        }
        val millis = try {
            OffsetDateTime.parse(date).toInstant().toEpochMilli()
        } catch (e: Exception) {
            return null
        }
        if (title.isBlank() || country.isBlank()) return null
        return EconomicEvent(
            title = title,
            country = country,
            impact = impact,
            dateTimeUtc = millis,
            forecast = forecast,
            previous = previous
        )
    }
}
