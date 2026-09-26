@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wineselector.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wineselector.app.data.AppSettings
import com.wineselector.app.ui.components.SectionCard
import com.wineselector.app.ui.components.StyleSwatch
import com.wineselector.app.ui.components.localCurrencySymbol
import com.wineselector.app.viewmodel.DatabaseSource
import com.wineselector.app.viewmodel.DatabaseUiState
import com.wineselector.core.db.DatasetSize
import com.wineselector.core.model.WinePreferences
import com.wineselector.core.model.WineStyle
import kotlin.math.roundToInt

private const val PRICE_SLIDER_MAX = 300

@Composable
fun SettingsScreen(
    settings: AppSettings,
    database: DatabaseUiState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onDownload: (DatasetSize) -> Unit,
    onUseStarter: () -> Unit,
    onDismissDbError: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PreferencesSection(settings.preferences) { p -> onUpdate { it.copy(preferences = p) } }
            PricesSection(settings, onUpdate)
            DatabaseSection(database, onDownload, onUseStarter, onDismissDbError)
            Text(
                "Wine data: X-Wines dataset (github.com/rogerioxavier/X-Wines). Text recognition runs on your device; " +
                    "only price searches use the internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreferencesSection(prefs: WinePreferences, onChange: (WinePreferences) -> Unit) {
    SectionCard("Your preferences", Icons.Filled.Tune) {
        val symbol = localCurrencySymbol()
        val noLimit = prefs.maxPrice == WinePreferences.NO_LIMIT
        var slider by remember(prefs.maxPrice) {
            mutableFloatStateOf(if (noLimit) PRICE_SLIDER_MAX.toFloat() else prefs.maxPrice.toFloat().coerceAtMost(PRICE_SLIDER_MAX.toFloat()))
        }
        Text("Maximum bottle price", style = MaterialTheme.typography.titleSmall)
        Text(
            if (slider >= PRICE_SLIDER_MAX) "No limit" else "$symbol${slider.roundToInt()}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = slider,
            onValueChange = { slider = it },
            onValueChangeFinished = {
                onChange(prefs.copy(maxPrice = if (slider >= PRICE_SLIDER_MAX) WinePreferences.NO_LIMIT else slider.roundToInt()))
            },
            valueRange = 10f..PRICE_SLIDER_MAX.toFloat(),
            steps = (PRICE_SLIDER_MAX - 10) / 5 - 1
        )

        Spacer(Modifier.height(8.dp))
        Text("Styles to show", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WineStyle.entries.forEach { style ->
                val on = style in prefs.allowedStyles
                FilterChip(
                    selected = on,
                    onClick = {
                        val next = if (on) prefs.allowedStyles - style else prefs.allowedStyles + style
                        if (next.isNotEmpty()) onChange(prefs.copy(allowedStyles = next))
                    },
                    label = { Text(style.label) },
                    leadingIcon = { StyleSwatch(style, 10.dp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Grapes to avoid", style = MaterialTheme.typography.titleSmall)
        var grape by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = grape,
                onValueChange = { grape = it },
                placeholder = { Text("e.g. Chardonnay") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                enabled = grape.isNotBlank(),
                onClick = { onChange(prefs.copy(ignoredGrapes = prefs.ignoredGrapes + grape.trim())); grape = "" }
            ) { Text("Add") }
        }
        if (prefs.ignoredGrapes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prefs.ignoredGrapes.sorted().forEach { g ->
                    InputChip(
                        selected = false,
                        onClick = { onChange(prefs.copy(ignoredGrapes = prefs.ignoredGrapes - g)) },
                        label = { Text(g) },
                        trailingIcon = { Icon(Icons.Filled.Close, "Remove", Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PricesSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val uri = LocalUriHandler.current
    SectionCard("Live prices", Icons.Filled.Key) {
        Text(
            "Retail prices come from Google Shopping through SerpApi. Create a free account at serpapi.com and paste your API key here.",
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = { runCatching { uri.openUri("https://serpapi.com/users/sign_up") } }) { Text("Get a free key") }
        var key by remember(settings.serpApiKey) { mutableStateOf(settings.serpApiKey) }
        var visible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; onUpdate { s -> s.copy(serpApiKey = it.trim()) } },
            label = { Text("SerpApi key") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show key")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        var country by remember(settings.priceCountry) { mutableStateOf(settings.priceCountry) }
        OutlinedTextField(
            value = country,
            onValueChange = {
                country = it.take(2).uppercase()
                if (country.length == 2) onUpdate { s -> s.copy(priceCountry = country) }
            },
            label = { Text("Shopping country (2-letter code)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Check prices automatically", style = MaterialTheme.typography.titleSmall)
                Text("Looks up the top 3 wines after each scan (uses API quota).", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = settings.autoFetchPrices, onCheckedChange = { on -> onUpdate { it.copy(autoFetchPrices = on) } })
        }
    }
}

@Composable
private fun DatabaseSection(
    db: DatabaseUiState,
    onDownload: (DatasetSize) -> Unit,
    onUseStarter: () -> Unit,
    onDismissError: () -> Unit
) {
    SectionCard("Wine database", Icons.Filled.Storage) {
        Text(
            "${db.source.label} · ${"%,d".format(db.wineCount)} wines" + if (db.loading && db.downloading == null) " (loading…)" else "",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "A bigger database recognises more wines by name, with ratings, vintages and food matches.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        db.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismissError) { Text("Dismiss") }
        }
        Spacer(Modifier.height(12.dp))
        if (db.downloading != null) {
            Text("Downloading ${db.downloading.label}… ${db.downloadPercent}%", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { db.downloadPercent / 100f }, modifier = Modifier.fillMaxWidth())
            Text("You can keep using the app while this finishes.", style = MaterialTheme.typography.bodySmall)
        } else {
            DatasetSize.entries.forEach { ds ->
                val current = (ds == DatasetSize.FULL && db.source == DatabaseSource.FULL) ||
                    (ds == DatasetSize.SLIM && db.source == DatabaseSource.SLIM)
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ds.label, style = MaterialTheme.typography.titleSmall)
                        Text(ds.description, style = MaterialTheme.typography.bodySmall)
                    }
                    if (current) Text("In use", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    else Button(onClick = { onDownload(ds) }) { Text("Download") }
                }
            }
            if (db.source != DatabaseSource.BUNDLED) {
                OutlinedButton(onClick = onUseStarter, modifier = Modifier.padding(top = 8.dp)) { Text("Delete download & use starter set") }
            }
        }
    }
}
