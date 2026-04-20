package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Sinh distractor từ nội dung cùng topic/page, không lấy bừa.
 *
 * Nguyên tắc:
 * - Distractor phải cùng chủ đề với câu hỏi
 * - Distractor phải sai vừa phải, không quá vô lý
 * - Distractor phải đủ khác correct option
 * - Distractor phải plausible — học sinh có thể nhầm nếu không đọc kỹ
 *
 * Kỹ thuật:
 * 1. Lấy segments cùng page/topic gần
 * 2. Tách key terms từ segment nguồn
 * 3. Tạo distractor bằng cách biến đổi key term
 * 4. Validate: distractor phải cùng ngữ cảnh, khác nghĩa đúng
 */
object TopicScopedDistractorBuilder {

    private const val TAG = "TopicScopedDistractorBuilder"

    // Số distractors cần sinh mỗi câu hỏi
    private const val DISTRACTORS_PER_QUESTION = 3

    // Ngưỡng similarity: distractor phải khác correct option
    private const val DISTRACTOR_CORRECT_DIFF_THRESHOLD = 0.50

    // Ngưỡng similarity: distractor phải cùng topic với question
    private const val DISTRACTOR_TOPIC_MATCH_THRESHOLD = 0.25

    /**
     * Build distractors cho câu hỏi từ nội dung cùng topic.
     *
     * @param question        Câu hỏi cần sinh distractors
     * @param sourceSegments  Tất cả segments (để lấy cùng topic)
     * @param topicGroups     Nhóm topic (để lấy cùng page)
     * @param sourceSegment   Segment gốc của câu hỏi
     * @return Danh sách 3 distractors hoặc danh sách rỗng nếu không sinh được
     */
    fun buildDistractors(
        question: QuizQuestion,
        sourceSegments: List<ContentSegment>,
        topicGroups: List<SegmentTopicGroup>,
        sourceSegment: ContentSegment?
    ): List<String> {
        val correctOption = question.options.getOrNull(question.correctAnswerIndex) ?: ""
        if (correctOption.isBlank()) return emptyList()

        // Lấy candidates từ cùng topic/page
        val candidates = getTopicCandidates(
            sourceSegment,
            sourceSegments,
            topicGroups
        )

        if (candidates.isEmpty()) {
            logw("Không có candidates cùng topic cho: '${question.question.take(30)}'")
            return emptyList()
        }

        // Extract key terms từ correct option để tránh sinh trùng
        val correctTerms = extractKeyTerms(correctOption)

        val distractors = mutableListOf<String>()

        for (candidate in candidates) {
            if (distractors.size >= DISTRACTORS_PER_QUESTION) break

            val distractor = generateDistractorFromCandidate(
                candidateText = candidate,
                correctOption = correctOption,
                question = question.question,
                correctTerms = correctTerms
            )

            if (distractor != null && isValidDistractor(distractor, correctOption, question.question)) {
                distractors.add(distractor)
            }
        }

        // Nếu không đủ 3 distractors tốt → fallback generic distractors
        while (distractors.size < DISTRACTORS_PER_QUESTION) {
            val fallback = generateGenericDistractor(distractors, question.question, correctOption)
            if (fallback != null) {
                distractors.add(fallback)
            } else {
                break
            }
        }

        logd("buildDistractors: ${distractors.size} distractors cho: '${question.question.take(30)}'")
        return distractors.take(DISTRACTORS_PER_QUESTION)
    }

    // ─────────────────────────────────────────────────────────────
    // Lấy candidates cùng topic
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy segments cùng topic/page làm nguồn distractors.
     */
    private fun getTopicCandidates(
        sourceSegment: ContentSegment?,
        allSegments: List<ContentSegment>,
        topicGroups: List<SegmentTopicGroup>
    ): List<String> {
        val candidates = mutableListOf<String>()

        if (sourceSegment != null) {
            // Lấy từ cùng group
            val sameGroup = SegmentTopicGrouper.getSameGroupSegments(sourceSegment, topicGroups)
            candidates.addAll(sameGroup.map { it.text.take(150) })

            // Lấy từ page gần
            val nearby = SegmentTopicGrouper.getNearbySegments(sourceSegment, allSegments, 5)
            candidates.addAll(nearby.map { it.text.take(150) })
        }

        // Fallback: lấy từ page cùng loại
        if (candidates.isEmpty() && sourceSegment?.sourcePageStart != null) {
            val samePage = allSegments.filter {
                it.sourcePageStart == sourceSegment.sourcePageStart && it.id != sourceSegment.id
            }
            candidates.addAll(samePage.map { it.text.take(150) })
        }

        // Nếu vẫn không có → lấy random nhưng cùng độ dài
        if (candidates.isEmpty()) {
            candidates.addAll(
                allSegments
                    .filter { it.id != sourceSegment?.id }
                    .shuffled()
                    .take(5)
                    .map { it.text.take(150) }
            )
        }

        return candidates.distinct()
    }

    // ─────────────────────────────────────────────────────────────
    // Sinh distractor từ candidate
    // ─────────────────────────────────────────────────────────────

    /**
     * Sinh distractor từ candidate text cùng topic.
     */
    private fun generateDistractorFromCandidate(
        candidateText: String,
        correctOption: String,
        question: String,
        correctTerms: Set<String>
    ): String? {
        val sentences = candidateText.split(Regex("""[.!?]+""")).filter { it.trim().length > 10 }
        if (sentences.isEmpty()) return null

        // Chọn sentence ngẫu nhiên nhưng khác correct option
        for (sentence in sentences.shuffled()) {
            val trimmed = sentence.trim()

            // Skip nếu trùng correct option
            if (SemanticSimilarityHelper.similarity(trimmed, correctOption) > 0.6) {
                continue
            }

            // Skip nếu trùng terms với correct
            val terms = extractKeyTerms(trimmed)
            if (terms.intersect(correctTerms).size > terms.size * 0.5) {
                continue
            }

            // Rút gọn nếu quá dài
            val distractor = if (trimmed.length > correctOption.length * 1.5) {
                trimmed.take((correctOption.length * 1.2).toInt().coerceAtMost(100))
            } else {
                trimmed
            }

            if (distractor.length >= 8) {
                return distractor
            }
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────
    // Sinh generic distractor fallback
    // ─────────────────────────────────────────────────────────────

    /**
     * Sinh generic distractor khi không có candidate cùng topic.
     */
    private fun generateGenericDistractor(
        existingDistractors: List<String>,
        question: String,
        correctOption: String
    ): String? {
        val genericEN = listOf(
            "A completely unrelated concept from a different field",
            "An outdated or incorrect interpretation of the topic",
            "A partially correct but fundamentally flawed answer",
            "A distractor that sounds plausible but misses the key point"
        )

        val genericVI = listOf(
            "Một khái niệm không liên quan từ lĩnh vực khác",
            "Một diễn giải lỗi thời hoặc không chính xác",
            "Một đáp án gần đúng nhưng thiếu điểm mấu chốt",
            "Một lựa chọn nghe hợp lý nhưng sai về bản chất"
        )

        val candidates = if (question.contains(Regex("""[àáạảãâầấậẩẫăằắặẳẹèéẹẻẽêềếệểễìíịỉĩọòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]"""))) {
            genericVI
        } else {
            genericEN
        }

        return candidates
            .filter { !existingDistractors.contains(it) }
            .filter { SemanticSimilarityHelper.similarity(it, correctOption) < 0.4 }
            .randomOrNull()
    }

    // ─────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────

    /**
     * Validate distractor:
     * - Khác correct option đủ nhiều
     * - Cùng topic với câu hỏi
     * - Không trùng với distractor đã có
     */
    private fun isValidDistractor(
        distractor: String,
        correctOption: String,
        question: String
    ): Boolean {
        if (distractor.isBlank() || distractor.length < 5) {
            return false
        }

        // Phải khác correct option
        val diffSim = SemanticSimilarityHelper.similarity(distractor, correctOption)
        if (diffSim > DISTRACTOR_CORRECT_DIFF_THRESHOLD) {
            logw("isValidDistractor: FAIL — quá giống correct (${String.format("%.2f", diffSim)})")
            return false
        }

        // Phải cùng topic với question
        val topicSim = SemanticSimilarityHelper.tokenBasedSimilarity(distractor, question)
        if (topicSim < DISTRACTOR_TOPIC_MATCH_THRESHOLD) {
            logw("isValidDistractor: FAIL — không cùng topic ($topicSim)")
            return false
        }

        return true
    }

    // ─────────────────────────────────────────────────────────────
    // Key term extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Trích key terms từ text (danh từ, tính từ, động từ quan trọng).
     */
    private fun extractKeyTerms(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "can", "that", "which", "who", "this", "these", "those",
            "it", "its", "of", "in", "to", "for", "on", "with", "at", "by", "from",
            "và", "là", "của", "trong", "được", "có", "không", "để", "từ", "với",
            "cho", "này", "khi", "đã", "sẽ", "mà", "ra", "vào", "ở", "theo"
        )

        return text
            .lowercase()
            .split(Regex("""[\s\p{Punct}]+"""))
            .filter { it.length >= 4 && !stopWords.contains(it) }
            .toSet()
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
