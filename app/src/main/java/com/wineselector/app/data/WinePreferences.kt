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

        private fun extractNumericPrice(text: String): Double? {
            val regex = Regex("""[\$€£]\s*(\d+\.?\d*)|(\d+\.?\d*)\s*[\$€£]|(\d+\.\d{2})""")
            val match = regex.find(text) ?: return null
            val numStr = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
            return numStr.toDoubleOrNull()
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
