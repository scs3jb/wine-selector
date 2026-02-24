package com.wineselector.app.data

data class WineAlternative(
    val wineName: String,
    val price: String?,
    val score: Int,
    val reason: String
)

data class WineRecommendation(
    val wineName: String,
    val price: String?,
    val reasoning: String,
    val runnerUp: String?,
    val rawResponse: String,
    val alternatives: List<WineAlternative> = emptyList()
)
