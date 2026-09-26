package com.wineselector.core.pricing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File

/**
 * Disk-backed price cache (one JSON file) so repeat scans of the same list don't
 * spend API quota. Corrupt files are ignored and rewritten.
 */
class JsonFilePriceCache(private val file: File, private val maxEntries: Int = 300) : PriceCache {
    private val lock = Any()
    private var entries: LinkedHashMap<String, PriceSummary>? = null

    override fun get(key: String): PriceSummary? = synchronized(lock) { load()[key] }

    override fun put(key: String, summary: PriceSummary) = synchronized(lock) {
        val map = load()
        map.remove(key)
        map[key] = summary
        while (map.size > maxEntries) map.remove(map.keys.first())
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(encode(map))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
        Unit
    }

    private fun load(): LinkedHashMap<String, PriceSummary> {
        entries?.let { return it }
        val loaded = runCatching { if (file.exists()) decode(file.readText()) else LinkedHashMap() }
            .getOrElse { LinkedHashMap() }
        entries = loaded
        return loaded
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(map: Map<String, PriceSummary>): String = buildJsonObject {
            for ((k, s) in map) put(k, buildJsonObject {
                put("query", s.query); put("low", s.low); put("median", s.median); put("high", s.high)
                put("currency", s.currencySymbol); put("vintageSpecific", s.vintageSpecific)
                put("source", s.source); put("fetchedAt", s.fetchedAtMillis)
                put("offers", buildJsonArray {
                    for (o in s.offers) add(buildJsonObject {
                        put("merchant", o.merchant); put("title", o.title); put("price", o.price)
                        put("currency", o.currencySymbol); put("url", o.url); put("vintageMatched", o.vintageMatched)
                    })
                })
            })
        }.toString()

        fun decode(text: String): LinkedHashMap<String, PriceSummary> {
            val root = json.parseToJsonElement(text).jsonObject
            val out = LinkedHashMap<String, PriceSummary>()
            for ((k, v) in root) {
                val o = v.jsonObject
                out[k] = PriceSummary(
                    query = o.s("query") ?: "",
                    offers = (o["offers"] as? JsonArray).orEmpty().map { e ->
                        val x = e.jsonObject
                        PriceOffer(x.s("merchant") ?: "", x.s("title") ?: "", x["price"]!!.jsonPrimitive.double,
                            x.s("currency"), x.s("url"), (x["vintageMatched"] as? JsonPrimitive)?.booleanOrNull)
                    },
                    low = o["low"]!!.jsonPrimitive.double,
                    median = o["median"]!!.jsonPrimitive.double,
                    high = o["high"]!!.jsonPrimitive.double,
                    currencySymbol = o.s("currency"),
                    vintageSpecific = (o["vintageSpecific"] as? JsonPrimitive)?.booleanOrNull ?: false,
                    source = o.s("source") ?: "",
                    fetchedAtMillis = o["fetchedAt"]!!.jsonPrimitive.long
                )
            }
            return out
        }

        private fun JsonObject.s(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
