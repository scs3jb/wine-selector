package com.wineselector.core.vintage

import com.wineselector.core.model.WineStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VintageTest {
    private fun parse(s: String) = VintageParser.parse(s, currentYear = 2026)

    @Test fun `four digit vintage`() = assertEquals(2019, parse("Chablis 2019, Domaine X").year)
    @Test fun `abbreviated vintage`() {
        assertEquals(2015, parse("Duckhorn Three Palms, Napa Valley '15 7000").year)
        assertEquals(1998, parse("Château Latour ’98").year)
    }
    @Test fun `non vintage markers`() {
        assertTrue(parse("Bollinger Special Cuvée NV").nonVintage)
        assertTrue(parse("Krug Grande Cuvée (non-vintage)").nonVintage)
        assertTrue(parse("Ruinart Brut N.V.").nonVintage)
    }
    @Test fun `founding year is ignored`() = assertNull(parse("Classic Champagne House, est. 1829").year)
    @Test fun `future year is ignored`() = assertNull(parse("Released 2031").year)
    @Test fun `price is not a vintage`() = assertNull(parse("Merlot £2019.00").year)
    @Test fun `volumes are ignored`() = assertNull(parse("Magnum 1500ml").year)
    @Test fun `first plausible year wins`() = assertEquals(2016, parse("Est. 1850 Barolo 2016").year)
    @Test fun `strip removes the vintage token`() {
        val info = parse("Merlot 'Vuelo' 2021, Tagua Tagua")
        assertEquals("Merlot 'Vuelo', Tagua Tagua", VintageParser.strip("Merlot 'Vuelo' 2021, Tagua Tagua", info))
    }

    @Test fun `matcher exact nearest and unlisted`() {
        val listed = listOf(2020, 2018, 2016)
        assertEquals(VintageMatchKind.EXACT, VintageMatcher.match(VintageInfo(2018), listed).kind)
        val near = VintageMatcher.match(VintageInfo(2019), listed)
        assertEquals(VintageMatchKind.NEAREST, near.kind)
        assertEquals(2018, near.nearestYear)
        assertEquals(1, near.distance)
        assertEquals(VintageMatchKind.UNLISTED, VintageMatcher.match(VintageInfo(2019), emptyList()).kind)
        assertEquals(VintageMatchKind.NOT_GIVEN, VintageMatcher.match(VintageInfo.UNKNOWN, listed).kind)
        assertEquals(VintageMatchKind.NON_VINTAGE, VintageMatcher.match(VintageInfo(nonVintage = true), listed).kind)
    }

    @Test fun `regions resolve from appellations`() {
        assertEquals("bordeaux", VintageGuide.resolveRegion("Pauillac", "France"))
        assertEquals("bordeaux", VintageGuide.resolveRegion("Saint Julien"))
        assertEquals("piedmont", VintageGuide.resolveRegion("Barolo"))
        assertEquals("tuscany", VintageGuide.resolveRegion("Brunello di Montalcino"))
        assertEquals("burgundy", VintageGuide.resolveRegion("Gevrey-Chambertin"))
        assertNull(VintageGuide.resolveRegion("Albariño, Rías Baixas"))
        assertNull(VintageGuide.resolveRegion("Michel Schmitt Piesporter"))
    }

    @Test fun `vintage ratings`() {
        assertEquals(5, VintageGuide.rating("bordeaux", 2010)!!.stars)
        assertEquals(2, VintageGuide.rating("bordeaux", 2013)!!.stars)
        assertNull(VintageGuide.rating("bordeaux", 1987))
        assertNull(VintageGuide.rating(null, 2010))
        assertTrue(2016 in VintageGuide.greatYears("piedmont"))
    }

    @Test fun `great barolo is too young early and peaks later`() {
        val young = DrinkingWindowEstimator.estimate(2019, WineStyle.RED, listOf("Nebbiolo"), "Full-bodied", "piedmont", 5, 2022)!!
        assertEquals(DrinkingStatus.TOO_YOUNG, young.status)
        val later = DrinkingWindowEstimator.estimate(2019, WineStyle.RED, listOf("Nebbiolo"), "Full-bodied", "piedmont", 5, 2038)!!
        assertEquals(DrinkingStatus.PEAK, later.status)
    }

    @Test fun `light whites are for drinking now and fade`() {
        val now = DrinkingWindowEstimator.estimate(2024, WineStyle.WHITE, listOf("Sauvignon Blanc"), null, null, null, 2025)!!
        assertEquals(DrinkingStatus.DRINK_NOW, now.status)
        val old = DrinkingWindowEstimator.estimate(2015, WineStyle.WHITE, listOf("Pinot Grigio"), null, null, null, 2025)!!
        assertEquals(DrinkingStatus.PAST_PEAK, old.status)
    }

    @Test fun `great vintages live longer than weak ones`() {
        val great = DrinkingWindowEstimator.estimate(2010, WineStyle.RED, listOf("Cabernet Sauvignon"), null, "bordeaux", 5, 2025)!!
        val weak = DrinkingWindowEstimator.estimate(2013, WineStyle.RED, listOf("Cabernet Sauvignon"), null, "bordeaux", 2, 2025)!!
        assertTrue(great.toYear - 2010 > weak.toYear - 2013)
        assertNotNull(great)
    }

    @Test fun `no vintage no window`() = assertNull(DrinkingWindowEstimator.estimate(null, WineStyle.RED))
}
