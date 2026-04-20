package com.example.quizfromfileapp.domain.usecase

import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.data.repository.ExportRepository
import com.example.quizfromfileapp.data.repository.StudySetExportData
import com.example.quizfromfileapp.data.repository.StudySetRepositoryRoom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case xử lý import study set từ file .studyset / .json.
 *
 * Quy trình:
 * 1. parseImportFile() — đọc và validate file (chưa tạo gì trong DB)
 * 2. importStudySet() — tạo study set mới trong database
 *
 * Nếu trùng title → tự động thêm "(Bản sao)"
 */
class ImportStudySetUseCase(
    private val exportRepository: ExportRepository,
    private val studySetRepository: StudySetRepositoryRoom
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    /**
     * Bước 1: Parse file và trả về preview data.
     * KHÔNG tạo gì trong database.
     */
    suspend fun parseFile(jsonContent: String): Result<StudySetExportData> {
        return exportRepository.parseImportFile(jsonContent)
    }

    /**
     * Bước 2: Import thực sự vào database.
     * Luôn tạo bản sao mới (không ghi đè).
     * Nếu title trùng → thêm " (Bản sao)" hoặc timestamp.
     */
    suspend fun doImport(exportData: StudySetExportData): Result<ImportResult> {
        return try {
            // Bước 1: tạo StudySet mới
            var newStudySet = exportData.studySet.toEntity()

            // Kiểm tra trùng title → thêm suffix
            newStudySet = ensureUniqueTitle(newStudySet)

            // Bước 2: tạo trong DB
            val newId = studySetRepository.createStudySet(newStudySet)

            // Bước 3: import flashcards
            val flashcards = exportData.toFlashcards(newId)
            if (flashcards.isNotEmpty()) {
                studySetRepository.addFlashcards(flashcards)
            }

            // Bước 4: cập nhật card count
            studySetRepository.updateCardCount(newId, flashcards.size)

            Result.success(
                ImportResult(
                    studySetId = newId,
                    title = newStudySet.title,
                    cardCount = flashcards.size,
                    isDuplicate = newStudySet.title != exportData.studySet.title
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Đọc nội dung file từ Uri (dùng cho document picker).
     * Dùng trong Activity/Composable với ContentResolver.
     */
    suspend fun readFileContent(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri
    ): Result<String> {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Không mở được file"))

            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (content.isBlank()) {
                return Result.failure(IllegalStateException("File trống"))
            }
            Result.success(content)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(IllegalStateException("Lỗi khi đọc file: ${e.message}"))
        }
    }

    /**
     * Tạo title độc nhất bằng cách thêm suffix nếu trùng.
     */
    private suspend fun ensureUniqueTitle(entity: StudySetEntity): StudySetEntity {
        var title = entity.title
        var suffix = 0
        var exists = true

        while (exists) {
            val checkTitle = if (suffix == 0) title else "$title (Bản sao $suffix)"
            val existing = studySetRepository.getStudySetByTitle(checkTitle)
            exists = existing != null
            if (exists) suffix++
        }

        val finalTitle = if (suffix == 0) title else "$title (Bản sao $suffix)"
        return if (finalTitle != entity.title) {
            entity.copy(title = finalTitle)
        } else {
            entity
        }
    }

    companion object {
        // Extension hỗ trợ để check file type
        fun isSupportedFile(uri: android.net.Uri): Boolean {
            val path = uri.path?.lowercase() ?: return false
            return path.endsWith(".studyset") || path.endsWith(".json")
        }

        fun getFileName(uri: android.net.Uri, contentResolver: android.content.ContentResolver): String {
            var name = "study_set"
            uri.lastPathSegment?.let { segment ->
                name = segment.substringAfterLast("/")
                    .substringBefore("?")
                    .ifBlank { "study_set" }
            }
            return name
        }
    }
}

/**
 * Kết quả import — dùng để hiện preview và snackbar.
 */
data class ImportResult(
    val studySetId: Long,
    val title: String,
    val cardCount: Int,
    val isDuplicate: Boolean
)
