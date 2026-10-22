package com.voiceledger.ghana.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person who buys on credit. Reused across debts so balances accumulate per customer. */
@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"])]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
