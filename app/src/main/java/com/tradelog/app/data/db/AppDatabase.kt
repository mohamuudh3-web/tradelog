package com.tradelog.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tradelog.app.data.dao.AccountDao
import com.tradelog.app.data.dao.BacktestDao
import com.tradelog.app.data.dao.ChecklistRuleDao
import com.tradelog.app.data.dao.CountdownDao
import com.tradelog.app.data.dao.CurrencyDao
import com.tradelog.app.data.dao.EconomicEventDao
import com.tradelog.app.data.dao.InstrumentDao
import com.tradelog.app.data.dao.GoalDao
import com.tradelog.app.data.dao.JournalDao
import com.tradelog.app.data.dao.NotebookDao
import com.tradelog.app.data.dao.PayoutDao
import com.tradelog.app.data.dao.PositionPresetDao
import com.tradelog.app.data.dao.SetupTagDao
import com.tradelog.app.data.dao.SyncMetaDao
import com.tradelog.app.data.dao.TaskDao
import com.tradelog.app.data.dao.TradeDao
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.data.entity.Currency
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PositionPreset
import com.tradelog.app.data.entity.SetupTag
import com.tradelog.app.data.entity.SyncMeta
import com.tradelog.app.data.entity.TaskCompletion
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.Trade

@Database(
    entities = [
        Account::class,
        Trade::class,
        JournalEntry::class,
        NotebookNote::class,
        SetupTag::class,
        PayoutRecord::class,
        EconomicEvent::class,
        Goal::class,
        TaskItem::class,
        TaskCompletion::class,
        PositionPreset::class,
        Instrument::class,
        Backtest::class,
        BacktestImage::class,
        ChecklistRule::class,
        Countdown::class,
        SyncMeta::class,
        Currency::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun accountDao(): AccountDao
    abstract fun journalDao(): JournalDao
    abstract fun notebookDao(): NotebookDao
    abstract fun setupTagDao(): SetupTagDao
    abstract fun payoutDao(): PayoutDao
    abstract fun economicEventDao(): EconomicEventDao
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun positionPresetDao(): PositionPresetDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun backtestDao(): BacktestDao
    abstract fun checklistRuleDao(): ChecklistRuleDao
    abstract fun countdownDao(): CountdownDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun currencyDao(): CurrencyDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** v1 -> v2: morning routine category, saved instruments, backtesting journal. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'TASK'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS instruments (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, pipValuePerLot REAL NOT NULL, sortOrder INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_instruments_name ON instruments(name)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS backtests (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, instrument TEXT NOT NULL, dateMillis INTEGER NOT NULL, " +
                        "bias TEXT NOT NULL, notes TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS backtest_images (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "backtestId INTEGER NOT NULL, path TEXT NOT NULL, sortOrder INTEGER NOT NULL)"
                )
            }
        }

        /** v2 -> v3: backtest direction/result/session columns. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE backtests ADD COLUMN direction TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE backtests ADD COLUMN result TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE backtests ADD COLUMN session TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v3 -> v4: trade session/pips/psychology/checklist/urls, backtest pips/checklist/urls, checklist rules. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN session TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN slPips REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN tpPips REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN psychology TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN checkedRules TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN imageUrls TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE backtests ADD COLUMN slPips REAL")
                db.execSQL("ALTER TABLE backtests ADD COLUMN tpPips REAL")
                db.execSQL("ALTER TABLE backtests ADD COLUMN checkedRules TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE backtests ADD COLUMN imageUrls TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS checklist_rules (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "text TEXT NOT NULL, sortOrder INTEGER NOT NULL)"
                )
            }
        }

        /** v4 -> v5: labeled backtest chart-URL slots. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE backtests ADD COLUMN chart5Url TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE backtests ADD COLUMN chart15Url TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v5 -> v6: daily-journal battle-plan fields. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN gratitude TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN battlePlan TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN affirmation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN moodLabel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN focusTasks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN accountBalance REAL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN tradesTarget INTEGER")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN pipsTarget REAL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN riskPercent REAL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN riskAmount REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN challengePhase TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN status TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN website TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN startingBalance REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN splitPercent REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN drawdownPercent REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN targetPercent REAL")
            }
        }

        /** v6 -> v7: correct gold/silver point value (lot = 100oz / 5000oz). Only if still at the old default. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE instruments SET pipValuePerLot = 100 WHERE name = 'XAUUSD' AND pipValuePerLot = 10")
                db.execSQL("UPDATE instruments SET pipValuePerLot = 5000 WHERE name = 'XAGUSD' AND pipValuePerLot = 50")
            }
        }

        /** v7 -> v8: convert the default English checklist rules to the user's Somali wording. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun rename(from: String, to: String) =
                    db.execSQL("UPDATE checklist_rules SET text = ? WHERE text = ?", arrayOf(to, from))
                rename("Find the trend (H1/H4 for S1, 15M for S2/S3, 5M for S4)", "Soo hel Trend-ka (H1/H4 for S1) (15M for S2/S3) (5M for S4)")
                rename("Find the institutional order-flow zone", "Soo hel Zone-ka maamulaya Order Flow-ga suuqa.")
                rename("Wait for the liquidity grab with reversal volume", "Sug in Liquidity-ga lagu jebiyo Reversal Volume muuqda.")
                rename("Confirm volume drives the countertrend break", "Hubi in Volume-ka uu keeno Countertrend Break.")
                rename("Confirm momentum aligns with the direction", "Hubi in Momentum-ku la jaanqaadayo direction-ka.")
            }
        }

        /** v8 -> v9: goal countdowns table. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS countdowns (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, targetDateMillis INTEGER NOT NULL, motivation TEXT NOT NULL, " +
                        "reminderHour INTEGER NOT NULL, reminderMinute INTEGER NOT NULL, " +
                        "reviewDone INTEGER NOT NULL, reachedIt INTEGER NOT NULL, " +
                        "wentWrong TEXT NOT NULL, improveNext TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }

        /** v9 -> v10: cloud-sync side-table mapping local rows to their Supabase uid. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sync_meta (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "tableName TEXT NOT NULL, localId INTEGER NOT NULL, uid TEXT NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, pending INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_meta_tableName_localId ON sync_meta(tableName, localId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_meta_uid ON sync_meta(uid)")
            }
        }

        /** v10 -> v11: per-item daily reminder times on tasks and goals. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE goals ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v11 -> v12: user-managed currency list (shown in every currency dropdown). */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS currencies (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "code TEXT NOT NULL, sortOrder INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_currencies_code ON currencies(code)")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tradelog.db"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
