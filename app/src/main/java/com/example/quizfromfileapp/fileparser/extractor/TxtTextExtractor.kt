package com.example.quizfromfileapp.fileparser.extractor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.RawExtractedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "TxtTextExtractor"

class TxtTextExtractor : FileTextExtractor {

    override fun canHandle(mimeType: String): Boolean = mimeType == "text/plain"

    override suspend fun extract(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): Result<ExtractedContent> = withContext(Dispatchers.IO) {
        Log.d(TAG, "extract: uri=$uri, fileName=$fileName")

        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "extract: openInputStream trả về null")
            return@withContext Result.failure(
                IllegalStateException("Không thể mở file: inputStream == null")
            )
        }

        val rawText = inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }

        Log.d(TAG, "extract: đọc được ${rawText.length} ký tự raw")

        if (rawText.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("File không có nội dung văn bản")
            )
        }

        // Tạo RawExtractedContent
        val raw = RawExtractedContent(
            sourceUri = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            rawText = rawText,
            rawCharCount = rawText.length,
            totalPages = null, // TXT không có khái niệm trang
            extractedPages = null,
            ocrPages = 0,
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
