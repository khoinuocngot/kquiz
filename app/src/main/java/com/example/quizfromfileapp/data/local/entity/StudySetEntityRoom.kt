package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity cho StudySet — một bộ học tập (flashcard set).
 *
 * Migration strategy: đọc JSON cũ từ StudySetStorage,
 * convert sang entity này, insert vào Room.
 *
 * @param id             ID duy nhất (auto-generate)
 * @param title          Tiêu đề bộ học
 * @param description    Mô tả (optional)
 * @param cardCount      Số thẻ trong bộ (denormalized for performance)
 * @param sourceType     Nguồn tạo: FILE_IMPORTED / QUICK_IMPORTED / GENERATED
 * @param sourceFileName Tên file gốc (nếu FILE_IMPORTED)
 * @param studySetType   Loại bộ học: TERM_DEFINITION / QUESTION_ANSWER / MULTIPLE_CHOICE
 * @param isPinned       Đánh dấu ghim (pin to top)
 * @param isFavorite     Đánh dấu sao (favorite)
 * @param folderId       ID thư mục chứa (null = root)
 * @param createdAt      Timestamp tạo
 * @param updatedAt      Timestamp cập nhật cuối
 */
@Entity(tableName = "study_sets")
data class StudySetEntityRoom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val cardCount: Int = 0,
    val sourceType: String = SOURCE_TYPE_QUICK_IMPORT,
    val sourceFileName: String = "",
    val studySetType: String = STUDY_SET_TYPE_TERM_DEFINITION,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_TYPE_FILE_IMPORT = "FILE_IMPORTED"
        const val SOURCE_TYPE_QUICK_IMPORT = "QUICK_IMPORTED"
        const val SOURCE_TYPE_GENERATED = "GENERATED"

        const val STUDY_SET_TYPE_TERM_DEFINITION = "TERM_DEFINITION"
        const val STUDY_SET_TYPE_QUESTION_ANSWER = "QUESTION_ANSWER"
        const val STUDY_SET_TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE"

        // Convert from legacy JSON entity (StudySetEntity)
        fun fromJsonEntity(entity: StudySetEntity): StudySetEntityRoom {
            return StudySetEntityRoom(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                cardCount = entity.cardCount,
                sourceType = entity.sourceType,
                sourceFileName = entity.sourceFileName,
                studySetType = entity.studySetType,
                isPinned = entity.isPinned,
                isFavorite = entity.isFavorite,
                folderId = null,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    // Legacy compatibility — convert back to JSON entity if needed
    fun toJsonEntity(): StudySetEntity {
        return StudySetEntity(
            id = id,
            title = title,
            description = description,
            cardCount = cardCount,
            sourceType = sourceType,
            sourceFileName = sourceFileName,
            studySetType = studySetType,
            isPinned = isPinned,
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    val sourceTypeLabel: String
        get() = when (sourceType) {
            SOURCE_TYPE_FILE_IMPORT -> "Từ file"
            SOURCE_TYPE_QUICK_IMPORT -> "Nhập nhanh"
            SOURCE_TYPE_GENERATED -> "Tạo bằng AI"
            else -> sourceType
        }

    val studySetTypeLabel: String
        get() = when (studySetType) {
            STUDY_SET_TYPE_TERM_DEFINITION -> "Thuật ngữ - Định nghĩa"
            STUDY_SET_TYPE_QUESTION_ANSWER -> "Câu hỏi - Đáp án"
            STUDY_SET_TYPE_MULTIPLE_CHOICE -> "Trắc nghiệm"
            else -> studySetType
        }
}
