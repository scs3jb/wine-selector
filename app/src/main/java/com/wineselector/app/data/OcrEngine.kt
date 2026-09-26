package com.wineselector.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.wineselector.core.scan.Box
import com.wineselector.core.scan.OcrLine
import com.wineselector.core.scan.OcrPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR with ML Kit. Photos are decoded at a bounded size with EXIF
 * rotation applied, so line boxes line up with the photo as displayed. When a
 * first pass finds little text (dim restaurant lighting, glossy labels), a
 * second pass runs on a grayscale, contrast-boosted copy and the better result wins.
 */
class OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(photo: File): OcrPage = withContext(Dispatchers.Default) {
        val bitmap = decode(photo) ?: throw OcrException("Couldn't open the photo. Please try again.")
        try {
            var page = run(bitmap)
            if (page.letterCount() < MIN_LETTERS_FOR_CONFIDENCE) {
                val enhanced = enhance(bitmap)
                try {
                    val second = run(enhanced)
                    if (second.letterCount() > page.letterCount()) page = second
                } finally {
                    enhanced.recycle()
                }
            }
            if (page.letterCount() < MIN_LETTERS) {
                throw OcrException("Couldn't read any text. Move closer, avoid glare and make sure the text is in focus.")
            }
            page
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun run(bitmap: Bitmap): OcrPage {
        val result: Text = suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(OcrException("Text recognition failed: ${it.message}")) }
        }
        val lines = result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val r = line.boundingBox
                OcrLine(line.text, r?.let { Box(it.left, it.top, it.right, it.bottom) })
            }
        }
        return OcrPage(lines, bitmap.width, bitmap.height)
    }

    private fun OcrPage.letterCount() = lines.sumOf { l -> l.text.count { it.isLetter() } }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val raw = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val rotation = runCatching {
            when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (rotation == 0f) return raw
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation) }, true)
        if (rotated !== raw) raw.recycle()
        return rotated
    }

    private fun enhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.6f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        matrix.postConcat(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )))
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        return out
    }

    companion object {
        private const val MAX_DIMENSION = 2800
        private const val MIN_LETTERS = 6
        private const val MIN_LETTERS_FOR_CONFIDENCE = 60
    }
}

class OcrException(message: String) : Exception(message)
