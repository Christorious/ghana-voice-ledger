package com.voiceledger.ghana.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end")
    fun observeTotalBetween(start: Long, end: Long): Flow<Double>

    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
