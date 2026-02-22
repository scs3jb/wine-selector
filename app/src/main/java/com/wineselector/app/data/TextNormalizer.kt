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

    /**
     * Standard Levenshtein edit distance (DP algorithm).
     * Returns the minimum number of single-character edits (insertions,
     * deletions, substitutions) to transform [a] into [b].
     */
    fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        // Use single-row DP to save memory
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,       // deletion
                    curr[j - 1] + 1,   // insertion
                    prev[j - 1] + cost  // substitution
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[n]
    }

    /**
     * Find the closest match for [word] among [candidates] within [maxDistance]
     * edit distance. Only considers words >= 5 chars to avoid false positives
     * on short words (e.g., "del" matching "dei").
     *
     * Returns the best matching candidate, or null if none within threshold.
     */
    fun fuzzyWordMatch(word: String, candidates: Collection<String>, maxDistance: Int = 1): String? {
        if (word.length < 5) return null
        var bestMatch: String? = null
        var bestDist = maxDistance + 1
        for (candidate in candidates) {
            // Skip candidates with length difference > maxDistance (impossible to match)
            if (kotlin.math.abs(candidate.length - word.length) > maxDistance) continue
            val dist = levenshteinDistance(word, candidate)
            if (dist in 1 until bestDist) {
                bestDist = dist
                bestMatch = candidate
            }
        }
        return bestMatch
    }
}
