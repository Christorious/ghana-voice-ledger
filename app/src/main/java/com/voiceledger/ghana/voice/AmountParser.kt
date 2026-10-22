package com.voiceledger.ghana.voice

/**
 * Shared parsing of Ghanaian money phrases: number words (English + romanised Akan/Twi,
 * Ga, Ewe, incl. simple compounds) and cedis/pesewas amounts. Used by both the sales parser
 * ([TransactionParser]) and the debt parser ([DebtParser]).
 */
object AmountParser {

    val numberWords: Map<String, Int> = buildMap {
        putAll(
            mapOf(
                "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
                "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
                "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
                "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
                "nineteen" to 19
            )
        )
        putAll(
            mapOf(
                "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
                "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
            )
        )
        // Akan / Twi 1-10
        putAll(
            mapOf(
                "baako" to 1, "abien" to 2, "mmienu" to 2, "abiesa" to 3, "mmiensa" to 3,
                "anan" to 4, "enan" to 4, "anum" to 5, "enum" to 5, "asia" to 6,
                "ason" to 7, "awotwe" to 8, "akron" to 9, "edu" to 10
            )
        )
        // Ga 1-10
        putAll(
            mapOf(
                "ekome" to 1, "enyo" to 2, "ete" to 3, "ejwe" to 4, "enumo" to 5,
                "ekpaa" to 6, "kpawo" to 7, "kpaanyo" to 8, "nehu" to 9, "nyongma" to 10
            )
        )
        // Ewe 1-10
        putAll(
            mapOf(
                "deka" to 1, "eve" to 2, "eto" to 3, "ene" to 4, "ato" to 5,
                "ade" to 6, "adre" to 7, "enyi" to 8, "asieke" to 9, "ewo" to 10
            )
        )
    }

    private val hundredWords = setOf("hundred")

    const val CEDIS = """cedis|cedi|ghc|ghs|gh₵|₵|gh"""
    const val PESEWAS = """pesewas|pesewa"""

    private val cedisPattern = Regex("""(\d+(?:\.\d+)?)\s*(?:$CEDIS)\b""")
    private val pesewasPattern = Regex("""(\d+)\s*(?:$PESEWAS)\b""")
    private val forAmountPattern = Regex("""\bfor\s+(\d+(?:\.\d+)?)""")
    private val trailingAmountPattern = Regex("""(\d+(?:\.\d+)?)\s*$""")

    /** Replace number words (incl. compounds) with digits, leaving the rest intact. */
    fun wordsToDigits(text: String): String {
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
                token == "and" && inNumber -> Unit
                else -> {
                    flush()
                    out.append(token).append(' ')
                }
            }
        }
        flush()
        return out.toString().trim()
    }

    /** Extract a cedis amount (with optional pesewas) from digit-normalised text. */
    fun extractAmount(normalized: String): Double? {
        val cedis = cedisPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
        val pesewas = pesewasPattern.find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            cedis != null -> cedis + (pesewas ?: 0).coerceIn(0, 99) / 100.0
            pesewas != null -> pesewas.coerceIn(0, 99) / 100.0
            else -> forAmountPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: trailingAmountPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }
}
