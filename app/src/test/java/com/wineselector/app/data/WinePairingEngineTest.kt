package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for WinePairingEngine using grape keyword matching.
 * Tests the recommendWines(List<String>, ...) method which takes OCR candidate
 * wine names and matches them using grape/region keyword profiles.
 */
class WinePairingEngineTest {

    private lateinit var engine: WinePairingEngine

    @Before
    fun setUp() {
        engine = WinePairingEngine()
    }

    // ==========================================
    // Basic grape keyword matching
    // ==========================================

    @Test
    fun `should match Merlot by grape keyword`() {
        val results = engine.recommendWines(
            listOf("Origem Merlot"),
            FoodCategory.BEEF
        )
        assertTrue("Should find at least 1 wine", results.isNotEmpty())
        assertEquals("Origem Merlot", results[0].displayName)
    }

    @Test
    fun `should match Chardonnay by grape keyword`() {
        val results = engine.recommendWines(
            listOf("Reserva Chardonnay"),
            FoodCategory.FISH
        )
        assertTrue("Should find at least 1 wine", results.isNotEmpty())
        assertEquals("Reserva Chardonnay", results[0].displayName)
    }

    @Test
    fun `should match Cabernet Sauvignon with multi-word keyword`() {
        val results = engine.recommendWines(
            listOf("Boatman's Drift Cabernet Sauvignon"),
            FoodCategory.BEEF
        )
        assertTrue("Should find wine", results.isNotEmpty())
        assertTrue("Score should be high for beef",
            results[0].score >= 8)
    }

    @Test
    fun `should match multiple wines and rank by score`() {
        val results = engine.recommendWines(
            listOf("Origem Merlot", "Estate Cabernet Sauvignon"),
            FoodCategory.BEEF
        )
        assertTrue("Should find at least 2 wines", results.size >= 2)
        // Cabernet Sauvignon should score higher for beef (10) than Merlot (8)
        val merlot = results.find { it.displayName?.contains("Merlot") == true }
        val cabernet = results.find { it.displayName?.contains("Cabernet") == true }
        assertNotNull("Should find Merlot", merlot)
        assertNotNull("Should find Cabernet", cabernet)
        assertTrue("Cabernet should score >= Merlot for beef",
            cabernet!!.score >= merlot!!.score)
    }

    @Test
    fun `should match Riesling for sushi`() {
        val results = engine.recommendWines(
            listOf("Black Label Nik Weis Saar Riesling"),
            FoodCategory.SUSHI
        )
        assertTrue("Should find Riesling", results.isNotEmpty())
        assertTrue("Display name should contain Riesling",
            results[0].displayName!!.contains("Riesling", ignoreCase = true))
    }

    // ==========================================
    // Scoring
    // ==========================================

    @Test
    fun `Merlot should score 8 for beef`() {
        val results = engine.recommendWines(
            listOf("Some Merlot Wine"),
            FoodCategory.BEEF
        )
        assertTrue(results.isNotEmpty())
        assertEquals(8, results[0].score)
    }

    @Test
    fun `Cabernet Sauvignon should score 10 for beef`() {
        val results = engine.recommendWines(
            listOf("Estate Cabernet Sauvignon"),
            FoodCategory.BEEF
        )
        assertTrue(results.isNotEmpty())
        assertEquals(10, results[0].score)
    }

    @Test
    fun `Sangiovese should score 10 for pasta`() {
        val results = engine.recommendWines(
            listOf("Tuscan Sangiovese"),
            FoodCategory.PASTA
        )
        assertTrue(results.isNotEmpty())
        assertEquals(10, results[0].score)
    }

    @Test
    fun `regional keywords should match — Bordeaux for beef`() {
        val results = engine.recommendWines(
            listOf("Château Gachon Bordeaux"),
            FoodCategory.BEEF
        )
        assertTrue(results.isNotEmpty())
        assertEquals(9, results[0].score)
    }

    @Test
    fun `regional keywords should match — Chablis for fish`() {
        val results = engine.recommendWines(
            listOf("Domaine Gautheron Chablis"),
            FoodCategory.FISH
        )
        assertTrue(results.isNotEmpty())
        assertEquals(9, results[0].score)
    }

    // ==========================================
    // Preference filtering
    // ==========================================

    @Test
    fun `should filter by wine type`() {
        val redsOnly = WinePreferences(allowedTypes = setOf(WineType.RED))
        val results = engine.recommendWines(
            listOf("Origem Merlot", "Reserva Chardonnay"),
            FoodCategory.BEEF,
            redsOnly
        )
        val chardonnay = results.find { it.displayName == "Reserva Chardonnay" }
        assertNull("Chardonnay should be filtered out in red-only mode", chardonnay)
    }

    @Test
    fun `should filter by max price`() {
        val lowBudget = WinePreferences(maxPrice = 10)
        val results = engine.recommendWines(
            listOf("Origem Merlot"),
            FoodCategory.BEEF,
            lowBudget,
            ocrLines = listOf("Origem Merlot $50")
        )
        assertTrue("Wine should be filtered out by price", results.isEmpty())
    }

    // ==========================================
    // Edge cases
    // ==========================================

    @Test
    fun `should return empty for empty wine list`() {
        val results = engine.recommendWines(emptyList(), FoodCategory.BEEF)
        assertTrue("Empty input should return empty results", results.isEmpty())
    }

    @Test
    fun `should return empty for no keyword matches`() {
        val results = engine.recommendWines(
            listOf("Nonexistent Wine XYZ 2024"),
            FoodCategory.BEEF
        )
        assertTrue("Unmatched wines should return empty", results.isEmpty())
    }

    @Test
    fun `should skip blank wine names`() {
        val results = engine.recommendWines(
            listOf("", "  ", "Origem Merlot"),
            FoodCategory.BEEF
        )
        assertTrue("Should still match Origem Merlot", results.isNotEmpty())
        assertEquals("Should have exactly 1 result", 1, results.size)
    }

    @Test
    fun `should deduplicate by display name`() {
        val results = engine.recommendWines(
            listOf("Origem Merlot", "Origem Merlot"),
            FoodCategory.BEEF
        )
        assertEquals("Should deduplicate to 1 entry", 1, results.size)
    }

    // ==========================================
    // Section header filtering
    // ==========================================

    @Test
    fun `should skip section headers like Champagne`() {
        val results = engine.recommendWines(
            listOf("Champagne"),
            FoodCategory.SEAFOOD
        )
        assertTrue("Bare 'Champagne' header should be skipped", results.isEmpty())
    }

    @Test
    fun `should skip section headers like Sparkling Wines`() {
        val results = engine.recommendWines(
            listOf("Sparkling Wines", "RED WINES"),
            FoodCategory.BEEF
        )
        assertTrue("Section headers should be skipped", results.isEmpty())
    }

    @Test
    fun `should match wine with Champagne in name`() {
        val results = engine.recommendWines(
            listOf("Taittinger Rosé Champagne"),
            FoodCategory.SEAFOOD
        )
        assertTrue("Full wine name containing 'Champagne' should match", results.isNotEmpty())
    }

    // ==========================================
    // Menu integration tests
    // ==========================================

    @Test
    fun `menu1 human-extracted names should produce matches`() {
        val wineNames = listOf(
            "Taittinger Rosé Champagne",
            "Boatman's Drift Chenin Blanc",
            "San Antini Pinot Grigio",
            "Hamilton Heights Chardonnay",
            "Turning Heads Sauvignon Blanc",
            "Domaine Gautheron Chablis",
            "Domaine Neveu Sancerre",
            "Boatman's Drift Cabernet Sauvignon",
            "La Vigneau Merlot",
            "Bodegas Tarón Tempranillo Rioja",
            "Tor Del Colle Montepulciano Riserva",
            "La Playa Shiraz",
            "Punto Alto Malbec",
            "Lorgeril 1620 Pinot Noir",
            "Azabache Rioja Reserva",
            "Château Gachon Bordeaux",
            "Domaine Tinel Châteauneuf du Pape"
        )

        val results = engine.recommendWines(wineNames, FoodCategory.BEEF)
        assertTrue("Should match at least 10 wines from menu1", results.size >= 10)
    }

    @Test
    fun `menu3 fine dining names should produce matches`() {
        val wineNames = listOf(
            "Hanzell Sebella Pinot Noir",
            "Château Petrus Pomerol",
            "Duckhorn Three Palms Vineyard Merlot",
            "Antinori Tignanello",
            "G. D. Vajra Albe Barolo",
            "Gaja Barbaresco",
            "Sartori Amarone Della Valpolicella",
            "Chateau de Beaucastel Chateauneuf-du-Pape"
        )

        val results = engine.recommendWines(wineNames, FoodCategory.BEEF)
        assertTrue("Should match at least 6 fine dining wines", results.size >= 6)
    }

    // ==========================================
    // Price extraction from OCR lines
    // ==========================================

    @Test
    fun `extractPrice finds currency symbols`() {
        assertEquals("$55", engine.extractPrice("Something $55"))
        assertEquals("€42", engine.extractPrice("Wine €42"))
        assertEquals("£38", engine.extractPrice("Bottle £38"))
    }

    @Test
    fun `extractPrice finds glass bottle format`() {
        assertEquals("13/41", engine.extractPrice("Some wine 13/41"))
    }

    @Test
    fun `extractPrice finds decimal prices`() {
        assertEquals("42.50", engine.extractPrice("Wine 42.50"))
    }

    @Test
    fun `extractPrice returns null for no price`() {
        assertNull(engine.extractPrice("Just a wine name"))
    }

    // ==========================================
    // cleanNameForMatching
    // ==========================================

    @Test
    fun `cleanNameForMatching strips menu numbering`() {
        assertEquals("Merlot Reserve", engine.cleanNameForMatching("12. Merlot Reserve"))
        assertEquals("Chardonnay", engine.cleanNameForMatching("3) Chardonnay"))
        assertEquals("Wine", engine.cleanNameForMatching("#5 Wine"))
    }

    @Test
    fun `cleanNameForMatching strips country names`() {
        val cleaned = engine.cleanNameForMatching("Malbec FRANCE")
        assertFalse("Should strip FRANCE", cleaned.contains("FRANCE"))
        assertTrue("Should keep Malbec", cleaned.contains("Malbec"))
    }

    @Test
    fun `cleanNameForMatching strips embedded prices`() {
        val cleaned = engine.cleanNameForMatching("Merlot 10.50 30.00")
        assertFalse("Should strip decimal prices", cleaned.contains("10.50"))
        assertTrue("Should keep Merlot", cleaned.contains("Merlot"))
    }

    @Test
    fun `cleanNameForMatching preserves vintage years`() {
        val cleaned = engine.cleanNameForMatching("Merlot 2019")
        assertTrue("Should preserve 2019", cleaned.contains("2019"))
    }

    @Test
    fun `cleanNameForMatching strips currency prices`() {
        val cleaned = engine.cleanNameForMatching("Merlot $55")
        assertFalse("Should strip $55", cleaned.contains("$55"))
    }

    // ==========================================
    // cleanDisplayName
    // ==========================================

    @Test
    fun `cleanDisplayName keeps producer and grape`() {
        assertEquals("Origem Merlot",
            engine.cleanDisplayName("Origem Merlot", "merlot"))
    }

    @Test
    fun `cleanDisplayName truncates after keyword`() {
        assertEquals("La Vigneau Merlot",
            engine.cleanDisplayName("La Vigneau Merlot, Smooth medium body", "merlot"))
    }

    @Test
    fun `cleanDisplayName keeps qualifier after keyword`() {
        assertEquals("Tor Del Colle Montepulciano Riserva",
            engine.cleanDisplayName("Tor Del Colle Montepulciano Riserva", "montepulciano"))
    }

    @Test
    fun `cleanDisplayName keeps Reserva qualifier`() {
        assertEquals("Azabache Rioja Reserva",
            engine.cleanDisplayName("Azabache Rioja Reserva", "rioja"))
    }

    @Test
    fun `cleanDisplayName strips volume measurements`() {
        assertEquals("Pinot Grigio Rosé",
            engine.cleanDisplayName("Pinot Grigio Rosé 'Ancora' 2021 Adria Vini 187ml", "pinot grigio"))
    }

    @Test
    fun `cleanDisplayName strips quoted vineyard names and keeps producer`() {
        assertEquals("Pinot Grigio, Cramele Recas",
            engine.cleanDisplayName("Pinot Grigio 'Frunza' 2021, Cramele Recas", "pinot grigio"))
    }

    @Test
    fun `cleanDisplayName strips parenthetical text`() {
        assertEquals("Domaine Gautheron Chablis",
            engine.cleanDisplayName("Domaine Gautheron Chablis (½ bottle)", "chablis"))
    }

    @Test
    fun `cleanDisplayName strips abbreviated vintages`() {
        assertEquals("Hanzell Barolo",
            engine.cleanDisplayName("Hanzell Barolo '14", "barolo"))
    }

    @Test
    fun `cleanDisplayName strips Glass and Bottle`() {
        assertEquals("Merlot",
            engine.cleanDisplayName("Merlot Bottle", "merlot"))
    }

    @Test
    fun `cleanDisplayName handles Champagne with producer`() {
        assertEquals("Taittinger Rosé Champagne",
            engine.cleanDisplayName("Taittinger Rosé Champagne", "champagne"))
    }

    @Test
    fun `cleanDisplayName handles Châteauneuf du Pape`() {
        assertEquals("Domaine Tinel Châteauneuf du Pape",
            engine.cleanDisplayName("Domaine Tinel Châteauneuf du Pape, France", "chateauneuf"))
    }

    // ==========================================
    // lineHasPrice
    // ==========================================

    @Test
    fun `lineHasPrice detects currency symbols`() {
        assertTrue(WinePairingEngine.lineHasPrice("Wine $55"))
        assertTrue(WinePairingEngine.lineHasPrice("Wine €42"))
        assertTrue(WinePairingEngine.lineHasPrice("Wine £38"))
    }

    @Test
    fun `lineHasPrice detects glass bottle format`() {
        assertTrue(WinePairingEngine.lineHasPrice("Wine 13/41"))
    }

    @Test
    fun `lineHasPrice detects bare trailing numbers`() {
        assertTrue(WinePairingEngine.lineHasPrice("Wine 130"))
        assertTrue(WinePairingEngine.lineHasPrice("Wine 7000"))
    }

    @Test
    fun `lineHasPrice rejects vintage years`() {
        assertFalse(WinePairingEngine.lineHasPrice("Wine 2019"))
        assertFalse(WinePairingEngine.lineHasPrice("Wine 1998"))
    }

    @Test
    fun `lineHasPrice returns false for no price`() {
        assertFalse(WinePairingEngine.lineHasPrice("Just a wine name"))
    }

    // ==========================================
    // Sorting and ordering
    // ==========================================

    @Test
    fun `results are sorted by score descending`() {
        val results = engine.recommendWines(
            listOf("Origem Merlot", "Reserva Chardonnay", "Estate Cabernet Sauvignon"),
            FoodCategory.BEEF
        )
        if (results.size >= 2) {
            for (i in 0 until results.size - 1) {
                assertTrue("Results should be sorted by score desc",
                    results[i].score >= results[i + 1].score)
            }
        }
    }

    // ==========================================
    // buildRecommendation
    // ==========================================

    @Test
    fun `buildRecommendation returns no-match when empty`() {
        val rec = engine.buildRecommendation(emptyList(), FoodCategory.BEEF, "raw text")
        assertEquals("No match found", rec.wineName)
        assertNull(rec.price)
    }

    @Test
    fun `buildRecommendation uses top scored wine`() {
        val scored = engine.recommendWines(
            listOf("Origem Merlot", "Reserva Chardonnay"),
            FoodCategory.BEEF
        )
        val rec = engine.buildRecommendation(scored, FoodCategory.BEEF, "raw text")
        assertNotEquals("No match found", rec.wineName)
        assertTrue("Reasoning should mention score",
            rec.reasoning.contains("/10"))
    }

    @Test
    fun `buildRecommendation includes alternatives`() {
        val scored = engine.recommendWines(
            listOf("Origem Merlot", "Reserva Chardonnay", "Estate Cabernet Sauvignon"),
            FoodCategory.BEEF
        )
        if (scored.size >= 2) {
            val rec = engine.buildRecommendation(scored, FoodCategory.BEEF, "raw text")
            assertTrue("Should have alternatives", rec.alternatives.isNotEmpty())
            assertNotNull("Should have a runner-up", rec.runnerUp)
        }
    }
}
