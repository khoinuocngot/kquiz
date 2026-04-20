package com.example.quizfromfileapp.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "MlKitOcrEngine"

class MlKitOcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(uri: Uri): Result<String> = suspendCancellableCoroutine { continuation ->
        var bitmap: Bitmap? = null
        try {
            Log.d(TAG, "recognizeText: uri=$uri")

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "recognizeText: openInputStream trả về null")
                continuation.resume(Result.failure(IllegalStateException("Không thể mở file ảnh: inputStream == null")))
                return@suspendCancellableCoroutine
            }

            bitmap = try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e(TAG, "recognizeText: BitmapFactory.decodeStream thất bại: ${e.message}")
                continuation.resume(Result.failure(IllegalStateException("Không thể giải mã ảnh: ${e.message}")))
                return@suspendCancellableCoroutine
            } finally {
                try { inputStream.close() } catch (_: Exception) { }
            }

            if (bitmap == null) {
                Log.e(TAG, "recognizeText: BitmapFactory.decodeStream trả về null")
                continuation.resume(Result.failure(IllegalStateException("Không thể giải mã ảnh: bitmap == null")))
                return@suspendCancellableCoroutine
            }

            if (bitmap.isRecycled) {
                Log.e(TAG, "recognizeText: bitmap đã bị recycle")
                continuation.resume(Result.failure(IllegalStateException("Ảnh không hợp lệ (đã bị giải phóng)")))
                return@suspendCancellableCoroutine
            }

            val image = try {
                InputImage.fromBitmap(bitmap, 0)
            } catch (e: Exception) {
                Log.e(TAG, "recognizeText: InputImage.fromBitmap thất bại: ${e.message}")
                continuation.resume(Result.failure(IllegalStateException("Tạo InputImage thất bại: ${e.message}")))
                return@suspendCancellableCoroutine
            }

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    Log.d(TAG, "recognizeText: OCR thành công, ${text.length} ký tự")
                    try { bitmap?.recycle() } catch (_: Exception) { }
                    if (text.isBlank()) {
                        continuation.resume(Result.failure(IllegalStateException("Không nhận diện được text từ ảnh. Hãy thử ảnh có chất lượng tốt hơn hoặc dùng file văn bản.")))
                    } else {
                        continuation.resume(Result.success(text))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "recognizeText: MLKit process thất bại: ${e.message}", e)
                    try { bitmap?.recycle() } catch (_: Exception) { }
                    continuation.resume(Result.failure(IllegalStateException("OCR thất bại: ${e.message}")))
                }

        } catch (e: Exception) {
            Log.e(TAG, "recognizeText: EXCEPTION tổng: ${e.javaClass.simpleName}: ${e.message}", e)
            try { bitmap?.recycle() } catch (_: Exception) { }
            continuation.resume(Result.failure(IllegalStateException("Lỗi OCR tổng: ${e.message}")))
        }
    }

    /**
     * OCR từ Bitmap (dùng cho PDF page rendering).
     * Caller chịu trách nhiệm recycle bitmap.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): Result<String> =
        suspendCancellableCoroutine { continuation ->
            try {
                if (bitmap.isRecycled) {
                    continuation.resume(Result.failure(
                        IllegalStateException("Bitmap đã bị recycle")))
                    return@suspendCancellableCoroutine
                }

                val image = try {
                    InputImage.fromBitmap(bitmap, 0)
                } catch (e: Exception) {
                    Log.e(TAG, "recognizeTextFromBitmap: InputImage.fromBitmap thất bại: ${e.message}")
                    continuation.resume(Result.failure(
                        IllegalStateException("Tạo InputImage thất bại: ${e.message}")))
                    return@suspendCancellableCoroutine
                }

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        Log.d(TAG, "recognizeTextFromBitmap: OCR thành công, ${text.length} ký tự")
                        if (text.isBlank()) {
                            continuation.resume(Result.failure(
                                IllegalStateException("Không nhận diện được text từ trang này")))
                        } else {
                            continuation.resume(Result.success(text))
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "recognizeTextFromBitmap: MLKit thất bại: ${e.message}", e)
                        continuation.resume(Result.failure(
                            IllegalStateException("OCR thất bại: ${e.message}")))
                    }

            } catch (e: Exception) {
                Log.e(TAG, "recognizeTextFromBitmap: EXCEPTION: ${e.message}", e)
                continuation.resume(Result.failure(
                    IllegalStateException("Lỗi OCR: ${e.message}")))
            }
        }

    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.w(TAG, "close: recognizer.close() thất bại: ${e.message}")
        }
    }
}