package com.example.quizfromfileapp.fileparser.extractor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.RawExtractedContent
import com.example.quizfromfileapp.ocr.MlKitOcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ImageOcrExtractor"

class ImageOcrExtractor(private val context: Context) : FileTextExtractor {

    private val ocrEngine by lazy { MlKitOcrEngine(context) }

    override fun canHandle(mimeType: String): Boolean {
        return mimeType in setOf("image/jpeg", "image/png", "image/jpg", "image/webp")
    }

    override suspend fun extract(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): Result<ExtractedContent> = withContext(Dispatchers.IO) {
        Log.d(TAG, "extract: uri=$uri, fileName=$fileName, mimeType=$mimeType")

        // OCR bằng ML Kit
        val ocrResult = ocrEngine.recognizeText(uri)
        val rawText = ocrResult.getOrNull()

        if (rawText == null || rawText.isBlank()) {
            val errorMsg = ocrResult.exceptionOrNull()?.message ?: "Không nhận diện được text từ ảnh"
            Log.w(TAG, "extract: OCR thất bại hoặc trả về rỗng: $errorMsg")
            return@withContext Result.failure(
                IllegalStateException("$errorMsg. Hãy thử ảnh có chất lượng tốt hơn hoặc dùng file văn bản.")
            )
        }

        Log.d(TAG, "extract: OCR thành công, ${rawText.length} ký tự raw")

        // Tạo RawExtractedContent
        val raw = RawExtractedContent(
            sourceUri = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            rawText = rawText,
            rawCharCount = rawText.length,
            totalPages = 1,
            extractedPages = 0,
            ocrPages = 1, // Ảnh = 1 trang, tất cả đều OCR
            emptyPages = emptyList()
        )

        // Clean
        val cleanedResult = try {
            com.example.quizfromfileapp.domain.model.ContentCleaner.clean(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "extract: ContentCleaner.clean thất bại: ${e.message}", e)
            return@withContext Result.failure(
                IllegalStateException("Lỗi làm sạch nội dung: ${e.message}")
            )
        }

        val content = ExtractedContent.fromRaw(raw, cleanedResult.cleanedText, cleanedResult)

        Log.d(TAG, "extract: SUCCESS, rawChars=${rawText.length}, " +
                "cleanedChars=${content.cleanedCharCount}, segments=${content.segmentCount}")

        Result.success(content)
    }
}
