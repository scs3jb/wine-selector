package com.wineselector.app.data

import java.text.Normalizer

/**
 * Text normalization utilities for OCR matching. Handles two classes of
 * OCR errors that cause match failures on real-world wine menu photos:
 *
 * 1. Accent/diacritical stripping — ML Kit often drops accents from
 *    French/Italian/Spanish names (Château→Chateau, Côtes→Cotes, Rosé→Rose)
 * 2. Character substitution — visually similar characters get swapped
 *    (O↔0, l↔1, S↔5, rn↔m)
 */
object TextNormalizer {

    /** Matches Unicode combining diacritical marks (category Mn = Mark, Nonspacing). */
    private val COMBINING_MARKS = Regex("[\\p{InCombiningDiacriticalMarks}]")

    /**
     * Strip diacritical marks (accents) from text using Unicode NFD decomposition.
     * Splits base characters from combining marks, then removes the marks.
     *
     * Examples: "Château" → "Chateau", "Côtes du Rhône" → "Cotes du Rhone",
     *           "Rosé" → "Rose", "Gewürztraminer" → "Gewurztraminer"
     */
    fun stripAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return COMBINING_MARKS.replace(decomposed, "")
    }

    /**
     * Full matching normalization: lowercase + strip accents.
     * Used symmetrically by both index builders and query paths.
     */
    fun normalizeForMatching(text: String): String {
        return stripAccents(text.lowercase())
    }

    /**
     * Apply common OCR character substitution corrections to a word.
     * Only applied to words containing at least one letter — pure numbers
     * (prices, vintages) are returned unchanged.
     *
     * Substitutions: 0→o, 1→l, 5→s
     */
    fun ocrCorrectWord(word: String): String {
        if (word.all { it.isDigit() }) return word

        val sb = StringBuilder(word.length)
        for (c in word) {
            when (c) {
                '0' -> sb.append('o')
                '1' -> sb.append('l')
                '5' -> sb.append('s')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Generate rn↔m ligature variants. OCR frequently reads "m" as "rn"
     * or vice versa due to visual similarity.
     */
    fun rnmVariants(word: String): List<String> {
        val variants = mutableListOf(word)
        if (word.contains("rn")) {
            variants.add(word.replace("rn", "m"))
        }
        if (word.contains("m")) {
            variants.add(word.replace("m", "rn"))
        }
        return variants
    }

    /**
     * Generate all OCR-corrected word variants for HashMap lookup expansion.
     * Returns a small set (typically 1-6 entries) containing the original
     * word plus digit-corrected and rn/m variants.
     */
    fun ocrWordVariants(word: String): Set<String> {
        val corrected = ocrCorrectWord(word)
        val base = if (corrected != word) listOf(word, corrected) else listOf(word)
        val result = mutableSetOf<String>()
        for (w in base) {
            result.addAll(rnmVariants(w))
        }
        return result
    }

    /**
     * Full OCR-corrected normalization for substring keyword matching:
     * lowercase + strip accents + apply digit corrections.
     */
    fun normalizeForOcrMatching(text: String): String {
        val normalized = normalizeForMatching(text)
        val sb = StringBuilder(normalized.length)
        for (c in normalized) {
            when (c) {
                '0' -> sb.append('o')
                '1' -> sb.append('l')
                '5' -> sb.append('s')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
