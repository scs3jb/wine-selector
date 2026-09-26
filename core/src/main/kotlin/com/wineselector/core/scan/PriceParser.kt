package com.wineselector.core.scan

import java.time.Year

/** A menu row split into its descriptive text and its price column. */
data class PriceSplit(
    val text: String,
    val prices: List<Double>,
    val currency: String?,
    val halfBottle: Boolean
)

/**
 * Finds prices on a menu row. Handles "£5.75 £7.75 £22.50", "5.50 / 6.75 / 19.50",
 * "13/41", "45", "1,250", "24,50 €" and a leading bin number, while refusing to
 * read vintages ("2019"), volumes ("175ml"), ABV ("13.5%") or bin/cuvée numbers
 * ("Bin 389") as prices.
 */
object PriceParser {

    private val CURRENCY_SYMBOLS = setOf('£', '$', '€', '¥')
    private val PRICE_TOKEN = Regex("""^([£$€¥])?(\d{1,3}(?:,\d{3})+|\d{1,5})(?:([.,])(\d{1,2}))?([£$€¥])?$""")
    private val SEPARATORS = setOf("/", "|", "-", "–", "—", "·", "•", "gl", "glass", "btl", "bt", "bottle", "carafe", "or")
    private val NUMBER_OWNERS = setOf("bin", "no", "no.", "n°", "nº", "#", "cuvee", "cuvée", "lot", "block", "clone", "parcel")
    private val LEADING_BIN = Regex("""^\s*#?(\d{1,4})[.):\-]?\s+(?=\S*[A-Za-zÀ-ÿ'"])""")
    private val HALF_BOTTLE = Regex("""(?i)\(?\s*(?:½|(?<![\d/.])1/2(?![\d/])|\bhalf\b)\s*(?:bottle|btl)?\s*\)?|\b37\.?5\s*cl\b|\b375\s*ml\b""")
    private val SLASH_BETWEEN_NUMBERS = Regex("""(?<=\d)\s*([/|])\s*(?=[£$€¥]?\d)""")

    fun split(row: String, currentYear: Int = Year.now().value): PriceSplit {
        var working = row.replace(' ', ' ')
        val halfBottle = HALF_BOTTLE.containsMatchIn(working)
        if (halfBottle) working = HALF_BOTTLE.replace(working, " ")

        // Leading bin number ("12. Chablis", "105 Barolo") but not a leading vintage.
        LEADING_BIN.find(working)?.let { m ->
            val n = m.groupValues[1].toInt()
            if (n !in 1900..currentYear) working = working.substring(m.range.last + 1)
        }

        working = SLASH_BETWEEN_NUMBERS.replace(working) { " ${it.groupValues[1]} " }
        val tokens = working.split(Regex("""\s+""")).filter { it.isNotEmpty() }.toMutableList()

        val prices = mutableListOf<Double>()
        var currency: String? = null
        var sawSeparatorPattern = false

        // 1. Trailing price zone, scanned from the end.
        var cut = tokens.size
        var i = tokens.size - 1
        while (i >= 0) {
            val raw = tokens[i]
            val tok = raw.trimEnd(',', ';', ':', ')').trimStart('(')
            if (tok.lowercase() in SEPARATORS || tok.isEmpty() || (tok.length == 1 && tok[0] in CURRENCY_SYMBOLS)) {
                if (tok == "/" || tok == "|") sawSeparatorPattern = true
                if (tok.length == 1 && tok[0] in CURRENCY_SYMBOLS) currency = currency ?: tok
                i--; continue
            }
            val parsed = parseToken(tok, currentYear) ?: break
            val owner = tokens.getOrNull(i - 1)?.lowercase()?.trimEnd('.', ':')
            if (owner != null && (owner in NUMBER_OWNERS || "$owner." in NUMBER_OWNERS) && !parsed.hasCurrency && !parsed.hasDecimals) break
            prices.add(0, parsed.value)
            if (parsed.symbol != null) currency = currency ?: parsed.symbol
            cut = i
            i--
        }
        // Drop separators left dangling at the end of the text part.
        while (cut > 0 && tokens[cut - 1].trim(',', ';', ':').let { it.lowercase() in SEPARATORS || it.isEmpty() || (it.length == 1 && it[0] in CURRENCY_SYMBOLS) }) cut--
        val textTokens = tokens.subList(0, cut).toMutableList()

        // 2. Explicitly currency-tagged prices elsewhere in the row.
        val kept = mutableListOf<String>()
        for (raw in textTokens) {
            val tok = raw.trimEnd(',', ';', ':')
            val parsed = parseToken(tok, currentYear)
            if (parsed != null && parsed.hasCurrency) {
                prices.add(parsed.value)
                currency = currency ?: parsed.symbol
            } else kept += raw
        }

        // A lone bare integer is only a price if it looks like one.
        if (prices.size == 1 && !sawSeparatorPattern && currency == null) {
            val original = tokens.getOrNull(cut)?.trimEnd(',', ';', ':') ?: ""
            if (!original.contains('.') && !original.contains(',') && prices[0] < 10) {
                return PriceSplit(row.trim(), emptyList(), null, halfBottle)
            }
        }

        val text = kept.joinToString(" ").trim().trimEnd(',', '-', '–', '/', '|', ':').trim()
        return PriceSplit(text, prices, currency, halfBottle)
    }

    fun hasPrice(row: String): Boolean = split(row).prices.isNotEmpty()

    private class Parsed(val value: Double, val symbol: String?, val hasDecimals: Boolean) {
        val hasCurrency get() = symbol != null
    }

    private fun parseToken(tok: String, currentYear: Int): Parsed? {
        val m = PRICE_TOKEN.matchEntire(tok) ?: return null
        val symbol = m.groupValues[1].ifEmpty { m.groupValues[5] }.ifEmpty { null }
        val intPart = m.groupValues[2].replace(",", "")
        val sep = m.groupValues[3]
        val frac = m.groupValues[4]
        // "1,250" is a thousands separator; "24,50" a decimal comma.
        val value = when {
            frac.isNotEmpty() -> "$intPart.$frac".toDoubleOrNull()
            else -> intPart.toDoubleOrNull()
        } ?: return null
        if (sep.isEmpty() && symbol == null) {
            val n = value.toInt()
            if (n in 1700..currentYear + 1) return null      // a vintage or founding year, not a price
            if (intPart.length > 4) return null               // phone numbers, postcodes
        }
        if (value <= 0.0) return null
        return Parsed(value, symbol, frac.isNotEmpty())
    }
}
