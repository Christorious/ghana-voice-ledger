package com.voiceledger.ghana.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Transaction::class, Customer::class, Debt::class],
    version = 2,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun customerDao(): CustomerDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var instance: LedgerDatabase? = null

        fun get(context: Context): LedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "voice_ledger.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
