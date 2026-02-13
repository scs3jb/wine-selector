package com.wineselector.app.data

/**
 * On-device wine pairing engine. Uses keyword matching against a knowledge base
 * of grape varieties, wine regions, and styles to score wines against food categories.
 */
class WinePairingEngine {

    data class ScoredWine(
        val originalText: String,
        val score: Int,
        val reason: String
    )

    /**
     * Each entry maps a keyword (grape, region, or style) to a map of FoodCategory -> score (1-10)
     * plus a short description for pairing reasoning.
     */
    private data class WineProfile(
        val scores: Map<FoodCategory, Int>,
        val description: String
    )

    private val wineKeywords: Map<String, WineProfile> = buildMap {
        // --- RED GRAPES ---
        put("cabernet sauvignon", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4,
                FoodCategory.VEGETARIAN to 3, FoodCategory.PIZZA to 6),
            "Full-bodied red with firm tannins that cut through rich, fatty meats"
        ))
        put("cabernet", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4),
            "Full-bodied red with firm tannins that cut through rich, fatty meats"
        ))
        put("merlot", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 7, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 7, FoodCategory.VEGETARIAN to 5),
            "Medium-bodied, smooth red that pairs broadly with meats and pasta"
        ))
        put("pinot noir", WineProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.PORK to 8, FoodCategory.LAMB to 7,
                FoodCategory.FISH to 6, FoodCategory.PASTA to 7, FoodCategory.BEEF to 5,
                FoodCategory.CHEESE to 7, FoodCategory.SUSHI to 5, FoodCategory.VEGETARIAN to 7,
                FoodCategory.PIZZA to 6),
            "Light, elegant red with earthy notes — extremely versatile with lighter dishes"
        ))
        put("malbec", WineProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6),
            "Bold, juicy red with dark fruit — a classic steak wine"
        ))
        put("syrah", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 5),
            "Spicy, peppery red that stands up to bold, gamey flavors"
        ))
        put("shiraz", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 6),
            "Bold, fruit-forward red with spice — great with grilled meats"
        ))
        put("zinfandel", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 5),
            "Jammy, bold red with high fruit — loves BBQ and spiced dishes"
        ))
        put("tempranillo", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 5),
            "Medium-bodied Spanish red with savory leather and cherry notes"
        ))
        put("sangiovese", WineProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6,
                FoodCategory.CHEESE to 7, FoodCategory.VEGETARIAN to 6),
            "Italian red with high acidity — born for tomato-based dishes"
        ))
        put("nebbiolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 6),
            "Powerful, tannic Italian red with roses and tar — pairs with rich dishes"
        ))
        put("grenache", WineProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 6, FoodCategory.VEGETARIAN to 6),
            "Fruity, spicy red that works with a wide range of Mediterranean dishes"
        ))
        put("barbera", WineProfile(
            mapOf(FoodCategory.PASTA to 9, FoodCategory.PIZZA to 8, FoodCategory.PORK to 7,
                FoodCategory.BEEF to 6, FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "High-acid Italian red — excellent with tomato sauces and cured meats"
        ))
        put("primitivo", WineProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 7),
            "Rich, ripe red similar to Zinfandel — pairs with hearty, grilled fare"
        ))

        // --- WHITE GRAPES ---
        put("chardonnay", WineProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 6,
                FoodCategory.CHEESE to 6),
            "Rich white with buttery notes — ideal with poultry and creamy sauces"
        ))
        put("sauvignon blanc", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.CHICKEN to 7,
                FoodCategory.VEGETARIAN to 8, FoodCategory.SUSHI to 7, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 5),
            "Crisp, zesty white with herbal notes — perfect with seafood and salads"
        ))
        put("riesling", WineProfile(
            mapOf(FoodCategory.SUSHI to 9, FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.PORK to 7, FoodCategory.VEGETARIAN to 7,
                FoodCategory.DESSERT to 6, FoodCategory.CHEESE to 6),
            "Aromatic white with bright acidity — versatile, especially with Asian cuisine"
        ))
        put("pinot grigio", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6,
                FoodCategory.PIZZA to 5),
            "Light, refreshing white — a safe, easy-drinking choice with lighter fare"
        ))
        put("pinot gris", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.PORK to 6),
            "Fuller-bodied style of Pinot Grigio with stone fruit notes"
        ))
        put("viognier", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Aromatic, full white with peach and floral notes"
        ))
        put("gewurztraminer", WineProfile(
            mapOf(FoodCategory.SUSHI to 8, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 6),
            "Intensely aromatic white with lychee and spice — great with Asian food"
        ))
        put("gruner veltliner", WineProfile(
            mapOf(FoodCategory.VEGETARIAN to 8, FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.SUSHI to 7, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 6),
            "Crisp Austrian white with white pepper — excellent with vegetables"
        ))
        put("albarino", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Bright Spanish white with citrus and salinity — made for shellfish"
        ))
        put("muscadet", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.SUSHI to 6,
                FoodCategory.VEGETARIAN to 5),
            "Bone-dry, mineral French white — the classic oyster wine"
        ))
        put("chenin blanc", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.PORK to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.SEAFOOD to 6, FoodCategory.CHEESE to 6,
                FoodCategory.DESSERT to 5),
            "Versatile white ranging from dry to sweet — pairs broadly"
        ))
        put("semillon", WineProfile(
            mapOf(FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.CHEESE to 6, FoodCategory.DESSERT to 5),
            "Waxy, full white with honey notes"
        ))

        // --- ROSÉ ---
        put("rosé", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry rosé is extremely versatile — a great crowd-pleaser"
        ))
        put("rose", WineProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry rosé is extremely versatile — a great crowd-pleaser"
        ))

        // --- SPARKLING ---
        put("champagne", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 5),
            "Sparkling wine with high acidity and bubbles that cleanse the palate"
        ))
        put("prosecco", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 7, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5, FoodCategory.DESSERT to 5),
            "Light, fruity sparkling — refreshing aperitif or light food pairing"
        ))
        put("cava", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Spanish sparkling with citrus and toast — great value bubbly"
        ))
        put("sparkling", WineProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Bubbles and acidity make sparkling wine a versatile food partner"
        ))

        // --- DESSERT WINES ---
        put("moscato", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 6, FoodCategory.SUSHI to 4),
            "Sweet, lightly sparkling wine — a natural dessert companion"
        ))
        put("port", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 9, FoodCategory.BEEF to 4),
            "Rich, sweet fortified wine — classic with chocolate and blue cheese"
        ))
        put("sauternes", WineProfile(
            mapOf(FoodCategory.DESSERT to 10, FoodCategory.CHEESE to 8, FoodCategory.FISH to 4),
            "Luscious sweet French wine — the ultimate dessert pairing"
        ))
        put("ice wine", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes"
        ))
        put("icewine", WineProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes"
        ))

        // --- REGIONAL / BLENDS ---
        put("bordeaux", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.CHEESE to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 5),
            "Classic Bordeaux blend — structured, age-worthy, and built for red meat"
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
            "Tuscan Sangiovese — the definitive Italian food wine"
        ))
        put("barolo", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 5),
            "King of Italian wines — powerful Nebbiolo with truffle and tar"
        ))
        put("barbaresco", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 7, FoodCategory.PORK to 6),
            "Elegant Nebbiolo — slightly lighter than Barolo, equally food-friendly"
        ))
        put("rioja", WineProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 5),
            "Spanish Tempranillo — oaky, savory, built for grilled meats"
        ))
        put("cotes du rhone", WineProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5),
            "Southern Rhône blend — fruity, spicy, great value"
        ))
        put("chateauneuf", WineProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Complex Rhône blend — rich and powerful with herbal garrigue notes"
        ))
        put("sancerre", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 8, FoodCategory.CHEESE to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6),
            "Loire Sauvignon Blanc — crisp and mineral with goat cheese affinity"
        ))
        put("chablis", WineProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Unoaked Burgundy Chardonnay — steely, mineral, built for shellfish"
        ))
        put("pouilly", WineProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 8, FoodCategory.CHICKEN to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Loire white — crisp, elegant, great with lighter fare"
        ))
        put("valpolicella", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 7, FoodCategory.BEEF to 6,
                FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6),
            "Light Italian red — fresh cherry fruit, great with everyday Italian food"
        ))
        put("amarone", WineProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.CHEESE to 8,
                FoodCategory.PASTA to 6),
            "Rich, dried-grape Italian red — intense and powerful, pairs with bold dishes"
        ))
        put("beaujolais", WineProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.PORK to 7, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.FISH to 5,
                FoodCategory.VEGETARIAN to 6),
            "Light, fruity Gamay — serve slightly chilled with lighter dishes"
        ))
        put("montepulciano", WineProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.BEEF to 7,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6),
            "Full-bodied Italian red — dark fruit and soft tannins, great with red sauce"
        ))
    }

    /**
     * Analyze extracted wine list text and return scored recommendations for the given food.
     */
    fun recommendWines(extractedText: String, food: FoodCategory): List<ScoredWine> {
        val lines = extractedText.lines()
            .map { it.trim() }
            .filter { it.length > 2 }

        val scored = mutableListOf<ScoredWine>()

        for (line in lines) {
            val lower = line.lowercase()
            var bestScore = 0
            var bestReason = ""
            var matchedKeyword = ""

            for ((keyword, profile) in wineKeywords) {
                if (lower.contains(keyword)) {
                    val score = profile.scores[food] ?: 0
                    if (score > bestScore) {
                        bestScore = score
                        bestReason = profile.description
                        matchedKeyword = keyword
                    }
                }
            }

            if (bestScore > 0) {
                scored.add(ScoredWine(
                    originalText = line,
                    score = bestScore,
                    reason = bestReason
                ))
            }
        }

        // Deduplicate: if two lines matched the same keyword, keep the one with more text (likely has the price)
        return scored
            .sortedByDescending { it.score }
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
        val price = extractPrice(top.originalText)
        val runnerUp = if (scoredWines.size > 1) scoredWines[1].originalText else null

        return WineRecommendation(
            wineName = top.originalText,
            price = price,
            reasoning = "${top.reason}. Scored ${top.score}/10 as a pairing with ${food.displayName}.",
            runnerUp = runnerUp,
            rawResponse = fullText
        )
    }

    private fun extractPrice(text: String): String? {
        val priceRegex = Regex("""\$\s*\d+\.?\d*|\d+\.?\d*\s*€|€\s*\d+\.?\d*|\d+\.?\d*\s*£|£\s*\d+\.?\d*|\d+\.\d{2}""")
        return priceRegex.find(text)?.value
    }
}
