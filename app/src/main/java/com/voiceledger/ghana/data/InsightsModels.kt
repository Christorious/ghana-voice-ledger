package com.voiceledger.ghana.data

/** One product's takings over a period — the "best sellers" row. */
data class ProductTotal(
    val name: String,
    val total: Double,
    val count: Int
)

/** Sales for one calendar day — a bar in the trend chart. */
data class DayTotal(
    val day: String,   // "YYYY-MM-DD" (local)
    val total: Double
)
