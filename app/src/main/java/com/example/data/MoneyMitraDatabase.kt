package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DbUser::class,
        UserSession::class,
        DbTransaction::class,
        DbInvestmentPlan::class,
        DbDailyTask::class
    ],
    version = 7,
    exportSchema = false
)
abstract class MoneyMitraDatabase : RoomDatabase() {
    abstract fun moneyMitraDao(): MoneyMitraDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyMitraDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MoneyMitraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyMitraDatabase::class.java,
                    "money_mitra_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
