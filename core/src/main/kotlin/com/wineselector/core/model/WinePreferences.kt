package com.wineselector.core.model

/**
 * User filters applied to scan results. Pure data; persistence lives in the app.
 */
data class WinePreferences(
    val maxPrice: Int = NO_LIMIT,
    val ignoredGrapes: Set<String> = emptySet(),
    val allowedStyles: Set<WineStyle> = WineStyle.entries.toSet()
) {
    fun acceptsPrice(price: Double?): Boolean {
        if (price == null || maxPrice == NO_LIMIT) return true
        return price <= maxPrice
    }

    fun acceptsGrapes(grapes: Collection<String>): Boolean {
        if (ignoredGrapes.isEmpty()) return true
        val ignored = ignoredGrapes.map { it.lowercase().trim() }.toSet()
        return grapes.none { it.lowercase().trim() in ignored }
    }

    fun acceptsStyle(style: WineStyle?): Boolean {
        if (style == null || allowedStyles.size == WineStyle.entries.size) return true
        return style in allowedStyles
    }

    companion object {
        const val NO_LIMIT = Int.MAX_VALUE
    }
}
