package com.wineselector.app.data

/**
 * On-device wine pairing engine. Uses keyword matching against a knowledge base
 * of grape varieties, wine regions, and styles to score wines against food categories.
 */
class WinePairingEngine(private val xWinesDb: XWinesDatabase? = null) {

    enum class MatchSource { XWINES, KEYWORD, SECTION_CONTEXT }

    data class ScoredWine(
        val originalText: String,
        val score: Int,
        val reason: String,
        val xWinesMatch: XWineEntry? = null,
        val fullEntryText: String? = null,
        val priceText: String? = null,
        val displayName: String? = null,
        val vintageMatch: VintageMatch = VintageMatch.NOT_CHECKED,
        val ocrYear: Int? = null,
        val matchSource: MatchSource = MatchSource.KEYWORD
    )

    /**
     * Each entry maps a keyword (grape, region, or style) to a map of FoodCategory -> score (1-10)
     * plus a short description for pairing reasoning.
     */
    private data class WineProfile(
        val scores: Map<FoodCategory, Int>,
        val description: String,
        val type: WineType? = null  // null = ambiguous/region blend
    )

    private val wineKeywords: Map<String, WineProfile> = buildMap {
        // --- RED GRAPES ---
        put("cabernet sauvignon", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4,
                FoodCategory.VEGETARIAN to 3, FoodCategory.PIZZA to 6),
            "Full-bodied red with firm tannins that cut through rich, fatty meats",
            WineType.RED
        ))
        put("cabernet", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4),
            "Full-bodied red with firm tannins that cut through rich, fatty meats",
            WineType.RED
        ))
        put("merlot", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 7, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 7, FoodCategory.VEGETARIAN to 5),
            "Medium-bodied, smooth red that pairs broadly with meats and pasta",
            WineType.RED
        ))
        put("pinot noir", WineProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.PORK to 8, FoodCategory.LAMB to 7,
                FoodCategory.FISH to 6, FoodCategory.PASTA to 7, FoodCategory.BEEF to 5,
                FoodCategory.CHEESE to 7, FoodCategory.SUSHI to 5, FoodCategory.VEGETARIAN to 7,
                FoodCategory.PIZZA to 6),
            "Light, elegant red with earthy notes — extremely versatile with lighter dishes",
            WineType.RED
        ))
        put("malbec", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6),
            "Bold, juicy red with dark fruit — a classic steak wine",
            WineType.RED
        ))
        put("syrah", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 5),
            "Spicy, peppery red that stands up to bold, gamey flavors",
            WineType.RED
        ))
        put("shiraz", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 6),
            "Bold, fruit-forward red with spice — great with grilled meats",
            WineType.RED
        ))
        put("zinfandel", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 5),
            "Jammy, bold red with high fruit — loves BBQ and spiced dishes",
            WineType.RED
        ))
        put("tempranillo", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 5),
            "Medium-bodied Spanish red with savory leather and cherry notes",
            WineType.RED
        ))
        put("sangiovese", WineProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6,
                FoodCategory.CHEESE to 7, FoodCategory.VEGETARIAN to 6),
            "Italian red with high acidity — born for tomato-based dishes",
            WineType.RED
        ))
        put("nebbiolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 6),
            "Powerful, tannic Italian red with roses and tar — pairs with rich dishes",
            WineType.RED
        ))
        put("grenache", WineProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 6, FoodCategory.VEGETARIAN to 6),
            "Fruity, spicy red that works with a wide range of Mediterranean dishes",
            WineType.RED
        ))
        put("barbera", WineProfile(
            mapOf(FoodCategory.PASTA to 9, FoodCategory.PIZZA to 8, FoodCategory.PORK to 7,
                FoodCategory.BEEF to 6, FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "High-acid Italian red — excellent with tomato sauces and cured meats",
            WineType.RED
        ))
        put("primitivo", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 7),
            "Rich, ripe red similar to Zinfandel — pairs with hearty, grilled fare",
            WineType.RED
        ))

        // --- WHITE GRAPES ---
        put("chardonnay", WineProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 6,
                FoodCategory.CHEESE to 6),
            "Rich white with buttery notes — ideal with poultry and creamy sauces",
            WineType.WHITE
        ))
        put("sauvignon blanc", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.CHICKEN to 7,
                FoodCategory.VEGETARIAN to 8, FoodCategory.SUSHI to 7, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 5),
            "Crisp, zesty white with herbal notes — perfect with seafood and salads",
            WineType.WHITE
        ))
        put("riesling", WineProfile(
            mapOf(FoodCategory.SUSHI to 9, FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.PORK to 7, FoodCategory.VEGETARIAN to 7,
                FoodCategory.DESSERT to 6, FoodCategory.CHEESE to 6),
            "Aromatic white with bright acidity — versatile, especially with Asian cuisine",
            WineType.WHITE
        ))
        put("pinot grigio", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6,
                FoodCategory.PIZZA to 5),
            "Light, refreshing white — a safe, easy-drinking choice with lighter fare",
            WineType.WHITE
        ))
        put("pinot gris", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.PORK to 6),
            "Fuller-bodied style of Pinot Grigio with stone fruit notes",
            WineType.WHITE
        ))
        put("viognier", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Aromatic, full white with peach and floral notes",
            WineType.WHITE
        ))
        put("gewurztraminer", WineProfile(
            mapOf(FoodCategory.SUSHI to 8, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 6),
            "Intensely aromatic white with lychee and spice — great with Asian food",
            WineType.WHITE
        ))
        put("gruner veltliner", WineProfile(
            mapOf(FoodCategory.VEGETARIAN to 8, FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.SUSHI to 7, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 6),
            "Crisp Austrian white with white pepper — excellent with vegetables",
            WineType.WHITE
        ))
        put("albarino", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Bright Spanish white with citrus and salinity — made for shellfish",
            WineType.WHITE
        ))
        put("muscadet", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.SUSHI to 6,
                FoodCategory.VEGETARIAN to 5),
            "Bone-dry, mineral French white — the classic oyster wine",
            WineType.WHITE
        ))
        put("chenin blanc", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.PORK to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.SEAFOOD to 6, FoodCategory.CHEESE to 6,
                FoodCategory.DESSERT to 5),
            "Versatile white ranging from dry to sweet — pairs broadly",
            WineType.WHITE
        ))
        put("semillon", WineProfile(
            mapOf(FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.CHEESE to 6, FoodCategory.DESSERT to 5),
            "Waxy, full white with honey notes",
            WineType.WHITE
        ))

        // --- ROSÉ ---
        put("rosé", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry rosé is extremely versatile — a great crowd-pleaser",
            WineType.ROSE
        ))
        put("rose", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry rosé is extremely versatile — a great crowd-pleaser",
            WineType.ROSE
        ))

        // --- SPARKLING ---
        put("champagne", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 5),
            "Sparkling wine with high acidity and bubbles that cleanse the palate",
            WineType.WHITE
        ))
        put("prosecco", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 7, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5, FoodCategory.DESSERT to 5),
            "Light, fruity sparkling — refreshing aperitif or light food pairing",
            WineType.WHITE
        ))
        put("cava", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Spanish sparkling with citrus and toast — great value bubbly",
            WineType.WHITE
        ))
        put("sparkling", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Bubbles and acidity make sparkling wine a versatile food partner",
            WineType.WHITE
        ))

        // --- DESSERT WINES ---
        put("moscato", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 6, FoodCategory.SUSHI to 4),
            "Sweet, lightly sparkling wine — a natural dessert companion",
            WineType.WHITE
        ))
        put("port", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 9, FoodCategory.BEEF to 4),
            "Rich, sweet fortified wine — classic with chocolate and blue cheese",
            WineType.RED
        ))
        put("sauternes", WineProfile(
            mapOf(FoodCategory.DESSERT to 10, FoodCategory.CHEESE to 8, FoodCategory.FISH to 4),
            "Luscious sweet French wine — the ultimate dessert pairing",
            WineType.WHITE
        ))
        put("ice wine", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes",
            WineType.WHITE
        ))
        put("icewine", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes",
            WineType.WHITE
        ))

        // --- REGIONAL / BLENDS ---
        put("bordeaux", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.CHEESE to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 5),
            "Classic Bordeaux blend — structured, age-worthy, and built for red meat",
            WineType.RED
        ))
        put("burgundy", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 6),
            "Elegant Burgundy — Pinot Noir or Chardonnay depending on color"
        ))
        put("bourgogne", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7),
            "Elegant Burgundy — Pinot Noir or Chardonnay depending on color"
        ))
        put("chianti", WineProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 5),
            "Tuscan Sangiovese — the definitive Italian food wine",
            WineType.RED
        ))
        put("barolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 5),
            "King of Italian wines — powerful Nebbiolo with truffle and tar",
            WineType.RED
        ))
        put("barbaresco", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 7, FoodCategory.PORK to 6),
            "Elegant Nebbiolo — slightly lighter than Barolo, equally food-friendly",
            WineType.RED
        ))
        put("rioja", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 5),
            "Spanish Tempranillo — oaky, savory, built for grilled meats",
            WineType.RED
        ))
        put("cotes du rhone", WineProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5),
            "Southern Rhône blend — fruity, spicy, great value",
            WineType.RED
        ))
        put("chateauneuf", WineProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Complex Rhône blend — rich and powerful with herbal garrigue notes",
            WineType.RED
        ))
        put("sancerre", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 8, FoodCategory.CHEESE to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6),
            "Loire Sauvignon Blanc — crisp and mineral with goat cheese affinity",
            WineType.WHITE
        ))
        put("chablis", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Unoaked Burgundy Chardonnay — steely, mineral, built for shellfish",
            WineType.WHITE
        ))
        put("pouilly", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 8, FoodCategory.CHICKEN to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Loire white — crisp, elegant, great with lighter fare",
            WineType.WHITE
        ))
        put("valpolicella", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 7, FoodCategory.BEEF to 6,
                FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6),
            "Light Italian red — fresh cherry fruit, great with everyday Italian food",
            WineType.RED
        ))
        put("amarone", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.CHEESE to 8,
                FoodCategory.PASTA to 6),
            "Rich, dried-grape Italian red — intense and powerful, pairs with bold dishes",
            WineType.RED
        ))
        put("beaujolais", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.PORK to 7, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.FISH to 5,
                FoodCategory.VEGETARIAN to 6),
            "Light, fruity Gamay — serve slightly chilled with lighter dishes",
            WineType.RED
        ))
        put("montepulciano", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.BEEF to 7,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6),
            "Full-bodied Italian red — dark fruit and soft tannins, great with red sauce",
            WineType.RED
        ))
    }

    companion object {
        private val VINTAGE_PATTERN = Regex("""\b(19|20)\d{2}\b""")
        private val PRICE_PATTERN = Regex("""\$\s*\d|\d+\s*€|€\s*\d|\d+\s*£|£\s*\d|\d+\.\d{2}""")
        private val VOLUME_PATTERN = Regex("""\b\d+\s*ml\b""", RegexOption.IGNORE_CASE)

        // Glass/bottle price format — "13/41", "28/104"
        private val GLASS_BOTTLE_PATTERN = Regex("""\b\d{1,4}/\d{1,4}\b""")

        // Bare trailing number — a 2-5 digit number at the end of a line,
        // used to detect prices without currency symbols (e.g., "130", "600").
        private val BARE_TRAILING_NUMBER = Regex("""(?:^|\s)(\d{2,5})\s*$""")

        fun hasBareTrailingPrice(line: String): Boolean {
            val match = BARE_TRAILING_NUMBER.find(line) ?: return false
            val num = match.groupValues[1].toIntOrNull() ?: return false
            return num !in 1900..2099 // exclude vintage years
        }

        /**
         * Comprehensive price detection — checks currency symbols, glass/bottle
         * format, and bare trailing numbers. Use this instead of PRICE_PATTERN
         * alone when you need to detect prices in all formats.
         */
        fun lineHasPrice(line: String): Boolean {
            if (PRICE_PATTERN.containsMatchIn(line)) return true
            if (GLASS_BOTTLE_PATTERN.containsMatchIn(line)) return true
            if (hasBareTrailingPrice(line)) return true
            return false
        }

        // Continuation suffixes — "Pinot Noir cont.", "Reds (continued)", etc.
        private val CONTINUATION_PATTERN = Regex(
            """(?i)\b(?:cont\.?|cont'd|continued)\s*\.?\s*$|\((?:cont\.?|cont'd|continued)\)\s*$"""
        )

        // Header phrases — these are section titles, not individual wines
        private val HEADER_PHRASES = setOf(
            "wines", "wine", "wine list", "wine selection", "wine & beer",
            "vins", "vins blancs", "vins rouges", "vini", "vini italiani",
            "reds", "whites", "red wines", "white wines",
            "rosés", "sparkling wines", "dessert wines", "sweet wines",
            "sparklings", "champagnes",
            "selection", "list", "carte", "carte des vins", "menu",
            "by the glass", "by the bottle", "bottles", "glasses",
            "premium", "premium bottles", "reserve list",
            "house wines", "house wines by the glass",
            "beer", "cocktails", "spirits", "aperitifs",
            "bianchi", "rossi", "italian reds", "italian whites",
            "appetizers", "entrees", "desserts", "sides"
        )
    }

    /**
     * Returns true if a line is a section header rather than an actual wine entry.
     * A header is a short, generic label with no vintage, price, or volume.
     * When a header IS a wine keyword (e.g., "CHAMPAGNE"), the keyword gets
     * carried as section context so that wines listed under it inherit the type.
     */
    private fun isSectionHeader(line: String): Boolean {
        val lower = line.lowercase().trim()

        // Lines with a vintage year, price, or volume are wine entries
        if (VINTAGE_PATTERN.containsMatchIn(line)) return false
        if (lineHasPrice(line)) return false
        if (VOLUME_PATTERN.containsMatchIn(line)) return false

        // Continuation headers: "Pinot Noir cont.", "Cabernet Sauvignon (continued)", etc.
        // These are section continuation labels on multi-page menus, not wine entries.
        if (CONTINUATION_PATTERN.containsMatchIn(lower)) return true

        val stripped = lower.replace(Regex("[^a-z\\s]"), "").trim()
        val wordCount = stripped.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

        // Exact match to a known header phrase
        if (stripped in HEADER_PHRASES) return true

        // Short ALL-CAPS lines with no year/price are headers
        // (e.g., "ROSÉ", "CHAMPAGNE", "SPARKLING", "VINS ROUGES")
        if (wordCount <= 4) {
            val lettersOnly = line.filter { it.isLetter() }
            if (lettersOnly.isNotEmpty() && lettersOnly == lettersOnly.uppercase()) {
                return true
            }
        }

        return false
    }

    /**
     * Returns true if a line is a bare wine keyword that should be treated as
     * a section header when it starts a new group (after a blank line or header).
     * E.g., "Champagne", "Rosé", "Merlot" — but NOT when appearing mid-entry
     * as a grape variety under a producer name.
     */
    private fun isBareKeywordLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        if (VINTAGE_PATTERN.containsMatchIn(line)) return false
        if (lineHasPrice(line)) return false
        if (VOLUME_PATTERN.containsMatchIn(line)) return false

        val wordCount = lower.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount > 3) return false

        for (keyword in wineKeywords.keys) {
            if (lower == keyword || lower == "${keyword}s" ||
                lower == "$keyword wines" || lower == "$keyword wine") {
                return true
            }
        }
        return false
    }

    private val PRICE_LINE_PATTERN = Regex(
        """(?i)\b(glass|bottle|btl|carafe|split|half|magnum)\b"""
    )

    /**
     * Returns true if a line plausibly looks like a wine entry (producer name, label,
     * vintage, etc.) vs. a bare price line, single region, or logistics text.
     * Used to gate section-context inheritance so "Glass $12 | Bottle $44" doesn't
     * inherit the "champagne" section score.
     */
    private fun looksLikeWineEntry(line: String): Boolean {
        // Lines that are price/serving info — never wine names
        if (PRICE_LINE_PATTERN.containsMatchIn(line) && lineHasPrice(line)) {
            return false
        }
        // Has a vintage year — almost certainly a wine entry
        if (VINTAGE_PATTERN.containsMatchIn(line)) return true
        // Contains "NV" (non-vintage) — wine label
        if (line.contains(Regex("""\bNV\b"""))) return true
        // Has at least 2 capitalized words (producer + name) and > 1 word total
        val words = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2) return false
        val capitalizedWords = words.count { it[0].isUpperCase() && it.length > 1 }
        if (capitalizedWords >= 2) return true
        return false
    }

    /**
     * Returns true if a coalesced entry is just a bare wine keyword with no
     * producer name, vintage, price, or other identifying detail.
     * E.g., "Champagne" or "Merlot" but NOT "Château Margaux Merlot 2019 $120".
     */
    private fun isBareKeywordEntry(entry: WineEntry): Boolean {
        val text = entry.combinedText
        // Has price, vintage, NV, or volume → not bare
        if (lineHasPrice(text)) return false
        if (VINTAGE_PATTERN.containsMatchIn(text)) return false
        if (text.contains(Regex("""\bNV\b"""))) return false
        if (VOLUME_PATTERN.containsMatchIn(text)) return false

        val lower = text.lowercase().trim()
        val wordCount = lower.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

        // Very short entries (1-3 words) that match a keyword are likely labels
        if (wordCount > 3) return false

        for (keyword in wineKeywords.keys) {
            if (lower == keyword || lower == "${keyword}s" ||
                lower == "$keyword wines" || lower == "$keyword wine") {
                return true
            }
        }
        return false
    }

    /**
     * Extract the wine keyword that a section header represents, if any.
     * E.g., "CHAMPAGNE" -> "champagne", "ROSÉ" -> "rosé", "SPARKLING" -> "sparkling"
     */
    private fun extractSectionKeyword(headerLine: String): String? {
        val lower = headerLine.lowercase().trim()
        // Check if the header itself is or contains a wine keyword
        for (keyword in wineKeywords.keys) {
            if (lower.contains(keyword)) return keyword
        }
        return null
    }

    /**
     * A wine entry coalesced from one or more consecutive OCR lines.
     * E.g., "Terrazas de los Andes Reserva" + "Cabernet Sauvignon" +
     * "Mendoza, Argentina 2019" + "Glass $14 | Bottle $55"
     */
    private data class WineEntry(
        val lines: List<String>,
        val combinedText: String,
        val displayName: String,
        val priceText: String?
    )

    /**
     * Returns true if this line looks like it starts a new wine entry — i.e., it
     * has a vintage year or a producer-style name (multiple capitalized words)
     * and is NOT just a price/serving line.
     */
    private fun looksLikeNewWineStart(line: String): Boolean {
        // Price-only or serving-only lines don't start entries
        if (PRICE_LINE_PATTERN.containsMatchIn(line) && lineHasPrice(line)) {
            return false
        }
        // Lines that are just a price (e.g., "$55") don't start entries
        if (line.trim().matches(Regex("""\$\s*\d+\.?\d*"""))) return false

        // Has a vintage year — strong indicator of a new wine entry
        if (VINTAGE_PATTERN.containsMatchIn(line)) return true

        // Has a price AND some text — likely a self-contained wine line
        if (lineHasPrice(line)) {
            val textWithoutPrice = line
                .replace(PRICE_PATTERN, "")
                .replace(GLASS_BOTTLE_PATTERN, "")
                .replace(BARE_TRAILING_NUMBER, "")
                .trim()
            if (textWithoutPrice.length > 3) return true
        }

        // Multi-word line starting with a capital letter and containing another
        // capitalized word — looks like "Producer Name" or "Wine Label 2019"
        val words = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size >= 2) {
            val startsWithCap = words[0].firstOrNull()?.isUpperCase() == true
            val hasAnotherCap = words.drop(1).any { it.firstOrNull()?.isUpperCase() == true }
            if (startsWithCap && hasAnotherCap) return true
        }

        return false
    }

    /**
     * Determine if we should split before this line — i.e., the previous
     * accumulated lines form a complete entry and this line starts a new one.
     * Heuristics: previous group already has a price line, and this line
     * looks like a new wine start.
     */
    private fun shouldSplitBefore(currentLines: List<String>, nextLine: String): Boolean {
        if (currentLines.isEmpty()) return false

        val prevHasPrice = currentLines.any { lineHasPrice(it) }
        val prevHasVintage = currentLines.any { VINTAGE_PATTERN.containsMatchIn(it) }

        // If previous group has a price and this line looks like a new wine, split
        if (prevHasPrice && looksLikeNewWineStart(nextLine)) return true

        // If previous group has both a vintage and 2+ lines, and this line looks
        // like a new wine entry (not a region/price continuation), split
        if (prevHasVintage && currentLines.size >= 2 && looksLikeNewWineStart(nextLine)) return true

        // If the previous group has 4+ lines, this is getting long — split on
        // any line that has a vintage or starts with a capital producer name
        if (currentLines.size >= 4 && looksLikeNewWineStart(nextLine)) return true

        return false
    }

    /**
     * Group consecutive non-header, non-blank lines into wine entries.
     * Entries are separated by blank lines, section headers, or detected
     * entry boundaries (e.g., a new vintage/price after a previous entry's price).
     * Returns pairs of (entry, sectionKeyword) where sectionKeyword is the
     * active section context for that entry.
     */
    private fun coalesceEntries(rawLines: List<String>): List<Pair<WineEntry, String?>> {
        val result = mutableListOf<Pair<WineEntry, String?>>()
        var sectionKeyword: String? = null
        var currentLines = mutableListOf<String>()

        fun flushEntry() {
            if (currentLines.isEmpty()) return
            val combined = currentLines.joinToString(" ")
            // Display name = first non-price line (the wine/producer name)
            val nameLine = currentLines.firstOrNull { !PRICE_LINE_PATTERN.containsMatchIn(it) || !lineHasPrice(it) }
                ?: currentLines.first()
            // Find price from any line in the group — try currency patterns
            // first, then glass/bottle format, then bare trailing numbers
            val priceText = currentLines.firstOrNull { PRICE_PATTERN.containsMatchIn(it) }
                ?: currentLines.firstOrNull { GLASS_BOTTLE_PATTERN.containsMatchIn(it) }
                ?: currentLines.lastOrNull { hasBareTrailingPrice(it) }
            result.add(Pair(
                WineEntry(
                    lines = currentLines.toList(),
                    combinedText = combined,
                    displayName = nameLine,
                    priceText = priceText
                ),
                sectionKeyword
            ))
            currentLines = mutableListOf()
        }

        for (rawLine in rawLines) {
            val line = rawLine.trim()
            if (line.length <= 2) {
                // Blank or very short line — entry separator
                flushEntry()
                continue
            }
            if (isSectionHeader(line)) {
                flushEntry()
                sectionKeyword = extractSectionKeyword(line)
                continue
            }
            // Bare keyword lines (e.g., "Champagne", "Rosé", "Merlot") at the
            // start of a group are section headers, not wine entries. Mid-entry
            // they're grape varieties (e.g., line 2 under a producer name).
            if (currentLines.isEmpty() && isBareKeywordLine(line)) {
                flushEntry()
                sectionKeyword = extractSectionKeyword(line)
                continue
            }
            // Detect entry boundaries within continuous OCR text — split when
            // we detect a new wine entry starting after a previous complete entry
            if (shouldSplitBefore(currentLines, line)) {
                flushEntry()
            }
            currentLines.add(line)
        }
        flushEntry()
        return result
    }

    /**
     * Analyze extracted wine list text and return scored recommendations for the given food.
     *
     * Two-pass architecture:
     * 1. X-Wines database matching (when available) — matches exact wine names with vintage awareness
     * 2. Keyword fallback — for entries not matched by X-Wines, uses grape/region keyword profiles
     *
     * Falls back entirely to keyword matching when no X-Wines database is loaded.
     */
    fun recommendWines(
        extractedText: String,
        food: FoodCategory,
        preferences: WinePreferences = WinePreferences()
    ): List<ScoredWine> {
        val entries = coalesceEntries(extractedText.lines())
        val scored = mutableListOf<ScoredWine>()

        // === Pass 1: X-Wines database matching (when available) ===
        val xWinesMatchedKeys = mutableSetOf<String>()

        if (xWinesDb != null) {
            for ((entry, sectionKw) in entries) {
                val matchResult = xWinesDb.findMatchWithVintage(entry.combinedText) ?: continue
                val xEntry = matchResult.entry

                // Apply user preference filters
                if (!preferences.acceptsType(xEntry.type)) continue
                if (!preferences.acceptsGrapes(xEntry.grapes)) continue
                if (!preferences.acceptsPrice(entry.priceText)) continue

                val harmonizes = xWinesDb.harmonizesWithFood(xEntry, food)
                val grapeStr = if (xEntry.grapes.isNotEmpty()) xEntry.grapes.joinToString(", ") else xEntry.type

                // Compute stable base score from grape inference, keywords,
                // and section context — same sources Pass 2 would use.
                val grapeInference = inferScoreFromGrapes(xEntry.grapes, food)
                val grapeScore = grapeInference?.first ?: 0

                val entryLower = entry.combinedText.lowercase()
                var keywordScore = 0
                var keywordReason = ""
                for ((keyword, profile) in wineKeywords) {
                    if (entryLower.contains(keyword)) {
                        if (profile.type != null && !preferences.allowedTypes.contains(profile.type)) continue
                        val s = profile.scores[food] ?: 0
                        if (s > keywordScore) {
                            keywordScore = s
                            keywordReason = profile.description
                        }
                    }
                }

                if (keywordScore == 0 && sectionKw != null) {
                    val profile = wineKeywords[sectionKw]
                    if (profile != null && (profile.type == null || preferences.allowedTypes.contains(profile.type))) {
                        val s = profile.scores[food] ?: 0
                        if (s > 0) {
                            keywordScore = s
                            keywordReason = profile.description
                        }
                    }
                }

                val baseScore: Int
                val baseReason: String
                when {
                    grapeScore >= keywordScore && grapeScore > 0 -> {
                        baseScore = grapeScore
                        baseReason = grapeInference!!.second
                    }
                    keywordScore > 0 -> {
                        baseScore = keywordScore
                        baseReason = keywordReason
                    }
                    else -> {
                        baseScore = 0
                        baseReason = ""
                    }
                }

                // Apply harmonization as a bonus on top of the stable base,
                // so rankings are anchored by keyword scores (deterministic)
                // and X-Wines only fine-tunes.
                val score: Int
                val reason: String
                if (harmonizes && baseScore > 0) {
                    score = minOf(baseScore + 2, 10)
                    reason = baseReason
                } else if (harmonizes) {
                    // No keyword/grape base — fall back to X-Wines rating
                    score = when {
                        xEntry.averageRating != null && xEntry.averageRating >= 4.0f -> 8
                        xEntry.averageRating != null && xEntry.averageRating >= 3.5f -> 7
                        else -> 6
                    }
                    reason = "$grapeStr from ${xEntry.regionName}, ${xEntry.country} \u2014 " +
                        "database confirms food pairing"
                } else if (baseScore > 0) {
                    score = baseScore
                    reason = baseReason
                } else {
                    score = when {
                        xEntry.averageRating != null && xEntry.averageRating >= 4.0f -> 5
                        xEntry.averageRating != null && xEntry.averageRating >= 3.5f -> 4
                        else -> 3
                    }
                    reason = "$grapeStr from ${xEntry.regionName}, ${xEntry.country} \u2014 " +
                        "pairing unconfirmed, but a highly rated wine"
                }

                val entryKey = entry.combinedText.lowercase().replace(Regex("[^a-z]"), "")
                xWinesMatchedKeys.add(entryKey)

                // Always use the menu text as display name — X-Wines is for
                // enrichment only, the user needs to find the wine on their menu
                val displayName = bestWineDisplayName(entry) ?: entry.displayName

                scored.add(ScoredWine(
                    originalText = entry.combinedText,
                    score = score,
                    reason = reason,
                    xWinesMatch = xEntry,
                    fullEntryText = entry.combinedText,
                    priceText = entry.priceText,
                    displayName = displayName,
                    vintageMatch = matchResult.vintageMatch,
                    ocrYear = matchResult.ocrYear,
                    matchSource = MatchSource.XWINES
                ))
            }
        }

        // === Pass 2: Keyword fallback (for entries not matched by X-Wines) ===
        for ((entry, sectionKw) in entries) {
            val entryKey = entry.combinedText.lowercase().replace(Regex("[^a-z]"), "")
            if (entryKey in xWinesMatchedKeys) continue

            // Apply price filter to keyword matches too
            if (!preferences.acceptsPrice(entry.priceText)) continue

            // Skip entries that are just bare wine keywords (e.g., "Champagne",
            // "Merlot") — these are section labels that slipped past header
            // detection, not specific wine listings.
            if (isBareKeywordEntry(entry) && sectionKw == null) continue

            val lower = entry.combinedText.lowercase()
            var bestScore = 0
            var bestReason = ""

            for ((keyword, profile) in wineKeywords) {
                if (lower.contains(keyword)) {
                    // Apply type filter for keyword matches
                    if (profile.type != null && !preferences.allowedTypes.contains(profile.type)) continue
                    val score = profile.scores[food] ?: 0
                    if (score > bestScore) {
                        bestScore = score
                        bestReason = profile.description
                    }
                }
            }

            // Section context fallback
            if (bestScore == 0 && sectionKw != null && looksLikeWineEntry(entry.displayName)) {
                val profile = wineKeywords[sectionKw]
                if (profile != null) {
                    // Apply type filter
                    if (profile.type == null || preferences.allowedTypes.contains(profile.type)) {
                        val score = profile.scores[food] ?: 0
                        if (score > 0) {
                            bestScore = score
                            bestReason = profile.description
                        }
                    }
                }
            }

            // X-Wines grape fallback — when neither keyword nor section context matched,
            // try identifying the wine via X-Wines and infer score from its grapes
            if (bestScore == 0 && xWinesDb != null) {
                val xMatch = xWinesDb.findMatch(entry.combinedText)
                if (xMatch != null && preferences.acceptsGrapes(xMatch.grapes) &&
                    preferences.acceptsType(xMatch.type)) {
                    val grapeInference = inferScoreFromGrapes(xMatch.grapes, food)
                    if (grapeInference != null) {
                        bestScore = grapeInference.first
                        bestReason = grapeInference.second
                    }
                }
            }

            if (bestScore > 0) {
                // Still do X-Wines lookup for enrichment data (but scoring is keyword-based)
                var xMatch = xWinesDb?.findMatch(entry.combinedText)
                // Filter enrichment match by grape preferences
                if (xMatch != null && !preferences.acceptsGrapes(xMatch.grapes)) {
                    xMatch = null
                }
                if (xMatch != null && xWinesDb?.harmonizesWithFood(xMatch, food) == true) {
                    bestScore = minOf(bestScore + 1, 10)
                }

                val matchSource = if (sectionKw != null && bestReason == (wineKeywords[sectionKw]?.description ?: ""))
                    MatchSource.SECTION_CONTEXT else MatchSource.KEYWORD

                // Build the best display name — prefer a line with the wine
                // keyword and a vintage/producer, falling back to the entry's
                // display name (first non-price line)
                val bestDisplayName = bestWineDisplayName(entry) ?: entry.displayName

                scored.add(ScoredWine(
                    originalText = entry.combinedText,
                    score = bestScore,
                    reason = bestReason,
                    xWinesMatch = xMatch,
                    fullEntryText = entry.combinedText,
                    priceText = entry.priceText,
                    displayName = bestDisplayName,
                    matchSource = matchSource
                ))
            }
        }

        return scored
            .sortedWith(
                compareByDescending<ScoredWine> { it.score }
                    .thenByDescending { it.xWinesMatch?.averageRating ?: 0f }
                    .thenBy { (it.displayName ?: it.originalText).lowercase() }
            )
            .distinctBy { it.originalText.lowercase().replace(Regex("[^a-z]"), "") }
    }

    /**
     * Build a WineRecommendation from the top scored results.
     */
    fun buildRecommendation(
        scoredWines: List<ScoredWine>,
        food: FoodCategory,
        fullText: String
    ): WineRecommendation {
        if (scoredWines.isEmpty()) {
            return WineRecommendation(
                wineName = "No match found",
                price = null,
                reasoning = "Could not identify any wines from the list that match known varieties. " +
                    "Try taking a clearer photo of the wine list.",
                runnerUp = null,
                rawResponse = fullText
            )
        }

        val top = scoredWines[0]
        val price = extractPrice(top.priceText ?: top.fullEntryText ?: top.originalText)

        // Build alternatives from positions 1..min(4, size-1)
        val alts = scoredWines.drop(1).take(3).map { scored ->
            WineAlternative(
                wineName = scored.displayName ?: scored.originalText,
                price = extractPrice(scored.priceText ?: scored.fullEntryText ?: scored.originalText),
                score = scored.score,
                reason = scored.reason,
                xWinesMatch = scored.xWinesMatch,
                vintageMatch = scored.vintageMatch,
                vintageNote = buildVintageNote(scored)
            )
        }

        val runnerUp = alts.firstOrNull()?.wineName

        return WineRecommendation(
            wineName = top.displayName ?: top.originalText,
            price = price,
            reasoning = "${top.reason}. Scored ${top.score}/10 as a pairing with ${food.displayName}.",
            runnerUp = runnerUp,
            rawResponse = fullText,
            xWinesMatch = top.xWinesMatch,
            vintageMatch = top.vintageMatch,
            vintageNote = buildVintageNote(top),
            alternatives = alts
        )
    }

    private fun buildVintageNote(scored: ScoredWine): String? {
        return when (scored.vintageMatch) {
            VintageMatch.CLOSEST -> {
                val ocrYear = scored.ocrYear
                val closest = scored.xWinesMatch?.vintages?.let { vintages ->
                    ocrYear?.let { XWinesDatabase.findClosestVintage(vintages, it) }
                }
                if (ocrYear != null && closest != null) {
                    "$ocrYear not found in database, showing data for $closest vintage"
                } else {
                    "Vintage not found in database"
                }
            }
            VintageMatch.NOT_IN_DATABASE -> "No vintage data available for this wine"
            else -> null
        }
    }

    /**
     * Extract the best display name for a keyword-matched wine entry.
     * Prefers lines that contain a wine keyword along with a vintage or
     * producer name, then falls back to the first line with a vintage,
     * then the first non-price line. Strips trailing price info.
     */
    private fun bestWineDisplayName(entry: WineEntry): String? {
        val lines = entry.lines
        if (lines.size <= 1) return null // single-line entry — default is fine

        // First pass: a line containing a wine keyword + vintage (e.g., "Merlot Reserve 2019")
        for (line in lines) {
            val lower = line.lowercase()
            val hasKeyword = wineKeywords.keys.any { lower.contains(it) }
            val hasVintage = VINTAGE_PATTERN.containsMatchIn(line)
            if (hasKeyword && hasVintage) {
                return cleanDisplayName(line)
            }
        }

        // Second pass: first line with a vintage year that isn't just a price
        for (line in lines) {
            if (VINTAGE_PATTERN.containsMatchIn(line)) {
                val isPriceOnly = PRICE_LINE_PATTERN.containsMatchIn(line) && lineHasPrice(line)
                if (!isPriceOnly) {
                    return cleanDisplayName(line)
                }
            }
        }

        // Third pass: first line with a wine keyword that isn't a bare keyword
        for (line in lines) {
            val lower = line.lowercase()
            val hasKeyword = wineKeywords.keys.any { lower.contains(it) }
            if (hasKeyword && line.trim().split(Regex("\\s+")).size > 1) {
                return cleanDisplayName(line)
            }
        }

        return null // use default
    }

    /**
     * Clean a line for display — strip trailing price info and trim.
     */
    private fun cleanDisplayName(line: String): String {
        // Remove trailing price patterns like "$55", "| Bottle $55", "13/41", or bare "130"
        return line.replace(Regex("""\s*\|?\s*(?:Glass|Bottle|btl)\s*[\$€£]?\s*\d+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[\$€£]\s*\d+\.?\d*\s*$"""), "")
            .replace(Regex("""\s+\d{1,4}/\d{1,4}\s*$"""), "") // glass/bottle
            .replace(Regex("""\s+(\d{2,5})\s*$""")) { match ->
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num !in 1900..2099) "" else match.value // keep years
            }
            .trim()
            .ifEmpty { line.trim() }
    }

    /**
     * Given grape names from an X-Wines entry, look them up in the keyword map
     * and return the best (score, reason) pair. Bridges X-Wines grape data to
     * the keyword-based food pairing scores.
     */
    private fun inferScoreFromGrapes(grapes: List<String>, food: FoodCategory): Pair<Int, String>? {
        var bestScore = 0
        var bestReason = ""
        for (grape in grapes) {
            val key = grape.lowercase()
            val profile = wineKeywords[key] ?: continue
            val score = profile.scores[food] ?: 0
            if (score > bestScore) {
                bestScore = score
                bestReason = profile.description
            }
        }
        return if (bestScore > 0) Pair(bestScore, bestReason) else null
    }

    private fun extractPrice(text: String): String? {
        // Currency symbol patterns ($/€/£)
        val currencyRegex = Regex("""\$\s*\d+\.?\d*|\d+\.?\d*\s*€|€\s*\d+\.?\d*|\d+\.?\d*\s*£|£\s*\d+\.?\d*|\d+\.\d{2}""")
        currencyRegex.find(text)?.let { return it.value }

        // Glass/bottle format — "13/41"
        GLASS_BOTTLE_PATTERN.find(text)?.let { return it.value }

        // Bare trailing number (not a year)
        val bareMatch = BARE_TRAILING_NUMBER.find(text)
        if (bareMatch != null) {
            val num = bareMatch.groupValues[1].toIntOrNull()
            if (num != null && num !in 1900..2099) return bareMatch.groupValues[1]
        }

        return null
    }
}
