package com.wineselector.app

import android.app.Application
import com.wineselector.app.data.OcrEngine
import com.wineselector.app.data.SettingsStore
import com.wineselector.app.data.XWinesDownloader
import com.wineselector.core.history.ScanHistoryStore
import com.wineselector.core.pricing.JsonFilePriceCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Process-wide singletons (manual dependency injection). */
class WineSelectorApplication : Application() {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val settingsStore by lazy { SettingsStore(this) }
    val ocrEngine by lazy { OcrEngine() }
    val downloader by lazy { XWinesDownloader(this) }
    val historyStore by lazy { ScanHistoryStore(File(filesDir, "history/history.json")) }
    val priceCache by lazy { JsonFilePriceCache(File(filesDir, "prices/prices.json")) }
    val scansDir: File get() = File(filesDir, "scans").apply { mkdirs() }
}
