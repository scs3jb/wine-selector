# Wine Selector

An Android app that reads **wine lists** and **bottle labels** and tells you what to drink. Point your camera at a menu and every wine is ranked for your food, with its vintage explained and, if you like, what it costs in the shops. Point it at a bottle and it identifies the wine, rates the vintage, estimates when to drink it and finds retail prices online.

Text recognition and wine matching run entirely on your phone. Only the optional live price lookup uses the internet.

## Features

- **Scan a wine list** — Every wine on the menu is read, matched against the X-Wines database and ranked for what you're eating. Two-column menus, glass/bottle prices, half bottles, section headers and tasting notes are all understood.
- **Scan a bottle** — The label's producer and wine name are picked out from the largest type. If the match is uncertain you get a "Not this wine?" list of alternatives.
- **Smart vintages** — Reads 2019, '19 and NV. Tells you whether the vintage is recorded for that wine or which is closest, rates the year for 15 classic regions, and estimates a drinking window ("too young", "at its peak", "drink up").
- **Live prices** — Retail prices from Google Shopping through SerpApi, vintage first. Menu prices are compared with retail to show the markup. Wine-Searcher, Vivino and Google Shopping links are always available, no key needed.
- **Picks** — Best pairing, best value and top rated, plus filters by style and sorting by price, rating or vintage.
- **Instant re-ranking** — Change the food and the list re-orders without rescanning.
- **History** — Recent scans with photos, ready to reopen.
- **Import from photos** — Scan a picture you already took.

## Using the app

1. Choose what you're eating on the home screen, or "Just drinking".
2. Tap **Scan a wine list** or **Scan a bottle**. Tap to focus, pinch to zoom, use the torch in dim restaurants.
3. Browse the ranked list. Tap any wine for vintage, pairing and price details.

### Getting live prices

Create a free account at [serpapi.com](https://serpapi.com), copy your API key and paste it in **Settings → Live prices**. The app searches for the exact vintage first and tells you when prices are for other vintages.

### Recognising more wines

The app ships with a 100-wine starter set. In **Settings → Wine database** you can download the Slim set (about 1,000 wines, 3 MB) or the Full set (about 100,000 wines, 300 MB). The download happens in the background.

## Building

Requirements: JDK 17 and an Android SDK with platform 36. See `CLAUDE.md` for a fully self-contained setup under `.buildtools/`.

```bash
./gradlew :core:test        # Fast JVM tests for all scanning and matching logic
./gradlew assembleDebug     # Debug APK
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Every push also builds it in GitHub Actions and attaches it as the `app-debug` artifact.

Release builds are signed only when the maintainer's keystore is present; otherwise `assembleRelease` produces an unsigned APK.

## Project structure

- **`core/`** — Pure Kotlin module with the scanning and matching logic: OCR layout analysis, menu and label parsing, database matching, vintage guide, pairing engine, price lookup and history. Fully unit-tested on the JVM.
- **`app/`** — Android app: CameraX camera, ML Kit OCR, Jetpack Compose UI, ViewModel, settings and dataset downloads.

## Tech stack

| Area | Library |
|------|---------|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose (BOM 2025.01), Material 3, Navigation Compose |
| Camera | CameraX 1.5.3 |
| OCR | ML Kit Text Recognition 16.0.1 (on-device) |
| Images | Coil 2.7 |
| Networking | OkHttp 4.12, kotlinx-serialization-json |
| Wine data | [X-Wines](https://github.com/rogerioxavier/X-Wines) |

## Permissions

- **CAMERA** — to photograph wine lists and labels.
- **INTERNET** — for optional price lookups and wine database downloads.

## Troubleshooting

- **"Couldn't read any text"** — Move closer, hold the phone parallel to the page, avoid glare and tap to focus.
- **Wines show "Not in database"** — They are still ranked from their grapes and style. Download the Full database to identify more by name.
- **Prices say "Add a free SerpApi key"** — Add a key in Settings, or use the compare links on the wine page.
