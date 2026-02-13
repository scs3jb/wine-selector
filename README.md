# Wine Selector

An Android app that recommends wines from a photographed wine list based on what you're eating. Point your camera at any wine list, select your food, and get an AI-powered pairing recommendation instantly.

Powered by Google Gemini's free-tier vision API — no paid API subscription required.

## How It Works

1. **Pick your food** — Choose from 12 categories: Beef, Pork, Chicken, Pasta, Fish, Seafood, Lamb, Vegetarian, Cheese, Dessert, Sushi, Pizza
2. **Scan the wine list** — Take a photo of any wine list using the built-in camera
3. **Get a recommendation** — Gemini's vision AI reads the wine list and recommends the best pairing, explaining why it's a good match and noting the price if visible

## Screenshots

The app has four screens:

- **Home** — Food category picker with emoji icons and a "Scan Wine List" button
- **Camera** — Full-screen CameraX viewfinder with a capture button
- **Result** — Displays the AI recommendation with wine name, price, pairing explanation, and runner-up
- **Settings** — Enter and manage your Google AI API key

## Setup

### Prerequisites

- A free [Google AI Studio API key](https://aistudio.google.com/apikey) (no billing required)
- An Android device running Android 8.0 (API 26) or higher
- A camera on the device

### Getting Your Free API Key

1. Go to [aistudio.google.com](https://aistudio.google.com/apikey)
2. Sign in with your Google account
3. Click "Create API key"
4. Copy the key (starts with `AIza...`)

No credit card or billing setup needed. The free tier includes generous usage limits.

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
2. Tap the gear icon (top right) to open **Settings**
3. Enter your Google AI API key and tap **Save**
4. Return to the home screen
5. Select a food category
6. Tap **Scan Wine List** and photograph a wine list
7. Wait a few seconds for the recommendation

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

### Clean Build

```bash
./gradlew clean assembleDebug
```

## Project Structure

```
wine-selector/
├── app/
│   ├── build.gradle.kts              # App dependencies and build config
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions: CAMERA, INTERNET
│       ├── java/com/wineselector/app/
│       │   ├── MainActivity.kt       # Entry point, camera permission request
│       │   ├── WineSelectorApp.kt    # Navigation graph and theme setup
│       │   ├── data/
│       │   │   ├── ClaudeApiService.kt      # Gemini vision API client
│       │   │   ├── FoodCategory.kt          # Food category enum with emojis
│       │   │   └── PreferencesRepository.kt # DataStore for API key storage
│       │   ├── viewmodel/
│       │   │   └── WineSelectorViewModel.kt # App state and business logic
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt        # Food picker + scan button
│       │       │   ├── CameraScreen.kt      # CameraX photo capture
│       │       │   ├── ResultScreen.kt      # AI recommendation display
│       │       │   └── SettingsScreen.kt    # API key management
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
│           └── xml/                 # Network security config (HTTPS only)
├── build.gradle.kts                 # Root Gradle config
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Build properties
├── local.properties                 # Local SDK path
└── .buildtools/                     # Self-contained JDK + Android SDK
```

## Architecture

- **Pattern**: MVVM with Jetpack Compose
- **State Management**: Kotlin StateFlow in ViewModel
- **Navigation**: Jetpack Navigation Compose (home → camera → result, home → settings)
- **Camera**: CameraX with ImageCapture for JPEG output
- **AI**: Google Gemini 2.0 Flash via REST API (free tier, no billing required)
- **Networking**: OkHttp3 direct HTTP calls to Google Generative Language API
- **Storage**: DataStore Preferences for the API key
- **Theme**: Material 3 with wine-inspired colors (deep reds, golds, cream)

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.01.00 | UI framework |
| Material 3 | (BOM) | Design system |
| Navigation Compose | 2.7.6 | Screen navigation |
| CameraX | 1.3.1 | Camera capture |
| OkHttp3 | 4.12.0 | HTTP client |
| DataStore | 1.0.0 | Local preferences |
| Coil | 2.5.0 | Image loading |
| Lifecycle ViewModel | 2.7.0 | MVVM support |

## API Details

The app sends wine list photos to Google's Gemini API (`gemini-2.0-flash`) with a structured prompt requesting:

- **WINE** — The recommended wine name from the list
- **PRICE** — Price if visible on the list
- **WHY** — 2-3 sentence pairing explanation
- **RUNNER_UP** — Second-best option

The image is sent as base64-encoded JPEG via the Generative Language REST API with inline image data.

### Free Tier Limits

Google AI Studio's free tier includes:
- 15 requests per minute
- 1,500 requests per day
- No credit card required

This is more than enough for personal wine list scanning.

## Permissions

- **CAMERA** — Required to photograph wine lists
- **INTERNET** — Required to call the Gemini API

No other permissions are requested. The API key is stored locally on the device only.

## Troubleshooting

**"Set your API key in Settings first"** — Tap the gear icon on the home screen and enter a valid Google AI API key from [aistudio.google.com](https://aistudio.google.com/apikey).

**Camera not working** — Make sure you granted camera permission when prompted. You can also enable it in Android Settings > Apps > Wine Selector > Permissions.

**"API error 400"** — The image may be too large or unreadable. Try taking a clearer photo with better lighting.

**"API error 403"** — Your API key may be invalid or restricted. Generate a new one from Google AI Studio.

**"API error 429"** — Rate limited. Wait a minute and try again (free tier allows 15 requests/minute).

**Blurry results** — Hold the camera steady and make sure the wine list text is in focus before tapping the capture button. Good lighting helps.
