package com.wineselector.core.pricing

import com.wineselector.core.text.TextNormalizer
import com.wineselector.core.vintage.VintageParser
import java.util.concurrent.ConcurrentHashMap

/** Cache for price summaries. The app provides a disk-backed version. */
interface PriceCache {
    fun get(key: String): PriceSummary?
    fun put(key: String, summary: PriceSummary)
}

class InMemoryPriceCache : PriceCache {
    private val map = ConcurrentHashMap<String, PriceSummary>()
    override fun get(key: String) = map[key]
    override fun put(key: String, summary: PriceSummary) { map[key] = summary }
}

/**
 * Finds what a wine costs online, vintage first. Searches "<wine> <vintage>" and
 * keeps listings for that vintage (or with no vintage shown); if too few are
 * found it falls back to any vintage and says so. Listings for other bottle
 * sizes, multipacks and accessories are dropped, as are price outliers.
 */
class PriceService(
    private val provider: PriceProvider,
    private val cache: PriceCache = InMemoryPriceCache(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = 24L * 60 * 60 * 1000
) {

    suspend fun lookup(wineName: String, vintage: Int?, countryCode: String?): PriceResult {
        if (!provider.isConfigured) {
            return PriceResult.Unavailable("Add a free SerpApi key in Settings to see live retail prices.")
        }
        val key = cacheKey(wineName, vintage, countryCode)
        cache.get(key)?.takeIf { clock() - it.fetchedAtMillis < ttlMillis }?.let { return PriceResult.Found(it) }

        return try {
            var vintageSpecific = false
            var query = wineName
            var offers = emptyList<PriceOffer>()
            if (vintage != null) {
                query = "$wineName $vintage"
                offers = OfferFilter.filter(provider.search(query, countryCode), wineName, vintage, strictVintage = true)
                vintageSpecific = offers.any { it.vintageMatched == true }
            }
            if (offers.size < 2) {
                val fallback = OfferFilter.filter(provider.search(wineName, countryCode), wineName, vintage, strictVintage = false)
                if (fallback.size > offers.size) {
                    offers = fallback
                    query = wineName
                    vintageSpecific = vintage != null && fallback.all { it.vintageMatched != false } &&
                        fallback.any { it.vintageMatched == true }
                }
            }
            if (offers.isEmpty()) return PriceResult.NotFound
            val summary = summarize(query, offers, vintageSpecific)
            cache.put(key, summary)
            PriceResult.Found(summary)
        } catch (e: PriceProviderException) {
            PriceResult.Failed(e.message ?: "Price lookup failed")
        }
    }

    private fun summarize(query: String, offers: List<PriceOffer>, vintageSpecific: Boolean): PriceSummary {
        val sorted = offers.sortedBy { it.price }
        val prices = sorted.map { it.price }
        val currency = sorted.groupingBy { it.currencySymbol }.eachCount().maxByOrNull { it.value }?.key
        return PriceSummary(
            query = query,
            offers = sorted,
            low = prices.first(),
            median = median(prices),
            high = prices.last(),
            currencySymbol = currency,
            vintageSpecific = vintageSpecific,
            source = provider.name,
            fetchedAtMillis = clock()
        )
    }

    companion object {
        fun cacheKey(name: String, vintage: Int?, country: String?) =
            "${TextNormalizer.normalizeForMatching(name).replace(Regex("[^a-z0-9]+"), " ").trim()}|${vintage ?: "any"}|${country ?: ""}"

        fun median(sorted: List<Double>): Double {
            if (sorted.isEmpty()) return 0.0
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        }
    }
}

/** Relevance, format, vintage and outlier filtering for shopping listings. */
object OfferFilter {
    private val WRONG_FORMAT = Regex(
        """(?i)\bmagnum\b|\b1[.,]5\s?l\b|\b150\s?cl\b|\b3\s?l\b|\bjeroboam\b|\b375\s?ml\b|\b37[.,]5\s?cl\b|\bhalf[- ]bottle\b|""" +
            """\b187\s?ml\b|\b18[.,]7\s?cl\b|\bminiature\b|\bcase of\b|\b\d+\s?(?:x|pack|bottles|btls)\b|\bx\s?\d+\b|\b\d+-pack\b|""" +
            """\bgift (?:set|box|basket)\b|\bglass(?:es)?\b|\bdecanter\b|\bopener\b|\bbook\b|\bposter\b|\bt-shirt\b|\bcandle\b|""" +
            """\bsauce\b|\bvinegar\b|\bchocolate\b|\bgummies\b|\bnon[- ]alcoholic\b|\balcohol[- ]free\b|\bdealcoholi[sz]ed\b"""
    )
    private val IGNORED_WORDS = setOf("the", "and", "wine", "wines", "red", "white", "de", "la", "le", "du", "des", "di", "del", "vineyards", "winery")

    fun filter(offers: List<PriceOffer>, wineName: String, vintage: Int?, strictVintage: Boolean): List<PriceOffer> {
        val nameWords = words(wineName)
        val relevant = offers.mapNotNull { o ->
            if (WRONG_FORMAT.containsMatchIn(o.title)) return@mapNotNull null
            val titleWords = words(o.title).toSet()
            val hits = nameWords.count { w -> w in titleWords || titleWords.any { it.length >= 5 && TextNormalizer.levenshteinDistance(it, w) <= 1 } }
            val needed = if (nameWords.size <= 2) nameWords.size else kotlin.math.ceil(nameWords.size * 0.6).toInt()
            if (nameWords.isEmpty() || hits < needed) return@mapNotNull null
            val titleYear = VintageParser.parse(o.title).year
            val matched = if (vintage == null || titleYear == null) null else titleYear == vintage
            if (strictVintage && matched == false) return@mapNotNull null
            o.copy(vintageMatched = matched)
        }
        return removeOutliers(relevant).distinctBy { it.merchant.lowercase() to it.price }
    }

    private fun words(text: String) = TextNormalizer.normalizeForMatching(text).replace(Regex("[^a-z0-9 ]"), " ")
        .split(' ').filter { it.length > 1 && it !in IGNORED_WORDS && !it.all(Char::isDigit) }.distinct()

    private fun removeOutliers(offers: List<PriceOffer>): List<PriceOffer> {
        if (offers.size < 4) return offers
        val m = PriceService.median(offers.map { it.price }.sorted())
        return offers.filter { it.price >= m / 2.5 && it.price <= m * 2.5 }
    }
}
