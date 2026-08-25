package com.voiceledger.ghana.voice

import com.voiceledger.ghana.data.ExpenseCategory

/** Result of parsing a spoken/typed expense phrase. */
data class ParsedExpense(
    val description: String,
    val amount: Double,
    val category: ExpenseCategory
)

/**
 * Turns natural spending phrases into a structured cost, e.g. "bought rice stock for 200 cedis",
 * "paid 5 cedis trotro", "light bill 40". A transparent heuristic (no ML): amounts and number
 * words are handled by the shared [AmountParser]; this adds a keyword-based category guess and
 * strips filler down to a short description.
 */
object ExpenseParser {

    private val fillerPattern =
        Regex("""\b(bought|buy|buying|paid|pay|paying|spent|spend|for|on|at|of|a|an|the|and|to|some|please|i|my|this|today|gave|give)\b""")
    private val currencyWordPattern = Regex("""\b(?:${AmountParser.CEDIS}|${AmountParser.PESEWAS})\b""")
    private val numberPattern = Regex("""\d+(?:\.\d+)?""")

    /** category -> trigger keywords (matched against the normalised phrase). */
    private val categoryKeywords: Map<ExpenseCategory, List<String>> = mapOf(
        ExpenseCategory.STOCK to listOf(
            "stock", "restock", "goods", "supply", "supplies", "wholesale", "carton",
            "cartons", "bag", "bags", "sack", "sacks", "box", "boxes", "crate", "crates"
        ),
        ExpenseCategory.TRANSPORT to listOf(
            "transport", "trotro", "tro", "taxi", "uber", "bolt", "fare", "fuel", "petrol",
            "diesel", "lorry", "truck", "car", "cart", "kaya", "kayayo", "porter"
        ),
        ExpenseCategory.RENT to listOf(
            "rent", "toll", "table", "stall", "store", "shop", "space", "ticket", "council"
        ),
        ExpenseCategory.UTILITIES to listOf(
            "light", "electricity", "ecg", "power", "water", "gas", "airtime", "credit",
            "data", "bundle", "phone", "charge"
        ),
        ExpenseCategory.WAGES to listOf(
            "labour", "labor", "wages", "wage", "salary", "boy", "girl", "help", "helper",
            "worker", "assistant", "apprentice"
        )
    )

    fun parse(text: String): ParsedExpense? {
        if (text.isBlank()) return null

        val normalized = AmountParser.wordsToDigits(text.lowercase().trim())
        val amount = AmountParser.extractAmount(normalized) ?: return null
        val category = guessCategory(normalized)
        val description = extractDescription(normalized).ifBlank { category.label }

        return ParsedExpense(description = description, amount = amount, category = category)
    }

    private fun guessCategory(normalized: String): ExpenseCategory {
        val words = normalized.split(Regex("""\s+""")).toSet()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { it in words }) return category
        }
        return ExpenseCategory.OTHER
    }

    private fun extractDescription(normalized: String): String {
        val stripped = normalized
            .replace(fillerPattern, " ")
            .replace(numberPattern, " ")
            .replace(currencyWordPattern, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return stripped.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }
}
