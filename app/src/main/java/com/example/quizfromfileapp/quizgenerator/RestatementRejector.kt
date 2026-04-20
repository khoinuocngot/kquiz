package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Reject câu hỏi chỉ là paraphrase/restatement y hệt nguồn.
 *
 * Loại bỏ:
 * - Câu hỏi mà question text gần như copy sourceSnippet
 * - Câu hỏi mà correct option gần như copy sourceSnippet
 * - Câu hỏi mà explanation copy nguyên sourceSnippet dài
 * - Câu hỏi chỉ đổi 1-2 từ so với nguồn
 *
 * Nguyên tắc: thay vì reject, có thể rewrite nếu có thể.
 */
object RestatementRejector {

    private const val TAG = "RestatementRejector"

    // Thresholds
    private const val QUESTION_RESTATEMENT_THRESHOLD = 0.70
    private const val CORRECT_OPTION_COPY_THRESHOLD = 0.60
    private const val MIN_CORRECT_OPTION_LENGTH = 10
    private const val MAX_CORRECT_OPTION_RATIO = 0.85  // correctOption không được dài hơn 85% sourceSnippet

    /**
     * Kiểm tra xem câu hỏi có bị reject không.
     * Trả về null nếu OK, hoặc lý do reject nếu loại.
     */
    fun shouldReject(question: QuizQuestion): RejectReason? {
        if (question.question.isBlank()) {
            return RejectReason("Câu hỏi rỗng")
        }

        if (question.sourceSnippet.isBlank()) {
            // Không có nguồn để so sánh → chấp nhận
            return null
        }

        val correctOption = question.options.getOrNull(question.correctAnswerIndex) ?: ""

        // Check 1: Correct option copy nguyên sourceSnippet
        if (correctOption.isNotBlank()) {
            if (SemanticSimilarityHelper.isVerbatimCopy(
                    correctOption,
                    question.sourceSnippet,
                    CORRECT_OPTION_COPY_THRESHOLD
                )
            ) {
                logw("Reject: correctOption copy nguyên sourceSnippet")
                return RejectReason(
                    "Đáp án đúng gần như copy nguyên nguồn (${String.format("%.0f", CORRECT_OPTION_COPY_THRESHOLD * 100)}% similar)"
                )
            }

            // Check 1b: Correct option quá dài so với sourceSnippet
            val ratio = correctOption.length.toDouble() / maxOf(question.sourceSnippet.length, 1)
            if (ratio > MAX_CORRECT_OPTION_RATIO) {
                logw("Reject: correctOption quá dài (${String.format("%.0f", ratio * 100)}% của nguồn)")
                return RejectReason("Đáp án đúng quá dài so với nguồn")
            }

            // Check 1c: Correct option phải ngắn hơn sourceSnippet rõ ràng
            if (correctOption.length >= question.sourceSnippet.length) {
                logw("Reject: correctOption không ngắn hơn sourceSnippet")
                return RejectReason("Đáp án đúng phải ngắn hơn đoạn nguồn")
            }
        }

        // Check 2: Question là restatement của sourceSnippet
        if (SemanticSimilarityHelper.isQuestionRestatement(
                question.question,
                question.sourceSnippet,
                QUESTION_RESTATEMENT_THRESHOLD
            )
        ) {
            logw("Reject: question là restatement của sourceSnippet")
            return RejectReason("Câu hỏi chỉ paraphrase nguồn gốc")
        }

        // Check 3: Options trùng lặp
        val uniqueOptions = question.options.toSet()
        if (uniqueOptions.size < question.options.size) {
            logw("Reject: options trùng nhau")
            return RejectReason("Các đáp án trùng lặp")
        }

        // Check 4: Option quá ngắn
        for ((i, opt) in question.options.withIndex()) {
            if (opt.length < MIN_CORRECT_OPTION_LENGTH) {
                logw("Reject: option[$i] quá ngắn (${opt.length} chars)")
                return RejectReason("Đáp án quá ngắn")
            }
        }

        return null
    }

    /**
     * Kiểm tra từng câu hỏi, trả về danh sách chỉ gồm câu không bị reject.
     */
    fun filterQuestions(questions: List<QuizQuestion>): List<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val reason = shouldReject(q)
            if (reason == null) {
                result.add(q)
            } else {
                logw("Bỏ câu: ${q.question.take(40)} — lý do: ${reason.message}")
            }
        }

        logd("RestatementRejector: ${questions.size} → ${result.size} câu (đã lọc)")
        return result
    }

    /**
     * Rewrite đáp án đúng nếu bị copy.
     * Trả về null nếu không rewrite được, hoặc QuizQuestion đã rewrite.
     */
    fun tryRewrite(question: QuizQuestion): QuizQuestion? {
        val correctOption = question.options.getOrNull(question.correctAnswerIndex) ?: return null

        if (!SemanticSimilarityHelper.isVerbatimCopy(correctOption, question.sourceSnippet, 0.60)) {
            return null
        }

        // Thử paraphrase
        val rewritten = CorrectAnswerRewriter.rewriteCorrectOption(correctOption, question.sourceSnippet)
        if (rewritten != null && rewritten != correctOption) {
            logd("Rewrite correctOption: '${correctOption.take(40)}' → '${rewritten.take(40)}'")
            val newOptions = question.options.toMutableList()
            newOptions[question.correctAnswerIndex] = rewritten
            return question.copy(options = newOptions)
        }

        return null
    }

    /**
     * Tỷ lệ question vs source — dùng để đánh giá xem câu hỏi có quá sát nguồn không.
     */
    fun questionToSourceRatio(question: String, sourceSnippet: String): Double {
        if (question.isBlank() || sourceSnippet.isBlank()) return 0.0
        return SemanticSimilarityHelper.tokenBasedSimilarity(question, sourceSnippet)
    }

    data class RejectReason(val message: String)

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }
}
