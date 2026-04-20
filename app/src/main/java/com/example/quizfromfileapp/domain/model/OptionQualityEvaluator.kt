package com.example.quizfromfileapp.domain.model

/**
 * Chấm điểm chất lượng đáp án.
 *
 * KHÔNG cho phép bất kỳ template distractor nào.
 * Auto-reject nếu:
 * - Quá ngắn (< 4 từ)
 * - Quá dài (> 110 ký tự)
 * - Trùng với question
 * - Trùng với option khác
 * - Heading rác
 * - Pattern template rác
 */
object OptionQualityEvaluator {

    const val MIN_WORDS = 4
    const val MAX_WORDS = 20
    const val MAX_CHARS = 110
    const val MIN_SCORE = 70.0

    private val templateDistractorPhrases = setOf(
        "related concept",
        "related factors",
        "related aspects",
        "khái niệm liên quan",
        "yếu tố liên quan",
        "các yếu tố liên quan",
        "một số đặc điểm khác biệt",
        "but with some different",
        "different characteristics",
        "một phần",
        "nhưng với",
        "được nhấn mạnh",
        "được đề cập trong nội dung",
        "general principle",
        "applies broadly",
        "broadly accurate",
        "incomplete statement",
        "in another context",
        "trong ngữ cảnh khác",
        "nguyên tắc chung",
        "áp dụng rộng rãi",
        "nhận định đúng nhưng",
        "diễn giải đúng một phần"
    )

    private val stopwords = setOf(
        "và", "của", "là", "có", "được", "trong", "với", "để", "cho", "không",
        "theo", "này", "một", "những", "các", "tại", "từ", "về", "cũng", "đã",
        "sẽ", "hoặc", "nếu", "thì", "nhưng", "vì", "nên", "khi", "đó", "đây",
        "còn", "phải", "hay", "rằng", "bởi", "vào", "ra", "lên", "xuống",
        "lại", "đi", "lúc", "kia", "nữa", "chỉ", "mà", "vậy", "tuy", "nhiên",
        "đều", "cả", "hết", "mỗi", "tất", "sau", "trước", "ngay", "luôn",
        "and", "the", "a", "an", "of", "in", "to", "for", "with", "on", "at",
        "by", "from", "as", "is", "was", "are", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "that", "this", "it", "its", "which", "what", "who", "how"
    )

    /**
     * Chấm điểm option.
     * @return điểm 0–100; < 70 = reject
     */
    fun score(
        option: String,
        questionText: String = "",
        existingOptions: List<String> = emptyList()
    ): Double {
        var score = 100.0
        val trimmed = option.trim()
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val wordCount = words.size
        val charCount = trimmed.length

        // ── Ràng buộc cứng: vi phạm → 0 ──

        if (wordCount < MIN_WORDS) return 0.0
        if (charCount > MAX_CHARS) return 0.0

        // REJECT template distractor
        if (isTemplateDistractor(trimmed)) return 0.0

        // Toàn stopwords
        if (words.all { it.replace(Regex("""[^\p{L}\p{M}]"""), "").lowercase() in stopwords }) {
            return 0.0
        }

        // Trùng với question text
        if (questionText.isNotBlank() && similarity(trimmed, questionText) > 0.70) {
            return 0.0
        }

        // Trùng với option khác
        if (existingOptions.any { similarity(trimmed, it) > 0.80 }) {
            return 0.0
        }

        // Heading rác
        if (HeadingCaptionFilter.isHeadingOrCaption(trimmed)) return 0.0
        if (OptionQualityBlacklist.isBlacklisted(trimmed)) return 0.0

        // ── Điểm mềm ──

        // Quá ngắn (4–5 từ)
        if (wordCount in 4..5) score -= 10.0

        // Quá dài (80–110 ký tự)
        if (charCount in 80..MAX_CHARS) score -= 5.0

        // Option ngắn hơn nhiều so với trung bình
        if (existingOptions.isNotEmpty()) {
            val avgLen = existingOptions.map { it.length }.average()
            if (trimmed.length < avgLen * 0.4) score -= 15.0
            if (trimmed.length > avgLen * 2.5) score -= 20.0
        }

        // Gần-trùng với option khác
        val nearDup = existingOptions.count { similarity(trimmed, it) > 0.55 }
        score -= nearDup * 15.0

        return maxOf(0.0, minOf(100.0, score))
    }

    fun isAcceptable(
        option: String,
        questionText: String = "",
        existingOptions: List<String> = emptyList()
    ): Boolean = score(option, questionText, existingOptions) >= MIN_SCORE

    fun filterAndRank(
        options: List<String>,
        questionText: String = ""
    ): List<Pair<String, Double>> {
        val scored = options.map { opt ->
            opt to score(opt, questionText, options.filter { it != opt })
        }
        return scored
            .filter { it.second >= MIN_SCORE }
            .sortedByDescending { it.second }
    }

    fun rejectReason(
        option: String,
        questionText: String = "",
        existingOptions: List<String> = emptyList()
    ): String? {
        val trimmed = option.trim()
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        return when {
            trimmed.isBlank() -> "Option trống"
            words.size < MIN_WORDS -> "Option quá ngắn (< $MIN_WORDS từ)"
            trimmed.length > MAX_CHARS -> "Option quá dài (> $MAX_CHARS ký tự)"
            isTemplateDistractor(trimmed) -> "Là template distractor rác"
            words.all { it.replace(Regex("""[^\p{L}\p{M}]"""), "").lowercase() in stopwords } ->
                "Option chỉ chứa stopwords"
            questionText.isNotBlank() && similarity(trimmed, questionText) > 0.70 ->
                "Option trùng với câu hỏi"
            existingOptions.any { similarity(trimmed, it) > 0.80 } ->
                "Option trùng với option khác"
            HeadingCaptionFilter.isHeadingOrCaption(trimmed) ->
                "Option là heading/caption rác"
            else -> null
        }
    }

    private fun isTemplateDistractor(text: String): Boolean {
        val lower = text.lowercase()
        return templateDistractorPhrases.any { lower.contains(it) }
    }

    private fun similarity(a: String, b: String): Double {
        val wordsA = a.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
        val wordsB = b.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return intersection.toDouble() / union
    }
}

/**
 * Blacklist cố định cho options.
 * Những text này LUÔN bị reject.
 */
object OptionQualityBlacklist {
    private val blacklist = setOf(
        "Figure", "Table", "Example", "Find", "Each", "The", "Number",
        "Content", "Introduction", "Chapter", "Part", "Section", "Types",
        "Note", "Page", "Reference", "Summary", "Conclusion", "Appendix",
        "See", "See also", "As shown", "As follows", "Below", "Above",
        "Hình", "Bảng", "Chương", "Mục", "Phần", "Ghi chú", "Lưu ý",
        "Bài học", "Tài liệu", "Nội dung", "Mục lục"
    )

    fun isBlacklisted(text: String): Boolean {
        val first = text.trim().split(Regex("""\s+""")).firstOrNull() ?: return false
        return blacklist.any { first.equals(it, ignoreCase = true) }
    }
}
