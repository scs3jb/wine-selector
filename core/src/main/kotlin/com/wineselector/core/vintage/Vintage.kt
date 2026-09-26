package com.wineselector.core.vintage

import java.time.Year

/** What a piece of text says about the vintage. */
data class VintageInfo(
    val year: Int? = null,
    val nonVintage: Boolean = false,
    /** The exact text that produced [year] or [nonVintage], e.g. "'19" or "NV". */
    val raw: String? = null
) {
    val isKnown: Boolean get() = year != null || nonVintage

    companion object {
        val UNKNOWN = VintageInfo()
    }
}

/**
 * Extracts vintages from menu or label text while avoiding the usual traps:
 * founding years ("Est. 1887"), prices that look like years, volumes, and
 * future years. Understands abbreviated vintages ('19, ’08) and non-vintage
 * markers (NV, "non-vintage", "sans année").
 */
object VintageParser {

    private val NON_VINTAGE = Regex(
        """(?i)(?:^|[^a-z])(?:n\.?\s?v\.?|non[- ]?vintage|sans\s+ann[ée]e|multi[- ]?vintage|m\.?v\.?)(?=$|[^a-z])"""
    )
    private val FOUR_DIGIT = Regex("""(?<![\d.,£$€/])(1[89]\d{2}|20\d{2})(?![\d]|[.,]\d|\s?(?:ml|cl|l\b|%))""")
    private val ABBREVIATED = Regex("""(?:^|[\s,(])['ʼ‘’`](\d{2})(?![\d%])""")
    private val FOUNDING_CONTEXT = Regex(
        """(?i)(?:est\.?|estd\.?|established|since|founded|fondée?|depuis|dal|desde|seit|anno|circa|c\.)\s*$"""
    )
    private val CURRENCY_BEFORE = Regex("""[£$€]\s*$""")

    fun parse(text: String, currentYear: Int = Year.now().value): VintageInfo {
        if (text.isBlank()) return VintageInfo.UNKNOWN

        val years = FOUR_DIGIT.findAll(text).mapNotNull { m ->
            val year = m.groupValues[1].toInt()
            val before = text.substring(0, m.range.first)
            when {
                year > currentYear || year < 1900 -> null
                FOUNDING_CONTEXT.containsMatchIn(before) -> null
                CURRENCY_BEFORE.containsMatchIn(before) -> null
                else -> m
            }
        }.toList()
        if (years.isNotEmpty()) {
            val m = years.first()
            return VintageInfo(year = m.groupValues[1].toInt(), raw = m.value)
        }

        ABBREVIATED.find(text)?.let { m ->
            val two = m.groupValues[1].toInt()
            val century = if (2000 + two <= currentYear) 2000 else 1900
            return VintageInfo(year = century + two, raw = m.value.trim())
        }

        NON_VINTAGE.find(text)?.let { m ->
            return VintageInfo(nonVintage = true, raw = m.value.trim())
        }
        return VintageInfo.UNKNOWN
    }

    /** Remove the vintage marker found by [parse] from display text. */
    fun strip(text: String, info: VintageInfo): String {
        val raw = info.raw ?: return text
        return text.replaceFirst(raw, " ").replace(Regex("""\s+"""), " ").replace(Regex("""\s+,"""), ",")
            .trim().trim(',', '-', ' ')
    }
}

enum class VintageMatchKind {
    /** The requested vintage is in the database's list for this wine. */
    EXACT,
    /** Requested vintage not listed; [VintageMatch.nearestYear] is the closest listed year. */
    NEAREST,
    /** The database has no vintage list for this wine. */
    UNLISTED,
    /** No vintage in the scanned text. */
    NOT_GIVEN,
    /** The wine is labelled non-vintage. */
    NON_VINTAGE
}

data class VintageMatch(
    val kind: VintageMatchKind,
    val requestedYear: Int? = null,
    val nearestYear: Int? = null,
    val availableYears: List<Int> = emptyList()
) {
    val distance: Int? get() = if (requestedYear != null && nearestYear != null) kotlin.math.abs(requestedYear - nearestYear) else null
}

object VintageMatcher {
    fun match(info: VintageInfo, databaseVintages: List<Int>): VintageMatch {
        val sorted = databaseVintages.distinct().sortedDescending()
        return when {
            info.nonVintage -> VintageMatch(VintageMatchKind.NON_VINTAGE, availableYears = sorted)
            info.year == null -> VintageMatch(VintageMatchKind.NOT_GIVEN, availableYears = sorted)
            sorted.isEmpty() -> VintageMatch(VintageMatchKind.UNLISTED, info.year)
            info.year in sorted -> VintageMatch(VintageMatchKind.EXACT, info.year, info.year, sorted)
            else -> {
                // Ties go to the older vintage: a wine listed as 2018/2020 asked for 2019
                // is more often the earlier release still in circulation.
                val nearest = sorted.minWith(compareBy<Int> { kotlin.math.abs(it - info.year) }.thenBy { it })
                VintageMatch(VintageMatchKind.NEAREST, info.year, nearest, sorted)
            }
        }
    }
}
