@file:OptIn(ExperimentalMaterial3Api::class)

package com.wineselector.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wineselector.app.ui.components.FoodChipRow
import com.wineselector.app.ui.components.FullscreenPhoto
import com.wineselector.app.ui.components.HighlightedPhoto
import com.wineselector.app.ui.components.PhotoHighlight
import com.wineselector.app.ui.components.Pill
import com.wineselector.app.ui.components.RatingStars
import com.wineselector.app.ui.components.ScoreRing
import com.wineselector.app.ui.components.drinkingStatusColor
import com.wineselector.app.ui.components.formatPrice
import com.wineselector.app.ui.components.menuPriceLabel
import com.wineselector.app.ui.components.subtitle
import com.wineselector.app.ui.components.vintageLabel
import com.wineselector.app.ui.theme.Medal1
import com.wineselector.app.ui.theme.Medal2
import com.wineselector.app.ui.theme.Medal3
import com.wineselector.app.ui.theme.styleColor
import com.wineselector.app.viewmodel.DatabaseSource
import com.wineselector.app.viewmodel.PriceUiState
import com.wineselector.app.viewmodel.ScanMode
import com.wineselector.app.viewmodel.ScanUiState
import com.wineselector.core.analysis.AnalyzedWine
import com.wineselector.core.analysis.MenuAnalysis
import com.wineselector.core.analysis.Pick
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.pricing.MarkupAnalyzer
import com.wineselector.core.pricing.MarkupVerdict
import com.wineselector.core.pricing.PriceResult
import com.wineselector.core.scan.OcrPage
import java.io.File

/** Progress view shared by menu and bottle scans: the photo with a sweeping scan line. */
@Composable
fun ScanProgress(state: ScanUiState.Working, onCancel: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(model = File(state.imagePath), contentDescription = null, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * sweep
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x66C9A24B), Color.Transparent), startY = y - 120f, endY = y + 120f),
                topLeft = Offset(0f, y - 120f),
                size = androidx.compose.ui.geometry.Size(size.width, 240f)
            )
            drawLine(Color(0xFFE9C46A), Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        }
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).padding(bottom = 32.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.stage, color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                TextButton(onClick = onCancel) { Text("Cancel", color = Color.White.copy(alpha = 0.8f)) }
            }
        }
    }
}

@Composable
fun ScanFailed(state: ScanUiState.Failed, onRetake: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Couldn't read that") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.imagePath?.let {
                AsyncImage(model = File(it), contentDescription = null, modifier = Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.large))
                Spacer(Modifier.height(20.dp))
            }
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(state.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Tips(state.mode)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetake) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try again")
            }
        }
    }
}

@Composable
private fun Tips(mode: ScanMode) {
    val tips = if (mode == ScanMode.MENU) listOf(
        "Hold the phone parallel to the page",
        "Fill the frame with one page or column",
        "Turn on the torch in dim restaurants",
        "Tap the text to focus before shooting"
    ) else listOf(
        "Photograph the front label, straight on",
        "Rotate the bottle so the name faces you",
        "Avoid reflections from lights"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tips.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private enum class SortMode(val label: String) {
    BEST("Best match"),
    PRICE_LOW("Price: low to high"),
    PRICE_HIGH("Price: high to low"),
    RATING("Community rating"),
    VINTAGE("Oldest vintage first")
}

@Composable
fun MenuResultsScreen(
    state: ScanUiState.Menu,
    food: FoodCategory?,
    prices: Map<String, PriceUiState>,
    databaseSource: DatabaseSource,
    onFoodSelected: (FoodCategory?) -> Unit,
    onOpenWine: (String) -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val analysis = state.analysis
    var sort by rememberSaveable { mutableStateOf(SortMode.BEST) }
    var sortMenu by remember { mutableStateOf(false) }
    var styleFilter by rememberSaveable { mutableStateOf<WineStyle?>(null) }
    var fullscreen by remember { mutableStateOf(false) }

    val stylesPresent = analysis.wines.mapNotNull { it.style }.distinct().sortedBy { it.ordinal }
    val shown = analysis.wines
        .filter { styleFilter == null || it.style == styleFilter }
        .let { list ->
            when (sort) {
                SortMode.BEST -> list
                SortMode.PRICE_LOW -> list.sortedWith(compareBy(nullsLast()) { it.bottlePrice })
                SortMode.PRICE_HIGH -> list.sortedWith(compareByDescending(nullsFirst<Double>()) { it.bottlePrice })
                SortMode.RATING -> list.sortedByDescending { it.rating ?: 0f }
                SortMode.VINTAGE -> list.sortedWith(compareBy(nullsLast()) { it.vintage.year })
            }
        }
    val rankOf = analysis.wines.withIndex().associate { it.value.key to it.index + 1 }
    val highlights = remember(analysis) { highlightsFor(analysis, state.page) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wine list", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${analysis.wines.size} wines · ${analysis.identifiedCount} identified",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onRetake) { Icon(Icons.Filled.CameraAlt, "Scan again") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "photo") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    HighlightedPhoto(
                        imagePath = state.imagePath,
                        imageWidth = state.page.width,
                        imageHeight = state.page.height,
                        highlights = highlights,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        onClick = { fullscreen = true }
                    )
                    Pill("Tap to enlarge", modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                        container = Color.Black.copy(alpha = 0.55f), content = Color.White)
                }
            }
            item(key = "food") {
                Column {
                    Text("Pairing with", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                    FoodChipRow(selected = food, onSelect = onFoodSelected, contentPadding = PaddingValues(horizontal = 16.dp))
                }
            }
            if (analysis.picks.isNotEmpty()) {
                item(key = "picks") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(analysis.picks.entries.toList(), key = { it.key.name }) { (pick, key) ->
                            analysis.wine(key)?.let { PickCard(pick, it, onClick = { onOpenWine(key) }) }
                        }
                    }
                }
            }
            item(key = "filters") {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = styleFilter == null, onClick = { styleFilter = null }, label = { Text("All") }) }
                        items(stylesPresent) { s ->
                            FilterChip(selected = styleFilter == s, onClick = { styleFilter = if (styleFilter == s) null else s },
                                label = { Text(s.label) },
                                leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(styleColor(s))) })
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(text = { Text(m.label, fontWeight = if (m == sort) FontWeight.Bold else null) },
                                    onClick = { sort = m; sortMenu = false })
                            }
                        }
                    }
                }
            }
            itemsIndexed(shown, key = { _, w -> w.key }) { _, wine ->
                WineListCard(
                    rank = rankOf[wine.key] ?: 0,
                    wine = wine,
                    picks = analysis.picksFor(wine.key),
                    price = prices[wine.key],
                    showRank = sort == SortMode.BEST && food != null,
                    onClick = { onOpenWine(wine.key) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "footer") {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (analysis.hiddenByFilters > 0) {
                        Text(
                            "${analysis.hiddenByFilters} wine${if (analysis.hiddenByFilters == 1) "" else "s"} hidden by your preferences.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (databaseSource == DatabaseSource.BUNDLED && analysis.identifiedCount < analysis.wines.size) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), onClick = onOpenSettings) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.HelpOutline, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Unidentified wines are scored from their grapes and style. Download the full 100K-wine database in Settings to identify more.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (fullscreen) {
        FullscreenPhoto(state.imagePath, state.page.width, state.page.height, highlights, onDismiss = { fullscreen = false })
    }
}

private fun highlightsFor(analysis: MenuAnalysis, page: OcrPage): List<PhotoHighlight> {
    val colors = listOf(Medal1, Medal2, Medal3)
    return analysis.wines.take(3).flatMapIndexed { i, w ->
        w.lineIndices.mapNotNull { idx -> page.lines.getOrNull(idx)?.box }
            .take(2)
            .map { PhotoHighlight(it, colors[i]) }
    }
}

private fun pickIcon(pick: Pick): ImageVector = when (pick) {
    Pick.BEST_PAIRING -> Icons.Filled.EmojiEvents
    Pick.BEST_VALUE -> Icons.Filled.Savings
    Pick.TOP_RATED -> Icons.Filled.Star
}

@Composable
private fun PickCard(pick: Pick, wine: AnalyzedWine, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.width(230.dp), shape = MaterialTheme.shapes.large) {
        Box(Modifier.fillMaxWidth().height(6.dp).background(styleColor(wine.style)))
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(pickIcon(pick), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(pick.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(6.dp))
            Text(wine.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(wine.vintageLabel().takeIf { wine.vintage.isKnown }, wine.menuPriceLabel()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            wine.pairing?.let {
                Spacer(Modifier.height(4.dp))
                Text("${it.score}/10 pairing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun WineListCard(
    rank: Int,
    wine: AnalyzedWine,
    picks: List<Pick>,
    price: PriceUiState?,
    showRank: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(styleColor(wine.style)))
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                if (showRank) {
                    val medal = when (rank) { 1 -> Medal1; 2 -> Medal2; 3 -> Medal3; else -> MaterialTheme.colorScheme.surfaceVariant }
                    Surface(shape = CircleShape, color = medal, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$rank", style = MaterialTheme.typography.labelLarge, color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(wine.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val sub = wine.subtitle()
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (wine.vintage.isKnown) {
                            val stars = wine.vintageRating?.stars
                            Pill(wine.vintageLabel() + (stars?.let { " · " + "★".repeat(it) } ?: ""))
                        }
                        wine.drinkingWindow?.let {
                            Pill(it.status.label.substringBefore(" —").substringBefore(","),
                                container = drinkingStatusColor(it.status).copy(alpha = 0.15f),
                                content = drinkingStatusColor(it.status))
                        }
                        if (!wine.isIdentified) {
                            Pill("Not in database", container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (picks.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            picks.forEach { Pill(it.label, icon = pickIcon(it), container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    wine.menuPriceLabel()?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                    RetailLine(wine, price)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pairing = wine.pairing
                    if (pairing != null) ScoreRing(pairing.score)
                    wine.rating?.let {
                        Spacer(Modifier.height(6.dp))
                        RatingStars(it, size = 12.dp)
                        Text("%.1f".format(it), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailLine(wine: AnalyzedWine, price: PriceUiState?) {
    val summary = ((price as? PriceUiState.Loaded)?.result as? PriceResult.Found)?.summary
    if (summary == null) {
        if (price is PriceUiState.Loading) {
            Text("Checking online prices…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val markup = MarkupAnalyzer.analyze(wine.bottlePrice, wine.currency, summary, wine.halfBottle)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Retail ~${formatPrice(summary.median, summary.currencySymbol)}" + if (!summary.vintageSpecific && wine.vintage.year != null) " (any vintage)" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (markup != null) {
            Spacer(Modifier.width(6.dp))
            val good = markup.verdict == MarkupVerdict.BARGAIN || markup.verdict == MarkupVerdict.FAIR
            Pill(
                "%.1f× markup".format(markup.ratio),
                icon = if (good) Icons.Filled.ThumbUp else null,
                container = if (good) Color(0x222E7D4F) else Color(0x22B3261E),
                content = if (good) Color(0xFF2E7D4F) else Color(0xFFB3261E)
            )
        }
    }
}

@Composable
fun EmptyScanPlaceholder(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No scan in progress", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack) { Text("Back to home") }
    }
}
