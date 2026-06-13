package com.tradelog.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tradelog.app.data.dao.AccountDao
import com.tradelog.app.data.dao.EconomicEventDao
import com.tradelog.app.data.dao.GoalDao
import com.tradelog.app.data.dao.JournalDao
import com.tradelog.app.data.dao.NotebookDao
import com.tradelog.app.data.dao.PayoutDao
import com.tradelog.app.data.dao.PositionPresetDao
import com.tradelog.app.data.dao.SetupTagDao
import com.tradelog.app.data.dao.TaskDao
import com.tradelog.app.data.dao.TradeDao
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.EconomicEvent
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
        PositionPreset::class
    ],
    version = 1,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tradelog.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
