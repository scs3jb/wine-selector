package com.wineselector.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Rect
import com.wineselector.app.data.DatasetSize
import com.wineselector.app.data.FoodCategory
import com.wineselector.app.data.HighlightTier
import com.wineselector.app.data.OcrResult
import com.wineselector.app.data.TextRecognitionService
import com.wineselector.app.data.WineHighlight
import com.wineselector.app.data.WinePairingEngine
import com.wineselector.app.data.WinePreferences
import com.wineselector.app.data.WinePreferencesStore
import com.wineselector.app.data.WineRecommendation
import com.wineselector.app.data.XWinesDatabase
import com.wineselector.app.data.XWinesDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class DatasetStatus {
    data class UsingBundled(val wineCount: Int) : DatasetStatus()
    data class Downloading(val progressPercent: Int, val datasetLabel: String) : DatasetStatus()
    object Extracting : DatasetStatus()
    data class UsingEnhanced(val wineCount: Int, val datasetLabel: String) : DatasetStatus()
    data class DownloadFailed(val message: String) : DatasetStatus()
    data class InsufficientSpace(val requiredMb: Long, val availableMb: Long) : DatasetStatus()
    object NeedsChoice : DatasetStatus()
}

class WineSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val textRecognitionService = TextRecognitionService()
    val xWinesDownloader = XWinesDownloader(application)
    private val preferencesStore = WinePreferencesStore(application)

    // Start with bundled dataset — always available instantly
    private var xWinesDb = XWinesDatabase().also { it.load(application) }
    private var winePairingEngine = WinePairingEngine(xWinesDb)

    private val _selectedCategory = MutableStateFlow<FoodCategory?>(null)
    val selectedCategory: StateFlow<FoodCategory?> = _selectedCategory.asStateFlow()

    private val _capturedImagePath = MutableStateFlow<String?>(null)
    val capturedImagePath: StateFlow<String?> = _capturedImagePath.asStateFlow()

    private val _recommendation = MutableStateFlow<WineRecommendation?>(null)
    val recommendation: StateFlow<WineRecommendation?> = _recommendation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()

    private val _datasetStatus = MutableStateFlow<DatasetStatus>(
        DatasetStatus.UsingBundled(xWinesDb.wineCount)
    )
    val datasetStatus: StateFlow<DatasetStatus> = _datasetStatus.asStateFlow()

    private val _showDatasetChoice = MutableStateFlow(false)
    val showDatasetChoice: StateFlow<Boolean> = _showDatasetChoice.asStateFlow()

    private val _winePreferences = MutableStateFlow(preferencesStore.load())
    val winePreferences: StateFlow<WinePreferences> = _winePreferences.asStateFlow()

    private val _wineHighlights = MutableStateFlow<List<WineHighlight>>(emptyList())
    val wineHighlights: StateFlow<List<WineHighlight>> = _wineHighlights.asStateFlow()

    private val _ocrImageSize = MutableStateFlow<Pair<Int, Int>?>(null)
    val ocrImageSize: StateFlow<Pair<Int, Int>?> = _ocrImageSize.asStateFlow()

    init {
        viewModelScope.launch { initializeDataset() }
    }

    private suspend fun initializeDataset() {
        // If user already chose and we have cached files, load them
        val cached = xWinesDownloader.getCachedFiles()
        if (cached != null) {
            loadEnhancedDataset(cached.first, cached.second)
            return
        }

        // If user previously chose to skip downloads
        if (xWinesDownloader.isSkipped()) {
            _datasetStatus.value = DatasetStatus.UsingBundled(xWinesDb.wineCount)
            return
        }

        // If user made a choice before but cache was cleared, re-download
        val savedChoice = xWinesDownloader.getSavedChoice()
        if (savedChoice != null) {
            downloadDataset(savedChoice)
            return
        }

        // First boot — show choice dialog
        _showDatasetChoice.value = true
        _datasetStatus.value = DatasetStatus.NeedsChoice
    }

    fun onDatasetChosen(choice: DatasetSize) {
        _showDatasetChoice.value = false
        viewModelScope.launch {
            // If this dataset is already cached, just load it — no re-download
            val cached = xWinesDownloader.getCachedFiles(choice)
            if (cached != null) {
                xWinesDownloader.saveChoice(choice)
                _datasetStatus.value = DatasetStatus.Extracting
                loadEnhancedDataset(cached.first, cached.second)
            } else {
                downloadDataset(choice)
            }
        }
    }

    fun onSkipDownload() {
        _showDatasetChoice.value = false
        xWinesDownloader.saveSkipChoice()
        // Reset to bundled dataset
        val app = getApplication<Application>()
        xWinesDb = XWinesDatabase().also { it.load(app) }
        winePairingEngine = WinePairingEngine(xWinesDb)
        _datasetStatus.value = DatasetStatus.UsingBundled(xWinesDb.wineCount)
    }

    private suspend fun downloadDataset(dataset: DatasetSize) {
        // Check available space
        if (!xWinesDownloader.hasEnoughSpace(dataset)) {
            val available = xWinesDownloader.getAvailableSpaceMb()
            _datasetStatus.value = DatasetStatus.InsufficientSpace(
                requiredMb = dataset.requiredSpaceMb,
                availableMb = available
            )
            return
        }

        _datasetStatus.value = DatasetStatus.Downloading(0, dataset.label)

        val result = xWinesDownloader.downloadDataset(dataset) { percent ->
            _datasetStatus.value = DatasetStatus.Downloading(percent, dataset.label)
        }

        val files = result.getOrElse { e ->
            _datasetStatus.value = DatasetStatus.DownloadFailed(e.message ?: "Download failed")
            return
        }

        _datasetStatus.value = DatasetStatus.Extracting
        loadEnhancedDataset(files.first, files.second)
    }

    private suspend fun loadEnhancedDataset(winesFile: File, ratingsFile: File) {
        try {
            val newDb = XWinesDatabase().also {
                it.loadFromFilesAsync(winesFile, ratingsFile)
            }

            // Hot-swap (back on Main)
            xWinesDb = newDb
            winePairingEngine = WinePairingEngine(newDb)

            val label = xWinesDownloader.getSavedChoice()?.label ?: "Enhanced"
            _datasetStatus.value = DatasetStatus.UsingEnhanced(newDb.wineCount, label)
        } catch (e: Exception) {
            val choice = xWinesDownloader.getSavedChoice()
            if (choice != null) xWinesDownloader.clearCache(choice)
            _datasetStatus.value = DatasetStatus.DownloadFailed(
                "Failed to load dataset: ${e.message}"
            )
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            val savedChoice = xWinesDownloader.getSavedChoice()
            if (savedChoice != null) {
                xWinesDownloader.clearCache(savedChoice)
                downloadDataset(savedChoice)
            } else {
                _showDatasetChoice.value = true
                _datasetStatus.value = DatasetStatus.NeedsChoice
            }
        }
    }

    fun changeDataset() {
        _showDatasetChoice.value = true
    }

    fun updatePreferences(prefs: WinePreferences) {
        _winePreferences.value = prefs
        preferencesStore.save(prefs)
    }

    fun selectCategory(category: FoodCategory) {
        _selectedCategory.value = category
    }

    fun onPhotoCaptured(photoFile: File) {
        _capturedImagePath.value = photoFile.absolutePath
        _showResult.value = true
        analyzeWineList(photoFile)
    }

    private fun analyzeWineList(photoFile: File) {
        val category = _selectedCategory.value ?: run {
            _error.value = "No food category selected. Please go back and select one."
            return
        }

        _isLoading.value = true
        _error.value = null
        _recommendation.value = null
        _wineHighlights.value = emptyList()

        viewModelScope.launch {
            try {
                val ocrResultKt = textRecognitionService.extractText(
                    photoFile,
                    getApplication()
                )

                ocrResultKt.fold(
                    onSuccess = { ocrResult ->
                        _ocrImageSize.value = Pair(ocrResult.imageWidth, ocrResult.imageHeight)

                        val scoredWines = winePairingEngine.recommendWines(
                            ocrResult.fullText, category, _winePreferences.value
                        )
                        val rec = winePairingEngine.buildRecommendation(
                            scoredWines, category, ocrResult.fullText
                        )
                        _recommendation.value = rec
                        _wineHighlights.value = buildHighlights(scoredWines, ocrResult)
                        _isLoading.value = false
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Failed to read wine list"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun buildHighlights(
        scoredWines: List<WinePairingEngine.ScoredWine>,
        ocrResult: OcrResult
    ): List<WineHighlight> {
        if (scoredWines.isEmpty()) return emptyList()

        val highlights = mutableListOf<WineHighlight>()
        val usedLineIndices = mutableSetOf<Int>()

        scoredWines.take(5).forEachIndexed { index, scored ->
            val tier = if (index == 0) HighlightTier.TOP_PICK else HighlightTier.ALTERNATIVE
            val originalLower = scored.originalText.lowercase()
            val matchedBoxes = mutableListOf<Rect>()

            for ((lineIdx, ocrLine) in ocrResult.lines.withIndex()) {
                if (lineIdx in usedLineIndices) continue
                val lineLower = ocrLine.text.trim().lowercase()
                if (lineLower.length > 2 && originalLower.contains(lineLower)) {
                    ocrLine.boundingBox?.let {
                        matchedBoxes.add(it)
                        usedLineIndices.add(lineIdx)
                    }
                }
            }

            if (matchedBoxes.isNotEmpty()) {
                highlights.add(
                    WineHighlight(
                        boundingBoxes = matchedBoxes,
                        tier = tier,
                        wineName = scored.displayName ?: scored.originalText
                    )
                )
            }
        }

        return highlights
    }

    fun setError(message: String) {
        _error.value = message
        _showResult.value = true
    }

    fun reset() {
        _capturedImagePath.value = null
        _recommendation.value = null
        _error.value = null
        _isLoading.value = false
        _showResult.value = false
        _wineHighlights.value = emptyList()
        _ocrImageSize.value = null
    }
}
