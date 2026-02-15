package com.wineselector.app.data

import android.graphics.Rect

/**
 * A single line of OCR text with its bounding box in image pixel coordinates.
 */
data class OcrLine(
    val text: String,
    val boundingBox: Rect?
)

/**
 * Full OCR result containing the raw text plus per-line bounding boxes
 * and the original image dimensions (needed for coordinate transformation).
 */
data class OcrResult(
    val fullText: String,
    val lines: List<OcrLine>,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Highlight data for a single matched wine on the photo.
 */
data class WineHighlight(
    val boundingBoxes: List<Rect>,
    val tier: HighlightTier,
    val wineName: String
)

enum class HighlightTier {
    TOP_PICK,
    ALTERNATIVE
}
