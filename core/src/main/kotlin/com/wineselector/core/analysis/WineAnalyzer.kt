package com.wineselector.core.analysis

import com.wineselector.core.db.DbMatchTier
import com.wineselector.core.db.XWineEntry
import com.wineselector.core.db.XWinesDatabase
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WinePreferences
import com.wineselector.core.model.WineStyle
import com.wineselector.core.pairing.GrapeProfiles
import com.wineselector.core.pairing.PairingEngine
import com.wineselector.core.scan.LabelParser
import com.wineselector.core.scan.MenuEntry
import com.wineselector.core.scan.MenuParser
import com.wineselector.core.scan.OcrPage
import com.wineselector.core.text.TextNormalizer
import com.wineselector.core.vintage.DrinkingWindowEstimator
import com.wineselector.core.vintage.VintageGuide
import com.wineselector.core.vintage.VintageInfo
import com.wineselector.core.vintage.VintageMatcher
import java.time.Year

/**
 * The scanning brain: turns OCR output into ranked, fully annotated wines.
 * Menu scans use strict matching (a wrong identification on a wine list is
 * worse than none); bottle scans rank candidates and offer alternatives.
 */
class WineAnalyzer(
    private val database: XWinesDatabase,
    private val pairing: PairingEngine = PairingEngine(database),
    private val currentYear: Int = Year.now().value
) {

    // ---- Menus ---------------------------------------------------------------

    fun analyzeMenu(page: OcrPage, food: FoodCategory?, preferences: WinePreferences = WinePreferences()): MenuAnalysis =
        analyzeMenuEntries(MenuParser.parse(page, currentYear), food, preferences)

    fun analyzeMenuEntries(
        entries: List<MenuEntry>,
        food: FoodCategory?,
        preferences: WinePreferences = WinePreferences()
    ): MenuAnalysis {
        val all = entries.mapIndexedNotNull { index, e ->
            val match = identifyMenuEntry(e)
            val keep = match != null || e.prices.isNotEmpty() || e.vintage.year != null
            if (!keep) null
            else build(
                key = "m$index",
                scannedName = e.name,
                scannedDetail = e.detail,
                text = e.fullText,
                entry = match?.first,
                confidence = match?.second ?: 0f,
                sectionStyle = e.section,
                vintage = e.vintage,
                food = food
            ).copy(
                menuPrices = e.prices,
                currency = e.currency,
                halfBottle = e.halfBottle,
                tastingNote = e.tastingNote,
                lineIndices = e.lineIndices
            )
        }.distinctBy { listOf(it.displayName.lowercase(), it.vintage.year, it.bottlePrice, it.halfBottle) }

        val visible = all.filter { w ->
            preferences.acceptsPrice(w.bottlePrice) && preferences.acceptsStyle(w.style) && preferences.acceptsGrapes(w.grapes)
        }
        val ranked = visible.sortedWith(rankingFor(food))
        return MenuAnalysis(
            wines = ranked,
            food = food,
            hiddenByFilters = all.size - visible.size,
            currency = entries.firstNotNullOfOrNull { it.currency },
            picks = picks(ranked, food)
        )
    }

    /**
     * Strict database identification for a menu entry, with a style sanity check.
     * X-Wines often shares a name across producers ("Brunello di Montalcino"), so a
     * candidate whose winery is also printed on the menu is preferred.
     */
    private fun identifyMenuEntry(e: MenuEntry): Pair<XWineEntry, Float>? {
        val year = e.vintage.year
        val textStyle = WineStyle.detectInText(e.name) ?: e.section
        fun ok(entry: XWineEntry) = !stylesConflict(textStyle, WineStyle.fromDatabaseType(entry.type)) &&
            database.hasDistinctiveEvidence(entry, e.fullText)

        val candidates = database.searchCandidates(e.fullText, limit = 8, minConfidence = 0.5f).filter { ok(it.entry) }
        candidates.firstOrNull { it.nameCoverage >= 1f && it.wineryCoverage >= 0.5f }?.let { best ->
            // Among same-named wines from that winery, prefer one listing the scanned vintage.
            val sameName = candidates.filter { it.entry.wineName == best.entry.wineName && it.entry.wineryName == best.entry.wineryName }
            val pick = sameName.firstOrNull { year != null && year in it.entry.vintages } ?: best
            return pick.entry to maxOf(pick.confidence, 0.9f)
        }

        val tiered = (database.findMatchTiered(e.name, year) ?: database.findMatchTiered(e.fullText, year))
            ?.takeIf { ok(it.entry) }
        if (tiered != null) return tiered.entry to (if (tiered.matchTier == DbMatchTier.EXACT) 0.9f else 0.75f)

        return candidates.firstOrNull { it.nameCoverage >= 1f && it.confidence >= 0.7f }?.let { it.entry to it.confidence }
    }

    /** Only clear colour contradictions count: a "Rioja Blanco" is not the red Rioja. */
    private fun stylesConflict(scanned: WineStyle?, db: WineStyle?): Boolean {
        val colours = setOf(WineStyle.RED, WineStyle.WHITE, WineStyle.ROSE)
        return scanned in colours && db in colours && scanned != db
    }

    private fun rankingFor(food: FoodCategory?): Comparator<AnalyzedWine> {
        val byQuality = compareByDescending<AnalyzedWine> { it.rating ?: 0f }
            .thenByDescending { it.vintageRating?.stars ?: 0 }
            .thenBy { it.displayName.lowercase() }
        return if (food == null) compareByDescending<AnalyzedWine> { it.isIdentified }.then(byQuality)
        else compareByDescending<AnalyzedWine> { it.pairing?.score ?: 0 }.then(byQuality)
    }

    private fun picks(ranked: List<AnalyzedWine>, food: FoodCategory?): Map<Pick, String> {
        val picks = linkedMapOf<Pick, String>()
        if (food != null) ranked.firstOrNull { (it.pairing?.score ?: 0) > 0 }?.let { picks[Pick.BEST_PAIRING] = it.key }
        ranked.filter { it.rating != null }.maxByOrNull { it.rating!! }?.let { picks[Pick.TOP_RATED] = it.key }
        val bestScore = ranked.maxOfOrNull { it.pairing?.score ?: 0 } ?: 0
        ranked.filter { w ->
            w.bottlePrice != null && !w.halfBottle &&
                if (food != null) (w.pairing?.score ?: 0) >= bestScore - 1 else (w.rating ?: 0f) >= 3.8f
        }.minByOrNull { it.bottlePrice!! }?.let { picks[Pick.BEST_VALUE] = it.key }
        return picks
    }

    // ---- Bottles -------------------------------------------------------------

    fun analyzeBottle(page: OcrPage): BottleAnalysis {
        val label = LabelParser.parse(page, currentYear)
        val prominent = TextNormalizer.normalizeForMatching(label.prominentLines.take(3).joinToString(" "))
        val candidates = database.searchCandidates(label.meaningfulText, limit = 8, minConfidence = 0.3f)
            .map { c ->
                val words = TextNormalizer.normalizeForMatching(c.entry.wineName).split(Regex("[^a-z]+"))
                    .filter { it.length > 2 }
                val inProminent = if (words.isEmpty()) 0f else words.count { prominent.contains(it) }.toFloat() / words.size
                c to (c.confidence + 0.15f * inProminent).coerceAtMost(1f)
            }
            .sortedByDescending { it.second }

        val textStyle = WineStyle.detectInText(label.meaningfulText)
        val plausible = candidates.filter { !stylesConflict(textStyle, WineStyle.fromDatabaseType(it.first.entry.type)) }
        val best = plausible.firstOrNull()?.takeIf { it.second >= 0.5f }

        val wine = if (best != null) {
            build("b0", best.first.entry.wineName, null, label.meaningfulText, best.first.entry, best.second,
                null, label.vintage, food = null)
        } else {
            val name = label.guessedName.ifBlank { "Unidentified wine" }
            build("b0", name, null, label.meaningfulText, null, 0f, null, label.vintage, food = null)
        }
        val alternatives = plausible
            .filter { it.first.entry.wineId != best?.first?.entry?.wineId && it.second >= 0.35f }
            .take(4)
            .mapIndexed { i, (c, conf) ->
                build("b${i + 1}", c.entry.wineName, null, label.meaningfulText, c.entry, conf, null, label.vintage, null)
            }
        return BottleAnalysis(wine, alternatives, label)
    }

    /** Re-annotate a wine as a different database entry (user picked an alternative). */
    fun reidentify(wine: AnalyzedWine, entry: XWineEntry, food: FoodCategory?): AnalyzedWine =
        build(wine.key, wine.scannedName, wine.scannedDetail, wine.scannedName, entry, 1f, null, wine.vintage, food)
            .copy(menuPrices = wine.menuPrices, currency = wine.currency, halfBottle = wine.halfBottle,
                tastingNote = wine.tastingNote, lineIndices = wine.lineIndices)

    // ---- Shared annotation -----------------------------------------------------

    private fun build(
        key: String,
        scannedName: String,
        scannedDetail: String?,
        text: String,
        entry: XWineEntry?,
        confidence: Float,
        sectionStyle: WineStyle?,
        vintage: VintageInfo,
        food: FoodCategory?
    ): AnalyzedWine {
        val profiles = GrapeProfiles.findInText(text)
        val style = entry?.let { WineStyle.fromDatabaseType(it.type) }
            ?: WineStyle.detectInText(text)
            ?: sectionStyle
            ?: profiles.firstNotNullOfOrNull { it.profile.style }
        val grapes = entry?.grapes?.takeIf { it.isNotEmpty() }
            ?: profiles.map { it.keyword.split(' ').joinToString(" ") { w -> w.replaceFirstChar(Char::uppercase) } }
        val regionKey = VintageGuide.resolveRegion(entry?.regionName, entry?.country, entry?.wineName, text)
        val stars = VintageGuide.rating(regionKey, vintage.year)
        val window = DrinkingWindowEstimator.estimate(
            vintage.year, style, grapes, entry?.body, regionKey, stars?.stars, currentYear
        )
        return AnalyzedWine(
            key = key,
            displayName = entry?.wineName ?: scannedName,
            scannedName = scannedName,
            scannedDetail = scannedDetail,
            entry = entry,
            matchConfidence = confidence,
            style = style,
            grapes = grapes,
            region = entry?.regionName?.takeIf { it.isNotBlank() } ?: scannedDetail,
            country = entry?.country?.takeIf { it.isNotBlank() },
            vintage = vintage,
            vintageMatch = entry?.let { VintageMatcher.match(vintage, it.vintages) },
            vintageRating = stars,
            drinkingWindow = window,
            pairing = food?.let { pairing.score(it, entry, text, style) }
        )
    }
}
