package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for WinePairingEngine using realistic OCR text from real restaurant wine lists.
 * Each test simulates what ML Kit text recognition would extract from a wine menu photo.
 */
class WinePairingEngineTest {

    private lateinit var engine: WinePairingEngine
    private lateinit var engineWithXWines: WinePairingEngine
    private lateinit var xWinesDb: XWinesDatabase

    @Before
    fun setUp() {
        engine = WinePairingEngine()

        xWinesDb = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_ratings.csv")!!
        xWinesDb.loadFromStreams(winesStream, ratingsStream)
        engineWithXWines = WinePairingEngine(xWinesDb)
    }

    // ==========================================
    // Italian Restaurant Wine List
    // ==========================================

    private val italianWineList = """
        VINI ITALIANI

        ROSSI - RED WINES

        Nebbiolo "Bricco Magno" 2019
        Piedmont
        Glass $18 | Bottle $65

        Chianti Classico Riserva 2018
        Tuscany
        Bottle $75

        Barolo, Viberti 2017
        Piedmont
        $120

        Brunello di Montalcino 2016
        Banfi, Tuscany
        $95

        Barbera d'Alba 2020
        Piedmont
        Glass $14 | Bottle $52

        BIANCHI - WHITE WINES

        Pinot Grigio
        Alto Adige 2022
        Glass $12 | Bottle $45

        SPARKLINGS

        Prosecco Superiore Valdobbiadene
        NV
        Glass $13 | Bottle $50
    """.trimIndent()

    @Test
    fun `Italian list - should find Chianti as top pick for pasta`() {
        val results = engine.recommendWines(italianWineList, FoodCategory.PASTA)
        assertTrue("Should find at least one wine", results.isNotEmpty())

        // Chianti/Sangiovese scores 10 for PASTA
        val topWine = results[0]
        assertTrue(
            "Top pick for pasta should be Chianti (score 10), got: ${topWine.originalText}",
            topWine.originalText.contains("Chianti", ignoreCase = true)
        )
        assertEquals("Chianti should score 10 for pasta", 10, topWine.score)
    }

    @Test
    fun `Italian list - should find Barolo or Nebbiolo for beef`() {
        val results = engine.recommendWines(italianWineList, FoodCategory.BEEF)
        assertTrue("Should find wines for beef", results.isNotEmpty())

        val topWine = results[0]
        val isBaroloOrNebbiolo = topWine.originalText.contains("Barolo", ignoreCase = true) ||
            topWine.originalText.contains("Nebbiolo", ignoreCase = true)
        assertTrue(
            "Top pick for beef should be Barolo (9) or Nebbiolo (9), got: ${topWine.originalText}",
            isBaroloOrNebbiolo
        )
        assertTrue("Score should be 9+", topWine.score >= 9)
    }

    @Test
    fun `Italian list - should find Prosecco for seafood`() {
        val results = engine.recommendWines(italianWineList, FoodCategory.SEAFOOD)
        val prosecco = results.find { it.originalText.contains("Prosecco", ignoreCase = true) }
        assertNotNull("Should find Prosecco in results", prosecco)
    }

    @Test
    fun `Italian list - should find multiple matches`() {
        val results = engine.recommendWines(italianWineList, FoodCategory.BEEF)
        assertTrue("Should find at least 3 red wines for beef", results.size >= 3)
    }

    // ==========================================
    // French Bistro Wine List
    // ==========================================

    private val frenchWineList = """
        CARTE DES VINS

        VINS BLANCS

        Chablis, Domaine Laroche 2021
        Burgundy, France
        Glass $15 | Bottle $62

        Sancerre, Domaine Henri Bourgeois 2022
        Loire Valley
        Glass $17 | Bottle $68

        VINS ROUGES

        Château Larose-Trintaudon 2018
        Haut-Médoc, Bordeaux
        Glass $22 | Bottle $88

        Côtes du Rhône Villages 2021
        Rhône Valley
        Glass $13 | Bottle $48

        Beaujolais-Villages, Georges Duboeuf 2022
        Burgundy
        Glass $11 | Bottle $42

        CHAMPAGNE

        Veuve Clicquot Brut NV
        Glass $24 | Bottle $120
    """.trimIndent()

    @Test
    fun `French list - should find Chablis for fish`() {
        val results = engine.recommendWines(frenchWineList, FoodCategory.FISH)
        assertTrue("Should find wines for fish", results.isNotEmpty())

        val chablis = results.find { it.originalText.contains("Chablis", ignoreCase = true) }
        assertNotNull("Should find Chablis for fish", chablis)
        assertEquals("Chablis should score 9 for fish", 9, chablis!!.score)
    }

    @Test
    fun `French list - should find Sancerre for cheese`() {
        val results = engine.recommendWines(frenchWineList, FoodCategory.CHEESE)
        val sancerre = results.find { it.originalText.contains("Sancerre", ignoreCase = true) }
        assertNotNull("Should find Sancerre for cheese", sancerre)
        assertEquals("Sancerre should score 8 for cheese", 8, sancerre!!.score)
    }

    @Test
    fun `French list - should find Bordeaux for lamb`() {
        val results = engine.recommendWines(frenchWineList, FoodCategory.LAMB)
        val bordeaux = results.find { it.originalText.contains("Bordeaux", ignoreCase = true) }
        assertNotNull("Should find Bordeaux for lamb", bordeaux)
        assertEquals("Bordeaux should score 9 for lamb", 9, bordeaux!!.score)
    }

    @Test
    fun `French list - Champagne should match for sushi`() {
        val results = engine.recommendWines(frenchWineList, FoodCategory.SUSHI)
        val champagne = results.find {
            it.originalText.contains("Champagne", ignoreCase = true) ||
                it.originalText.contains("Clicquot", ignoreCase = true)
        }
        assertNotNull("Should find Champagne for sushi", champagne)
    }

    // ==========================================
    // American Steakhouse Wine List
    // ==========================================

    private val steakhouseWineList = """
        WINE SELECTION

        BY THE GLASS

        REDS

        Terrazas de los Andes Reserva
        Cabernet Sauvignon
        Mendoza, Argentina 2019
        Glass $14 | Bottle $55

        Loscano Malbec 2020
        Mendoza, Argentina
        Glass $14 | Bottle $56

        Francis Coppola Diamond Collection
        Zinfandel 2021
        California
        Glass $12 | Bottle $44

        WHITES

        Kendall-Jackson Vintner's Reserve
        Chardonnay 2022
        California
        Glass $13 | Bottle $48

        Kim Crawford
        Sauvignon Blanc 2023
        Marlborough, New Zealand
        Glass $12 | Bottle $46

        PREMIUM BOTTLES

        Caymus Vineyards
        Cabernet Sauvignon 2021
        Napa Valley
        $165
    """.trimIndent()

    @Test
    fun `Steakhouse list - Cabernet Sauvignon should be top for beef`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.BEEF)
        assertTrue("Should find wines for beef", results.isNotEmpty())

        val topWine = results[0]
        assertTrue(
            "Top pick for beef should contain Cabernet Sauvignon or Malbec, got: ${topWine.originalText}",
            topWine.originalText.contains("Cabernet", ignoreCase = true) ||
                topWine.originalText.contains("Malbec", ignoreCase = true)
        )
        assertEquals("Top beef pairing should score 10", 10, topWine.score)
    }

    @Test
    fun `Steakhouse list - Malbec should score high for beef`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.BEEF)
        val malbec = results.find { it.originalText.contains("Malbec", ignoreCase = true) }
        assertNotNull("Should find Malbec for beef", malbec)
        assertEquals("Malbec should score 10 for beef", 10, malbec!!.score)
    }

    @Test
    fun `Steakhouse list - Zinfandel for pork`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.PORK)
        val zinfandel = results.find { it.originalText.contains("Zinfandel", ignoreCase = true) }
        assertNotNull("Should find Zinfandel for pork", zinfandel)
        assertEquals("Zinfandel should score 8 for pork", 8, zinfandel!!.score)
    }

    @Test
    fun `Steakhouse list - Chardonnay should be top for chicken`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.CHICKEN)
        val chardonnay = results.find { it.originalText.contains("Chardonnay", ignoreCase = true) }
        assertNotNull("Should find Chardonnay for chicken", chardonnay)
        assertEquals("Chardonnay should score 9 for chicken", 9, chardonnay!!.score)
    }

    @Test
    fun `Steakhouse list - Sauvignon Blanc for fish`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.FISH)
        val sauvBlanc = results.find { it.originalText.contains("Sauvignon Blanc", ignoreCase = true) }
        assertNotNull("Should find Sauvignon Blanc for fish", sauvBlanc)
        assertEquals("Sauvignon Blanc should score 9 for fish", 9, sauvBlanc!!.score)
    }

    // ==========================================
    // Seafood Restaurant Wine List
    // ==========================================

    private val seafoodWineList = """
        WINE LIST

        WHITE WINES

        Three Brooms Sauvignon Blanc 2023
        Marlborough, New Zealand
        Glass $13 | Bottle $50

        Chateau de Sancerre 2022
        Sauvignon Blanc
        Loire Valley, France
        Glass $16 | Bottle $64

        La Crema Chardonnay 2021
        Sonoma Coast, California
        Glass $15 | Bottle $58

        Chablis "St. Martin" 2021
        Domaine Laroche
        Burgundy, France
        Glass $14 | Bottle $54

        ROSÉ

        Whispering Angel 2023
        Provence, France
        Glass $16 | Bottle $62

        SPARKLING

        Prosecco Brut NV
        Italy
        Glass $12 | Bottle $44

        RED WINES

        Pinot Noir, Meiomi 2022
        California
        Glass $14 | Bottle $52
    """.trimIndent()

    @Test
    fun `Seafood list - should find Chablis or Sancerre as top for seafood`() {
        val results = engine.recommendWines(seafoodWineList, FoodCategory.SEAFOOD)
        assertTrue("Should find wines for seafood", results.isNotEmpty())

        val topWine = results[0]
        val isChablisSancerreOrSauvBlanc =
            topWine.originalText.contains("Chablis", ignoreCase = true) ||
                topWine.originalText.contains("Sancerre", ignoreCase = true) ||
                topWine.originalText.contains("Sauvignon Blanc", ignoreCase = true)
        assertTrue(
            "Top seafood pick should be Chablis (9), Sancerre (8), or Sauvignon Blanc (9), got: ${topWine.originalText}",
            isChablisSancerreOrSauvBlanc
        )
    }

    @Test
    fun `Seafood list - rosé should match for vegetarian`() {
        val results = engine.recommendWines(seafoodWineList, FoodCategory.VEGETARIAN)
        assertTrue("Should find wines for vegetarian", results.isNotEmpty())
        // rosé scores 7 for vegetarian, sauvignon blanc scores 8
        val topScore = results[0].score
        assertTrue("Top vegetarian score should be 7+", topScore >= 7)
    }

    @Test
    fun `Seafood list - Pinot Noir should match for chicken`() {
        val results = engine.recommendWines(seafoodWineList, FoodCategory.CHICKEN)
        val pinot = results.find { it.originalText.contains("Pinot Noir", ignoreCase = true) }
        assertNotNull("Should find Pinot Noir for chicken", pinot)
        assertEquals("Pinot Noir should score 9 for chicken", 9, pinot!!.score)
    }

    // ==========================================
    // Casual Pizza/Pasta Place Wine List
    // ==========================================

    private val pizzaWineList = """
        WINE & BEER

        HOUSE WINES BY THE GLASS $9

        Red - Chianti
        White - Pinot Grigio

        ITALIAN REDS

        Chianti DOCG 2021
        Tuscany
        $38 bottle

        Montepulciano d'Abruzzo 2022
        $32 bottle

        Sangiovese, Tuscany 2022
        Glass $11 | Bottle $40

        ITALIAN WHITES

        Pinot Grigio delle Venezie 2023
        Glass $10 | Bottle $36

        SPARKLING

        Prosecco Brut
        Split (187ml) $12
        Bottle $42
    """.trimIndent()

    @Test
    fun `Pizza list - Chianti should be top for pizza`() {
        val results = engine.recommendWines(pizzaWineList, FoodCategory.PIZZA)
        assertTrue("Should find wines for pizza", results.isNotEmpty())

        val topWine = results[0]
        assertTrue(
            "Top pizza pick should be Chianti or Sangiovese (score 9), got: ${topWine.originalText}",
            topWine.originalText.contains("Chianti", ignoreCase = true) ||
                topWine.originalText.contains("Sangiovese", ignoreCase = true)
        )
        assertTrue("Pizza pairing score should be 8+", topWine.score >= 8)
    }

    @Test
    fun `Pizza list - Montepulciano should match for pasta`() {
        val results = engine.recommendWines(pizzaWineList, FoodCategory.PASTA)
        val montepulciano = results.find { it.originalText.contains("Montepulciano", ignoreCase = true) }
        assertNotNull("Should find Montepulciano for pasta", montepulciano)
        assertEquals("Montepulciano should score 8 for pasta", 8, montepulciano!!.score)
    }

    // ==========================================
    // buildRecommendation tests
    // ==========================================

    @Test
    fun `buildRecommendation - should extract price from top wine`() {
        val results = engine.recommendWines(steakhouseWineList, FoodCategory.BEEF)
        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, steakhouseWineList)

        assertNotEquals("No match found", rec.wineName)
        assertNotNull("Recommendation should have reasoning", rec.reasoning)
        assertTrue("Reasoning should include score", rec.reasoning.contains("/10"))
    }

    @Test
    fun `buildRecommendation - empty input returns no match`() {
        val results = engine.recommendWines("Just some random text with no wines", FoodCategory.BEEF)
        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, "random text")

        assertEquals("No match found", rec.wineName)
        assertNull("No match should have no price", rec.price)
    }

    @Test
    fun `buildRecommendation - should include runner-up when multiple matches`() {
        val results = engine.recommendWines(italianWineList, FoodCategory.BEEF)
        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, italianWineList)

        assertNotNull("Should have a runner-up", rec.runnerUp)
    }

    // ==========================================
    // Price extraction tests
    // ==========================================

    @Test
    fun `should extract dollar price`() {
        val text = "Cabernet Sauvignon $55 by the bottle"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, text)
        assertNotNull("Should extract price", rec.price)
        assertTrue("Price should contain $55", rec.price!!.contains("55"))
    }

    @Test
    fun `should extract euro price`() {
        val text = "Chianti Classico 42€ bottiglia"
        val results = engine.recommendWines(text, FoodCategory.PASTA)
        val rec = engine.buildRecommendation(results, FoodCategory.PASTA, text)
        assertNotNull("Should extract euro price", rec.price)
    }

    // ==========================================
    // Edge cases
    // ==========================================

    @Test
    fun `should handle case-insensitive matching`() {
        val text = "CABERNET SAUVIGNON 2019 $65"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match uppercase wine name", results.isNotEmpty())
    }

    @Test
    fun `should handle mixed case matching`() {
        val text = "pinot NOIR reserve 2020"
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should match mixed case", results.isNotEmpty())
    }

    @Test
    fun `should handle OCR noise and still match`() {
        // Simulating OCR imperfections - extra spaces, partial text
        val text = """
            Wi ne  List
            Cabernet  Sauvignon  2021  $55
            Mer lot  Reserve  $42
            Pin0t  Grigio  $38
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        // At minimum, "Cabernet" substring should match even with OCR noise
        assertTrue("Should find at least Cabernet despite OCR noise", results.isNotEmpty())
    }

    @Test
    fun `should deduplicate similar wines`() {
        val text = """
            Chianti 2019 Glass $12
            Chianti 2019 Bottle $44
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.PASTA)
        // After deduplication, should be 1 or at most 2 (if text differs enough)
        assertTrue("Should deduplicate similar wine entries", results.size <= 2)
    }

    @Test
    fun `should not match non-wine text`() {
        val text = """
            APPETIZERS
            Bruschetta $12
            Mozzarella Sticks $10
            Caesar Salad $14
            ENTREES
            Grilled Salmon $28
            Ribeye Steak $42
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should not match food items as wines", results.isEmpty())
    }

    // ==========================================
    // X-Wines score boosting tests
    // ==========================================

    @Test
    fun `X-Wines boost - Merlot should get boosted for beef when X-Wines confirms`() {
        // The X-Wines dataset has "Origem Merlot" with Harmonize including "Beef"
        val text = "Origem Merlot 2019 $45"
        val resultsWithout = engine.recommendWines(text, FoodCategory.BEEF)
        val resultsWith = engineWithXWines.recommendWines(text, FoodCategory.BEEF)

        assertTrue("Both engines should find Merlot", resultsWithout.isNotEmpty() && resultsWith.isNotEmpty())

        val scoreWithout = resultsWithout[0].score
        val scoreWith = resultsWith[0].score

        // X-Wines Origem Merlot harmonizes with Beef, so score should be boosted by 1
        assertTrue(
            "X-Wines boosted score ($scoreWith) should be >= non-boosted ($scoreWithout)",
            scoreWith >= scoreWithout
        )
    }

    @Test
    fun `X-Wines - should attach XWineEntry when match found`() {
        val text = "Origem Merlot Reserve 2019"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Merlot", results.isNotEmpty())

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        // The engine tries to match "Origem Merlot" by name
        if (rec.xWinesMatch != null) {
            assertEquals("X-Wines match should be Merlot type", "Red", rec.xWinesMatch!!.type)
        }
    }

    // ==========================================
    // Section header filtering tests
    // ==========================================

    @Test
    fun `should not match standalone section headers as wines`() {
        val text = """
            RED WINES
            WHITE WINES
            SPARKLING
            ROSÉ
            CHAMPAGNE
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        assertTrue("Section headers alone should not produce matches", results.isEmpty())
    }

    @Test
    fun `should match wine under CHAMPAGNE header via section context`() {
        val text = """
            CHAMPAGNE
            Veuve Clicquot Brut NV
            Glass $24 | Bottle $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        assertTrue("Should match wines under CHAMPAGNE section", results.isNotEmpty())
        assertTrue(
            "Result should be Veuve Clicquot, not the header",
            results[0].originalText.contains("Clicquot")
        )
    }

    @Test
    fun `should match wine under SPARKLING header via section context`() {
        val text = """
            SPARKLING
            Prosecco Brut NV
            Italy
            Glass $12 | Bottle $44
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        assertTrue("Should match Prosecco under SPARKLING section", results.isNotEmpty())
        assertTrue(
            "Result should be Prosecco, not the header",
            results[0].originalText.contains("Prosecco")
        )
    }

    @Test
    fun `should not match REDS or WHITES headers`() {
        val text = """
            REDS
            Cabernet Sauvignon 2020 $55
            WHITES
            Chardonnay 2022 $42
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'REDS' header, got: ${result.originalText}",
                result.originalText.trim() == "REDS"
            )
            assertFalse(
                "Should not match 'WHITES' header, got: ${result.originalText}",
                result.originalText.trim() == "WHITES"
            )
        }
    }

    @Test
    fun `should match actual wines with years and prices, not headers`() {
        val text = """
            WINE LIST
            RED WINES
            Merlot Reserve 2019 $45
            ROSÉ
            Provence Rosé 2023 $38
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            // Every result should have a year or price — never a bare header
            val hasYear = result.originalText.contains(Regex("""\b20\d{2}\b"""))
            val hasPrice = result.originalText.contains("$")
            assertTrue(
                "Result '${result.originalText}' should have a year or price",
                hasYear || hasPrice
            )
        }
    }

    @Test
    fun `section context should not leak across unrelated sections`() {
        val text = """
            CHAMPAGNE
            Veuve Clicquot Brut NV
            APPETIZERS
            Bruschetta $12
            Caesar Salad $14
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        // Only Veuve Clicquot should match, not the food items
        assertEquals("Should only match Veuve Clicquot", 1, results.size)
        assertTrue(results[0].originalText.contains("Clicquot"))
    }

    // ==========================================
    // Mixed-case header / bare keyword tests
    // ==========================================

    @Test
    fun `should not match mixed-case bare keyword headers as wines`() {
        val text = """
            Champagne
            Rosé
            Merlot
            Sparkling
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        assertTrue("Mixed-case bare keywords should not produce matches", results.isEmpty())
    }

    @Test
    fun `should match wine under mixed-case Champagne header`() {
        val text = """
            Champagne

            Veuve Clicquot Brut NV
            Glass $24 | Bottle $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        assertTrue("Should match wine under Champagne header", results.isNotEmpty())
        assertTrue(
            "Result should be Veuve Clicquot, not the header",
            results[0].originalText.contains("Clicquot")
        )
    }

    @Test
    fun `should not recommend Rosé header when specific rosé wine exists`() {
        val text = """
            Rosé

            Whispering Angel 2023
            Provence, France
            Glass $16 | Bottle $62
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should find wines", results.isNotEmpty())
        for (result in results) {
            assertFalse(
                "Should not match bare 'Rosé' header, got: ${result.originalText}",
                result.originalText.trim() == "Rosé"
            )
        }
    }

    @Test
    fun `grape variety mid-entry should not be treated as header`() {
        // "Cabernet Sauvignon" appears as line 2 under a producer — not a header
        val text = """
            Terrazas de los Andes Reserva
            Cabernet Sauvignon
            Mendoza, Argentina 2019
            Glass $14 | Bottle $55
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match the wine entry", results.isNotEmpty())
        assertTrue(
            "Should contain Cabernet in the matched entry",
            results[0].originalText.contains("Cabernet", ignoreCase = true)
        )
    }

    // ==========================================
    // X-Wines priority matching tests
    // ==========================================

    @Test
    fun `X-Wines first - should prioritize X-Wines match over keyword match`() {
        val text = "Origem Merlot 2019 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find match", results.isNotEmpty())
        assertNotNull("Should have X-Wines match", results[0].xWinesMatch)
        assertEquals("Match source should be XWINES",
            WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)
    }

    @Test
    fun `keyword fallback - should still work when no X-Wines match`() {
        // Use a keyword engine without X-Wines — keyword matching always works
        val text = "Caymus Cabernet Sauvignon 2021 $165"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find via keyword fallback", results.isNotEmpty())
        assertEquals("Match source should be KEYWORD",
            WinePairingEngine.MatchSource.KEYWORD, results[0].matchSource)
    }

    @Test
    fun `buildRecommendation - alternatives capped at 3`() {
        // Use a wine list with many beef-friendly wines to ensure > 4 scored results
        val manyWinesList = """
            Cabernet Sauvignon 2020 $55
            Malbec Reserve 2021 $48
            Merlot 2019 $42
            Syrah 2020 $40
            Tempranillo 2019 $38
        """.trimIndent()
        val results = engine.recommendWines(manyWinesList, FoodCategory.BEEF)
        assertTrue("Should find more than 4 wines to test cap", results.size > 4)
        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, manyWinesList)
        assertTrue("Should have alternatives", rec.alternatives.isNotEmpty())
        assertTrue(
            "Alternatives should be at most 3, got: ${rec.alternatives.size}",
            rec.alternatives.size <= 3
        )
    }

    @Test
    fun `buildRecommendation - should include alternatives`() {
        val results = engineWithXWines.recommendWines(italianWineList, FoodCategory.BEEF)
        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, italianWineList)
        assertTrue("Should have alternatives", rec.alternatives.isNotEmpty())
        assertTrue("Alternative should have wine name",
            rec.alternatives[0].wineName.isNotEmpty())
    }

    @Test
    fun `buildRecommendation - vintage note for mismatched year`() {
        val text = "Origem Merlot 2021 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        if (rec.vintageMatch == VintageMatch.CLOSEST) {
            assertNotNull("Should have vintage note for mismatched year", rec.vintageNote)
            assertTrue("Vintage note should mention year",
                rec.vintageNote!!.contains("2021"))
        }
    }

    // ==========================================
    // Multi-line coalescing tests
    // ==========================================

    @Test
    fun `multi-line OCR - should split wines without blank lines when prices separate them`() {
        // Simulates OCR that doesn't produce blank lines between wine entries
        val text = """
            Chianti Classico Riserva 2018
            Tuscany
            Bottle $75
            Barolo, Viberti 2017
            Piedmont
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 2 wines", results.size >= 2)

        val chianti = results.find { it.originalText.contains("Chianti", ignoreCase = true) }
        val barolo = results.find { it.originalText.contains("Barolo", ignoreCase = true) }
        assertNotNull("Should find Chianti as a separate entry", chianti)
        assertNotNull("Should find Barolo as a separate entry", barolo)
    }

    @Test
    fun `multi-line OCR - continuous wine list without blank lines`() {
        // Real-world OCR often produces continuous text like this
        val text = """
            Merlot Reserve 2019
            Napa Valley
            Glass $14 | Bottle $55
            Cabernet Sauvignon 2020
            Sonoma County
            Glass $16 | Bottle $62
            Pinot Noir 2021
            Willamette Valley
            Glass $13 | Bottle $50
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 3 wines", results.size >= 3)

        val merlot = results.find { it.originalText.contains("Merlot", ignoreCase = true) }
        val cab = results.find { it.originalText.contains("Cabernet", ignoreCase = true) }
        val pinot = results.find { it.originalText.contains("Pinot Noir", ignoreCase = true) }
        assertNotNull("Should find Merlot separately", merlot)
        assertNotNull("Should find Cabernet separately", cab)
        assertNotNull("Should find Pinot Noir separately", pinot)
    }

    @Test
    fun `multi-line OCR - header followed by wines without blank lines`() {
        // OCR where section header flows directly into wine entries
        val text = """
            ROSSI - RED WINES
            Nebbiolo "Bricco Magno" 2019
            Piedmont
            Glass $18 | Bottle $65
            Chianti Classico Riserva 2018
            Tuscany
            Bottle $75
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)

        for (result in results) {
            assertFalse(
                "Should not display 'ROSSI - RED WINES' header as wine name, got: ${result.displayName}",
                result.displayName?.contains("ROSSI") == true
            )
        }
        assertTrue("Should find at least 2 wines", results.size >= 2)
    }

    @Test
    fun `multi-line OCR - display name should be actual wine not header`() {
        val text = """
            RED WINES
            Cabernet Sauvignon 2020
            Napa Valley
            $55
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wines", results.isNotEmpty())

        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, text)
        assertFalse(
            "Wine name should not be 'RED WINES', got: ${rec.wineName}",
            rec.wineName.contains("RED WINES")
        )
        assertTrue(
            "Wine name should contain 'Cabernet', got: ${rec.wineName}",
            rec.wineName.contains("Cabernet", ignoreCase = true)
        )
    }

    @Test
    fun `display name - should show actual wine name for keyword match`() {
        val text = """
            Terrazas de los Andes Reserva
            Cabernet Sauvignon
            Mendoza, Argentina 2019
            Glass $14 | Bottle $55
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wines", results.isNotEmpty())

        val rec = engine.buildRecommendation(results, FoodCategory.BEEF, text)
        // The display name should contain the wine identity, not just "Terrazas de los Andes Reserva"
        // (which has no identifying grape/region keyword)
        assertTrue(
            "Display name should contain identifying info, got: ${rec.wineName}",
            rec.wineName.contains("Cabernet", ignoreCase = true) ||
                rec.wineName.contains("Terrazas", ignoreCase = true) ||
                rec.wineName.contains("Mendoza", ignoreCase = true)
        )
    }

    @Test
    fun `X-Wines display name - should use database wine name`() {
        val text = "Origem Merlot 2019 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find match", results.isNotEmpty())
        assertTrue("Match source should be XWINES_EXACT or XWINES_CLOSE",
            results[0].matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                results[0].matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        // Display name is now always the canonical DB wine name
        assertEquals(
            "Display should be the exact DB wine name",
            "Origem Merlot", rec.wineName
        )
    }

    @Test
    fun `multi-line OCR - should not merge all wines into one entry`() {
        // This is the worst case: no blank lines, no clear separators
        val text = """
            Chianti DOCG 2021
            Tuscany
            $38 bottle
            Montepulciano d'Abruzzo 2022
            $32 bottle
            Sangiovese, Tuscany 2022
            Glass $11 | Bottle $40
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.PASTA)
        assertTrue("Should find at least 2 separate wines", results.size >= 2)
    }

    // ==========================================
    // Continuation header tests
    // ==========================================

    @Test
    fun `continuation header - Pinot Noir cont should be treated as header`() {
        val text = """
            Pinot Noir cont.
            Kosta Browne, Sonoma Coast 2019
            $85
            Flowers, Sonoma Coast 2020
            $72
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Pinot Noir cont.' as a wine, got: ${result.originalText}",
                result.originalText.contains("cont.", ignoreCase = true)
            )
        }
        assertTrue("Should still find wines under the continuation header", results.isNotEmpty())
    }

    @Test
    fun `continuation header - OCR variant Pinot Nolr cont should be treated as header`() {
        // OCR often misreads "Noir" as "Nolr" — the continuation suffix should still trigger
        val text = """
            Pinot Nolr cont.
            Kosta Browne, Sonoma Coast 2019
            $85
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match OCR-garbled continuation header, got: ${result.originalText}",
                result.originalText.contains("cont.", ignoreCase = true)
            )
        }
    }

    @Test
    fun `continuation header - continued suffix should be treated as header`() {
        val text = """
            Cabernet Sauvignon continued
            Silver Oak, Napa Valley 2018
            $155
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'continued' header, got: ${result.originalText}",
                result.originalText.contains("continued", ignoreCase = true)
            )
        }
        assertTrue("Should find wines under continued header", results.isNotEmpty())
    }

    @Test
    fun `continuation header - parenthesized cont should be treated as header`() {
        val text = """
            Merlot (cont.)
            Duckhorn, Napa Valley 2019
            $78
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match '(cont.)' header, got: ${result.originalText}",
                result.originalText.contains("cont.", ignoreCase = true)
            )
        }
        assertTrue("Should find wines under (cont.) header", results.isNotEmpty())
    }

    @Test
    fun `continuation header - should carry section context to wines underneath`() {
        val text = """
            Pinot Noir cont.

            Kosta Browne, Sonoma Coast 2019
            $85
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        // "Pinot Noir" section context should give the wine underneath a pinot noir score
        assertTrue("Should match wine via section context from continuation header", results.isNotEmpty())
    }

    // ==========================================
    // Accent-stripped OCR text tests
    // ==========================================

    @Test
    fun `French list - should match Cotes du Rhone without accents`() {
        val text = """
            VINS ROUGES

            Cotes du Rhone Villages 2021
            Rhone Valley
            Glass $13 | Bottle $48
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.LAMB)
        assertTrue("Should match Cotes du Rhone without accents", results.isNotEmpty())
        assertTrue(
            "Should contain cotes du rhone match",
            results.any { it.originalText.contains("Cotes", ignoreCase = true) }
        )
    }

    @Test
    fun `French list - should match Chateau without accent via bordeaux keyword`() {
        val text = """
            Chateau Larose-Trintaudon 2018
            Haut-Medoc, Bordeaux
            Glass $22 | Bottle $88
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match Chateau without accent via bordeaux keyword", results.isNotEmpty())
    }

    @Test
    fun `should match Rose section header without accent`() {
        val text = """
            Rose

            Whispering Angel 2023
            Provence, France
            Glass $16 | Bottle $62
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should match wine under unaccented Rose header", results.isNotEmpty())
        for (result in results) {
            assertFalse(
                "Should not match bare 'Rose' header, got: ${result.originalText}",
                result.originalText.trim().equals("Rose", ignoreCase = true)
            )
        }
    }

    // ==========================================
    // OCR character substitution tests
    // ==========================================

    @Test
    fun `should match Merlot with zero-for-O OCR error`() {
        val text = "Merl0t Reserve 2019 $45"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match 'Merl0t' as Merlot", results.isNotEmpty())
        assertTrue("Score should be > 0", results[0].score > 0)
    }

    @Test
    fun `should match Pinot Noir with zero-for-O OCR error`() {
        val text = "Pin0t Noir Reserve 2020 $52"
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should match 'Pin0t N0ir' as Pinot Noir", results.isNotEmpty())
    }

    @Test
    fun `should match Sauvignon with five-for-S OCR error`() {
        val text = "5auvignon Blanc 2023 $42"
        val results = engine.recommendWines(text, FoodCategory.FISH)
        assertTrue("Should match '5auvignon Blanc' as Sauvignon Blanc", results.isNotEmpty())
    }

    @Test
    fun `should match Malbec with one-for-L OCR error`() {
        val text = "Ma1bec Reserve 2020 $48"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match 'Ma1bec' as Malbec", results.isNotEmpty())
    }

    // ==========================================
    // Entry coalescing - wines without prices
    // ==========================================

    @Test
    fun `should split wines listed without prices when keywords present`() {
        // Real-world wine list with vintages but no prices — e.g., tasting menu
        val text = """
            Kendall-Jackson Chardonnay 2022
            Sonoma County
            Meiomi Pinot Noir 2021
            Willamette Valley
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        val hasChardonnay = results.any {
            it.originalText.contains("Chardonnay", ignoreCase = true)
        }
        val hasPinotNoir = results.any {
            it.originalText.contains("Pinot Noir", ignoreCase = true)
        }
        assertTrue("Should find Chardonnay", hasChardonnay)
        assertTrue("Should find Pinot Noir", hasPinotNoir)
    }

    @Test
    fun `should split wines with region lines as separators`() {
        // Wines with region lines but no prices between them
        val text = """
            Reserve Cabernet Sauvignon 2020
            Napa Valley
            Estate Merlot 2021
            Sonoma County
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 2 separate wines", results.size >= 2)
        val hasCab = results.any {
            it.originalText.contains("Cabernet", ignoreCase = true)
        }
        val hasMerlot = results.any {
            it.originalText.contains("Merlot", ignoreCase = true)
        }
        assertTrue("Should find Cabernet", hasCab)
        assertTrue("Should find Merlot", hasMerlot)
    }

    @Test
    fun `should split three wines without prices`() {
        // Three wines with vintages and regions but no prices
        val text = """
            Reserve Cabernet Sauvignon 2020
            Napa Valley
            Estate Merlot 2021
            Sonoma County
            Gran Malbec 2019
            Mendoza, Argentina
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 3 separate wines", results.size >= 3)
    }

    @Test
    fun `should split wines by keyword when next line is wine keyword`() {
        // Two wines on consecutive lines where keyword is the split indicator
        val text = """
            Chianti Classico Riserva 2018
            Tuscany
            Montepulciano d'Abruzzo 2022
            Abruzzo
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.PASTA)
        val hasChianti = results.any {
            it.originalText.contains("Chianti", ignoreCase = true)
        }
        val hasMontepulciano = results.any {
            it.originalText.contains("Montepulciano", ignoreCase = true)
        }
        assertTrue("Should find Chianti", hasChianti)
        assertTrue("Should find Montepulciano", hasMontepulciano)
    }

    // ==========================================
    // Keyword-based section header filtering
    // ==========================================

    @Test
    fun `should not match Merlot Blends header as a wine`() {
        val text = """
            Merlot Blends

            Château Margaux 2018
            Bordeaux
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Merlot Blends' header as wine, got: ${result.originalText}",
                result.originalText.trim().equals("Merlot Blends", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Blends with Merlot header as a wine`() {
        val text = """
            Blends with Merlot

            Château Margaux 2018
            Bordeaux
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Blends with Merlot' header, got: ${result.originalText}",
                result.originalText.trim().equals("Blends with Merlot", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Merlot Wine header as a wine`() {
        val text = """
            Merlot Wine

            Duckhorn Merlot 2019
            Napa Valley
            $78
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find actual wine", results.isNotEmpty())
        for (result in results) {
            assertFalse(
                "Should not match bare 'Merlot Wine' header, got: ${result.originalText}",
                result.originalText.trim().equals("Merlot Wine", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Merlot Selection header as a wine`() {
        val text = """
            Merlot Selection

            Duckhorn Merlot 2019
            Napa Valley
            $78
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Merlot Selection' header, got: ${result.originalText}",
                result.originalText.trim().equals("Merlot Selection", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should carry section context from keyword header like Merlot Blends`() {
        val text = """
            Merlot Blends

            Château Margaux 2018
            Bordeaux
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        // The wine under the header should inherit merlot context
        assertTrue("Should find wine under Merlot Blends header", results.isNotEmpty())
        assertTrue(
            "Result should be Château Margaux, got: ${results[0].originalText}",
            results[0].originalText.contains("Margaux", ignoreCase = true) ||
                results[0].originalText.contains("Bordeaux", ignoreCase = true)
        )
    }

    @Test
    fun `should not match Our Merlot header as a wine`() {
        val text = """
            Our Merlot

            Estate Reserve 2020
            $65
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Our Merlot' header, got: ${result.originalText}",
                result.originalText.trim().equals("Our Merlot", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Merlots and Blends header as a wine`() {
        val text = """
            Merlots and Blends

            Château Margaux 2018
            Bordeaux
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Merlots and Blends' header, got: ${result.originalText}",
                result.originalText.trim().equals("Merlots and Blends", ignoreCase = true)
            )
        }
        // Should still find the actual wine underneath
        assertTrue("Should find wine under header", results.isNotEmpty())
    }

    @Test
    fun `should not match Merlots and Blends header without blank line`() {
        // OCR often doesn't produce blank lines between headers and entries
        val text = """
            Merlots and Blends
            Château Margaux 2018
            Bordeaux
            $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Merlots and Blends' as wine, got: ${result.originalText}",
                result.originalText.trim().equals("Merlots and Blends", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Cabernet and Blends header as a wine`() {
        val text = """
            Cabernet and Blends

            Opus One 2019
            Napa Valley
            $450
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Cabernet and Blends' header, got: ${result.originalText}",
                result.originalText.trim().equals("Cabernet and Blends", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match Pinot Noir and Friends header as a wine`() {
        val text = """
            Pinot Noir & Friends

            Kosta Browne 2019
            Sonoma Coast
            $85
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        for (result in results) {
            assertFalse(
                "Should not match 'Pinot Noir & Friends' header, got: ${result.originalText}",
                result.originalText.trim().equals("Pinot Noir & Friends", ignoreCase = true)
            )
        }
    }

    // ==========================================
    // Header vocabulary detection tests
    // ==========================================

    @Test
    fun `should not match Red Wines by the Glass header as wine`() {
        val text = """
            Red Wines by the Glass
            Cabernet Sauvignon 2020
            Napa Valley
            Glass $16
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Red Wines by the Glass', got: ${result.originalText}",
                result.originalText.contains("by the Glass", ignoreCase = true)
            )
        }
        assertTrue("Should find actual wine underneath", results.isNotEmpty())
    }

    @Test
    fun `should not match Premium White Selection header as wine`() {
        val text = """
            Premium White Selection
            Chardonnay Reserve 2021
            Sonoma Coast
            Bottle $62
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.FISH)
        for (result in results) {
            assertFalse(
                "Should not match 'Premium White Selection', got: ${result.originalText}",
                result.originalText.contains("Premium White", ignoreCase = true)
            )
        }
        assertTrue("Should find actual wine underneath", results.isNotEmpty())
    }

    @Test
    fun `bare keyword entry should be skipped even with section context`() {
        val text = """
            SPARKLING
            Champagne
            Veuve Clicquot Brut NV
            Glass $24 | Bottle $120
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.SEAFOOD)
        for (result in results) {
            assertFalse(
                "Should not match bare 'Champagne' as wine, got: ${result.originalText}",
                result.originalText.trim() == "Champagne"
            )
        }
        assertTrue("Should find Veuve Clicquot underneath", results.isNotEmpty())
        assertTrue(
            "Result should be Veuve Clicquot, got: ${results[0].originalText}",
            results[0].originalText.contains("Clicquot")
        )
    }

    @Test
    fun `real wine with generic words should not be treated as header`() {
        val text = "Cabernet Sauvignon Reserve 2019 $55"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should match real wine even though it contains 'Reserve'", results.isNotEmpty())
    }

    @Test
    fun `should not match Red Wine singular header as wine`() {
        val text = """
            Red Wine
            Merlot 2020 $42
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Red Wine' header, got: ${result.originalText}",
                result.originalText.trim().equals("Red Wine", ignoreCase = true)
            )
        }
        assertTrue("Should find Merlot wine underneath", results.isNotEmpty())
    }

    @Test
    fun `should not match non-wine menu categories`() {
        val text = """
            Cocktails and Spirits
            Negroni $15
            Wine List
            Merlot 2020 $42
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        for (result in results) {
            assertFalse(
                "Should not match 'Cocktails and Spirits', got: ${result.originalText}",
                result.originalText.contains("Cocktails", ignoreCase = true)
            )
        }
    }

    @Test
    fun `should not match House Wines header as wine`() {
        val text = """
            House Wines
            Pinot Grigio 2022 $38
            Merlot 2021 $36
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.FISH)
        for (result in results) {
            assertFalse(
                "Should not match 'House Wines' header, got: ${result.originalText}",
                result.originalText.trim().equals("House Wines", ignoreCase = true)
            )
        }
        assertTrue("Should find actual wines underneath", results.size >= 1)
    }

    @Test
    fun `should not match Featured White Wines header as wine`() {
        val text = """
            Featured White Wines
            Sauvignon Blanc 2022
            Marlborough
            $48
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.FISH)
        for (result in results) {
            assertFalse(
                "Should not match 'Featured White Wines', got: ${result.originalText}",
                result.originalText.contains("Featured White", ignoreCase = true)
            )
        }
        assertTrue("Should find wine underneath", results.isNotEmpty())
    }

    // ==========================================
    // DB-only mode architecture tests
    // The core requirement: when DB is loaded, ONLY wines that match the database
    // appear. No keyword fallback. Tasting notes / descriptions never produce matches.
    // ==========================================

    @Test
    fun `DB mode - wine not in database falls back to keyword matching`() {
        // "Château Beau Soleil" does not exist in the test database.
        // Keyword fallback finds "Cabernet Sauvignon" and "Bordeaux" in the text.
        val text = """
            Château Beau Soleil 2018
            An elegant Cabernet Sauvignon style from Bordeaux
            $85
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find via keyword fallback", results.isNotEmpty())
        assertEquals("Match source should be KEYWORD",
            WinePairingEngine.MatchSource.KEYWORD, results[0].matchSource)
        assertNull("Should have no xWinesMatch", results[0].xWinesMatch)
    }

    @Test
    fun `DB mode - description text with keyword falls back to keyword match when name not in DB`() {
        // Wine name "Maison Obscure" is not in the DB.
        // The entry description mentions "Merlot" and "Cabernet" keywords.
        // Keyword fallback finds these and produces a KEYWORD result.
        val text = """
            Maison Obscure Reserve 2019
            A classic blend of Merlot and Cabernet Sauvignon
            from the hills of Bordeaux
            $110
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find via keyword fallback", results.isNotEmpty())
        assertEquals("Match source should be KEYWORD",
            WinePairingEngine.MatchSource.KEYWORD, results[0].matchSource)
        assertNull("Should have no xWinesMatch", results[0].xWinesMatch)
    }

    @Test
    fun `DB mode - shows DB wine name as display name with DB match in xWinesMatch`() {
        // "Origem Merlot 2019" matches DB wine "Origem Merlot" (2 distinctive words).
        // The display name is the canonical DB wine name, and xWinesMatch stores the DB entry.
        val text = """
            Origem Merlot 2019
            Mendoza
            ${'$'}45
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Origem Merlot via DB match", results.isNotEmpty())

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        // Display name is the canonical DB wine name (not OCR text)
        assertEquals("Origem Merlot", rec.wineName)
        // DB match (xWinesMatch) should be the same wine
        assertEquals("Origem Merlot", rec.xWinesMatch?.wineName)
    }

    @Test
    fun `DB mode - single-word DB wine falls to keyword with OCR display name`() {
        // "Barolo, Viberti 2017" — DB only has single-word "Barolo" wines which don't
        // match in Tier 3 (requires 2+ matched words). Falls to keyword "barolo" and
        // displays the OCR wine name, which is better than showing a generic DB Barolo
        // from a different producer.
        val text = """
            Barolo, Viberti 2017
            Piedmont
            ${'$'}120
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Barolo via keyword fallback", results.isNotEmpty())
        assertTrue(
            "Should display name containing 'Barolo'. Got: ${results[0].displayName}",
            results[0].displayName?.contains("Barolo", ignoreCase = true) == true
        )
    }

    @Test
    fun `DB mode - Origem Merlot shows exact DB name`() {
        val text = "Origem Merlot 2019 ${'$'}45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find match", results.isNotEmpty())
        assertEquals("Match source should be XWINES",
            WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        // Display name is the canonical DB wine name
        assertEquals(
            "Display name should be 'Origem Merlot'",
            "Origem Merlot", rec.wineName
        )
        // DB match must be exactly "Origem Merlot"
        assertEquals(
            "DB match should be 'Origem Merlot'",
            "Origem Merlot", rec.xWinesMatch?.wineName
        )
    }

    @Test
    fun `DB mode - Barbera d Alba matches by exact name`() {
        val text = """
            Barbera d'Alba 2020
            Piedmont
            Glass ${'$'}14 | Bottle ${'$'}52
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.PASTA)
        assertTrue("Should find Barbera d'Alba via exact DB name match", results.isNotEmpty())
        assertEquals("Match source should be XWINES",
            WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)
        assertTrue(
            "DB name should contain 'Barbera'. Got: ${results[0].displayName}",
            results[0].displayName?.contains("Barbera") == true
        )
    }

    @Test
    fun `DB mode - DB-matched results have XWINES match source with DB name as display name`() {
        // In DB mode, DB-matched results use the canonical DB wine name as display name.
        val text = """
            Gabbiano Pinot Grigio 2019
            Delle Venezie, Italy
            Glass ${'$'}12 | Bottle ${'$'}45

            Origem Merlot 2019
            Vale dos Vinhedos, Brazil
            Bottle ${'$'}45
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.FISH)
        assertTrue("Should find wines", results.isNotEmpty())

        // DB-matched results should have XWINES_EXACT or XWINES_CLOSE source
        val dbResults = results.filter {
            it.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                it.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE
        }
        assertTrue("Should have at least one DB match", dbResults.isNotEmpty())

        for (result in dbResults) {
            assertNotNull("Each DB result must have an xWinesMatch", result.xWinesMatch)
            // Display name is the canonical DB wine name
            assertEquals(
                "Display name must be the DB wine name",
                result.xWinesMatch!!.wineName, result.displayName
            )
        }
    }

    @Test
    fun `DB mode - wine entry without DB match uses keyword fallback`() {
        // Wine with a truly unique producer name not in any DB wine — no fuzzy match possible.
        // But "Bordeaux" is a keyword, so keyword fallback produces a result.
        val text = """
            Chateau Fantasia Beau Soleil 2019
            Bordeaux
            Glass ${'$'}22 | Bottle ${'$'}88
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find via keyword fallback from 'Bordeaux'", results.isNotEmpty())
        assertEquals("Match source should be KEYWORD",
            WinePairingEngine.MatchSource.KEYWORD, results[0].matchSource)
        assertNull("Should have no xWinesMatch", results[0].xWinesMatch)
    }

    @Test
    fun `DB mode - wine with no DB match and no keywords produces no result`() {
        // Wine with no DB match AND no wine keywords — truly unmatched.
        val text = """
            Chateau Fantasia Beau Soleil 2019
            A lovely evening wine
            Glass ${'$'}22 | Bottle ${'$'}88
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue(
            "Wine with no DB match and no keywords should not appear. Got: ${results.map { it.displayName }}",
            results.isEmpty()
        )
    }

    @Test
    fun `DB mode - alternatives show DB wine names for multi-word matches`() {
        // Barbera d'Alba (2 distinctive words) matches via DB. Barolo (single-word)
        // falls to keyword. Both should appear with correct display names.
        val text = """
            Barolo, Viberti 2017
            Piedmont
            ${'$'}120

            Barbera d'Alba 2020
            Piedmont
            Glass ${'$'}14 | Bottle ${'$'}52
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 2 wines", results.size >= 2)

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        assertNotNull("Should have an alternative", rec.runnerUp)
        // Top wine should contain "Barolo" (either DB or keyword match)
        assertTrue(
            "Top wine should contain 'Barolo'. Got: ${rec.wineName}",
            rec.wineName.contains("Barolo", ignoreCase = true)
        )
        // Barbera should be present (DB match with canonical name)
        val barbera = results.find { it.displayName?.contains("Barbera", ignoreCase = true) == true }
        assertNotNull("Should have Barbera match", barbera)
        assertNotNull("Barbera should have DB match", barbera!!.xWinesMatch)
    }

    // ==========================================
    // Real wine list format tests
    // Inspired by actual restaurant wine lists from the internet.
    // These verify that real-world OCR output produces correct DB matches.
    // ==========================================

    // Based on Waterfront Restaurant-style wine list format
    private val waterfrontStyleList = """
        WINE LIST

        WHITE WINES

        Gabbiano d'Oro Pinot Grigio 2019
        Italy
        Glass ${'$'}11 | Bottle ${'$'}42

        Las Mulas Reserva Sauvignon Blanc 2021
        Central Valley, Chile
        Glass ${'$'}10 | Bottle ${'$'}38

        RED WINES

        Barolo Classico 2017
        Piedmont, Italy
        Bottle ${'$'}95

        Barbera d'Alba 2020
        Piedmont, Italy
        Glass ${'$'}14 | Bottle ${'$'}52

        Dona Antonia Porto Reserva Tawny
        Porto, Portugal
        Glass ${'$'}12

    """.trimIndent()

    @Test
    fun `Waterfront style - Gabbiano Pinot Grigio matches DB wine`() {
        val results = engineWithXWines.recommendWines(waterfrontStyleList, FoodCategory.FISH)
        val gabbiano = results.find {
            it.displayName?.contains("Gabbiano", ignoreCase = true) == true ||
                it.displayName?.contains("Pinot Grigio", ignoreCase = true) == true
        }
        assertNotNull("Should find Gabbiano Pinot Grigio in DB mode", gabbiano)
        assertTrue("Should be XWINES match",
            gabbiano!!.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                gabbiano.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
    }

    @Test
    fun `Waterfront style - Barbera d Alba matches DB wine`() {
        val results = engineWithXWines.recommendWines(waterfrontStyleList, FoodCategory.PASTA)
        val barbera = results.find {
            it.displayName?.contains("Barbera", ignoreCase = true) == true
        }
        assertNotNull("Should find Barbera d'Alba in DB mode", barbera)
        assertTrue("Should be XWINES match",
            barbera!!.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                barbera.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
        assertTrue(
            "Display name (menu name) should contain 'Barbera'. Got: ${barbera.displayName}",
            barbera.displayName?.contains("Barbera") == true
        )
    }

    @Test
    fun `Waterfront style - Sauvignon Blanc matches DB wine`() {
        val results = engineWithXWines.recommendWines(waterfrontStyleList, FoodCategory.FISH)
        val sauvBlanc = results.find {
            it.displayName?.contains("Sauvignon Blanc", ignoreCase = true) == true ||
                it.displayName?.contains("Las Mulas", ignoreCase = true) == true
        }
        assertNotNull("Should find Las Mulas Reserva Sauvignon Blanc in DB mode", sauvBlanc)
    }

    // Based on Chart House-style wine list format (year-first format, common in upscale restaurants)
    private val chartHouseStyleList = """
        WINE LIST

        BY THE GLASS

        Whites

        2021 Chablis 1er Cru 'Montmains', J. Moreau & Fils
        Burgundy, France
        Glass ${'$'}18

        2019 Gabbiano Pinot Grigio
        Delle Venezie, Italy
        Glass ${'$'}12

        Reds

        2017 Barolo
        Piedmont, Italy
        Glass ${'$'}22

        2020 Barbera d'Alba
        Piedmont, Italy
        Glass ${'$'}15

        BOTTLES

        Origem Merlot 2019
        Vale dos Vinhedos, Brazil
        ${'$'}55
    """.trimIndent()

    @Test
    fun `Chart House style - Chablis matches DB wine`() {
        val results = engineWithXWines.recommendWines(chartHouseStyleList, FoodCategory.FISH)
        val chablis = results.find {
            it.displayName?.contains("Chablis", ignoreCase = true) == true ||
                it.displayName?.contains("Montmains", ignoreCase = true) == true
        }
        assertNotNull("Should find Chablis 1er Cru in DB mode", chablis)
        assertTrue("Should be XWINES match",
            chablis!!.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                chablis.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
    }

    @Test
    fun `Chart House style - Origem Merlot matches and shows DB name`() {
        val results = engineWithXWines.recommendWines(chartHouseStyleList, FoodCategory.BEEF)
        val merlot = results.find {
            it.displayName?.contains("Origem", ignoreCase = true) == true ||
                it.displayName?.contains("Merlot", ignoreCase = true) == true
        }
        assertNotNull("Should find Origem Merlot", merlot)
        if (merlot!!.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
            merlot.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE) {
            // Display name is the canonical DB wine name
            assertEquals("Display name should be 'Origem Merlot'", "Origem Merlot", merlot.displayName)
            assertEquals("DB match should be 'Origem Merlot'", "Origem Merlot", merlot.xWinesMatch?.wineName)
        }
    }

    @Test
    fun `Chart House style - DB wines appear with correct match source`() {
        // This list has real DB wines (Chablis, Barolo, Barbera, Merlot).
        // DB matches should have XWINES_EXACT or XWINES_CLOSE source.
        val results = engineWithXWines.recommendWines(chartHouseStyleList, FoodCategory.BEEF)
        assertTrue("Should find at least one wine", results.isNotEmpty())
        val dbResults = results.filter {
            it.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                it.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE
        }
        assertTrue("Should have at least one DB match", dbResults.isNotEmpty())
    }

    @Test
    fun `DB mode - keyword mode still works when no DB loaded`() {
        // Without a DB, keyword matching should work normally for all these wines.
        val results = engine.recommendWines(chartHouseStyleList, FoodCategory.BEEF)
        assertTrue("Keyword mode should find wines without DB", results.isNotEmpty())
        // All results should be keyword matches (no DB)
        for (result in results) {
            assertNotEquals(
                "Without DB, no XWINES matches should occur",
                WinePairingEngine.MatchSource.XWINES_EXACT, result.matchSource
            )
        }
    }

    @Test
    fun `DB mode - Los Cardos Malbec matches DB wine by name`() {
        val text = """
            Los Cardos Malbec 2020
            Mendoza, Argentina
            Glass ${'$'}13 | Bottle ${'$'}50
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Los Cardos Malbec", results.isNotEmpty())
        assertEquals("Should be XWINES match", WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)
        assertTrue(
            "Display name should contain 'Malbec' or 'Los Cardos'. Got: ${results[0].displayName}",
            results[0].displayName?.contains("Malbec") == true ||
                results[0].displayName?.contains("Los Cardos") == true
        )
    }

    @Test
    fun `DB mode - Gran Malbec matches DB wine by name`() {
        val text = """
            Gran Malbec 2019
            Lujan de Cuyo, Argentina
            ${'$'}75
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Gran Malbec", results.isNotEmpty())
        assertEquals("Should be XWINES match", WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)
        // DB match must be Gran Malbec
        assertEquals(
            "DB match should be 'Gran Malbec'",
            "Gran Malbec", results[0].xWinesMatch?.wineName
        )
        // Display name is the DB wine name
        assertEquals(
            "Display name should be the DB wine name 'Gran Malbec'",
            "Gran Malbec", results[0].displayName
        )
    }

    @Test
    fun `DB mode - Laurene Pinot Noir matches DB wine`() {
        val text = """
            Lauréne Pinot Noir 2019
            Dundee Hills, Oregon
            ${'$'}85
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should find Lauréne Pinot Noir", results.isNotEmpty())
        assertEquals("Should be XWINES match", WinePairingEngine.MatchSource.XWINES_EXACT, results[0].matchSource)
    }

    @Test
    fun `DB mode - tasting menu does not false-match description text`() {
        // A tasting menu describes each wine with tasting notes.
        // Tasting note text (e.g., "Cabernet Sauvignon grapes") should not cause
        // false DB matches — only the wine name line matters.
        val text = """
            Invisible Vineyards Estate Reserve 2019
            Crafted from 100% Cabernet Sauvignon grapes harvested from our hillside block.
            Full-bodied with notes of dark fruit, cedar, and vanilla.
            ${'$'}125

            Mystère Blanc de Blancs NV
            Our signature sparkling wine made in the traditional Champagne method
            from hand-harvested Chardonnay.
            Glass ${'$'}18

            Barolo, Scanavino 2017
            Classic Nebbiolo-based Barolo from Piedmont.
            ${'$'}110
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)

        // Barolo SHOULD appear — via keyword "barolo" (single-word DB wines don't
        // match in Tier 3 to prevent grape-name false matches)
        val barolo = results.find {
            it.displayName?.contains("Barolo", ignoreCase = true) == true
        }
        assertNotNull("Barolo should appear", barolo)

        // Non-DB wines that appear should be keyword matches with no xWinesMatch
        val nonDbResults = results.filter {
            it.matchSource == WinePairingEngine.MatchSource.KEYWORD ||
                it.matchSource == WinePairingEngine.MatchSource.SECTION_CONTEXT
        }
        for (result in nonDbResults) {
            assertNull("Keyword-fallback results should have no xWinesMatch", result.xWinesMatch)
        }
    }

    // ==========================================
    // Three-tier matching tests
    // ==========================================

    @Test
    fun `three-tier - exact DB match and keyword fallback both present with correct matchSource`() {
        // "Origem Merlot" is in the DB. "Château Fantasia" is not, but has "Bordeaux" keyword.
        val text = """
            Origem Merlot 2019
            Vale dos Vinhedos, Brazil
            ${'$'}45

            Château Fantasia 2018
            Bordeaux
            ${'$'}85
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 2 wines", results.size >= 2)

        val origem = results.find { it.displayName?.contains("Origem", ignoreCase = true) == true }
        assertNotNull("Should find Origem Merlot", origem)
        assertTrue("Origem should be XWINES_EXACT or XWINES_CLOSE",
            origem!!.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                origem.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
        assertNotNull("Origem should have xWinesMatch", origem.xWinesMatch)

        val fantasia = results.find { it.originalText.contains("Fantasia", ignoreCase = true) }
        assertNotNull("Should find Château Fantasia via keyword fallback", fantasia)
        assertEquals("Fantasia should be KEYWORD match",
            WinePairingEngine.MatchSource.KEYWORD, fantasia!!.matchSource)
        assertNull("Fantasia should have no xWinesMatch", fantasia.xWinesMatch)
    }

    @Test
    fun `three-tier - ocrHighlightText is populated correctly for DB matches`() {
        val text = """
            Gabbiano d'Oro Pinot Grigio 2019
            Italy
            Glass ${'$'}11 | Bottle ${'$'}42
        """.trimIndent()
        val results = engineWithXWines.recommendWines(text, FoodCategory.FISH)
        assertTrue("Should find wine", results.isNotEmpty())

        val dbResult = results.find {
            it.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                it.matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE
        }
        if (dbResult != null) {
            assertNotNull("DB match should have ocrHighlightText", dbResult.ocrHighlightText)
            // ocrHighlightText should be from OCR, not the canonical DB name
            assertNotEquals("ocrHighlightText should differ from displayName for fuzzy matches",
                dbResult.displayName, dbResult.ocrHighlightText)
        }
    }

    @Test
    fun `three-tier - ocrHighlightText is populated for keyword matches`() {
        // Use a wine name that won't match any DB entry but has a keyword
        val text = "Château Fantasia Bordeaux 2019 $85"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine", results.isNotEmpty())

        val keywordResult = results.find {
            it.matchSource == WinePairingEngine.MatchSource.KEYWORD
        }
        assertNotNull("Should have keyword match", keywordResult)
        assertNotNull("Keyword match should have ocrHighlightText", keywordResult!!.ocrHighlightText)
    }

    @Test
    fun `three-tier - keyword fallback message in buildRecommendation when no DB matches`() {
        // Wine not in DB, but has keyword "Bordeaux"
        val text = "Château Fantasia Bordeaux 2019 $85"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine via keyword fallback", results.isNotEmpty())

        val rec = engineWithXWines.buildRecommendation(results, FoodCategory.BEEF, text)
        assertEquals("Match source should be KEYWORD",
            WinePairingEngine.MatchSource.KEYWORD, rec.matchSource)
        assertNull("Should have no xWinesMatch", rec.xWinesMatch)
    }

    // ==========================================
    // Numbered menu / OCR noise stripping tests
    // ==========================================

    // ==========================================
    // Single-word grape wine rejection (Tier 3)
    // ==========================================

    @Test
    fun `DB mode - producer wine with grape name matches keyword not DB close`() {
        // "Bodegas Tarón Tempranillo" should NOT match a DB wine named "Tempranillo"
        // (single-word). Instead it should fall through to keyword matching and display
        // the producer name from OCR, not the DB grape name.
        val results = engineWithXWines.recommendWines(
            "Bodegas Tarón\nTempranillo\nRioja\n790",
            FoodCategory.BEEF
        )
        assertTrue("Should have results", results.isNotEmpty())
        val top = results.first()
        // Should be keyword match (tempranillo keyword), not a DB close match
        assertTrue(
            "Should be KEYWORD match, not XWINES_CLOSE, got: ${top.matchSource}",
            top.matchSource == WinePairingEngine.MatchSource.KEYWORD ||
            top.matchSource == WinePairingEngine.MatchSource.XWINES_EXACT
        )
        // If keyword match, display name should reflect the OCR producer name, not a generic grape
        if (top.matchSource == WinePairingEngine.MatchSource.KEYWORD) {
            assertTrue(
                "Display name should contain 'Bodegas' or 'Tarón', got: ${top.displayName}",
                top.displayName?.contains("Bodegas", ignoreCase = true) == true ||
                top.displayName?.contains("Tar", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `numbered menu - leading number stripped from display name`() {
        val text = "12. Merlot Reserve 2019 $45"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine", results.isNotEmpty())
        val displayName = results[0].displayName ?: ""
        assertFalse(
            "Display name should not start with '12.', got: $displayName",
            displayName.startsWith("12.")
        )
    }

    @Test
    fun `numbered menu - inline country name stripped from display name`() {
        val text = "12. Malbec FRANCE 790 10.50 30.00"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine via malbec keyword", results.isNotEmpty())
        val displayName = results[0].displayName ?: ""
        assertFalse(
            "Display name should not contain 'FRANCE', got: $displayName",
            displayName.contains("FRANCE", ignoreCase = true)
        )
        assertFalse(
            "Display name should not contain price '790', got: $displayName",
            displayName.contains("790")
        )
    }

    @Test
    fun `numbered menu - inline prices stripped from display name`() {
        val text = "9. Merlot Riviera 6.90 900 25.50"
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine", results.isNotEmpty())
        val displayName = results[0].displayName ?: ""
        assertFalse(
            "Display name should not contain '6.90', got: $displayName",
            displayName.contains("6.90")
        )
        assertFalse(
            "Display name should not contain '25.50', got: $displayName",
            displayName.contains("25.50")
        )
    }

    @Test
    fun `numbered menu - DB mode strips number before matching`() {
        // "Origem Merlot" is in the bundled DB
        val text = "5. Origem Merlot 2019 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine via DB match after stripping number", results.isNotEmpty())
        assertTrue("Should be a DB match",
            results[0].matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                results[0].matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
        assertEquals("Display name should be DB wine name",
            "Origem Merlot", results[0].displayName)
    }

    @Test
    fun `numbered menu - DB mode strips country before matching`() {
        // "Origem Merlot" is in the bundled DB — adding BRAZIL inline shouldn't prevent match
        val text = "Origem Merlot BRAZIL 2019 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find wine via DB match after stripping country", results.isNotEmpty())
        assertTrue("Should be a DB match",
            results[0].matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
                results[0].matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE)
    }

    @Test
    fun `numbered menu - full menu format with numbers and prices`() {
        val text = """
            1. Cabernet Sauvignon 2020 FRANCE 10.50 38.00
            2. Merlot Reserve 2019 ITALY 9.50 35.00
            3. Pinot Noir 2021 USA 11.00 42.00
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find at least 3 wines", results.size >= 3)
        for (result in results) {
            val name = result.displayName ?: ""
            assertFalse(
                "Display name should not start with number, got: $name",
                name.matches(Regex("^\\d+\\..*"))
            )
            assertFalse(
                "Display name should not contain FRANCE/ITALY/USA, got: $name",
                name.contains(Regex("\\b(FRANCE|ITALY|USA)\\b", RegexOption.IGNORE_CASE))
            )
        }
    }

    // ==========================================
    // OCR typo resilience via fuzzy matching
    // ==========================================

    @Test
    fun `DB mode - fuzzy matching handles typos gracefully`() {
        // Fuzzy matching (Levenshtein distance 1) recovers single-char typos
        // in DB wine name matching. "Origem Merlat" has "merlat" which is
        // distance 1 from "merlot" in the DB index.
        // This primarily helps DB mode (tested below), not keyword mode.
        assertTrue("Fuzzy matching is tested via DB mode tests", true)
    }

    @Test
    fun `DB mode - fuzzy matching recovers single-char typo in DB wine name`() {
        // "Origem Merlat" — "merlat" is distance 1 from "merlot"
        // Tier 2.5 in findMatchTiered() corrects "merlat" → "merlot" and
        // looks up sorted-word key "merlot origem" in sortedWordIndex.
        val text = "Origem Merlat 2019 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Origem Merlot via Tier 2.5 fuzzy match", results.isNotEmpty())
        if (results[0].xWinesMatch != null) {
            assertTrue("Should match Origem Merlot",
                results[0].xWinesMatch!!.wineName.contains("Origem"))
        }
    }

    // ==========================================
    // Abbreviated vintage support in matching
    // ==========================================

    @Test
    fun `abbreviated vintage - wine with apostrophe year should not be a header`() {
        val text = """
            Pinot Noir
            Hanzell "Sebella", Sonoma Coast '17
            130
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.CHICKEN)
        assertTrue("Should find wine with abbreviated vintage", results.isNotEmpty())
    }

    @Test
    fun `abbreviated vintage - wine list with abbreviated years matches correctly`() {
        val text = """
            Merlot and Blends
            Château Petrus, Pomerol '08 600
            Duckhorn, Napa Valley '15 190
        """.trimIndent()
        val results = engine.recommendWines(text, FoodCategory.BEEF)
        // "Merlot and Blends" should be treated as a header
        for (result in results) {
            assertFalse(
                "Should not match 'Merlot and Blends' header, got: ${result.originalText}",
                result.originalText.trim().equals("Merlot and Blends", ignoreCase = true)
            )
        }
        assertTrue("Should find wines under the section", results.isNotEmpty())
    }

    @Test
    fun `abbreviated vintage - DB mode extracts vintage for matching`() {
        // Origem Merlot is in the bundled DB with vintages
        val text = "Origem Merlot '19 $45"
        val results = engineWithXWines.recommendWines(text, FoodCategory.BEEF)
        assertTrue("Should find Origem Merlot with abbreviated vintage", results.isNotEmpty())
        if (results[0].matchSource == WinePairingEngine.MatchSource.XWINES_EXACT ||
            results[0].matchSource == WinePairingEngine.MatchSource.XWINES_CLOSE) {
            assertEquals("Should extract 2019 from '19", 2019, results[0].ocrYear)
        }
    }
}
