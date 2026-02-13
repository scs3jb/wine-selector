package com.wineselector.app.data

data class WineRecommendation(
    val wineName: String,
    val price: String?,
    val reasoning: String,
    val runnerUp: String?,
    val rawResponse: String
)
