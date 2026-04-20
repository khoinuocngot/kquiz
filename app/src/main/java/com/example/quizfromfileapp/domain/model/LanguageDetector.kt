package com.example.quizfromfileapp.domain.model

/**
 * Phát hiện ngôn ngữ chính của tài liệu nguồn.
 *
 * Quy tắc:
 * - Nếu > 60% từ là ASCII English words phổ biến → English
 * - Ngược lại → Vietnamese
 *
 * Quiz sẽ được sinh bằng cùng ngôn ngữ với tài liệu nguồn
 * để tránh lai ngôn ngữ trong cùng câu hỏi.
 */
object LanguageDetector {

    val ENGLISH = "en"
    val VIETNAMESE = "vi"

    private val englishCommonWords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        // Additional English words common in educational documents
        "is", "are", "was", "were", "been", "being", "has", "had", "does", "did",
        "will", "would", "could", "should", "may", "might", "must", "can", "need",
        "such", "own", "same", "very", "each", "every", "both", "few", "more", "most",
        "other", "some", "no", "nor", "not", "only", "own", "same", "so", "too", "very",
        "and", "but", "or", "yet", "for", "nor", "so", "because", "although", "while",
        "since", "unless", "until", "before", "after", "where", "when", "why", "whether",
        "process", "system", "method", "data", "information", "result", "research",
        "study", "analysis", "development", "design", "implementation", "application",
        "theory", "concept", "principle", "approach", "strategy", "solution",
        "problem", "issue", "challenge", "opportunity", "objective", "goal", "purpose",
        "function", "feature", "component", "element", "factor", "aspect", "issue",
        "provide", "include", "contain", "describe", "explain", "show", "demonstrate",
        "support", "affect", "determine", "increase", "decrease", "change", "apply",
        "require", "used", "using", "based", "example", "figure", "table", "chapter",
        "section", "introduction", "conclusion", "reference", "note", "content"
    )

    /**
     * Phát hiện ngôn ngữ chính của văn bản.
     *
     * @param text văn bản cần kiểm tra (nên dùng extractedText đã clean)
     * @return "en" hoặc "vi"
     */
    fun detect(text: String): String {
        if (text.isBlank()) return VIETNAMESE // default

        val words = text
            .lowercase()
            .split(Regex("""[\s\p{Punct}]+"""))
            .filter { it.length >= 2 }

        if (words.isEmpty()) return VIETNAMESE

        val englishWordCount = words.count { it in englishCommonWords }
        val englishRatio = englishWordCount.toDouble() / words.size

        return if (englishRatio > 0.60) ENGLISH else VIETNAMESE
    }

    /**
     * Trả về true nếu văn bản là tiếng Anh.
     */
    fun isEnglish(text: String): Boolean = detect(text) == ENGLISH

    /**
     * Trả về true nếu văn bản là tiếng Việt.
     */
    fun isVietnamese(text: String): Boolean = detect(text) == VIETNAMESE

    /**
     * Tính confidence của việc phát hiện ngôn ngữ.
     * Giá trị từ 0.0 → 1.0.
     */
    fun confidence(text: String): Double {
        if (text.isBlank()) return 0.0

        val words = text
            .lowercase()
            .split(Regex("""[\s\p{Punct}]+"""))
            .filter { it.length >= 2 }

        if (words.isEmpty()) return 0.0

        val englishWordCount = words.count { it in englishCommonWords }
        val ratio = englishWordCount.toDouble() / words.size

        // Confidence cao nhất khi ratio càng xa 0.5
        return if (ratio > 0.5) {
            (ratio - 0.5) * 2.0
        } else {
            (0.5 - ratio) * 2.0
        }
    }
}
