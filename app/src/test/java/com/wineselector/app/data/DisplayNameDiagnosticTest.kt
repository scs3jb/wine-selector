package com.wineselector.app.data

import org.junit.Before
import org.junit.Test

/**
 * Diagnostic test to inspect display names produced by the engine
 * for all real menu texts. Run and inspect stdout.
 */
class DisplayNameDiagnosticTest {

    private lateinit var engine: WinePairingEngine

    @Before
    fun setUp() {
        engine = WinePairingEngine()
    }

    private fun processMenu(
        menuFile: String,
        label: String,
        food: FoodCategory = FoodCategory.BEEF,
        preferences: WinePreferences = WinePreferences()
    ) {
        val menuText = javaClass.classLoader!!.getResourceAsStream(menuFile)!!
            .bufferedReader().readText()
        val ocrLines = menuText.lines().filter { it.isNotBlank() }

        val results = engine.recommendWines(ocrLines, food, preferences)

        println("=== $label (${results.size} matches) ===")
        for (r in results) {
            println("  DISPLAY: \"${r.displayName}\"")
            println("     FROM: \"${r.originalText}\"")
            println("")
        }
    }

    @Test
    fun `inspect menu1 display names`() {
        processMenu("menus/menu1.txt", "Menu 1 - pub style")
    }

    @Test
    fun `inspect menu3 display names`() {
        processMenu("menus/menu3.txt", "Menu 3 - fine dining")
    }

    @Test
    fun `inspect menu4 all wines`() {
        processMenu("menus/menu4.txt", "Menu 4 - Gallipoli (all wines, beef)")
    }

    @Test
    fun `inspect menu4 red only`() {
        processMenu(
            "menus/menu4.txt", "Menu 4 - Gallipoli (RED only, beef)",
            preferences = WinePreferences(allowedTypes = setOf(WineType.RED))
        )
    }

    @Test
    fun `inspect menu5 display names`() {
        processMenu("menus/menu5.txt", "Menu 5 - British restaurant")
    }

    @Test
    fun `inspect menu5 red only`() {
        processMenu(
            "menus/menu5.txt", "Menu 5 - British (RED only, beef)",
            preferences = WinePreferences(allowedTypes = setOf(WineType.RED))
        )
    }

    @Test
    fun `inspect menu6 all wines`() {
        processMenu("menus/menu6.txt", "Menu 6 - wine list")
    }

    @Test
    fun `inspect menu6 red only`() {
        processMenu(
            "menus/menu6.txt", "Menu 6 - wine list (RED only, beef)",
            preferences = WinePreferences(allowedTypes = setOf(WineType.RED))
        )
    }
}
