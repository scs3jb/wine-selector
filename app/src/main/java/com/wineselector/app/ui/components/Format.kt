package com.wineselector.app.ui.components

import com.wineselector.core.analysis.AnalyzedWine
import com.wineselector.core.vintage.VintageMatchKind
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

fun localCurrencySymbol(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrDefault("$")

/** "£22.50", "£22", "$1,250". */
fun formatPrice(value: Double, symbol: String? = null): String {
    val s = symbol ?: localCurrencySymbol()
    val whole = value % 1.0 == 0.0
    val num = if (whole) "%,d".format(value.roundToInt()) else "%,.2f".format(value)
    return "$s$num"
}

fun AnalyzedWine.menuPriceLabel(): String? {
    val bottle = bottlePrice ?: return null
    val glass = glassPrice
    val bottleText = formatPrice(bottle, currency) + if (halfBottle) " (½ btl)" else ""
    return if (glass != null && glass != bottle) "${formatPrice(glass, currency)} glass · $bottleText" else bottleText
}

fun AnalyzedWine.vintageLabel(): String = when {
    vintage.nonVintage -> "NV"
    vintage.year != null -> vintage.year.toString()
    else -> "Vintage n/a"
}

fun AnalyzedWine.vintageMatchText(): String? {
    val m = vintageMatch ?: return null
    return when (m.kind) {
        VintageMatchKind.EXACT -> "${m.requestedYear} is a recorded vintage of this wine"
        VintageMatchKind.NEAREST ->
            "${m.requestedYear} isn't in the database — closest recorded vintage is ${m.nearestYear}" +
                if ((m.distance ?: 0) > 3) ". Treat details with care." else ""
        VintageMatchKind.UNLISTED -> "The database doesn't list vintages for this wine"
        VintageMatchKind.NOT_GIVEN -> m.availableYears.takeIf { it.isNotEmpty() }?.let {
            "No vintage shown · recorded vintages ${it.last()}–${it.first()}"
        }
        VintageMatchKind.NON_VINTAGE -> "Non-vintage blend — consistent house style year to year"
    }
}

fun AnalyzedWine.subtitle(): String =
    listOfNotNull(winery, region, country).distinct().filter { it.isNotBlank() }.joinToString(" · ")
