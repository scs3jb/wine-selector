@file:OptIn(ExperimentalMaterial3Api::class)

package com.wineselector.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wineselector.app.ui.components.FoodChipRow
import com.wineselector.app.ui.components.Pill
import com.wineselector.app.ui.theme.Bordeaux
import com.wineselector.app.ui.theme.BordeauxDark
import com.wineselector.app.ui.theme.Gold
import com.wineselector.app.viewmodel.DatabaseSource
import com.wineselector.app.viewmodel.DatabaseUiState
import com.wineselector.app.viewmodel.ScanMode
import com.wineselector.core.history.ScanKind
import com.wineselector.core.history.ScanRecord
import com.wineselector.core.model.FoodCategory
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    food: FoodCategory?,
    onFoodSelected: (FoodCategory?) -> Unit,
    database: DatabaseUiState,
    history: List<ScanRecord>,
    onScan: (ScanMode) -> Unit,
    onImport: (ScanMode) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecord: (ScanRecord) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Hero(onOpenHistory, onOpenSettings)

        Column(Modifier.padding(horizontal = 20.dp).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ActionCard(
                title = "Scan a wine list",
                body = "Ranks every wine on the menu for your food, with vintages, value and ratings.",
                icon = Icons.Filled.MenuBook,
                accent = Bordeaux,
                onClick = { onScan(ScanMode.MENU) },
                onImport = { onImport(ScanMode.MENU) }
            )
            ActionCard(
                title = "Scan a bottle",
                body = "Identify a label, check the vintage, drinking window and what it costs online.",
                icon = Icons.Filled.WineBar,
                accent = Color(0xFF8A6A4F),
                onClick = { onScan(ScanMode.BOTTLE) },
                onImport = { onImport(ScanMode.BOTTLE) }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("What are you eating?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        Text(
            "Results re-rank instantly when you change this.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        FoodChipRow(selected = food, onSelect = onFoodSelected)

        if (database.source == DatabaseSource.BUNDLED || database.downloading != null) {
            Spacer(Modifier.height(20.dp))
            DatabaseCard(database, onOpenSettings)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent scans", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenHistory) { Text("See all") }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history.take(8), key = { it.id }) { record -> RecentScanCard(record) { onOpenRecord(record) } }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Hero(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(BordeauxDark, Bordeaux, Color(0xFF9E3B52))))
            .statusBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 28.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WineBar, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Wine Selector", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, "History", tint = Color.White) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings", tint = Color.White) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Pick the perfect bottle,", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("every time.", style = MaterialTheme.typography.headlineLarge, color = Gold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Point your camera at a wine list or a label. Everything is read on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    onImport: () -> Unit
) {
    ElevatedCard(onClick = onClick, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Choose from photos", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DatabaseCard(state: DatabaseUiState, onOpenSettings: () -> Unit) {
    Card(
        onClick = onOpenSettings,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (state.downloading != null) Icons.Filled.CloudDownload else Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (state.downloading != null) {
                    Text("Downloading wine database… ${state.downloadPercent}%", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { state.downloadPercent / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Recognise 100,000 wines", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "You're using the ${"%,d".format(state.wineCount)}-wine starter set. Download the full database for far better matches.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentScanCard(record: ScanRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(Modifier.height(110.dp).fillMaxWidth()) {
            record.imagePath?.let {
                AsyncImage(model = File(it), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Pill(
                if (record.kind == ScanKind.MENU) "Wine list" else "Bottle",
                modifier = Modifier.padding(8.dp),
                container = Color.Black.copy(alpha = 0.55f),
                content = Color.White
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                record.wines.firstOrNull()?.let { w -> listOfNotNull(w.name, w.vintage?.toString()).joinToString(" ") } ?: "No wines",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(record.timestampMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
