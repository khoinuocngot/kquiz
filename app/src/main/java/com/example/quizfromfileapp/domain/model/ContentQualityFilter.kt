package com.example.quizfromfileapp.domain.model

/**
 * Bộ lọc segment chất lượng thấp — BƯỚC 7.5.
 *
 * NGHIÊM NGẶT — segment phải thỏa TẤT CẢ điều kiện:
 * - Số từ >= 12
 * - Có declarative sentence thật (dùng DeclarativeSentenceExtractor)
 * - Không phải heading/caption (HeadingCaptionFilter)
 * - Không phải instructional/meta sentence (InstructionSentenceFilter)
 * - Không phải dòng mục lục
 * - Không quá nhiều bullet markers
 */
object ContentQualityFilter {

    const val MIN_WORDS = 12
    const val MAX_WORDS = 120

    /**
     * Kiểm tra segment có đủ chất lượng để sinh câu hỏi không.
     */
    fun isQualitySegment(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false

        // 1. Độ dài
        if (trimmed.length < 60) return false
        if (trimmed.length > 1200) return false

        // 2. Số từ
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size < MIN_WORDS) return false
        if (words.size > MAX_WORDS) return false

        // 3. REJECT heading/caption
        if (HeadingCaptionFilter.isHeadingOrCaption(trimmed)) return false

        // 4. REJECT instructional/meta sentence
        if (InstructionSentenceFilter.isInstructional(trimmed)) return false

        // 5. REJECT dòng mục lục
        if (isTocLine(trimmed)) return false

        // 6. REJECT quá nhiều bullet markers
        if (hasExcessiveBullets(trimmed)) return false

        // 7. Phải có declarative sentence thật sự
        if (!hasDeclarativeContent(trimmed)) return false

        return true
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isBlank() -> "Segment trống"
            trimmed.length < 60 -> "Segment quá ngắn (< 60 ký tự)"
            trimmed.length > 1200 -> "Segment quá dài (> 1200 ký tự)"
            trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }.size < MIN_WORDS ->
                "Ít hơn $MIN_WORDS từ"
            HeadingCaptionFilter.isHeadingOrCaption(trimmed) ->
                "Là heading/caption: ${HeadingCaptionFilter.rejectReason(trimmed) ?: ""}"
            InstructionSentenceFilter.isInstructional(trimmed) ->
                "Là câu chỉ dẫn/instructional: ${InstructionSentenceFilter.rejectReason(trimmed) ?: ""}"
            isTocLine(trimmed) -> "Là dòng mục lục / số thứ tự"
            hasExcessiveBullets(trimmed) -> "Quá nhiều bullet markers"
            !hasDeclarativeContent(trimmed) -> "Không có declarative sentence"
            else -> null
        }
    }

    private val tocLinePatterns = listOf(
        Regex("""^\\s*\\d+(\\.\\d+)*\\s+\\S{0,40}$"""),
        Regex("""^\\s*[a-z]\\.\\s+\\S{0,40}$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*[ivxlcdm]+\\.\\s+\\S{0,40}$""", RegexOption.IGNORE_CASE),
    )

    private fun isTocLine(text: String): Boolean {
        if (text.length > 80) return false
        return tocLinePatterns.any { it.matches(text.trim()) }
    }

    private fun hasExcessiveBullets(text: String): Boolean {
        val bulletCount = text.count { it in "•·-*–—•" }
        val lines = text.lines()
        if (lines.size <= 1) return false
        return bulletCount >= lines.size / 2
    }

    private fun hasDeclarativeContent(text: String): Boolean {
        val sentences = DeclarativeSentenceExtractor.extractDeclarativeSentences(text)
        return sentences.isNotEmpty()
    }
}
