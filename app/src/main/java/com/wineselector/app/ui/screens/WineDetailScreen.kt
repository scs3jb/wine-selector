@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wineselector.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wineselector.app.ui.components.DrinkingWindowBar
import com.wineselector.app.ui.components.Pill
import com.wineselector.app.ui.components.RatingStars
import com.wineselector.app.ui.components.ScoreRing
import com.wineselector.app.ui.components.SectionCard
import com.wineselector.app.ui.components.drinkingStatusColor
import com.wineselector.app.ui.components.formatPrice
import com.wineselector.app.ui.components.menuPriceLabel
import com.wineselector.app.ui.components.subtitle
import com.wineselector.app.ui.components.vintageLabel
import com.wineselector.app.ui.components.vintageMatchText
import com.wineselector.app.ui.theme.Gold
import com.wineselector.app.ui.theme.scoreColor
import com.wineselector.app.ui.theme.styleColor
import com.wineselector.app.viewmodel.PriceUiState
import com.wineselector.core.analysis.AnalyzedWine
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.pairing.PairingScore
import com.wineselector.core.pricing.MarkupAnalyzer
import com.wineselector.core.pricing.PriceLinks
import com.wineselector.core.pricing.PriceResult
import com.wineselector.core.pricing.PriceSummary
import com.wineselector.core.vintage.VintageGuide
import java.io.File

/** Extra content shown when the detail screen is the result of a bottle scan. */
data class BottleContext(
    val imagePath: String,
    val alternatives: List<AnalyzedWine>,
    val onChooseAlternative: (String) -> Unit,
    val onRetake: () -> Unit
)

@Composable
fun WineDetailScreen(
    wine: AnalyzedWine,
    food: FoodCategory?,
    pairings: List<Pair<FoodCategory, PairingScore>>,
    price: PriceUiState?,
    hasPriceKey: Boolean,
    onFetchPrice: (force: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    bottle: BottleContext? = null
) {
    LaunchedEffect(wine.key, wine.entry?.wineId, hasPriceKey) { if (hasPriceKey) onFetchPrice(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Header(wine, bottle, onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (bottle != null) IdentificationCard(wine, bottle)
            FactsCard(wine)
            VintageCard(wine)
            PairingCard(wine, food, pairings)
            PriceCard(wine, price, hasPriceKey, onFetchPrice, onOpenSettings)
            if (wine.tastingNote != null) {
                SectionCard("From the menu", Icons.Filled.Restaurant) {
                    Text(wine.tastingNote!!, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(wine: AnalyzedWine, bottle: BottleContext?, onBack: () -> Unit) {
    val swatch = styleColor(wine.style)
    val light = wine.style == WineStyle.WHITE || wine.style == WineStyle.SPARKLING || wine.style == WineStyle.ROSE
    val onHeader = if (light) Color(0xFF231A1C) else Color.White
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(swatch.copy(alpha = 0.95f), swatch.copy(alpha = 0.75f))))
    ) {
        Column(Modifier.statusBarsPadding().padding(start = 8.dp, end = 20.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = onHeader) }
                Spacer(Modifier.weight(1f))
                if (bottle != null) {
                    IconButton(onClick = bottle.onRetake) { Icon(Icons.Filled.CameraAlt, "Scan another bottle", tint = onHeader) }
                }
            }
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (wine.style?.label ?: "Wine").uppercase() + (wine.country?.let { " · ${it.uppercase()}" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = onHeader.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(wine.displayName, style = MaterialTheme.typography.headlineMedium, color = onHeader)
                    val sub = wine.subtitle()
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = onHeader.copy(alpha = 0.85f))
                    wine.rating?.let { r ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingStars(r, size = 18.dp, color = if (light) Color(0xFF8A6A00) else Gold)
                            Spacer(Modifier.width(6.dp))
                            Text("%.1f community rating".format(r), style = MaterialTheme.typography.labelLarge, color = onHeader)
                        }
                    }
                }
                if (bottle != null) {
                    AsyncImage(
                        model = File(bottle.imagePath),
                        contentDescription = "Label photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 84.dp, height = 112.dp).clip(MaterialTheme.shapes.medium)
                    )
                } else if (wine.vintage.isKnown) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(wine.vintageLabel(), style = MaterialTheme.typography.displaySmall, color = onHeader)
                        Text("vintage", style = MaterialTheme.typography.labelSmall, color = onHeader.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentificationCard(wine: AnalyzedWine, bottle: BottleContext) {
    SectionCard("Label match", Icons.Filled.Verified) {
        val (label, color) = when {
            !wine.isIdentified -> "Not found in the wine database — details below are inferred from the label." to MaterialTheme.colorScheme.error
            wine.matchConfidence >= 0.8f -> "Confident match (${(wine.matchConfidence * 100).toInt()}%)" to Color(0xFF2E7D4F)
            else -> "Best guess (${(wine.matchConfidence * 100).toInt()}%) — check the alternatives below" to Color(0xFFD08A1E)
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
        if (wine.vintage.year != null || wine.vintage.nonVintage) {
            Text("Vintage read from label: ${wine.vintageLabel()}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (bottle.alternatives.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Not this wine?", style = MaterialTheme.typography.titleSmall)
            bottle.alternatives.forEach { alt ->
                Card(
                    onClick = { bottle.onChooseAlternative(alt.key) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(styleColor(alt.style)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alt.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(alt.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Choose")
                    }
                }
            }
        }
    }
}

@Composable
private fun FactsCard(wine: AnalyzedWine) {
    val e = wine.entry
    SectionCard("The wine", Icons.Filled.WineBar) {
        if (wine.grapes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                wine.grapes.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
            }
            Spacer(Modifier.height(12.dp))
        }
        val facts = listOfNotNull(
            wine.region?.let { "Region" to it },
            wine.country?.let { "Country" to it },
            e?.body?.takeIf { it.isNotBlank() }?.let { "Body" to it },
            e?.acidity?.takeIf { it.isNotBlank() }?.let { "Acidity" to it },
            e?.abv?.let { "Alcohol" to "%.1f%%".format(it) },
            wine.menuPriceLabel()?.let { "On the menu" to it }
        )
        facts.forEachIndexed { i, (k, v) ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(v, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (e == null) {
            Spacer(Modifier.height(8.dp))
            Text("This wine isn't in the database, so these details come from the scanned text.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VintageCard(wine: AnalyzedWine) {
    SectionCard("Vintage", Icons.Filled.CalendarMonth) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(wine.vintageLabel(), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            wine.vintageRating?.let { r ->
                Column {
                    Text("★".repeat(r.stars) + "☆".repeat(5 - r.stars), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleMedium)
                    Text("${r.verdict} in ${r.regionLabel}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        wine.vintageMatchText()?.let {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        wine.drinkingWindow?.let { w ->
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(drinkingStatusColor(w.status)))
                Spacer(Modifier.width(8.dp))
                Text(w.status.label, style = MaterialTheme.typography.titleSmall, color = drinkingStatusColor(w.status))
            }
            Spacer(Modifier.height(8.dp))
            DrinkingWindowBar(w)
            Text("Estimated drinking window ${w.fromYear}–${w.toYear}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val regionKey = wine.vintageRating?.regionKey
        if (regionKey != null) {
            val great = VintageGuide.greatYears(regionKey).take(6)
            if (great.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Standout ${VintageGuide.regionLabel(regionKey)} years: ${great.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (wine.vintage.year == null && !wine.vintage.nonVintage) {
            Text("No vintage was printed. Ask for it — it can matter as much as the producer.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text("Vintage ratings and windows are an approximate guide.", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun PairingCard(wine: AnalyzedWine, food: FoodCategory?, pairings: List<Pair<FoodCategory, PairingScore>>) {
    var showAll by remember { mutableStateOf(false) }
    SectionCard("Food pairing", Icons.Filled.Restaurant) {
        val selected = food?.let { f -> pairings.firstOrNull { it.first == f } }
        if (selected != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(selected.second.score, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("With ${selected.first.icon} ${selected.first.displayName.lowercase()}", style = MaterialTheme.typography.titleMedium)
                    Text(selected.second.headline, style = MaterialTheme.typography.bodySmall)
                }
            }
            selected.second.reasons.forEach {
                Text("•  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(if (selected != null) "How it suits other dishes" else "Best with", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val list = if (showAll) pairings else pairings.take(5)
        list.forEach { (f, s) ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${f.icon}  ${f.displayName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(130.dp))
                LinearProgressIndicator(
                    progress = { s.score / 10f },
                    color = scoreColor(s.score),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape)
                )
                Text("${s.score}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp).padding(start = 8.dp))
            }
        }
        if (pairings.size > 5) {
            TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Show fewer" else "Show all ${pairings.size}") }
        }
    }
}

@Composable
private fun PriceCard(
    wine: AnalyzedWine,
    price: PriceUiState?,
    hasKey: Boolean,
    onFetch: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uri = LocalUriHandler.current
    fun open(url: String) = runCatching { uri.openUri(url) }
    SectionCard(
        "Prices online",
        Icons.Filled.ShoppingCart,
        trailing = {
            if (hasKey && price !is PriceUiState.Loading) {
                IconButton(onClick = { onFetch(true) }) { Icon(Icons.Filled.Refresh, "Refresh prices") }
            }
        }
    ) {
        when {
            !hasKey -> {
                Text("Add a free SerpApi key in Settings to see live retail prices and how the menu's markup compares.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onOpenSettings) { Text("Set up live prices") }
            }
            price == null || price is PriceUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Searching retailers${wine.vintage.year?.let { " for the $it vintage" } ?: ""}…")
            }
            price is PriceUiState.Loaded -> when (val r = price.result) {
                is PriceResult.Found -> PriceSummaryView(wine, r.summary) { open(it) }
                PriceResult.NotFound -> Text("No retail listings found for this wine.", style = MaterialTheme.typography.bodyMedium)
                is PriceResult.Unavailable -> Text(r.reason, style = MaterialTheme.typography.bodyMedium)
                is PriceResult.Failed -> {
                    Text(r.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onFetch(true) }) { Text("Try again") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Compare elsewhere", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { open(PriceLinks.wineSearcher(wine.searchName, wine.vintage.year)) }) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Wine-Searcher")
            }
            OutlinedButton(onClick = { open(PriceLinks.vivino(wine.searchName)) }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Vivino")
            }
            OutlinedButton(onClick = { open(PriceLinks.googleShopping(wine.searchName, wine.vintage.year)) }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Google Shopping")
            }
        }
    }
}

@Composable
private fun PriceSummaryView(wine: AnalyzedWine, s: PriceSummary, open: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text("Typical retail", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatPrice(s.median, s.currencySymbol), style = MaterialTheme.typography.headlineMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Range", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${formatPrice(s.low, s.currencySymbol)} – ${formatPrice(s.high, s.currencySymbol)}", style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(4.dp))
    val vintageNote = when {
        wine.vintage.year == null -> "Across all vintages"
        s.vintageSpecific -> "For the ${wine.vintage.year} vintage"
        else -> "No ${wine.vintage.year} listings found — prices shown are for other vintages"
    }
    Text("$vintageNote · ${s.offers.size} listing${if (s.offers.size == 1) "" else "s"} via ${s.source}",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    MarkupAnalyzer.analyze(wine.bottlePrice, wine.currency, s, wine.halfBottle)?.let { m ->
        Spacer(Modifier.height(10.dp))
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("%.1f×".format(m.ratio), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(m.verdict.label, style = MaterialTheme.typography.titleSmall)
                    Text("Menu ${formatPrice(wine.bottlePrice!!, wine.currency)} vs retail ${formatPrice(s.median, s.currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    s.offers.take(6).forEach { o ->
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                .then(if (o.url != null) Modifier.padding(vertical = 2.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(o.merchant, style = MaterialTheme.typography.titleSmall)
                Text(o.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (o.vintageMatched == true) Pill("${wine.vintage.year}", modifier = Modifier.padding(end = 6.dp))
            Text(formatPrice(o.price, o.currencySymbol ?: s.currencySymbol), style = MaterialTheme.typography.titleSmall)
            if (o.url != null) {
                IconButton(onClick = { open(o.url!!) }) { Icon(Icons.Filled.OpenInNew, "Open listing", modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
