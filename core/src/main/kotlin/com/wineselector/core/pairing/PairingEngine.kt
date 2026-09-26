package com.wineselector.core.pairing

import com.wineselector.core.db.XWineEntry
import com.wineselector.core.db.XWinesDatabase
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.text.TextNormalizer
import kotlin.math.roundToInt

/** How well a wine suits a food, with human-readable reasons. */
data class PairingScore(val score: Int, val headline: String, val reasons: List<String>)

/**
 * Scores wine/food pairings. The base score comes from grape profiles (the
 * database's grape list when the wine is identified, keywords in the text
 * otherwise), falling back to a style table. X-Wines' own "harmonize" list adds a
 * +2 bonus (capped at 10), and body/acidity nudge the result by one point.
 */
class PairingEngine(private val database: XWinesDatabase? = null) {

    fun score(
        food: FoodCategory,
        entry: XWineEntry?,
        text: String,
        style: WineStyle?
    ): PairingScore {
        val reasons = mutableListOf<String>()
        var headline: String? = null

        val grapeProfiles = entry?.grapes.orEmpty().mapNotNull { GrapeProfiles.forGrape(it) }
        val textProfiles = GrapeProfiles.findInText(text)
        val profiles = grapeProfiles.ifEmpty { textProfiles }

        var base: Int
        if (profiles.isNotEmpty()) {
            val scores = profiles.map { it.profile.scores[food] ?: POOR_MATCH }
            // The lead grape matters most; supporting grapes can lift the score.
            base = (0.6 * scores.first() + 0.4 * scores.max()).roundToInt()
            headline = profiles.first().profile.description
        } else {
            base = STYLE_FALLBACK[style]?.get(food) ?: 4
            if (style != null) reasons += "Scored on its style (${style.label.lowercase()}) — grapes unknown"
        }

        if (entry != null && database?.harmonizesWithFood(entry, food) == true) {
            base += 2
            reasons += "The X-Wines community pairs it with ${food.displayName.lowercase()}"
        }

        val body = TextNormalizer.normalizeForMatching(entry?.body ?: "")
        val acidity = TextNormalizer.normalizeForMatching(entry?.acidity ?: "")
        if ("full" in body && food in RICH_FOODS) { base += 1; reasons += "Full body stands up to rich ${food.displayName.lowercase()}" }
        if ("light" in body && food in RICH_FOODS) { base -= 1; reasons += "Light body may be overpowered" }
        if ("high" in acidity && food in ACID_LOVING_FOODS) { base += 1; reasons += "High acidity keeps the palate fresh" }

        val score = base.coerceIn(1, 10)
        return PairingScore(score, headline ?: verdict(score, food), reasons)
    }

    /** Scores for every food, best first — used on the wine detail screen. */
    fun scoreAll(entry: XWineEntry?, text: String, style: WineStyle?): List<Pair<FoodCategory, PairingScore>> =
        FoodCategory.entries.map { it to score(it, entry, text, style) }.sortedByDescending { it.second.score }

    private fun verdict(score: Int, food: FoodCategory) = when {
        score >= 9 -> "A superb match for ${food.displayName.lowercase()}"
        score >= 7 -> "A good match for ${food.displayName.lowercase()}"
        score >= 5 -> "Works with ${food.displayName.lowercase()}"
        else -> "Not an ideal partner for ${food.displayName.lowercase()}"
    }

    companion object {
        private const val POOR_MATCH = 2
        private val RICH_FOODS = setOf(FoodCategory.BEEF, FoodCategory.LAMB)
        private val ACID_LOVING_FOODS = setOf(FoodCategory.FISH, FoodCategory.SEAFOOD, FoodCategory.SUSHI)

        private fun row(vararg p: Pair<FoodCategory, Int>) = p.toMap()

        val STYLE_FALLBACK: Map<WineStyle, Map<FoodCategory, Int>> = mapOf(
            WineStyle.RED to row(
                FoodCategory.BEEF to 7, FoodCategory.LAMB to 7, FoodCategory.PORK to 6, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.CHICKEN to 5,
                FoodCategory.VEGETARIAN to 5, FoodCategory.FISH to 3, FoodCategory.SEAFOOD to 2,
                FoodCategory.SUSHI to 2, FoodCategory.DESSERT to 2
            ),
            WineStyle.WHITE to row(
                FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7, FoodCategory.SUSHI to 6, FoodCategory.CHICKEN to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.PASTA to 5, FoodCategory.CHEESE to 5, FoodCategory.PORK to 5,
                FoodCategory.PIZZA to 4, FoodCategory.BEEF to 2, FoodCategory.LAMB to 2, FoodCategory.DESSERT to 3
            ),
            WineStyle.ROSE to row(
                FoodCategory.VEGETARIAN to 7, FoodCategory.FISH to 6, FoodCategory.SEAFOOD to 6,
                FoodCategory.CHICKEN to 6, FoodCategory.PIZZA to 6, FoodCategory.PASTA to 6, FoodCategory.SUSHI to 6,
                FoodCategory.PORK to 6, FoodCategory.CHEESE to 5, FoodCategory.BEEF to 4, FoodCategory.LAMB to 4,
                FoodCategory.DESSERT to 3
            ),
            WineStyle.SPARKLING to row(
                FoodCategory.SEAFOOD to 8, FoodCategory.SUSHI to 8, FoodCategory.FISH to 7, FoodCategory.CHEESE to 6,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.PIZZA to 5,
                FoodCategory.PASTA to 5, FoodCategory.PORK to 5, FoodCategory.DESSERT to 5, FoodCategory.BEEF to 3,
                FoodCategory.LAMB to 3
            ),
            WineStyle.DESSERT to row(
                FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 8, FoodCategory.SUSHI to 2, FoodCategory.FISH to 2,
                FoodCategory.SEAFOOD to 2, FoodCategory.BEEF to 2, FoodCategory.LAMB to 2, FoodCategory.PORK to 3,
                FoodCategory.CHICKEN to 2, FoodCategory.PASTA to 2, FoodCategory.PIZZA to 2, FoodCategory.VEGETARIAN to 2
            ),
            WineStyle.FORTIFIED to row(
                FoodCategory.CHEESE to 8, FoodCategory.DESSERT to 8, FoodCategory.BEEF to 3, FoodCategory.PORK to 3,
                FoodCategory.LAMB to 3, FoodCategory.SUSHI to 2, FoodCategory.FISH to 2, FoodCategory.SEAFOOD to 3,
                FoodCategory.CHICKEN to 2, FoodCategory.PASTA to 2, FoodCategory.PIZZA to 2, FoodCategory.VEGETARIAN to 2
            )
        )
    }
}
