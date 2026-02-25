package com.wineselector.app.data

/**
 * On-device wine pairing engine. Scores wines against food categories using
 * grape variety and region keyword profiles.
 *
 * Pipeline: OCR text → candidate lines → grape/region keyword detection → scoring.
 */
class WinePairingEngine {

    data class ScoredWine(
        val originalText: String,
        val score: Int,
        val reason: String,
        val priceText: String? = null,
        val displayName: String? = null,
        val ocrHighlightText: String? = null
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
            "Light, elegant red with earthy notes \u2014 extremely versatile with lighter dishes",
            WineType.RED
        ))
        put("malbec", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6),
            "Bold, juicy red with dark fruit \u2014 a classic steak wine",
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
            "Bold, fruit-forward red with spice \u2014 great with grilled meats",
            WineType.RED
        ))
        put("zinfandel", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 5),
            "Jammy, bold red with high fruit \u2014 loves BBQ and spiced dishes",
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
            "Italian red with high acidity \u2014 born for tomato-based dishes",
            WineType.RED
        ))
        put("nebbiolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 6),
            "Powerful, tannic Italian red with roses and tar \u2014 pairs with rich dishes",
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
            "High-acid Italian red \u2014 excellent with tomato sauces and cured meats",
            WineType.RED
        ))
        put("primitivo", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 7),
            "Rich, ripe red similar to Zinfandel \u2014 pairs with hearty, grilled fare",
            WineType.RED
        ))

        // --- WHITE GRAPES ---
        put("chardonnay", WineProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 6,
                FoodCategory.CHEESE to 6),
            "Rich white with buttery notes \u2014 ideal with poultry and creamy sauces",
            WineType.WHITE
        ))
        put("sauvignon blanc", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.CHICKEN to 7,
                FoodCategory.VEGETARIAN to 8, FoodCategory.SUSHI to 7, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 5),
            "Crisp, zesty white with herbal notes \u2014 perfect with seafood and salads",
            WineType.WHITE
        ))
        put("riesling", WineProfile(
            mapOf(FoodCategory.SUSHI to 9, FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.PORK to 7, FoodCategory.VEGETARIAN to 7,
                FoodCategory.DESSERT to 6, FoodCategory.CHEESE to 6),
            "Aromatic white with bright acidity \u2014 versatile, especially with Asian cuisine",
            WineType.WHITE
        ))
        put("pinot grigio", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6,
                FoodCategory.PIZZA to 5),
            "Light, refreshing white \u2014 a safe, easy-drinking choice with lighter fare",
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
            "Intensely aromatic white with lychee and spice \u2014 great with Asian food",
            WineType.WHITE
        ))
        put("gruner veltliner", WineProfile(
            mapOf(FoodCategory.VEGETARIAN to 8, FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.SUSHI to 7, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 6),
            "Crisp Austrian white with white pepper \u2014 excellent with vegetables",
            WineType.WHITE
        ))
        put("albarino", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Bright Spanish white with citrus and salinity \u2014 made for shellfish",
            WineType.WHITE
        ))
        put("muscadet", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.SUSHI to 6,
                FoodCategory.VEGETARIAN to 5),
            "Bone-dry, mineral French white \u2014 the classic oyster wine",
            WineType.WHITE
        ))
        put("chenin blanc", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.PORK to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.SEAFOOD to 6, FoodCategory.CHEESE to 6,
                FoodCategory.DESSERT to 5),
            "Versatile white ranging from dry to sweet \u2014 pairs broadly",
            WineType.WHITE
        ))
        put("semillon", WineProfile(
            mapOf(FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.CHEESE to 6, FoodCategory.DESSERT to 5),
            "Waxy, full white with honey notes",
            WineType.WHITE
        ))

        // --- ROS\u00c9 ---
        put("ros\u00e9", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry ros\u00e9 is extremely versatile \u2014 a great crowd-pleaser",
            WineType.ROSE
        ))
        put("rose", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry ros\u00e9 is extremely versatile \u2014 a great crowd-pleaser",
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
            "Light, fruity sparkling \u2014 refreshing aperitif or light food pairing",
            WineType.WHITE
        ))
        put("cava", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Spanish sparkling with citrus and toast \u2014 great value bubbly",
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
            "Sweet, lightly sparkling wine \u2014 a natural dessert companion",
            WineType.WHITE
        ))
        put("port", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 9, FoodCategory.BEEF to 4),
            "Rich, sweet fortified wine \u2014 classic with chocolate and blue cheese",
            WineType.RED
        ))
        put("sauternes", WineProfile(
            mapOf(FoodCategory.DESSERT to 10, FoodCategory.CHEESE to 8, FoodCategory.FISH to 4),
            "Luscious sweet French wine \u2014 the ultimate dessert pairing",
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
            "Classic Bordeaux blend \u2014 structured, age-worthy, and built for red meat",
            WineType.RED
        ))
        put("burgundy", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 6),
            "Elegant Burgundy \u2014 Pinot Noir or Chardonnay depending on color"
        ))
        put("bourgogne", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7),
            "Elegant Burgundy \u2014 Pinot Noir or Chardonnay depending on color"
        ))
        put("chianti", WineProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 5),
            "Tuscan Sangiovese \u2014 the definitive Italian food wine",
            WineType.RED
        ))
        put("barolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 5),
            "King of Italian wines \u2014 powerful Nebbiolo with truffle and tar",
            WineType.RED
        ))
        put("barbaresco", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 7, FoodCategory.PORK to 6),
            "Elegant Nebbiolo \u2014 slightly lighter than Barolo, equally food-friendly",
            WineType.RED
        ))
        put("rioja", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 5),
            "Spanish Tempranillo \u2014 oaky, savory, built for grilled meats",
            WineType.RED
        ))
        put("cotes du rhone", WineProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5),
            "Southern Rh\u00f4ne blend \u2014 fruity, spicy, great value",
            WineType.RED
        ))
        put("chateauneuf", WineProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Complex Rh\u00f4ne blend \u2014 rich and powerful with herbal garrigue notes",
            WineType.RED
        ))
        put("sancerre", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 8, FoodCategory.CHEESE to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6),
            "Loire Sauvignon Blanc \u2014 crisp and mineral with goat cheese affinity",
            WineType.WHITE
        ))
        put("chablis", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Unoaked Burgundy Chardonnay \u2014 steely, mineral, built for shellfish",
            WineType.WHITE
        ))
        put("pouilly", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 8, FoodCategory.CHICKEN to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Loire white \u2014 crisp, elegant, great with lighter fare",
            WineType.WHITE
        ))
        put("valpolicella", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 7, FoodCategory.BEEF to 6,
                FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6),
            "Light Italian red \u2014 fresh cherry fruit, great with everyday Italian food",
            WineType.RED
        ))
        put("amarone", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.CHEESE to 8,
                FoodCategory.PASTA to 6),
            "Rich, dried-grape Italian red \u2014 intense and powerful, pairs with bold dishes",
            WineType.RED
        ))
        put("beaujolais", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.PORK to 7, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.FISH to 5,
                FoodCategory.VEGETARIAN to 6),
            "Light, fruity Gamay \u2014 serve slightly chilled with lighter dishes",
            WineType.RED
        ))
        put("montepulciano", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.BEEF to 7,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6),
            "Full-bodied Italian red \u2014 dark fruit and soft tannins, great with red sauce",
            WineType.RED
        ))
    }

    companion object {
        private val PRICE_PATTERN = Regex("""\$\s*\d|\d+\s*€|€\s*\d|\d+\s*£|£\s*\d|\d+\.\d{2}""")

        // Glass/bottle price format — "13/41", "28/104"
        private val GLASS_BOTTLE_PATTERN = Regex("""\b\d{1,4}/\d{1,4}\b""")

        // Bare trailing number — a 2-5 digit number at the end of a line
        private val BARE_TRAILING_NUMBER = Regex("""(?:^|\s)(\d{2,5})\s*$""")

        // Section headers that should not be treated as wine names
        private val SECTION_HEADERS = setOf(
            "red wines", "white wines", "rosé wines", "rose wines",
            "sparkling wines", "dessert wines", "wines by the glass",
            "wines by the bottle", "house wines", "wine list",
            "reds", "whites", "sparkling", "champagnes", "champagne",
            "red", "white", "rosé", "rose", "dessert", "port wines",
            "sweet wines", "fortified wines", "by the glass", "by the bottle",
            "wine", "wines", "our wines", "the wines",
            "prosecco & champagne", "dessert wine & port",
            "interesting reds", "california grill"
        )

        // Color modifiers that override a keyword's default wine type
        private val WHITE_MODIFIERS = Regex(
            """\b(?:blanc[oa]?|white|bianco|weiss|weisswein)\b""", RegexOption.IGNORE_CASE
        )
        private val ROSE_MODIFIERS = Regex(
            """\b(?:ros[eé]|rosato|rosado)\b""", RegexOption.IGNORE_CASE
        )

        // Patterns that indicate a section header, not a wine
        private val HEADER_SUFFIXES = Regex(
            """\b(?:and\s+blends?|cont\.?|continued)\s*$""", RegexOption.IGNORE_CASE
        )

        /** True if this line looks like a menu section header, not a wine name. */
        fun isSectionHeader(text: String): Boolean {
            val lower = text.lowercase().trim()
            if (lower in SECTION_HEADERS) return true
            // All-caps short lines are likely headers (e.g., "RED WINES", "SPARKLING")
            val stripped = lower.replace(Regex("[^a-z\\s&]"), "").trim()
            if (text.length <= 30 && text == text.uppercase() && text.any { it.isLetter() }) {
                if (stripped in SECTION_HEADERS) return true
            }
            // "Merlot and Blends", "Pinot Noir cont."
            if (HEADER_SUFFIXES.containsMatchIn(lower)) return true
            return false
        }

        /**
         * True if this line looks like a tasting note or description, not a wine name.
         * Description lines typically start with lowercase or common description patterns.
         */
        fun isDescriptionLine(text: String): Boolean {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return true
            // Lines starting with lowercase are almost always descriptions
            if (trimmed[0].isLowerCase()) return true
            // Common description starters
            val lower = trimmed.lowercase()
            val descStarters = listOf(
                "a ", "an ", "the ", "this ", "our ", "from ", "grown ",
                "ripe ", "rich ", "light ", "full ", "dry ", "sweet ",
                "smooth ", "crisp ", "fresh ", "classic ", "elegant ",
                "medium ", "bone ", "simple ", "award ", "just ",
                "aromatic ", "intense ", "off-dry", "fruity ",
                "dark ", "pole ", "spanish ", "when only",
                "mouth", "warm ", "ruby ", "cherry ", "expressive ",
                "new world", "attractive ", "easy ", "bright ",
                "zesty ", "very ", "lively ", "with "
            )
            if (descStarters.any { lower.startsWith(it) }) return true
            // Very long lines are likely descriptions (wine names are rarely > 70 chars)
            if (trimmed.length > 80) return true
            // Lines with 4+ adjective-like words are likely descriptions, not wine names
            val descWords = setOf(
                "crispy", "fruity", "mouth-watering", "refreshing", "delicious",
                "balanced", "rounded", "bodied", "flavoured", "flavored",
                "generoux", "generous", "velvety", "silky", "concentrated",
                "oaked", "unoaked", "complex", "subtle", "powerful"
            )
            val wordCount = lower.split(Regex("\\s+")).count { it in descWords }
            if (wordCount >= 2) return true
            return false
        }

        fun hasBareTrailingPrice(line: String): Boolean {
            val match = BARE_TRAILING_NUMBER.find(line) ?: return false
            val num = match.groupValues[1].toIntOrNull() ?: return false
            return num !in 1900..2099
        }

        fun lineHasPrice(line: String): Boolean {
            if (PRICE_PATTERN.containsMatchIn(line)) return true
            if (GLASS_BOTTLE_PATTERN.containsMatchIn(line)) return true
            if (hasBareTrailingPrice(line)) return true
            return false
        }
    }

    /**
     * Score a list of OCR candidate wine names against the selected food category.
     *
     * For each candidate:
     *   1. Clean via cleanNameForMatching()
     *   2. Detect grape/region keywords
     *   3. Score using keyword profiles
     *   4. Find price by matching wine name back to OCR text lines
     *
     * Each wine with a recognized keyword gets its own entry (no deduplication).
     */
    fun recommendWines(
        wineNames: List<String>,
        food: FoodCategory,
        preferences: WinePreferences = WinePreferences(),
        ocrLines: List<String> = emptyList()
    ): List<ScoredWine> {
        val scored = mutableListOf<ScoredWine>()
        val seenDisplayNames = mutableSetOf<String>()

        for (wineName in wineNames) {
            if (isSectionHeader(wineName)) continue
            if (isDescriptionLine(wineName)) continue

            val cleanedName = cleanNameForMatching(wineName)
            if (cleanedName.isBlank()) continue
            // Skip very short names — likely fragments or headers
            if (cleanedName.length < 5) continue

            val keywordMatch = findKeywordMatch(cleanedName, food) ?: continue

            // Find price from OCR lines
            val priceText = findPriceForWine(wineName, ocrLines)
            if (!preferences.acceptsPrice(priceText)) continue

            // Preference filtering by wine type — color modifiers override keyword default
            val actualType = detectActualType(cleanedName, keywordMatch.type)
            if (actualType != null && !preferences.acceptsType(actualType.label)) continue

            // Build a clean display name from the OCR text
            val displayName = cleanDisplayName(cleanedName, keywordMatch.keyword)

            // Skip bare keyword-only matches where the cleaned input name is also just
            // the keyword (e.g., "Rioja, Spain" → cleaned "Rioja" → display "Rioja").
            // But keep entries where the original OCR text has additional wine info
            // (e.g., "Malbec 2021, Cavalo Preto" → display "Malbec" but OCR has producer).
            val displayNorm = TextNormalizer.normalizeForMatching(displayName)
            val isJustKeyword = wineKeywords.keys.any { displayNorm == it }
            if (isJustKeyword) {
                // Check if the original cleaned name has content beyond the keyword
                val cleanedNorm = TextNormalizer.normalizeForMatching(cleanedName)
                val keyNorm = keywordMatch.keyword
                val withoutKeyword = cleanedNorm.replace(keyNorm, "").trim()
                    .replace(Regex("[,\\-.:;'\"\\s]+"), "")
                // If nothing meaningful remains, skip this match
                if (withoutKeyword.length < 3) continue
            }

            if (!seenDisplayNames.add(displayName.lowercase())) continue

            scored.add(ScoredWine(
                originalText = wineName,
                score = keywordMatch.score,
                reason = keywordMatch.reason,
                priceText = priceText,
                displayName = displayName,
                ocrHighlightText = wineName
            ))
        }

        return scored.sortedWith(
            compareByDescending<ScoredWine> { it.score }
                .thenBy { (it.displayName ?: it.originalText).lowercase() }
        )
    }

    /**
     * Find the price for a wine by scanning OCR lines for ones that contain
     * key words from the wine name AND have a price.
     */
    private fun findPriceForWine(wineName: String, ocrLines: List<String>): String? {
        if (ocrLines.isEmpty()) return null

        val nameWords = wineName.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()

        if (nameWords.isEmpty()) return null

        for (line in ocrLines) {
            if (!lineHasPrice(line)) continue
            val lineLower = line.lowercase()
            val matchCount = nameWords.count { lineLower.contains(it) }
            if (matchCount >= 2 || (nameWords.size == 1 && matchCount == 1)) {
                return extractPrice(line)
            }
        }

        return null
    }

    private data class KeywordMatchResult(
        val keyword: String,
        val score: Int,
        val reason: String,
        val type: WineType?
    )

    /**
     * Detect the actual wine type by checking for color modifiers in the text.
     * "Rioja Blanco" → WHITE even though "rioja" keyword defaults to RED.
     */
    private fun detectActualType(text: String, defaultType: WineType?): WineType? {
        val lower = text.lowercase()
        if (WHITE_MODIFIERS.containsMatchIn(lower)) return WineType.WHITE
        if (ROSE_MODIFIERS.containsMatchIn(lower)) return WineType.ROSE
        return defaultType
    }

    /**
     * Find the best grape/region keyword match in a wine name.
     * Uses word-boundary matching to avoid false positives like "port" in "Piesporter".
     */
    private fun findKeywordMatch(cleanedName: String, food: FoodCategory): KeywordMatchResult? {
        val nameLower = TextNormalizer.normalizeForMatching(cleanedName)
        var bestKeyword: String? = null
        var bestScore = 0
        var bestReason = ""
        var bestType: WineType? = null

        for ((keyword, profile) in wineKeywords) {
            // Use word-boundary regex to avoid partial matches (e.g., "port" in "Piesporter")
            val pattern = Regex("""(?:^|[\s,\-.(])${Regex.escape(keyword)}(?:[\s,\-.):]|$)""")
            if (pattern.containsMatchIn(nameLower)) {
                val score = profile.scores[food] ?: 0
                // Prefer longer keyword matches (more specific)
                if (score > bestScore || (score == bestScore && keyword.length > (bestKeyword?.length ?: 0))) {
                    bestScore = score
                    bestKeyword = keyword
                    bestReason = profile.description
                    bestType = profile.type
                }
            }
        }

        if (bestKeyword == null || bestScore == 0) return null
        return KeywordMatchResult(bestKeyword, bestScore, bestReason, bestType)
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
                reasoning = "Could not identify any wines from the list. " +
                    "Try taking a clearer photo of the wine list.",
                runnerUp = null,
                rawResponse = fullText
            )
        }

        val top = scoredWines[0]
        val price = extractPrice(top.priceText ?: top.originalText)

        val alts = scoredWines.drop(1).take(3).map { scored ->
            WineAlternative(
                wineName = scored.displayName ?: scored.originalText,
                price = extractPrice(scored.priceText ?: scored.originalText),
                score = scored.score,
                reason = scored.reason
            )
        }

        val runnerUp = alts.firstOrNull()?.wineName

        return WineRecommendation(
            wineName = top.displayName ?: top.originalText,
            price = price,
            reasoning = "${top.reason}. Scored ${top.score}/10 as a pairing with ${food.displayName}.",
            runnerUp = runnerUp,
            rawResponse = fullText,
            alternatives = alts
        )
    }

    /**
     * Clean a wine name for matching — strips menu numbering, inline country
     * names, and embedded prices that pollute the display name from OCR.
     */
    fun cleanNameForMatching(name: String): String {
        return name
            // Strip leading menu numbering: "12.", "12)", "12 -", "#12"
            .replace(Regex("""^[#]?\d{1,3}[.):\-]?\s*"""), "")
            // Strip inline country names (case-insensitive)
            .replace(Regex("""\b(?:FRANCE|ITALY|SPAIN|ARGENTINA|CHILE|AUSTRALIA|PORTUGAL|GERMANY|AUSTRIA|SOUTH\s+AFRICA|NEW\s+ZEALAND|UNITED\s+STATES|USA|BRAZIL|URUGUAY|GREECE|HUNGARY|LEBANON|CROATIA|SLOVENIA|GEORGIA)\b""", RegexOption.IGNORE_CASE), " ")
            // Strip decimal prices (10.50, 30.00, 6.90)
            .replace(Regex("""\b\d{1,5}\.\d{2}\b"""), " ")
            // Strip currency-prefixed prices ($55, €42, £38)
            .replace(Regex("""[\$€£]\s*\d+\.?\d*"""), " ")
            // Strip glass/bottle format (13/41)
            .replace(Regex("""\b\d{1,4}/\d{1,4}\b"""), " ")
            // Strip bare numbers that look like prices (not years)
            .replace(Regex("""\b(\d{2,5})\b""")) { match ->
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num !in 1900..2099) " " else match.value
            }
            // Collapse whitespace and trim
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifEmpty { name.trim() }
    }

    /**
     * Clean a wine name for display — produces a short, human-readable name.
     * Strips descriptions, volume measurements, regions, quotes, and other noise,
     * keeping only the producer name + grape/region keyword.
     *
     * Strategy: find the matched keyword in the cleaned text, then keep
     * everything from the start of the line up to and including the keyword
     * (plus one following word for qualifiers like "Riserva", "Reserva", "Grand Cru").
     * This drops trailing descriptions, regions, and tasting notes.
     */
    fun cleanDisplayName(cleanedName: String, matchedKeyword: String): String {
        var s = cleanedName
            // Strip volume measurements (125ml, 175ml, 250ml, 750ml, 187ml, etc.)
            .replace(Regex("""\b\d{2,4}\s*ml\b""", RegexOption.IGNORE_CASE), " ")
            // Strip "(½ bottle)" or "(half bottle)" style text
            .replace(Regex("""\(.*?\)"""), " ")
            // Unquote vineyard/producer names: "Sebella" → Sebella, 'Frunza' → Frunza
            .replace(Regex("""["'"'"]\s*([^"'"'"]*?)\s*["'"'"]"""), " $1 ")
            // Strip "Glass", "Bottle" as standalone words
            .replace(Regex("""\b(?:glass|bottle|cont)\b""", RegexOption.IGNORE_CASE), " ")
            // Strip abbreviated vintages: '17, '13, '08
            .replace(Regex("""'\d{2}\b"""), " ")
            // Strip region qualifiers and dietary markers (DOCG, DOC, IGT, AOC, VG, etc.)
            .replace(Regex("""\b(?:DOCG|DOC|IGT|AOC|AOP|VDP|DO|AVA|VG|VE)\b""", RegexOption.IGNORE_CASE), " ")
            // Collapse whitespace
            .replace(Regex("\\s+"), " ")
            .trim()

        // Find the keyword position using normalized text (handles accents like Châteauneuf → chateauneuf)
        val keyLower = matchedKeyword.lowercase()
        val sNorm = TextNormalizer.normalizeForMatching(s)
        val keyIdx = sNorm.indexOf(keyLower)
        if (keyIdx >= 0) {
            val afterKeyword = keyIdx + matchedKeyword.length
            // Check for trailing qualifier words (Riserva, Reserva, du Pape, Grand Cru, etc.)
            // Also consume a leading hyphen so "Chateauneuf-du-Pape" keeps "du Pape"
            val rest = if (afterKeyword < s.length) s.substring(afterKeyword) else ""
            val trailingWords = Regex("""^[\s,\-]*\s*((?:[\w\u00C0-\u024F]+[\s\-]*){0,2})""").find(rest)
                ?.groupValues?.get(1)?.trim() ?: ""
            val trailingLower = trailingWords.lowercase().replace("-", " ")
            val qualifierPatterns = setOf(
                "riserva", "reserva", "reserve", "réserve",
                "superiore", "classico", "brut", "ripasso",
                "vecchio", "nobile", "rosé", "rose",
                "du pape", "grand cru", "gran reserva",
                "blanco", "blanc", "rouge", "nero", "bianco",
                "crianza", "joven"
            )
            // Check if trailing words start with a known qualifier
            val matchedQualifier = qualifierPatterns.find { trailingLower.startsWith(it) }

            if (keyIdx == 0) {
                // Keyword is at the start: "Malbec, Cavalo Preto" — keep keyword + producer
                // Take the first 1-2 words after the keyword (the producer name)
                val afterWithQual = if (matchedQualifier != null) {
                    val qualLen = matchedQualifier.length
                    val actualQual = trailingWords.substring(0, minOf(qualLen, trailingWords.length))
                        .replace("-", " ")
                    actualQual
                } else null

                // Extract producer: the text after the keyword, skipping vintages, prices, noise
                val producerPart = rest.replace(Regex("""^[\s,\-]+"""), "")
                val producer = producerPart.split(Regex("[,\\s]+"))
                    .filter { it.isNotBlank() }
                    .filter { !it.matches(Regex("""\d{4}""")) }  // Drop vintage years
                    .filter { !it.matches(Regex("""[\d£€$.,/]+""")) }  // Drop prices/currency
                    .takeWhile { it.length >= 2 }  // Stop at single-char fragments
                    .take(2)
                    .joinToString(" ")

                s = if (afterWithQual != null) {
                    s.substring(0, afterKeyword) + " " + afterWithQual
                } else if (producer.isNotBlank() && producer.length >= 3) {
                    // "Malbec" + "Cavalo Preto" → "Malbec, Cavalo Preto"
                    s.substring(0, afterKeyword) + ", " + producer
                } else {
                    s.substring(0, afterKeyword)
                }
            } else {
                // Keyword is in the middle/end: "Domaine Tinel Châteauneuf du Pape"
                s = if (matchedQualifier != null) {
                    val qualLen = matchedQualifier.length
                    val actualQual = trailingWords.substring(0, minOf(qualLen, trailingWords.length))
                        .replace("-", " ")
                    s.substring(0, afterKeyword) + " " + actualQual
                } else {
                    s.substring(0, afterKeyword)
                }
            }
        }

        // Normalize: "SkyWalker,Pinot" → "SkyWalker, Pinot"
        s = s.replace(Regex(""",(?!\s)"""), ", ")
        // Collapse multiple spaces (e.g., after stripping quotes: "Zenato , " → "Zenato,")
        s = s.replace(Regex("""\s+,"""), ",")
        s = s.replace(Regex("\\s+"), " ").trim()
        // Final cleanup: strip trailing commas, dashes, periods
        s = s.replace(Regex("""[,\-.\s]+$"""), "").trim()
        // Strip leading commas/dashes
        s = s.replace(Regex("""^[,\-.\s]+"""), "").trim()

        return s.ifEmpty { cleanedName }
    }

    fun extractPrice(text: String): String? {
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
