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
import com.tradelog.app.data.dao.EconomicEventDao
import com.tradelog.app.data.dao.InstrumentDao
import com.tradelog.app.data.dao.GoalDao
import com.tradelog.app.data.dao.JournalDao
import com.tradelog.app.data.dao.NotebookDao
import com.tradelog.app.data.dao.PayoutDao
import com.tradelog.app.data.dao.PositionPresetDao
import com.tradelog.app.data.dao.SetupTagDao
import com.tradelog.app.data.dao.TaskDao
import com.tradelog.app.data.dao.TradeDao
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
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
        BacktestImage::class
    ],
    version = 3,
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

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tradelog.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
