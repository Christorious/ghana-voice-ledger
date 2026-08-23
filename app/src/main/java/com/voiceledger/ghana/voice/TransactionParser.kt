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
 * Handles the shapes a Ghanaian trader is likely to speak or type, e.g.:
 *  - "sold 3 tilapia for 20 cedis"
 *  - "two yams 15 cedis"
 *  - "twenty five cedis rice"
 *  - "5 cedis 50 pesewas bread"
 *  - "baako kenkey for 3 cedis"        (Twi number word)
 *
 * A deliberately transparent heuristic (no ML) so the core loop works offline and
 * predictably. It leans into the project's core idea — capturing a sale the way it is
 * spoken, in Ghana Cedis — with three Ghana-specific extensions:
 *   1. number words in English **and** romanised Akan/Twi, Ga and Ewe (1-10),
 *   2. cedis + pesewas amounts (fractional cedis),
 *   3. canonicalisation of common market products (incl. frequent speech-to-text variants).
 */
object TransactionParser {

    // --- Number words ------------------------------------------------------------------

    private val numberWords: Map<String, Int> = buildMap {
        // English units and teens
        putAll(
            mapOf(
                "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
                "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
                "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
                "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
                "nineteen" to 19
            )
        )
        // English tens
        putAll(
            mapOf(
                "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
                "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
            )
        )
        // Akan / Twi 1-10 (romanised, diacritics dropped; common variants included)
        putAll(
            mapOf(
                "baako" to 1, "abien" to 2, "mmienu" to 2, "abiesa" to 3, "mmiensa" to 3,
                "anan" to 4, "enan" to 4, "anum" to 5, "enum" to 5, "asia" to 6,
                "ason" to 7, "awotwe" to 8, "akron" to 9, "edu" to 10
            )
        )
        // Ga 1-10 (romanised)
        putAll(
            mapOf(
                "ekome" to 1, "enyo" to 2, "ete" to 3, "ejwe" to 4, "enumo" to 5,
                "ekpaa" to 6, "kpawo" to 7, "kpaanyo" to 8, "nehu" to 9, "nyongma" to 10
            )
        )
        // Ewe 1-10 (romanised)
        putAll(
            mapOf(
                "deka" to 1, "eve" to 2, "eto" to 3, "ene" to 4, "ato" to 5,
                "ade" to 6, "adre" to 7, "enyi" to 8, "asieke" to 9, "ewo" to 10
            )
        )
    }

    private val hundredWords = setOf("hundred")

    // --- Currency / filler -------------------------------------------------------------

    private const val CEDIS = """cedis|cedi|ghc|ghs|gh₵|₵|gh"""
    private const val PESEWAS = """pesewas|pesewa"""

    private val cedisPattern = Regex("""(\d+(?:\.\d+)?)\s*(?:$CEDIS)\b""")
    private val pesewasPattern = Regex("""(\d+)\s*(?:$PESEWAS)\b""")
    private val forAmountPattern = Regex("""\bfor\s+(\d+(?:\.\d+)?)""")
    private val trailingAmountPattern = Regex("""(\d+(?:\.\d+)?)\s*$""")
    private val quantityPattern =
        Regex("""\b(\d+)\s+(?!(?:$CEDIS|$PESEWAS|for)\b)[a-z]""")

    private val fillerPattern =
        Regex("""\b(sold|sell|selling|bought|buy|buying|paid|pay|got|received|for|at|of|a|an|the|and|to|some|please|i)\b""")
    private val currencyWordPattern = Regex("""\b(?:$CEDIS|$PESEWAS)\b""")
    private val numberPattern = Regex("""\d+(?:\.\d+)?""")

    // --- Product canonicalisation ------------------------------------------------------

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

    // --- Public API --------------------------------------------------------------------

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        val normalized = wordsToDigits(text.lowercase().trim())

        val amount = extractAmount(normalized) ?: return null
        val quantity = quantityPattern.find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        val description = extractDescription(normalized)

        return ParsedTransaction(description = description, amount = amount, quantity = quantity)
    }

    // --- Internals ---------------------------------------------------------------------

    /**
     * Replace number words (English + local, including simple compounds like "twenty five"
     * and "one hundred twenty") with digits, leaving the rest of the text intact.
     */
    private fun wordsToDigits(text: String): String {
        val tokens = text.split(Regex("""\s+"""))
        val out = StringBuilder()
        var current = 0
        var inNumber = false

        fun flush() {
            if (inNumber) {
                out.append(current).append(' ')
                current = 0
                inNumber = false
            }
        }

        for (token in tokens) {
            val value = numberWords[token]
            when {
                value != null -> {
                    current += value
                    inNumber = true
                }
                token in hundredWords -> {
                    current = (if (current == 0) 1 else current) * 100
                    inNumber = true
                }
                token == "and" && inNumber -> {
                    // keep the current number open, e.g. "hundred and fifty"
                }
                else -> {
                    flush()
                    out.append(token).append(' ')
                }
            }
        }
        flush()
        return out.toString().trim()
    }

    private fun extractAmount(normalized: String): Double? {
        val cedis = cedisPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
        val pesewas = pesewasPattern.find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            cedis != null -> cedis + (pesewas ?: 0).coerceIn(0, 99) / 100.0
            pesewas != null -> pesewas.coerceIn(0, 99) / 100.0
            else -> forAmountPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: trailingAmountPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
        }
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
            .joinToString(" ") { word -> variantToCanonical[word] ?: titleCase(word) }
    }

    private fun titleCase(word: String): String =
        word.replaceFirstChar { it.uppercase() }
}
