package com.voiceledger.ghana.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single credit given to a customer. `amountPaid` accumulates payments (supports partial
 * payments); a debt is settled once `amountPaid >= amount`.
 */
@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val amountPaid: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val outstanding: Double get() = (amount - amountPaid).coerceAtLeast(0.0)
    val isSettled: Boolean get() = amountPaid >= amount - 0.0049
}
