package com.wineselector.core.pairing

import com.wineselector.core.TestData
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingEngineTest {
    private val db = TestData.bundledDb
    private val engine = PairingEngine(db)
    private fun entry(name: String) = db.entries.first { it.wineName == name }

    @Test fun `cabernet with beef beats cabernet with fish`() {
        val beef = engine.score(FoodCategory.BEEF, null, "Estate Cabernet Sauvignon", WineStyle.RED)
        val fish = engine.score(FoodCategory.FISH, null, "Estate Cabernet Sauvignon", WineStyle.RED)
        assertTrue(beef.score >= 9)
        assertTrue(fish.score <= 4)
    }

    @Test fun `harmonization bonus is added on top of the grape score and capped`() {
        val merlot = entry("Origem Merlot")
        assertTrue(db.harmonizesWithFood(merlot, FoodCategory.BEEF))
        val withDb = engine.score(FoodCategory.BEEF, merlot, "Origem Merlot", WineStyle.RED)
        val grapeOnly = PairingEngine(null).score(FoodCategory.BEEF, merlot, "Origem Merlot", WineStyle.RED)
        assertTrue(withDb.score > grapeOnly.score)
        assertTrue(withDb.score <= 10)
        assertTrue(withDb.reasons.any { "X-Wines" in it })
    }

    @Test fun `unknown grapes fall back to style`() {
        val s = engine.score(FoodCategory.SEAFOOD, null, "Mystery Cuvée", WineStyle.SPARKLING)
        assertEquals(8, s.score)
    }

    @Test fun `score all ranks foods`() {
        val all = engine.scoreAll(null, "Sauternes", WineStyle.DESSERT)
        assertEquals(FoodCategory.DESSERT, all.first().first)
    }

    @Test fun `keywords use word boundaries`() {
        assertTrue(GrapeProfiles.findInText("Michel Schmitt Piesporter").none { it.keyword == "port" })
        assertEquals("cabernet sauvignon", GrapeProfiles.findInText("Boatman's Drift Cabernet Sauvignon").first().keyword)
        assertEquals("merlot", GrapeProfiles.forGrape("Merlot")!!.keyword)
        assertTrue(GrapeProfiles.forGrape("Syrah/Shiraz")!!.keyword in setOf("syrah", "shiraz"))
    }
}
