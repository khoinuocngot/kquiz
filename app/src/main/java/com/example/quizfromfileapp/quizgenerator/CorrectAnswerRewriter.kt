package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Rewrite đáp án đúng bị copy nguyên từ sourceSnippet.
 *
 * Kỹ thuật rewrite:
 * 1. Rút gọn — lấy phần tóm tắt ngắn nhất của ý chính
 * 2. Định nghĩa lại — "X là Y" → "X là một loại Y"
 * 3. Chức năng — "X dùng để Y" → "X thực hiện chức năng Y"
 * 4. Property — "X có/thuộc tính Y" → "X được biết đến vì Y"
 * 5. Paraphrase đơn giản — đổi cấu trúc câu, giữ nghĩa
 *
 * Nguyên tắc:
 * - Rewrite phải ngắn hơn source rõ ràng
 * - Rewrite phải cùng ngôn ngữ, cùng ngữ cảnh
 * - Không thể rewrite → trả về null (để caller reject)
 */
object CorrectAnswerRewriter {

    private const val TAG = "CorrectAnswerRewriter"

    // Ngưỡng: correct option phải ngắn hơn source ít nhất 30%
    private const val MIN_LENGTH_REDUCTION_RATIO = 0.70

    // Ngưỡng: rewrite phải khác source ít nhất 30% (similarity < 70%)
    private const val MAX_REWRITE_SIMILARITY = 0.70

    /**
     * Rewrite correct option để không còn copy nguyên nguồn.
     *
     * @param correctOption Đáp án đúng bị copy (cần rewrite)
     * @param sourceSnippet Nguồn gốc để tham khảo ngữ cảnh
     * @return Câu đã rewrite, hoặc null nếu không rewrite được
     */
    fun rewriteCorrectOption(correctOption: String, sourceSnippet: String): String? {
        if (correctOption.isBlank()) return null

        val source = sourceSnippet.trim()
        val option = correctOption.trim()

        // Nếu option đã ngắn hơn nhiều so với source → có thể đã OK
        if (option.length < source.length * 0.5) {
            logd("Option đã đủ ngắn (${option.length} vs ${source.length}), không cần rewrite")
            return option
        }

        // Thử từng kỹ thuật rewrite
        val rewritten = tryAllRewriteTechniques(option, source)
            ?: return null

        // Validate: rewritten phải ngắn hơn source
        if (rewritten.length >= source.length) {
            logw("Rewrite không ngắn hơn source: ${rewritten.length} >= ${source.length}")
            return null
        }

        // Validate: rewritten phải khác source (similarity < threshold)
        val sim = SemanticSimilarityHelper.similarity(rewritten, source)
        if (sim > MAX_REWRITE_SIMILARITY) {
            logw("Rewrite vẫn còn giống nguồn: ${String.format("%.2f", sim)}")
            return null
        }

        logd("Rewrite thành công: '${rewritten.take(60)}' (sim=${String.format("%.2f", sim)})")
        return rewritten
    }

    /**
     * Thử tất cả kỹ thuật rewrite, trả về kết quả đầu tiên hợp lệ.
     */
    private fun tryAllRewriteTechniques(option: String, source: String): String? {
        return shortenToEssence(option)
            ?: paraphraseVerbally(option)
            ?: extractFirstClause(option)
            ?: extractSubjectDefinition(option)
    }

    // ─────────────────────────────────────────────────────────────
    // Kỹ thuật 1: Rút gọn còn essence
    // ─────────────────────────────────────────────────────────────

    /**
     * Rút gọn option xuống còn phần essence.
     * Cắt ở ranh giới tự nhiên: dấu phẩy, chấm phẩy, "which", "that".
     */
    private fun shortenToEssence(option: String): String? {
        val trimmed = option.trim()

        // Tìm vị trí cắt tự nhiên
        val cutPoints = listOf(
            trimmed.lastIndexOf(','),
            trimmed.lastIndexOf(';'),
            trimmed.lastIndexOf(" which", ignoreCase = true),
            trimmed.lastIndexOf(" that", ignoreCase = true),
            trimmed.lastIndexOf(" là ", ignoreCase = true),
            trimmed.lastIndexOf(" được ", ignoreCase = true),
            trimmed.lastIndexOf(" có ", ignoreCase = true),
            trimmed.lastIndexOf(" dùng ", ignoreCase = true),
            trimmed.lastIndexOf(" được sử dụng ", ignoreCase = true)
        ).filter { it > trimmed.length / 3 }  // Chỉ cắt ở phần sau

        if (cutPoints.isNotEmpty()) {
            val cutAt = cutPoints.minOrNull() ?: return null
            val shortened = trimmed.substring(0, cutAt).trim()

            if (shortened.length >= trimmed.length * 0.4 && shortened.length >= 8) {
                logd("shortenToEssence: '$shortened'")
                return shortened
            }
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────
    // Kỹ thuật 2: Paraphrase đổi cấu trúc
    // ─────────────────────────────────────────────────────────────

    /**
     * Paraphrase bằng cách đổi cấu trúc câu.
     * VD: "It is used for X" → "Used for X"
     * VD: "X is defined as Y" → "X = Y definition"
     * VD: "X là một loại Y" → "Thuộc loại Y"
     */
    private fun paraphraseVerbally(option: String): String? {
        val o = option.trim()

        // Pattern 1: "It is used for X" / "X is used for Y"
        if (o.contains("used for", ignoreCase = true) || o.contains("dùng để", ignoreCase = true)) {
            val result = o
                .replace(Regex("""^[Ii]t\s+(is\s+)?"""), "")
                .replace(Regex("""\s+is\s+used\s+for\s+""", RegexOption.IGNORE_CASE), ": ")
                .replace(Regex("""\s+used\s+for\s+""", RegexOption.IGNORE_CASE), ": ")
                .replace(Regex("""[,\s]+$"""), "")

            if (result.length < o.length && result.length >= 8) {
                logd("paraphraseVerbally (used-for): '$result'")
                return result
            }
        }

        // Pattern 2: "X is a Y" / "X là một Y"
        if (o.matches(Regex(""".*là\s+một\s+.*""", RegexOption.IGNORE_CASE))) {
            val result = o
                .replace(Regex("""^[^\s]+\s+là\s+một\s+""", RegexOption.IGNORE_CASE), "Loại ")
                .replace(Regex(""",?\s*$"""), "")

            if (result.length < o.length && result.length >= 8) {
                logd("paraphraseVerbally (là một): '$result'")
                return result
            }
        }

        // Pattern 3: "X is known for Y" / "X được biết đến vì Y"
        if (o.contains("known for", ignoreCase = true) || o.contains("biết đến vì", ignoreCase = true)) {
            val result = o
                .replace(Regex("""^[^\s]+\s+is\s+known\s+for\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^[^\s]+\s+được\s+biết\s+đến\s+vì\s+""", RegexOption.IGNORE_CASE), "")

            if (result.length < o.length && result.length >= 8) {
                logd("paraphraseVerbally (known for): '$result'")
                return result
            }
        }

        // Pattern 4: "X is defined as Y" / "X được định nghĩa là Y"
        if (o.contains("defined as", ignoreCase = true) || o.contains("định nghĩa là", ignoreCase = true)) {
            val result = o
                .replace(Regex("""^[^\s]+\s+is\s+defined\s+as\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^[^\s]+\s+được\s+định\s+nghĩa\s+là\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex(""",?\s*$"""), "")

            if (result.length < o.length && result.length >= 8) {
                logd("paraphraseVerbally (defined as): '$result'")
                return result
            }
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────
    // Kỹ thuật 3: Lấy clause đầu tiên
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy clause đầu tiên của câu (trước dấu phẩy/dấu chấm đầu tiên).
     */
    private fun extractFirstClause(option: String): String? {
        val firstComma = option.indexOf(',')
        val firstSemicolon = option.indexOf(';')

        val cutAt = when {
            firstComma > 0 && (firstSemicolon < 0 || firstComma < firstSemicolon) -> firstComma
            firstSemicolon > 0 -> firstSemicolon
            else -> -1
        }

        if (cutAt > option.length * 0.3) {
            val extracted = option.substring(0, cutAt).trim()
            if (extracted.length >= 8 && extracted.length < option.length) {
                logd("extractFirstClause: '$extracted'")
                return extracted
            }
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────
    // Kỹ thuật 4: Lấy phần định nghĩa subject
    // ─────────────────────────────────────────────────────────────

    /**
     * Trích phần định nghĩa của subject khỏi source dài.
     * VD: "Raster graphics is a type of digital image..." → "Raster graphics"
     * Chỉ lấy nếu option bắt đầu bằng subject name.
     */
    private fun extractSubjectDefinition(option: String): String? {
        // Nếu option bắt đầu bằng chữ in hoa/danh từ riêng và dài quá
        if (option.length > 40) {
            // Tìm dấu "." đầu tiên sau ký tự 15
            val dotIdx = option.indexOf('.')
            if (dotIdx in 15..(option.length * 0.6).toInt()) {
                val extracted = option.substring(0, dotIdx).trim()
                if (extracted.length >= 8 && extracted.length < option.length * 0.7) {
                    logd("extractSubjectDefinition: '$extracted'")
                    return extracted
                }
            }
        }

        return null
    }

    /**
     * Rewrite explanation nếu quá dài hoặc copy nguồn.
     */
    fun rewriteExplanation(explanation: String, sourceSnippet: String): String {
        if (explanation.isBlank()) return ""

        val trimmed = explanation.trim()

        // Quá dài → cắt
        if (trimmed.length > 200) {
            val cutAt = trimmed.lastIndexOf('.', 180)
            val truncated = if (cutAt > 100) {
                trimmed.substring(0, cutAt + 1)
            } else {
                trimmed.take(200 - 3) + "..."
            }

            // Nếu vẫn còn giống nguồn → thay bằng mô tả ngắn
            val sim = SemanticSimilarityHelper.similarity(truncated, sourceSnippet)
            if (sim < 0.50) {
                return truncated
            }
        }

        // Vẫn còn giống nguồn → thay bằng ngắn gọn
        val sim = SemanticSimilarityHelper.similarity(trimmed, sourceSnippet)
        if (sim > 0.60) {
            return "Theo nội dung tài liệu, đáp án này được hỗ trợ bởi thông tin trong nguồn."
        }

        return trimmed
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
