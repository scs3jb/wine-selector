package com.wineselector.core.vintage

import com.wineselector.core.text.TextNormalizer

/** A region's vintage rating on a 1..5 scale (5 = exceptional). */
data class VintageRating(val regionKey: String, val regionLabel: String, val year: Int, val stars: Int) {
    val verdict: String get() = when (stars) {
        5 -> "Exceptional year"
        4 -> "Very good year"
        3 -> "Good year"
        2 -> "Challenging year"
        else -> "Poor year"
    }
}

/**
 * Approximate regional vintage chart for the world's most-listed classic regions.
 * The ratings are a consensus-style guide (1..5 stars), not a single critic's
 * scores, and only cover years with a reasonably clear reputation. Unknown
 * region/year pairs return null rather than guessing.
 */
object VintageGuide {

    private class Region(val key: String, val label: String, val keywords: List<String>, val ratings: Map<Int, Int>)

    private fun region(key: String, label: String, keywords: List<String>, vararg ratings: Pair<Int, Int>) =
        Region(key, label, keywords, ratings.toMap())

    private val REGIONS = listOf(
        region("bordeaux", "Bordeaux",
            listOf("bordeaux", "medoc", "haut-medoc", "pauillac", "margaux", "saint-julien", "st-julien", "saint-estephe",
                "st-estephe", "pessac", "graves", "saint-emilion", "st-emilion", "pomerol", "fronsac", "listrac", "moulis"),
            2000 to 5, 2001 to 4, 2002 to 3, 2003 to 4, 2004 to 3, 2005 to 5, 2006 to 4, 2007 to 3, 2008 to 4,
            2009 to 5, 2010 to 5, 2011 to 3, 2012 to 3, 2013 to 2, 2014 to 4, 2015 to 5, 2016 to 5, 2017 to 3,
            2018 to 5, 2019 to 5, 2020 to 5, 2021 to 3, 2022 to 5, 2023 to 4, 2024 to 3),
        region("burgundy", "Burgundy",
            listOf("burgundy", "bourgogne", "cote de nuits", "cote de beaune", "chablis", "gevrey", "vosne", "chambolle",
                "nuits-saint-georges", "meursault", "puligny", "chassagne", "pommard", "volnay", "beaune", "corton",
                "morey-saint-denis", "santenay", "mercurey", "givry", "macon", "pouilly-fuisse"),
            2002 to 4, 2003 to 3, 2004 to 3, 2005 to 5, 2006 to 4, 2007 to 3, 2008 to 3, 2009 to 4, 2010 to 5,
            2011 to 3, 2012 to 4, 2013 to 3, 2014 to 4, 2015 to 5, 2016 to 4, 2017 to 4, 2018 to 4, 2019 to 5,
            2020 to 5, 2021 to 3, 2022 to 5, 2023 to 4, 2024 to 3),
        region("champagne", "Champagne", listOf("champagne"),
            1996 to 5, 2002 to 5, 2004 to 4, 2006 to 4, 2008 to 5, 2009 to 4, 2011 to 2, 2012 to 5, 2013 to 4,
            2014 to 3, 2015 to 4, 2016 to 3, 2017 to 2, 2018 to 4, 2019 to 4, 2020 to 4, 2021 to 2, 2022 to 4),
        region("rhone", "Rhône",
            listOf("rhone", "chateauneuf", "gigondas", "vacqueyras", "hermitage", "crozes", "cote-rotie", "cote rotie",
                "cornas", "saint-joseph", "st-joseph", "lirac", "rasteau", "condrieu"),
            2003 to 4, 2005 to 4, 2006 to 4, 2007 to 5, 2008 to 2, 2009 to 4, 2010 to 5, 2011 to 3, 2012 to 4,
            2013 to 3, 2014 to 3, 2015 to 5, 2016 to 5, 2017 to 4, 2018 to 4, 2019 to 5, 2020 to 4, 2021 to 3,
            2022 to 4, 2023 to 4),
        region("loire", "Loire",
            listOf("loire", "sancerre", "pouilly-fume", "pouilly fume", "vouvray", "chinon", "bourgueil", "saumur",
                "savennieres", "muscadet", "touraine", "menetou"),
            2005 to 5, 2009 to 4, 2010 to 4, 2014 to 4, 2015 to 5, 2016 to 3, 2017 to 3, 2018 to 5, 2019 to 4,
            2020 to 4, 2021 to 2, 2022 to 4, 2023 to 3),
        region("piedmont", "Piedmont",
            listOf("piedmont", "piemonte", "barolo", "barbaresco", "langhe", "alba", "roero", "gattinara", "ghemme"),
            2001 to 5, 2002 to 2, 2003 to 3, 2004 to 5, 2005 to 3, 2006 to 4, 2007 to 4, 2008 to 4, 2009 to 3,
            2010 to 5, 2011 to 4, 2012 to 3, 2013 to 5, 2014 to 3, 2015 to 4, 2016 to 5, 2017 to 3, 2018 to 4,
            2019 to 5, 2020 to 4, 2021 to 5, 2022 to 4),
        region("tuscany", "Tuscany",
            listOf("tuscany", "toscana", "chianti", "brunello", "montalcino", "bolgheri", "nobile di montepulciano",
                "vino nobile", "maremma", "carmignano", "morellino"),
            2001 to 4, 2002 to 2, 2003 to 3, 2004 to 5, 2005 to 3, 2006 to 5, 2007 to 4, 2008 to 3, 2009 to 4,
            2010 to 5, 2011 to 4, 2012 to 4, 2013 to 4, 2014 to 2, 2015 to 5, 2016 to 5, 2017 to 3, 2018 to 4,
            2019 to 5, 2020 to 4, 2021 to 5, 2022 to 4),
        region("rioja", "Rioja", listOf("rioja"),
            2001 to 5, 2002 to 2, 2003 to 3, 2004 to 5, 2005 to 5, 2006 to 3, 2007 to 3, 2008 to 3, 2009 to 4,
            2010 to 5, 2011 to 4, 2012 to 4, 2013 to 2, 2014 to 3, 2015 to 4, 2016 to 5, 2017 to 3, 2018 to 4,
            2019 to 5, 2020 to 4, 2021 to 4, 2022 to 4),
        region("douro", "Douro & Port", listOf("douro", "porto", "port"),
            1994 to 5, 1997 to 4, 2000 to 5, 2003 to 4, 2007 to 4, 2011 to 5, 2015 to 4, 2016 to 5, 2017 to 5,
            2018 to 4, 2019 to 3, 2020 to 3, 2021 to 4, 2022 to 4),
        region("germany", "Mosel & Germany",
            listOf("mosel", "rheingau", "pfalz", "nahe", "rheinhessen", "saar", "ruwer", "germany", "deutschland"),
            2001 to 5, 2005 to 5, 2007 to 4, 2008 to 3, 2009 to 4, 2010 to 4, 2011 to 4, 2012 to 4, 2013 to 3,
            2014 to 3, 2015 to 5, 2016 to 4, 2017 to 4, 2018 to 4, 2019 to 5, 2020 to 4, 2021 to 4, 2022 to 4,
            2023 to 3),
        region("napa", "Napa & California",
            listOf("napa", "sonoma", "california", "paso robles", "santa barbara", "russian river", "oakville", "rutherford"),
            2001 to 5, 2002 to 4, 2003 to 3, 2004 to 4, 2005 to 4, 2006 to 4, 2007 to 5, 2008 to 4, 2009 to 4,
            2010 to 4, 2011 to 3, 2012 to 5, 2013 to 5, 2014 to 4, 2015 to 4, 2016 to 5, 2017 to 3, 2018 to 5,
            2019 to 5, 2020 to 2, 2021 to 5, 2022 to 4, 2023 to 4),
        region("oregon", "Oregon", listOf("oregon", "willamette"),
            2008 to 5, 2010 to 4, 2011 to 3, 2012 to 5, 2013 to 3, 2014 to 5, 2015 to 4, 2016 to 4, 2017 to 4,
            2018 to 5, 2019 to 4, 2020 to 2, 2021 to 5, 2022 to 4),
        region("south-australia", "South Australia",
            listOf("barossa", "mclaren vale", "south australia", "clare valley", "eden valley", "coonawarra"),
            2002 to 5, 2004 to 4, 2005 to 4, 2006 to 4, 2008 to 3, 2010 to 5, 2011 to 2, 2012 to 5, 2013 to 4,
            2014 to 3, 2015 to 4, 2016 to 4, 2017 to 4, 2018 to 5, 2019 to 4, 2020 to 3, 2021 to 5, 2022 to 4),
        region("marlborough", "Marlborough", listOf("marlborough"),
            2013 to 4, 2014 to 4, 2015 to 4, 2016 to 4, 2017 to 3, 2018 to 3, 2019 to 5, 2020 to 5, 2021 to 4,
            2022 to 3, 2023 to 3, 2024 to 4),
        region("mendoza", "Mendoza", listOf("mendoza", "uco valley", "lujan de cuyo"),
            2006 to 4, 2008 to 4, 2009 to 4, 2010 to 4, 2011 to 4, 2013 to 5, 2014 to 3, 2015 to 4, 2016 to 2,
            2017 to 5, 2018 to 5, 2019 to 5, 2020 to 4, 2021 to 4, 2022 to 4)
    )

    private val patterns: Map<String, Regex> = REGIONS.flatMap { it.keywords }.associateWith { kw ->
        Regex("""(?:^|[^a-z])${Regex.escape(kw)}(?:[^a-z]|$)""")
    }

    /**
     * Resolve a region key from any descriptive text (region name, appellation,
     * wine name, country). More specific keywords win: "Pauillac" → Bordeaux.
     */
    fun resolveRegion(vararg texts: String?): String? {
        val t = TextNormalizer.normalizeForMatching(texts.filterNotNull().joinToString(" | "))
            .replace(Regex("""\bsaint\s+"""), "saint-").replace(Regex("""\bst\.?\s+"""), "st-")
        var best: Pair<String, Int>? = null
        for (r in REGIONS) for (kw in r.keywords) {
            if (patterns.getValue(kw).containsMatchIn(t) && (best == null || kw.length > best.second)) {
                best = r.key to kw.length
            }
        }
        return best?.first
    }

    fun regionLabel(key: String): String? = REGIONS.firstOrNull { it.key == key }?.label

    fun rating(regionKey: String?, year: Int?): VintageRating? {
        if (regionKey == null || year == null) return null
        val r = REGIONS.firstOrNull { it.key == regionKey } ?: return null
        val stars = r.ratings[year] ?: return null
        return VintageRating(r.key, r.label, year, stars)
    }

    /** Best-rated years for a region (for "try instead" suggestions), newest first. */
    fun greatYears(regionKey: String, minStars: Int = 5): List<Int> =
        REGIONS.firstOrNull { it.key == regionKey }?.ratings?.filterValues { it >= minStars }?.keys?.sortedDescending()
            ?: emptyList()
}
