package com.wineselector.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.wineselector.core.model.WineStyle

// Brand palette
val Bordeaux = Color(0xFF7B1E34)
val BordeauxDark = Color(0xFF4A0F1F)
val BordeauxLight = Color(0xFFB5475F)
val Claret = Color(0xFFFFD9DF)
val Gold = Color(0xFFC9A24B)
val GoldLight = Color(0xFFF6E7C1)
val Cork = Color(0xFF8A6A4F)
val Parchment = Color(0xFFFFF8F3)
val ParchmentDim = Color(0xFFF4EAE3)
val Ink = Color(0xFF231A1C)
val InkMuted = Color(0xFF5E4F52)
val Night = Color(0xFF161113)
val NightSurface = Color(0xFF221A1D)
val NightHigh = Color(0xFF2E2427)

// Wine style colours (glass swatches)
val StyleRed = Color(0xFF8E1B2E)
val StyleWhite = Color(0xFFE3C565)
val StyleRose = Color(0xFFF08FA5)
val StyleSparkling = Color(0xFFD9C27A)
val StyleDessert = Color(0xFFD08A1E)
val StyleFortified = Color(0xFF5A1A2A)
val StyleUnknown = Color(0xFF9E8F92)

fun styleColor(style: WineStyle?): Color = when (style) {
    WineStyle.RED -> StyleRed
    WineStyle.WHITE -> StyleWhite
    WineStyle.ROSE -> StyleRose
    WineStyle.SPARKLING -> StyleSparkling
    WineStyle.DESSERT -> StyleDessert
    WineStyle.FORTIFIED -> StyleFortified
    null -> StyleUnknown
}

// Ranking medals
val Medal1 = Color(0xFFD4AF37)
val Medal2 = Color(0xFFB8BCC2)
val Medal3 = Color(0xFFCD7F32)

// Score scale
val ScoreGreat = Color(0xFF2E7D4F)
val ScoreGood = Color(0xFF7A9A2E)
val ScoreOk = Color(0xFFD08A1E)
val ScorePoor = Color(0xFFB3261E)

fun scoreColor(score: Int): Color = when {
    score >= 9 -> ScoreGreat
    score >= 7 -> ScoreGood
    score >= 5 -> ScoreOk
    else -> ScorePoor
}
