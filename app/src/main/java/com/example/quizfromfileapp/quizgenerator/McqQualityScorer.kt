package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Chấm điểm chất lượng của một QuizQuestion đã sinh từ fact.
 *
 * Reject rules:
 * 1. Correct option copy source quá nhiều
 * 2. Distractors không cùng topic
 * 3. Question quá generic
 * 4. Option set không cân bằng
 * 5. Options trùng nhau hoặc quá giống nhau
 * 6. Explanation quá dài hoặc copy nguồn
 */
object McqQualityScorer {

    private const val TAG = "McqQualityScorer"

    // Thresholds
    private const val CORRECT_COPY_THRESHOLD = 0.60
    private const val DISTRACTOR_TOPIC_THRESHOLD = 0.25
    private const val OPTION_SIMILARITY_THRESHOLD = 0.85
    private const val MIN_QUESTION_LENGTH = 15
    private const val MIN_OPTION_LENGTH = 8
    private const val MAX_EXPLANATION_LENGTH = 200

    /**
     * Kiểm tra câu hỏi có bị reject không.
     *
     * @param question Câu hỏi đã sinh
     * @return null nếu OK, RejectReason nếu reject
     */
    fun shouldReject(question: QuizQuestion): RejectReason? {
        // 1. Question quá ngắn
        if (question.question.length < MIN_QUESTION_LENGTH) {
            return RejectReason("Question quá ngắn (${question.question.length} < $MIN_QUESTION_LENGTH)")
        }

        // 2. Correct option copy source
        val correctOption = question.options.getOrNull(question.correctAnswerIndex) ?: ""
        if (correctOption.isNotBlank() && question.sourceSnippet.isNotBlank()) {
            if (SemanticSimilarityHelper.isVerbatimCopy(
                    correctOption,
                    question.sourceSnippet,
                    CORRECT_COPY_THRESHOLD
                )
            ) {
                return RejectReason("Correct option copy nguồn (${String.format("%.0f", CORRECT_COPY_THRESHOLD * 100)}%+ similar)")
            }
        }

        // 3. Correct option dài hơn sourceSnippet
        if (correctOption.isNotBlank() && question.sourceSnippet.isNotBlank()) {
            if (correctOption.length >= question.sourceSnippet.length) {
                return RejectReason("Correct option không ngắn hơn sourceSnippet")
            }
        }

        // 4. Options trùng nhau
        val uniqueOptions = question.options.toSet()
        if (uniqueOptions.size < question.options.size) {
            return RejectReason("Options trùng nhau (${uniqueOptions.size}/4 unique)")
        }

        // 5. Options quá giống nhau
        for (i in question.options.indices) {
            for (j in i + 1 until question.options.size) {
                val sim = SemanticSimilarityHelper.similarity(
                    question.options[i],
                    question.options[j]
                )
                if (sim > OPTION_SIMILARITY_THRESHOLD) {
                    return RejectReason("Options[$i] và[$j] quá giống nhau (${String.format("%.0f", sim * 100)}%)")
                }
            }
        }

        // 6. Option quá ngắn
        for ((i, opt) in question.options.withIndex()) {
            if (opt.length < MIN_OPTION_LENGTH) {
                return RejectReason("Option[$i] quá ngắn (${opt.length} < $MIN_OPTION_LENGTH)")
            }
        }

        // 7. Options mất cân bằng nghiêm trọng (max > 2.5x min)
        val lengths = question.options.map { it.length }
        val minLen = lengths.minOrNull() ?: 0
        val maxLen = lengths.maxOrNull() ?: 0
        if (minLen > 0 && maxLen > minLen * 2.5) {
            return RejectReason("Options mất cân bằng (min=$minLen max=$maxLen)")
        }

        // 8. Explanation quá dài
        if (question.explanation.length > MAX_EXPLANATION_LENGTH) {
            return RejectReason("Explanation quá dài (${question.explanation.length} > $MAX_EXPLANATION_LENGTH)")
        }

        // 9. Explanation copy nguồn
        if (question.explanation.isNotBlank() && question.sourceSnippet.isNotBlank()) {
            val expSim = SemanticSimilarityHelper.similarity(question.explanation, question.sourceSnippet)
            if (expSim > 0.75) {
                return RejectReason("Explanation copy nguồn quá nhiều (${String.format("%.0f", expSim * 100)}%)")
            }
        }

        // 10. Distractors không cùng topic với question
        val topicSim = SemanticSimilarityHelper.tokenBasedSimilarity(
            question.question,
            question.sourceSnippet
        )
        if (topicSim < 0.10 && question.sourceSnippet.isNotBlank()) {
            logw("Warning: question có thể không bám nguồn (topic sim=${String.format("%.2f", topicSim)})")
        }

        // 11. Question quá generic (check patterns)
        if (isGenericQuestion(question.question)) {
            return RejectReason("Question quá generic")
        }

        return null
    }

    /**
     * Kiểm tra question có phải là generic không.
     */
    private fun isGenericQuestion(question: String): Boolean {
        val genericPatterns = listOf(
            Regex("""^(which|what|who|when|where|how)\s+(of\s+the\s+following\s+)?is\s+.*\?\s*$""", RegexOption.IGNORE_CASE),
            Regex("""^đoạn\s+văn\s+nói\s+về.*$""", RegexOption.IGNORE_CASE),
            Regex("""^theo\s+đoạn\s+văn.*$""", RegexOption.IGNORE_CASE),
            Regex("""^câu\s+nào\s+sau\s+đây.*$""", RegexOption.IGNORE_CASE)
        )

        return genericPatterns.any { it.matches(question.trim()) }
    }

    /**
     * Lọc và rank câu hỏi theo quality.
     */
    fun filterQuestions(questions: List<QuizQuestion>): List<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val reason = shouldReject(q)
            if (reason == null) {
                result.add(q)
            } else {
                logw("McqQualityScorer reject: '${q.question.take(40)}' — ${reason.message}")
            }
        }

        logd("McqQualityScorer: ${questions.size} → ${result.size} câu")
        return result
    }

    /**
     * Tính quality score tổng hợp cho quiz.
     */
    fun scoreQuiz(questions: List<QuizQuestion>): Float {
        if (questions.isEmpty()) return 0f

        var totalScore = 0f
        var validCount = 0

        for (q in questions) {
            val reason = shouldReject(q)
            if (reason == null) {
                validCount++
                // Score cho câu hỏi OK = 1.0
                totalScore += 1.0f
            }
        }

        return if (questions.isNotEmpty()) {
            totalScore / questions.size
        } else 0f
    }

    /**
     * Thống kê quality của quiz.
     */
    fun getQualityReport(questions: List<QuizQuestion>): QualityReport {
        val rejected = questions.mapNotNull { q ->
            shouldReject(q)?.let { q to it }
        }

        val copySource = rejected.count { it.second.message.contains("copy") }
        val generic = rejected.count { it.second.message.contains("generic") }
        val similar = rejected.count { it.second.message.contains("giống") || it.second.message.contains("similar") }
        val short = rejected.count { it.second.message.contains("ngắn") }
        val unbalanced = rejected.count { it.second.message.contains("cân bằng") }

        return QualityReport(
            totalQuestions = questions.size,
            validQuestions = questions.size - rejected.size,
            rejectedByCopySource = copySource,
            rejectedByGeneric = generic,
            rejectedBySimilarOptions = similar,
            rejectedByTooShort = short,
            rejectedByUnbalanced = unbalanced,
            qualityScore = scoreQuiz(questions)
        )
    }

    data class RejectReason(val message: String)

    data class QualityReport(
        val totalQuestions: Int,
        val validQuestions: Int,
        val rejectedByCopySource: Int,
        val rejectedByGeneric: Int,
        val rejectedBySimilarOptions: Int,
        val rejectedByTooShort: Int,
        val rejectedByUnbalanced: Int,
        val qualityScore: Float
    )

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }
}
