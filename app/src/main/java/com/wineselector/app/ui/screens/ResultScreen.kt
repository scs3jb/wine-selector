package com.wineselector.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var showFullscreen by remember { mutableStateOf(false) }

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
                    ocrImageSize = ocrImageSize,
                    onClick = { showFullscreen = true }
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

    if (showFullscreen && imagePath != null) {
        FullscreenPhotoOverlay(
            imagePath = imagePath,
            highlights = wineHighlights,
            ocrImageSize = ocrImageSize,
            onDismiss = { showFullscreen = false }
        )
    }
}

// Highlight colors — ranked by placement
private val GoldFill = Color(0x55D4A843)
private val GoldBorder = Color(0xCCD4A843)
private val SilverFill = Color(0x44A8A8A8)
private val SilverBorder = Color(0xBBA8A8A8)
private val BronzeFill = Color(0x44CD7F32)
private val BronzeBorder = Color(0xBBCD7F32)
private val RedFill = Color(0x33A35D67)
private val RedBorder = Color(0x88A35D67)

private fun highlightColors(tier: HighlightTier): Pair<Color, Color> = when (tier) {
    HighlightTier.GOLD -> GoldFill to GoldBorder
    HighlightTier.SILVER -> SilverFill to SilverBorder
    HighlightTier.BRONZE -> BronzeFill to BronzeBorder
    HighlightTier.RED -> RedFill to RedBorder
}

private fun DrawScope.drawHighlights(
    highlights: List<WineHighlight>,
    ocrImageSize: Pair<Int, Int>,
    layoutSize: IntSize,
    borderWidth: Float
) {
    val (imgW, imgH) = ocrImageSize
    if (imgW <= 0 || imgH <= 0) return

    val layoutW = layoutSize.width.toFloat()
    val layoutH = layoutSize.height.toFloat()
    val scale = minOf(layoutW / imgW, layoutH / imgH)
    val offsetX = (layoutW - imgW * scale) / 2f
    val offsetY = (layoutH - imgH * scale) / 2f

    for (highlight in highlights) {
        val (fillColor, borderColor) = highlightColors(highlight.tier)

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

@Composable
private fun HighlightedWineImage(
    imagePath: String,
    highlights: List<WineHighlight>,
    ocrImageSize: Pair<Int, Int>?,
    onClick: () -> Unit
) {
    var imageLayoutSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Wine list photo (tap to enlarge)",
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageLayoutSize = it },
            contentScale = ContentScale.Fit
        )

        if (highlights.isNotEmpty() && ocrImageSize != null && imageLayoutSize != IntSize.Zero) {
            val capturedSize = imageLayoutSize
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawHighlights(highlights, ocrImageSize, capturedSize, 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun FullscreenPhotoOverlay(
    imagePath: String,
    highlights: List<WineHighlight>,
    ocrImageSize: Pair<Int, Int>?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var imageLayoutSize by remember { mutableStateOf(IntSize.Zero) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(modifier = Modifier.fillMaxSize()) {
            // Black background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black)
            }

            // Zoomable layer: image + highlights zoom/pan together
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            // Clamp offset so image stays visible
                            val maxOffsetX = (newScale - 1f) * size.width / 2f
                            val maxOffsetY = (newScale - 1f) * size.height / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.1f) {
                                    // Reset to 1x
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    // Zoom to 2.5x
                                    scale = 2.5f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            onTap = {
                                // Only dismiss when not zoomed in
                                if (scale <= 1.1f) {
                                    onDismiss()
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                ) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Wine list photo (fullscreen)",
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { imageLayoutSize = it },
                        contentScale = ContentScale.Fit
                    )

                    if (highlights.isNotEmpty() && ocrImageSize != null && imageLayoutSize != IntSize.Zero) {
                        val capturedSize = imageLayoutSize
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawHighlights(highlights, ocrImageSize, capturedSize, 3.dp.toPx())
                        }
                    }
                }
            }

            // Close button — above the zoomable layer, always accessible
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
