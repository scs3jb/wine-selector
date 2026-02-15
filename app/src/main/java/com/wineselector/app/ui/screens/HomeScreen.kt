package com.wineselector.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wineselector.app.data.DatasetSize
import com.wineselector.app.data.FoodCategory
import com.wineselector.app.ui.components.FoodCategoryPicker
import com.wineselector.app.viewmodel.DatasetStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedCategory: FoodCategory?,
    onCategorySelected: (FoodCategory) -> Unit,
    onScanWineList: () -> Unit,
    datasetStatus: DatasetStatus = DatasetStatus.UsingBundled(0),
    showDatasetChoice: Boolean = false,
    onDatasetChosen: (DatasetSize) -> Unit = {},
    onSkipDownload: () -> Unit = {},
    onRetryDownload: () -> Unit = {},
    onChangeDataset: () -> Unit = {},
    maxPrice: Int = 60,
    onOpenSettings: () -> Unit = {}
) {
    if (showDatasetChoice) {
        DatasetChoiceDialog(
            onChosen = onDatasetChosen,
            onSkip = onSkipDownload
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wine Selector") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83C\uDF77",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find the perfect pairing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            FoodCategoryPicker(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onScanWineList,
                enabled = selectedCategory != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = "Scan Wine List",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedCategory == null) {
                Text(
                    text = "Select a food above, then scan a wine list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = "Works offline \u2014 no API key needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Budget: up to \$$maxPrice",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onOpenSettings)
            )

            Spacer(modifier = Modifier.height(4.dp))
            DatasetStatusIndicator(
                status = datasetStatus,
                onRetry = onRetryDownload,
                onChangeDataset = onChangeDataset
            )
        }
    }
}

@Composable
private fun DatasetChoiceDialog(
    onChosen: (DatasetSize) -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissable on first boot */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Wine Database",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Choose a wine database for better recommendations. " +
                        "You can always change this later.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Full option
                OutlinedButton(
                    onClick = { onChosen(DatasetSize.FULL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DatasetSize.FULL.label,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DatasetSize.FULL.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Slim option
                OutlinedButton(
                    onClick = { onChosen(DatasetSize.SLIM) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DatasetSize.SLIM.label,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DatasetSize.SLIM.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip — use grape matching only")
            }
        }
    )
}

@Composable
private fun DatasetStatusIndicator(
    status: DatasetStatus,
    onRetry: () -> Unit,
    onChangeDataset: () -> Unit = {}
) {
    when (status) {
        is DatasetStatus.NeedsChoice -> {
            Text(
                text = "Choose a database to enhance recommendations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onChangeDataset)
            )
        }
        is DatasetStatus.UsingBundled -> {
            Text(
                text = "Using grape matching only \u2014 tap to change",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.clickable(onClick = onChangeDataset)
            )
        }
        is DatasetStatus.Downloading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Downloading ${status.datasetLabel}... ${status.progressPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { status.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }
        is DatasetStatus.Extracting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Loading wine database...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        is DatasetStatus.UsingEnhanced -> {
            Text(
                text = "${status.wineCount} wines loaded (${status.datasetLabel}) \u2014 tap to change",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                modifier = Modifier.clickable(onClick = onChangeDataset)
            )
        }
        is DatasetStatus.DownloadFailed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Download failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        is DatasetStatus.InsufficientSpace -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Not enough space (need ${status.requiredMb} MB, have ${status.availableMb} MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Try again",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
