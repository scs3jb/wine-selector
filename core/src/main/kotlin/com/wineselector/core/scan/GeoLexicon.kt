package com.wineselector.core.scan

import com.wineselector.core.text.TextNormalizer

/** Countries, wine regions and geographic words used to separate "where" from "what". */
object GeoLexicon {

    private val PHRASES: Set<String> = setOf(
        // Countries
        "france", "italy", "italia", "spain", "espana", "portugal", "germany", "deutschland", "austria",
        "switzerland", "greece", "hungary", "romania", "bulgaria", "moldova", "georgia", "croatia", "slovenia",
        "lebanon", "israel", "turkey", "turkiye", "england", "english", "wales", "scotland", "uk", "united kingdom",
        "usa", "united states", "us", "canada", "mexico", "argentina", "chile", "uruguay", "brazil", "peru",
        "australia", "new zealand", "south africa", "china", "japan", "india",
        // Major regions & appellations
        "bordeaux", "burgundy", "bourgogne", "champagne", "loire", "loire valley", "rhone", "cotes du rhone",
        "alsace", "languedoc", "roussillon", "provence", "beaujolais", "jura", "savoie", "sud ouest", "gascogne",
        "pays d'oc", "rioja", "ribera del duero", "priorat", "rueda", "rias baixas", "navarra", "jumilla", "toro",
        "catalunya", "penedes", "la mancha", "castilla", "douro", "alentejo", "dao", "lisboa", "minho",
        "tuscany", "toscana", "piedmont", "piemonte", "veneto", "sicily", "sicilia", "puglia", "abruzzo", "friuli",
        "marche", "campania", "sardinia", "sardegna", "trentino", "alto adige", "umbria", "lombardy", "lombardia",
        "mosel", "rheingau", "pfalz", "nahe", "rheinhessen", "baden", "wachau", "kamptal", "kremstal", "burgenland",
        "napa", "napa valley", "sonoma", "sonoma coast", "california", "oregon", "willamette valley", "washington",
        "washington state", "central coast", "paso robles", "finger lakes", "marlborough", "hawkes bay",
        "central otago", "martinborough", "barossa", "barossa valley", "mclaren vale", "margaret river", "yarra valley",
        "coonawarra", "hunter valley", "adelaide hills", "clare valley", "eden valley", "south australia",
        "western australia", "victoria", "tasmania", "mendoza", "salta", "patagonia", "uco valley", "maipo",
        "maipo valley", "colchagua", "casablanca", "rapel", "rapel valley", "central valley", "aconcagua",
        "stellenbosch", "swartland", "western cape", "franschhoek", "paarl", "constantia", "walker bay",
        "cornwall", "sussex", "kent", "hampshire", "banat", "tokaj", "bekaa", "bekaa valley", "valle central",
        "languedoc-roussillon", "cotes de provence", "cotes de gascogne", "vin de france", "igp", "igt", "doc",
        "docg", "aoc", "aop", "do", "doca", "ava", "vdp", "gi", "wo"
    ).map { TextNormalizer.normalizeForMatching(it) }.toSet()

    private val COUNTRIES: Set<String> = setOf(
        "france", "italy", "italia", "spain", "espana", "portugal", "germany", "deutschland", "austria",
        "switzerland", "greece", "hungary", "romania", "bulgaria", "moldova", "georgia", "croatia", "slovenia",
        "lebanon", "israel", "turkey", "turkiye", "england", "wales", "uk", "usa", "canada", "mexico", "argentina",
        "chile", "uruguay", "brazil", "peru", "australia", "new zealand", "south africa", "china", "japan"
    )

    /** True if [text] is exactly a country name ("FRANCE", "New Zealand"). */
    fun isCountry(text: String): Boolean =
        TextNormalizer.normalizeForMatching(text).replace(Regex("[^a-z ]"), " ").trim()
            .replace(Regex("\\s+"), " ") in COUNTRIES

    private val GEO_WORDS = setOf(
        "valley", "valle", "coast", "hills", "region", "county", "vale", "river", "highlands", "mountains",
        "north", "south", "east", "west", "central", "upper", "lower", "de", "del", "di", "du", "da", "d"
    )

    private val maxPhraseWords = PHRASES.maxOf { it.split(' ').size }

    private fun words(text: String): List<String> =
        TextNormalizer.normalizeForMatching(text).replace(Regex("[^a-z'\\- ]"), " ")
            .replace("-", " ").split(Regex("\\s+")).filter { it.isNotEmpty() }

    /** Count of words in [text] covered by known geographic phrases. */
    private fun geoCoverage(ws: List<String>): Pair<Int, Int> {
        var covered = 0
        var strong = 0
        var i = 0
        while (i < ws.size) {
            var matched = 0
            for (n in minOf(maxPhraseWords, ws.size - i) downTo 1) {
                if (ws.subList(i, i + n).joinToString(" ") in PHRASES) { matched = n; break }
            }
            if (matched > 0) { covered += matched; strong++; i += matched }
            else { if (ws[i] in GEO_WORDS) covered++; i++ }
        }
        return covered to strong
    }

    /** True if every word is geographic ("Rapel Valley", "Marlborough, New Zealand"). */
    fun isGeographic(text: String): Boolean {
        val ws = words(text)
        if (ws.isEmpty()) return false
        val (covered, strong) = geoCoverage(ws)
        if (covered == ws.size && strong > 0) return true
        // "Rapel Valley", "Loire Valley": one unknown place name + a geo word.
        return ws.size <= 3 && strong == 0 && ws.count { it in GEO_WORDS } >= 1 &&
            ws.last() in setOf("valley", "coast", "hills", "region", "county", "vale", "highlands")
    }

    /** True if the text names at least one country or known region. */
    fun containsPlace(text: String): Boolean = geoCoverage(words(text)).second > 0
}
