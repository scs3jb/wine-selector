package com.wineselector.core.analysis

import com.wineselector.core.db.XWineEntry
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.pairing.PairingScore
import com.wineselector.core.scan.LabelText
import com.wineselector.core.vintage.DrinkingWindow
import com.wineselector.core.vintage.VintageInfo
import com.wineselector.core.vintage.VintageMatch
import com.wineselector.core.vintage.VintageRating

/** Everything the app knows about one scanned wine. */
data class AnalyzedWine(
    /** Stable identity within a scan (used for navigation and price lookups). */
    val key: String,
    /** Canonical database name when identified, otherwise the cleaned scanned name. */
    val displayName: String,
    val scannedName: String,
    val scannedDetail: String?,
    val entry: XWineEntry?,
    /** 0..1 confidence that [entry] is the wine that was scanned. */
    val matchConfidence: Float,
    val style: WineStyle?,
    val grapes: List<String>,
    val region: String?,
    val country: String?,
    val vintage: VintageInfo,
    val vintageMatch: VintageMatch?,
    val vintageRating: VintageRating?,
    val drinkingWindow: DrinkingWindow?,
    val menuPrices: List<Double> = emptyList(),
    val currency: String? = null,
    val halfBottle: Boolean = false,
    val pairing: PairingScore? = null,
    val tastingNote: String? = null,
    val lineIndices: List<Int> = emptyList()
) {
    val isIdentified: Boolean get() = entry != null
    val bottlePrice: Double? get() = menuPrices.maxOrNull()
    val glassPrice: Double? get() = if (menuPrices.size >= 2) menuPrices.minOrNull() else null
    val rating: Float? get() = entry?.averageRating
    val winery: String? get() = entry?.wineryName?.takeIf { it.isNotBlank() }

    /** Query used for online price search: producer + wine, without repeating words. */
    val searchName: String
        get() {
            val e = entry ?: return scannedName
            val wineWords = e.wineName.lowercase().split(' ').toSet()
            val wineryPart = e.wineryName.split(' ').filter { it.lowercase() !in wineWords }.joinToString(" ")
            return listOf(wineryPart, e.wineName).filter { it.isNotBlank() }.joinToString(" ")
        }
}

enum class Pick(val label: String) {
    BEST_PAIRING("Best pairing"),
    BEST_VALUE("Best value"),
    TOP_RATED("Top rated")
}

data class MenuAnalysis(
    /** Wines ranked best first (after preference filters). */
    val wines: List<AnalyzedWine>,
    val food: FoodCategory?,
    val hiddenByFilters: Int,
    val currency: String?,
    /** Highlighted wines, by [AnalyzedWine.key]. */
    val picks: Map<Pick, String>
) {
    val identifiedCount: Int get() = wines.count { it.isIdentified }
    fun wine(key: String): AnalyzedWine? = wines.firstOrNull { it.key == key }
    fun picksFor(key: String): List<Pick> = picks.filterValues { it == key }.keys.toList()
}

data class BottleAnalysis(
    val wine: AnalyzedWine,
    /** Other plausible database wines, best first ("Not this one?"). */
    val alternatives: List<AnalyzedWine>,
    val label: LabelText
)
