package com.wineselector.core.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutAnalyzerTest {

    private fun line(text: String, left: Int, top: Int, width: Int = text.length * 10, height: Int = 20) =
        OcrLine(text, Box(left, top, left + width, top + height))

    @Test
    fun `two column menu is read column by column`() {
        val lines = mutableListOf(line("RED WINES", 350, 0))
        val left = listOf("Chianti Classico 2019 38", "Rioja Reserva 2016 45", "Malbec Mendoza 2021 32", "Barolo 2017 95")
        val right = listOf("Chablis 2021 42", "Sancerre 2022 48", "Albarino 2022 36", "Riesling 2020 34")
        left.forEachIndexed { i, t -> lines += line(t, 20, 60 + i * 40) }
        right.forEachIndexed { i, t -> lines += line(t, 520, 60 + i * 40) }
        val rows = LayoutAnalyzer.rows(OcrPage(lines, 1000, 400)).map { it.text }
        assertEquals("RED WINES", rows.first())
        assertEquals(left + right, rows.drop(1))
    }

    @Test
    fun `name and price columns of a single list are merged into rows`() {
        val lines = mutableListOf<OcrLine>()
        val names = listOf("Chianti Classico 2019", "Rioja Reserva 2016", "Malbec Mendoza 2021", "Barolo 2017", "Chablis 2021", "Sancerre 2022")
        names.forEachIndexed { i, t ->
            lines += line(t, 20, i * 40)
            lines += line("${30 + i}.00", 850, i * 40 + 2, width = 60)
        }
        val rows = LayoutAnalyzer.rows(OcrPage(lines, 1000, 300)).map { it.text }
        assertEquals(names.mapIndexed { i, n -> "$n ${30 + i}.00" }, rows)
    }

    @Test
    fun `lines without boxes are kept`() {
        val rows = LayoutAnalyzer.rows(OcrPage(listOf(OcrLine("Merlot 30")), 0, 0))
        assertTrue(rows.single().text == "Merlot 30")
    }
}
