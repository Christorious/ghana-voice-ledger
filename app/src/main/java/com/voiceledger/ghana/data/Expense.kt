package com.voiceledger.ghana.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Broad buckets a trader's spending falls into. Stored by [Enum.name]. */
enum class ExpenseCategory(val label: String) {
    STOCK("Stock"),
    TRANSPORT("Transport"),
    RENT("Rent/Toll"),
    UTILITIES("Utilities"),
    WAGES("Wages"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): ExpenseCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/**
 * A single cost the trader paid — restocking goods, transport, table toll, light bill, etc.
 * Kept separate from [Transaction] (sales) so the two never blur; profit is simply the
 * difference between them over a period.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** What the money went on, e.g. "Rice stock", "Trotro". */
    val description: String,

    /** Amount in Ghana Cedis (GHS). */
    val amount: Double,

    /** Which bucket this falls into (stored as [ExpenseCategory.name]). */
    val category: String = ExpenseCategory.OTHER.name,

    /** When the cost was recorded. */
    val timestamp: Long = System.currentTimeMillis(),

    /** The original spoken/typed phrase, kept for transparency and future re-parsing. */
    val rawText: String = ""
)
