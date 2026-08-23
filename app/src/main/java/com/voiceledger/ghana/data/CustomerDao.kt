package com.voiceledger.ghana.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE lower(trim(name)) = lower(trim(:name)) LIMIT 1")
    suspend fun findByName(name: String): Customer?

    /** Customers who currently owe something, with their outstanding balance. */
    @Query(
        """
        SELECT c.id AS customerId, c.name AS name, c.phone AS phone,
               COALESCE(SUM(d.amount - d.amountPaid), 0) AS outstanding,
               COUNT(d.id) AS debtCount
        FROM customers c
        JOIN debts d ON d.customerId = c.id
        GROUP BY c.id
        HAVING SUM(d.amount - d.amountPaid) > 0.0049
        ORDER BY outstanding DESC
        """
    )
    fun observeDebtors(): Flow<List<CustomerBalance>>
}
