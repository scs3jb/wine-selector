package com.wineselector.core.analysis

import com.wineselector.core.TestData
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WinePreferences
import com.wineselector.core.model.WineStyle
import com.wineselector.core.scan.Box
import com.wineselector.core.scan.OcrLine
import com.wineselector.core.scan.OcrPage
import com.wineselector.core.vintage.VintageMatchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WineAnalyzerTest {
    private val bundled = WineAnalyzer(TestData.bundledDb, currentYear = 2026)
    private val slim = WineAnalyzer(TestData.slimDb, currentYear = 2026)

    private fun menu(vararg rows: String) = OcrPage.fromPlainText(rows.joinToString("\n"))

    @Test
    fun `identified wines use the canonical database name`() {
        val a = bundled.analyzeMenu(menu("RED WINES", "Casa Valduga Origem Merlot 2018 34.00", "House Red 22.00"), FoodCategory.BEEF)
        val merlot = a.wines.first { it.isIdentified }
        assertEquals("Origem Merlot", merlot.displayName)
        assertEquals(VintageMatchKind.EXACT, merlot.vintageMatch!!.kind)
        assertEquals(34.0, merlot.bottlePrice)
        assertTrue(a.wines.any { !it.isIdentified && it.displayName == "House Red" })
    }

    @Test
    fun `nearest listed vintage is reported`() {
        val a = bundled.analyzeMenu(menu("Origem Merlot 2009 34.00"), FoodCategory.BEEF)
        val m = a.wines.single().vintageMatch!!
        assertEquals(VintageMatchKind.NEAREST, m.kind)
        assertEquals(2009, m.requestedYear)
        assertEquals(1, m.distance)
    }

    @Test
    fun `ranking follows the pairing score`() {
        val a = bundled.analyzeMenu(
            menu("Estate Cabernet Sauvignon 45", "Crisp Sauvignon Blanc 30", "Moscato 25"),
            FoodCategory.BEEF
        )
        assertTrue(a.wines.first().displayName.contains("Cabernet"))
        assertEquals(a.wines.first().key, a.picks[Pick.BEST_PAIRING])
    }

    @Test
    fun `preferences filter by style and price and report hidden count`() {
        val prefs = WinePreferences(maxPrice = 40, allowedStyles = setOf(WineStyle.RED))
        val a = bundled.analyzeMenu(
            menu("Estate Cabernet Sauvignon 45", "Merlot Reserve 35", "Sauvignon Blanc 30"),
            FoodCategory.BEEF, prefs
        )
        assertEquals(listOf("Merlot Reserve"), a.wines.map { it.displayName })
        assertEquals(2, a.hiddenByFilters)
    }

    @Test
    fun `colour contradiction blocks a database match`() {
        val a = bundled.analyzeMenu(menu("WHITE WINES", "Origem Merlot Blanc 30"), FoodCategory.FISH)
        assertFalse(a.wines.single().isIdentified)
    }

    @Test
    fun `winery on the menu picks the right producer for a shared name`() {
        val a = slim.analyzeMenu(menu("Salvioni Brunello di Montalcino 2016 140"), FoodCategory.BEEF)
        val w = a.wines.single()
        assertEquals("Salvioni", w.entry!!.wineryName)
        assertEquals("tuscany", w.vintageRating!!.regionKey)
        assertEquals(5, w.vintageRating!!.stars)
        assertNotNull(w.drinkingWindow)
    }

    @Test
    fun `generic database names need the producer on the menu`() {
        val a = slim.analyzeMenu(menu(
            "Boatman's Drift Cabernet Sauvignon 20.00",
            "Château Gachon, Bordeaux, France 35.00",
            "Punto Alto Malbec, Mendoza 27.00",
            "Vieux Télégraphe La Crau, Chateauneuf-Du-Pape '15 99"
        ), FoodCategory.BEEF)
        assertTrue(a.wines.none { it.isIdentified })
        val b = slim.analyzeMenu(menu("Caymus Special Selection Cabernet Sauvignon 2016 220"), FoodCategory.BEEF)
        assertEquals("Caymus", b.wines.single().entry?.wineryName)
    }

    @Test
    fun `menu without food ranks by rating`() {
        val a = slim.analyzeMenu(menu("Salvioni Brunello di Montalcino 2016 140", "Mystery Red 2020 30"), null)
        assertTrue(a.wines.first().isIdentified)
        assertNull(a.wines.first().pairing)
    }

    @Test
    fun `junk rows without price vintage or match are dropped`() {
        val a = bundled.analyzeMenu(menu("GALLIPOLI", "Welcome to our restaurant", "Merlot Reserve 35"), FoodCategory.BEEF)
        assertEquals(1, a.wines.size)
    }

    @Test
    fun `real pub menu produces a ranked list`() {
        val a = bundled.analyzeMenu(OcrPage.fromPlainText(TestData.menuText("menu1")), FoodCategory.BEEF)
        assertTrue(a.wines.size >= 18)
        assertTrue((a.wines.first().pairing?.score ?: 0) >= 9)
        assertNotNull(a.picks[Pick.BEST_VALUE])
    }

    // ---- Bottles ----

    private fun label(vararg lines: Pair<String, Int>): OcrPage {
        var top = 0
        val ocr = lines.map { (text, h) -> OcrLine(text, Box(0, top, text.length * h / 2, top + h)).also { top += h + 10 } }
        return OcrPage(ocr, 1000, top)
    }

    @Test
    fun `bottle label is identified from producer and appellation`() {
        val b = slim.analyzeBottle(label(
            "SALVIONI" to 90, "Brunello di Montalcino" to 60, "DENOMINAZIONE DI ORIGINE CONTROLLATA E GARANTITA" to 12,
            "2016" to 40, "14,5% vol" to 12, "750 ml" to 12, "Imbottigliato all'origine da Salvioni" to 10
        ))
        assertEquals("Salvioni", b.wine.entry?.wineryName)
        assertEquals(2016, b.wine.vintage.year)
        assertTrue(b.label.prominentLines.first() == "SALVIONI")
        assertTrue(b.label.prominentLines.none { "vol" in it || "ml" in it })
    }

    @Test
    fun `ambiguous label offers alternatives`() {
        val b = slim.analyzeBottle(label("Brunello di Montalcino" to 60, "2015" to 30))
        assertTrue(b.wine.isIdentified)
        assertTrue(b.alternatives.isNotEmpty())
        assertTrue(b.alternatives.all { it.entry!!.wineName == "Brunello di Montalcino" })
    }

    @Test
    fun `unknown label still gives a named wine with a guessed style`() {
        val b = bundled.analyzeBottle(label("QUINTA DO ZZYZX" to 80, "Vinho Tinto" to 40, "2019" to 30))
        assertFalse(b.wine.isIdentified)
        assertTrue(b.wine.displayName.contains("ZZYZX"))
        assertEquals(WineStyle.RED, b.wine.style)
        assertEquals(2019, b.wine.vintage.year)
    }
}
