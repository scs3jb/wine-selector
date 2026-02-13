# Wine Selector

An Android app that recommends wines from a photographed wine list based on what you're eating. Point your camera at any wine list, select your food, and get a pairing recommendation instantly.

**Works fully offline** — no API key, no cloud service, no internet required. Uses on-device text recognition and a built-in wine pairing knowledge base.

## How It Works

1. **Pick your food** — Choose from 12 categories: Beef, Pork, Chicken, Pasta, Fish, Seafood, Lamb, Vegetarian, Cheese, Dessert, Sushi, Pizza
2. **Scan the wine list** — Take a photo of any wine list using the in-app camera
3. **Get a recommendation** — The app reads the wine list, matches wines against a knowledge base of 60+ grape varieties and regions, and recommends the best pairing with an explanation

## Screenshots

The app has three screens:

- **Home** — Food category picker with emoji icons and a "Scan Wine List" button
- **Camera** — Full-screen CameraX viewfinder with a capture button (no confirmation step — instant capture)
- **Result** — Displays the recommendation with wine name, price (if detected), pairing explanation, and runner-up

## Setup

### Prerequisites

- An Android device running Android 8.0 (API 26) or higher
- A camera on the device

That's it — no API key or account needed.

### Install the APK

The debug APK is located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK file to your device and install it directly.

### First Launch

1. Open **Wine Selector**
2. Select a food category
3. Tap **Scan Wine List** and grant camera permission when prompted
4. Photograph a wine list
5. View the recommendation instantly

## Building from Source

### Build Environment

The project includes a self-contained build environment under `.buildtools/`:

- **JDK 17** — OpenJDK 17.0.2 at `.buildtools/jdk-17.0.2/`
- **Android SDK** — Platform 34, Build Tools 34.0.0 at `.buildtools/android-sdk/`
- **Gradle 8.5** — Via the included Gradle wrapper

### Build Commands

Set up the environment and build:

```bash
export JAVA_HOME="/src/wine-selector/.buildtools/jdk-17.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/src/wine-selector/.buildtools/android-sdk"

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

The output APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
wine-selector/
├── app/
│   ├── build.gradle.kts              # App dependencies and build config
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions: CAMERA, INTERNET
│       ├── java/com/wineselector/app/
│       │   ├── MainActivity.kt       # Entry point
│       │   ├── WineSelectorApp.kt    # Screen switching and theme
│       │   ├── data/
│       │   │   ├── FoodCategory.kt          # 12 food categories with emojis
│       │   │   ├── WineRecommendation.kt    # Result data class
│       │   │   ├── TextRecognitionService.kt # ML Kit on-device OCR
│       │   │   └── WinePairingEngine.kt     # 60+ wine profiles with food scores
│       │   ├── viewmodel/
│       │   │   └── WineSelectorViewModel.kt # App state and business logic
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt        # Food picker + scan button
│       │       │   ├── CameraScreen.kt      # CameraX in-app capture
│       │       │   └── ResultScreen.kt      # Recommendation display
│       │       ├── components/
│       │       │   ├── FoodCategoryPicker.kt       # Food chip grid
│       │       │   └── WineRecommendationCard.kt   # Recommendation card
│       │       └── theme/
│       │           ├── Theme.kt     # Material 3 theme (light/dark)
│       │           ├── Color.kt     # Wine-themed color palette
│       │           └── Type.kt      # Serif headlines typography
│       └── res/
│           ├── drawable/            # Launcher icon (wine glass vector)
│           ├── mipmap-hdpi/         # Adaptive icon definition
│           ├── values/              # Strings, themes
│           └── xml/                 # FileProvider paths
├── build.gradle.kts                 # Root Gradle config
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Build properties
├── local.properties                 # Local SDK path
└── .buildtools/                     # Self-contained JDK + Android SDK
```

## Architecture

- **Pattern**: MVVM with Jetpack Compose
- **State Management**: Kotlin StateFlow in ViewModel
- **Screen Navigation**: State-based switching via `rememberSaveable` (no Navigation Compose)
- **Camera**: CameraX with ImageCapture for instant JPEG capture
- **OCR**: Google ML Kit Text Recognition (on-device, no API key)
- **Wine Matching**: Built-in rules engine with 60+ grape/region/style profiles
- **Image Display**: Coil AsyncImage from file path
- **Theme**: Material 3 with wine-inspired colors (deep reds, golds, cream)

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.02.00 | UI framework |
| Material 3 | (BOM) | Design system |
| CameraX | 1.3.1 | In-app camera |
| ML Kit Text Recognition | 16.0.0 | On-device OCR |
| Coil | 2.5.0 | Image loading |
| Lifecycle ViewModel | 2.7.0 | MVVM support |

## Wine Knowledge Base

The app includes a built-in knowledge base covering:

**Red wines**: Cabernet Sauvignon, Merlot, Pinot Noir, Malbec, Syrah/Shiraz, Zinfandel, Tempranillo, Sangiovese, Nebbiolo, Grenache, Barbera, Primitivo

**White wines**: Chardonnay, Sauvignon Blanc, Riesling, Pinot Grigio/Gris, Viognier, Gewurztraminer, Gruner Veltliner, Albarino, Muscadet, Chenin Blanc, Semillon

**Sparkling**: Champagne, Prosecco, Cava

**Dessert**: Moscato, Port, Sauternes, Ice Wine

**Regions/Styles**: Bordeaux, Burgundy, Chianti, Barolo, Barbaresco, Rioja, Cotes du Rhone, Chateauneuf-du-Pape, Sancerre, Chablis, Valpolicella, Amarone, Beaujolais, Montepulciano

**Rosé**: Generic rosé detection

Each entry has food pairing scores for all 12 food categories based on established sommelier pairing principles.

## Permissions

- **CAMERA** — Required to photograph wine lists
- **INTERNET** — Used by ML Kit for initial model download (first use only; works offline after)

## Troubleshooting

**Camera not working** — Make sure you granted camera permission when prompted. You can also enable it in Android Settings > Apps > Wine Selector > Permissions.

**"Could not read any text from the photo"** — The photo may be blurry or poorly lit. Hold the camera steady, ensure good lighting, and make sure the wine list text is in focus.

**"No match found"** — The app couldn't identify any known wine varieties in the text. This can happen with very unusual wines, heavily stylized fonts, or non-Latin scripts. Try a clearer photo.

**Blurry results** — Hold the camera steady and make sure the wine list text is in focus before tapping the capture button. Good lighting helps significantly with OCR accuracy.
