package com.wineselector.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wineselector.app.ui.screens.CameraScreen
import com.wineselector.app.ui.screens.HomeScreen
import com.wineselector.app.ui.screens.ResultScreen
import com.wineselector.app.ui.screens.SettingsScreen
import com.wineselector.app.ui.theme.WineSelectorTheme
import com.wineselector.app.viewmodel.WineSelectorViewModel

@Composable
fun WineSelectorApp(viewModel: WineSelectorViewModel) {
    WineSelectorTheme(dynamicColor = false) {
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        val capturedImagePath by viewModel.capturedImagePath.collectAsState()
        val recommendation by viewModel.recommendation.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        val showResult by viewModel.showResult.collectAsState()
        val datasetStatus by viewModel.datasetStatus.collectAsState()
        val showDatasetChoice by viewModel.showDatasetChoice.collectAsState()
        val winePreferences by viewModel.winePreferences.collectAsState()
        val wineHighlights by viewModel.wineHighlights.collectAsState()
        val ocrImageSize by viewModel.ocrImageSize.collectAsState()

        var currentScreen by rememberSaveable { mutableStateOf("home") }

        if (showResult && currentScreen == "camera") {
            currentScreen = "result"
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                currentScreen = "camera"
            }
        }

        when (currentScreen) {
            "home" -> {
                HomeScreen(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    onScanWineList = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    datasetStatus = datasetStatus,
                    showDatasetChoice = showDatasetChoice,
                    onDatasetChosen = { viewModel.onDatasetChosen(it) },
                    onSkipDownload = { viewModel.onSkipDownload() },
                    onRetryDownload = { viewModel.retryDownload() },
                    onChangeDataset = { viewModel.changeDataset() },
                    maxPrice = winePreferences.maxPrice,
                    onOpenSettings = { currentScreen = "settings" }
                )
            }
            "camera" -> {
                CameraScreen(
                    onPhotoCaptured = { photoFile ->
                        viewModel.onPhotoCaptured(photoFile)
                    },
                    onBack = { currentScreen = "home" }
                )
            }
            "result" -> {
                ResultScreen(
                    imagePath = capturedImagePath,
                    foodCategory = selectedCategory,
                    recommendation = recommendation,
                    isLoading = isLoading,
                    error = error,
                    wineHighlights = wineHighlights,
                    ocrImageSize = ocrImageSize,
                    onBack = {
                        viewModel.reset()
                        currentScreen = "home"
                    },
                    onTryAnother = {
                        viewModel.reset()
                        currentScreen = "home"
                    }
                )
            }
            "settings" -> {
                SettingsScreen(
                    preferences = winePreferences,
                    onPreferencesChanged = { viewModel.updatePreferences(it) },
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
}
