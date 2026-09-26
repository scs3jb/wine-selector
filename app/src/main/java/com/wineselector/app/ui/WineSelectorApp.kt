package com.wineselector.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wineselector.app.ui.screens.BottleContext
import com.wineselector.app.ui.screens.CameraScreen
import com.wineselector.app.ui.screens.EmptyScanPlaceholder
import com.wineselector.app.ui.screens.HistoryScreen
import com.wineselector.app.ui.screens.HomeScreen
import com.wineselector.app.ui.screens.MenuResultsScreen
import com.wineselector.app.ui.screens.ScanFailed
import com.wineselector.app.ui.screens.ScanProgress
import com.wineselector.app.ui.screens.SettingsScreen
import com.wineselector.app.ui.screens.WineDetailScreen
import com.wineselector.app.ui.theme.WineSelectorTheme
import com.wineselector.app.viewmodel.ScanMode
import com.wineselector.app.viewmodel.ScanUiState
import com.wineselector.app.viewmodel.WineSelectorViewModel

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera/{mode}"
    const val SCAN = "scan"
    const val WINE = "wine/{key}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    fun camera(mode: ScanMode) = "camera/${mode.name}"
    fun wine(key: String) = "wine/$key"
}

@Composable
fun WineSelectorApp(viewModel: WineSelectorViewModel) {
    WineSelectorTheme {
        val nav = rememberNavController()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val database by viewModel.database.collectAsStateWithLifecycle()
        val scan by viewModel.scan.collectAsStateWithLifecycle()
        val prices by viewModel.prices.collectAsStateWithLifecycle()
        val history by viewModel.history.collectAsStateWithLifecycle()

        var importMode by remember { mutableStateOf(ScanMode.MENU) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.importPhoto(uri, importMode)
                nav.navigate(Routes.SCAN) { popUpTo(Routes.HOME) }
            }
        }
        val importPhoto: (ScanMode) -> Unit = { mode ->
            importMode = mode
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        val goHome: () -> Unit = {
            viewModel.clearScan()
            nav.popBackStack(Routes.HOME, inclusive = false)
        }
        val retake: (ScanMode) -> Unit = { mode -> nav.navigate(Routes.camera(mode)) { popUpTo(Routes.HOME) } }

        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    food = settings.lastFood,
                    onFoodSelected = viewModel::selectFood,
                    database = database,
                    history = history,
                    onScan = { nav.navigate(Routes.camera(it)) },
                    onImport = importPhoto,
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenRecord = { record -> viewModel.rescan(record); nav.navigate(Routes.SCAN) }
                )
            }
            composable(Routes.CAMERA, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
                val mode = runCatching { ScanMode.valueOf(entry.arguments?.getString("mode") ?: "") }.getOrDefault(ScanMode.MENU)
                CameraScreen(
                    initialMode = mode,
                    onCaptured = { file, m ->
                        viewModel.onPhotoCaptured(file, m)
                        nav.navigate(Routes.SCAN) { popUpTo(Routes.HOME) }
                    },
                    onImport = importPhoto,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SCAN) {
                ScanRoute(viewModel, nav, scan, goHome, retake)
            }
            composable(Routes.WINE, arguments = listOf(navArgument("key") { type = NavType.StringType })) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                // Re-read on every scan change so re-ranking (food, database) is reflected.
                val wine = remember(scan, key) { viewModel.wine(key) }
                if (wine == null) {
                    EmptyScanPlaceholder(onBack = goHome)
                } else {
                    WineDetailScreen(
                        wine = wine,
                        food = settings.lastFood,
                        pairings = remember(wine) { viewModel.pairingsFor(wine) },
                        price = prices[key],
                        hasPriceKey = settings.serpApiKey.isNotBlank(),
                        onFetchPrice = { force -> viewModel.fetchPrice(key, force) },
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onBack = { nav.popBackStack() }
                    )
                }
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    history = history,
                    onOpen = { record -> viewModel.rescan(record); nav.navigate(Routes.SCAN) { popUpTo(Routes.HOME) } },
                    onDelete = viewModel::deleteHistory,
                    onClearAll = viewModel::clearHistory,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    database = database,
                    onUpdate = viewModel::updateSettings,
                    onDownload = viewModel::downloadDataset,
                    onUseStarter = viewModel::useStarterDatabase,
                    onDismissDbError = viewModel::dismissDatabaseError,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun ScanRoute(
    viewModel: WineSelectorViewModel,
    nav: NavHostController,
    scan: ScanUiState,
    goHome: () -> Unit,
    retake: (ScanMode) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val database by viewModel.database.collectAsStateWithLifecycle()
    when (scan) {
        ScanUiState.Idle -> EmptyScanPlaceholder(onBack = goHome)
        is ScanUiState.Working -> ScanProgress(scan, onCancel = goHome)
        is ScanUiState.Failed -> ScanFailed(scan, onRetake = { retake(scan.mode) }, onBack = goHome)
        is ScanUiState.Menu -> MenuResultsScreen(
            state = scan,
            food = settings.lastFood,
            prices = prices,
            databaseSource = database.source,
            onFoodSelected = viewModel::selectFood,
            onOpenWine = { nav.navigate(Routes.wine(it)) },
            onRetake = { retake(ScanMode.MENU) },
            onBack = goHome,
            onOpenSettings = { nav.navigate(Routes.SETTINGS) }
        )
        is ScanUiState.Bottle -> {
            val wine = scan.analysis.wine
            WineDetailScreen(
                wine = wine,
                food = settings.lastFood,
                pairings = remember(wine) { viewModel.pairingsFor(wine) },
                price = prices[wine.key],
                hasPriceKey = settings.serpApiKey.isNotBlank(),
                onFetchPrice = { force -> viewModel.fetchPrice(wine.key, force) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onBack = goHome,
                bottle = BottleContext(
                    imagePath = scan.imagePath,
                    alternatives = scan.analysis.alternatives,
                    onChooseAlternative = viewModel::chooseAlternative,
                    onRetake = { retake(ScanMode.BOTTLE) }
                )
            )
        }
    }
}
