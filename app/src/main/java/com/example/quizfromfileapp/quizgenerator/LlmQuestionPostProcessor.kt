package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Post-process danh sách câu hỏi từ LLM.
 *
 * Pipeline chất lượng đầy đủ:
 * 1. Bỏ câu trùng lặp exact (exhibit cùng question text)
 * 2. Bỏ câu có similarity quá cao (>65%) với câu trước
 * 3. RestatementRejector — loại câu chỉ paraphrase nguồn
 * 4. CorrectAnswerRewriter — rewrite đáp án đúng bị copy
 * 5. Bỏ câu mà correct option gần giống nguyên sourceSnippet
 * 6. Bỏ câu có options mất cân bằng nghiêm trọng
 * 7. Bỏ câu có explanation quá dài hoặc copy nguồn
 * 8. Rewrite explanation
 * 9. Option quality: bỏ option 1 từ, quá dài, copy nguyên nguồn
 * 10. Question diversity: không quá 2 câu cùng template/page
 *
 * Nguyên tắc: CHẤP NHẬN ÍT CÂU HƠN thay vì nhét câu rác.
 */
object LlmQuestionPostProcessor {

    private const val TAG = "LlmQuestionPostProcessor"

    // Thresholds
    private const val SIMILARITY_THRESHOLD = 0.65
    private const val SOURCE_SNIPPET_COPY_THRESHOLD = 0.70
    private const val MIN_QUESTION_LENGTH = 15
    private const val MAX_EXPLANATION_CHARS = 200
    private const val MAX_OPTION_LENGTH_RATIO = 2.5  // Option dài nhất không được > 2.5x option ngắn nhất
    private const val MIN_OPTION_LENGTH = 8

    // Diversity thresholds
    private const val MAX_SAME_PAGE_QUESTIONS = 2
    private const val MAX_SAME_TOPIC_QUESTIONS = 2

    /**
     * Process toàn bộ danh sách câu hỏi.
     *
     * @param questions    Danh sách câu hỏi đã parse
     * @param segments     Segments nguồn (để kiểm tra diversity, page distribution)
     * @param sourceSnippets Danh sách source snippets (để check copy)
     * @return             Danh sách đã được clean
     */
    fun process(
        questions: List<QuizQuestion>,
        segments: List<ContentSegment> = emptyList(),
        sourceSnippets: List<String> = emptyList()
    ): List<QuizQuestion> {
        if (questions.isEmpty()) return emptyList()

        var result = questions.toMutableList()

        logd("PostProcess[START]: ${result.size} câu hỏi")

        // Bước 1: Bỏ câu trùng lặp exact
        result = removeExactDuplicates(result)
        logd("After removeExactDuplicates: ${result.size}")

        // Bước 2: Bỏ câu similarity cao (cross-question)
        result = removeHighSimilarityCross(result)
        logd("After removeHighSimilarity: ${result.size}")

        // Bước 3: RestatementRejector — loại câu copy nguyên nguồn
        result = applyRestatementRejector(result)
        logd("After RestatementRejector: ${result.size}")

        // Bước 4: CorrectAnswerRewriter — rewrite đáp án bị copy
        result = rewriteCorrectOptions(result)
        logd("After CorrectAnswerRewriter: ${result.size}")

        // Bước 5: Bỏ câu copy nguyên sourceSnippet (fallback check)
        result = removeVerbatimCopies(result, sourceSnippets)
        logd("After removeVerbatimCopies: ${result.size}")

        // Bước 6: Option quality post-processing
        result = processOptionQuality(result)
        logd("After processOptionQuality: ${result.size}")

        // Bước 7: Rewrite explanation
        result = rewriteExplanations(result, sourceSnippets)
        logd("After rewriteExplanations: ${result.size}")

        // Bước 8: Question diversity — không quá 2 câu cùng page/topic
        result = enforceQuestionDiversity(result, segments)
        logd("After enforceQuestionDiversity: ${result.size}")

        logd("PostProcess[END]: ${result.size} câu hỏi (đã lọc từ ${questions.size})")
        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 1: Remove exact duplicates
    // ─────────────────────────────────────────────────────────────

    private fun removeExactDuplicates(questions: List<QuizQuestion>): MutableList<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()
        val seenTexts = mutableSetOf<String>()

        for (q in questions) {
            val normalized = normalizeForComparison(q.question)
            if (!seenTexts.contains(normalized)) {
                seenTexts.add(normalized)
                result.add(q)
            } else {
                logw("Bỏ trùng lặp exact: ${q.question.take(40)}")
            }
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 2: Remove high similarity cross-question
    // ─────────────────────────────────────────────────────────────

    private fun removeHighSimilarityCross(questions: List<QuizQuestion>): MutableList<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val isSimilar = result.any { prev ->
                SemanticSimilarityHelper.similarity(q.question, prev.question) > SIMILARITY_THRESHOLD ||
                SemanticSimilarityHelper.nGramSimilarity(q.question, prev.question) > SIMILARITY_THRESHOLD
            }
            if (!isSimilar) {
                result.add(q)
            } else {
                logw("Bỏ similarity cao: ${q.question.take(40)}")
            }
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 3: RestatementRejector
    // ─────────────────────────────────────────────────────────────

    private fun applyRestatementRejector(questions: List<QuizQuestion>): MutableList<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val reason = RestatementRejector.shouldReject(q)
            if (reason == null) {
                result.add(q)
            } else {
                logw("RestatementRejector bỏ: ${q.question.take(40)} — ${reason.message}")
            }
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 4: CorrectAnswerRewriter
    // ─────────────────────────────────────────────────────────────

    private fun rewriteCorrectOptions(questions: List<QuizQuestion>): MutableList<QuizQuestion> {
        return questions.mapNotNull { q ->
            val rewritten = RestatementRejector.tryRewrite(q)
            if (rewritten != null) {
                logd("Rewrite OK: '${q.options.getOrNull(q.correctAnswerIndex)?.take(30)}' → '${rewritten.options.getOrNull(rewritten.correctAnswerIndex)?.take(30)}'")
                rewritten
            } else {
                q
            }
        }.toMutableList()
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 5: Remove verbatim copies (fallback)
    // ─────────────────────────────────────────────────────────────

    private fun removeVerbatimCopies(
        questions: List<QuizQuestion>,
        sourceSnippets: List<String>
    ): MutableList<QuizQuestion> {
        if (sourceSnippets.isEmpty()) {
            return questions.toMutableList()
        }

        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val correctOption = q.options.getOrNull(q.correctAnswerIndex) ?: ""
            if (correctOption.isBlank()) {
                result.add(q)
                continue
            }

            // Dùng SemanticSimilarityHelper thay vì textSimilarity đơn giản
            val isVerbatim = sourceSnippets.any { snippet ->
                SemanticSimilarityHelper.similarity(correctOption, snippet) > SOURCE_SNIPPET_COPY_THRESHOLD
            }

            if (!isVerbatim) {
                result.add(q)
            } else {
                logw("Bỏ verbatim copy: '${correctOption.take(40)}'")
            }
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 6: Option quality post-processing
    // ─────────────────────────────────────────────────────────────

    private fun processOptionQuality(questions: List<QuizQuestion>): MutableList<QuizQuestion> {
        return questions.mapNotNull { q ->
            val processed = processOptionQualitySingle(q)
            if (processed != null) processed else {
                logw("Option quality reject: '${q.question.take(40)}'")
                null
            }
        }.toMutableList()
    }

    private fun processOptionQualitySingle(q: QuizQuestion): QuizQuestion? {
        val options = q.options.toMutableList()
        val validOptions = mutableListOf<String>()

        for (opt in options) {
            if (isOptionAcceptable(opt, validOptions, q.sourceSnippet)) {
                validOptions.add(opt)
            }
        }

        // Phải còn đủ 4 options
        if (validOptions.size < 4) {
            return null
        }

        // Cập nhật correctAnswerIndex nếu cần
        val correctOpt = q.options.getOrNull(q.correctAnswerIndex) ?: ""
        val newCorrectIdx = validOptions.indexOf(correctOpt).takeIf { it >= 0 } ?: 0

        return q.copy(
            options = validOptions.take(4),
            correctAnswerIndex = newCorrectIdx
        )
    }

    private fun isOptionAcceptable(
        option: String,
        existingOptions: List<String>,
        sourceSnippet: String
    ): Boolean {
        if (option.isBlank()) return false

        // 1. Không option 1 từ quá ngắn
        val words = option.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size == 1 && words[0].length < MIN_OPTION_LENGTH) {
            logw("Option rejected: 1 từ quá ngắn '$option'")
            return false
        }

        // 2. Không quá dài so với option ngắn nhất trong set
        if (existingOptions.isNotEmpty()) {
            val minLen = existingOptions.minOfOrNull { it.length } ?: 0
            if (minLen > 0 && option.length > minLen * MAX_OPTION_LENGTH_RATIO) {
                logw("Option rejected: quá dài '$option' (min=$minLen)")
                return false
            }
        }

        // 3. Không copy nguyên sourceSnippet dài
        if (sourceSnippet.isNotBlank()) {
            if (SemanticSimilarityHelper.isVerbatimCopy(option, sourceSnippet, 0.65)) {
                logw("Option rejected: copy nguyên nguồn '$option'")
                return false
            }
        }

        // 4. Không trùng với option đã có
        if (existingOptions.any { SemanticSimilarityHelper.similarity(it, option) > 0.85 }) {
            logw("Option rejected: trùng với option khác '$option'")
            return false
        }

        return true
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 7: Rewrite explanation
    // ─────────────────────────────────────────────────────────────

    private fun rewriteExplanations(
        questions: List<QuizQuestion>,
        sourceSnippets: List<String>
    ): MutableList<QuizQuestion> {
        return questions.map { q ->
            val rewritten = CorrectAnswerRewriter.rewriteExplanation(
                explanation = q.explanation,
                sourceSnippet = q.sourceSnippet
            )
            q.copy(explanation = rewritten)
        }.toMutableList()
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 8: Question diversity
    // ─────────────────────────────────────────────────────────────

    private fun enforceQuestionDiversity(
        questions: List<QuizQuestion>,
        segments: List<ContentSegment>
    ): MutableList<QuizQuestion> {
        if (questions.size <= 1) return questions.toMutableList()

        val result = mutableListOf<QuizQuestion>()
        val pageCount = mutableMapOf<Int?, Int>()
        val topicCount = mutableMapOf<String, Int>()

        for (q in questions) {
            val page = q.sourcePageStart

            // Check page diversity
            val currentPageCount = pageCount.getOrDefault(page, 0)
            if (currentPageCount >= MAX_SAME_PAGE_QUESTIONS) {
                logw("Diversity reject: quá $MAX_SAME_PAGE_QUESTIONS câu cùng page $page")
                continue
            }

            // Check topic diversity (dùng sourceSnippet prefix)
            val topicKey = q.sourceSnippet.take(50).lowercase()
            val currentTopicCount = topicCount.getOrDefault(topicKey, 0)
            if (currentTopicCount >= MAX_SAME_TOPIC_QUESTIONS) {
                logw("Diversity reject: quá $MAX_SAME_TOPIC_QUESTIONS câu cùng topic")
                continue
            }

            result.add(q)
            pageCount[page] = currentPageCount + 1
            topicCount[topicKey] = currentTopicCount + 1
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────

    private fun normalizeForComparison(text: String): String {
        return text.lowercase()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .trim()
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }
}
