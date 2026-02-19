package com.wineselector.app.data

import android.content.Context
import android.content.SharedPreferences

enum class WineType(val label: String) {
    RED("Red"),
    WHITE("White"),
    ROSE("Rosé");
}

data class WinePreferences(
    val maxPrice: Int = Int.MAX_VALUE,
    val ignoredGrapes: Set<String> = emptySet(),
    val allowedTypes: Set<WineType> = WineType.entries.toSet()
) {
    fun acceptsPrice(priceText: String?): Boolean {
        if (priceText == null) return true // no price info — allow it
        val price = extractNumericPrice(priceText) ?: return true
        return price <= maxPrice
    }

    fun acceptsGrapes(grapes: List<String>): Boolean {
        if (ignoredGrapes.isEmpty()) return true
        val ignored = ignoredGrapes.map { it.lowercase() }.toSet()
        return grapes.none { it.lowercase() in ignored }
    }

    fun acceptsType(type: String): Boolean {
        if (allowedTypes.size == WineType.entries.size) return true // all selected = no filter
        val lower = type.lowercase()
        return allowedTypes.any { lower.contains(it.label.lowercase()) }
    }

    companion object {
        const val DEFAULT_MAX_PRICE = 60

        fun localCurrencySymbol(): String {
            return try {
                java.util.Currency.getInstance(java.util.Locale.getDefault()).symbol
            } catch (_: Exception) {
                "$"
            }
        }

        private fun extractNumericPrice(text: String): Double? {
            // Currency symbol patterns ($/€/£)
            val currencyRegex = Regex("""[\$€£]\s*(\d+\.?\d*)|(\d+\.?\d*)\s*[\$€£]|(\d+\.\d{2})""")
            val currencyMatch = currencyRegex.find(text)
            if (currencyMatch != null) {
                val numStr = currencyMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
                if (numStr != null) return numStr.toDoubleOrNull()
            }

            // Glass/bottle format — "13/41" — use higher number (bottle price)
            val gbRegex = Regex("""\b(\d{1,4})/(\d{1,4})\b""")
            val gbMatch = gbRegex.find(text)
            if (gbMatch != null) {
                val prices = listOf(gbMatch.groupValues[1], gbMatch.groupValues[2])
                    .mapNotNull { it.toDoubleOrNull() }
                if (prices.isNotEmpty()) return prices.max()
            }

            // Bare trailing number (not a year)
            val bareRegex = Regex("""(?:^|\s)(\d{2,5})\s*$""")
            val bareMatch = bareRegex.find(text)
            if (bareMatch != null) {
                val num = bareMatch.groupValues[1].toIntOrNull()
                if (num != null && num !in 1900..2099) return num.toDouble()
            }

            return null
        }
    }
}

class WinePreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wine_preferences", Context.MODE_PRIVATE)

    fun load(): WinePreferences {
        val maxPrice = prefs.getInt("max_price", 60)
        val ignoredGrapes = prefs.getStringSet("ignored_grapes", emptySet()) ?: emptySet()
        val allowedTypeNames = prefs.getStringSet("allowed_types", null)
        val allowedTypes = if (allowedTypeNames == null) {
            WineType.entries.toSet()
        } else {
            allowedTypeNames.mapNotNull { name ->
                try { WineType.valueOf(name) } catch (_: Exception) { null }
            }.toSet()
        }
        return WinePreferences(maxPrice, ignoredGrapes, allowedTypes)
    }

    fun save(prefs: WinePreferences) {
        this.prefs.edit()
            .putInt("max_price", prefs.maxPrice)
            .putStringSet("ignored_grapes", prefs.ignoredGrapes)
            .putStringSet("allowed_types", prefs.allowedTypes.map { it.name }.toSet())
            .apply()
    }
}
