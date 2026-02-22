package com.wineselector.app.data

data class WineAlternative(
    val wineName: String,
    val price: String?,
    val score: Int,
    val reason: String,
    val xWinesMatch: XWineEntry?,
    val vintageMatch: VintageMatch = VintageMatch.NOT_CHECKED,
    val vintageNote: String? = null,
    val matchSource: WinePairingEngine.MatchSource? = null
)

data class WineRecommendation(
    val wineName: String,
    val price: String?,
    val reasoning: String,
    val runnerUp: String?,
    val rawResponse: String,
    val xWinesMatch: XWineEntry? = null,
    val vintageMatch: VintageMatch = VintageMatch.NOT_CHECKED,
    val vintageNote: String? = null,
    val alternatives: List<WineAlternative> = emptyList(),
    val matchSource: WinePairingEngine.MatchSource? = null
)
