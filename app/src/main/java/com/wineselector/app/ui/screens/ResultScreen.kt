package com.wineselector.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wineselector.app.data.FoodCategory
import com.wineselector.app.data.HighlightTier
import com.wineselector.app.data.WineHighlight
import com.wineselector.app.data.WineRecommendation
import com.wineselector.app.ui.components.WineRecommendationCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imagePath: String?,
    foodCategory: FoodCategory?,
    recommendation: WineRecommendation?,
    isLoading: Boolean,
    error: String?,
    wineHighlights: List<WineHighlight> = emptyList(),
    ocrImageSize: Pair<Int, Int>? = null,
    onBack: () -> Unit,
    onTryAnother: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (foodCategory != null) "Pairing with ${foodCategory.displayName}"
                        else "Recommendation"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            if (imagePath != null) {
                HighlightedWineImage(
                    imagePath = imagePath,
                    highlights = wineHighlights,
                    ocrImageSize = ocrImageSize
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            when {
                isLoading -> {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing wine list...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                error != null -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onTryAnother) {
                        Text("Try Again")
                    }
                }
                recommendation != null -> {
                    WineRecommendationCard(recommendation = recommendation)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onTryAnother,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Another List")
                    }
                }
            }
        }
    }
}

// Highlight colors
private val TopPickFill = Color(0x55D4A843)        // semi-transparent gold
private val TopPickBorder = Color(0xCCD4A843)       // opaque gold
private val AlternativeFill = Color(0x33A35D67)     // semi-transparent wine-red
private val AlternativeBorder = Color(0x88A35D67)   // wine-red border

@Composable
private fun HighlightedWineImage(
    imagePath: String,
    highlights: List<WineHighlight>,
    ocrImageSize: Pair<Int, Int>?
) {
    var imageLayoutSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Wine list photo",
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageLayoutSize = it },
            contentScale = ContentScale.Fit
        )

        if (highlights.isNotEmpty() && ocrImageSize != null && imageLayoutSize != IntSize.Zero) {
            val (imgW, imgH) = ocrImageSize
            if (imgW > 0 && imgH > 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val layoutW = imageLayoutSize.width.toFloat()
                    val layoutH = imageLayoutSize.height.toFloat()

                    // ContentScale.Fit: scale to fit entirely within container
                    val scale = minOf(layoutW / imgW, layoutH / imgH)
                    val offsetX = (layoutW - imgW * scale) / 2f
                    val offsetY = (layoutH - imgH * scale) / 2f

                    val borderWidth = 2.dp.toPx()

                    for (highlight in highlights) {
                        val (fillColor, borderColor) = when (highlight.tier) {
                            HighlightTier.TOP_PICK -> TopPickFill to TopPickBorder
                            HighlightTier.ALTERNATIVE -> AlternativeFill to AlternativeBorder
                        }

                        for (box in highlight.boundingBoxes) {
                            val left = box.left * scale + offsetX
                            val top = box.top * scale + offsetY
                            val right = box.right * scale + offsetX
                            val bottom = box.bottom * scale + offsetY

                            drawRect(
                                color = fillColor,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                            drawRect(
                                color = borderColor,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                                style = Stroke(width = borderWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}
