package com.wineselector.app.data

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class TextRecognitionService {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from a photo file using ML Kit on-device text recognition.
     * Returns an OcrResult with the full text, per-line bounding boxes, and image dimensions.
     */
    suspend fun extractText(photoFile: File, context: android.content.Context): Result<OcrResult> {
        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
            val ocrResult = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val ocrLines = mutableListOf<OcrLine>()
                        for (block in result.textBlocks) {
                            for (line in block.lines) {
                                ocrLines.add(OcrLine(line.text, line.boundingBox))
                            }
                        }
                        cont.resume(
                            OcrResult(
                                fullText = result.text,
                                lines = ocrLines,
                                imageWidth = image.width,
                                imageHeight = image.height
                            )
                        )
                    }
                    .addOnFailureListener { _ ->
                        cont.resume(OcrResult("", emptyList(), 0, 0))
                    }
            }
            if (ocrResult.fullText.isBlank()) {
                Result.failure(Exception("Could not read any text from the photo. Try taking a clearer, well-lit photo of the wine list."))
            } else {
                Result.success(ocrResult)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to process image: ${e.message}"))
        }
    }
}
