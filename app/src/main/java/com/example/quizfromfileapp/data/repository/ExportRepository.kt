package com.example.quizfromfileapp.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository xử lý export và share study sets ra các format khác nhau.
 * Hỗ trợ export .studyset (JSON), CSV, TXT và import từ file.
 */
class ExportRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ═══════════════════════════════════════════════════════════════
    // EXPORT: .studyset (JSON) — format chính thức của app
    // ═══════════════════════════════════════════════════════════════

    /**
     * Export study set thành file .studyset (JSON).
     * Đây là format chính thức của app, dùng để chia sẻ offline giữa các thiết bị.
     */
    suspend fun exportToStudySetFile(
        studySet: StudySetEntity,
        flashcards: List<FlashcardEntity>
    ): Result<ExportResult> {
        return try {
            val exportData = StudySetExportData.from(studySet, flashcards)
            val jsonContent = json.encodeToString(exportData)

            val safeName = sanitizeFileName(studySet.title)
            val fileName = "${safeName}_${dateFormat.format(Date())}.studyset"

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)

            Result.success(
                ExportResult(
                    file = file,
                    fileName = fileName,
                    format = StudySetExportFormat.STUDYSET,
                    mimeType = "application/json",
                    displayName = studySet.title
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Export thành JSON thường (backup/restore).
     */
    suspend fun exportToJson(
        studySet: StudySetEntity,
        flashcards: List<FlashcardEntity>
    ): Result<ExportResult> {
        return try {
            val exportData = StudySetExportData.from(studySet, flashcards)
            val jsonContent = json.encodeToString(exportData)

            val safeName = sanitizeFileName(studySet.title)
            val fileName = "${safeName}_${dateFormat.format(Date())}.json"

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)

            Result.success(
                ExportResult(
                    file = file,
                    fileName = fileName,
                    format = StudySetExportFormat.JSON,
                    mimeType = "application/json",
                    displayName = studySet.title
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EXPORT: CSV
    // ═══════════════════════════════════════════════════════════════

    suspend fun exportToCsv(
        studySet: StudySetEntity,
        flashcards: List<FlashcardEntity>
    ): Result<ExportResult> {
        return try {
            val sb = StringBuilder()
            sb.append('\uFEFF') // BOM for Excel UTF-8
            sb.appendLine("Term,Definition,Explanation,Mastery Level,Starred,Source Snippet")

            flashcards.forEach { card ->
                sb.append(escapeCsv(card.term))
                sb.append(',')
                sb.append(escapeCsv(card.definition))
                sb.append(',')
                sb.append(escapeCsv(card.explanation))
                sb.append(',')
                sb.append(card.masteryLevel)
                sb.append(',')
                sb.append(if (card.isStarred) "Yes" else "No")
                sb.append(',')
                sb.append(escapeCsv(card.sourceSnippet))
                sb.appendLine()
            }

            val safeName = sanitizeFileName(studySet.title)
            val fileName = "${safeName}_${dateFormat.format(Date())}.csv"

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)

            Result.success(
                ExportResult(
                    file = file,
                    fileName = fileName,
                    format = StudySetExportFormat.CSV,
                    mimeType = "text/csv",
                    displayName = studySet.title
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EXPORT: TXT
    // ═══════════════════════════════════════════════════════════════

    suspend fun exportToTxt(
        studySet: StudySetEntity,
        flashcards: List<FlashcardEntity>
    ): Result<ExportResult> {
        return try {
            val sb = StringBuilder()
            sb.appendLine("=== ${studySet.title} ===")
            if (studySet.description.isNotBlank()) {
                sb.appendLine(studySet.description)
            }
            sb.appendLine()
            sb.appendLine("Exported: ${displayDateFormat.format(Date())}")
            sb.appendLine("Total cards: ${flashcards.size}")
            sb.appendLine()
            sb.appendLine("--- Content ---")

            flashcards.forEach { card ->
                sb.append(card.term)
                sb.append('\t')
                sb.append(card.definition)
                sb.appendLine()
            }

            val safeName = sanitizeFileName(studySet.title)
            val fileName = "${safeName}_${dateFormat.format(Date())}.txt"

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)

            Result.success(
                ExportResult(
                    file = file,
                    fileName = fileName,
                    format = StudySetExportFormat.TXT,
                    mimeType = "text/plain",
                    displayName = studySet.title
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // IMPORT: Parse file content (for preview + actual import)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parse nội dung file để xem trước trước khi import.
     * Không tạo gì trong database.
     */
    suspend fun parseImportFile(jsonContent: String): Result<StudySetExportData> {
        return try {
            // Validate basic structure first
            val jsonTree = json.parseToJsonElement(jsonContent).jsonObject
            if (!jsonTree.containsKey("studySet")) {
                return Result.failure(IllegalArgumentException("File không đúng định dạng study set"))
            }

            val exportData = json.decodeFromString<StudySetExportData>(jsonContent)

            // Validate required fields
            if (exportData.studySet.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Study set không có tiêu đề"))
            }
            if (exportData.flashcards.isEmpty()) {
                return Result.failure(IllegalArgumentException("Study set không có thẻ nào"))
            }

            Result.success(exportData)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("File không hợp lệ hoặc bị hỏng: ${e.message}"))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARE
    // ═══════════════════════════════════════════════════════════════

    fun createShareText(
        studySet: StudySetEntity,
        flashcards: List<FlashcardEntity>,
        maxCards: Int = 20
    ): String {
        val sb = StringBuilder()
        sb.appendLine("📚 ${studySet.title}")
        if (studySet.description.isNotBlank()) {
            sb.appendLine(studySet.description)
        }
        sb.appendLine("───")
        sb.appendLine("(${flashcards.size} thẻ)")
        sb.appendLine()

        val toShow = if (flashcards.size > maxCards) {
            sb.appendLine("(Hiển thị $maxCards/${flashcards.size} thẻ đầu tiên)")
            flashcards.take(maxCards)
        } else {
            flashcards
        }

        toShow.forEachIndexed { index, card ->
            sb.append("${index + 1}. ${card.term}")
            sb.appendLine()
            sb.append("   → ${card.definition}")
            sb.appendLine()
            if (card.explanation.isNotBlank()) {
                sb.append("   💡 ${card.explanation}")
                sb.appendLine()
            }
            sb.appendLine()
        }

        sb.appendLine("───")
        sb.appendLine("Generated by Quiz App")
        return sb.toString()
    }

    fun createShareIntent(content: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createShareFileIntent(exportResult: ExportResult): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportResult.file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = exportResult.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, exportResult.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(50)
            .trim('_')
            .ifBlank { "study_set" }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value
            .replace("\"", "\"\"")
            .replace("\n", " ")
            .replace("\r", "")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains(" ")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// EXPORT FORMAT ENUM
// ══════════════════════════════════════════════════════════════════════
enum class StudySetExportFormat {
    STUDYSET, // JSON with .studyset extension — main sharing format
    JSON,     // Plain JSON backup
    CSV,      // For Excel/Sheets
    TXT       // Plain text
}

// ══════════════════════════════════════════════════════════════════════
// EXPORT RESULT
// ══════════════════════════════════════════════════════════════════════
data class ExportResult(
    val file: File,
    val fileName: String,
    val format: StudySetExportFormat,
    val mimeType: String,
    val displayName: String
)

// ══════════════════════════════════════════════════════════════════════
// STUDY SET EXPORT MODEL
// Format chuẩn để export/import study set giữa các thiết bị
// ══════════════════════════════════════════════════════════════════════
@Serializable
data class StudySetExportData(
    val formatVersion: Int = 1,
    val appVersion: String = "1.0",
    val appName: String = "QuizApp",
    val exportedAt: Long = System.currentTimeMillis(),
    val studySet: StudySetExportInfo,
    val flashcards: List<FlashcardExportInfo>
) {
    companion object {
        fun from(set: StudySetEntity, cards: List<FlashcardEntity>): StudySetExportData {
            return StudySetExportData(
                studySet = StudySetExportInfo.from(set),
                flashcards = cards.map { FlashcardExportInfo.from(it) }
            )
        }
    }

    /**
     * Convert flashcards (FlashcardExportInfo) to domain entities (FlashcardEntity)
     * with the given studySetId.
     */
    fun toFlashcards(studySetId: Long): List<FlashcardEntity> {
        return flashcards.map { it.toEntity(studySetId) }
    }

    fun getPreviewTitle(): String = studySet.title
    fun getPreviewCardCount(): Int = flashcards.size
    fun getPreviewDescription(): String = studySet.description
    fun getPreviewType(): String = studySet.studySetTypeLabel
}

@Serializable
data class StudySetExportInfo(
    val title: String,
    val description: String = "",
    val sourceType: String = "IMPORTED",
    val sourceFileName: String = "",
    val studySetType: String = "TERM_DEFINITION",
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
) {
    companion object {
        fun from(entity: StudySetEntity): StudySetExportInfo {
            return StudySetExportInfo(
                title = entity.title,
                description = entity.description,
                sourceType = entity.sourceType,
                sourceFileName = entity.sourceFileName,
                studySetType = entity.studySetType,
                createdAt = entity.createdAt
            )
        }
    }

    val studySetTypeLabel: String
        get() = when (studySetType) {
            "TERM_DEFINITION" -> "Thuật ngữ - Định nghĩa"
            "QUESTION_ANSWER" -> "Câu hỏi - Đáp án"
            else -> studySetType
        }

    fun toEntity(): StudySetEntity {
        return StudySetEntity(
            title = title,
            description = description,
            sourceType = sourceType,
            sourceFileName = sourceFileName,
            studySetType = studySetType,
            cardCount = 0,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class FlashcardExportInfo(
    val term: String,
    val definition: String,
    val explanation: String = "",
    val choices: String = "",
    val correctChoiceIndex: Int = -1,
    val sourceSnippet: String = "",
    val sourcePageStart: Int? = null,
    val sourcePageEnd: Int? = null,
    val isStarred: Boolean = false,
    val masteryLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun from(entity: FlashcardEntity): FlashcardExportInfo {
            return FlashcardExportInfo(
                term = entity.term,
                definition = entity.definition,
                explanation = entity.explanation,
                choices = entity.choices,
                correctChoiceIndex = entity.correctChoiceIndex,
                sourceSnippet = entity.sourceSnippet,
                sourcePageStart = entity.sourcePageStart,
                sourcePageEnd = entity.sourcePageEnd,
                isStarred = entity.isStarred,
                masteryLevel = entity.masteryLevel,
                createdAt = entity.createdAt
            )
        }
    }

    fun toEntity(studySetId: Long): FlashcardEntity {
        return FlashcardEntity(
            studySetId = studySetId,
            term = term,
            definition = definition,
            explanation = explanation,
            choices = choices,
            correctChoiceIndex = correctChoiceIndex,
            sourceSnippet = sourceSnippet,
            sourcePageStart = sourcePageStart,
            sourcePageEnd = sourcePageEnd,
            isStarred = isStarred,
            masteryLevel = 0, // Reset mastery on import
            timesReviewed = 0,
            timesCorrect = 0,
            lastReviewedAt = null,
            createdAt = createdAt
        )
    }
}
