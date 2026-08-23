package com.voiceledger.ghana.voice

/** Result of parsing a spoken/typed sale phrase. */
data class ParsedTransaction(
    val description: String,
    val amount: Double,
    val quantity: Int?
)

/**
 * Turns natural sales phrases into a structured entry.
 *
 * Handles the shapes a Ghanaian trader is likely to speak, e.g.:
 *  - "sold 3 tilapia for 20 cedis"
 *  - "two yams 15 cedis"
 *  - "gari 5"
 *
 * This is intentionally a simple, transparent heuristic (no ML) so the core loop works
 * offline and predictably. It embodies the project's core idea — capturing a sale the way
 * it is spoken — on a foundation that actually runs; richer language handling (Twi/Ga/Ewe
 * number words, fuzzy product matching) can be layered on later.
 */
object TransactionParser {

    private val numberWords = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "twenty" to 20, "thirty" to 30,
        "forty" to 40, "fifty" to 50, "hundred" to 100
    )

    private val amountAfterCurrency =
        Regex("""(\d+(?:\.\d+)?)\s*(?:cedis|cedi|ghs|gh₵|₵|gh)\b""")
    private val amountAfterFor = Regex("""\bfor\s+(\d+(?:\.\d+)?)""")
    private val amountTrailing = Regex("""(\d+(?:\.\d+)?)\s*$""")
    private val leadingQuantity = Regex("""\b(\d+)\s+[a-z]""")

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        var normalized = text.lowercase().trim()
        for ((word, value) in numberWords) {
            normalized = normalized.replace(Regex("""\b$word\b"""), value.toString())
        }

        val amount = amountAfterCurrency.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: amountAfterFor.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: amountTrailing.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return null

        val quantity = leadingQuantity.find(normalized)?.groupValues?.get(1)?.toIntOrNull()

        var description = normalized
            .replace(Regex("""\b(sold|bought|buy|sell|for|at|of|a|an)\b"""), " ")
            .replace(Regex("""\d+(?:\.\d+)?"""), " ")
            .replace(Regex("""\b(cedis|cedi|ghs|gh₵|₵|gh)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (description.isBlank()) description = "Sale"
        description = description.replaceFirstChar { it.uppercase() }

        return ParsedTransaction(description = description, amount = amount, quantity = quantity)
    }
}
