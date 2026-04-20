package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity cho FlashcardItem — một thẻ ghi nhớ trong StudySet.
 *
 * Supports multiple study item types:
 * - TERM_DEFINITION: standard flashcard (term → definition)
 * - QUESTION_ANSWER: Q&A card (question → answer)
 * - MULTIPLE_CHOICE: MCQ with choices, correct index, explanation
 *
 * @param id                  ID duy nhất (auto-generate)
 * @param studySetId         ID của StudySet chứa thẻ này
 * @param term               Thuật ngữ / câu hỏi / mặt trước
 * @param definition         Định nghĩa / đáp án / mặt sau
 * @param itemType           Loại item: TERM_DEFINITION / QUESTION_ANSWER / MULTIPLE_CHOICE
 * @param choices            JSON array of choice strings (for MCQ)
 * @param correctChoiceIndex Index của đáp án đúng trong choices (for MCQ)
 * @param explanation        Giải thích thêm (optional)
 * @param sourceSnippet      Dòng raw gốc (để truy vết khi import nhanh)
 * @param sourcePageStart    Trang bắt đầu (cho file import)
 * @param sourcePageEnd      Trang kết thúc
 * @param isStarred          Đánh dấu sao (ghim)
 * @param masteryLevel        Mức độ thành thạo (0-5)
 * @param timesReviewed      Số lần ôn tập
 * @param timesCorrect       Số lần trả lời đúng
 * @param lastReviewedAt     Timestamp ôn tập cuối
 * @param createdAt          Timestamp tạo
 * @param position           Thứ tự trong bộ học (cho manual reordering)
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = StudySetEntityRoom::class,
            parentColumns = ["id"],
            childColumns = ["studySetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studySetId")]
)
data class FlashcardEntityRoom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studySetId: Long,
    val term: String,
    val definition: String,
    val itemType: String = ITEM_TYPE_TERM_DEFINITION,
    val choices: String = "",           // JSON array string, e.g. "[\"A\",\"B\",\"C\",\"D\"]"
    val correctChoiceIndex: Int = -1,  // -1 means not MCQ
    val explanation: String = "",
    val sourceSnippet: String = "",
    val sourcePageStart: Int? = null,
    val sourcePageEnd: Int? = null,
    val isStarred: Boolean = false,
    val masteryLevel: Int = 0,
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0
) {
    companion object {
        const val ITEM_TYPE_TERM_DEFINITION = "TERM_DEFINITION"
        const val ITEM_TYPE_QUESTION_ANSWER = "QUESTION_ANSWER"
        const val ITEM_TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE"

        const val MAX_MASTERY_LEVEL = 5

        // Convert from legacy JSON entity (FlashcardEntity)
        fun fromJsonEntity(entity: FlashcardEntity): FlashcardEntityRoom {
            val itemType = when {
                entity.choices.isNotEmpty() -> ITEM_TYPE_MULTIPLE_CHOICE
                entity.definition.length > entity.term.length * 2 -> ITEM_TYPE_QUESTION_ANSWER
                else -> ITEM_TYPE_TERM_DEFINITION
            }
            return FlashcardEntityRoom(
                id = entity.id,
                studySetId = entity.studySetId,
                term = entity.term,
                definition = entity.definition,
                itemType = itemType,
                choices = entity.choices,
                correctChoiceIndex = entity.correctChoiceIndex,
                explanation = entity.explanation,
                sourceSnippet = entity.sourceSnippet,
                sourcePageStart = entity.sourcePageStart,
                sourcePageEnd = entity.sourcePageEnd,
                isStarred = entity.isStarred,
                masteryLevel = entity.masteryLevel,
                timesReviewed = entity.timesReviewed,
                timesCorrect = entity.timesCorrect,
                lastReviewedAt = entity.lastReviewedAt,
                createdAt = entity.createdAt,
                position = 0
            )
        }
    }

    val accuracyRate: Float
        get() = if (timesReviewed > 0) timesCorrect.toFloat() / timesReviewed else 0f

    val isMultipleChoice: Boolean
        get() = itemType == ITEM_TYPE_MULTIPLE_CHOICE && choices.isNotEmpty()

    val isMastered: Boolean
        get() = masteryLevel >= MAX_MASTERY_LEVEL - 1

    val needsReview: Boolean
        get() = masteryLevel < MAX_MASTERY_LEVEL / 2

    fun withReview(correct: Boolean): FlashcardEntityRoom {
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
