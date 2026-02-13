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
     */
    suspend fun extractText(photoFile: File, context: android.content.Context): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
            val text = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        cont.resume("")
                    }
            }
            if (text.isBlank()) {
                Result.failure(Exception("Could not read any text from the photo. Try taking a clearer, well-lit photo of the wine list."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to process image: ${e.message}"))
        }
    }
}
