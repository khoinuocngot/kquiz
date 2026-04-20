package com.example.quizfromfileapp.domain.model

/**
 * Bộ lọc heading/caption nghiêm ngặt.
 *
 * Reject các segment/heading nếu:
 * - Bắt đầu bằng Figure, Table, Example, Chapter, Overview, Introduction...
 * - Giống cấu trúc mục lục (số thứ tự + title ngắn)
 * - Toàn chữ IN HOA
 * - Quá ngắn nhưng chứa keyword heading đặc trưng
 * - Chứa cấu trúc outline/chapter title
 *
 * Phân biệt heading vs. prose thật:
 * - Heading: in hoa, ngắn, không có động từ thường
 * - Prose: có câu hoàn chỉnh, có động từ, dài hơn
 */
object HeadingCaptionFilter {

    /**
     * Kiểm tra text có phải là heading/caption không.
     * @param text văn bản cần kiểm tra
     * @return true nếu là heading/caption → reject
     */
    fun isHeadingOrCaption(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        if (trimmed.length < 100 && startsWithHeadingKeyword(trimmed)) return true
        if (matchesHeadingPatterns(trimmed)) return true
        if (isPureOutlineHeading(trimmed)) return true
        if (isAllCapsHeading(trimmed)) return true
        if (isNumberedTitleHeading(trimmed)) return true
        return false
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isBlank() -> "Text trống"
            trimmed.length < 100 && startsWithHeadingKeyword(trimmed) ->
                "Bắt đầu bằng keyword heading: ${firstKeyword(trimmed)}"
            matchesHeadingPatterns(trimmed) -> "Khớp pattern heading/caption"
            isPureOutlineHeading(trimmed) -> "Là heading dạng outline"
            isAllCapsHeading(trimmed) -> "Toàn chữ IN HOA"
            isNumberedTitleHeading(trimmed) -> "Là heading số thứ tự"
            else -> null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Kiểm tra heading keyword
    // ─────────────────────────────────────────────────────────────

    private val headingKeywords = setOf(
        "Figure", "Table", "Example", "Find", "Each", "The",
        "Introduction", "Conclusion", "Summary", "References",
        "Content", "Chapter", "Part", "Section", "Types", "Note",
        "Page", "Overview", "Appendix", "Bibliography", "Index",
        "LAN", "WAN", "MAN", "OSI", "TCP", "IP", "HTTP", "DNS",
        // Việt
        "Hình", "Bảng", "Chương", "Mục", "Phần", "Ghi chú",
        "Lưu ý", "Bài học", "Mục lục", "Tài liệu", "Nội dung"
    )

    private fun startsWithHeadingKeyword(text: String): Boolean {
        val first = text.trim().split(Regex("""\s+""")).firstOrNull() ?: return false
        return headingKeywords.any { first.equals(it, ignoreCase = true) }
    }

    private fun firstKeyword(text: String): String {
        return text.trim().split(Regex("""\s+""")).firstOrNull() ?: ""
    }

    // ─────────────────────────────────────────────────────────────
    // Pattern matching
    // ─────────────────────────────────────────────────────────────

    private val headingPatterns = listOf(
        Regex("""(?i)^\\s*(figure|table|example|note|page)\\s+\\d+.*"""),
        Regex("""(?i)^\\s*(introduction|conclusion|summary|references|bibliography|appendix)\\s*$"""),
        Regex("""(?i)^\\s*(chapter|section|part|unit)\\s+\\d+.*"""),
        Regex("""(?i)^\\s*(table\\s+of\\s+contents|mục\\s+lục).*"""),
        Regex("""(?i)^\\s*(hình|bảng|chương|mục)\\s+\\d+.*"""),
        Regex("""(?i)^\\s*(overview|outline)\\s*:.*"""),
        Regex("""(?i)^\\s*(types|features|characteristics)\\s+of.*"""),
        Regex("""(?i)^\\s*(what|which)\\s+is\\s+(figure|table)\\s+\\d.*"""),
        Regex("""(?i)^\\s*(find|each)\\s+\\w+\\s+(in|of|the)\\s+.*$"""),
        Regex("""(?i)^\\s*(see|see\\s+also|as\\s+shown|as\\s+follows).*"""),
        Regex("""(?i)^\\s*layers\\s+in\\s+(the\\s+)?\\w+.*"""),
        Regex("""(?i)^\\s*\\w+\\s*&\\s*\\w+\\s*$"""),  // "LAN & WAN"
        Regex("""(?i)^\\s*(lan|wan|man|osi|tcp|ip|http|dns)\\s*:.*"""),
    )

    private fun matchesHeadingPatterns(text: String): Boolean {
        return headingPatterns.any { it.containsMatchIn(text) }
    }

    // ─────────────────────────────────────────────────────────────
    // Outline heading — "4.1 Overview", "1.2.3 Title"
    // ─────────────────────────────────────────────────────────────

    private val outlinePatterns = listOf(
        Regex("""^\\s*\\d+(\\.\\d+)*\\s+[A-Z][a-zA-Z\\s]{0,50}$"""),
        Regex("""^\\s*\\d+\\.\\s+\\S{0,60}$"""),
        Regex("""^\\s*[a-z]\\.\\s+\\S{0,60}$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*[ivxlcdm]+\\.\\s+\\S{0,60}$""", RegexOption.IGNORE_CASE),
    )

    private fun isPureOutlineHeading(text: String): Boolean {
        if (text.length > 100) return false
        return outlinePatterns.any { it.matches(text.trim()) }
    }

    // ─────────────────────────────────────────────────────────────
    // All-caps heading
    // ─────────────────────────────────────────────────────────────

    private fun isAllCapsHeading(text: String): Boolean {
        val alpha = text.filter { it.isLetter() }
        if (alpha.isEmpty()) return false
        if (alpha.length < 5) return false
        val upperRatio = alpha.count { it.isUpperCase() }.toDouble() / alpha.length
        return upperRatio > 0.80 && text.length < 120
    }

    // ─────────────────────────────────────────────────────────────
    // Numbered title — "4.1 Introduction to Networking"
    // ─────────────────────────────────────────────────────────────

    private fun isNumberedTitleHeading(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 120) return false
        val words = trimmed.split(Regex("""\s+"""))
        if (words.size < 2) return false
        val first = words.first()
        if (!first.matches(Regex("""\d+(\.\d+)*"""))) return false
        // Heading thường có title ngắn phía sau số
        if (words.size <= 5 && words.drop(1).all { it.length < 15 }) return true
        return false
    }
}
