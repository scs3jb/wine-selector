package com.wineselector.core.pricing

/** One retail listing found online. */
data class PriceOffer(
    val merchant: String,
    val title: String,
    val price: Double,
    val currencySymbol: String?,
    val url: String?,
    /** true = listing names the requested vintage, false = another vintage, null = no vintage shown. */
    val vintageMatched: Boolean? = null
)

data class PriceSummary(
    val query: String,
    val offers: List<PriceOffer>,
    val low: Double,
    val median: Double,
    val high: Double,
    val currencySymbol: String?,
    /** True when the prices are for the requested vintage (not other years of the same wine). */
    val vintageSpecific: Boolean,
    val source: String,
    val fetchedAtMillis: Long
)

sealed class PriceResult {
    data class Found(val summary: PriceSummary) : PriceResult()
    data object NotFound : PriceResult()
    /** No provider configured (e.g. no API key); deep links still work. */
    data class Unavailable(val reason: String) : PriceResult()
    data class Failed(val message: String) : PriceResult()
}

/** A source of retail prices. Implementations must be safe to call from any thread. */
interface PriceProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun search(query: String, countryCode: String?): List<PriceOffer>
}

class PriceProviderException(message: String) : Exception(message)
