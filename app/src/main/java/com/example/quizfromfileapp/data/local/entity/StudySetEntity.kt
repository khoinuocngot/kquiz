package com.example.quizfromfileapp.data.local.entity

import kotlinx.serialization.Serializable

/**
 * Entity cho StudySet — một bộ học tập (flashcard set).
 *
 * Có 2 loại nguồn:
 * - FILE_IMPORTED: tạo từ PDF/TXT file (qua QuizConfig flow)
 * - QUICK_IMPORTED: tạo từ Quick Import text
 *
 * @param id           ID duy nhất
 * @param title        Tiêu đề bộ học
 * @param description  Mô tả (optional)
 * @param cardCount    Số thẻ trong bộ
 * @param sourceType   Nguồn tạo: FILE_IMPORTED / QUICK_IMPORTED
 * @param sourceFileName Tên file gốc (nếu FILE_IMPORTED)
 * @param createdAt    Timestamp tạo
 * @param updatedAt    Timestamp cập nhật cuối
 */
@Serializable
data class StudySetEntity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val cardCount: Int = 0,
    val sourceType: String = SOURCE_TYPE_QUICK_IMPORT,
    val sourceFileName: String = "",
    val studySetType: String = STUDY_SET_TYPE_TERM_DEFINITION,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_TYPE_FILE_IMPORT = "FILE_IMPORTED"
        const val SOURCE_TYPE_QUICK_IMPORT = "QUICK_IMPORTED"

        const val STUDY_SET_TYPE_TERM_DEFINITION = "TERM_DEFINITION"
        const val STUDY_SET_TYPE_QUESTION_ANSWER = "QUESTION_ANSWER"
        const val STUDY_SET_TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
    }

    val sourceTypeLabel: String
        get() = when (sourceType) {
            SOURCE_TYPE_FILE_IMPORT -> "Từ file"
            SOURCE_TYPE_QUICK_IMPORT -> "Nhập nhanh"
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
