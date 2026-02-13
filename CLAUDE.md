# Wine Selector - Claude Code Project Guide

## Project Overview

Android app (Kotlin + Jetpack Compose) that photographs wine lists and recommends wine pairings based on food selection. Uses on-device ML Kit text recognition + a built-in wine pairing rules engine. No API key, no internet required for core functionality.

## Build Environment

All build tools are self-contained in `.buildtools/` — no system-level installs required.

```bash
export JAVA_HOME="/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/src/wine-selector/.buildtools/android-sdk"
```

### Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew clean                  # Clean build outputs
./gradlew dependencies           # List all dependencies
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Key Architecture Decisions

- **Fully on-device** — No cloud API, no API key, no internet needed for wine analysis
- **ML Kit Text Recognition** — Google's on-device OCR extracts wine list text from photos (model bundled in APK)
- **Wine Pairing Rules Engine** — Knowledge base of 60+ grape varieties, regions, and styles with food pairing scores (1-10)
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
│   ├── WineRecommendation.kt    # Data class for recommendation results
│   ├── TextRecognitionService.kt # ML Kit on-device OCR wrapper
│   └── WinePairingEngine.kt     # Rules engine: 60+ grape/region profiles with food scores
├── viewmodel/
│   └── WineSelectorViewModel.kt # Central state: photo capture → OCR → pairing → result
└── ui/
    ├── screens/
    │   ├── HomeScreen.kt        # Food picker + scan button
    │   ├── CameraScreen.kt      # CameraX in-app capture (no confirm step)
    │   └── ResultScreen.kt      # Recommendation display with photo preview
    ├── components/
    │   ├── FoodCategoryPicker.kt       # FlowRow of FilterChips
    │   └── WineRecommendationCard.kt   # Recommendation card UI
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

Keywords are matched case-insensitively against OCR text. Use lowercase.

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
2. **OCR** — `TextRecognitionService` uses ML Kit to extract all text from the image
3. **Matching** — `WinePairingEngine.recommendWines()` scans each text line for known wine keywords
4. **Scoring** — Each matched wine is scored 1-10 for the selected food category
5. **Result** — Top match displayed with name, price (if detected), pairing reasoning, and runner-up
