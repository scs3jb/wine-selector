# Wine Selector - Claude Code Project Guide

## Project Overview

Android app (Kotlin + Jetpack Compose) that photographs wine lists and recommends wine pairings based on food selection. Uses on-device ML Kit text recognition + a built-in wine pairing rules engine. No API key, no internet required for core functionality.

## Build Environment

All build tools are self-contained in `.buildtools/` — no system-level installs required.
**Before building, always check if `.buildtools/` exists.** If it does not, run the setup steps below.

### Environment Variables (required for every build)

```bash
export JAVA_HOME="/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/src/wine-selector/.buildtools/android-sdk"
```

### First-Time Setup: Installing Build Tools

If `.buildtools/jdk-17.0.2` or `.buildtools/android-sdk` do not exist, install them:

```bash
# 1. Create directory
mkdir -p /src/wine-selector/.buildtools && cd /src/wine-selector/.buildtools

# 2. Download and install JDK 17 (Adoptium Temurin, Linux x64)
curl -fSL -o jdk17.tar.gz "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.2_8.tar.gz"
tar xzf jdk17.tar.gz && rm jdk17.tar.gz
mv jdk-17.0.2+8 jdk-17.0.2

# 3. Download and install Android SDK command-line tools
mkdir -p android-sdk
curl -fSL -o cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdline-tools.zip -d android-sdk/ && rm cmdline-tools.zip
mkdir -p android-sdk/cmdline-tools/latest
mv android-sdk/cmdline-tools/bin android-sdk/cmdline-tools/lib android-sdk/cmdline-tools/latest/

# 4. Set env vars (needed for sdkmanager)
export JAVA_HOME="/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/src/wine-selector/.buildtools/android-sdk"

# 5. Accept licenses and install SDK components
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

After setup, verify with: `$JAVA_HOME/bin/java -version` (should show 17.0.2).

### Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew testDebugUnitTest      # Run unit tests
./gradlew clean                  # Clean build outputs
./gradlew dependencies           # List all dependencies
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Key Architecture Decisions

- **Fully on-device** — No cloud API, no API key, no internet needed for wine analysis
- **ML Kit Text Recognition** — Google's on-device OCR extracts wine list text from photos (model bundled in APK)
- **Wine Pairing Rules Engine** — Knowledge base of 60+ grape varieties, regions, and styles with food pairing scores (1-10). Two-pass architecture: Pass 1 matches against X-Wines database, Pass 2 uses keyword fallback with section context inheritance
- **Keyword-base + harmonization bonus scoring** — Wine scores are anchored to keyword/grape scores from the rules engine. X-Wines harmonization adds a +2 bonus (capped at 10) rather than overriding the base score, ensuring consistent rankings
- **Spatial OCR merging** — `OcrResult.spatiallyMergedText()` groups OCR lines by vertical overlap into visual rows (sorted left-to-right), fixing two-column menu layouts where producer names and descriptions are read as separate text blocks
- **Comprehensive price detection** — Detects currency symbols ($/€/£), glass/bottle format (13/41), and bare trailing numbers. Used consistently across entry splitting, coalescing, filtering, and display name cleaning via `lineHasPrice()`
- **X-Wines Dataset Integration** — Three-tier strategy: bundled 100-wine fallback, downloadable Slim (1K wines/150K ratings), or Full (100K wines/21M ratings). User chooses on first boot
- **Performance-optimized matching** — XWinesDatabase builds HashMap indexes after loading for O(1) word lookups instead of O(n) linear scan. Matching completes in <1ms per query even with 100K wines
- **Single Activity** — `MainActivity` hosts Compose UI with state-based screen switching (no Navigation Compose)
- **Single ViewModel** — `WineSelectorViewModel` holds all app state
- **In-app CameraX** — Direct photo capture with no confirmation step (tap shutter → instant result)
- **State-based screens** — Simple `when (currentScreen)` switching via `rememberSaveable`, NOT Navigation Compose (removed due to Compose BOM animation version conflicts)

## Important Gotchas

- **Compose BOM version matters** — BOM `2024.02.00` is required. Earlier versions (e.g., `2024.01.00`) cause `NoSuchMethodError` in `KeyframesSpec` animation classes at runtime when used with certain transitive dependencies
- **Navigation Compose was removed** — It pulled in incompatible `compose-animation` versions. The app uses simple state-based screen switching instead
- **CameraX lifecycle** — Must bind to the Activity lifecycle (via `context.findActivity()`), NOT `LocalLifecycleOwner` which returns `NavBackStackEntry` and can be DESTROYED
- **Camera capture** — Uses file-based `OnImageSavedCallback`, NOT `OnImageCapturedCallback` (which returns YUV data that `BitmapFactory` can't decode)
- **CameraX 1.3.1** — Do NOT call `.setJpegQuality()` — that method was added in CameraX 1.4.0 and causes `NoSuchMethodError` at runtime
- **Image display** — Uses Coil `AsyncImage` with file path, NOT in-memory `ByteArray` (which causes OOM on high-res photos)
- **Price detection consistency** — All price detection in `WinePairingEngine` must use `lineHasPrice()` (not `PRICE_PATTERN` alone), which checks currency symbols, glass/bottle format, and bare trailing numbers. Using `PRICE_PATTERN` alone misses bare number prices (e.g., "7000") and causes entry splitting failures where wines merge into mega-entries and bypass the price filter
- **Pass 1 scoring** — Do NOT use flat scores (e.g., 8-10) for X-Wines harmonization matches. This overrides keyword differentiation and makes rankings inconsistent. Always compute a keyword/grape base score first, then add harmonization as a +2 bonus

## Code Conventions

- Kotlin with Jetpack Compose (no XML layouts)
- Material 3 theming with wine-themed colors defined in `ui/theme/Color.kt`
- `@OptIn` annotations used for experimental Material 3 and Layout APIs
- State management via `StateFlow` in ViewModel, collected with `collectAsState()` in Composables
- Screen names are plain strings: `"home"`, `"camera"`, `"result"`

## File Layout

```
app/src/main/java/com/wineselector/app/
├── MainActivity.kt              # Activity entry point
├── WineSelectorApp.kt           # State-based screen switching + theme
├── data/
│   ├── FoodCategory.kt          # 12 food categories with emoji icons
│   ├── WineRecommendation.kt    # Data class for recommendation results (includes optional XWineEntry)
│   ├── OcrResult.kt             # OCR data classes with spatial line merging for two-column menus
│   ├── TextRecognitionService.kt # ML Kit on-device OCR wrapper
│   ├── WinePairingEngine.kt     # Two-pass rules engine: X-Wines DB match → keyword fallback, 60+ grape/region profiles
│   ├── WinePreferences.kt       # User preferences (max price, ignored grapes, wine type filter) with multi-format price parsing
│   ├── XWinesDatabase.kt        # X-Wines CSV loader with HashMap indexes for fast matching
│   └── XWinesDownloader.kt      # Downloads zip datasets, extracts CSVs, manages cache and user choice
├── viewmodel/
│   └── WineSelectorViewModel.kt # Central state: photo capture → OCR → pairing → result + dataset management
└── ui/
    ├── screens/
    │   ├── HomeScreen.kt        # Food picker + scan button + dataset choice dialog
    │   ├── CameraScreen.kt      # CameraX in-app capture (no confirm step)
    │   └── ResultScreen.kt      # Recommendation display with photo preview
    ├── components/
    │   ├── FoodCategoryPicker.kt       # FlowRow of FilterChips
    │   └── WineRecommendationCard.kt   # Recommendation card UI with X-Wines metadata
    └── theme/
        ├── Theme.kt     # Material 3 theme (light/dark)
        ├── Color.kt     # Wine-themed palette (deep reds, golds, cream)
        └── Type.kt      # Serif headlines typography
```

## Common Tasks

### Adding a new food category

Edit `data/FoodCategory.kt` and add an entry to the enum:

```kotlin
STEAK("Steak", "\uD83E\uDD69"),
```

The food picker grid updates automatically.

### Adding a new wine/grape to the pairing engine

Edit `data/WinePairingEngine.kt`, add an entry to `wineKeywords`:

```kotlin
put("new grape", WineProfile(
    mapOf(FoodCategory.BEEF to 8, FoodCategory.FISH to 3, ...),
    "Description of why this grape pairs well"
))
```

Keywords are matched case-insensitively against OCR text. Use lowercase. These keywords are used in both passes: Pass 2 matches them directly against OCR text, and Pass 1 uses them via `inferScoreFromGrapes()` to score X-Wines entries by their grape varieties.

### Adding a new screen

1. Create a composable in `ui/screens/`
2. Add a case in `WineSelectorApp.kt` inside the `when (currentScreen)` block
3. Set `currentScreen = "newscreen"` to navigate

### Modifying the camera behavior

Edit `ui/screens/CameraScreen.kt`. The camera uses CameraX with:
- `CAPTURE_MODE_MINIMIZE_LATENCY` for fast capture
- File-based output to `cacheDir/wine_list.jpg`
- Activity lifecycle binding via `context.findActivity()`

## Emulator Testing

This environment lacks KVM hardware acceleration, so the Android emulator cannot run here. To test:

- Transfer the APK to a physical device, or
- Build locally on a machine with KVM support and use the emulator

## Dependencies

Managed in `app/build.gradle.kts`. Key dependency versions:

- Compose BOM: `2024.02.00` (DO NOT downgrade — causes animation crashes)
- CameraX: `1.3.1`
- ML Kit Text Recognition: `16.0.0`
- Coil: `2.5.0`
- Kotlin: `1.9.22`
- Compose Compiler: `1.5.8`
- AGP: `8.2.2`

## Processing Pipeline

1. **Photo capture** — CameraX saves JPEG to `cacheDir/wine_list.jpg`
2. **OCR** — `TextRecognitionService` uses ML Kit to extract text with per-line bounding boxes
3. **Spatial merge** — `OcrResult.spatiallyMergedText()` groups lines by vertical overlap into visual rows, fixing two-column layouts
4. **Entry coalescing** — `coalesceEntries()` groups consecutive OCR lines into wine entries using heuristics (vintage detection, price detection, section headers, entry boundary splitting via `shouldSplitBefore`)
5. **Two-pass matching**:
   - **Pass 1 (X-Wines)** — Matches entries against the X-Wines database by name. Computes a stable base score from keyword/grape inference, then adds +2 harmonization bonus if X-Wines confirms the food pairing (capped at 10). Wines without keyword matches get a modest rating-based score (3-5)
   - **Pass 2 (Keywords)** — Fallback for entries not matched in Pass 1. Scans for known grape/region keywords in OCR text, with section context inheritance (e.g., wines under a "Champagne" header inherit that keyword). Falls back to X-Wines grape inference if no keyword match
6. **Preference filtering** — Filters by max price (supports $/€/£ symbols, glass/bottle format like 13/41, and bare trailing numbers), ignored grapes, and allowed wine types
7. **Ranking** — Sorted by score (desc) → X-Wines average rating (desc) → alphabetical display name (asc) for deterministic tiebreaking
8. **Result** — Top match displayed with name, price (if detected), pairing reasoning, runner-up, and X-Wines metadata (rating, grapes, body, acidity, region, food harmonizations) when available

## X-Wines Dataset

Source: [github.com/rogerioxavier/X-Wines](https://github.com/rogerioxavier/X-Wines)

### Three-tier dataset strategy

On first boot, a dialog asks the user to choose:

| Option | Wines | Ratings | Download | Disk space |
|--------|-------|---------|----------|------------|
| **Full** (100K wines) | 100,000 | 21M | ~300 MB zip | Requires 1 GB free |
| **Slim** (1K wines) | 1,007 | 150K | ~3 MB zip | Requires 300 MB free |
| **Skip** (grape matching only) | 100 (bundled) | 1K | None | None |

### Download URLs (configured in `DatasetSize` enum in `XWinesDownloader.kt`)

```
Slim: https://repo.buildanddeploy.com/wines/XWines_Slim_1K_wines_150K_ratings.zip
Full: https://repo.buildanddeploy.com/wines/All-XWines_Full_100K_wines_21M_ratings.zip
```

### Startup behavior

1. ViewModel loads bundled 100-wine dataset instantly (synchronous, from assets)
2. Checks SharedPreferences for previous user choice:
   - **Cached dataset found?** → loads from `filesDir/xwines_cache/` → hot-swaps database
   - **Choice saved but no cache?** → re-downloads → caches → hot-swaps
   - **"Skip" was chosen?** → stays on bundled dataset
   - **No choice saved (first boot)?** → shows dataset choice dialog
3. User can use the app immediately — no blocking on download

### Performance optimization

`XWinesDatabase` builds two HashMap indexes after loading:

1. **Name word index** — Maps each significant word (length > 2) from wine names to a list of wines containing that word. During matching, only wines whose name words appear in the query are scored.
2. **Grape index** — Maps each grape variety name to the first wine having that grape. Used as a fallback when name matching fails.

Result: matching takes <1ms per query even with 100K wines, compared to ~50ms with linear scan.

### Key classes

- `DatasetSize` — Enum with `SLIM` and `FULL` variants, each containing URL, file names, space requirements, and validation thresholds
- `XWineEntry` — Data class for a single wine entry
- `XWinesDatabase` — CSV parser with HashMap-indexed matching. Methods: `load(context)`, `loadFromFiles(File, File)`, `loadFromStreams()`, `findMatch(ocrText)`, `harmonizesWithFood(entry, food)`
- `XWinesDownloader` — ZIP download via `HttpURLConnection`, extraction via `ZipInputStream`, caching to `filesDir/xwines_cache/`, space checking via `StatFs`, user choice persistence via SharedPreferences
- `DatasetStatus` — Sealed class (`NeedsChoice`, `UsingBundled`, `Downloading`, `Extracting`, `UsingEnhanced`, `DownloadFailed`, `InsufficientSpace`) exposed as `StateFlow` from ViewModel

### Cache management

- Cache location: `context.filesDir/xwines_cache/`
- Downloaded zips are extracted to `wines.csv` and `ratings.csv`, zip is deleted after extraction
- Files validated by minimum size before accepting
- Corrupt cache auto-cleared on parse failure
- Users can clear via Android Settings → Apps → Wine Selector → Storage → Clear data
- Developers can change dataset via `viewModel.changeDataset()` (shows choice dialog again)

### Harmonize → FoodCategory mapping

Defined in `XWinesDatabase.harmonizeToCategory`. X-Wines food labels ("Beef", "Poultry", "Shellfish", "Codfish", etc.) are mapped to the app's 12 `FoodCategory` enum values.

## Tests

80 unit tests across 3 test suites:

- `WinePairingEngineTest` (30 tests) — Wine list matching across 5 scenarios, price extraction, X-Wines boosting
- `XWinesDatabaseTest` (42 tests) — CSV parsing, indexed name/grape matching, food harmonization, slim dataset loading, performance benchmarks
- `XWinesDownloaderTest` (8 tests) — URL configuration, space requirements, dataset filename validation

Run with: `./gradlew testDebugUnitTest`
