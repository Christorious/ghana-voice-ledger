package com.voiceledger.ghana.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single ledger entry — one recorded sale.
 *
 * Deliberately minimal for the core app. The salvaged full entity carried many extra
 * fields (speaker confidence, market session, sync flags, etc.); those can be reintroduced
 * as the corresponding features are rebuilt.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Product or free-text description of the sale. */
    val description: String,

    /** Amount in Ghana Cedis (GHS). */
    val amount: Double,

    /** Optional quantity sold. */
    val quantity: Int? = null,

    /** When the sale was recorded. */
    val timestamp: Long = System.currentTimeMillis(),

    /** The original spoken/typed phrase, kept for transparency and future re-parsing. */
    val rawText: String = ""
)
