package com.voiceledger.ghana.data

/** A customer plus their outstanding balance — the row shown on the Credit tab's debtor list. */
data class CustomerBalance(
    val customerId: Long,
    val name: String,
    val phone: String?,
    val outstanding: Double,
    val debtCount: Int
)
