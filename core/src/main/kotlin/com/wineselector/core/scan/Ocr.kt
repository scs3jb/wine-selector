package com.wineselector.core.scan

/** Axis-aligned bounding box in image pixel coordinates. */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    fun verticalOverlap(other: Box): Int = minOf(bottom, other.bottom) - maxOf(top, other.top)

    fun union(other: Box) = Box(
        minOf(left, other.left), minOf(top, other.top),
        maxOf(right, other.right), maxOf(bottom, other.bottom)
    )
}

/** One line of recognised text. [box] may be null for synthetic/plain-text input. */
data class OcrLine(val text: String, val box: Box? = null)

/** Recognised text for one photo, independent of the OCR engine. */
data class OcrPage(val lines: List<OcrLine>, val width: Int, val height: Int) {
    val fullText: String get() = lines.joinToString("\n") { it.text }

    companion object {
        /**
         * Build a page from plain text, one OCR line per text line, laid out
         * top-to-bottom in a single column. Used by tests and for pasted text.
         */
        fun fromPlainText(text: String, lineHeight: Int = 20): OcrPage {
            val rows = text.lines().filter { it.isNotBlank() }
            val lines = rows.mapIndexed { i, row ->
                val top = i * lineHeight * 2
                OcrLine(row.trim(), Box(0, top, row.length * 10, top + lineHeight))
            }
            val width = (rows.maxOfOrNull { it.length } ?: 1) * 10
            return OcrPage(lines, width, rows.size * lineHeight * 2)
        }
    }
}
