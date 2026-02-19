package com.wineselector.app.data

import android.content.Context
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class XWineEntry(
    val wineId: String,
    val wineName: String,
    val type: String,
    val grapes: List<String>,
    val harmonize: List<String>,
    val abv: Float?,
    val body: String,
    val acidity: String,
    val country: String,
    val regionName: String,
    val wineryName: String,
    val averageRating: Float?,
    val vintages: List<Int> = emptyList()
)

enum class VintageMatch {
    EXACT,           // OCR year found in wine's vintages list
    CLOSEST,         // OCR year not found, showing data for closest vintage
    NOT_IN_DATABASE, // Wine found but no vintages data at all
    NOT_CHECKED      // No year extracted from OCR text
}

data class XWineMatchResult(
    val entry: XWineEntry,
    val vintageMatch: VintageMatch = VintageMatch.NOT_CHECKED,
    val ocrYear: Int? = null
)

class XWinesDatabase {

    private var wines: List<XWineEntry> = emptyList()

    // Performance indexes — built once after loading, O(1) lookups during matching
    private var nameWordIndex: Map<String, MutableList<IndexEntry>> = emptyMap()
    private var wineWordCount: Map<XWineEntry, Int> = emptyMap()
    private var grapeIndex: Map<String, XWineEntry> = emptyMap()

    private data class IndexEntry(val wine: XWineEntry, val totalNameWords: Int)

    // Words too common across wine names to be useful for matching.
    // These cause false matches: "Château Petrus" matching "Château Belair" etc.
    private val STOP_WORDS = setOf(
        // French/Italian/Spanish titles & connectors
        "château", "chateau", "domaine", "clos", "casa", "bodega", "tenuta",
        "grand", "cru", "premier", "classé", "classe", "superiore",
        "del", "della", "delle", "dei", "des", "les", "the",
        // Common wine label terms
        "reserve", "reserva", "riserva", "selection", "estate", "vineyard",
        "vineyards", "winery", "cellars", "collection",
        // Regions/geography too broad to distinguish
        "valley", "river", "hills", "county", "coast", "mountain",
        "napa", "sonoma", "russian", "santa", "san",
        // Generic descriptors
        "wine", "wines", "red", "white", "old", "vine", "vines",
        "special", "limited", "edition", "vintage", "bottle",
        "brut", "sec", "dry", "sweet", "noir", "blanc"
    )

    private val harmonizeToCategory: Map<String, FoodCategory> = buildMap {
        // Meat
        put("beef", FoodCategory.BEEF)
        put("veal", FoodCategory.BEEF)
        put("game meat", FoodCategory.BEEF)
        put("pork", FoodCategory.PORK)
        put("cured meat", FoodCategory.PORK)
        put("cold cuts", FoodCategory.PORK)
        put("barbecue", FoodCategory.PORK)
        put("chicken", FoodCategory.CHICKEN)
        put("poultry", FoodCategory.CHICKEN)
        put("lamb", FoodCategory.LAMB)
        // Pasta / grains
        put("pasta", FoodCategory.PASTA)
        put("risotto", FoodCategory.PASTA)
        put("tomato dishes", FoodCategory.PASTA)
        // Fish / seafood
        put("fish", FoodCategory.FISH)
        put("rich fish", FoodCategory.FISH)
        put("lean fish", FoodCategory.FISH)
        put("codfish", FoodCategory.FISH)
        put("seafood", FoodCategory.SEAFOOD)
        put("shellfish", FoodCategory.SEAFOOD)
        // Vegetarian
        put("vegetarian", FoodCategory.VEGETARIAN)
        put("salad", FoodCategory.VEGETARIAN)
        put("mushrooms", FoodCategory.VEGETARIAN)
        // Cheese
        put("maturated cheese", FoodCategory.CHEESE)
        put("soft cheese", FoodCategory.CHEESE)
        put("blue cheese", FoodCategory.CHEESE)
        put("hard cheese", FoodCategory.CHEESE)
        put("goat cheese", FoodCategory.CHEESE)
        put("cheese", FoodCategory.CHEESE)
        // Dessert
        put("sweet dessert", FoodCategory.DESSERT)
        put("fruit dessert", FoodCategory.DESSERT)
        put("cake", FoodCategory.DESSERT)
        put("chocolate", FoodCategory.DESSERT)
        put("fruit", FoodCategory.DESSERT)
        // Pizza
        put("pizza", FoodCategory.PIZZA)
        put("grilled", FoodCategory.PIZZA)
    }

    companion object {
        private const val BINARY_CACHE_NAME = "xwines.bin"
        private const val BINARY_VERSION: Int = 3

        private val VINTAGE_REGEX = Regex("""\b(19|20)\d{2}\b""")

        fun findClosestVintage(vintages: List<Int>, targetYear: Int): Int? {
            if (vintages.isEmpty()) return null
            return vintages.minByOrNull { kotlin.math.abs(it - targetYear) }
        }
    }

    fun load(context: Context) {
        loadFromStreams(
            context.assets.open("xwines.csv"),
            context.assets.open("xwines_ratings.csv")
        )
    }

    fun loadFromFiles(winesFile: File, ratingsFile: File) {
        val cacheDir = winesFile.parentFile!!
        val binFile = File(cacheDir, BINARY_CACHE_NAME)

        // Try binary cache first — skips all CSV parsing
        if (binFile.exists() && binFile.length() > 8) {
            try {
                loadFromBinary(binFile)
                return
            } catch (_: Exception) {
                binFile.delete()
            }
        }

        // Fall back to CSV parsing
        loadFromStreams(FileInputStream(winesFile), FileInputStream(ratingsFile))

        // Write binary cache for next time
        try {
            writeBinaryCache(binFile)
        } catch (_: Exception) {
            binFile.delete()
        }
    }

    suspend fun loadFromFilesAsync(winesFile: File, ratingsFile: File) {
        val cacheDir = winesFile.parentFile!!
        val binFile = File(cacheDir, BINARY_CACHE_NAME)

        // Try binary cache first
        if (binFile.exists() && binFile.length() > 8) {
            try {
                loadFromBinary(binFile)
                return
            } catch (_: Exception) {
                binFile.delete()
            }
        }

        // Parallel CSV parsing
        loadFromStreamsParallel(FileInputStream(winesFile), FileInputStream(ratingsFile))

        // Write binary cache for next time
        try {
            writeBinaryCache(binFile)
        } catch (_: Exception) {
            binFile.delete()
        }
    }

    fun loadFromStreams(winesStream: java.io.InputStream, ratingsStream: java.io.InputStream) {
        val ratings = loadRatings(ratingsStream)
        wines = loadWines(winesStream, ratings)
        buildIndexes()
    }

    private suspend fun loadFromStreamsParallel(
        winesStream: java.io.InputStream,
        ratingsStream: java.io.InputStream
    ) = coroutineScope {
        val ratingsDeferred = async(Dispatchers.IO) { loadRatings(ratingsStream) }
        val winesDeferred = async(Dispatchers.IO) { loadWinesWithoutRatings(winesStream) }

        val ratings = ratingsDeferred.await()
        val winesList = winesDeferred.await()

        // Attach ratings — fast 100K-entry loop
        wines = winesList.map { wine ->
            val avg = ratings[wine.wineId]
            if (avg != null) wine.copy(averageRating = avg) else wine
        }
        buildIndexes()
    }

    // ==========================================
    // Binary cache — read/write
    // ==========================================

    private fun writeBinaryCache(file: File) {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        DataOutputStream(FileOutputStream(tempFile).buffered(65536)).use { out ->
            out.writeInt(BINARY_VERSION)
            out.writeInt(wines.size)
            for (wine in wines) {
                out.writeUTF(wine.wineId)
                out.writeUTF(wine.wineName)
                out.writeUTF(wine.type)
                // grapes
                out.writeShort(wine.grapes.size)
                for (g in wine.grapes) out.writeUTF(g)
                // harmonize
                out.writeShort(wine.harmonize.size)
                for (h in wine.harmonize) out.writeUTF(h)
                // abv: write NaN if null
                out.writeFloat(wine.abv ?: Float.NaN)
                out.writeUTF(wine.body)
                out.writeUTF(wine.acidity)
                out.writeUTF(wine.country)
                out.writeUTF(wine.regionName)
                out.writeUTF(wine.wineryName)
                // averageRating: write NaN if null
                out.writeFloat(wine.averageRating ?: Float.NaN)
                // vintages
                out.writeShort(wine.vintages.size)
                for (v in wine.vintages) out.writeInt(v)
            }
        }
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
    }

    private fun loadFromBinary(file: File) {
        DataInputStream(FileInputStream(file).buffered(65536)).use { inp ->
            val version = inp.readInt()
            if (version != BINARY_VERSION) throw Exception("Binary cache version mismatch")
            val count = inp.readInt()
            val result = ArrayList<XWineEntry>(count)
            for (i in 0 until count) {
                val wineId = inp.readUTF()
                val wineName = inp.readUTF()
                val type = inp.readUTF()
                val grapeCount = inp.readUnsignedShort()
                val grapes = if (grapeCount == 0) emptyList() else {
                    ArrayList<String>(grapeCount).also { list ->
                        repeat(grapeCount) { list.add(inp.readUTF()) }
                    }
                }
                val harmonizeCount = inp.readUnsignedShort()
                val harmonize = if (harmonizeCount == 0) emptyList() else {
                    ArrayList<String>(harmonizeCount).also { list ->
                        repeat(harmonizeCount) { list.add(inp.readUTF()) }
                    }
                }
                val abvRaw = inp.readFloat()
                val abv = if (abvRaw.isNaN()) null else abvRaw
                val body = inp.readUTF()
                val acidity = inp.readUTF()
                val country = inp.readUTF()
                val regionName = inp.readUTF()
                val wineryName = inp.readUTF()
                val ratingRaw = inp.readFloat()
                val averageRating = if (ratingRaw.isNaN()) null else ratingRaw
                val vintageCount = inp.readUnsignedShort()
                val vintages = if (vintageCount == 0) emptyList() else {
                    ArrayList<Int>(vintageCount).also { list ->
                        repeat(vintageCount) { list.add(inp.readInt()) }
                    }
                }
                result.add(XWineEntry(
                    wineId, wineName, type, grapes, harmonize,
                    abv, body, acidity, country, regionName, wineryName,
                    averageRating, vintages
                ))
            }
            wines = result
        }
        buildIndexes()
    }

    // ==========================================
    // Indexes
    // ==========================================

    private fun buildIndexes() {
        // Name word index: map each significant word -> list of (wine, totalWordCount)
        // Excludes stop words that are too common to be useful for matching
        val nameIdx = HashMap<String, MutableList<IndexEntry>>(wines.size * 3)
        val wordCounts = HashMap<XWineEntry, Int>(wines.size)
        for (wine in wines) {
            val words = TextNormalizer.normalizeForMatching(wine.wineName)
                .replace(Regex("[^a-z\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 && it !in STOP_WORDS }
            val entry = IndexEntry(wine, words.size)
            wordCounts[wine] = words.size
            for (word in words) {
                nameIdx.getOrPut(word) { mutableListOf() }.add(entry)
            }
        }
        nameWordIndex = nameIdx
        wineWordCount = wordCounts

        // Grape index: map each grape name (normalized) -> first wine that has it
        val gIdx = HashMap<String, XWineEntry>(wines.size)
        for (wine in wines) {
            for (grape in wine.grapes) {
                val key = TextNormalizer.normalizeForMatching(grape)
                if (key.length > 3 && key !in gIdx) {
                    gIdx[key] = wine
                }
            }
        }
        grapeIndex = gIdx
    }

    // ==========================================
    // CSV parsing
    // ==========================================

    private fun loadRatings(stream: java.io.InputStream): Map<String, Float> {
        val ratingSums = HashMap<String, FloatArray>(131072)

        stream.use { s ->
            BufferedReader(InputStreamReader(s), 131072).use { reader ->
                reader.readLine() // skip header
                var line = reader.readLine()
                while (line != null) {
                    // Fast inline CSV parse: RatingID,UserID,WineID,Vintage,Rating,Date
                    val firstComma = line.indexOf(',')
                    if (firstComma > 0) {
                        val secondComma = line.indexOf(',', firstComma + 1)
                        if (secondComma > 0) {
                            val thirdComma = line.indexOf(',', secondComma + 1)
                            if (thirdComma > 0) {
                                val fourthComma = line.indexOf(',', thirdComma + 1)
                                if (fourthComma > 0) {
                                    val fifthComma = line.indexOf(',', fourthComma + 1)
                                    val ratingEnd = if (fifthComma > 0) fifthComma else line.length
                                    val rating = parseFloatInline(line, fourthComma + 1, ratingEnd)
                                    if (!rating.isNaN()) {
                                        val wineId = line.substring(secondComma + 1, thirdComma)
                                        val arr = ratingSums.getOrPut(wineId) { floatArrayOf(0f, 0f) }
                                        arr[0] += rating  // sum
                                        arr[1] += 1f      // count
                                    }
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        }

        return buildMap(ratingSums.size) {
            for ((wineId, arr) in ratingSums) {
                val avg = arr[0] / arr[1]
                put(wineId, (avg * 10).toInt() / 10f)
            }
        }
    }

    /** Parse a float directly from a substring range, avoiding String allocation. */
    private fun parseFloatInline(s: String, start: Int, end: Int): Float {
        if (start >= end) return Float.NaN
        var result = 0f
        var decimal = false
        var divisor = 1f
        var negative = false
        var i = start
        if (i < end && s[i] == '-') { negative = true; i++ }
        while (i < end) {
            val c = s[i]
            if (c == '.') {
                if (decimal) return Float.NaN
                decimal = true
            } else {
                val digit = c - '0'
                if (digit !in 0..9) return Float.NaN
                if (decimal) {
                    divisor *= 10f
                    result += digit / divisor
                } else {
                    result = result * 10f + digit
                }
            }
            i++
        }
        return if (negative) -result else result
    }

    private fun loadWines(stream: java.io.InputStream, ratings: Map<String, Float>): List<XWineEntry> {
        val result = ArrayList<XWineEntry>(131072)

        stream.use { s ->
            BufferedReader(InputStreamReader(s), 131072).use { reader ->
                reader.readLine() // skip header
                var line = reader.readLine()
                while (line != null) {
                    val fields = parseCsvLine(line)
                    if (fields.size >= 15) {
                        val wineId = fields[0]
                        result.add(XWineEntry(
                            wineId = wineId,
                            wineName = fields[1],
                            type = fields[2],
                            grapes = parsePythonList(fields[4]),
                            harmonize = parsePythonList(fields[5]),
                            abv = fields[6].toFloatOrNull(),
                            body = fields[7],
                            acidity = fields[8],
                            country = fields[10],
                            regionName = fields[12],
                            wineryName = fields[14],
                            averageRating = ratings[wineId],
                            vintages = if (fields.size > 16) parseVintagesList(fields[16]) else emptyList()
                        ))
                    }
                    line = reader.readLine()
                }
            }
        }

        return result
    }

    private fun loadWinesWithoutRatings(stream: java.io.InputStream): List<XWineEntry> {
        val result = ArrayList<XWineEntry>(131072)

        stream.use { s ->
            BufferedReader(InputStreamReader(s), 131072).use { reader ->
                reader.readLine() // skip header
                var line = reader.readLine()
                while (line != null) {
                    val fields = parseCsvLine(line)
                    if (fields.size >= 15) {
                        result.add(XWineEntry(
                            wineId = fields[0],
                            wineName = fields[1],
                            type = fields[2],
                            grapes = parsePythonList(fields[4]),
                            harmonize = parsePythonList(fields[5]),
                            abv = fields[6].toFloatOrNull(),
                            body = fields[7],
                            acidity = fields[8],
                            country = fields[10],
                            regionName = fields[12],
                            wineryName = fields[14],
                            averageRating = null,
                            vintages = if (fields.size > 16) parseVintagesList(fields[16]) else emptyList()
                        ))
                    }
                    line = reader.readLine()
                }
            }
        }

        return result
    }

    val wineCount: Int get() = wines.size

    /**
     * Find an X-Wines entry matching OCR text using indexed lookups.
     * Uses name word index for O(candidates) matching instead of O(wines).
     *
     * Requires that the majority of the database wine's distinctive name words
     * appear in the OCR text. This prevents false matches where common words
     * like "château" or "valley" cause unrelated wines to match.
     */
    fun findMatch(ocrText: String): XWineEntry? {
        if (ocrText.isBlank()) return null
        val lower = TextNormalizer.normalizeForMatching(ocrText)

        // Build query words with OCR-corrected variants for fuzzy matching
        val rawWords = lower
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
        val queryWords = rawWords
            .flatMap { TextNormalizer.ocrWordVariants(it) }
            .filter { it !in STOP_WORDS }
            .toSet()

        // Score wines by how many of their distinctive name words appear in the OCR text
        val candidates = HashMap<XWineEntry, Int>(32)
        for (word in queryWords) {
            val entries = nameWordIndex[word] ?: continue
            for (entry in entries) {
                candidates[entry.wine] = (candidates[entry.wine] ?: 0) + 1
            }
        }

        // Find best match: require at least 2 distinctive word matches AND
        // that matched words cover >= 50% of the wine name's distinctive words.
        // Score by match ratio to prefer tighter matches.
        var bestMatch: XWineEntry? = null
        var bestRatio = 0f
        var bestCount = 0
        for ((wine, matchCount) in candidates) {
            val totalWords = wineWordCount[wine] ?: continue
            if (totalWords == 0) continue

            val ratio = matchCount.toFloat() / totalWords

            // Need at least 2 matched distinctive words and >= 50% coverage
            if (matchCount < 2) continue
            if (ratio < 0.5f) continue

            // Prefer higher ratio, then higher absolute count
            if (ratio > bestRatio || (ratio == bestRatio && matchCount > bestCount)) {
                bestRatio = ratio
                bestCount = matchCount
                bestMatch = wine
            }
        }
        if (bestMatch != null) return bestMatch

        // Fallback: grape index lookup (single words)
        for (word in queryWords) {
            grapeIndex[word]?.let { return it }
        }
        // Try multi-word grapes by checking if OCR text contains them
        for ((grape, wine) in grapeIndex) {
            if (grape.contains(' ') && lower.contains(grape)) {
                return wine
            }
        }

        return null
    }

    fun findMatchWithVintage(ocrText: String): XWineMatchResult? {
        val entry = findMatch(ocrText) ?: return null

        val yearMatch = VINTAGE_REGEX.find(ocrText)
        val ocrYear = yearMatch?.value?.toIntOrNull()

        if (ocrYear == null) {
            return XWineMatchResult(entry, VintageMatch.NOT_CHECKED)
        }

        if (entry.vintages.isEmpty()) {
            return XWineMatchResult(entry, VintageMatch.NOT_IN_DATABASE, ocrYear)
        }

        return if (ocrYear in entry.vintages) {
            XWineMatchResult(entry, VintageMatch.EXACT, ocrYear)
        } else {
            XWineMatchResult(entry, VintageMatch.CLOSEST, ocrYear)
        }
    }

    fun harmonizesWithFood(entry: XWineEntry, food: FoodCategory): Boolean {
        return entry.harmonize.any { harmonize ->
            harmonizeToCategory[harmonize.lowercase()] == food
        }
    }

    fun getMappedFoodCategories(entry: XWineEntry): Set<FoodCategory> {
        return entry.harmonize.mapNotNull { harmonize ->
            harmonizeToCategory[harmonize.lowercase()]
        }.toSet()
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    private fun parsePythonList(text: String): List<String> {
        if (text.isBlank() || text == "[]") return emptyList()
        return text
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("'") }
            .filter { it.isNotBlank() }
    }

    private fun parseVintagesList(text: String): List<Int> {
        if (text.isBlank() || text == "[]") return emptyList()
        return text
            .removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { it.trim().removeSurrounding("'").toIntOrNull() }
    }
}
