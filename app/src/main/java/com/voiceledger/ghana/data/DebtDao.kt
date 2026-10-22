package com.voiceledger.ghana.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Insert
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("SELECT * FROM debts WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeByCustomer(customerId: Long): Flow<List<Debt>>

    @Query("SELECT COALESCE(SUM(amount - amountPaid), 0) FROM debts")
    fun observeTotalOutstanding(): Flow<Double>
}
