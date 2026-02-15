package com.wineselector.app.data

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

enum class DatasetSize(
    val label: String,
    val description: String,
    val url: String,
    val winesFilename: String,
    val ratingsFilename: String,
    val requiredSpaceMb: Long,
    val minWinesBytes: Long,
    val minRatingsBytes: Long
) {
    SLIM(
        label = "Slim (1K wines)",
        description = "1,007 wines with 150K ratings (~3 MB download)",
        url = "https://repo.buildanddeploy.com/wines/XWines_Slim_1K_wines_150K_ratings.zip",
        winesFilename = "XWines_Slim_1K_wines.csv",
        ratingsFilename = "XWines_Slim_150K_ratings.csv",
        requiredSpaceMb = 300,
        minWinesBytes = 50_000L,
        minRatingsBytes = 100_000L
    ),
    FULL(
        label = "Full (100K wines)",
        description = "100K wines with 21M ratings (~300 MB download)",
        url = "https://repo.buildanddeploy.com/wines/All-XWines_Full_100K_wines_21M_ratings.zip",
        winesFilename = "XWines_Full_100K_wines.csv",
        ratingsFilename = "XWines_Full_21M_ratings.csv",
        requiredSpaceMb = 1024,
        minWinesBytes = 1_000_000L,
        minRatingsBytes = 10_000_000L
    );
}

sealed class DownloadStatus {
    object NotStarted : DownloadStatus()
    data class Downloading(val progressPercent: Int) : DownloadStatus()
    object Extracting : DownloadStatus()
    object Complete : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
    data class InsufficientSpace(val requiredMb: Long, val availableMb: Long) : DownloadStatus()
}

class XWinesDownloader(private val context: Context) {

    companion object {
        private const val CACHE_DIR_NAME = "xwines_cache"
        private const val PREFS_NAME = "xwines_prefs"
        private const val KEY_DATASET_CHOICE = "dataset_choice"
        private const val WINES_CACHE = "wines.csv"
        private const val RATINGS_CACHE = "ratings.csv"

        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 300_000
    }

    private val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun datasetCacheDir(dataset: DatasetSize): File =
        File(cacheDir, dataset.name.lowercase())

    private fun winesCacheFile(dataset: DatasetSize): File =
        File(datasetCacheDir(dataset), WINES_CACHE)

    private fun ratingsCacheFile(dataset: DatasetSize): File =
        File(datasetCacheDir(dataset), RATINGS_CACHE)

    fun getSavedChoice(): DatasetSize? {
        val name = prefs.getString(KEY_DATASET_CHOICE, null) ?: return null
        return try { DatasetSize.valueOf(name) } catch (_: Exception) { null }
    }

    fun saveChoice(choice: DatasetSize?) {
        prefs.edit().apply {
            if (choice != null) putString(KEY_DATASET_CHOICE, choice.name)
            else remove(KEY_DATASET_CHOICE)
        }.apply()
    }

    fun hasUserMadeChoice(): Boolean {
        return prefs.contains(KEY_DATASET_CHOICE)
    }

    fun saveSkipChoice() {
        prefs.edit().putString(KEY_DATASET_CHOICE, "BUNDLED_ONLY").apply()
    }

    fun isSkipped(): Boolean {
        return prefs.getString(KEY_DATASET_CHOICE, null) == "BUNDLED_ONLY"
    }

    fun isCached(dataset: DatasetSize): Boolean {
        val wines = winesCacheFile(dataset)
        val ratings = ratingsCacheFile(dataset)
        return wines.exists() &&
            ratings.exists() &&
            wines.length() > dataset.minWinesBytes &&
            ratings.length() > dataset.minRatingsBytes
    }

    fun getCachedFiles(): Pair<File, File>? {
        val choice = getSavedChoice() ?: return null
        return getCachedFiles(choice)
    }

    fun getCachedFiles(dataset: DatasetSize): Pair<File, File>? {
        return if (isCached(dataset)) Pair(winesCacheFile(dataset), ratingsCacheFile(dataset)) else null
    }

    fun getAvailableSpaceMb(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes / (1024 * 1024)
    }

    fun hasEnoughSpace(dataset: DatasetSize): Boolean {
        return getAvailableSpaceMb() >= dataset.requiredSpaceMb
    }

    suspend fun downloadDataset(
        dataset: DatasetSize,
        onProgress: (Int) -> Unit = {}
    ): Result<Pair<File, File>> = withContext(Dispatchers.IO) {
        try {
            val destDir = datasetCacheDir(dataset)
            destDir.mkdirs()

            val zipFile = File(destDir, "download.zip")
            try {
                downloadZip(dataset.url, zipFile, onProgress)
                extractZip(zipFile, dataset)
            } finally {
                zipFile.delete()
            }

            val wines = winesCacheFile(dataset)
            val ratings = ratingsCacheFile(dataset)

            if (!wines.exists() || wines.length() < dataset.minWinesBytes) {
                throw Exception("Wines CSV missing or too small after extraction")
            }
            if (!ratings.exists() || ratings.length() < dataset.minRatingsBytes) {
                throw Exception("Ratings CSV missing or too small after extraction")
            }

            saveChoice(dataset)
            Result.success(Pair(wines, ratings))
        } catch (e: Exception) {
            clearCache(dataset)
            Result.failure(e)
        }
    }

    fun clearCache(dataset: DatasetSize) {
        val dir = datasetCacheDir(dataset)
        dir.deleteRecursively()
    }

    fun clearAll() {
        cacheDir.deleteRecursively()
        prefs.edit().clear().apply()
    }

    private fun downloadZip(urlString: String, destination: File, onProgress: (Int) -> Unit) {
        val tempFile = File(destination.parentFile, "${destination.name}.tmp")

        var connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true

            var redirects = 0
            while (connection.responseCode in 301..302 && redirects < 5) {
                val redirectUrl = connection.getHeaderField("Location")
                connection.disconnect()
                connection = URL(redirectUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                redirects++
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            val totalBytes = connection.contentLength.toLong()
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            BufferedInputStream(connection.inputStream, 32768).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }

            if (!tempFile.renameTo(destination)) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }
        } finally {
            connection.disconnect()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun extractZip(zipFile: File, dataset: DatasetSize) {
        val wines = winesCacheFile(dataset)
        val ratings = ratingsCacheFile(dataset)
        ZipInputStream(BufferedInputStream(zipFile.inputStream(), 32768)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val destFile = when {
                    entry.name.contains(dataset.winesFilename, ignoreCase = true) -> wines
                    entry.name.contains(dataset.ratingsFilename, ignoreCase = true) -> ratings
                    entry.name.endsWith("wines.csv", ignoreCase = true) -> wines
                    entry.name.endsWith("ratings.csv", ignoreCase = true) -> ratings
                    else -> { zis.closeEntry(); entry = zis.nextEntry; continue }
                }

                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (zis.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
