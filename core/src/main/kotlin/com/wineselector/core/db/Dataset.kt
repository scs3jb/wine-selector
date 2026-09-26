package com.wineselector.core.db

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
