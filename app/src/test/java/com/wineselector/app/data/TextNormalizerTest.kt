package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Test

class TextNormalizerTest {

    // ==========================================
    // Accent stripping
    // ==========================================

    @Test
    fun `stripAccents - should strip French accents`() {
        assertEquals("Chateau", TextNormalizer.stripAccents("Château"))
        assertEquals("Cotes du Rhone", TextNormalizer.stripAccents("Côtes du Rhône"))
        assertEquals("Rose", TextNormalizer.stripAccents("Rosé"))
        assertEquals("Medoc", TextNormalizer.stripAccents("Médoc"))
        assertEquals("Haut-Medoc", TextNormalizer.stripAccents("Haut-Médoc"))
    }

    @Test
    fun `stripAccents - should strip Portuguese accents`() {
        assertEquals("Alem do Rio", TextNormalizer.stripAccents("Além do Rio"))
        assertEquals("Fernao Pires", TextNormalizer.stripAccents("Fernão Pires"))
    }

    @Test
    fun `stripAccents - should strip German umlauts`() {
        assertEquals("Wurttemberg", TextNormalizer.stripAccents("Württemberg"))
        assertEquals("Gewurztraminer", TextNormalizer.stripAccents("Gewürztraminer"))
    }

    @Test
    fun `stripAccents - should strip Spanish accents`() {
        assertEquals("Rioja Crianza", TextNormalizer.stripAccents("Rioja Crianza"))
        assertEquals("Espana", TextNormalizer.stripAccents("España"))
    }

    @Test
    fun `stripAccents - should handle already unaccented text`() {
        assertEquals("Merlot Reserve", TextNormalizer.stripAccents("Merlot Reserve"))
    }

    // ==========================================
    // normalizeForMatching
    // ==========================================

    @Test
    fun `normalizeForMatching - should lowercase and strip accents`() {
        assertEquals("chateau", TextNormalizer.normalizeForMatching("Château"))
        assertEquals("cotes du rhone", TextNormalizer.normalizeForMatching("Côtes du Rhône"))
        assertEquals("rose", TextNormalizer.normalizeForMatching("Rosé"))
        assertEquals("merlot reserve", TextNormalizer.normalizeForMatching("Merlot Reserve"))
    }

    @Test
    fun `normalizeForMatching - should be idempotent`() {
        val text = "Côtes du Rhône"
        val once = TextNormalizer.normalizeForMatching(text)
        val twice = TextNormalizer.normalizeForMatching(once)
        assertEquals(once, twice)
    }

    // ==========================================
    // OCR character correction
    // ==========================================

    @Test
    fun `ocrCorrectWord - should replace 0 with o`() {
        assertEquals("merlot", TextNormalizer.ocrCorrectWord("merl0t"))
        assertEquals("pinot", TextNormalizer.ocrCorrectWord("pin0t"))
    }

    @Test
    fun `ocrCorrectWord - should replace 1 with l`() {
        assertEquals("riesling", TextNormalizer.ocrCorrectWord("ries1ing"))
        assertEquals("malbec", TextNormalizer.ocrCorrectWord("ma1bec"))
    }

    @Test
    fun `ocrCorrectWord - should replace 5 with s`() {
        assertEquals("sauvignon", TextNormalizer.ocrCorrectWord("5auvignon"))
        assertEquals("sangiovese", TextNormalizer.ocrCorrectWord("5angiovese"))
    }

    @Test
    fun `ocrCorrectWord - should not modify pure numbers`() {
        assertEquals("2019", TextNormalizer.ocrCorrectWord("2019"))
        assertEquals("55", TextNormalizer.ocrCorrectWord("55"))
        assertEquals("120", TextNormalizer.ocrCorrectWord("120"))
    }

    @Test
    fun `ocrCorrectWord - should handle multiple substitutions`() {
        assertEquals("solosimo", TextNormalizer.ocrCorrectWord("50l05im0"))
    }

    // ==========================================
    // rn/m ligature variants
    // ==========================================

    @Test
    fun `rnmVariants - should generate m to rn variant`() {
        val variants = TextNormalizer.rnmVariants("merlot")
        assertTrue("Should contain original", variants.contains("merlot"))
        assertTrue("Should contain rn variant", variants.contains("rnerlot"))
    }

    @Test
    fun `rnmVariants - should generate rn to m variant`() {
        val variants = TextNormalizer.rnmVariants("sancerne")
        assertTrue("Should contain original", variants.contains("sancerne"))
        assertTrue("Should contain m variant", variants.contains("sanceme"))
    }

    @Test
    fun `rnmVariants - should return only original when no rn or m`() {
        val variants = TextNormalizer.rnmVariants("chianti")
        assertEquals(1, variants.size)
        assertEquals("chianti", variants[0])
    }

    // ==========================================
    // ocrWordVariants (combined)
    // ==========================================

    @Test
    fun `ocrWordVariants - should generate digit-corrected variant`() {
        val variants = TextNormalizer.ocrWordVariants("merl0t")
        assertTrue("Should contain original", variants.contains("merl0t"))
        assertTrue("Should contain corrected", variants.contains("merlot"))
    }

    @Test
    fun `ocrWordVariants - should generate combined digit + rnm variants`() {
        val variants = TextNormalizer.ocrWordVariants("merl0t")
        assertTrue("Should contain merlot", variants.contains("merlot"))
        // merlot -> rnerlot (m->rn)
        assertTrue("Should contain rnerlot", variants.contains("rnerlot"))
    }

    @Test
    fun `ocrWordVariants - should not expand pure numbers`() {
        val variants = TextNormalizer.ocrWordVariants("2019")
        assertEquals("Pure number should have only 1 variant", 1, variants.size)
        assertTrue(variants.contains("2019"))
    }

    // ==========================================
    // normalizeForOcrMatching
    // ==========================================

    @Test
    fun `normalizeForOcrMatching - should lowercase strip accents and correct digits`() {
        assertEquals("merlot", TextNormalizer.normalizeForOcrMatching("Merl0t"))
        assertEquals("pinot noir", TextNormalizer.normalizeForOcrMatching("Pin0t Noir"))
        assertEquals("sauvignon blanc", TextNormalizer.normalizeForOcrMatching("5auvignon Blanc"))
        assertEquals("cotes du rhone", TextNormalizer.normalizeForOcrMatching("Côtes du Rhône"))
    }
}
