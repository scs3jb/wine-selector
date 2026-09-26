package com.wineselector.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wineselector.app.WineSelectorApplication
import com.wineselector.app.data.AppSettings
import com.wineselector.app.data.OcrException
import com.wineselector.core.analysis.AnalyzedWine
import com.wineselector.core.analysis.BottleAnalysis
import com.wineselector.core.analysis.MenuAnalysis
import com.wineselector.core.analysis.WineAnalyzer
import com.wineselector.core.db.DatasetSize
import com.wineselector.core.db.XWinesDatabase
import com.wineselector.core.history.HistoryWine
import com.wineselector.core.history.ScanKind
import com.wineselector.core.history.ScanRecord
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.pairing.PairingEngine
import com.wineselector.core.pairing.PairingScore
import com.wineselector.core.pricing.PriceResult
import com.wineselector.core.pricing.PriceService
import com.wineselector.core.pricing.SerpApiShoppingProvider
import com.wineselector.core.scan.MenuEntry
import com.wineselector.core.scan.MenuParser
import com.wineselector.core.scan.OcrPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class ScanMode { MENU, BOTTLE }

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Working(val imagePath: String, val mode: ScanMode, val stage: String) : ScanUiState
    data class Menu(
        val imagePath: String,
        val page: OcrPage,
        val entries: List<MenuEntry>,
        val analysis: MenuAnalysis
    ) : ScanUiState
    data class Bottle(val imagePath: String, val page: OcrPage, val analysis: BottleAnalysis) : ScanUiState
    data class Failed(val imagePath: String?, val mode: ScanMode, val message: String) : ScanUiState
}

enum class DatabaseSource(val label: String) {
    BUNDLED("Starter set"),
    SLIM("Slim"),
    FULL("Full")
}

data class DatabaseUiState(
    val wineCount: Int = 0,
    val source: DatabaseSource = DatabaseSource.BUNDLED,
    val loading: Boolean = true,
    val downloading: DatasetSize? = null,
    val downloadPercent: Int = 0,
    val error: String? = null
)

sealed interface PriceUiState {
    data object Loading : PriceUiState
    data class Loaded(val result: PriceResult) : PriceUiState
}

/**
 * Central app state: settings, the wine database, the current scan and its
 * online prices, and scan history. Screens observe the StateFlows.
 */
class WineSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WineSelectorApplication

    private val _settings = MutableStateFlow(app.settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _database = MutableStateFlow(DatabaseUiState())
    val database: StateFlow<DatabaseUiState> = _database.asStateFlow()

    private val _scan = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scan: StateFlow<ScanUiState> = _scan.asStateFlow()

    private val _prices = MutableStateFlow<Map<String, PriceUiState>>(emptyMap())
    val prices: StateFlow<Map<String, PriceUiState>> = _prices.asStateFlow()

    private val _history = MutableStateFlow<List<ScanRecord>>(emptyList())
    val history: StateFlow<List<ScanRecord>> = _history.asStateFlow()

    @Volatile private var db: XWinesDatabase = XWinesDatabase()
    @Volatile private var analyzer: WineAnalyzer = WineAnalyzer(db)
    @Volatile private var pairing: PairingEngine = PairingEngine(db)
    private var scanJob: Job? = null

    private val priceService = PriceService(
        SerpApiShoppingProvider(apiKey = { _settings.value.serpApiKey }, client = app.httpClient),
        app.priceCache
    )

    init {
        viewModelScope.launch { loadDatabase() }
        viewModelScope.launch(Dispatchers.IO) { _history.value = app.historyStore.all() }
    }

    // ---- Settings -------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val before = _settings.value
        val updated = transform(before)
        _settings.value = updated
        app.settingsStore.save(updated)
        if (updated.preferences != before.preferences || updated.lastFood != before.lastFood) rerankMenu()
    }

    fun selectFood(food: FoodCategory?) = updateSettings { it.copy(lastFood = food) }

    // ---- Database ---------------------------------------------------------------

    private suspend fun loadDatabase() {
        _database.update { it.copy(loading = true, error = null) }
        val bundled = withContext(Dispatchers.IO) {
            XWinesDatabase().apply {
                loadFromStreams(app.assets.open("xwines.csv"), app.assets.open("xwines_ratings.csv"))
            }
        }
        swapDatabase(bundled, DatabaseSource.BUNDLED)
        val choice = app.downloader.getSavedChoice()
        val cached = choice?.let { app.downloader.getCachedFiles(it) }
        if (choice != null && cached != null) {
            try {
                val loaded = XWinesDatabase().apply { loadFromFilesAsync(cached.first, cached.second) }
                swapDatabase(loaded, sourceFor(choice))
            } catch (e: Exception) {
                app.downloader.clearCache(choice)
                _database.update { it.copy(error = "The downloaded wine database was damaged and has been removed.") }
            }
        }
        _database.update { it.copy(loading = false) }
    }

    private fun sourceFor(dataset: DatasetSize) = if (dataset == DatasetSize.FULL) DatabaseSource.FULL else DatabaseSource.SLIM

    private fun swapDatabase(newDb: XWinesDatabase, source: DatabaseSource) {
        db = newDb
        analyzer = WineAnalyzer(newDb)
        pairing = PairingEngine(newDb)
        _database.update { it.copy(wineCount = newDb.wineCount, source = source) }
    }

    fun downloadDataset(dataset: DatasetSize) {
        if (_database.value.downloading != null) return
        if (!app.downloader.hasEnoughSpace(dataset)) {
            _database.update {
                it.copy(error = "Not enough free space: ${dataset.requiredSpaceMb} MB needed, " +
                    "${app.downloader.getAvailableSpaceMb()} MB available.")
            }
            return
        }
        _database.update { it.copy(downloading = dataset, downloadPercent = 0, error = null) }
        viewModelScope.launch {
            val result = app.downloader.downloadDataset(dataset) { pct ->
                _database.update { it.copy(downloadPercent = pct) }
            }
            result.fold(
                onSuccess = { (wines, ratings) ->
                    _database.update { it.copy(downloadPercent = 100, loading = true) }
                    try {
                        val loaded = XWinesDatabase().apply { loadFromFilesAsync(wines, ratings) }
                        swapDatabase(loaded, sourceFor(dataset))
                        rerankMenu(reidentify = true)
                    } catch (e: Exception) {
                        app.downloader.clearCache(dataset)
                        _database.update { it.copy(error = "The downloaded database couldn't be read: ${e.message}") }
                    }
                    _database.update { it.copy(downloading = null, loading = false) }
                },
                onFailure = { e ->
                    _database.update { it.copy(downloading = null, error = "Download failed: ${e.message ?: "unknown error"}") }
                }
            )
        }
    }

    fun useStarterDatabase() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { app.downloader.clearAll() }
            loadDatabase()
            rerankMenu(reidentify = true)
        }
    }

    fun dismissDatabaseError() = _database.update { it.copy(error = null) }

    // ---- Scanning -----------------------------------------------------------------

    fun importPhoto(uri: Uri, mode: ScanMode) {
        viewModelScope.launch {
            val file = newScanFile()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    } != null
                }.getOrDefault(false)
            }
            if (ok) processPhoto(file, mode, alreadyStored = true)
            else _scan.value = ScanUiState.Failed(null, mode, "Couldn't open that photo.")
        }
    }

    fun onPhotoCaptured(photo: File, mode: ScanMode) = processPhoto(photo, mode, alreadyStored = false)

    fun rescan(record: ScanRecord) {
        val mode = if (record.kind == ScanKind.MENU) ScanMode.MENU else ScanMode.BOTTLE
        val file = record.imagePath?.let(::File)
        if (file == null || !file.exists()) {
            _scan.value = ScanUiState.Failed(null, mode, "The photo for this scan is no longer available.")
            return
        }
        processPhoto(file, mode, alreadyStored = true, recordId = record.id)
    }

    private fun newScanFile() = File(app.scansDir, "scan_${System.currentTimeMillis()}.jpg")

    private fun processPhoto(photo: File, mode: ScanMode, alreadyStored: Boolean, recordId: String? = null) {
        scanJob?.cancel()
        _prices.value = emptyMap()
        _scan.value = ScanUiState.Working(photo.absolutePath, mode, "Preparing photo…")
        scanJob = viewModelScope.launch {
            val stored = if (alreadyStored) photo else withContext(Dispatchers.IO) {
                newScanFile().also { dest -> photo.copyTo(dest, overwrite = true); photo.delete() }
            }
            val path = stored.absolutePath
            try {
                val what = if (mode == ScanMode.MENU) "wine list" else "label"
                _scan.value = ScanUiState.Working(path, mode, "Reading the $what…")
                val page = app.ocrEngine.recognize(stored)
                _scan.value = ScanUiState.Working(path, mode, "Matching against ${"%,d".format(db.wineCount)} wines…")
                val food = _settings.value.lastFood
                val prefs = _settings.value.preferences
                val currentAnalyzer = analyzer
                val result = withContext(Dispatchers.Default) {
                    when (mode) {
                        ScanMode.MENU -> {
                            val entries = MenuParser.parse(page)
                            ScanUiState.Menu(path, page, entries, currentAnalyzer.analyzeMenuEntries(entries, food, prefs))
                        }
                        ScanMode.BOTTLE -> ScanUiState.Bottle(path, page, currentAnalyzer.analyzeBottle(page))
                    }
                }
                if (result is ScanUiState.Menu && result.analysis.wines.isEmpty() && result.analysis.hiddenByFilters == 0) {
                    _scan.value = ScanUiState.Failed(path, mode,
                        "Text was found but no wines could be picked out. Try photographing just the wine section, straight on and in good light.")
                    return@launch
                }
                _scan.value = result
                saveHistory(result, recordId)
                autoFetchPrices(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OcrException) {
                _scan.value = ScanUiState.Failed(path, mode, e.message ?: "Couldn't read the photo.")
            } catch (e: Exception) {
                _scan.value = ScanUiState.Failed(path, mode, "Something went wrong: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    /** Re-rank the current menu for a new food / preferences, optionally re-identifying with a new database. */
    private fun rerankMenu(reidentify: Boolean = false) {
        val s = _settings.value
        when (val current = _scan.value) {
            is ScanUiState.Menu ->
                _scan.value = current.copy(analysis = analyzer.analyzeMenuEntries(current.entries, s.lastFood, s.preferences))
            is ScanUiState.Bottle -> if (reidentify) _scan.value = current.copy(analysis = analyzer.analyzeBottle(current.page))
            else -> Unit
        }
    }

    /** Bottle scans: the user says the label is one of the alternatives. */
    fun chooseAlternative(key: String) {
        val current = _scan.value as? ScanUiState.Bottle ?: return
        val alt = current.analysis.alternatives.firstOrNull { it.key == key } ?: return
        val previous = current.analysis.wine
        val newAlternatives = current.analysis.alternatives.filter { it.key != key } +
            listOfNotNull(previous.takeIf { it.isIdentified }?.copy(key = "prev_${previous.entry?.wineId}"))
        _scan.value = current.copy(
            analysis = current.analysis.copy(wine = alt.copy(key = "b0", matchConfidence = 1f), alternatives = newAlternatives)
        )
        _prices.update { it - "b0" }
        if (_settings.value.autoFetchPrices && _settings.value.serpApiKey.isNotBlank()) fetchPrice("b0")
    }

    fun clearScan() {
        scanJob?.cancel()
        _scan.value = ScanUiState.Idle
        _prices.value = emptyMap()
    }

    fun wine(key: String): AnalyzedWine? = when (val s = _scan.value) {
        is ScanUiState.Menu -> s.analysis.wine(key)
        is ScanUiState.Bottle ->
            if (s.analysis.wine.key == key) s.analysis.wine else s.analysis.alternatives.firstOrNull { it.key == key }
        else -> null
    }

    fun pairingsFor(wine: AnalyzedWine): List<Pair<FoodCategory, PairingScore>> =
        pairing.scoreAll(wine.entry, listOfNotNull(wine.scannedName, wine.scannedDetail).joinToString(", "), wine.style)

    // ---- Prices ----------------------------------------------------------------------

    fun fetchPrice(key: String, force: Boolean = false) {
        val wine = wine(key) ?: return
        val existing = _prices.value[key]
        val alreadyDone = existing is PriceUiState.Loading ||
            (existing is PriceUiState.Loaded && existing.result !is PriceResult.Failed && existing.result !is PriceResult.Unavailable)
        if (!force && alreadyDone) return
        _prices.update { it + (key to PriceUiState.Loading) }
        viewModelScope.launch {
            val result = priceService.lookup(wine.searchName, wine.vintage.year, _settings.value.priceCountry)
            _prices.update { it + (key to PriceUiState.Loaded(result)) }
        }
    }

    private fun autoFetchPrices(result: ScanUiState) {
        val s = _settings.value
        if (!s.autoFetchPrices || s.serpApiKey.isBlank()) return
        when (result) {
            is ScanUiState.Menu -> result.analysis.wines.take(3).forEach { fetchPrice(it.key) }
            is ScanUiState.Bottle -> fetchPrice(result.analysis.wine.key)
            else -> Unit
        }
    }

    // ---- History ----------------------------------------------------------------------

    private suspend fun saveHistory(result: ScanUiState, recordId: String?) = withContext(Dispatchers.IO) {
        val (kind, path, wines) = when (result) {
            is ScanUiState.Menu -> Triple(ScanKind.MENU, result.imagePath, result.analysis.wines)
            is ScanUiState.Bottle -> Triple(ScanKind.BOTTLE, result.imagePath, listOf(result.analysis.wine))
            else -> return@withContext
        }
        val record = ScanRecord(
            id = recordId ?: UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            kind = kind,
            imagePath = path,
            food = _settings.value.lastFood?.name,
            wines = wines.take(5).map {
                HistoryWine(it.displayName, it.vintage.year, it.style?.name, it.pairing?.score, it.bottlePrice, it.currency, it.rating)
            }
        )
        val evicted = app.historyStore.add(record)
        evicted.forEach { r -> r.imagePath?.let { File(it).delete() } }
        _history.value = app.historyStore.all()
    }

    fun deleteHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.historyStore.remove(id)?.imagePath?.let { p -> if (!isCurrentImage(p)) File(p).delete() }
            _history.value = app.historyStore.all()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            app.historyStore.clear().forEach { r -> r.imagePath?.let { p -> if (!isCurrentImage(p)) File(p).delete() } }
            _history.value = emptyList()
        }
    }

    private fun isCurrentImage(path: String) = when (val s = _scan.value) {
        is ScanUiState.Menu -> s.imagePath == path
        is ScanUiState.Bottle -> s.imagePath == path
        is ScanUiState.Working -> s.imagePath == path
        is ScanUiState.Failed -> s.imagePath == path
        ScanUiState.Idle -> false
    }
}
