package com.example.quizfromfileapp.domain.model

/**
 * Bộ lọc instructional/meta sentences — câu chỉ dẫn, hướng dẫn, yêu cầu.
 *
 * Loại bỏ segment/sentence nếu nó là câu chỉ dẫn chứ không phải câu mô tả tri thức.
 *
 * Ví dụ bị loại:
 * - "Describe the hexadecimal system..."
 * - "Review the content above..."
 * - "After studying this section, you should be able to..."
 * - "Answer the following questions..."
 * - "Find the correct answer..."
 * - "Objectives: To understand..."
 * - "Exercise: What is..."
 */
object InstructionSentenceFilter {

    /**
     * Kiểm tra text có phải là instructional/meta sentence không.
     * @return true nếu là instructional → reject
     */
    fun isInstructional(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        if (trimmed.length < 200 && startsWithInstructionalKeyword(trimmed)) return true
        if (matchesInstructionalPatterns(trimmed)) return true
        if (matchesMetaPatterns(trimmed)) return true
        return false
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isBlank() -> "Text trống"
            trimmed.length < 200 && startsWithInstructionalKeyword(trimmed) ->
                "Bắt đầu bằng keyword chỉ dẫn: ${firstWord(trimmed)}"
            matchesInstructionalPatterns(trimmed) -> "Khớp pattern câu chỉ dẫn"
            matchesMetaPatterns(trimmed) -> "Khớp pattern meta/instruction"
            else -> null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Instructional keywords
    // ─────────────────────────────────────────────────────────────

    private val instructionalKeywords = listOf(
        // English
        "Describe", "Review", "Answer", "Find", "Explain", "Discuss",
        "List", "Compare", "Define", "Identify", "Calculate", "Solve",
        "Objectives", "Objective", "Outcomes", "Learning outcomes",
        "After studying", "Upon completion", "By the end of",
        "Exercise", "Exercises", "Practice", "Quiz", "Question",
        "Assignment", "Homework", "Test", "Exam",
        "Instructions", "Instruction", "Directions",
        "Note that", "Note:", "Remember", "Keep in mind",
        "Figure", "Table", "Example", "See also", "Refer to",
        "Introduction", "Overview", "Summary", "Conclusion",
        // Vietnamese
        "Mô tả", "Xem lại", "Trả lời", "Tìm", "Giải thích",
        "Bài tập", "Mục tiêu", "Sau khi học", "Kết quả học tập",
        "Hướng dẫn", "Chỉ dẫn", "Yêu cầu"
    )

    private fun startsWithInstructionalKeyword(text: String): Boolean {
        val first = text.trim().split(Regex("""\s+""")).firstOrNull() ?: return false
        return instructionalKeywords.any { first.equals(it, ignoreCase = true) }
    }

    private fun firstWord(text: String): String {
        return text.trim().split(Regex("""\s+""")).firstOrNull() ?: ""
    }

    // ─────────────────────────────────────────────────────────────
    // Pattern matching
    // ─────────────────────────────────────────────────────────────

    private val instructionalPatterns = listOf(
        Regex("""(?i)^\\s*(describe|explain|discuss|list|compare|define|identify|calculate|solve)\\s+.*\.\s*$"""),
        Regex("""(?i)^\\s*(answer|find)\\s+the\\s+(following|question).*"""),
        Regex("""(?i)^\\s*(objectives?|outcomes?|learning\\s+outcomes?)\\s*[:.]?\\s*"""),
        Regex("""(?i)^\\s*(after\\s+studying|upon\\s+completion|by\\s+the\\s+end)\\s+.*"""),
        Regex("""(?i)^\\s*(exercise|exercises?|practice|quiz|question|assignment|homework)\\s*[:.]?\\s*"""),
        Regex("""(?i)^\\s*(instructions?|directions?)\\s*[:.]?\\s*"""),
        Regex("""(?i)^\\s*(note|remember|keep\\s+in\\s+mind)\\s*[:.]?\\s*.*"""),
        Regex("""(?i)^\\s*(refer\\s+to|see\\s+also|see\\s+figure)\\s+.*"""),
        Regex("""(?i)^\\s*(this\\s+(section|chapter|module|unit)\\s+covers?|covers?|focuses?)\\s+.*"""),
        Regex("""(?i)^\\s*(upon|after|when)\\s+.*\\s+(you\\s+should|you\\s+will|you\\s+can).*"""),
        Regex("""(?i)^\\s*\\d+\\.\\s*(describe|explain|discuss|list|compare|define|answer).*"""),
    )

    private val metaPatterns = listOf(
        Regex("""(?i)^\\s*(introduction|conclusion|summary)\\s+of\\s+.*"""),
        Regex("""(?i)^\\s*(overview|outline)\\s+(of|for)\\s+.*"""),
        Regex("""(?i)^\\s*(what|which|how)\\s+(do|does|can|will)\\s+.*\?$"""),
        Regex("""(?i)^\\s*(the\\s+following|below|above)\\s+(is|are|shows?).*"""),
        Regex("""(?i)^\\s*chapter\\s+\\d+\\s*:.*"""),
        Regex("""(?i)^\\s*section\\s+\\d+\\.\\d+.*"""),
        Regex("""(?i)^\\s*\\d+(\\.\\d+)+\\s+.*\\s+(definition|meaning|introduction).*"""),
    )

    private fun matchesInstructionalPatterns(text: String): Boolean {
        return instructionalPatterns.any { it.containsMatchIn(text) }
    }

    private fun matchesMetaPatterns(text: String): Boolean {
        return metaPatterns.any { it.containsMatchIn(text) }
    }
}
