package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests that load OCR transcription text files from real menu images
 * and verify the matching pipeline produces correct results with the bundled DB.
 */
class MenuMatchingIntegrationTest {

    private lateinit var db: XWinesDatabase
    private lateinit var engine: WinePairingEngine
    private lateinit var keywordEngine: WinePairingEngine

    @Before
    fun setUp() {
        db = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_ratings.csv")!!
        db.loadFromStreams(winesStream, ratingsStream)
        engine = WinePairingEngine(db)
        keywordEngine = WinePairingEngine()
    }

    private fun loadMenu(filename: String): String {
        return javaClass.classLoader!!.getResourceAsStream("menus/$filename")!!
            .bufferedReader().readText()
    }

    // ==========================================
    // Menu 1 — Boathouse restaurant (UK format)
    // Multi-column, prices with /, sections
    // ==========================================

    @Test
    fun `menu1 - should find Merlot for beef`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wines for beef", results.isNotEmpty())
        val merlot = results.find { it.originalText.contains("Merlot", ignoreCase = true) }
        assertNotNull("Should find Merlot", merlot)
    }

    @Test
    fun `menu1 - should find Chablis for fish`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.FISH)
        val chablis = results.find { it.originalText.contains("Chablis", ignoreCase = true) }
        assertNotNull("Should find Chablis for fish", chablis)
    }

    @Test
    fun `menu1 - should find Sancerre for seafood`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        val sancerre = results.find { it.originalText.contains("Sancerre", ignoreCase = true) }
        assertNotNull("Should find Sancerre for seafood", sancerre)
    }

    @Test
    fun `menu1 - should not match section headers as wines`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match bare 'White' header, got: ${result.originalText}",
                result.originalText.trim().equals("White", ignoreCase = true)
            )
            assertFalse(
                "Should not match bare 'Red' header, got: ${result.originalText}",
                result.originalText.trim().equals("Red", ignoreCase = true)
            )
            assertFalse(
                "Should not match bare 'Dessert' header, got: ${result.originalText}",
                result.originalText.trim().equals("Dessert", ignoreCase = true)
            )
        }
    }

    @Test
    fun `menu1 - Chateauneuf du Pape should match for lamb`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.LAMB)
        val chateauneuf = results.find {
            it.originalText.contains("Chateauneuf", ignoreCase = true) ||
                it.originalText.contains("Châteauneuf", ignoreCase = true)
        }
        assertNotNull("Should find Châteauneuf du Pape for lamb", chateauneuf)
    }

    @Test
    fun `menu1 - should find Tempranillo via keyword`() {
        val text = loadMenu("menu1.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        val tempranillo = results.find {
            it.originalText.contains("Tempranillo", ignoreCase = true) ||
                it.originalText.contains("Tarón", ignoreCase = true)
        }
        assertNotNull("Should find Tempranillo via keyword", tempranillo)
    }

    // ==========================================
    // Menu 3 — California Grill (abbreviated vintages)
    // Has '08, '14, '15 style vintages, "Merlot and Blends" section header
    // ==========================================

    @Test
    fun `menu3 - should extract abbreviated vintages`() {
        // Château Petrus, Pomerol '08 has abbreviated vintage
        val vintage = XWinesDatabase.extractVintage("Château Petrus, Pomerol '08")
        assertEquals("Should extract 2008 from '08", 2008, vintage)
    }

    @Test
    fun `menu3 - should extract recent abbreviated vintage`() {
        val vintage = XWinesDatabase.extractVintage("Hanzell \"Sebella\", Sonoma Coast '17")
        assertEquals("Should extract 2017 from '17", 2017, vintage)
    }

    @Test
    fun `menu3 - Merlot and Blends should be treated as section header`() {
        val text = loadMenu("menu3.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Merlot and Blends' header, got: ${result.originalText}",
                result.originalText.trim().equals("Merlot and Blends", ignoreCase = true)
            )
        }
    }

    @Test
    fun `menu3 - should find Pinot Noir wines under continuation header`() {
        val text = loadMenu("menu3.txt")
        val results = keywordEngine.recommendWines(text, FoodCategory.CHICKEN)
        val pinotNoir = results.find {
            it.originalText.contains("Pinot Noir", ignoreCase = true)
        }
        assertNotNull("Should find Pinot Noir wines under cont. header", pinotNoir)
    }

    @Test
    fun `menu3 - should find Barolo via keyword`() {
        val text = loadMenu("menu3.txt")
        val results = keywordEngine.recommendWines(text, FoodCategory.BEEF)
        val barolo = results.find {
            it.originalText.contains("Barolo", ignoreCase = true)
        }
        assertNotNull("Should find Barolo via keyword", barolo)
    }

    @Test
    fun `menu3 - should find Barbaresco via keyword`() {
        val text = loadMenu("menu3.txt")
        val results = keywordEngine.recommendWines(text, FoodCategory.BEEF)
        val barbaresco = results.find {
            it.originalText.contains("Barbaresco", ignoreCase = true)
        }
        assertNotNull("Should find Barbaresco via keyword", barbaresco)
    }

    @Test
    fun `menu3 - should find Valpolicella or Amarone via keyword`() {
        val text = loadMenu("menu3.txt")
        val results = keywordEngine.recommendWines(text, FoodCategory.BEEF)
        val amarone = results.find {
            it.originalText.contains("Amarone", ignoreCase = true) ||
                it.originalText.contains("Valpolicella", ignoreCase = true)
        }
        assertNotNull("Should find Amarone/Valpolicella via keyword", amarone)
    }

    @Test
    fun `menu3 - wines with abbreviated vintage are not treated as headers`() {
        val text = loadMenu("menu3.txt")
        val results = keywordEngine.recommendWines(text, FoodCategory.BEEF)
        // With abbreviated vintage support, wines like "G. D. Vajra Albe, Barolo DOCG '14"
        // should not be skipped. They have a detectable vintage.
        assertTrue("Should find multiple wines from menu3", results.size >= 3)
    }

    // ==========================================
    // Menu 5 — UK pub style (£ prices, sections)
    // Full 4-digit vintages, multiple wine types
    // ==========================================

    @Test
    fun `menu5 - should find Malbec for beef`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        val malbec = results.find {
            it.originalText.contains("Malbec", ignoreCase = true)
        }
        assertNotNull("Should find Malbec for beef", malbec)
    }

    @Test
    fun `menu5 - should find Sauvignon Blanc for fish`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.FISH)
        val sauvBlanc = results.find {
            it.originalText.contains("Sauvignon Blanc", ignoreCase = true)
        }
        assertNotNull("Should find Sauvignon Blanc for fish", sauvBlanc)
    }

    @Test
    fun `menu5 - should find Cotes du Rhone for lamb`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.LAMB)
        val cdr = results.find {
            it.originalText.contains("Rhone", ignoreCase = true) ||
                it.originalText.contains("Rhône", ignoreCase = true)
        }
        assertNotNull("Should find Côtes du Rhône for lamb", cdr)
    }

    @Test
    fun `menu5 - should find Rioja for beef`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        val rioja = results.find {
            it.originalText.contains("Rioja", ignoreCase = true)
        }
        assertNotNull("Should find Rioja for beef", rioja)
    }

    @Test
    fun `menu5 - should find Champagne or Prosecco for seafood`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        val sparkling = results.find {
            it.originalText.contains("Champagne", ignoreCase = true) ||
                it.originalText.contains("Prosecco", ignoreCase = true) ||
                it.originalText.contains("Cava", ignoreCase = true) ||
                it.originalText.contains("Bollinger", ignoreCase = true)
        }
        assertNotNull("Should find sparkling wine for seafood", sparkling)
    }

    @Test
    fun `menu5 - should not match section headers`() {
        val menuText = loadMenu("menu5.txt")
        val results = engine.recommendWines(menuText, FoodCategory.BEEF)
        for (result in results) {
            val resultText = result.originalText.trim().lowercase()
            assertFalse("Should not match WHITE WINES header", resultText == "white wines")
            assertFalse("Should not match RED WINES header", resultText == "red wines")
            assertFalse("Should not match ROSÉ WINES header",
                resultText == "rosé wines" || resultText == "rose wines")
        }
    }

    @Test
    fun `menu5 - should find Port for dessert`() {
        val text = loadMenu("menu5.txt")
        val results = engine.recommendWines(text, FoodCategory.DESSERT)
        val port = results.find {
            it.originalText.contains("Port", ignoreCase = true) ||
                it.originalText.contains("Fonseca", ignoreCase = true)
        }
        assertNotNull("Should find Port for dessert", port)
    }

    // ==========================================
    // Cross-menu: price detection
    // ==========================================

    @Test
    fun `menu1 - should detect glass bottle prices with slash format`() {
        assertTrue("Should detect 5.50 / 6.75 / 19.50 as price",
            WinePairingEngine.lineHasPrice("Boatman's Drift Chenin Blanc, 5.50 / 6.75 / 19.50"))
    }

    @Test
    fun `menu5 - should detect pound prices`() {
        assertTrue("Should detect £5.75 as price",
            WinePairingEngine.lineHasPrice("Banat, Romania £5.75 £7.75 £22.50"))
    }
}
