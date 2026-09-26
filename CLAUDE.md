# Wine Selector - Claude Code Project Guide

## Project Overview

Android app (Kotlin 2 + Jetpack Compose) that scans **wine lists** and **bottle labels**. It reads text on-device with ML Kit, identifies wines against the X-Wines database, ranks them for the diner's food, explains the vintage (regional vintage guide + drinking window) and, optionally, looks up live retail prices online to show the menu markup. Scanning and matching work offline; only price lookups use the internet.

The project has two Gradle modules:

- **`:core`** — pure Kotlin/JVM. All the "brains": OCR layout analysis, menu and label parsing, database matching, vintage intelligence, pairing, pricing, history persistence. No Android dependencies, so it is fully unit-tested on a plain JVM.
- **`:app`** — Android UI and platform glue: CameraX, ML Kit, Compose screens, ViewModel, settings, dataset downloads.

## Build Environment

All build tools are self-contained in `.buildtools/` — no system-level installs required.
**Before building, always check if `.buildtools/` exists.** If it does not, run the setup steps below.

### Environment Variables (required for every build)

```bash
export JAVA_HOME="/home/jbriggs/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"
```

### First-Time Setup: Installing Build Tools

If `.buildtools/jdk-17.0.2` or `.buildtools/android-sdk` do not exist, install them:

```bash
# 1. Create directory
mkdir -p /home/jbriggs/src/wine-selector/.buildtools && cd /home/jbriggs/src/wine-selector/.buildtools

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
export JAVA_HOME="/home/jbriggs/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"

# 5. Accept licenses and install SDK components
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

After setup, verify with: `$JAVA_HOME/bin/java -version` (should show 17.0.2).

### Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew :core:test             # Core logic tests (plain JVM, fast)
./gradlew :app:testDebugUnitTest # App unit tests
./gradlew clean                  # Clean build outputs
./gradlew dependencies           # List all dependencies
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

**After every successful APK build, always tell the user:**
> "APK ready: `app/build/outputs/apk/debug/app-debug.apk`"

## Key Architecture Decisions

- **Core/app split** — Put new logic in `:core` whenever it doesn't need Android. The app converts ML Kit output into `OcrPage` (lines + `Box` bounding boxes) and hands it to `WineAnalyzer`.
- **Column-aware layout** — `LayoutAnalyzer` finds a vertical gutter and only treats it as a real column split if *both* sides carry priced wine text. This separates two-column menus without splitting the name/price columns of a single list. Full-width lines (section headers) split the page into bands so reading order stays correct.
- **Menu parsing** — `MenuParser` classifies rows (format header, section header, tasting note, priced row, continuation) and assembles `MenuEntry`s: name, geography detail, vintage, glass/bottle prices, currency, half-bottle flag, section style, tasting note and source OCR line indices (for highlights).
- **Price parsing** — `PriceParser` reads the trailing price zone of a row plus any currency-tagged amounts. It rejects vintages and founding years (1700..now+1), volumes, ABV, bin/cuvée numbers ("Bin 389") and lone single digits.
- **Strict menu matching with distinctive evidence** — Many X-Wines names are generic ("Chenin Blanc", "Brunello di Montalcino", "Special Selection Cabernet Sauvignon") and differ only by winery. `XWinesDatabase.hasDistinctiveEvidence()` requires a non-generic name word on the menu, or — for all-generic names — the winery. Generic words = grape, region and country words from the data plus label vocabulary. This removed many false identifications; a wrong wine is worse than "Not in database".
- **Winery-aware candidate search** — `XWinesDatabase.searchCandidates()` scores name coverage, winery coverage, precision, grape and region hits, with OCR variants and Levenshtein-1 fuzzy words. Menus prefer a candidate whose winery is printed, then the tiered sorted-word/strict match. Bottles take the best candidate (boosted by words in the largest-type lines) and offer the rest as "Not this wine?" alternatives.
- **Style sanity check** — A match is rejected when the scanned colour contradicts the database colour (a "Rioja Blanco" is not the red Rioja). Only red/white/rosé contradictions count; menus file sparkling and sweet wines loosely.
- **Unidentified wines still rank** — Wines not in the database are scored from grape/region keywords (`GrapeProfiles`) or their section style, and shown with a "Not in database" badge. Junk rows (no price, no vintage, no match) are dropped.
- **Vintage intelligence** — `VintageParser` handles 4-digit, abbreviated ('15, ’08) and NV markers and ignores founding years, future years, prices and volumes. `VintageMatcher` reports exact / nearest / unlisted against the database's vintage list. `VintageGuide` is an approximate 1–5★ chart for 15 classic regions; `DrinkingWindowEstimator` derives a window from style, grapes, body, region and vintage quality.
- **Pairing** — `PairingEngine`: grape-profile base score (lead grape 60%, best grape 40%), style fallback table, +2 X-Wines harmonize bonus (capped at 10), ±1 for body/acidity. Never use flat scores for harmonize matches.
- **Online prices** — `PriceService` + `SerpApiShoppingProvider` (Google Shopping via SerpApi; the user pastes their own free key in Settings). Searches "<wine> <vintage>" first and keeps listings for that vintage or with no vintage; falls back to any vintage and says so. `OfferFilter` drops other bottle sizes, multipacks, accessories and outliers. Results are cached on disk for 24h (`JsonFilePriceCache`). `MarkupAnalyzer` compares menu vs retail. Keyless compare links (Wine-Searcher, Vivino, Google Shopping) are always shown.
- **Single Activity + Navigation Compose** — Routes: `home`, `camera/{mode}`, `scan`, `wine/{key}`, `history`, `settings`. The old BOM/animation conflict that forced state-based navigation is gone with Kotlin 2 + BOM 2025.01.
- **Single ViewModel** — `WineSelectorViewModel` holds settings, database state, the current scan (`ScanUiState`), per-wine price state and history. Changing food or preferences re-ranks the current menu instantly without re-running OCR.
- **OCR** — `OcrEngine` decodes the photo at ≤2800px with EXIF rotation applied (so boxes match the displayed image), runs ML Kit, and retries on a grayscale, contrast-boosted copy when little text is found.
- **Camera** — CameraX bound to the Activity lifecycle, `CAPTURE_MODE_MAXIMIZE_QUALITY`, tap-to-focus, pinch zoom, torch, file-based capture. Photo Picker import for existing photos.

## Important Gotchas

- **DB display names** — `displayName` is ALWAYS `entry.wineName` (the canonical DB name) when a wine is identified. Never show OCR text as the name of an identified wine.
- **Stop words** — "noir" and "blanc" are NOT stop words in `XWinesDatabase` (they distinguish Pinot Noir from Pinot Grigio). They *are* in the generic-word set used only for the distinctive-evidence check.
- **Harmonization bonus** — Always compute a grape/style base score first, then add +2 (capped at 10).
- **Google Play target API level** — Play requires new uploads to target API 36 from 31 Aug 2026. Bumping `targetSdk` also requires a matching AGP (AGP 8.10+ for `compileSdk 36`).
- **16 KB page size (Play requirement)** — CameraX 1.5.3 and ML Kit 16.0.1 ship 16 KB-aligned `.so` files; older versions are rejected. CI runs `zipalign -c -P 16`. After any camera/ML Kit bump also check `readelf -lW <lib>.so | grep LOAD` shows align `0x4000` for 64-bit ABIs.
- **CameraX lifecycle** — Bind to the Activity (`context.findActivity()`), not a `NavBackStackEntry`.
- **Camera capture** — Use file-based `OnImageSavedCallback`, NOT `OnImageCapturedCallback` (YUV output can't be decoded by `BitmapFactory`).
- **Image display** — Coil `AsyncImage` with a `File`, never an in-memory `ByteArray` (OOM on high-res photos).
- **Release signing is optional** — `app/build.gradle.kts` only configures the release keystore when `~/documents/sync/personal/keystore/wine-selector.release.pswd` exists, so CI and other machines can build.
- **Price API key** — Never hard-code a SerpApi key. It lives only in the user's SharedPreferences.

## Code Conventions

- Kotlin with Jetpack Compose (no XML layouts), Material 3, colours in `ui/theme/Color.kt` (including `styleColor()` per wine style)
- `@file:OptIn(ExperimentalMaterial3Api::class)` on screens using experimental M3 APIs
- State via `StateFlow` in the ViewModel, collected with `collectAsStateWithLifecycle()`
- Dependencies are declared in `gradle/libs.versions.toml`

## File Layout

```
core/src/main/kotlin/com/wineselector/core/
├── analysis/   WineAnalyzer (menu + bottle pipelines), AnalyzedWine / MenuAnalysis / BottleAnalysis models
├── db/         XWinesDatabase (indexes, tiered + candidate matching, distinctive evidence), DatasetSize
├── history/    ScanHistoryStore (JSON)
├── model/      FoodCategory, WineStyle, WinePreferences
├── pairing/    GrapeProfiles (80+ keyword profiles), PairingEngine
├── pricing/    PriceService, SerpApiShoppingProvider, OfferFilter, MarkupAnalyzer, PriceLinks, JsonFilePriceCache
├── scan/       OcrPage/Box, LayoutAnalyzer, MenuParser, PriceParser, LabelParser, GeoLexicon
├── text/       TextNormalizer (accents, OCR substitutions, Levenshtein)
└── vintage/    VintageParser, VintageMatcher, VintageGuide, DrinkingWindowEstimator

app/src/main/java/com/wineselector/app/
├── MainActivity.kt / WineSelectorApplication.kt (manual DI container)
├── data/       OcrEngine (ML Kit), SettingsStore, XWinesDownloader
├── viewmodel/  WineSelectorViewModel
└── ui/
    ├── WineSelectorApp.kt   NavHost
    ├── screens/  Home, Camera, Scan (progress / menu results / failure), WineDetail, History, Settings
    ├── components/ Components (score ring, stars, pills, food chips, drinking window bar), HighlightedPhoto, Format
    └── theme/    Color, Theme (light + dark), Type (serif display)
```

## Common Tasks

### Adding a food category
Add it to `core/.../model/FoodCategory.kt`, give it scores in the relevant `GrapeProfiles` entries and in `PairingEngine.STYLE_FALLBACK`, and map any X-Wines harmonize labels in `XWinesDatabase.harmonizeToCategory`.

### Adding a grape / region profile
Add a `put("keyword", GrapeProfile(scores, description, style))` in `GrapeProfiles`. Keywords are lowercase and matched with word boundaries against menu text and database grape lists.

### Adding a vintage region
Add a `region(...)` to `VintageGuide.REGIONS` with appellation keywords and 1–5★ ratings for years with a clear reputation. Unknown years must stay absent rather than guessed.

### Adding a price source
Implement `PriceProvider` in `core/pricing` and pass it to `PriceService` in the ViewModel.

## Processing Pipeline

1. **Capture / import** — CameraX saves a JPEG (or the Photo Picker copies one) into `filesDir/scans/`.
2. **OCR** — `OcrEngine` → `OcrPage` (lines with boxes, image size).
3. **Layout** — `LayoutAnalyzer.rows()` → rows in reading order, columns separated.
4. **Parse** — Menu: `MenuParser` → `MenuEntry` list. Bottle: `LabelParser` → prominent lines, meaningful text, vintage.
5. **Identify** — `WineAnalyzer` matches against `XWinesDatabase` with the distinctive-evidence and style checks.
6. **Annotate** — style, grapes, region, `VintageMatch`, `VintageRating`, `DrinkingWindow`, pairing score for the chosen food.
7. **Filter & rank** — preferences (max price, styles, avoided grapes); rank by pairing score → rating → vintage stars → name (or by rating when no food is chosen). Picks: best pairing, best value, top rated.
8. **Prices** — auto-fetched for the top 3 (or the bottle) when a key is set; otherwise on the detail screen.
9. **History** — top wines saved to `filesDir/history/history.json` (max 50; evicted photos deleted).

## X-Wines Dataset

Source: [github.com/rogerioxavier/X-Wines](https://github.com/rogerioxavier/X-Wines)

| Option | Wines | Ratings | Download | Disk space |
|--------|-------|---------|----------|------------|
| **Starter** (bundled) | 100 | 1K | None | None |
| **Slim** | 1,007 | 150K | ~3 MB zip | 300 MB free |
| **Full** | ~100K | 21M | ~300 MB zip | 1 GB free |

Download URLs are in `DatasetSize` (`core/.../db/Dataset.kt`). The app starts on the bundled set instantly, then hot-swaps in a cached download if one exists. Users download or remove datasets from Settings; downloads are extracted to `filesDir/xwines_cache/<size>/` and a binary cache (`xwines.bin`) speeds up later loads.

## Tests

`:core` has ~190 JVM unit tests (run `./gradlew :core:test`):

- `MenuParserTest` — real menu OCR fixtures (`core/src/test/resources/menus/*.txt`): continuations, priced region lines, section headers, half bottles, notes, abbreviated vintages
- `PriceParserTest`, `LayoutAnalyzerTest` — price zones and two-column detection
- `WineAnalyzerTest` — identification, false-match regressions, vintage matching, ranking, filters, bottle labels and alternatives
- `VintageTest` — parsing, matching, regional ratings, drinking windows
- `PairingEngineTest`, `PricingTest` (fake provider + MockWebServer for SerpApi), `ScanHistoryTest`
- `XWinesDatabaseTest`, `TextNormalizerTest`, `DatasetSizeTest` — database loading, indexes, fuzzy matching

CI (`.github/workflows/android.yml`) runs the core and app tests, builds the debug APK, checks 16 KB alignment and uploads the APK as an artifact.

## Emulator Testing

The emulator runs locally in `.buildtools/android-sdk/` with KVM acceleration. Everything is self-contained — no system-wide installs needed.

### First-Time Emulator Setup

If the emulator or system image aren't installed yet:

```bash
export JAVA_HOME="/home/jbriggs/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"

# Install emulator and system image
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "emulator" "system-images;android-36;google_apis;x86_64"

# Create AVD
echo "no" | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  -n wine_test36 -k "system-images;android-36;google_apis;x86_64" \
  -d pixel_6
```

### Starting the Emulator

```bash
export ANDROID_HOME="/home/jbriggs/src/wine-selector/.buildtools/android-sdk"

# Launch emulator (requires DISPLAY for GPU; use :1 or :0 depending on environment)
DISPLAY=:1 nohup $ANDROID_HOME/emulator/emulator \
  -avd wine_test36 -no-audio -gpu auto -no-boot-anim -memory 4096 \
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
$ANDROID_HOME/platform-tools/adb logcat -d | grep -iE "wineselector"

# Check for crashes
$ANDROID_HOME/platform-tools/adb logcat -d | grep -iE "FATAL|SIGILL|has died"

# Check app is still running
$ANDROID_HOME/platform-tools/adb shell pidof com.wineselector.app
```

### Important Emulator Gotchas

- **KVM required** — Check with `ls /dev/kvm`. Without KVM the emulator is unusably slow
- **DISPLAY required for `-gpu auto`** — The emulator needs a display server. Use `DISPLAY=:1` (Xvfb or real display). Without it, GPU initialization fails
- **Cold boot is slow** — First boot takes ~60-100 seconds. Subsequent boots with snapshot are ~10 seconds. Use `-no-boot-anim` to speed up
- **No `-gpu swiftshader_indirect`** — This software renderer is too slow for practical use; boot never completes. Always use `-gpu auto` with a display
- **AVD lives in `~/.android/avd/`** — The AVD named `wine_test36` stores its disk image in `~/.android/avd/wine_test36.avd/`. This is outside the project directory

## Dependencies

Managed in `gradle/libs.versions.toml`. Key versions:

- Kotlin `2.1.0` with the Compose compiler Gradle plugin
- AGP `8.10.1`, Gradle `8.11.1`, compileSdk / targetSdk `36`, minSdk `26`
- Compose BOM `2025.01.00`, Navigation Compose `2.8.5`, Lifecycle `2.8.7`, Activity `1.9.3`
- CameraX `1.5.3`, ML Kit Text Recognition `16.0.1` (both 16 KB-aligned)
- Coil `2.7.0`, OkHttp `4.12.0`, kotlinx-serialization-json `1.7.3`, kotlinx-coroutines `1.9.0`

