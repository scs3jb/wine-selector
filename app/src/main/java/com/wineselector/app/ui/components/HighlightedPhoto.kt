package com.wineselector.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import com.wineselector.core.scan.Box as OcrBox
import java.io.File

/** A box to draw over the photo, in OCR image pixel coordinates. */
data class PhotoHighlight(val box: OcrBox, val color: Color)

private fun DrawScope.drawHighlights(highlights: List<PhotoHighlight>, imageW: Int, imageH: Int, layout: IntSize, stroke: Float) {
    if (imageW <= 0 || imageH <= 0 || layout.width == 0) return
    val scale = minOf(layout.width.toFloat() / imageW, layout.height.toFloat() / imageH)
    val offX = (layout.width - imageW * scale) / 2f
    val offY = (layout.height - imageH * scale) / 2f
    val pad = 4f
    for (h in highlights) {
        val left = h.box.left * scale + offX - pad
        val top = h.box.top * scale + offY - pad
        val w = h.box.width * scale + pad * 2
        val hgt = h.box.height * scale + pad * 2
        drawRoundRect(h.color.copy(alpha = 0.28f), Offset(left, top), Size(w, hgt), CornerRadius(6f))
        drawRoundRect(h.color, Offset(left, top), Size(w, hgt), CornerRadius(6f), style = Stroke(stroke))
    }
}

@Composable
fun HighlightedPhoto(
    imagePath: String,
    imageWidth: Int,
    imageHeight: Int,
    highlights: List<PhotoHighlight>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var layout by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Scanned photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().onSizeChanged { layout = it }
        )
        if (highlights.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) { drawHighlights(highlights, imageWidth, imageHeight, layout, 2.dp.toPx()) }
        }
    }
}

@Composable
fun FullscreenPhoto(
    imagePath: String,
    imageWidth: Int,
    imageHeight: Int,
    highlights: List<PhotoHighlight>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var layout by remember { mutableStateOf(IntSize.Zero) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                            val maxX = (newScale - 1f) * size.width / 2f
                            val maxY = (newScale - 1f) * size.height / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1.1f) 1f else 2.5f
                                offsetX = 0f; offsetY = 0f
                            },
                            onTap = { if (scale <= 1.1f) onDismiss() }
                        )
                    }
            ) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY
                }) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Scanned photo, full screen",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().onSizeChanged { layout = it }
                    )
                    Canvas(Modifier.fillMaxSize()) { drawHighlights(highlights, imageWidth, imageHeight, layout, 3.dp.toPx()) }
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(16.dp).size(44.dp)
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White) }
            }
        }
    }
}
