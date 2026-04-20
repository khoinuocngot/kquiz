package com.example.quizfromfileapp.domain.model

/**
 * Blacklist cho generic fallback question và option.
 *
 * BẤT KỲ câu hỏi hoặc đáp án nào khớp với blacklist → REJECT NGAY.
 * KHÔNG fallback, KHÔNG tạo bất kỳ câu nào trong danh sách này.
 */
object GenericFallbackBlacklist {

    // ─────────────────────────────────────────────────────────────
    // Blacklist câu hỏi fallback
    // ─────────────────────────────────────────────────────────────

    private val forbiddenQuestionFragments = listOf(
        "Based on the passage, which statement is correct?",
        "Based on the passage, which description is accurate?",
        "Based on the passage, which statement is most accurate?",
        "Based on the passage, what can be said about this concept?",
        "According to the passage, which statement is correct?",
        "According to the content, which of the following is true?",
        "Review the content above",
        "Review the content above to answer",
        "Review the content above to answer this question",
        "Based on the passage",
        "Based on the passage, which",
        "According to the passage",
        "Based on the content",
        "The passage presents",
        "The content covers",
        "The main ideas are",
        "The information is structured",
        "The passage provides",
        "The content provides",
        "This passage discusses",
        "This content discusses",
        "Auto-generated question",
        "Auto-generated",
    )

    // ─────────────────────────────────────────────────────────────
    // Blacklist đáp án fallback
    // ─────────────────────────────────────────────────────────────

    private val forbiddenOptionFragments = listOf(
        "The passage presents factual information",
        "The passage presents factual information about the topic",
        "The content covers multiple related concepts",
        "The content covers multiple related",
        "The main ideas are explained with supporting details",
        "The main ideas are explained",
        "The information is structured in an organized manner",
        "The information is structured",
        "Auto-generated question",
        "Review the source material",
        "Review the source material for accurate answers",
        "Source material",
        "Review the above",
        "The content above",
        "This passage provides",
        "This content provides",
        "The passage explains",
        "The content explains",
        "General information",
        "General overview",
        "Basic overview",
        "General overview of the topic",
        "Multiple related concepts",
        "Related concepts",
        "Factual information about the topic",
        "Supporting details",
        "Organized manner",
    )

    /**
     * Kiểm tra câu hỏi có trong blacklist không.
     * @param question text câu hỏi
     * @return true nếu bị cấm
     */
    fun isForbiddenQuestion(question: String): Boolean {
        val lower = question.lowercase()
        return forbiddenQuestionFragments.any { fragment ->
            lower.contains(fragment.lowercase())
        }
    }

    /**
     * Kiểm tra đáp án có trong blacklist không.
     * @param option text đáp án
     * @return true nếu bị cấm
     */
    fun isForbiddenOption(option: String): Boolean {
        val lower = option.lowercase()
        return forbiddenOptionFragments.any { fragment ->
            lower.contains(fragment.lowercase())
        }
    }

    /**
     * Kiểm tra toàn bộ option set có bị cấm không.
     */
    fun hasForbiddenOptions(options: List<String>): Boolean {
        return options.any { isForbiddenOption(it) }
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(question: String? = null, option: String? = null): String? {
        if (question != null && isForbiddenQuestion(question)) {
            return "Question nằm trong fallback blacklist"
        }
        if (option != null && isForbiddenOption(option)) {
            return "Option nằm trong fallback blacklist"
        }
        return null
    }
}
