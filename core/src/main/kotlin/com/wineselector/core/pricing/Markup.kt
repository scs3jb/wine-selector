package com.wineselector.core.pricing

import java.net.URLEncoder
import com.wineselector.core.text.TextNormalizer

enum class MarkupVerdict(val label: String) {
    BARGAIN("Close to retail — a bargain"),
    FAIR("Fair markup"),
    TYPICAL("Typical restaurant markup"),
    STEEP("Steep markup"),
    VERY_STEEP("Very steep markup")
}

data class Markup(val ratio: Double, val verdict: MarkupVerdict)

/** Compares a menu's bottle price with the typical retail price. */
object MarkupAnalyzer {
    fun analyze(menuPrice: Double?, menuCurrency: String?, summary: PriceSummary?, halfBottle: Boolean = false): Markup? {
        if (menuPrice == null || summary == null || summary.median <= 0) return null
        if (menuCurrency != null && summary.currencySymbol != null && menuCurrency != summary.currencySymbol) return null
        val retail = if (halfBottle) summary.median / 2 else summary.median
        val ratio = menuPrice / retail
        val verdict = when {
            ratio < 1.4 -> MarkupVerdict.BARGAIN
            ratio < 2.2 -> MarkupVerdict.FAIR
            ratio < 3.2 -> MarkupVerdict.TYPICAL
            ratio < 4.2 -> MarkupVerdict.STEEP
            else -> MarkupVerdict.VERY_STEEP
        }
        return Markup(ratio, verdict)
    }
}

/** Keyless links to compare prices in the browser. */
object PriceLinks {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun wineSearcher(name: String, vintage: Int?): String {
        val slug = TextNormalizer.normalizeForMatching(name).replace(Regex("[^a-z0-9]+"), "+").trim('+')
        return "https://www.wine-searcher.com/find/$slug" + (vintage?.let { "/$it" } ?: "")
    }

    fun vivino(name: String): String = "https://www.vivino.com/search/wines?q=${enc(name)}"

    fun googleShopping(name: String, vintage: Int?): String =
        "https://www.google.com/search?tbm=shop&q=${enc(listOfNotNull(name, vintage?.toString()).joinToString(" "))}"
}
