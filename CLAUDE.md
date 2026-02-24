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

**After every successful APK build, always tell the user:**
> "APK ready: `app/build/outputs/apk/debug/app-debug.apk`"

## Key Architecture Decisions

- **Fully on-device** — No cloud API, no API key, no internet needed for wine analysis
- **ML Kit Text Recognition** — Google's on-device OCR extracts wine list text from photos (model bundled in APK)
- **LLM-based wine name extraction** — On-device LLM (SmolLM2-360M-Instruct GGUF) extracts wine names from OCR text, replacing the old heuristic pipeline (~1000 lines of coalescing/keyword/section-context logic). Wine names are then matched exactly against the X-Wines database
- **WineNameExtractor with LlmBackend interface** — `WineNameExtractor` uses a pluggable `LlmBackend` interface for LLM inference. Prompt building and response parsing are testable independently. The llamacpp-kotlin library (0.1.2) is the intended backend but requires Kotlin 2.0+ (currently commented out in build.gradle.kts)
- **LlmModelManager** — Downloads and caches the GGUF model (~271MB) following the same pattern as `XWinesDownloader`: download → validate → cache in `filesDir/llm_model/`
- **Wine Pairing Engine** — Knowledge base of 60+ grape varieties with food pairing scores (1-10). Takes LLM-extracted wine names, matches against X-Wines DB via `findMatchTiered()`, scores via grape inference + harmonization bonus (+2, capped at 10). No keyword fallback or section context — only DB-matched wines are shown
- **Strict exact matching with fuzzy fallback** — Uses sorted-word index for order-independent matching with Levenshtein distance-1 fuzzy matching. Display names come from the database (canonical wine names)
- **Spatial OCR merging** — `OcrResult.spatiallyMergedText()` groups OCR lines by vertical overlap into visual rows (sorted left-to-right), fixing two-column menu layouts
- **Comprehensive price detection** — Detects currency symbols ($/€/£), glass/bottle format (13/41), and bare trailing numbers via `lineHasPrice()`
- **X-Wines Dataset Integration** — Three-tier strategy: bundled 100-wine fallback, downloadable Slim (1K wines/150K ratings), or Full (100K wines/21M ratings). User chooses on first boot
- **Performance-optimized matching** — XWinesDatabase builds three HashMap indexes plus an allIndexedWords set after loading: name word index, sorted-word index, grape index, and a flat word set for Levenshtein fuzzy fallback. O(1) lookups instead of O(n) linear scan. Matching completes in <1ms per query even with 100K wines
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
- **Price detection consistency** — All price detection in `WinePairingEngine` must use `lineHasPrice()` (not `PRICE_PATTERN` alone), which checks currency symbols, glass/bottle format, and bare trailing numbers
- **Harmonization bonus scoring** — Do NOT use flat scores (e.g., 8-10) for X-Wines harmonization matches. Always compute a keyword/grape base score first, then add harmonization as a +2 bonus (capped at 10)
- **Stop words** — "noir" and "blanc" are NOT stop words in XWinesDatabase — they're essential grape qualifiers. Without them, "Pinot Noir" and "Pinot Grigio" both reduce to "pinot" and become indistinguishable. Do NOT add them to the STOP_WORDS set
- **DB display names** — displayName is ALWAYS `xEntry.wineName` (the canonical DB name). NEVER use OCR text or LLM-extracted text as the display name — the card headline must match the DB metadata (grapes, region, body, etc.)
- **Vendored llama.cpp native libraries** — The pre-compiled arm64-v8a `.so` files are extracted from the `llamacpp-kotlin` AAR (Apache 2.0) and placed in `app/src/main/jniLibs/arm64-v8a/`. The Kotlin JNI wrapper (`org.nehuatl.llamacpp.LlamaContext`) is our own code — the package name MUST stay `org.nehuatl.llamacpp` to match the native JNI function names baked into the `.so` files. This approach avoids the Kotlin 2.0+ dependency that the published library requires

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
│   ├── LlmModelManager.kt      # GGUF model download/cache manager
│   ├── WineNameExtractor.kt    # LLM-based wine name extraction with LlmBackend interface
│   ├── WinePairingEngine.kt     # Scores LLM-extracted wine names against food via DB matching + grape profiles
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

The emulator runs locally in `.buildtools/android-sdk/` with KVM acceleration. Everything is self-contained — no system-wide installs needed.

### First-Time Emulator Setup

If the emulator or system image aren't installed yet:

```bash
export JAVA_HOME="/home/jbriggs/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"

# Install emulator and system image
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"

# Create AVD
echo "no" | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  -n wine_test -k "system-images;android-34;google_apis;x86_64" \
  -d pixel_6
```

### Starting the Emulator

```bash
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"

# Launch emulator (requires DISPLAY for GPU; use :1 or :0 depending on environment)
DISPLAY=:1 nohup $ANDROID_HOME/emulator/emulator \
  -avd wine_test -no-audio -gpu auto -no-boot-anim -memory 4096 \
  -no-snapshot-save > /tmp/emulator.log 2>&1 &

# Wait for boot to complete
for i in $(seq 1 30); do
  if $ANDROID_HOME/platform-tools/adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
    echo "Boot complete"; break
  fi
  sleep 10
done
```

### Install and Launch App

```bash
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb shell am start -n com.wineselector.app/.MainActivity
```

### Checking Logs

```bash
# All app logs
$ANDROID_HOME/platform-tools/adb logcat -d | grep -iE "wineselector|LlamaContext"

# Check for crashes
$ANDROID_HOME/platform-tools/adb logcat -d | grep -iE "FATAL|SIGILL|has died"

# Check app is still running
$ANDROID_HOME/platform-tools/adb shell pidof com.wineselector.app
```

### Important Emulator Gotchas

- **KVM required** — Check with `ls /dev/kvm`. Without KVM the emulator is unusably slow
- **DISPLAY required for `-gpu auto`** — The emulator needs a display server. Use `DISPLAY=:1` (Xvfb or real display). Without it, GPU initialization fails
- **ARM translation on x86_64** — The emulator is x86_64 but the app's native `.so` files (llama.cpp) are arm64-v8a only. The Google APIs system image includes ARM translation that runs arm64 code on x86_64. This works but only with the basic `librnllama_v8.so` variant — optimized variants (fp16, dotprod, i8mm) cause `SIGILL` crashes because the translation layer doesn't support advanced ARM extensions. `LlamaContext.kt` handles this automatically by detecting x86_64 and loading the basic variant
- **Cold boot is slow** — First boot takes ~60-100 seconds. Subsequent boots with snapshot are ~10 seconds. Use `-no-boot-anim` to speed up
- **No `-gpu swiftshader_indirect`** — This software renderer is too slow for practical use; boot never completes. Always use `-gpu auto` with a display
- **AVD lives in `~/.android/avd/`** — The AVD named `wine_test` stores its disk image in `~/.android/avd/wine_test.avd/`. This is outside the project directory

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
4. **LLM extraction** — `WineNameExtractor` sends merged OCR text to the on-device LLM with a prompt asking it to extract wine names (one per line, including producer and vintage). Response is parsed and filtered (removes headers, all-digit lines, short lines)
5. **DB matching** — For each LLM-extracted wine name: clean via `cleanNameForMatching()`, extract vintage, look up via `XWinesDatabase.findMatchTiered()` (sorted-word index → fuzzy → strict word match). No match → skip. Display name comes from canonical DB wine name
6. **Scoring** — Base score from `inferScoreFromGrapes()` (60+ grape profiles), +2 harmonization bonus if X-Wines confirms food pairing (capped at 10). Wines without keyword-matching grapes get a modest rating-based score (3-5)
7. **Price extraction** — Prices found by matching wine name words back to OCR lines that contain prices
8. **Preference filtering** — Filters by max price, ignored grapes, and allowed wine types
9. **Ranking** — Sorted by score (desc) → X-Wines average rating (desc) → alphabetical display name (asc) for deterministic tiebreaking
10. **Result** — Top match displayed with name, price (if detected), pairing reasoning, runner-up, and X-Wines metadata when available

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

`XWinesDatabase` builds three HashMap indexes plus a word set after loading:

1. **Name word index** — Maps each significant word (length > 2) from wine names to a list of wines containing that word. During matching, only wines whose name words appear in the query are scored.
2. **Grape index** — Maps each grape variety name to the first wine having that grape. Used as a fallback when name matching fails.
3. **Sorted-word index** — Maps sorted significant words to wines for order-independent matching.
4. **All indexed words set** — Flat set of every unique indexed word for Levenshtein distance-1 fuzzy fallback (recovers OCR typos like "Cabermet" → "Cabernet").

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

171 unit tests across 5 test suites:

- `WinePairingEngineTest` (40 tests) — Tests `recommendWines(List<String>, ...)` with known bundled DB wines, grape inference scoring, harmonization bonus, preference filtering (type, ignored grapes, max price), edge cases (empty input, no DB matches, blank names, deduplication), vintage handling, match source classification, `cleanNameForMatching()`, `extractPrice()`, `lineHasPrice()`, `buildRecommendation()`
- `WineNameExtractorTest` (17 tests) — Prompt building (`buildPrompt()` includes OCR text, instructions), response parsing (`parseResponse()` handles newlines, numbered lists, bullet points, mixed formats), filtering (blank lines, all-digit lines, section headers, short lines)
- `XWinesDatabaseTest` (87 tests) — CSV parsing, indexed name/grape matching, food harmonization, slim dataset loading, performance benchmarks, Levenshtein fuzzy matching, abbreviated vintage extraction
- `TextNormalizerTest` (19 tests) — Accent stripping, OCR character substitution, Levenshtein distance, fuzzy word matching
- `XWinesDownloaderTest` (8 tests) — URL configuration, space requirements, dataset filename validation

Run with: `./gradlew testDebugUnitTest`
