package com.wineselector.core.scan

/** A visual row of text: OCR lines that sit side by side, read left to right. */
data class TextRow(val text: String, val lineIndices: List<Int>, val box: Box?)

/**
 * Turns OCR lines into rows in reading order. Detects multi-column menus by
 * looking for a vertical gutter with priced text on both sides, so that the
 * left and right columns are read separately instead of being merged into
 * nonsense rows. Full-width lines (titles, section headers) are kept in place.
 */
object LayoutAnalyzer {

    private const val BINS = 120

    fun rows(page: OcrPage): List<TextRow> {
        val indexed = page.lines.withIndex().filter { it.value.text.isNotBlank() }
        val boxed = indexed.filter { it.value.box != null }
        val unboxed = indexed.filter { it.value.box == null }
        if (boxed.isEmpty()) return unboxed.map { TextRow(it.value.text.trim(), listOf(it.index), null) }

        val width = maxOf(page.width, boxed.maxOf { it.value.box!!.right })
        val result = layoutRegion(boxed, width, depth = 0)
        return result + unboxed.map { TextRow(it.value.text.trim(), listOf(it.index), null) }
    }

    private fun layoutRegion(lines: List<IndexedValue<OcrLine>>, width: Int, depth: Int): List<TextRow> {
        val gutter = (if (depth < 2) findGutter(lines, width) else null) ?: return mergeRows(lines)

        val spanning = lines.filter { it.value.box!!.left < gutter && it.value.box!!.right > gutter }
            .sortedBy { it.value.box!!.top }
        val left = lines.filter { it.value.box!!.right <= gutter }
        val right = lines.filter { it.value.box!!.left >= gutter }

        // Split the page into horizontal bands at each full-width line, and read
        // each band's left column then right column.
        val out = mutableListOf<TextRow>()
        var bandTop = Int.MIN_VALUE
        val boundaries = spanning.map { it.value.box!!.centerY } + Int.MAX_VALUE
        var spanIdx = 0
        for (boundary in boundaries) {
            val inBand = { l: IndexedValue<OcrLine> -> l.value.box!!.centerY in bandTop until boundary }
            out += layoutRegion(left.filter(inBand), width, depth + 1)
            out += layoutRegion(right.filter(inBand), width, depth + 1)
            if (spanIdx < spanning.size) {
                val s = spanning[spanIdx++]
                out += TextRow(s.value.text.trim(), listOf(s.index), s.value.box)
            }
            bandTop = boundary
        }
        return out
    }

    /** X position of a column gutter, or null for a single-column layout. */
    private fun findGutter(lines: List<IndexedValue<OcrLine>>, width: Int): Int? {
        if (lines.size < 6 || width <= 0) return null
        val coverage = IntArray(BINS)
        val binW = width.toDouble() / BINS
        for (l in lines) {
            val b = l.value.box!!
            val from = (b.left / binW).toInt().coerceIn(0, BINS - 1)
            val to = ((b.right - 1) / binW).toInt().coerceIn(0, BINS - 1)
            for (x in from..to) coverage[x]++
        }
        val threshold = maxOf(1, (lines.size * 0.05).toInt())
        val candidates = mutableListOf<Pair<Int, Int>>() // (startBin, endBin)
        var start = -1
        for (x in 0 until BINS) {
            val empty = coverage[x] <= threshold
            if (empty && start < 0) start = x
            if ((!empty || x == BINS - 1) && start >= 0) {
                val end = if (empty) x else x - 1
                if (end - start + 1 >= 2 && start > BINS * 0.15 && end < BINS * 0.85) candidates += start to end
                start = -1
            }
        }
        return candidates
            .sortedByDescending { it.second - it.first }
            .map { ((it.first + it.second + 1) / 2.0 * binW).toInt() }
            .firstOrNull { isRealColumnSplit(lines, it) }
    }

    /**
     * A gutter only separates two menu columns if both sides carry their own wine
     * names and prices. The gap between a name column and a price column (or a
     * region column) in a single-column table fails this test.
     */
    private fun isRealColumnSplit(lines: List<IndexedValue<OcrLine>>, x: Int): Boolean {
        val left = lines.filter { it.value.box!!.right <= x }.map { it.value.text }
        val right = lines.filter { it.value.box!!.left >= x }.map { it.value.text }
        fun stats(texts: List<String>): Pair<Int, Int> {
            var priced = 0
            var wordy = 0
            for (t in texts) {
                val split = PriceParser.split(t)
                if (split.prices.isNotEmpty()) priced++
                if (split.text.count { it.isLetter() } >= 4) wordy++
            }
            return priced to wordy
        }
        val (lp, lw) = stats(left)
        val (rp, rw) = stats(right)
        return lp >= 2 && rp >= 2 && lw >= 3 && rw >= 3
    }

    /** Group lines into rows by vertical overlap; each row is read left to right. */
    private fun mergeRows(lines: List<IndexedValue<OcrLine>>): List<TextRow> {
        if (lines.isEmpty()) return emptyList()
        val rows = mutableListOf<MutableList<IndexedValue<OcrLine>>>()
        val rowBoxes = mutableListOf<Box>()
        for (line in lines.sortedBy { it.value.box!!.top }) {
            val box = line.value.box!!
            val idx = rowBoxes.indexOfFirst { rb ->
                val overlap = rb.verticalOverlap(box)
                overlap > minOf(rb.height, box.height) * 0.5 && box.centerY in rb.top..rb.bottom
            }
            if (idx >= 0) {
                rows[idx].add(line)
                rowBoxes[idx] = rowBoxes[idx].union(box)
            } else {
                rows.add(mutableListOf(line))
                rowBoxes.add(box)
            }
        }
        return rows.indices.sortedBy { rowBoxes[it].top }.map { i ->
            val sorted = rows[i].sortedBy { it.value.box!!.left }
            TextRow(sorted.joinToString(" ") { it.value.text.trim() }, sorted.map { it.index }, rowBoxes[i])
        }
    }
}
