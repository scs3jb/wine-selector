package com.wineselector.core.pricing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Live retail prices from Google Shopping via SerpApi (https://serpapi.com,
 * free tier available). The user supplies their own API key in Settings.
 */
class SerpApiShoppingProvider(
    private val apiKey: () -> String?,
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = "https://serpapi.com/".toHttpUrl()
) : PriceProvider {

    override val name = "Google Shopping"
    override val isConfigured: Boolean get() = !apiKey().isNullOrBlank()

    override suspend fun search(query: String, countryCode: String?): List<PriceOffer> = withContext(Dispatchers.IO) {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) throw PriceProviderException("No SerpApi key configured")
        val url = baseUrl.newBuilder()
            .addPathSegment("search.json")
            .addQueryParameter("engine", "google_shopping")
            .addQueryParameter("q", query)
            .addQueryParameter("hl", "en")
            .apply { countryCode?.takeIf { it.length == 2 }?.let { addQueryParameter("gl", it.lowercase()) } }
            .addQueryParameter("api_key", key)
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        val body = try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val apiError = runCatching { json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                    throw PriceProviderException(apiError ?: "Price search failed (HTTP ${resp.code})")
                }
                text
            }
        } catch (e: IOException) {
            throw PriceProviderException("Couldn't reach the price service: ${e.message ?: "network error"}")
        }
        parse(body)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private val SYMBOL = Regex("""[£$€¥₹]|(?:US|AU|NZ|CA|HK|S)\$""")
        private val NUMBER = Regex("""\d{1,3}(?:[,.\s]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?""")

        /** Parse a SerpApi google_shopping response body into offers. */
        fun parse(body: String): List<PriceOffer> {
            val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
            root["error"]?.jsonPrimitive?.contentOrNull?.let { err ->
                // "Google Shopping hasn't returned any results for this query." is not an error for us.
                if ("hasn't returned any results" in err || "no results" in err.lowercase()) return emptyList()
                throw PriceProviderException(err)
            }
            val results = (root["shopping_results"] as? JsonArray).orEmpty() +
                (root["inline_shopping_results"] as? JsonArray).orEmpty()
            return results.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val title = o.str("title") ?: return@mapNotNull null
                val priceText = o.str("price")
                val price = o["extracted_price"]?.jsonPrimitive?.doubleOrNull ?: priceText?.let(::parsePrice)
                    ?: return@mapNotNull null
                if (price <= 0) return@mapNotNull null
                PriceOffer(
                    merchant = o.str("source") ?: "Online retailer",
                    title = title,
                    price = price,
                    currencySymbol = priceText?.let { SYMBOL.find(it)?.value?.takeLast(1) },
                    url = o.str("product_link") ?: o.str("link")
                )
            }
        }

        private fun JsonObject.str(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.contentOrNull?.takeIf { it.isNotBlank() }

        internal fun parsePrice(text: String): Double? {
            val raw = NUMBER.find(text)?.value ?: return null
            val cleaned = when {
                // "1.299,00" or "24,50" → decimal comma
                Regex("""[.,]\d{2}$""").containsMatchIn(raw) && raw[raw.length - 3] == ',' ->
                    raw.dropLast(3).replace(".", "").replace(" ", "") + "." + raw.takeLast(2)
                else -> raw.replace(",", "").replace(" ", "")
            }
            return cleaned.toDoubleOrNull()
        }
    }
}
