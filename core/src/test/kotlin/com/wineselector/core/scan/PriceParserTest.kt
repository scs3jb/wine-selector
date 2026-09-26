package com.wineselector.core.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceParserTest {
    private fun split(s: String) = PriceParser.split(s, currentYear = 2026)

    @Test fun `currency prices at end of row`() {
        val r = split("Rapel Valley, Chile £5.75 £7.75 £22.50")
        assertEquals(listOf(5.75, 7.75, 22.5), r.prices)
        assertEquals("£", r.currency)
        assertEquals("Rapel Valley, Chile", r.text)
    }

    @Test fun `slash separated glass and bottle prices`() {
        assertEquals(listOf(5.5, 6.75, 19.5), split("Boatman's Drift Chenin Blanc, 5.50 / 6.75 / 19.50").prices)
        assertEquals(listOf(13.0, 41.0), split("Chablis 13/41").prices)
        assertEquals(listOf(9.0, 11.0, 29.0), split("Saporito Garganega Chardonnay Italy 9/11/29").prices)
    }

    @Test fun `eleven slash twenty nine is not a half bottle`() {
        assertFalse(split("Saporito Garganega Chardonnay Italy 9/11/29").halfBottle)
        assertTrue(split("Domaine Gautheron Chablis, France (½ bottle) 21.00").halfBottle)
        assertTrue(split("Chablis half bottle 21").halfBottle)
    }

    @Test fun `vintage is not a price`() {
        val r = split("Merlot 2019")
        assertTrue(r.prices.isEmpty())
        assertEquals("Merlot 2019", r.text)
        assertEquals(listOf(45.0), split("Merlot 2019 45").prices)
    }

    @Test fun `founding year is not a price`() {
        assertTrue(split("Classic Champagne House founded 1 1824").prices.isEmpty())
    }

    @Test fun `bin numbers stay in the name`() {
        val r = split("Penfolds Bin 389")
        assertTrue(r.prices.isEmpty())
        assertEquals("Penfolds Bin 389", r.text)
        assertEquals(listOf(95.0), split("Penfolds Bin 389 95.00").prices)
    }

    @Test fun `leading menu number is dropped`() {
        val r = split("12. Chateau Musar 2015 65")
        assertEquals(listOf(65.0), r.prices)
        assertEquals("Chateau Musar 2015", r.text)
    }

    @Test fun `leading vintage is kept`() {
        assertEquals("2015 Chateau Musar", split("2015 Chateau Musar 65").text)
    }

    @Test fun `euro decimal comma and trailing symbol`() {
        val r = split("Rioja Reserva 24,50 €")
        assertEquals(listOf(24.5), r.prices)
        assertEquals("€", r.currency)
    }

    @Test fun `thousands separator`() {
        assertEquals(listOf(1250.0), split("Petrus 2010 \$1,250").prices)
    }

    @Test fun `single digit bare number is not a price`() {
        assertTrue(split("Zolo Black, Petite Verdot, Mendoza '12 6").prices.isEmpty())
    }

    @Test fun `volume and abv are not prices`() {
        assertTrue(split("Prosecco 150ml Bottle").prices.isEmpty())
        assertNull(split("Riesling 12.5%").currency)
    }
}
