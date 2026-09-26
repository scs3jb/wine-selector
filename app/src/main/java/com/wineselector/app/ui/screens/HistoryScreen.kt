@file:OptIn(ExperimentalMaterial3Api::class)

package com.wineselector.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wineselector.app.ui.components.Pill
import com.wineselector.app.ui.components.StyleSwatch
import com.wineselector.app.ui.components.formatPrice
import com.wineselector.core.history.ScanKind
import com.wineselector.core.history.ScanRecord
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    history: List<ScanRecord>,
    onOpen: (ScanRecord) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Scan history") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (history.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.DeleteSweep, "Clear history") }
            }
        )
    }) { padding ->
        if (history.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("Your scans will appear here", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { it.id }) { record -> HistoryCard(record, { onOpen(record) }, { onDelete(record.id) }) }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all scans?") },
            text = { Text("This removes your scan history and the saved photos.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryCard(record: ScanRecord, onOpen: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onOpen, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(76.dp).clip(MaterialTheme.shapes.medium)) {
                record.imagePath?.let { AsyncImage(File(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(if (record.kind == ScanKind.MENU) "Wine list" else "Bottle")
                    record.food?.let { f ->
                        runCatching { FoodCategory.valueOf(f) }.getOrNull()?.let {
                            Spacer(Modifier.width(6.dp)); Pill("${it.icon} ${it.displayName}")
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.timestampMillis)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                record.wines.take(3).forEach { w ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                        StyleSwatch(w.style?.let { runCatching { WineStyle.valueOf(it) }.getOrNull() }, size = 8.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            listOfNotNull(w.name, w.vintage?.toString()).joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        w.price?.let { Text(formatPrice(it, w.currency), style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete scan", tint = MaterialTheme.colorScheme.outline) }
        }
    }
}
