package com.wineselector.core.model

import com.wineselector.core.text.TextNormalizer

/** Broad wine style. Drives colours in the UI, filters and fallback pairing scores. */
enum class WineStyle(val label: String) {
    RED("Red"),
    WHITE("White"),
    ROSE("Rosé"),
    SPARKLING("Sparkling"),
    DESSERT("Dessert"),
    FORTIFIED("Fortified");

    companion object {
        /** Map an X-Wines "Type" value ("Red", "Dessert/Port", "Sparkling"...) to a style. */
        fun fromDatabaseType(type: String): WineStyle? {
            val t = TextNormalizer.normalizeForMatching(type)
            return when {
                "port" in t || "fortified" in t || "sherry" in t -> FORTIFIED
                "dessert" in t || "sweet" in t -> DESSERT
                "sparkling" in t -> SPARKLING
                "rose" in t -> ROSE
                "white" in t -> WHITE
                "red" in t -> RED
                else -> null
            }
        }

        private val SPARKLING_WORDS = Regex("""\b(?:champagne|cremant|prosecco|cava|franciacorta|spumante|sekt|sparkling|brut|pet[- ]?nat|methode traditionnelle|blanc de blancs|blanc de noirs)\b""")
        private val DESSERT_WORDS = Regex("""\b(?:sauternes|tokaji|ice ?wine|eiswein|late harvest|vendanges tardives|moscato d'asti|passito|beerenauslese|trockenbeerenauslese|recioto|dessert)\b""")
        private val FORTIFIED_WORDS = Regex("""\b(?:port|porto|tawny|ruby port|lbv|sherry|fino|manzanilla|oloroso|amontillado|pedro ximenez|madeira|marsala|fortified)\b""")
        private val ROSE_WORDS = Regex("""\b(?:rose|rosato|rosado|blush)\b""")
        private val WHITE_WORDS = Regex("""\b(?:blanc|blanco|bianco|branco|weiss|white)\b""")
        private val RED_WORDS = Regex("""\b(?:rouge|tinto|rosso|red)\b""")

        /**
         * Detect an explicit style marker in free text. Colour modifiers win over
         * grape defaults: "Rioja Blanco" is white, "Pinot Noir Rosé" is rosé.
         */
        fun detectInText(text: String): WineStyle? {
            val t = TextNormalizer.normalizeForMatching(text)
            return when {
                SPARKLING_WORDS.containsMatchIn(t) -> SPARKLING
                FORTIFIED_WORDS.containsMatchIn(t) -> FORTIFIED
                DESSERT_WORDS.containsMatchIn(t) -> DESSERT
                ROSE_WORDS.containsMatchIn(t) -> ROSE
                // "Sauvignon Blanc", "Chenin Blanc", "Pinot Blanc" name white grapes, which is still white.
                WHITE_WORDS.containsMatchIn(t) && !Regex("""\bblanc de noirs\b""").containsMatchIn(t) -> WHITE
                RED_WORDS.containsMatchIn(t) -> RED
                else -> null
            }
        }
    }
}
