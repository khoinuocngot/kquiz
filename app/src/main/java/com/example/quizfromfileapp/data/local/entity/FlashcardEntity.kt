package com.example.quizfromfileapp.data.local.entity

import kotlinx.serialization.Serializable

/**
 * Entity cho FlashcardItem — một thẻ ghi nhớ trong StudySet.
 *
 * @param id              ID duy nhất
 * @param studySetId     ID của StudySet chứa thẻ này
 * @param term           Thuật ngữ / mặt trước của thẻ
 * @param definition     Định nghĩa / mặt sau của thẻ
 * @param explanation   Giải thích thêm (optional)
 * @param sourceSnippet Dòng raw gốc (để truy vết khi import nhanh)
 * @param sourcePageStart Trang bắt đầu (cho file import)
 * @param sourcePageEnd   Trang kết thúc
 * @param isStarred       Đánh dấu sao
 * @param masteryLevel    Mức độ thành thạo (0-5)
 * @param timesReviewed  Số lần ôn tập
 * @param timesCorrect   Số lần trả lời đúng
 * @param lastReviewedAt Timestamp ôn tập cuối
 * @param createdAt      Timestamp tạo
 */
@Serializable
data class FlashcardEntity(
    val id: Long = 0,
    val studySetId: Long,
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
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_MASTERY_LEVEL = 5
    }

    val accuracyRate: Float
        get() = if (timesReviewed > 0) timesCorrect.toFloat() / timesReviewed else 0f

    val isMultipleChoice: Boolean
        get() = choices.isNotBlank()

    fun withReview(correct: Boolean): FlashcardEntity {
        val newTimesReviewed = timesReviewed + 1
        val newTimesCorrect = if (correct) timesCorrect + 1 else timesCorrect
        val newMastery = when {
            correct && masteryLevel < MAX_MASTERY_LEVEL -> masteryLevel + 1
            !correct && masteryLevel > 0 -> masteryLevel - 1
            else -> masteryLevel
        }
        return copy(
            timesReviewed = newTimesReviewed,
            timesCorrect = newTimesCorrect,
            masteryLevel = newMastery,
            lastReviewedAt = System.currentTimeMillis()
        )
    }
}
