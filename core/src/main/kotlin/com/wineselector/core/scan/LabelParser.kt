package com.wineselector.core.scan

import com.wineselector.core.vintage.VintageInfo
import com.wineselector.core.vintage.VintageParser
import java.time.Year

/** The useful text on a bottle label, split by visual prominence. */
data class LabelText(
    /** Largest-type lines first (producer, wine name), boilerplate removed. */
    val prominentLines: List<String>,
    /** All meaningful label text, in reading order. */
    val meaningfulText: String,
    val vintage: VintageInfo,
    val lineIndices: List<Int>
) {
    /** Best guess at a name when the wine is not in the database. */
    val guessedName: String
        get() = prominentLines.take(2).joinToString(" ")
            .let { VintageParser.strip(it, vintage) }
            .let { MenuParser.cleanName(it) }
}

/**
 * Reads a bottle label photo. Labels print the producer and wine name in the
 * biggest type and bury legal boilerplate (ABV, volume, sulphites, importer)
 * in small print, so lines are ranked by glyph height after the boilerplate
 * is removed.
 */
object LabelParser {

    private val BOILERPLATE = listOf(
        Regex("""(?i)\b\d{2,4}\s?(?:ml|cl)\b|\b\d(?:[.,]\d{1,2})?\s?(?:l|litre|liter)s?\b"""),
        Regex("""(?i)\b\d{1,2}(?:[.,]\d)?\s?%\s?(?:vol|alc|abv)?"""),
        Regex("""(?i)\b(?:alc\.?|alcohol)\b.*\bvol"""),
        Regex("""(?i)contains? sulf|contains? sulph|sulfites|sulphites|allergen|\b(?:egg|milk)\b"""),
        Regex("""(?i)\b(?:product|produce) of\b|\bimported by\b|\bbottled by\b|\bproduced (?:and|&) bottled\b"""),
        Regex("""(?i)mis en bouteille|imbottigliato|embotellado|abgefüllt|estate bottled"""),
        Regex("""(?i)government warning|surgeon general|pregnan|drink responsibly|drinkaware"""),
        Regex("""(?i)www\.|https?:|\.com\b|\.fr\b|\.it\b|\.es\b|@"""),
        Regex("""\b\d{8,14}\b"""),
        Regex("""(?i)^\s*(?:lot|l\.)\s*\w+\s*$""")
    )

    fun parse(page: OcrPage, currentYear: Int = Year.now().value): LabelText {
        val kept = page.lines.withIndex().filter { (_, line) ->
            val t = line.text.trim()
            t.count { it.isLetterOrDigit() } >= 2 && BOILERPLATE.none { it.containsMatchIn(t) }
        }
        val vintage = VintageParser.parse(kept.joinToString(" ") { it.value.text }, currentYear)
            .takeIf { it.isKnown } ?: VintageParser.parse(page.fullText, currentYear)

        val byProminence = kept
            .filter { it.value.text.count(Char::isLetter) >= 3 }
            .sortedWith(compareByDescending<IndexedValue<OcrLine>> { it.value.box?.height ?: 0 }.thenBy { it.index })
            .map { it.value.text.trim() }
            .distinct()

        val readingOrder = kept.sortedWith(compareBy({ it.value.box?.top ?: it.index }, { it.value.box?.left ?: 0 }))
        return LabelText(
            prominentLines = byProminence.take(5),
            meaningfulText = readingOrder.joinToString(" ") { it.value.text.trim() },
            vintage = vintage,
            lineIndices = kept.map { it.index }
        )
    }
}
