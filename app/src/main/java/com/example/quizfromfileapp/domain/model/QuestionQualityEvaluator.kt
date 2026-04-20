package com.example.quizfromfileapp.domain.model

/**
 * Chấm điểm chất lượng câu hỏi trước khi đưa vào quiz.
 *
 * REJECT câu hỏi nếu:
 * - Quá ngắn hoặc quá dài
 * - Bắt nguồn từ heading/caption
 * - Chứa keyword rác
 * - Quá chung chung, không bám nội dung
 * - Không có declarative sentence trong source segment
 */
object QuestionQualityEvaluator {

    const val MIN_WORDS = 4
    const val MIN_CHARS = 25
    const val MAX_CHARS = 350
    const val MIN_SCORE = 70.0

    /**
     * Kiểm tra câu hỏi có đạt chất lượng tối thiểu không.
     */
    fun isAcceptable(questionText: String, sourceSegment: String = ""): Boolean {
        return score(questionText, sourceSegment) >= MIN_SCORE
    }

    /**
     * Chấm điểm câu hỏi. 0–100; < 70 = reject.
     */
    fun score(questionText: String, sourceSegment: String = ""): Double {
        if (questionText.isBlank()) return 0.0
        var score = 100.0
        val trimmed = questionText.trim()
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }

        // ── Ràng buộc cứng ──
        if (words.size < MIN_WORDS) return 0.0
        if (trimmed.length < MIN_CHARS) return 0.0
        if (trimmed.length > MAX_CHARS) return 0.0
        if (sourceSegment.isNotBlank() && trimmed == sourceSegment.trim()) return 0.0

        // ── Điểm mềm ──

        // Chứa keyword rác (bắt đầu hoặc trong nội dung)
        val trashHitCount = trashKeywords.count { kw ->
            trimmed.lowercase().contains(kw.lowercase())
        }
        score -= trashHitCount * 25.0

        // Khớp pattern rác
        val patternHits = trashQuestionPatterns.count { it.containsMatchIn(trimmed) }
        score -= patternHits * 30.0

        // Bắt đầu bằng "The" + ngắn → heading style
        if (words.firstOrNull()?.lowercase() == "the" && trimmed.length < 80) {
            score -= 20.0
        }

        // Source là heading/caption → reject
        if (sourceSegment.isNotBlank() && HeadingCaptionFilter.isHeadingOrCaption(sourceSegment)) {
            score -= 40.0
        }

        // Không có snippet trong ngoặc kép → thiếu nội dung tham chiếu
        if (!trimmed.contains("\"")) {
            score -= 10.0
        }

        // Quá generic (nhiều phần hỏi chung mà không bám vào content)
        if (isTooGeneric(trimmed)) {
            score -= 20.0
        }

        // Source segment không có declarative content
        if (sourceSegment.isNotBlank()) {
            val hasDecl = DeclarativeSentenceExtractor.extractDeclarativeSentences(sourceSegment).isNotEmpty()
            if (!hasDecl) score -= 35.0
        }

        return maxOf(0.0, minOf(100.0, score))
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(questionText: String, sourceSegment: String = ""): String? {
        if (questionText.isBlank()) return "Question trống"
        val words = questionText.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
        val trimmed = questionText.trim()

        return when {
            words.size < MIN_WORDS -> "Question quá ngắn (< $MIN_WORDS từ)"
            trimmed.length < MIN_CHARS -> "Question quá ngắn (< $MIN_CHARS ký tự)"
            trimmed.length > MAX_CHARS -> "Question quá dài (> $MAX_CHARS ký tự)"
            trashKeywords.any { trimmed.lowercase().contains(it.lowercase()) } ->
                "Question chứa keyword rác"
            trashQuestionPatterns.any { it.containsMatchIn(trimmed) } ->
                "Question khớp pattern rác"
            words.firstOrNull()?.lowercase() == "the" && trimmed.length < 80 ->
                "Question bắt đầu bằng 'The' (heading style)"
            sourceSegment.isNotBlank() && HeadingCaptionFilter.isHeadingOrCaption(sourceSegment) ->
                "Source segment là heading/caption"
            !trimmed.contains("\"") -> "Question không chứa snippet trích dẫn"
            isTooGeneric(trimmed) -> "Question quá generic, không bám nội dung"
            sourceSegment.isNotBlank() &&
                DeclarativeSentenceExtractor.extractDeclarativeSentences(sourceSegment).isEmpty() ->
                "Source segment không có declarative sentence"
            else -> null
        }
    }

    private val trashKeywords = setOf(
        "Figure", "Table", "Example", "Find", "Each", "The", "Number",
        "Content", "Introduction", "Chapter", "Part", "Section", "Types",
        "Note", "Page", "References", "Summary", "Conclusion", "Appendix",
        "Overview", "As shown", "As follows", "See also", "Below figure",
        "Hình", "Bảng", "Chương", "Mục", "Phần", "Ghi chú", "Lưu ý",
        "Bài học", "Tài liệu", "Mục lục"
    )

    private val trashQuestionPatterns = listOf(
        Regex("""(?i)^\\s*(figure|table|example|introduction|chapter|overview)\\s+\\d.*"""),
        Regex("""(?i)^\\s*(find|each)\\s+\\w+\\s+(in|of|the).*"""),
        Regex("""(?i)^\\s*the\\s+\\w+\\s+(is|are|was|were)\\s*$"""),
        Regex("""(?i)^\\s*(what|which)\\s+is\\s+(figure|table)\\s+\\d.*"""),
        Regex("""(?i)^\\s*mục\\s+lục.*"""),
        Regex("""(?i)^\\s*(hình|bảng|chương)\\s+\\d+.*"""),
        Regex("""(?i)^\\s*(note|see|find)\\s+.*$"""),
    )

    private fun isTooGeneric(text: String): Boolean {
        val lower = text.lowercase()
        // Đếm các phrase quá generic
        var genericCount = 0
        if (lower.contains("thành phần nào thuộc")) genericCount++
        if (lower.contains("nhận định nào đúng") && !text.contains("\"")) genericCount++
        if (lower.contains("ý nào đúng") && !text.contains("\"")) genericCount++
        if (lower.contains("theo nội dung") && !text.contains("\"")) genericCount++
        if (lower.contains("which statement") && text.count { it == '"' } < 2) genericCount++
        if (lower.contains("what is") && !text.contains("\"")) genericCount++
        return genericCount >= 2
    }
}
