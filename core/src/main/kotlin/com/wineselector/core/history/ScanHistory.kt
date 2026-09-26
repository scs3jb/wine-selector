package com.wineselector.core.history

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File

enum class ScanKind { MENU, BOTTLE }

/** A wine remembered from a past scan. */
data class HistoryWine(
    val name: String,
    val vintage: Int?,
    val style: String?,
    val pairingScore: Int?,
    val price: Double?,
    val currency: String?,
    val rating: Float?
)

data class ScanRecord(
    val id: String,
    val timestampMillis: Long,
    val kind: ScanKind,
    val imagePath: String?,
    val food: String?,
    val wines: List<HistoryWine>
)

/** Newest-first scan history persisted as a small JSON file. */
class ScanHistoryStore(private val file: File, private val maxRecords: Int = 50) {
    private val lock = Any()

    fun all(): List<ScanRecord> = synchronized(lock) {
        runCatching { if (file.exists()) decode(file.readText()) else emptyList() }.getOrElse { emptyList() }
    }

    /** Adds [record] and returns records evicted beyond [maxRecords] (so their photos can be deleted). */
    fun add(record: ScanRecord): List<ScanRecord> = synchronized(lock) {
        val list = listOf(record) + all().filter { it.id != record.id }
        write(list.take(maxRecords))
        list.drop(maxRecords)
    }

    fun remove(id: String): ScanRecord? = synchronized(lock) {
        val list = all()
        val removed = list.firstOrNull { it.id == id }
        write(list.filter { it.id != id })
        removed
    }

    fun clear(): List<ScanRecord> = synchronized(lock) { all().also { write(emptyList()) } }

    private fun write(records: List<ScanRecord>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(encode(records))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(records: List<ScanRecord>): String = buildJsonArray {
            for (r in records) add(buildJsonObject {
                put("id", r.id); put("ts", r.timestampMillis); put("kind", r.kind.name)
                put("image", r.imagePath); put("food", r.food)
                put("wines", buildJsonArray {
                    for (w in r.wines) add(buildJsonObject {
                        put("name", w.name); put("vintage", w.vintage); put("style", w.style)
                        put("score", w.pairingScore); put("price", w.price); put("currency", w.currency)
                        put("rating", w.rating)
                    })
                })
            })
        }.toString()

        fun decode(text: String): List<ScanRecord> = json.parseToJsonElement(text).jsonArray.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            ScanRecord(
                id = o.s("id") ?: return@mapNotNull null,
                timestampMillis = o["ts"]?.jsonPrimitive?.long ?: 0L,
                kind = runCatching { ScanKind.valueOf(o.s("kind") ?: "MENU") }.getOrDefault(ScanKind.MENU),
                imagePath = o.s("image"),
                food = o.s("food"),
                wines = (o["wines"] as? JsonArray).orEmpty().mapNotNull { we ->
                    val w = we as? JsonObject ?: return@mapNotNull null
                    HistoryWine(
                        name = w.s("name") ?: return@mapNotNull null,
                        vintage = (w["vintage"] as? JsonPrimitive)?.intOrNull,
                        style = w.s("style"),
                        pairingScore = (w["score"] as? JsonPrimitive)?.intOrNull,
                        price = (w["price"] as? JsonPrimitive)?.doubleOrNull,
                        currency = w.s("currency"),
                        rating = (w["rating"] as? JsonPrimitive)?.floatOrNull
                    )
                }
            )
        }

        private fun JsonObject.s(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
