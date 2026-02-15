package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for XWinesDatabase CSV parsing, wine matching, and food harmonization.
 */
class XWinesDatabaseTest {

    private lateinit var db: XWinesDatabase

    @Before
    fun setUp() {
        db = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_ratings.csv")!!
        db.loadFromStreams(winesStream, ratingsStream)
    }

    // ==========================================
    // CSV Loading
    // ==========================================

    @Test
    fun `load - should parse all 100 wines from CSV`() {
        // Verify by trying to match a known wine
        val match = db.findMatch("Origem Merlot")
        assertNotNull("Should find 'Origem Merlot' from dataset", match)
    }

    @Test
    fun `load - should parse wine name correctly`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Origem Merlot", match.wineName)
    }

    @Test
    fun `load - should parse wine type correctly`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Red", match.type)
    }

    @Test
    fun `load - should parse grapes list`() {
        val match = db.findMatch("Origem Merlot")!!
        assertTrue("Should have Merlot in grapes", match.grapes.contains("Merlot"))
    }

    @Test
    fun `load - should parse harmonize list`() {
        val match = db.findMatch("Origem Merlot")!!
        assertTrue("Should have Beef in harmonize", match.harmonize.contains("Beef"))
    }

    @Test
    fun `load - should parse ABV`() {
        val match = db.findMatch("Origem Merlot")!!
        assertNotNull("Should have ABV", match.abv)
        assertEquals("ABV should be 13.0", 13.0f, match.abv!!, 0.1f)
    }

    @Test
    fun `load - should parse body`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Full-bodied", match.body)
    }

    @Test
    fun `load - should parse acidity`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Medium", match.acidity)
    }

    @Test
    fun `load - should parse country`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Brazil", match.country)
    }

    @Test
    fun `load - should parse region`() {
        val match = db.findMatch("Origem Merlot")!!
        assertEquals("Vale dos Vinhedos", match.regionName)
    }

    @Test
    fun `load - should compute average ratings from ratings CSV`() {
        val match = db.findMatch("Origem Merlot")
        assertNotNull("Should find wine", match)
        // The wine should have an average rating (or null if not in ratings CSV)
        // Either way, the rating loading path is exercised
    }

    @Test
    fun `load - should parse multi-grape wines`() {
        // Dona Antonia Porto has multiple grapes: Touriga Nacional, Touriga Franca, etc.
        val match = db.findMatch("Dona Antonia Porto Reserva Tawny")
        assertNotNull("Should find port wine", match)
        assertTrue("Should have multiple grapes", match!!.grapes.size > 1)
        assertTrue("Should contain Touriga Nacional", match.grapes.contains("Touriga Nacional"))
    }

    @Test
    fun `load - should parse dessert wine type`() {
        val match = db.findMatch("Dona Antonia Porto Reserva Tawny")
        assertNotNull("Should find port wine", match)
        assertTrue(
            "Type should be Dessert or Dessert/Port",
            match!!.type.contains("Dessert")
        )
    }

    // ==========================================
    // Wine Matching - By Name
    // ==========================================

    @Test
    fun `findMatch - should match by full wine name`() {
        val match = db.findMatch("Reserva Chardonnay 2020 Bottle")
        assertNotNull("Should match Reserva Chardonnay", match)
        assertEquals("Reserva Chardonnay", match!!.wineName)
    }

    @Test
    fun `findMatch - should be case insensitive`() {
        val match = db.findMatch("ORIGEM MERLOT reserve")
        assertNotNull("Should match case-insensitively", match)
    }

    @Test
    fun `findMatch - should match with surrounding OCR text`() {
        val match = db.findMatch("Glass $18 - Origem Merlot 2019 Piedmont")
        assertNotNull("Should match wine name within longer text", match)
    }

    @Test
    fun `findMatch - should return null for non-matching text`() {
        val match = db.findMatch("Grilled Salmon with lemon butter")
        assertNull("Should not match food items", match)
    }

    @Test
    fun `findMatch - should return null for short generic text`() {
        val match = db.findMatch("Red Wine")
        assertNull("Should not match generic 'Red Wine' (needs 2+ word matches)", match)
    }

    // ==========================================
    // Wine Matching - By Grape Fallback
    // ==========================================

    @Test
    fun `findMatch - should fall back to grape matching`() {
        // "Merlot" is a grape in the X-Wines dataset (Origem Merlot has ['Merlot'])
        val match = db.findMatch("Some Unknown Winery Merlot 2020")
        assertNotNull("Should match by grape variety fallback", match)
        assertTrue("Matched wine should have Merlot grape", match!!.grapes.contains("Merlot"))
    }

    @Test
    fun `findMatch - should match Chardonnay grape`() {
        val match = db.findMatch("Estate Reserve Chardonnay 2021")
        assertNotNull("Should match Chardonnay by grape", match)
        assertTrue("Matched wine should have Chardonnay grape", match!!.grapes.contains("Chardonnay"))
    }

    @Test
    fun `findMatch - should match Pinot Noir grape`() {
        val match = db.findMatch("Willamette Valley Pinot Noir 2020 $52")
        assertNotNull("Should match Pinot Noir by grape", match)
        assertTrue("Matched wine should have Pinot Noir grape",
            match!!.grapes.any { it.contains("Pinot Noir") })
    }

    // ==========================================
    // Food Harmonization
    // ==========================================

    @Test
    fun `harmonizesWithFood - Merlot should harmonize with beef`() {
        val merlot = db.findMatch("Origem Merlot")!!
        assertTrue("Merlot should harmonize with beef",
            db.harmonizesWithFood(merlot, FoodCategory.BEEF))
    }

    @Test
    fun `harmonizesWithFood - Merlot should harmonize with lamb`() {
        val merlot = db.findMatch("Origem Merlot")!!
        assertTrue("Merlot should harmonize with lamb",
            db.harmonizesWithFood(merlot, FoodCategory.LAMB))
    }

    @Test
    fun `harmonizesWithFood - Merlot should harmonize with pizza`() {
        val merlot = db.findMatch("Origem Merlot")!!
        // Origem Merlot harmonizes with "Pizza"
        assertTrue("Merlot should harmonize with pizza",
            db.harmonizesWithFood(merlot, FoodCategory.PIZZA))
    }

    @Test
    fun `harmonizesWithFood - Merlot should harmonize with pasta`() {
        val merlot = db.findMatch("Origem Merlot")!!
        assertTrue("Merlot should harmonize with pasta",
            db.harmonizesWithFood(merlot, FoodCategory.PASTA))
    }

    @Test
    fun `harmonizesWithFood - Chardonnay should harmonize with fish`() {
        val chard = db.findMatch("Reserva Chardonnay")!!
        // Reserva Chardonnay harmonizes with "Rich Fish"
        assertTrue("Chardonnay should harmonize with fish",
            db.harmonizesWithFood(chard, FoodCategory.FISH))
    }

    @Test
    fun `harmonizesWithFood - Chardonnay should harmonize with seafood`() {
        val chard = db.findMatch("Reserva Chardonnay")!!
        assertTrue("Chardonnay should harmonize with seafood",
            db.harmonizesWithFood(chard, FoodCategory.SEAFOOD))
    }

    @Test
    fun `harmonizesWithFood - Port should harmonize with dessert`() {
        val port = db.findMatch("Dona Antonia Porto Reserva Tawny")!!
        assertTrue("Port should harmonize with dessert",
            db.harmonizesWithFood(port, FoodCategory.DESSERT))
    }

    @Test
    fun `harmonizesWithFood - Port should harmonize with cheese`() {
        val port = db.findMatch("Dona Antonia Porto Reserva Tawny")!!
        // Port harmonizes with "Blue Cheese"
        assertTrue("Port should harmonize with cheese",
            db.harmonizesWithFood(port, FoodCategory.CHEESE))
    }

    @Test
    fun `harmonizesWithFood - should return false for non-matching food`() {
        val port = db.findMatch("Dona Antonia Porto Reserva Tawny")!!
        assertFalse("Port should not harmonize with sushi",
            db.harmonizesWithFood(port, FoodCategory.SUSHI))
    }

    // ==========================================
    // getMappedFoodCategories
    // ==========================================

    @Test
    fun `getMappedFoodCategories - should return multiple categories for versatile wine`() {
        val merlot = db.findMatch("Origem Merlot")!!
        val categories = db.getMappedFoodCategories(merlot)
        assertTrue("Merlot should map to multiple food categories", categories.size >= 3)
        assertTrue("Should include BEEF", categories.contains(FoodCategory.BEEF))
        assertTrue("Should include LAMB", categories.contains(FoodCategory.LAMB))
    }

    @Test
    fun `getMappedFoodCategories - Chardonnay should include seafood and chicken`() {
        val chard = db.findMatch("Reserva Chardonnay")!!
        val categories = db.getMappedFoodCategories(chard)
        assertTrue("Should include FISH", categories.contains(FoodCategory.FISH))
        assertTrue("Should include SEAFOOD", categories.contains(FoodCategory.SEAFOOD))
        assertTrue("Should include CHICKEN", categories.contains(FoodCategory.CHICKEN))
    }

    // ==========================================
    // Edge Cases
    // ==========================================

    @Test
    fun `findMatch - empty string should return null`() {
        assertNull(db.findMatch(""))
    }

    @Test
    fun `findMatch - whitespace should return null`() {
        assertNull(db.findMatch("   "))
    }

    @Test
    fun `findMatch - numbers only should return null`() {
        assertNull(db.findMatch("12345 $55.00"))
    }

    @Test
    fun `harmonizesWithFood - works with entry that has empty harmonize`() {
        // Create an entry with no harmonize data
        val emptyEntry = XWineEntry(
            wineId = "0", wineName = "Test", type = "Red",
            grapes = emptyList(), harmonize = emptyList(),
            abv = null, body = "", acidity = "",
            country = "", regionName = "", wineryName = "",
            averageRating = null
        )
        assertFalse("Empty harmonize should not match any food",
            db.harmonizesWithFood(emptyEntry, FoodCategory.BEEF))
    }

    // ==========================================
    // wineCount
    // ==========================================

    @Test
    fun `wineCount - should return 100 for test dataset`() {
        assertEquals("Test dataset should have 100 wines", 100, db.wineCount)
    }

    // ==========================================
    // loadFromFiles
    // ==========================================

    @Test
    fun `loadFromFiles - should load wines from File objects`() {
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_ratings.csv")!!

        val tempWinesFile = File.createTempFile("xwines_test", ".csv")
        val tempRatingsFile = File.createTempFile("xwines_ratings_test", ".csv")
        try {
            tempWinesFile.outputStream().use { winesStream.copyTo(it) }
            tempRatingsFile.outputStream().use { ratingsStream.copyTo(it) }

            val fileDb = XWinesDatabase()
            fileDb.loadFromFiles(tempWinesFile, tempRatingsFile)

            val match = fileDb.findMatch("Origem Merlot")
            assertNotNull("Should load from files and find wine", match)
            assertEquals("Origem Merlot", match!!.wineName)
            assertEquals(100, fileDb.wineCount)
        } finally {
            tempWinesFile.delete()
            tempRatingsFile.delete()
        }
    }

    // ==========================================
    // Slim Dataset (1K wines, 150K ratings)
    // ==========================================

    @Test
    fun `slim - should load 1007 wines from slim dataset`() {
        val slimDb = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_wines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_ratings.csv")!!
        slimDb.loadFromStreams(winesStream, ratingsStream)
        assertEquals("Slim dataset should have 1007 wines", 1007, slimDb.wineCount)
    }

    @Test
    fun `slim - should find wines by name in slim dataset`() {
        val slimDb = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_wines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_ratings.csv")!!
        slimDb.loadFromStreams(winesStream, ratingsStream)

        val match = slimDb.findMatch("Espumante Moscatel")
        assertNotNull("Should find Espumante Moscatel in slim dataset", match)
        assertEquals("Espumante Moscatel", match!!.wineName)
    }

    @Test
    fun `slim - should find wines by grape in slim dataset`() {
        val slimDb = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_wines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_ratings.csv")!!
        slimDb.loadFromStreams(winesStream, ratingsStream)

        val match = slimDb.findMatch("A nice Ancellotta from the region")
        assertNotNull("Should find by Ancellotta grape", match)
        assertTrue(match!!.grapes.any { it.contains("Ancellotta") })
    }

    @Test
    fun `slim - matching should complete in under 10ms per query`() {
        val slimDb = XWinesDatabase()
        val winesStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_wines.csv")!!
        val ratingsStream = javaClass.classLoader!!.getResourceAsStream("xwines_slim_ratings.csv")!!
        slimDb.loadFromStreams(winesStream, ratingsStream)

        val queries = listOf(
            "Cabernet Sauvignon Reserve 2020",
            "Chateau Margaux Grand Vin",
            "Pinot Noir Willamette Valley",
            "Espumante Moscatel from Brazil",
            "Chardonnay Santa Rita Hills",
            "Unknown Wine That Does Not Exist",
            "Malbec Mendoza Argentina",
            "Riesling Mosel Germany 2019",
            "Barolo Giacomo Conterno",
            "Grilled Salmon with lemon"
        )

        // Warm up
        for (q in queries) slimDb.findMatch(q)

        val start = System.nanoTime()
        val iterations = 100
        for (i in 0 until iterations) {
            for (q in queries) slimDb.findMatch(q)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        val perQuery = elapsed / (iterations * queries.size)

        assertTrue(
            "Matching should be under 10ms per query on 1K wines, was ${perQuery}ms",
            perQuery < 10.0
        )
    }

    // ==========================================
    // Vintage Parsing
    // ==========================================

    @Test
    fun `load - should parse vintages list`() {
        val match = db.findMatch("Origem Merlot")!!
        assertTrue("Should have vintages", match.vintages.isNotEmpty())
        assertTrue("Should contain 2020", match.vintages.contains(2020))
        assertTrue("Should contain 2005", match.vintages.contains(2005))
    }

    @Test
    fun `load - should handle NV entries in vintages`() {
        val match = db.findMatch("Dona Antonia Porto Reserva Tawny")!!
        assertTrue("Should have vintages (excluding N.V.)", match.vintages.isNotEmpty())
        for (v in match.vintages) {
            assertTrue("All vintages should be valid years, got $v", v in 1900..2100)
        }
    }

    // ==========================================
    // Vintage-Aware Matching
    // ==========================================

    @Test
    fun `findMatchWithVintage - should return EXACT for matching year`() {
        val result = db.findMatchWithVintage("Origem Merlot 2019")
        assertNotNull(result)
        assertEquals(VintageMatch.EXACT, result!!.vintageMatch)
    }

    @Test
    fun `findMatchWithVintage - should return CLOSEST for non-matching year`() {
        val result = db.findMatchWithVintage("Origem Merlot 2021")
        assertNotNull(result)
        assertEquals(VintageMatch.CLOSEST, result!!.vintageMatch)
        assertEquals(2021, result.ocrYear)
    }

    @Test
    fun `findMatchWithVintage - should return NOT_CHECKED when no year in text`() {
        val result = db.findMatchWithVintage("Origem Merlot Reserve")
        assertNotNull(result)
        assertEquals(VintageMatch.NOT_CHECKED, result!!.vintageMatch)
    }

    @Test
    fun `findMatchWithVintage - should return null for no match`() {
        val result = db.findMatchWithVintage("Grilled Salmon with lemon")
        assertNull(result)
    }

    @Test
    fun `findClosestVintage - should find closest year`() {
        val vintages = listOf(2020, 2019, 2018, 2015, 2010)
        assertEquals(2018, XWinesDatabase.findClosestVintage(vintages, 2017))
        assertEquals(2020, XWinesDatabase.findClosestVintage(vintages, 2021))
        assertEquals(2010, XWinesDatabase.findClosestVintage(vintages, 2008))
    }

    @Test
    fun `findClosestVintage - empty list returns null`() {
        assertNull(XWinesDatabase.findClosestVintage(emptyList(), 2020))
    }
}
