package com.wineselector.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wineselector.app.data.FoodCategory
import com.wineselector.app.data.TextRecognitionService
import com.wineselector.app.data.WinePairingEngine
import com.wineselector.app.data.WineRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class WineSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val textRecognitionService = TextRecognitionService()
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

        viewModelScope.launch {
            try {
                // Step 1: Extract text from photo using ML Kit
                val textResult = textRecognitionService.extractText(
                    photoFile,
                    getApplication()
                )

                textResult.fold(
                    onSuccess = { extractedText ->
                        // Step 2: Run wine pairing engine
                        val scoredWines = winePairingEngine.recommendWines(extractedText, category)
                        val rec = winePairingEngine.buildRecommendation(scoredWines, category, extractedText)
                        _recommendation.value = rec
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
    }
}
