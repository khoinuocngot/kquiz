package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Tính độ tương đồng ngữ nghĩa giữa 2 text.
 *
 * Dùng 3 phương pháp:
 * 1. Jaccard similarity trên meaningful tokens
 * 2. Length-aware Jaccard (chuẩn hóa theo độ dài)
 * 3. Character n-gram Jaccard (bắt cặp subword, robust với word variation)
 *
 * Tất cả method trả về giá trị 0.0 → 1.0.
 * Score cao = giống nhau nhiều.
 */
object SemanticSimilarityHelper {

    private const val TAG = "SemanticSimilarityHelper"

    private val STOP_WORDS_EN = setOf(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "may", "might", "can", "that", "which", "who", "whom", "this", "these",
        "those", "it", "its", "of", "in", "to", "for", "on", "with", "at", "by",
        "from", "as", "or", "and", "but", "if", "then", "else", "when", "where",
        "what", "how", "why", "so", "such", "also", "very", "more", "most",
        "about", "into", "over", "after", "before", "between", "under", "above",
        "only", "just", "even", "still", "both", "each", "few", "many", "much",
        "other", "another", "all", "any", "some", "no", "not", "own", "same",
        "than", "too", "very", "s", "t", "d", "ll", "re", "ve", "m"
    )

    private val STOP_WORDS_VI = setOf(
        "và", "là", "của", "trong", "được", "có", "không", "để", "từ", "với",
        "cho", "này", "khi", "đã", "sẽ", "mà", "ra", "vào", "ở", "trên",
        "dưới", "năm", "một", "theo", "hay", "hoặc", "nhưng", "nên", "vì",
        "nếu", "thì", "cũng", "còn", "đó", "kia", "bởi", "tại", "từ",
        "về", "qua", "sau", "trước", "giữa", "bên", "ngoài", "bên trong",
        "sau đó", "trước đó", "mỗi", "tất cả", "không có", "có thể", "phải",
        "đang", "được", "bị", "làm", "gọi", "đi", "đến", "về", "lại"
    )

    /**
     * Similarity chính — dùng length-aware Jaccard.
     * Đây là method nên dùng trong hầu hết trường hợp.
     */
    fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0

        val normA = normalizeForComparison(a)
        val normB = normalizeForComparison(b)
        if (normA.isBlank() || normB.isBlank()) return 0.0

        return lengthAwareJaccard(normA, normB)
    }

    /**
     * Similarity chỉ dùng meaningful tokens (loại stop words).
     * Dùng khi muốn đo overlap về nội dung, bỏ qua từ nối.
     */
    fun tokenBasedSimilarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0

        val tokensA = extractMeaningfulTokens(normalizeForComparison(a))
        val tokensB = extractMeaningfulTokens(normalizeForComparison(b))

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0

        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size

        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    /**
     * Character n-gram Jaccard (3-grams).
     * Robust hơn với word variation, typos, và morphological changes.
     */
    fun nGramSimilarity(a: String, b: String, n: Int = 3): Double {
        if (a.isBlank() || b.isBlank()) return 0.0

        val ngramsA = generateNgrams(a.lowercase(), n)
        val ngramsB = generateNgrams(b.lowercase(), n)

        if (ngramsA.isEmpty() || ngramsB.isEmpty()) return 0.0

        val intersection = ngramsA.intersect(ngramsB).size
        val union = ngramsA.union(ngramsB).size

        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    /**
     * Substring containment check.
     * Trả về tỷ lệ a nằm trong b (hoặc ngược lại).
     * Dùng để detect copy nguyên văn.
     */
    fun containmentRatio(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0

        val short = if (a.length <= b.length) a else b
        val long = if (a.length <= b.length) b else a

        val shortNorm = short.lowercase().trim()
        val longNorm = long.lowercase().trim()

        if (longNorm.contains(shortNorm)) {
            return shortNorm.length.toDouble() / longNorm.length.coerceAtLeast(1)
        }

        return 0.0
    }

    /**
     * Kiểm tra xem a có phải là paraphrase gần nguyên của b không.
     * Dùng threshold: similarity > 0.75 → coi là paraphrase.
     */
    fun isNearParaphrase(a: String, b: String, threshold: Double = 0.75): Boolean {
        val sim = similarity(a, b)
        logd("isNearParaphrase: sim=${String.format("%.2f", sim)} threshold=$threshold")
        return sim > threshold
    }

    /**
     * Kiểm tra correctOption có copy quá nhiều từ sourceSnippet không.
     * threshold mặc định 0.60 — nếu similarity > 60% thì coi là copy.
     */
    fun isVerbatimCopy(correctOption: String, sourceSnippet: String, threshold: Double = 0.60): Boolean {
        if (correctOption.isBlank() || sourceSnippet.isBlank()) return false

        val sim = similarity(correctOption, sourceSnippet)
        val containment = containmentRatio(correctOption, sourceSnippet)

        // Nếu similarity cao HOẶC một option chứa gần như toàn bộ snippet → copy
        val isCopy = sim > threshold || containment > 0.7

        if (isCopy) {
            logw("Verbatim copy detected: sim=${String.format("%.2f", sim)} containment=${String.format("%.2f", containment)}")
            logw("  option: '${correctOption.take(60)}'")
            logw("  source: '${sourceSnippet.take(60)}'")
        }

        return isCopy
    }

    /**
     * Kiểm tra 2 options có quá giống nhau không (để phát hiện duplicate options).
     */
    fun areOptionsTooSimilar(opt1: String, opt2: String, threshold: Double = 0.80): Boolean {
        if (opt1.isBlank() || opt2.isBlank()) return false
        return similarity(opt1, opt2) > threshold
    }

    /**
     * Kiểm tra question có phải chỉ thay đổi rất ít so với sourceSnippet không.
     */
    fun isQuestionRestatement(question: String, sourceSnippet: String, threshold: Double = 0.70): Boolean {
        if (question.isBlank() || sourceSnippet.isBlank()) return false

        // So sánh question với snippet — nếu quá giống thì chỉ là restatement
        val sim = similarity(question, sourceSnippet)
        return sim > threshold
    }

    /**
     * Normalize text để so sánh: lowercase, loại bỏ punctuation, collapse whitespace.
     */
    fun normalizeForComparison(text: String): String {
        return text
            .lowercase()
            .replace(Regex("""[\u2018\u2019\u201c\u201d]"""), "'")
            .replace(Regex("""[\u2013\u2014]"""), "-")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Trích meaningful tokens (loại stop words, giữ từ >= 3 chars).
     */
    private fun extractMeaningfulTokens(text: String): Set<String> {
        val allStopWords = STOP_WORDS_EN + STOP_WORDS_VI

        return text
            .split(Regex("""[\s\p{Punct}]+"""))
            .filter { token ->
                token.length >= 3 && !allStopWords.contains(token.lowercase())
            }
            .toSet()
    }

    /**
     * Length-aware Jaccard: Jaccard × length penalty.
     * Giảm score nếu 2 text chênh lệch độ dài nhiều.
     */
    private fun lengthAwareJaccard(a: String, b: String): Double {
        val tokensA = extractMeaningfulTokens(a)
        val tokensB = extractMeaningfulTokens(b)

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0

        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size

        val jaccard = if (union == 0) 0.0 else intersection.toDouble() / union

        // Length penalty: giảm score nếu độ dài chênh lệch nhiều
        val lenRatio = minOf(a.length, b.length).toDouble() / maxOf(a.length, b.length, 1)
        // lenRatio = 1.0 khi bằng nhau, ~0.5 khi chênh gấp đôi

        return jaccard * (0.6 + 0.4 * lenRatio)
    }

    /**
     * Generate character n-grams.
     */
    private fun generateNgrams(text: String, n: Int): Set<String> {
        if (text.length < n) return setOf(text)
        return (0..text.length - n).map { i ->
            text.substring(i, i + n)
        }.toSet()
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
