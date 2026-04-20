package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Chấm điểm chất lượng của một FactItem.
 *
 * Fact yếu → reject trước khi sinh MCQ.
 *
 * Scoring dimensions:
 * 1. Fact completeness — có phải là atomic fact thật sự không
 * 2. Specificity — có thông tin cụ thể không (không phải generic)
 * 3. Knowledge density — có nhiều knowledge không (vs filler words)
 * 4. Source quality — nguồn có đáng tin cậy không
 */
object FactQualityScorer {

    private const val TAG = "FactQualityScorer"

    /** Ngưỡng tối thiểu để chấp nhận fact */
    const val MIN_FACT_CONFIDENCE = 0.35f

    /** Trọng số cho từng dimension */
    private const val WEIGHT_COMPLETENESS = 0.30f
    private const val WEIGHT_SPECIFICITY = 0.35f
    private const val WEIGHT_KNOWLEDGE_DENSITY = 0.20f
    private const val WEIGHT_SOURCE_QUALITY = 0.15f

    /**
     * Chấm điểm fact.
     *
     * @param factStatement  Câu fact
     * @param concept        Concept đã extract
     * @param sourceSnippet  Nguồn gốc
     * @param language       "vi" hoặc "en"
     * @return Điểm confidence 0.0 → 1.0
     */
    fun scoreFact(
        factStatement: String,
        concept: String,
        sourceSnippet: String,
        language: String
    ): Float {
        val c = scoreCompleteness(factStatement)
        val s = scoreSpecificity(factStatement, concept, language)
        val k = scoreKnowledgeDensity(factStatement)
        val q = scoreSourceQuality(sourceSnippet)

        val total = c * WEIGHT_COMPLETENESS +
                s * WEIGHT_SPECIFICITY +
                k * WEIGHT_KNOWLEDGE_DENSITY +
                q * WEIGHT_SOURCE_QUALITY

        logd("scoreFact: C=${String.format("%.2f", c)} S=${String.format("%.2f", s)} K=${String.format("%.2f", k)} Q=${String.format("%.2f", q)} → ${String.format("%.2f", total)}")
        logd("  fact: '${factStatement.take(50)}'")

        return total.coerceIn(0f, 1f)
    }

    // ─────────────────────────────────────────────────────────────
    // Completeness: fact có nguyên vẹn, có nghĩa không
    // ─────────────────────────────────────────────────────────────

    /**
     * Fact nguyên vẹn: có chủ ngữ, vị ngữ, có nghĩa.
     */
    private fun scoreCompleteness(factStatement: String): Float {
        val trimmed = factStatement.trim()
        if (trimmed.isEmpty()) return 0f

        var score = 0.5f

        // Có độ dài hợp lý (30-200 chars)
        if (trimmed.length in 40..150) {
            score += 0.2f
        } else if (trimmed.length in 30..200) {
            score += 0.1f
        }

        // Có verb chính
        if (hasMainVerb(trimmed)) {
            score += 0.2f
        }

        // Không kết thúc bằng preposition/particle
        val lastWord = trimmed.split(Regex("""\s+""")).lastOrNull()?.lowercase() ?: ""
        val trailingParticles = setOf(
            "and", "or", "but", "with", "for", "to", "of", "in", "on", "at",
            "và", "hoặc", "nhưng", "với", "cho", "của", "trong", "ở", "tại"
        )
        if (lastWord !in trailingParticles) {
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun hasMainVerb(text: String): Boolean {
        val verbPatterns = listOf(
            Regex("""\b(is|are|was|were|has|have|had|does|do|did|will|would|can|could|may|might|should|must)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(là|được|có|không|được|đã|sẽ|đang|để|vào|bằng)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\w+ed|ing|s|es)\b""", RegexOption.IGNORE_CASE)  // English morphological verbs
        )
        return verbPatterns.any { it.containsMatchIn(text) }
    }

    // ─────────────────────────────────────────────────────────────
    // Specificity: fact có thông tin cụ thể không
    // ─────────────────────────────────────────────────────────────

    /**
     * Fact cụ thể: có số, tên riêng, thuật ngữ, chi tiết.
     * Generic statements → low score.
     */
    private fun scoreSpecificity(factStatement: String, concept: String, language: String): Float {
        val text = factStatement.trim()
        if (text.isEmpty()) return 0f

        var score = 0.3f

        // Có số/counts
        val hasNumbers = Regex("""\b\d[\d,\.\-\+\%]*\b""").containsMatchIn(text)
        if (hasNumbers) {
            score += 0.25f
        }

        // Có thuật ngữ kỹ thuật
        if (hasTechnicalTerms(text)) {
            score += 0.20f
        }

        // Có tên riêng (capitalized words)
        val properNouns = Regex("""\b[A-Z][a-z]+(?:\s+[A-Z][a-z]+)*\b""").findAll(text)
        if (properNouns.count() >= 1) {
            score += 0.15f
        }

        // Generic indicators → penalize
        val genericIndicators = listOf(
            "something", "some thing", "it is", "this is", "there is", "there are",
            "một điều", "một thứ", "cái đó", "nó là", "đây là", "có cái"
        )
        val hasGeneric = genericIndicators.any { text.lowercase().contains(it) }
        if (hasGeneric) {
            score -= 0.20f
        }

        // Không có concept trong fact → generic
        if (concept.isNotBlank() && !text.lowercase().contains(concept.lowercase())) {
            score -= 0.10f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun hasTechnicalTerms(text: String): Boolean {
        val techIndicators = listOf(
            // EN
            "algorithm", "function", "method", "process", "system", "data", "model",
            "network", "layer", "pixel", "vector", "format", "protocol", "database",
            "interface", "framework", "component", "module", "parameter", "variable",
            "API", "CPU", "GPU", "RAM", "XML", "JSON", "HTML", "CSS", "SQL",
            "HTTP", "TCP", "IP", "DNS", "SSL", "TLS", "encryption", "compression",
            "ML", "AI", "CNN", "RNN", "LSTM", "transformer", "gradient", "neural",
            // VI
            "thuật toán", "chức năng", "phương pháp", "quy trình", "hệ thống",
            "dữ liệu", "mô hình", "mạng", "lớp", "điểm ảnh", "vec-tơ", "định dạng",
            "giao thức", "cơ sở dữ liệu", "giao diện", "khung làm việc", "thành phần",
            "tham số", "biến số", "nén", "mã hóa", "học máy", "trí tuệ nhân tạo"
        )

        val lower = text.lowercase()
        return techIndicators.any { lower.contains(it) }
    }

    // ─────────────────────────────────────────────────────────────
    // Knowledge density: filler vs knowledge
    // ─────────────────────────────────────────────────────────────

    /**
     * Knowledge density: tỷ lệ từ có nghĩa / tổng từ.
     */
    private fun scoreKnowledgeDensity(factStatement: String): Float {
        val words = factStatement.split(Regex("""[\s,\.\!\?\;\:]+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 0f

        val fillerWords = setOf(
            // EN
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "can", "that", "which", "who", "whom", "this", "these",
            "those", "it", "its", "of", "in", "to", "for", "on", "with", "at", "by",
            "from", "as", "or", "and", "but", "if", "then", "else", "when", "where",
            // VI
            "và", "là", "của", "trong", "được", "có", "không", "để", "từ", "với",
            "cho", "này", "khi", "đã", "sẽ", "mà", "ra", "vào", "ở", "theo"
        )

        val meaningfulWords = words.filter {
            val lower = it.lowercase().replace(Regex("""[^\p{L}]"""), "")
            lower.length >= 3 && !fillerWords.contains(lower)
        }

        val density = meaningfulWords.size.toFloat() / words.size
        return density.coerceIn(0f, 1f)
    }

    // ─────────────────────────────────────────────────────────────
    // Source quality: nguồn có đáng tin cậy không
    // ─────────────────────────────────────────────────────────────

    /**
     * Source quality: dựa trên độ dài và uniqueness.
     * Nguồn quá ngắn hoặc trùng lặp → low score.
     */
    private fun scoreSourceQuality(sourceSnippet: String): Float {
        if (sourceSnippet.isBlank()) return 0.2f

        var score = 0.5f

        // Có độ dài tốt
        if (sourceSnippet.length in 50..300) {
            score += 0.3f
        } else if (sourceSnippet.length in 30..500) {
            score += 0.15f
        }

        // Không có indicators low-quality
        val lowQualityIndicators = listOf(
            "...",
            "[edit]",
            "TODO",
            "FIXME",
            "TBD",
            "undefined",
            "placeholder",
            "copyright",
            "©", "™", "®"
        )
        val hasLowQuality = lowQualityIndicators.any { sourceSnippet.contains(it) }
        if (hasLowQuality) {
            score -= 0.2f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Filter và rank facts theo confidence.
     */
    fun filterAndRank(
        facts: List<FactItem>,
        minConfidence: Float = MIN_FACT_CONFIDENCE
    ): List<FactItem> {
        return facts
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }
}
