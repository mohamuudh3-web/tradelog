package com.tradelog.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PositionPreset
import com.tradelog.app.data.entity.SetupTag
import com.tradelog.app.data.entity.TaskCompletion
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.Trade
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<Trade>>

    @Query("SELECT * FROM trades ORDER BY openedAt ASC")
    suspend fun getAllAsc(): List<Trade>

    @Query("SELECT * FROM trades WHERE id = :id")
    fun observeById(id: Long): Flow<Trade?>

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getById(id: Long): Trade?

    @Query("SELECT COUNT(*) FROM trades WHERE openedAt BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: Trade): Long

    @Update
    suspend fun update(trade: Trade)

    @Delete
    suspend fun delete(trade: Trade)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Upsert
    suspend fun upsert(account: Account): Long

    @Delete
    suspend fun delete(account: Account)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<JournalEntry?>

    @Query("SELECT COUNT(*) FROM journal_entries WHERE date BETWEEN :start AND :end")
    suspend fun countBetween(start: String, end: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NotebookNote>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR tags LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<NotebookNote>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NotebookNote?

    @Upsert
    suspend fun upsert(note: NotebookNote): Long

    @Delete
    suspend fun delete(note: NotebookNote)
}

@Dao
interface SetupTagDao {
    @Query("SELECT * FROM setup_tags ORDER BY name ASC")
    fun observeAll(): Flow<List<SetupTag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: SetupTag): Long

    @Delete
    suspend fun delete(tag: SetupTag)
}

@Dao
interface PayoutDao {
    @Query("SELECT * FROM payouts ORDER BY date DESC")
    fun observeAll(): Flow<List<PayoutRecord>>

    @Query("SELECT * FROM payouts WHERE id = :id")
    suspend fun getById(id: Long): PayoutRecord?

    @Upsert
    suspend fun upsert(payout: PayoutRecord): Long

    @Delete
    suspend fun delete(payout: PayoutRecord)
}

@Dao
interface EconomicEventDao {
    @Query("SELECT * FROM economic_events ORDER BY dateTimeUtc ASC")
    fun observeAll(): Flow<List<EconomicEvent>>

    @Query("SELECT * FROM economic_events WHERE dateTimeUtc BETWEEN :start AND :end AND impact = 'HIGH' ORDER BY dateTimeUtc ASC")
    suspend fun highImpactBetween(start: Long, end: Long): List<EconomicEvent>

    @Query("SELECT * FROM economic_events WHERE dateTimeUtc BETWEEN :start AND :end ORDER BY dateTimeUtc ASC")
    suspend fun between(start: Long, end: Long): List<EconomicEvent>

    @Query("SELECT COUNT(*) FROM economic_events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EconomicEvent>)

    @Query("DELETE FROM economic_events")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY archived ASC, createdAt ASC")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): Goal?

    @Upsert
    suspend fun upsert(goal: Goal): Long

    @Delete
    suspend fun delete(goal: Goal)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskItem?

    @Upsert
    suspend fun upsert(task: TaskItem): Long

    @Delete
    suspend fun delete(task: TaskItem)

    @Insert
    suspend fun insertCompletion(c: TaskCompletion)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND date = :date")
    suspend fun removeCompletion(taskId: Long, date: String)

    @Query("SELECT COUNT(*) FROM task_completions WHERE date BETWEEN :start AND :end")
    suspend fun countCompletionsBetween(start: String, end: String): Int
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetDateMillis ASC")
    fun observeAll(): Flow<List<Countdown>>

    @Query("SELECT * FROM countdowns ORDER BY targetDateMillis ASC")
    suspend fun getAll(): List<Countdown>

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun getById(id: Long): Countdown?

    @Upsert
    suspend fun upsert(countdown: Countdown): Long

    @Delete
    suspend fun delete(countdown: Countdown)
}

@Dao
interface ChecklistRuleDao {
    @Query("SELECT * FROM checklist_rules ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ChecklistRule>>

    @Query("SELECT COUNT(*) FROM checklist_rules")
    suspend fun count(): Int

    @Insert
    suspend fun insert(rule: ChecklistRule): Long

    @Delete
    suspend fun delete(rule: ChecklistRule)
}

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<Instrument>>

    @Query("SELECT COUNT(*) FROM instruments")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(instrument: Instrument): Long

    @Upsert
    suspend fun upsert(instrument: Instrument): Long

    @Delete
    suspend fun delete(instrument: Instrument)
}

@Dao
interface BacktestDao {
    @Query("SELECT * FROM backtests ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<Backtest>>

    @Query("SELECT * FROM backtests WHERE id = :id")
    suspend fun getById(id: Long): Backtest?

    @Query("SELECT COUNT(*) FROM backtests WHERE createdAt BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Upsert
    suspend fun upsert(backtest: Backtest): Long

    @Delete
    suspend fun delete(backtest: Backtest)

    @Query("SELECT * FROM backtest_images WHERE backtestId = :backtestId ORDER BY sortOrder ASC, id ASC")
    fun observeImages(backtestId: Long): Flow<List<BacktestImage>>

    @Query("SELECT * FROM backtest_images WHERE backtestId = :backtestId ORDER BY sortOrder ASC, id ASC")
    suspend fun imagesOf(backtestId: Long): List<BacktestImage>

    @Query("SELECT * FROM backtest_images ORDER BY id DESC")
    fun observeAllImages(): Flow<List<BacktestImage>>

    @Insert
    suspend fun insertImage(image: BacktestImage): Long

    @Delete
    suspend fun deleteImage(image: BacktestImage)

    @Query("DELETE FROM backtest_images WHERE backtestId = :backtestId")
    suspend fun deleteImagesFor(backtestId: Long)
}

@Dao
interface PositionPresetDao {
    @Query("SELECT * FROM position_presets ORDER BY name ASC")
    fun observeAll(): Flow<List<PositionPreset>>

    @Upsert
    suspend fun upsert(preset: PositionPreset): Long

    @Delete
    suspend fun delete(preset: PositionPreset)
}
