package com.wineselector.core.pairing

import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.text.TextNormalizer

/** Food affinity for a grape, region or style keyword. Scores are 1..10. */
data class GrapeProfile(
    val scores: Map<FoodCategory, Int>,
    val description: String,
    val style: WineStyle? = null
)

/** A keyword profile found in a piece of text. */
data class ProfileMatch(val keyword: String, val profile: GrapeProfile)

/**
 * Knowledge base of 80+ grape varieties, regions and styles with food pairing
 * scores. Used to score wines by their grapes (database matches) or by keywords
 * found in menu / label text (unmatched wines).
 */
object GrapeProfiles {

    val profiles: Map<String, GrapeProfile> = buildMap {
        // --- RED GRAPES ---
        put("cabernet sauvignon", GrapeProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4,
                FoodCategory.VEGETARIAN to 3, FoodCategory.PIZZA to 6),
            "Full-bodied red with firm tannins that cut through rich, fatty meats",
            WineStyle.RED
        ))
        put("cabernet", GrapeProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 9, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.CHICKEN to 4),
            "Full-bodied red with firm tannins that cut through rich, fatty meats",
            WineStyle.RED
        ))
        put("merlot", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 7, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 7, FoodCategory.VEGETARIAN to 5),
            "Medium-bodied, smooth red that pairs broadly with meats and pasta",
            WineStyle.RED
        ))
        put("pinot noir", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.PORK to 8, FoodCategory.LAMB to 7,
                FoodCategory.FISH to 6, FoodCategory.PASTA to 7, FoodCategory.BEEF to 5,
                FoodCategory.CHEESE to 7, FoodCategory.SUSHI to 5, FoodCategory.VEGETARIAN to 7,
                FoodCategory.PIZZA to 6),
            "Light, elegant red with earthy notes \u2014 extremely versatile with lighter dishes",
            WineStyle.RED
        ))
        put("malbec", GrapeProfile(
            mapOf(FoodCategory.BEEF to 10, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6),
            "Bold, juicy red with dark fruit \u2014 a classic steak wine",
            WineStyle.RED
        ))
        put("syrah", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 5),
            "Spicy, peppery red that stands up to bold, gamey flavors",
            WineStyle.RED
        ))
        put("shiraz", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5, FoodCategory.PIZZA to 6),
            "Bold, fruit-forward red with spice \u2014 great with grilled meats",
            WineStyle.RED
        ))
        put("zinfandel", GrapeProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 5),
            "Jammy, bold red with high fruit \u2014 loves BBQ and spiced dishes",
            WineStyle.RED
        ))
        put("tempranillo", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 5),
            "Medium-bodied Spanish red with savory leather and cherry notes",
            WineStyle.RED
        ))
        put("sangiovese", GrapeProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6,
                FoodCategory.CHEESE to 7, FoodCategory.VEGETARIAN to 6),
            "Italian red with high acidity \u2014 born for tomato-based dishes",
            WineStyle.RED
        ))
        put("nebbiolo", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 6),
            "Powerful, tannic Italian red with roses and tar \u2014 pairs with rich dishes",
            WineStyle.RED
        ))
        put("grenache", GrapeProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 6, FoodCategory.CHEESE to 6,
                FoodCategory.PIZZA to 6, FoodCategory.VEGETARIAN to 6),
            "Fruity, spicy red that works with a wide range of Mediterranean dishes",
            WineStyle.RED
        ))
        put("barbera", GrapeProfile(
            mapOf(FoodCategory.PASTA to 9, FoodCategory.PIZZA to 8, FoodCategory.PORK to 7,
                FoodCategory.BEEF to 6, FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "High-acid Italian red \u2014 excellent with tomato sauces and cured meats",
            WineStyle.RED
        ))
        put("primitivo", GrapeProfile(
            mapOf(FoodCategory.BEEF to 7, FoodCategory.PORK to 8, FoodCategory.LAMB to 6,
                FoodCategory.PIZZA to 8, FoodCategory.PASTA to 7),
            "Rich, ripe red similar to Zinfandel \u2014 pairs with hearty, grilled fare",
            WineStyle.RED
        ))

        // --- WHITE GRAPES ---
        put("chardonnay", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 9, FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 6,
                FoodCategory.CHEESE to 6),
            "Rich white with buttery notes \u2014 ideal with poultry and creamy sauces",
            WineStyle.WHITE
        ))
        put("sauvignon blanc", GrapeProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.CHICKEN to 7,
                FoodCategory.VEGETARIAN to 8, FoodCategory.SUSHI to 7, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 5),
            "Crisp, zesty white with herbal notes \u2014 perfect with seafood and salads",
            WineStyle.WHITE
        ))
        put("riesling", GrapeProfile(
            mapOf(FoodCategory.SUSHI to 9, FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.PORK to 7, FoodCategory.VEGETARIAN to 7,
                FoodCategory.DESSERT to 6, FoodCategory.CHEESE to 6),
            "Aromatic white with bright acidity \u2014 versatile, especially with Asian cuisine",
            WineStyle.WHITE
        ))
        put("pinot grigio", GrapeProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6,
                FoodCategory.PIZZA to 5),
            "Light, refreshing white \u2014 a safe, easy-drinking choice with lighter fare",
            WineStyle.WHITE
        ))
        put("pinot gris", GrapeProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.PASTA to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.PORK to 6),
            "Fuller-bodied style of Pinot Grigio with stone fruit notes",
            WineStyle.WHITE
        ))
        put("viognier", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Aromatic, full white with peach and floral notes",
            WineStyle.WHITE
        ))
        put("gewurztraminer", GrapeProfile(
            mapOf(FoodCategory.SUSHI to 8, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 6),
            "Intensely aromatic white with lychee and spice \u2014 great with Asian food",
            WineStyle.WHITE
        ))
        put("gruner veltliner", GrapeProfile(
            mapOf(FoodCategory.VEGETARIAN to 8, FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7,
                FoodCategory.SUSHI to 7, FoodCategory.SEAFOOD to 7, FoodCategory.PORK to 6),
            "Crisp Austrian white with white pepper \u2014 excellent with vegetables",
            WineStyle.WHITE
        ))
        put("albarino", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Bright Spanish white with citrus and salinity \u2014 made for shellfish",
            WineStyle.WHITE
        ))
        put("muscadet", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.SUSHI to 6,
                FoodCategory.VEGETARIAN to 5),
            "Bone-dry, mineral French white \u2014 the classic oyster wine",
            WineStyle.WHITE
        ))
        put("chenin blanc", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.PORK to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.SEAFOOD to 6, FoodCategory.CHEESE to 6,
                FoodCategory.DESSERT to 5),
            "Versatile white ranging from dry to sweet \u2014 pairs broadly",
            WineStyle.WHITE
        ))
        put("semillon", GrapeProfile(
            mapOf(FoodCategory.FISH to 7, FoodCategory.CHICKEN to 7, FoodCategory.SEAFOOD to 6,
                FoodCategory.CHEESE to 6, FoodCategory.DESSERT to 5),
            "Waxy, full white with honey notes",
            WineStyle.WHITE
        ))

        // --- ROS\u00c9 ---
        put("ros\u00e9", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry ros\u00e9 is extremely versatile \u2014 a great crowd-pleaser",
            WineStyle.ROSE
        ))
        put("rose", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 7, FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 6, FoodCategory.PIZZA to 6,
                FoodCategory.SUSHI to 6, FoodCategory.PORK to 6, FoodCategory.CHEESE to 5),
            "Dry ros\u00e9 is extremely versatile \u2014 a great crowd-pleaser",
            WineStyle.ROSE
        ))

        // --- SPARKLING ---
        put("champagne", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 8, FoodCategory.FISH to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.CHEESE to 7, FoodCategory.DESSERT to 6,
                FoodCategory.VEGETARIAN to 7, FoodCategory.PASTA to 5),
            "Sparkling wine with high acidity and bubbles that cleanse the palate",
            WineStyle.SPARKLING
        ))
        put("prosecco", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 7, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5, FoodCategory.DESSERT to 5),
            "Light, fruity sparkling \u2014 refreshing aperitif or light food pairing",
            WineStyle.SPARKLING
        ))
        put("cava", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Spanish sparkling with citrus and toast \u2014 great value bubbly",
            WineStyle.SPARKLING
        ))
        put("sparkling", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Bubbles and acidity make sparkling wine a versatile food partner",
            WineStyle.SPARKLING
        ))

        // --- DESSERT WINES ---
        put("moscato", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 6, FoodCategory.SUSHI to 4),
            "Sweet, lightly sparkling wine \u2014 a natural dessert companion",
            WineStyle.DESSERT
        ))
        put("port", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 9, FoodCategory.BEEF to 4),
            "Rich, sweet fortified wine \u2014 classic with chocolate and blue cheese",
            WineStyle.FORTIFIED
        ))
        put("sauternes", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 10, FoodCategory.CHEESE to 8, FoodCategory.FISH to 4),
            "Luscious sweet French wine \u2014 the ultimate dessert pairing",
            WineStyle.DESSERT
        ))
        put("ice wine", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes",
            WineStyle.DESSERT
        ))
        put("icewine", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 9, FoodCategory.CHEESE to 7),
            "Intensely sweet wine from frozen grapes",
            WineStyle.DESSERT
        ))

        // --- REGIONAL / BLENDS ---
        put("bordeaux", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.CHEESE to 7,
                FoodCategory.PORK to 6, FoodCategory.PASTA to 5),
            "Classic Bordeaux blend \u2014 structured, age-worthy, and built for red meat",
            WineStyle.RED
        ))
        put("burgundy", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7,
                FoodCategory.PASTA to 6),
            "Elegant Burgundy \u2014 Pinot Noir or Chardonnay depending on color"
        ))
        put("bourgogne", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.BEEF to 7, FoodCategory.LAMB to 7,
                FoodCategory.PORK to 7, FoodCategory.FISH to 6, FoodCategory.CHEESE to 7),
            "Elegant Burgundy \u2014 Pinot Noir or Chardonnay depending on color"
        ))
        put("chianti", GrapeProfile(
            mapOf(FoodCategory.PASTA to 10, FoodCategory.PIZZA to 9, FoodCategory.BEEF to 6,
                FoodCategory.LAMB to 6, FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 5),
            "Tuscan Sangiovese \u2014 the definitive Italian food wine",
            WineStyle.RED
        ))
        put("barolo", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 5),
            "King of Italian wines \u2014 powerful Nebbiolo with truffle and tar",
            WineStyle.RED
        ))
        put("barbaresco", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 7, FoodCategory.PORK to 6),
            "Elegant Nebbiolo \u2014 slightly lighter than Barolo, equally food-friendly",
            WineStyle.RED
        ))
        put("rioja", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7, FoodCategory.CHICKEN to 6, FoodCategory.PASTA to 5),
            "Spanish Tempranillo \u2014 oaky, savory, built for grilled meats",
            WineStyle.RED
        ))
        put("cotes du rhone", GrapeProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.BEEF to 7, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6, FoodCategory.PASTA to 5,
                FoodCategory.PIZZA to 5),
            "Southern Rh\u00f4ne blend \u2014 fruity, spicy, great value",
            WineStyle.RED
        ))
        put("chateauneuf", GrapeProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Complex Rh\u00f4ne blend \u2014 rich and powerful with herbal garrigue notes",
            WineStyle.RED
        ))
        put("sancerre", GrapeProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 8, FoodCategory.CHEESE to 8,
                FoodCategory.CHICKEN to 7, FoodCategory.VEGETARIAN to 7, FoodCategory.SUSHI to 6),
            "Loire Sauvignon Blanc \u2014 crisp and mineral with goat cheese affinity",
            WineStyle.WHITE
        ))
        put("chablis", GrapeProfile(
            mapOf(FoodCategory.FISH to 9, FoodCategory.SEAFOOD to 9, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 6),
            "Unoaked Burgundy Chardonnay \u2014 steely, mineral, built for shellfish",
            WineStyle.WHITE
        ))
        put("pouilly", GrapeProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 8, FoodCategory.CHICKEN to 6,
                FoodCategory.VEGETARIAN to 6, FoodCategory.CHEESE to 6),
            "Loire white \u2014 crisp, elegant, great with lighter fare",
            WineStyle.WHITE
        ))
        put("valpolicella", GrapeProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 7, FoodCategory.BEEF to 6,
                FoodCategory.PORK to 6, FoodCategory.CHICKEN to 6),
            "Light Italian red \u2014 fresh cherry fruit, great with everyday Italian food",
            WineStyle.RED
        ))
        put("amarone", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 8, FoodCategory.CHEESE to 8,
                FoodCategory.PASTA to 6),
            "Rich, dried-grape Italian red \u2014 intense and powerful, pairs with bold dishes",
            WineStyle.RED
        ))
        put("beaujolais", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.PORK to 7, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.FISH to 5,
                FoodCategory.VEGETARIAN to 6),
            "Light, fruity Gamay \u2014 serve slightly chilled with lighter dishes",
            WineStyle.RED
        ))
        put("montepulciano", GrapeProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.BEEF to 7,
                FoodCategory.LAMB to 6, FoodCategory.PORK to 6),
            "Full-bodied Italian red \u2014 dark fruit and soft tannins, great with red sauce",
            WineStyle.RED
        ))
        // --- ADDITIONAL VARIETIES & STYLES ---
        put("cabernet franc", GrapeProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.PORK to 8, FoodCategory.BEEF to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.VEGETARIAN to 7, FoodCategory.CHEESE to 7,
                FoodCategory.PIZZA to 6, FoodCategory.PASTA to 6),
            "Medium-bodied red with herbal lift and fine tannins \u2014 flexible with roasts and vegetables",
            WineStyle.RED
        ))
        put("gamay", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.PORK to 7, FoodCategory.PASTA to 6,
                FoodCategory.PIZZA to 6, FoodCategory.CHEESE to 6, FoodCategory.FISH to 5,
                FoodCategory.VEGETARIAN to 6),
            "Light, juicy red with low tannin \u2014 serve slightly chilled",
            WineStyle.RED
        ))
        put("carmenere", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.VEGETARIAN to 6, FoodCategory.PIZZA to 6),
            "Chile's signature red \u2014 dark fruit and a peppery, herbal edge for grilled meats",
            WineStyle.RED
        ))
        put("pinotage", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 7, FoodCategory.PORK to 7,
                FoodCategory.PIZZA to 6),
            "Smoky South African red that loves the barbecue",
            WineStyle.RED
        ))
        put("mourvedre", GrapeProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6),
            "Dense, savoury red with gamey depth \u2014 superb with lamb",
            WineStyle.RED
        ))
        put("monastrell", GrapeProfile(
            mapOf(FoodCategory.LAMB to 9, FoodCategory.BEEF to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 6),
            "Dense, savoury Spanish red \u2014 superb with roast lamb and hard cheese",
            WineStyle.RED
        ))
        put("garnacha", GrapeProfile(
            mapOf(FoodCategory.LAMB to 8, FoodCategory.PORK to 8, FoodCategory.BEEF to 7,
                FoodCategory.PIZZA to 7, FoodCategory.PASTA to 7, FoodCategory.CHICKEN to 6),
            "Ripe, spicy red with soft tannins \u2014 great with roasts and charcuterie",
            WineStyle.RED
        ))
        put("aglianico", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.PASTA to 7,
                FoodCategory.CHEESE to 7),
            "Powerful southern Italian red with firm tannins for rich braises",
            WineStyle.RED
        ))
        put("nero d'avola", GrapeProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.BEEF to 7,
                FoodCategory.LAMB to 7, FoodCategory.PORK to 6),
            "Sicilian red with plush dark fruit \u2014 made for tomato-based dishes",
            WineStyle.RED
        ))
        put("dolcetto", GrapeProfile(
            mapOf(FoodCategory.PASTA to 8, FoodCategory.PIZZA to 8, FoodCategory.PORK to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Soft, everyday Piedmont red \u2014 an easy match for pasta and pizza",
            WineStyle.RED
        ))
        put("touriga nacional", GrapeProfile(
            mapOf(FoodCategory.BEEF to 8, FoodCategory.LAMB to 8, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Portugal's flagship red \u2014 floral, structured and built for grilled meat",
            WineStyle.RED
        ))
        put("brunello", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.PASTA to 8,
                FoodCategory.CHEESE to 8, FoodCategory.PORK to 7),
            "Tuscany's grand Sangiovese \u2014 structured and savoury for steak and aged cheese",
            WineStyle.RED
        ))
        put("ribera del duero", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.PORK to 7,
                FoodCategory.CHEESE to 7),
            "Concentrated Spanish Tempranillo with firm structure \u2014 roast lamb's best friend",
            WineStyle.RED
        ))
        put("priorat", GrapeProfile(
            mapOf(FoodCategory.BEEF to 9, FoodCategory.LAMB to 9, FoodCategory.CHEESE to 7),
            "Intense, mineral Catalan red \u2014 for the richest meat dishes",
            WineStyle.RED
        ))
        put("verdejo", GrapeProfile(
            mapOf(FoodCategory.FISH to 8, FoodCategory.SEAFOOD to 8, FoodCategory.VEGETARIAN to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.SUSHI to 7),
            "Zesty Spanish white with herbal notes \u2014 great with seafood and salads",
            WineStyle.WHITE
        ))
        put("vermentino", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.VEGETARIAN to 7,
                FoodCategory.PASTA to 6, FoodCategory.SUSHI to 7),
            "Saline, citrusy Mediterranean white \u2014 made for grilled fish and shellfish",
            WineStyle.WHITE
        ))
        put("torrontes", GrapeProfile(
            mapOf(FoodCategory.SUSHI to 7, FoodCategory.SEAFOOD to 6, FoodCategory.VEGETARIAN to 7,
                FoodCategory.CHICKEN to 6),
            "Aromatic Argentine white \u2014 lovely with spice and fragrant dishes",
            WineStyle.WHITE
        ))
        put("assyrtiko", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 9, FoodCategory.SUSHI to 7,
                FoodCategory.VEGETARIAN to 6),
            "Bone-dry, mineral Greek white with searing acidity \u2014 perfect for seafood",
            WineStyle.WHITE
        ))
        put("soave", GrapeProfile(
            mapOf(FoodCategory.FISH to 7, FoodCategory.SEAFOOD to 7, FoodCategory.PASTA to 6,
                FoodCategory.VEGETARIAN to 7, FoodCategory.CHICKEN to 6),
            "Gentle, almond-tinged Italian white \u2014 easy with light dishes",
            WineStyle.WHITE
        ))
        put("vinho verde", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8, FoodCategory.SUSHI to 7,
                FoodCategory.VEGETARIAN to 6),
            "Light, spritzy Portuguese white \u2014 refreshing with seafood",
            WineStyle.WHITE
        ))
        put("picpoul", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 9, FoodCategory.FISH to 8, FoodCategory.SUSHI to 7),
            "Crisp, lemony southern French white \u2014 the classic oyster wine",
            WineStyle.WHITE
        ))
        put("marsanne", GrapeProfile(
            mapOf(FoodCategory.CHICKEN to 8, FoodCategory.FISH to 7, FoodCategory.PORK to 6,
                FoodCategory.CHEESE to 6),
            "Rich, textured Rh\u00f4ne white \u2014 good with creamy poultry and fish",
            WineStyle.WHITE
        ))
        put("crémant", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Traditional-method French sparkling \u2014 Champagne style at a friendlier price",
            WineStyle.SPARKLING
        ))
        put("cremant", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 7, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 6, FoodCategory.CHEESE to 6),
            "Traditional-method French sparkling \u2014 Champagne style at a friendlier price",
            WineStyle.SPARKLING
        ))
        put("franciacorta", GrapeProfile(
            mapOf(FoodCategory.SEAFOOD to 8, FoodCategory.FISH to 8, FoodCategory.SUSHI to 7,
                FoodCategory.CHICKEN to 7, FoodCategory.CHEESE to 7),
            "Italy's finest traditional-method sparkling \u2014 creamy and precise",
            WineStyle.SPARKLING
        ))
        put("sherry", GrapeProfile(
            mapOf(FoodCategory.CHEESE to 8, FoodCategory.SEAFOOD to 7, FoodCategory.DESSERT to 6,
                FoodCategory.PORK to 6),
            "Fortified Spanish wine \u2014 dry styles love salty snacks, sweet ones love dessert",
            WineStyle.FORTIFIED
        ))
        put("madeira", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 8, FoodCategory.CHEESE to 8),
            "Tangy, nutty fortified wine \u2014 wonderful with cheese and caramel desserts",
            WineStyle.FORTIFIED
        ))
        put("tokaji", GrapeProfile(
            mapOf(FoodCategory.DESSERT to 10, FoodCategory.CHEESE to 8),
            "Hungary's legendary sweet wine \u2014 honeyed with vibrant acidity",
            WineStyle.DESSERT
        ))
    }

    /** Keywords sorted longest-first so "cabernet sauvignon" wins over "cabernet". */
    private val keywordsBySpecificity = profiles.keys.sortedByDescending { it.length }

    private val keywordPatterns: Map<String, Regex> = profiles.keys.associateWith { keyword ->
        val norm = TextNormalizer.normalizeForMatching(keyword)
        Regex("""(?:^|[^a-z0-9])${Regex.escape(norm)}(?:[^a-z0-9]|$)""")
    }

    /** Profile for an exact grape name (e.g. from the database's grape list). */
    fun forGrape(grape: String): ProfileMatch? {
        val norm = TextNormalizer.normalizeForMatching(grape.lowercase()).trim()
        profiles[norm]?.let { return ProfileMatch(norm, it) }
        for (key in keywordsBySpecificity) {
            if (keywordPatterns.getValue(key).containsMatchIn(norm)) return ProfileMatch(key, profiles.getValue(key))
        }
        return null
    }

    /**
     * All keyword profiles appearing in free text, most specific first. Overlapping
     * shorter keywords are dropped ("cabernet" when "cabernet sauvignon" matched).
     * Uses word boundaries so "port" does not match "Piesporter".
     */
    fun findInText(text: String): List<ProfileMatch> {
        val norm = TextNormalizer.normalizeForMatching(text.lowercase())
        val found = mutableListOf<ProfileMatch>()
        for (key in keywordsBySpecificity) {
            if (!keywordPatterns.getValue(key).containsMatchIn(norm)) continue
            val keyNorm = TextNormalizer.normalizeForMatching(key)
            if (found.any { TextNormalizer.normalizeForMatching(it.keyword).contains(keyNorm) }) continue
            found += ProfileMatch(key, profiles.getValue(key))
        }
        return found
    }
}
