package com.voiceledger.ghana.voice

/** Result of parsing a spoken/typed credit phrase. */
data class ParsedDebt(
    val customerName: String,
    val amount: Double,
    val note: String
)

/**
 * Parses a credit phrase such as "Ama owes 20 cedis for fish" into a customer name, amount,
 * and note. Amount/number handling is shared with [AmountParser]. Name detection is a
 * best-effort heuristic — the confirm sheet always lets the trader correct it before saving.
 */
object DebtParser {

    private val debtKeyword = Regex("""\b(owes|owe|owing|owed|credit|debt|debit|borrowed|took|collected)\b""")
    private val nameFiller = Regex("""\b(give|gave|to|for|please|mr|mrs|madam|maame|paa|the|owes|owe|owing|owed|credit|debt|debit|borrowed|took|collected)\b""")
    private val noteAfterFor = Regex("""\bfor\s+([a-z][a-z ]*)""")

    fun parse(text: String): ParsedDebt? {
        if (text.isBlank()) return null

        val normalized = AmountParser.wordsToDigits(text.lowercase().trim())
        val amount = AmountParser.extractAmount(normalized) ?: return null

        val name = extractName(normalized)
        val note = noteAfterFor.find(normalized)?.groupValues?.get(1)?.trim()
            ?.replaceFirstChar { it.uppercase() } ?: ""

        return ParsedDebt(customerName = name, amount = amount, note = note)
    }

    private fun extractName(normalized: String): String {
        val firstDigit = normalized.indexOfFirst { it.isDigit() }.let { if (it < 0) normalized.length else it }
        val keyword = debtKeyword.find(normalized)
        val keywordStart = keyword?.range?.first ?: normalized.length

        // Prefer the words before the debt keyword or the first number, whichever comes first.
        var candidate = normalized.substring(0, minOf(firstDigit, keywordStart))
        var name = cleanName(candidate)

        // If nothing usable before (e.g. "credit Ama 20 cedis"), try between the keyword and number.
        if (name.isBlank() && keyword != null && keyword.range.last + 1 < firstDigit) {
            candidate = normalized.substring(keyword.range.last + 1, firstDigit)
            name = cleanName(candidate)
        }

        return name.ifBlank { "Customer" }
    }

    private fun cleanName(raw: String): String =
        raw.replace(nameFiller, " ")
            .replace(Regex("""[^a-z ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
