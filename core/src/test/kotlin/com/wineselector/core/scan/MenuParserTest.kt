package com.wineselector.core.scan

import com.wineselector.core.TestData
import com.wineselector.core.model.WineStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuParserTest {

    private fun parse(fixture: String) = MenuParser.parse(OcrPage.fromPlainText(TestData.menuText(fixture)), 2026)
    private fun List<MenuEntry>.named(name: String) = first { it.name == name }

    @Test
    fun `pub menu - grape continuation lines join the name`() {
        val entries = parse("menu1")
        val hh = entries.named("Hamilton Heights Unoaked Chardonnay")
        assertEquals("Australia", hh.detail)
        assertEquals(listOf(6.5, 8.5, 24.5), hh.prices)
        assertEquals(24.5, hh.bottlePrice)
        assertEquals(6.5, hh.glassPrice)
        assertEquals(WineStyle.WHITE, hh.section)
        val cab = entries.named("Boatman's Drift Cabernet Sauvignon")
        assertEquals(WineStyle.RED, cab.section)
    }

    @Test
    fun `pub menu - tasting notes never leak into names`() {
        val entries = parse("menu1")
        assertTrue(entries.none { it.name.startsWith("Dry") || it.name.startsWith("Smooth") || it.name.startsWith("Enjoy") })
        assertNotNull(entries.named("San Antini Pinot Grigio").tastingNote)
    }

    @Test
    fun `pub menu - half bottles are flagged separately`() {
        val chablis = parse("menu1").filter { it.name == "Domaine Gautheron Chablis" }
        assertEquals(2, chablis.size)
        assertTrue(chablis.any { it.halfBottle && it.bottlePrice == 21.0 })
        assertTrue(chablis.any { !it.halfBottle && it.bottlePrice == 40.0 })
    }

    @Test
    fun `name line followed by priced region line`() {
        val entries = parse("menu5")
        val merlot = entries.named("Merlot Vuelo, Tagua Tagua")
        assertEquals(2021, merlot.vintage.year)
        assertEquals("Rapel Valley, Chile", merlot.detail)
        assertEquals(listOf(5.75, 7.75, 22.5), merlot.prices)
        assertEquals("£", merlot.currency)
        assertEquals(WineStyle.RED, merlot.section)
    }

    @Test
    fun `priced tasting note completes the entry above`() {
        val port = parse("menu5").first { it.name.startsWith("House Port") }
        assertEquals(listOf(3.5, 35.0), port.prices)
        assertNotNull(port.tastingNote)
    }

    @Test
    fun `sparkling section detected`() {
        val entries = parse("menu5")
        assertEquals(WineStyle.SPARKLING, entries.named("Bella Conchi Brut Cava").section)
    }

    @Test
    fun `headers with serving sizes are recognised`() {
        val entries = parse("menu4")
        assertTrue(entries.none { it.name.contains("WINES") })
        assertEquals(WineStyle.SPARKLING, entries.first { it.name.startsWith("Joseph Perrier") }.section)
        assertEquals(WineStyle.ROSE, entries.named("Bestue Garnacha Rose").section)
    }

    @Test
    fun `country without a comma moves to detail`() {
        val chablis = parse("menu4").named("Petit Chablis")
        assertEquals("France", chablis.detail)
        assertEquals(listOf(49.0), chablis.prices)
        val nz = parse("menu6").named("Sauvignon Blanc, Allan Scott")
        assertTrue(nz.detail!!.contains("NEW ZEALAND"))
    }

    @Test
    fun `numbered list and quoted names`() {
        val p = parse("menu6").named("Organic Prosecco Fiori di Campo")
        assertEquals(listOf(7.95, 33.0), p.prices)
        assertEquals(WineStyle.SPARKLING, p.section)
    }

    @Test
    fun `fine dining menu with abbreviated vintages`() {
        val entries = parse("menu3")
        val petrus = entries.named("Château Petrus, Pomerol")
        assertEquals(2008, petrus.vintage.year)
        assertEquals(listOf(600.0), petrus.prices)
        val tignanello = entries.named("Antinori Tignanello")
        assertEquals(2016, tignanello.vintage.year)
    }

    @Test
    fun `founded line is a note not a wine`() {
        assertFalse(parse("menu4").any { it.name.contains("founded") })
    }

    @Test
    fun `section header detection`() {
        assertEquals(WineStyle.RED, MenuParser.sectionHeader("RED WINES")?.style)
        assertEquals(WineStyle.WHITE, MenuParser.sectionHeader("White 175ml 250ml Bottle")?.style)
        assertEquals(WineStyle.SPARKLING, MenuParser.sectionHeader("Sparkling & Champagne")?.style)
        assertEquals(WineStyle.RED, MenuParser.sectionHeader("Interesting Reds")?.style)
        assertNotNull(MenuParser.sectionHeader("ITALY"))
        assertNull(MenuParser.sectionHeader("Rioja Blanco"))
        assertNull(MenuParser.sectionHeader("Taittinger Rosé"))
    }
}
