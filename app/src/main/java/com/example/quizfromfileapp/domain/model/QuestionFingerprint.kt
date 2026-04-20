package com.example.quizfromfileapp.domain.model

/**
 * Fingerprint cho câu hỏi — dùng để deduplicate.
 *
 * Cách tạo fingerprint:
 * 1. Lowercase
 * 2. Bỏ dấu câu, số, ký tự đặc biệt
 * 3. Bỏ stopwords phổ biến
 * 4. Ghép lại → so sánh similarity
 *
 * Hai câu có similarity > THRESHOLD → trùng lặp.
 */
object QuestionFingerprint {

    private const val SIMILARITY_THRESHOLD = 0.72

    private val stopwords = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "must", "can", "that", "this", "it", "its",
        "of", "in", "to", "for", "with", "on", "at", "by", "from", "as",
        "which", "what", "who", "how", "but", "or", "and", "not", "all",
        "any", "each", "every", "both", "few", "more", "most", "other",
        "some", "such", "no", "nor", "only", "own", "same", "so", "than",
        "too", "very", "just", "then", "when", "where", "why"
    )

    /**
     * Kiểm tra hai câu hỏi có trùng lặp không.
     *
     * @param a câu hỏi thứ nhất
     * @param b câu hỏi thứ hai
     * @return true nếu trùng lặp (similarity > THRESHOLD)
     */
    fun isDuplicate(a: String, b: String): Boolean {
        return similarity(a, b) > SIMILARITY_THRESHOLD
    }

    /**
     * Tính similarity giữa hai câu hỏi.
     * @return giá trị 0.0 – 1.0
     */
    fun similarity(a: String, b: String): Double {
        val fpA = fingerprint(a)
        val fpB = fingerprint(b)
        if (fpA.isBlank() || fpB.isBlank()) return 0.0
        return if (fpA == fpB) 1.0 else jaccard(fpA, fpB)
    }

    /**
     * Kiểm tra câu hỏi mới có trùng với bất kỳ câu nào trong danh sách không.
     */
    fun isDuplicateOfAny(
        question: String,
        existingQuestions: List<String>
    ): Boolean {
        return existingQuestions.any { isDuplicate(question, it) }
    }

    /**
     * Tạo fingerprint rút gọn từ câu.
     */
    fun fingerprint(text: String): String {
        val cleaned = text
            .lowercase()
            // Bỏ dấu câu và số
            .replace(Regex("""[.!?,\(\)\[\]{}'"]"""), " ")
            .replace(Regex("""\d+"""), " ")
            // Bỏ ký tự đặc biệt
            .replace(Regex("""[^a-z\s]"""), " ")
            // Normalize spaces
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Bỏ stopwords
        val words = cleaned.split(Regex("""\s+"""))
            .filter { it.length >= 3 && it !in stopwords }

        return words.joinToString(" ")
    }

    /**
     * Trả về "core" của câu hỏi — chỉ phần hỏi, không có snippet.
     */
    fun questionCore(text: String): String {
        // Lấy phần trước dấu \n hoặc trước dấu "
        val beforeNewline = text.split("\n").firstOrNull() ?: text
        val beforeQuote = beforeNewline.split("\"").firstOrNull() ?: beforeNewline
        return beforeQuote.trim()
    }

    private fun jaccard(a: String, b: String): Double {
        val wordsA = a.split(Regex("""\s+""")).filter { it.isNotBlank() }.toSet()
        val wordsB = b.split(Regex("""\s+""")).filter { it.isNotBlank() }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return intersection.toDouble() / union
    }
}
