package com.example.quizfromfileapp.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.quizfromfileapp.data.model.ImportedFile
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.fileparser.extractor.FileTextExtractor
import com.example.quizfromfileapp.fileparser.extractor.ImageOcrExtractor
import com.example.quizfromfileapp.fileparser.extractor.PdfTextExtractor
import com.example.quizfromfileapp.fileparser.extractor.TxtTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ExtractTextUseCase"

class ExtractTextFromFileUseCase(private val context: Context) {

    suspend fun execute(file: ImportedFile): Result<ExtractedContent> = withContext(Dispatchers.IO) {
        Log.d(TAG, "execute: file=${file.name}, mimeType=${file.mimeType}, uri=${file.uri}")

        // Kiểm tra URI hợp lệ
        val uri = try {
            Uri.parse(file.uri)
        } catch (e: Exception) {
            Log.e(TAG, "execute: Uri.parse thất bại: ${e.message}")
            return@withContext Result.failure(IllegalStateException("URI file không hợp lệ: ${file.uri}"))
        }

        // Kiểm tra mimeType hợp lệ
        val mimeType = file.mimeType
        if (mimeType.isBlank()) {
            Log.e(TAG, "execute: mimeType rỗng")
            return@withContext Result.failure(IllegalStateException("Không xác định được loại file"))
        }

        // Chọn extractor theo mimeType rõ ràng
        val extractor: FileTextExtractor? = when (mimeType) {
            "text/plain" -> {
                Log.d(TAG, "execute: chọn TxtTextExtractor")
                TxtTextExtractor()
            }
            "application/pdf" -> {
                Log.d(TAG, "execute: chọn PdfTextExtractor")
                PdfTextExtractor(context)
            }
            "image/jpeg", "image/png", "image/jpg", "image/webp" -> {
                Log.d(TAG, "execute: chọn ImageOcrExtractor")
                ImageOcrExtractor(context)
            }
            else -> {
                Log.w(TAG, "execute: mimeType không được hỗ trợ: $mimeType")
                null
            }
        }

        if (extractor == null) {
            return@withContext Result.failure(
                IllegalStateException("Định dạng file chưa được hỗ trợ: $mimeType")
            )
        }

        // Gọi extractor với try/catch bao ngoài
        try {
            extractor.extract(context, uri, file.name, file.mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "execute: extractor.extract thất bại: ${e.message}", e)
            Result.failure(e)
        }
    }
}