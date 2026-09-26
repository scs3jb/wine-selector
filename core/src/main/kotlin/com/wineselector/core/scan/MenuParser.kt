package com.wineselector.core.scan

import com.wineselector.core.model.WineStyle
import com.wineselector.core.pairing.GrapeProfiles
import com.wineselector.core.text.TextNormalizer
import com.wineselector.core.vintage.VintageInfo
import com.wineselector.core.vintage.VintageParser
import java.time.Year

/** One wine as listed on a menu. */
data class MenuEntry(
    /** Producer + wine as printed, without prices, vintage or geography. */
    val name: String,
    /** Region / country / secondary line ("Rapel Valley, Chile"). */
    val detail: String?,
    val vintage: VintageInfo,
    /** Prices in the order printed (e.g. 175ml, 250ml, bottle). */
    val prices: List<Double>,
    val currency: String?,
    /** Style implied by the menu section the wine sits in. */
    val section: WineStyle?,
    val sectionTitle: String?,
    val tastingNote: String?,
    val halfBottle: Boolean,
    /** Indices into [OcrPage.lines] that make up this entry (for highlighting). */
    val lineIndices: List<Int>
) {
    val bottlePrice: Double? get() = prices.maxOrNull()
    val glassPrice: Double? get() = if (prices.size >= 2) prices.minOrNull() else null
    /** Everything printed about the wine except the tasting note. */
    val fullText: String get() = listOfNotNull(name, detail).joinToString(", ")
}

/**
 * Reads wine entries out of menu rows. Handles the common layouts:
 *  - one line per wine: "Chablis, Domaine X 2020 .... 45"
 *  - name line + priced region line: "Merlot 'Vuelo' 2021, Tagua Tagua" / "Rapel Valley, Chile £5.75 £22.50"
 *  - priced name line + geography/grape continuation: "Hamilton Heights Unoaked 24.50" / "Chardonnay, Australia"
 *  - section headers (RED WINES, Sparkling, 175ml / 250ml / Bottle) and tasting notes.
 */
object MenuParser {

    fun parse(page: OcrPage, currentYear: Int = Year.now().value): List<MenuEntry> =
        parseRows(LayoutAnalyzer.rows(page), currentYear)

    fun parseRows(rows: List<TextRow>, currentYear: Int = Year.now().value): List<MenuEntry> {
        val entries = mutableListOf<MenuEntry>()
        var section: WineStyle? = null
        var sectionTitle: String? = null
        var current: Builder? = null
        var lastRowOwner: Builder? = null  // entry that consumed the previous row

        fun close() {
            current?.build(currentYear)?.let { entries += it }
            current = null
        }

        for (row in rows) {
            val text = row.text.trim()
            if (text.isEmpty() || isFormatHeader(text)) { lastRowOwner = null; continue }
            val split = PriceParser.split(text, currentYear)
            val body = split.text
            val hasLetters = body.count { it.isLetter() } >= 2

            // Prices on their own row belong to the entry above if it has none.
            if (!hasLetters) {
                val c = current
                if (split.prices.isNotEmpty() && c != null && c.prices.isEmpty()) {
                    c.addPrices(split, row.lineIndices)
                    lastRowOwner = c
                }
                continue
            }

            if (split.prices.isEmpty()) {
                val header = sectionHeader(body)
                if (header != null) {
                    close()
                    header.style?.let { section = it }
                    sectionTitle = header.title
                    lastRowOwner = null
                    continue
                }
                if (isTastingNote(body)) {
                    current?.addNote(body, row.lineIndices)
                    lastRowOwner = null
                    continue
                }
                val c = current
                if (c != null && lastRowOwner === c && c.prices.isNotEmpty() && isContinuation(body)) {
                    c.addContinuation(body, row.lineIndices)
                    continue
                }
                // A new wine whose prices may follow on the next row.
                close()
                current = Builder(section, sectionTitle).also { it.start(split, row.lineIndices) }
                lastRowOwner = current
                continue
            }

            // Row with prices.
            val c = current
            if (c != null && lastRowOwner === c && c.prices.isEmpty() && isTastingNote(body)) {
                c.addNote(body, row.lineIndices)
                c.addPrices(split, emptyList())
                lastRowOwner = null
                continue
            }
            if (c != null && lastRowOwner === c && c.prices.isEmpty() && !c.hasNote &&
                looksLikeDetailLine(body, c)
            ) {
                c.addDetailWithPrices(split, row.lineIndices)
                lastRowOwner = c
                continue
            }
            close()
            current = Builder(section, sectionTitle).also { it.start(split, row.lineIndices) }
            lastRowOwner = current
        }
        close()
        return entries
    }

    // ---- Row classification ------------------------------------------------

    private val FORMAT_HEADER = Regex(
        """(?i)^(?:\s*(?:\d{2,4}\s?(?:ml|cl|l)|glass(?:es)?|bottles?|btl|bt|carafe|half|½|gl|by the glass|by the bottle|magnum|jug|[/|&+\-]))+\s*$"""
    )

    fun isFormatHeader(text: String): Boolean = FORMAT_HEADER.matches(text.trim())

    data class SectionHeader(val title: String, val style: WineStyle?)

    private val HEADER_FILLER = setOf(
        "wine", "wines", "by", "the", "glass", "bottle", "our", "selection", "list", "house", "and", "&", "fine",
        "cont", "continued", "new", "old", "world", "of", "from", "other", "more", "cellar", "reserve", "premium"
    )
    private val STYLE_HEADER_WORDS: Map<String, WineStyle> = mapOf(
        "red" to WineStyle.RED, "reds" to WineStyle.RED, "rouge" to WineStyle.RED, "tinto" to WineStyle.RED,
        "tintos" to WineStyle.RED, "rosso" to WineStyle.RED, "rossi" to WineStyle.RED,
        "white" to WineStyle.WHITE, "whites" to WineStyle.WHITE, "blanc" to WineStyle.WHITE, "blancs" to WineStyle.WHITE,
        "blanco" to WineStyle.WHITE, "blancos" to WineStyle.WHITE, "bianco" to WineStyle.WHITE, "bianchi" to WineStyle.WHITE,
        "rose" to WineStyle.ROSE, "roses" to WineStyle.ROSE, "rosado" to WineStyle.ROSE, "rosato" to WineStyle.ROSE,
        "sparkling" to WineStyle.SPARKLING, "champagne" to WineStyle.SPARKLING, "champagnes" to WineStyle.SPARKLING,
        "bubbles" to WineStyle.SPARKLING, "fizz" to WineStyle.SPARKLING, "prosecco" to WineStyle.SPARKLING,
        "dessert" to WineStyle.DESSERT, "sweet" to WineStyle.DESSERT, "sticky" to WineStyle.DESSERT,
        "stickies" to WineStyle.DESSERT, "port" to WineStyle.FORTIFIED, "ports" to WineStyle.FORTIFIED,
        "sherry" to WineStyle.FORTIFIED, "sherries" to WineStyle.FORTIFIED, "fortified" to WineStyle.FORTIFIED
    )

    private val COLLECTIVE_STYLE_WORDS = setOf(
        "reds", "whites", "roses", "champagnes", "bubbles", "stickies", "sherries", "ports", "sparkling", "dessert", "fortified"
    )

    /** Section header like "RED WINES", "Sparkling & Champagne", "Whites by the glass", "ITALY". */
    private val FORMAT_TOKENS = Regex(
        """(?i)\b\d{2,4}\s?(?:ml|cl)\b|\b(?:glass(?:es)?|bottles?|btl|carafe|by the glass|by the bottle)\b|[/|]"""
    )

    fun sectionHeader(text: String): SectionHeader? {
        val trimmed = FORMAT_TOKENS.replace(text, " ").replace(Regex("\\s+"), " ").trim().trimEnd(':', '.', '-', '&').trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > 40 || trimmed.any { it.isDigit() }) return null
        val words = TextNormalizer.normalizeForMatching(trimmed).replace(Regex("[^a-z&' ]"), " ")
            .split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > 5) return null
        val meaningful = words.filter { it !in HEADER_FILLER }
        val styles = meaningful.mapNotNull { STYLE_HEADER_WORDS[it] }.distinct()
        val allStyleWords = meaningful.isNotEmpty() && meaningful.all { it in STYLE_HEADER_WORDS }
        val isUpper = trimmed.any { it.isLetter() } && trimmed == trimmed.uppercase()
        return when {
            allStyleWords -> SectionHeader(trimmed, styles.first())
            styles.isNotEmpty() && (isUpper || words.any { it == "wines" || it == "wine" }) ->
                SectionHeader(trimmed, styles.first())
            // "Interesting Reds", "Sparkling", "Stickies": plural/collective style words in a short title.
            words.size <= 3 && words.any { it in COLLECTIVE_STYLE_WORDS } ->
                SectionHeader(trimmed, words.firstNotNullOfOrNull { STYLE_HEADER_WORDS[it] })
            // All-caps region/country sub-headers ("ITALY", "NEW ZEALAND", "BORDEAUX")
            isUpper && GeoLexicon.isGeographic(trimmed) -> SectionHeader(trimmed, null)
            // Grape sub-headers ("PINOT NOIR", "Merlot and Blends")
            isUpper && meaningful.size <= 3 && GrapeProfiles.profiles.containsKey(meaningful.joinToString(" ")) ->
                SectionHeader(trimmed, null)
            Regex("""(?i)\b(?:and blends?|cont\.?|continued)$""").containsMatchIn(trimmed) -> SectionHeader(trimmed, null)
            else -> null
        }
    }

    private val NOTE_STARTERS = listOf(
        "a ", "an ", "the ", "this ", "these ", "our ", "grown ", "made ", "aged ", "ripe ", "rich ", "light ", "full ",
        "dry ", "sweet ", "smooth ", "crisp ", "fresh ", "classic ", "elegant ", "medium ", "bone ", "simple ",
        "award ", "just ", "aromatic ", "intense ", "off-dry", "off dry", "fruity ", "dark ", "juicy ", "soft ",
        "warm ", "ruby ", "expressive ", "attractive ", "easy ", "bright ", "zesty ", "very ", "lively ", "with ",
        "notes of", "aromas of", "flavours of", "flavors of", "hints of", "perfect ", "great ", "lovely ", "delicate ",
        "vibrant ", "velvety ", "silky ", "powerful ", "concentrated ", "floral ", "mineral ", "savoury ", "savory ",
        "wild ", "hand ", "biodynamic ", "enjoy ", "serve ", "ideal ", "pairs ", "goes ", "drink ",
        "founded ", "established ", "wickedly ", "pure ", "bone-dry", "zingy ", "mouth-watering", "refreshing ",
        "beautifully ", "excellent ", "superb ", "stunning ", "gorgeous "
    )
    private val NOTE_WORDS = setOf(
        "fruity", "refreshing", "delicious", "balanced", "rounded", "bodied", "flavoured", "flavored", "flavours",
        "flavors", "velvety", "silky", "concentrated", "oaked", "unoaked", "complex", "subtle", "powerful", "notes",
        "aromas", "palate", "finish", "tannins", "acidity", "hints", "nose", "texture", "tasty", "crisp", "zesty",
        "crispy", "mouth-watering", "elegance", "structured", "harmonious", "medium", "lively", "juicy", "notes",
        "character", "flavour", "flavor", "fresh", "youthful", "spritz"
    )
    private val FUNCTION_WORDS = setOf("with", "and", "of", "the", "a", "an", "on", "for", "at", "is", "it's", "its",
        "to", "from", "in", "by", "this", "that", "has", "which", "as")

    /** Tasting notes and descriptions, as opposed to wine names. */
    fun isTastingNote(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        if (t[0].isLowerCase()) return true
        val lower = t.lowercase().replace(Regex("[,;:&!.]"), " ").replace(Regex("\\s+"), " ").trim() + " "
        if (NOTE_STARTERS.any { lower.startsWith(it) }) return true
        val words = lower.split(Regex("""[\s,;]+""")).filter { it.isNotEmpty() }
        if (words.count { it.trim('.', '!') in NOTE_WORDS } >= 2) return true
        val functionWords = words.count { it in FUNCTION_WORDS }
        if (words.size >= 8 && functionWords >= 2) return true
        if (words.size >= 6 && (t.endsWith(".") || t.endsWith("!") || t.contains(". "))) return true
        return t.length > 90
    }

    /** "South Africa", "Chardonnay, Australia", "Rapel Valley, Chile" under an already priced wine. */
    private fun isContinuation(text: String): Boolean {
        if (VintageParser.parse(text).year != null) return false
        val segments = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty() || text.split(Regex("\\s+")).size > 6) return false
        return segments.all { seg -> GeoLexicon.isGeographic(seg) || isGrapeOrStyle(seg) } &&
            segments.any { GeoLexicon.isGeographic(it) || GeoLexicon.containsPlace(it) } ||
            segments.size == 1 && isGrapeOrStyle(segments[0])
    }

    private val GRAPE_WORDS: Set<String> = GrapeProfiles.profiles.keys
        .flatMap { TextNormalizer.normalizeForMatching(it).split(' ', '-') }.filter { it.length > 2 }.toSet()

    private fun isGrapeOrStyle(seg: String): Boolean {
        val norm = TextNormalizer.normalizeForMatching(seg).trim()
        val words = norm.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }
        if (words.isNotEmpty() && words.size <= 3 && words.all { it in GRAPE_WORDS }) return true
        return GrapeProfiles.profiles.containsKey(norm) ||
            GrapeProfiles.findInText(seg).any { TextNormalizer.normalizeForMatching(it.keyword) == norm }
    }

    /** A priced row that completes a priceless name row above it. */
    private fun looksLikeDetailLine(text: String, pending: Builder): Boolean {
        if (GeoLexicon.isGeographic(text)) return true
        val segments = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.size >= 2 && GeoLexicon.isGeographic(segments.last())) return true
        // Short tail with no vintage of its own when the name row had one: "Grand Cru Classé"
        val words = text.split(Regex("\\s+")).size
        return words <= 3 && VintageParser.parse(text).year == null && pending.vintage.year != null
    }

    // ---- Entry assembly ----------------------------------------------------

    private class Builder(val section: WineStyle?, val sectionTitle: String?) {
        var nameText = ""
        val details = mutableListOf<String>()
        val extraNameParts = mutableListOf<String>()
        val prices = mutableListOf<Double>()
        var currency: String? = null
        var halfBottle = false
        var note: String? = null
        val lines = mutableListOf<Int>()
        var vintage: VintageInfo = VintageInfo.UNKNOWN
        val hasNote get() = note != null

        fun start(split: PriceSplit, idx: List<Int>) {
            nameText = split.text
            vintage = VintageParser.parse(split.text)
            addPricesOnly(split)
            lines += idx
        }

        fun addPrices(split: PriceSplit, idx: List<Int>) { addPricesOnly(split); lines += idx }

        private fun addPricesOnly(split: PriceSplit) {
            prices += split.prices
            currency = currency ?: split.currency
            halfBottle = halfBottle || split.halfBottle
        }

        fun addDetailWithPrices(split: PriceSplit, idx: List<Int>) {
            if (vintage.year == null) VintageParser.parse(split.text).takeIf { it.isKnown }?.let { vintage = it }
            addContinuationText(VintageParser.strip(split.text, VintageParser.parse(split.text)))
            addPrices(split, idx)
        }

        fun addContinuation(text: String, idx: List<Int>) {
            addContinuationText(text)
            lines += idx
        }

        private fun addContinuationText(text: String) {
            for (seg in text.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
                if (GeoLexicon.isGeographic(seg) || GeoLexicon.containsPlace(seg) && seg.split(' ').size <= 3) details += seg
                else extraNameParts += seg
            }
        }

        fun addNote(text: String, idx: List<Int>) {
            note = if (note == null) text else "$note $text"
            lines += idx
        }

        fun build(currentYear: Int): MenuEntry? {
            if (nameText.isBlank()) return null
            var name = VintageParser.strip(nameText, vintage)
            name = cleanName(name)
            // Move trailing geography segments ("..., Champagne, France") into the detail.
            val segments = name.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            val trailingGeo = mutableListOf<String>()
            while (segments.size > 1 && GeoLexicon.isGeographic(segments.last())) trailingGeo.add(0, segments.removeLast())
            // "Petit Chablis France", "Allan Scott Marlborough NEW ZEALAND": countries printed without a comma.
            if (segments.isNotEmpty()) {
                var words = segments.last().split(' ').filter { it.isNotEmpty() }
                val otherWords = segments.dropLast(1).sumOf { seg -> seg.split(' ').count { it.isNotEmpty() } }
                var moved = true
                while (moved && words.size > 1 && words.size + otherWords > 2) {
                    moved = false
                    for (n in minOf(3, words.size - 1, words.size + otherWords - 2) downTo 1) {
                        val tail = words.takeLast(n).joinToString(" ")
                        val allCaps = tail.any { it.isLetter() } && tail == tail.uppercase()
                        if (GeoLexicon.isCountry(tail) || allCaps && GeoLexicon.isGeographic(tail)) {
                            trailingGeo.add(0, tail)
                            words = words.dropLast(n)
                            moved = true
                            break
                        }
                    }
                }
                segments[segments.size - 1] = words.joinToString(" ")
            }
            name = joinNameParts(segments, extraNameParts)
            if (name.count { it.isLetter() } < 3) return null
            val detail = (trailingGeo + details).distinct().joinToString(", ").ifBlank { null }
            return MenuEntry(
                name = name,
                detail = detail,
                vintage = vintage,
                prices = prices.toList(),
                currency = currency,
                section = section,
                sectionTitle = sectionTitle,
                tastingNote = note,
                halfBottle = halfBottle,
                lineIndices = lines.distinct()
            )
        }

        /** Grape continuations read naturally appended: "Hamilton Heights Unoaked Chardonnay". */
        private fun joinNameParts(segments: List<String>, extra: List<String>): String {
            val base = segments.joinToString(", ")
            if (extra.isEmpty()) return base
            return (base.trimEnd(',', ' ') + " " + extra.joinToString(" ")).trim()
        }
    }

    private val VOLUME = Regex("""(?i)\b\d{2,4}\s?(?:ml|cl)\b|\b\d(?:[.,]\d)?\s?l\b""")
    private val ABV = Regex("""\b\d{1,2}(?:[.,]\d)?\s?%(?:\s?(?:vol|abv|alc))?""", RegexOption.IGNORE_CASE)
    private val MARKERS = Regex("""\((?:v|ve|vg|o|b|n|org|bio)\)|\b(?:VG|VE)\b""")
    private val QUOTES = Regex("""["“”‘’'`]\s*([^"“”‘’'`]+?)\s*["“”‘’'`]""")

    fun cleanName(raw: String): String = raw
        .let { VOLUME.replace(it, " ") }
        .let { ABV.replace(it, " ") }
        .let { MARKERS.replace(it, " ") }
        .let { QUOTES.replace(it) { m -> " ${m.groupValues[1]} " } }
        .replace(Regex("""\(\s*\)"""), " ")
        .replace(Regex("""["“”]"""), " ")
        .replace(Regex("""\s+,"""), ",")
        .replace(Regex(""",(?=\S)"""), ", ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim(',', '-', '–', '·', '•', '*', ' ', '.', ':')
}
