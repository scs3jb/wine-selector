package com.wineselector.core.vintage

import com.wineselector.core.model.WineStyle
import com.wineselector.core.text.TextNormalizer
import java.time.Year
import kotlin.math.roundToInt

enum class DrinkingStatus(val label: String) {
    TOO_YOUNG("Too young — cellar it"),
    APPROACHING("Approachable, still improving"),
    PEAK("At its peak"),
    DRINK_UP("Drink up soon"),
    PAST_PEAK("Likely past its best"),
    DRINK_NOW("Made to drink now")
}

data class DrinkingWindow(
    val fromYear: Int,
    val toYear: Int,
    val peakFrom: Int,
    val peakTo: Int,
    val status: DrinkingStatus,
    /** 0..1 position of the current year within [fromYear, toYear] (clamped). */
    val progress: Float
)

/**
 * Estimates when a wine should be drunk from its style, grapes, body and region,
 * nudged by the vintage's quality. Heuristic, but far better than nothing for a
 * diner deciding between a 2021 and a 2012.
 */
object DrinkingWindowEstimator {

    private data class Ageing(val start: Int, val end: Int)

    private val AGE_WORTHY_REGIONS = setOf("bordeaux", "piedmont", "douro")
    private val SERIOUS_REGIONS = setOf("burgundy", "rhone", "tuscany", "rioja", "napa", "germany")

    fun estimate(
        vintage: Int?,
        style: WineStyle?,
        grapes: List<String> = emptyList(),
        body: String? = null,
        regionKey: String? = null,
        vintageStars: Int? = null,
        currentYear: Int = Year.now().value
    ): DrinkingWindow? {
        if (vintage == null) return null
        val g = TextNormalizer.normalizeForMatching(grapes.joinToString(" "))
        val b = TextNormalizer.normalizeForMatching(body ?: "")
        val full = "full" in b
        val light = "light" in b || "very light" in b

        var ageing = when (style) {
            WineStyle.ROSE -> Ageing(0, 2)
            WineStyle.SPARKLING -> if (regionKey == "champagne") Ageing(4, 18) else Ageing(0, 3)
            WineStyle.DESSERT -> Ageing(3, 25)
            WineStyle.FORTIFIED -> if (regionKey == "douro") Ageing(12, 45) else Ageing(0, 20)
            WineStyle.WHITE -> when {
                "riesling" in g -> Ageing(2, 15)
                "chenin" in g || "semillon" in g -> Ageing(2, 12)
                "chardonnay" in g && (full || regionKey == "burgundy") -> Ageing(2, 9)
                "sauvignon blanc" in g || "pinot grigio" in g || "pinot gris" in g || "albarino" in g ||
                    "vinho verde" in g || "muscadet" in g || light -> Ageing(0, 3)
                else -> Ageing(1, 5)
            }
            WineStyle.RED, null -> when {
                "nebbiolo" in g || "aglianico" in g -> Ageing(6, 25)
                "cabernet sauvignon" in g || "syrah" in g || "shiraz" in g || "mourvedre" in g ||
                    "monastrell" in g || "touriga" in g || "tannat" in g -> Ageing(4, 18)
                "pinot noir" in g -> Ageing(2, 10)
                "gamay" in g || "dolcetto" in g || light -> Ageing(0, 5)
                full -> Ageing(3, 15)
                else -> Ageing(2, 10)
            }
        }

        // Classic regions make longer-lived versions of the same grapes.
        if (style == WineStyle.RED || style == null) {
            if (regionKey in AGE_WORTHY_REGIONS) ageing = Ageing(ageing.start + 3, minOf(ageing.end + 10, 30))
            else if (regionKey in SERIOUS_REGIONS) ageing = Ageing(ageing.start + 1, minOf(ageing.end + 4, 25))
        }

        // Great vintages live longer; weak ones fade sooner.
        val span = ageing.end - ageing.start
        val adjust = when (vintageStars) {
            5 -> (span * 0.25).roundToInt()
            4 -> (span * 0.1).roundToInt()
            2 -> -(span * 0.2).roundToInt()
            1 -> -(span * 0.35).roundToInt()
            else -> 0
        }
        val from = vintage + ageing.start
        val to = maxOf(from + 1, vintage + ageing.end + adjust)
        val total = to - from
        val peakFrom = from + (total * 0.35).roundToInt()
        val peakTo = from + (total * 0.8).roundToInt()

        val status = when {
            ageing.end <= 3 && currentYear <= to -> DrinkingStatus.DRINK_NOW
            currentYear < from -> DrinkingStatus.TOO_YOUNG
            currentYear < peakFrom -> DrinkingStatus.APPROACHING
            currentYear <= peakTo -> DrinkingStatus.PEAK
            currentYear <= to -> DrinkingStatus.DRINK_UP
            else -> DrinkingStatus.PAST_PEAK
        }
        val progress = ((currentYear - from).toFloat() / (to - from).toFloat()).coerceIn(0f, 1f)
        return DrinkingWindow(from, to, peakFrom, peakTo, status, progress)
    }
}
