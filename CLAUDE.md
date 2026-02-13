# Wine Selector - Claude Code Project Guide

## Project Overview

Android app (Kotlin + Jetpack Compose) that photographs wine lists and uses Google Gemini's free-tier vision API to recommend wine pairings based on food selection.

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

- **No backend server** — Users provide their own Google AI Studio API key (free, no billing), stored locally via DataStore
- **Google Gemini 2.0 Flash** — Free-tier vision model via REST API, no paid subscription needed
- **Single Activity** — `MainActivity` hosts a Compose NavHost with 4 routes: `home`, `camera`, `result`, `settings`
- **Single ViewModel** — `WineSelectorViewModel` holds all app state (food selection, captured image, API result)
- **Direct API calls** — OkHttp to `generativelanguage.googleapis.com` with base64-encoded JPEG, no Retrofit or generated client

## Code Conventions

- Kotlin with Jetpack Compose (no XML layouts)
- Material 3 theming with wine-themed colors defined in `ui/theme/Color.kt`
- `@OptIn` annotations used for experimental Material 3 and Layout APIs
- State management via `StateFlow` in ViewModel, collected with `collectAsState()` in Composables
- Navigation routes are plain strings: `"home"`, `"camera"`, `"result"`, `"settings"`

## File Layout

```
app/src/main/java/com/wineselector/app/
├── MainActivity.kt              # Activity + camera permission
├── WineSelectorApp.kt           # NavHost + theme
├── data/
│   ├── ClaudeApiService.kt      # Gemini API client (file kept for git history)
│   ├── FoodCategory.kt          # Add new food categories here
│   └── PreferencesRepository.kt # Add new settings keys here
├── viewmodel/
│   └── WineSelectorViewModel.kt # Central state management
└── ui/
    ├── screens/                  # Full-screen composables
    ├── components/               # Reusable UI pieces
    └── theme/                    # Colors, typography, theme
```

## Common Tasks

### Adding a new food category

Edit `data/FoodCategory.kt` and add an entry to the enum:

```kotlin
STEAK("Steak", "\uD83E\uDD69"),
```

The food picker grid updates automatically.

### Changing the AI model

Edit `data/ClaudeApiService.kt`, change the model in the URL:

```kotlin
val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
```

Available free models: `gemini-2.0-flash`, `gemini-1.5-flash`, `gemini-1.5-pro`

### Modifying the AI prompt

Edit the `prompt` variable in `GeminiApiService.analyzeWineList()`. The response parser expects `WINE:`, `PRICE:`, `WHY:`, and `RUNNER_UP:` fields.

### Adding a new screen

1. Create a composable in `ui/screens/`
2. Add a route in `WineSelectorApp.kt` inside the `NavHost` block
3. Add navigation calls from other screens

### Adding a new setting

1. Add a key in `PreferencesRepository.kt`
2. Add a flow + setter method
3. Expose it in `WineSelectorViewModel.kt`
4. Add UI in `SettingsScreen.kt`

## Emulator Testing

This environment lacks KVM hardware acceleration, so the Android emulator cannot run here. To test:

- Transfer the APK to a physical device, or
- Build locally on a machine with KVM support and use the emulator

An AVD named `wine_test` (Pixel 6, Android 34, Google APIs x86_64) is already configured if KVM becomes available.

## Dependencies

Managed in `app/build.gradle.kts`. Key dependency versions:

- Compose BOM: `2024.01.00`
- CameraX: `1.3.1`
- OkHttp: `4.12.0`
- Navigation: `2.7.6`
- Kotlin: `1.9.22`
- Compose Compiler: `1.5.8`
- AGP: `8.2.2`
