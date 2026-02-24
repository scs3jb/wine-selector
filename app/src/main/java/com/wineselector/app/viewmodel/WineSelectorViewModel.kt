package com.wineselector.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Rect
import com.wineselector.app.data.FoodCategory
import com.wineselector.app.data.HighlightTier
import com.wineselector.app.data.OcrResult
import com.wineselector.app.data.TextRecognitionService
import com.wineselector.app.data.WineHighlight
import com.wineselector.app.data.WinePairingEngine
import com.wineselector.app.data.WinePreferences
import com.wineselector.app.data.WinePreferencesStore
import com.wineselector.app.data.WineRecommendation
import com.wineselector.app.data.TextNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class WineSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val textRecognitionService = TextRecognitionService()
    private val preferencesStore = WinePreferencesStore(application)
    private val winePairingEngine = WinePairingEngine()

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

    private val _winePreferences = MutableStateFlow(preferencesStore.load())
    val winePreferences: StateFlow<WinePreferences> = _winePreferences.asStateFlow()

    private val _wineHighlights = MutableStateFlow<List<WineHighlight>>(emptyList())
    val wineHighlights: StateFlow<List<WineHighlight>> = _wineHighlights.asStateFlow()

    private val _ocrImageSize = MutableStateFlow<Pair<Int, Int>?>(null)
    val ocrImageSize: StateFlow<Pair<Int, Int>?> = _ocrImageSize.asStateFlow()

    fun updatePreferences(prefs: WinePreferences) {
        _winePreferences.value = prefs
        preferencesStore.save(prefs)
    }

    // ==========================================
    // Wine Analysis Pipeline
    // ==========================================

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

                        val mergedText = ocrResult.spatiallyMergedText()
                        val ocrLines = mergedText.lines()

                        // Build wine name candidates from OCR lines.
                        // Try each line individually AND pairs of consecutive lines,
                        // because menus often split wine names across lines.
                        val wineNames = buildOcrCandidates(ocrLines)

                        val scoredWines = winePairingEngine.recommendWines(
                            wineNames, category, _winePreferences.value, ocrLines
                        )
                        val rec = winePairingEngine.buildRecommendation(
                            scoredWines, category, mergedText
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

    /**
     * Build highlights for the photo overlay. Only highlights the single
     * best-matching OCR line per wine — avoids over-highlighting.
     */
    private fun buildHighlights(
        scoredWines: List<WinePairingEngine.ScoredWine>,
        ocrResult: OcrResult
    ): List<WineHighlight> {
        if (scoredWines.isEmpty()) return emptyList()

        val highlights = mutableListOf<WineHighlight>()
        val usedLineIndices = mutableSetOf<Int>()

        scoredWines.take(4).forEachIndexed { index, scored ->
            val tier = when (index) {
                0 -> HighlightTier.GOLD
                1 -> HighlightTier.SILVER
                2 -> HighlightTier.BRONZE
                else -> HighlightTier.RED
            }
            val displayName = (scored.displayName ?: scored.originalText).lowercase()
            val displayNormalized = TextNormalizer.normalizeForMatching(displayName)
            val nameWords = displayNormalized
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 }
                .toSet()

            // Find the single best-matching OCR line
            var bestLineIdx = -1
            var bestOverlap = 0
            var bestBox: Rect? = null

            for ((lineIdx, ocrLine) in ocrResult.lines.withIndex()) {
                if (lineIdx in usedLineIndices) continue
                val lineText = ocrLine.text.trim()
                if (lineText.none { it.isLetter() }) continue
                if (ocrLine.boundingBox == null) continue
                val lineNormalized = TextNormalizer.normalizeForMatching(lineText.lowercase())

                val overlap = nameWords.count { lineNormalized.contains(it) }
                if (overlap > bestOverlap) {
                    bestOverlap = overlap
                    bestLineIdx = lineIdx
                    bestBox = ocrLine.boundingBox
                }
            }

            // Require at least 2 word overlap (or 1 if name has only 1 word)
            val minOverlap = if (nameWords.size <= 1) 1 else 2
            if (bestOverlap >= minOverlap && bestBox != null) {
                usedLineIndices.add(bestLineIdx)
                highlights.add(
                    WineHighlight(
                        boundingBoxes = listOf(bestBox),
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

    /**
     * Build wine name candidates from raw OCR lines.
     * Uses individual lines only — each line is a candidate.
     * Multi-word grape keywords (e.g., "cabernet sauvignon", "pinot noir")
     * will still match when the full name is on one line.
     */
    private fun buildOcrCandidates(ocrLines: List<String>): List<String> {
        return ocrLines.filter { it.isNotBlank() }
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
