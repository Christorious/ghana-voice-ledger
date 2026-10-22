package com.voiceledger.ghana.voice

/** Result of parsing a spoken/typed sale phrase. */
data class ParsedTransaction(
    val description: String,
    val amount: Double,
    val quantity: Int?
)

/**
 * Turns natural sales phrases into a structured entry, e.g. "sold 3 tilapia for 20 cedis".
 *
 * A transparent heuristic (no ML). Number words and cedis/pesewas amounts are handled by the
 * shared [AmountParser]; this adds quantity detection and canonicalisation of common market
 * products (including frequent speech-to-text mishears).
 */
object TransactionParser {

    private val quantityPattern =
        Regex("""\b(\d+)\s+(?!(?:${AmountParser.CEDIS}|${AmountParser.PESEWAS}|for)\b)[a-z]""")

    private val fillerPattern =
        Regex("""\b(sold|sell|selling|bought|buy|buying|paid|pay|got|received|for|at|of|a|an|the|and|to|some|please|i)\b""")
    private val currencyWordPattern = Regex("""\b(?:${AmountParser.CEDIS}|${AmountParser.PESEWAS})\b""")
    private val numberPattern = Regex("""\d+(?:\.\d+)?""")

    /** canonical name -> spoken/typed variants (incl. common speech-to-text mishears). */
    private val productVariants: Map<String, List<String>> = mapOf(
        "Tilapia" to listOf("tilapia", "talapia", "tilape", "tilapa"),
        "Fish" to listOf("fish"),
        "Gari" to listOf("gari", "garri"),
        "Kenkey" to listOf("kenkey", "kenke", "kenkei"),
        "Banku" to listOf("banku", "banko"),
        "Fufu" to listOf("fufu", "foofoo", "foufou"),
        "Waakye" to listOf("waakye", "wakye", "wache"),
        "Koko" to listOf("koko", "porridge"),
        "Kelewele" to listOf("kelewele"),
        "Yam" to listOf("yam", "yams"),
        "Plantain" to listOf("plantain", "plantains", "plantin"),
        "Cassava" to listOf("cassava"),
        "Maize" to listOf("maize", "corn"),
        "Rice" to listOf("rice"),
        "Beans" to listOf("beans", "bean"),
        "Groundnut" to listOf("groundnut", "groundnuts", "peanut", "peanuts"),
        "Tomatoes" to listOf("tomato", "tomatoes", "tomatoe"),
        "Onions" to listOf("onion", "onions"),
        "Pepper" to listOf("pepper", "peppe"),
        "Okro" to listOf("okro", "okra"),
        "Eggs" to listOf("egg", "eggs"),
        "Bread" to listOf("bread"),
        "Sugar" to listOf("sugar"),
        "Salt" to listOf("salt"),
        "Oil" to listOf("oil")
    )

    private val variantToCanonical: Map<String, String> = buildMap {
        for ((canonical, variants) in productVariants) {
            for (variant in variants) put(variant, canonical)
        }
    }

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        val normalized = AmountParser.wordsToDigits(text.lowercase().trim())
        val amount = AmountParser.extractAmount(normalized) ?: return null
        val quantity = quantityPattern.find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        val description = extractDescription(normalized)

        return ParsedTransaction(description = description, amount = amount, quantity = quantity)
    }

    private fun extractDescription(normalized: String): String {
        val stripped = normalized
            .replace(fillerPattern, " ")
            .replace(numberPattern, " ")
            .replace(currencyWordPattern, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (stripped.isBlank()) return "Sale"

        return stripped.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> variantToCanonical[word] ?: word.replaceFirstChar { it.uppercase() } }
    }
}
