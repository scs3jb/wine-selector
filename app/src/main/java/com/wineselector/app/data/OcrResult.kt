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
) {
    /**
     * Merge OCR lines that share the same visual row (by vertical overlap)
     * into single lines, sorted left-to-right within each row. This fixes
     * two-column menus where ML Kit reads producer names and grape/region
     * descriptions as separate text blocks.
     */
    fun spatiallyMergedText(): String {
        val withBoxes = lines.filter { it.boundingBox != null }
        if (withBoxes.isEmpty()) return fullText

        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in withBoxes.sortedBy { it.boundingBox!!.top }) {
            val box = line.boundingBox!!
            val lineHeight = box.bottom - box.top

            val matchingRow = rows.find { row ->
                val rowTop = row.minOf { it.boundingBox!!.top }
                val rowBottom = row.maxOf { it.boundingBox!!.bottom }
                val overlap = minOf(box.bottom, rowBottom) - maxOf(box.top, rowTop)
                overlap > lineHeight * 0.5
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        // Append any lines without bounding boxes at the end
        val noBbox = lines.filter { it.boundingBox == null }

        val merged = rows
            .sortedBy { row -> row.minOf { it.boundingBox!!.top } }
            .joinToString("\n") { row ->
                row.sortedBy { it.boundingBox!!.left }
                    .joinToString(" ") { it.text }
            }

        return if (noBbox.isEmpty()) merged
        else merged + "\n" + noBbox.joinToString("\n") { it.text }
    }
}

/**
 * Highlight data for a single matched wine on the photo.
 */
data class WineHighlight(
    val boundingBoxes: List<Rect>,
    val tier: HighlightTier,
    val wineName: String
)

enum class HighlightTier {
    GOLD,
    SILVER,
    BRONZE,
    RED
}
