package com.wineselector.app.data

import android.content.Context
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WinePreferences
import com.wineselector.core.model.WineStyle
import java.util.Locale

data class AppSettings(
    val preferences: WinePreferences = WinePreferences(),
    val serpApiKey: String = "",
    val priceCountry: String = Locale.getDefault().country.ifBlank { "US" },
    val autoFetchPrices: Boolean = true,
    val lastFood: FoodCategory? = null
)

/** SharedPreferences-backed settings. Reads the previous app version's keys too. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("wine_preferences", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val maxPrice = prefs.getInt("max_price", WinePreferences.NO_LIMIT)
        val ignored = prefs.getStringSet("ignored_grapes", emptySet()).orEmpty()
        val styleNames = prefs.getStringSet("allowed_styles", null)
            ?: prefs.getStringSet("allowed_types", null) // v1 stored RED/WHITE/ROSE here
        val styles = styleNames?.mapNotNull { runCatching { WineStyle.valueOf(it) }.getOrNull() }?.toSet()
            ?.let { if (prefs.contains("allowed_styles")) it else it + setOf(WineStyle.SPARKLING, WineStyle.DESSERT, WineStyle.FORTIFIED) }
            ?.ifEmpty { null }
            ?: WineStyle.entries.toSet()
        return AppSettings(
            preferences = WinePreferences(maxPrice, ignored, styles),
            serpApiKey = prefs.getString("serpapi_key", "").orEmpty(),
            priceCountry = prefs.getString("price_country", null) ?: Locale.getDefault().country.ifBlank { "US" },
            autoFetchPrices = prefs.getBoolean("auto_fetch_prices", true),
            lastFood = prefs.getString("last_food", null)?.let { runCatching { FoodCategory.valueOf(it) }.getOrNull() }
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putInt("max_price", s.preferences.maxPrice)
            .putStringSet("ignored_grapes", s.preferences.ignoredGrapes)
            .putStringSet("allowed_styles", s.preferences.allowedStyles.map { it.name }.toSet())
            .putString("serpapi_key", s.serpApiKey.trim())
            .putString("price_country", s.priceCountry.trim().uppercase())
            .putBoolean("auto_fetch_prices", s.autoFetchPrices)
            .putString("last_food", s.lastFood?.name)
            .apply()
    }
}
