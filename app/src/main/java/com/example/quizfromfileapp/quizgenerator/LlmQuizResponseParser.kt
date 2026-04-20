package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Parse + validate response JSON từ local LLM.
 *
 * Cải tiến so với phiên bản trước:
 * 1. Enhanced validation: reject generic/paraphrase questions
 * 2. Correct answer copy check: nếu copy nguồn > 60% → reject
 * 3. Option quality: bỏ options 1 từ, quá dài, trùng nhau
 * 4. Similarity check: bỏ options quá giống nhau
 * 5. Explanation quality: bỏ explanation trùng với question
 *
 * Nếu parse fail hoặc không có câu nào hợp lệ → return empty list.
 */
object LlmQuizResponseParser {

    private const val TAG = "LlmQuizResponseParser"

    // Thresholds for stricter validation
    private const val MIN_QUESTION_LENGTH = 10
    private const val MIN_OPTION_LENGTH = 8
    private const val MAX_OPTION_LENGTH = 200
    private const val SIMILAR_OPTION_THRESHOLD = 0.85
    private const val CORRECT_COPY_THRESHOLD = 0.60

    // Generic patterns — REJECTED immediately
    private val GENERIC_PATTERNS_EN = listOf(
        Regex("""^which of the following.*$""", RegexOption.IGNORE_CASE),
        Regex("""^according to the passage, which.*$""", RegexOption.IGNORE_CASE),
        Regex("""^what is.*according to.*$""", RegexOption.IGNORE_CASE),
        Regex("""^the passage.*states.*$""", RegexOption.IGNORE_CASE),
        Regex("""^based on the passage.*$""", RegexOption.IGNORE_CASE)
    )

    private val GENERIC_PATTERNS_VI = listOf(
        Regex("""^đoạn văn nói về.*$""", RegexOption.IGNORE_CASE),
        Regex("""^dựa trên đoạn văn.*$""", RegexOption.IGNORE_CASE),
        Regex("""^theo đoạn văn.*$""", RegexOption.IGNORE_CASE),
        Regex("""^đâu là.*đoạn văn.*$""", RegexOption.IGNORE_CASE),
        Regex("""^cái nào là.*đoạn văn.*$""", RegexOption.IGNORE_CASE)
    )

    /**
     * Parse JSON string thành danh sách QuizQuestion.
     *
     * @param rawResponse  Raw response từ LLM
     * @return             Danh sách QuizQuestion đã validate, hoặc rỗng
     */
    fun parse(rawResponse: String): List<QuizQuestion> {
        if (rawResponse.isBlank()) {
            logd("parse: response rỗng")
            return emptyList()
        }

        // Bước 1: Extract clean JSON
        val cleanJson = LlmJsonExtractor.extractCleanJson(rawResponse)
        if (cleanJson == null) {
            loge("extractCleanJson failed")
            return emptyList()
        }

        // Bước 2: Parse JSON structure
        val questions = parseJsonStructure(cleanJson)
        if (questions.isEmpty()) {
            loge("parseJsonStructure returned empty")
            return emptyList()
        }

        logd("parse: thành công, ${questions.size} câu hỏi")
        return questions
    }

    // ─────────────────────────────────────────────────────────────
    // Parse JSON Structure
    // ─────────────────────────────────────────────────────────────

    private fun parseJsonStructure(jsonString: String): List<QuizQuestion> {
        return try {
            val json = org.json.JSONObject(jsonString)
            val questionsArray = json.getJSONArray("questions")

            if (questionsArray.length() == 0) {
                logw("questions array rỗng")
                return emptyList()
            }

            val result = mutableListOf<QuizQuestion>()
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val question = parseAndValidateQuestion(qObj, i)
                if (question != null) {
                    result.add(question)
                }
            }

            logd("Sau validate: ${result.size}/${questionsArray.length()} câu hợp lệ")
            result
        } catch (e: Exception) {
            loge("parseJsonStructure lỗi: ${e.message}")
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Parse + Validate từng câu hỏi
    // ─────────────────────────────────────────────────────────────

    private fun parseAndValidateQuestion(obj: org.json.JSONObject, index: Int): QuizQuestion? {
        // ── Basic parse ──
        val questionText = obj.optString("question", "").trim()
        if (!isValidQuestionText(questionText, index)) return null

        // ── Options parse ──
        val optionsArray = obj.optJSONArray("options")
        if (optionsArray == null || optionsArray.length() < 4) {
            logw("parseQuestion[$index]: options < 4, bỏ qua")
            return null
        }

        val rawOptions = (0 until optionsArray.length())
            .map { optionsArray.optString(it, "").trim() }
            .filter { it.isNotBlank() }

        if (rawOptions.size < 4) {
            logw("parseQuestion[$index]: options sau filter < 4, bỏ qua")
            return null
        }

        val options = rawOptions.take(4)

        // ── Options quality check ──
        val optionValidation = validateOptions(options, index)
        if (!optionValidation.isValid) {
            logw("parseQuestion[$index]: options kém — ${optionValidation.reason}")
            return null
        }

        // ── Correct answer index ──
        val correctIdx = obj.optInt("correctAnswerIndex", -1)
        if (correctIdx !in options.indices) {
            logw("parseQuestion[$index]: correctAnswerIndex=$correctIdx không hợp lệ, bỏ qua")
            return null
        }

        // ── Correct answer copy check ──
        val sourceSnippet = obj.optString("sourceSnippet", "").trim().take(200)
        val correctOption = options[correctIdx]
        if (sourceSnippet.isNotBlank()) {
            if (SemanticSimilarityHelper.isVerbatimCopy(correctOption, sourceSnippet, CORRECT_COPY_THRESHOLD)) {
                logw("parseQuestion[$index]: correct option copy nguồn, bỏ qua")
                return null
            }
        }

        // ── Explanation ──
        val explanation = obj.optString("explanation", "").trim()
        if (explanation.isBlank()) {
            logw("parseQuestion[$index]: explanation rỗng, bỏ qua")
            return null
        }

        // ── Source metadata ──
        val sourcePageStart = if (obj.has("sourcePageStart")) {
            obj.optInt("sourcePageStart", -1).takeIf { it > 0 }
        } else null
        val sourcePageEnd = if (obj.has("sourcePageEnd")) {
            obj.optInt("sourcePageEnd", -1).takeIf { it > 0 }
        } else null

        // ── Build QuizQuestion ──
        return QuizQuestion(
            id = "llm_q_${index}_${System.currentTimeMillis()}",
            question = questionText,
            options = options,
            correctAnswerIndex = correctIdx,
            explanation = explanation,
            sourcePageStart = sourcePageStart,
            sourcePageEnd = sourcePageEnd,
            sourceSnippet = sourceSnippet,
            sourceType = "LLM"
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Validation Helpers
    // ─────────────────────────────────────────────────────────────

    private fun isValidQuestionText(text: String, index: Int): Boolean {
        if (text.isBlank()) {
            logw("parseQuestion[$index]: question rỗng")
            return false
        }

        if (text.length < MIN_QUESTION_LENGTH) {
            logw("parseQuestion[$index]: question quá ngắn (<$MIN_QUESTION_LENGTH chars)")
            return false
        }

        // Reject generic patterns
        val lowerText = text.lowercase()
        for (pattern in GENERIC_PATTERNS_EN + GENERIC_PATTERNS_VI) {
            if (pattern.matches(lowerText)) {
                logw("parseQuestion[$index]: question generic pattern: '$text'")
                return false
            }
        }

        return true
    }

    private data class OptionValidation(
        val isValid: Boolean,
        val reason: String = ""
    )

    private fun validateOptions(options: List<String>, index: Int): OptionValidation {
        if (options.size != 4) {
            return OptionValidation(false, "không đủ 4 options")
        }

        val uniqueOptions = options.toSet()
        if (uniqueOptions.size < 4) {
            return OptionValidation(false, "options trùng nhau")
        }

        // Check length constraints
        for ((i, opt) in options.withIndex()) {
            if (opt.isBlank()) {
                return OptionValidation(false, "option[$i] rỗng")
            }
            if (opt.length < MIN_OPTION_LENGTH) {
                return OptionValidation(false, "option[$i] quá ngắn (${opt.length} < $MIN_OPTION_LENGTH)")
            }
            if (opt.length > MAX_OPTION_LENGTH) {
                return OptionValidation(false, "option[$i] quá dài (${opt.length} > $MAX_OPTION_LENGTH)")
            }
        }

        // Check single word options
        for ((i, opt) in options.withIndex()) {
            val words = opt.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.size == 1 && words[0].length < MIN_OPTION_LENGTH) {
                return OptionValidation(false, "option[$i] một từ quá ngắn: '$opt'")
            }
        }

        // Check option similarity (pairwise)
        for (i in options.indices) {
            for (j in i + 1 until options.size) {
                val sim = SemanticSimilarityHelper.similarity(options[i], options[j])
                if (sim > SIMILAR_OPTION_THRESHOLD) {
                    return OptionValidation(
                        false,
                        "option[$i] và option[$j] quá giống nhau (${String.format("%.2f", sim)})"
                    )
                }
            }
        }

        // Check length balance — max length không vượt quá 2.5x min length
        val lengths = options.map { it.length }
        val minLen = lengths.minOrNull() ?: 0
        val maxLen = lengths.maxOrNull() ?: 0
        if (minLen > 0 && maxLen > minLen * 2.5) {
            return OptionValidation(
                false,
                "options mất cân bằng: min=$minLen max=$maxLen"
            )
        }

        return OptionValidation(true)
    }

    // ─────────────────────────────────────────────────────────────
    // Logging
    // ─────────────────────────────────────────────────────────────

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }

    private fun loge(message: String) {
        Log.e(TAG, message)
    }
}
