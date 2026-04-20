package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String = "",

    /** Trang bắt đầu nguồn (1-based), null nếu không xác định */
    val sourcePageStart: Int? = null,

    /** Trang kết thúc nguồn (1-based), null nếu không xác định */
    val sourcePageEnd: Int? = null,

    /** Đoạn trích nguyên gốc từ tài liệu — để truy vết */
    val sourceSnippet: String = "",

    /** Loại nguồn: TEXT_LAYER / OCR / MERGED */
    val sourceType: String = ContentSegment.SOURCE_TYPE_MERGED
) {
    /** Nhãn trang để hiển thị UI */
    val sourcePageLabel: String
        get() = when {
            sourcePageStart != null && sourcePageEnd != null && sourcePageStart != sourcePageEnd ->
                "Trang $sourcePageStart–$sourcePageEnd"
            sourcePageStart != null -> "Trang $sourcePageStart"
            else -> "Nguồn không xác định"
        }
}

@Serializable
data class QuizSession(
    val fileName: String,
    val questionCount: Int,
    val difficulty: String,
    val questionType: String,
    val questions: List<QuizQuestion>,
    val generationWarning: String? = null
) {
    /** Số câu hỏi từ text layer */
    val fromTextLayer: Int get() = questions.count {
        it.sourceType == ContentSegment.SOURCE_TYPE_TEXT_LAYER
    }

    /** Số câu hỏi từ OCR */
    val fromOcr: Int get() = questions.count {
        it.sourceType == ContentSegment.SOURCE_TYPE_OCR
    }

    /** Số câu hỏi từ merged */
    val fromMerged: Int get() = questions.count {
        it.sourceType == ContentSegment.SOURCE_TYPE_MERGED
    }
}
