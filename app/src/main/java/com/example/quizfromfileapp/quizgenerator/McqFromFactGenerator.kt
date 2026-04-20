package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Sinh MCQ từ atomic facts.
 *
 * Đây là core của chiến lược 2 bước:
 * Bước 1 (FactExtractor): segments → facts
 * Bước 2 (McqFromFactGenerator): facts → MCQ
 *
 * Điểm khác biệt quan trọng so với direct snippet → MCQ:
 * - Question phải hỏi VÀO fact, không phải paraphrase source
 * - Correct option phải là paraphrase NGẮN của factStatement
 * - Không copy factStatement nguyên văn
 * - Distractor chỉ từ cùng concept/topic
 * - Distractor sai theo 1 trong các hướng: property, purpose, quantity, representation
 */
object McqFromFactGenerator {

    private const val TAG = "McqFromFactGenerator"

    /**
     * Sinh câu hỏi từ danh sách fact.
     *
     * @param facts         Danh sách fact đã extract + score
     * @param targetCount  Số câu hỏi cần sinh
     * @param language     "vi" hoặc "en"
     * @param allFacts     Tất cả facts (để lấy distractors cùng concept)
     * @return            Danh sách QuizQuestion đã sinh
     */
    fun generateMcqsFromFacts(
        facts: List<FactItem>,
        targetCount: Int,
        language: String,
        allFacts: List<FactItem>
    ): List<QuizQuestion> {
        if (facts.isEmpty()) {
            logd("generateMcqsFromFacts: facts rỗng → return empty")
            return emptyList()
        }

        // Chỉ dùng facts đã quality-filtered
        val qualityFacts = facts.filter { it.confidence >= FactQualityScorer.MIN_FACT_CONFIDENCE }
            .sortedByDescending { it.confidence }

        if (qualityFacts.isEmpty()) {
            logw("Không có fact đủ chất lượng")
            return emptyList()
        }

        logd("generateMcqsFromFacts: ${qualityFacts.size} quality facts → target $targetCount MCQs")

        val questions = mutableListOf<QuizQuestion>()
        val usedFactIds = mutableSetOf<String>()

        for (fact in qualityFacts) {
            if (questions.size >= targetCount) break
            if (usedFactIds.contains(fact.id)) continue

            val question = generateMcqFromFact(fact, language, allFacts)
            if (question != null) {
                // Validate quality
                val rejection = McqQualityScorer.shouldReject(question)
                if (rejection == null) {
                    questions.add(question)
                    usedFactIds.add(fact.id)
                    logd("MCQ OK: '${question.question.take(40)}'")
                } else {
                    logw("MCQ rejected: ${rejection.message} — '${question.question.take(40)}'")
                }
            }
        }

        logd("generateMcqsFromFacts: ${questions.size} MCQs từ ${qualityFacts.size} facts")
        return questions
    }

    /**
     * Sinh 1 câu hỏi từ 1 fact.
     *
     * Key principles:
     * 1. Question phải hỏi VỀ fact, không phải restatement
     * 2. Correct option = paraphrase ngắn của factStatement
     * 3. Distractors = facts cùng concept nhưng sai theo property/purpose
     */
    private fun generateMcqFromFact(
        fact: FactItem,
        language: String,
        allFacts: List<FactItem>
    ): QuizQuestion? {
        val factStatement = fact.factStatement
        if (factStatement.isBlank()) return null

        // Chọn template
        val template = selectQuestionTemplate(factStatement, language)

        // Build question text
        val questionText = buildQuestionText(factStatement, template, fact.concept, language)
        if (questionText.isBlank()) return null

        // Build correct option — phải là paraphrase, KHÔNG copy factStatement
        val correctOption = buildCorrectOption(factStatement, fact, language)
        if (correctOption.isBlank()) return null

        // Build distractors
        val distractors = buildDistractorsFromFacts(fact, allFacts, correctOption, language)
        if (distractors.size < 3) {
            logw("Không đủ distractors tốt cho fact: '${factStatement.take(30)}'")
            return null
        }

        // Build options
        val allOptions = (distractors + correctOption).shuffled()
        val correctIdx = allOptions.indexOf(correctOption).coerceAtLeast(0)

        // Build explanation
        val explanation = buildExplanation(factStatement, correctOption, language)

        return QuizQuestion(
            id = "mcq_${System.currentTimeMillis()}_${fact.id.take(8)}",
            question = questionText,
            options = allOptions,
            correctAnswerIndex = correctIdx,
            explanation = explanation,
            sourcePageStart = fact.sourcePageStart,
            sourcePageEnd = fact.sourcePageEnd,
            sourceSnippet = fact.sourceSnippet,
            sourceType = fact.sourceType
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Question template selection
    // ─────────────────────────────────────────────────────────────

    private fun selectQuestionTemplate(factStatement: String, language: String): String {
        val templates = if (language == "vi") VI_MCQ_TEMPLATES else EN_MCQ_TEMPLATES

        // Chọn template dựa trên content type của fact
        val contentType = detectFactType(factStatement)

        for (template in templates) {
            if (template.contentTypes.contains(contentType)) {
                return template.pattern
            }
        }

        // Fallback: generic template
        return templates.lastOrNull()?.pattern ?: "What is true about {concept}?"
    }

    private fun detectFactType(factStatement: String): String {
        val lower = factStatement.lowercase()

        return when {
            // Definition / concept
            lower.contains(" is a") || lower.contains(" is an") ||
            lower.contains(" refers to") || lower.contains(" means ") ||
            lower.contains(" là ") || lower.contains(" được hiểu là") ||
            lower.contains("được định nghĩa là") -> "definition"

            // Function / purpose
            lower.contains(" used for") || lower.contains(" purpose") ||
            lower.contains("function") || lower.contains(" role") ||
            lower.contains("dùng để") || lower.contains("chức năng") ||
            lower.contains("vai trò") || lower.contains("mục đích") -> "function"

            // Property / characteristic
            lower.contains(" has ") || lower.contains(" have ") ||
            lower.contains(" contains") || lower.contains(" characterized") ||
            lower.contains("có thuộc tính") || lower.contains("đặc điểm") ||
            lower.contains("tính chất") -> "property"

            // Process / mechanism
            lower.contains(" works by") || lower.contains(" operates") ||
            lower.contains(" process") || lower.contains(" method") ||
            lower.contains("cơ chế") || lower.contains("hoạt động bằng") ||
            lower.contains("phương thức") -> "process"

            // Comparison / contrast
            lower.contains(" compared to") || lower.contains(" differs from") ||
            lower.contains(" unlike") || lower.contains(" similar to") ||
            lower.contains("so sánh") || lower.contains("khác với") ||
            lower.contains("giống như") -> "comparison"

            // Quantity / measurement
            lower.contains(Regex("""\d+\s*(%|percent|pixel|bit|byte|kg|mb|gb|nm|mm|cm|m)""")) ||
            lower.contains(Regex("""\d+\s*(lần|phần|mức|đơn vị|điểm)""")) -> "quantity"

            else -> "general"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Question text building
    // ─────────────────────────────────────────────────────────────

    private fun buildQuestionText(
        factStatement: String,
        template: String,
        concept: String,
        language: String
    ): String {
        // Rút gọn concept cho question
        val shortConcept = concept.take(40)

        return template
            .replace("{concept}", shortConcept)
            .replace("{fact}", factStatement.take(80))
            .replace("{short_fact}", truncateForQuestion(factStatement))
    }

    private fun truncateForQuestion(text: String): String {
        return if (text.length > 80) {
            text.take(77).replace(Regex("""\s+\S*$"""), "") + "..."
        } else {
            text
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Correct option building — KEY: paraphrase, not copy
    // ─────────────────────────────────────────────────────────────

    /**
     * Build correct option từ factStatement.
     *
     * CRITICAL: KHÔNG copy factStatement nguyên văn.
     * Phải paraphrase để:
     * 1. Ngắn hơn rõ ràng
     * 2. Khác với source để không bị detect là copy
     */
    private fun buildCorrectOption(
        factStatement: String,
        fact: FactItem,
        language: String
    ): String {
        // Thử paraphrase bằng CorrectAnswerRewriter trước
        val rewritten = CorrectAnswerRewriter.rewriteCorrectOption(factStatement, fact.sourceSnippet)
        if (rewritten != null && rewritten.length < factStatement.length * 0.8) {
            logd("CorrectOption rewritten: '${factStatement.take(40)}' → '${rewritten.take(40)}'")
            return rewritten
        }

        // Fallback: rút gọn bằng shortenToEssence
        val shortened = CorrectAnswerRewriter.rewriteCorrectOption(factStatement, fact.sourceSnippet)
        if (shortened != null) {
            return shortened
        }

        // Last resort: cắt ở clause đầu
        val firstClause = factStatement.split(Regex("""[,\.]+""")).firstOrNull()?.trim() ?: factStatement
        if (firstClause.length >= 15 && firstClause.length < factStatement.length) {
            return firstClause
        }

        // Không có cách nào paraphrase → skip fact này
        logw("Cannot paraphrase factStatement: '${factStatement.take(40)}'")
        return ""
    }

    // ─────────────────────────────────────────────────────────────
    // Distractor building — cùng concept, sai vừa phải
    // ─────────────────────────────────────────────────────────────

    /**
     * Build 3 distractors từ facts cùng concept.
     *
     * Distractor strategy:
     * 1. Lấy facts cùng concept hoặc cùng topic group
     * 2. Biến đổi fact thành distractor theo 1 trong các hướng:
     *    - Property wrong: đổi thuộc tính
     *    - Purpose wrong: đổi mục đích
     *    - Quantity wrong: đổi số liệu
     *    - Representation wrong: đổi cách biểu diễn
     *    - Relationship wrong: đổi quan hệ
     * 3. Validate: distractor phải khác correct đủ nhiều, cùng topic
     */
    private fun buildDistractorsFromFacts(
        sourceFact: FactItem,
        allFacts: List<FactItem>,
        correctOption: String,
        language: String
    ): List<String> {
        // Lấy facts cùng concept (nhưng khác fact)
        val sameConceptFacts = allFacts
            .filter { it.id != sourceFact.id }
            .filter { areSameTopic(it, sourceFact) }
            .take(10)

        if (sameConceptFacts.isEmpty()) {
            logw("Không có fact cùng concept cho distractor")
            return emptyList()
        }

        val distractors = mutableListOf<String>()

        for (fact in sameConceptFacts.shuffled()) {
            if (distractors.size >= 3) break

            val distractor = transformFactToDistractor(fact, correctOption, language)
            if (distractor != null && isValidDistractor(distractor, correctOption, sourceFact.factStatement)) {
                distractors.add(distractor)
            }
        }

        return distractors
    }

    /**
     * Kiểm tra 2 facts có cùng topic không.
     */
    private fun areSameTopic(fact1: FactItem, fact2: FactItem): Boolean {
        // Cùng page → cùng topic
        if (fact1.sourcePageStart != null && fact2.sourcePageStart != null) {
            if (fact1.sourcePageStart == fact2.sourcePageStart) return true
        }

        // So sánh concept overlap
        val sim = SemanticSimilarityHelper.tokenBasedSimilarity(
            fact1.concept + " " + fact1.factStatement,
            fact2.concept + " " + fact2.factStatement
        )
        return sim > 0.25
    }

    /**
     * Biến fact thành distractor.
     *
     * Các transformation strategies:
     * 1. Rút gọn fact → nếu trùng key term thì sai về property
     * 2. Đổi số liệu nếu có
     * 3. Đổi key term → đổi quan hệ/association
     */
    private fun transformFactToDistractor(
        fact: FactItem,
        correctOption: String,
        language: String
    ): String? {
        val factText = fact.factStatement

        // Strategy 1: Rút gọn và biến thành statement sai
        val shortened = CorrectAnswerRewriter.rewriteCorrectOption(factText, fact.sourceSnippet)
        if (shortened != null && shortened.length >= 10 && shortened != factText) {
            // Thêm từ sai nếu cần
            val distractor = addPlausibleError(shortened, correctOption, language)
            return distractor
        }

        // Strategy 2: Lấy partial fact — bỏ phần quan trọng
        val partial = getPartialFact(factText, language)
        if (partial != null) {
            return partial
        }

        // Strategy 3: Fallback — trả về fact đã rút gọn
        return if (factText.length in 15..120) {
            factText
        } else {
            factText.take(80).replace(Regex("""\s+\S*$"""), "") + "..."
        }
    }

    /**
     * Thêm plausible error vào fact để biến thành distractor.
     */
    private fun addPlausibleError(
        fact: String,
        correctOption: String,
        language: String
    ): String {
        // Nếu fact trùng quá nhiều với correct → biến thành sai
        val sim = SemanticSimilarityHelper.similarity(fact, correctOption)
        if (sim > 0.70) {
            // Đảo ngược nghĩa hoặc bỏ từ quan trọng
            val words = fact.split(Regex("""\s+"""))
            if (words.size > 3) {
                // Bỏ từ giữa → thay bằng generic
                val middle = words.drop(1).dropLast(1)
                if (middle.isNotEmpty()) {
                    val replaced = words.first() + " " + middle.joinToString(" ") + " " + words.last()
                    return replaced
                }
            }
        }

        return fact
    }

    /**
     * Lấy partial fact — bỏ đi phần quan trọng để tạo distractor.
     */
    private fun getPartialFact(fact: String, language: String): String? {
        val sentences = fact.split(Regex("""[.!]""")).filter { it.trim().length > 10 }
        if (sentences.isEmpty()) return null

        // Lấy sentence không phải đầu tiên
        for (s in sentences.drop(1)) {
            val trimmed = s.trim()
            if (trimmed.length >= 15) return trimmed
        }

        // Fallback: lấy nửa sau
        val half = sentences.first().trim().takeLast(fact.length / 2)
        return if (half.length >= 15) half else null
    }

    private fun isValidDistractor(
        distractor: String,
        correctOption: String,
        sourceFact: String
    ): Boolean {
        if (distractor.isBlank() || distractor.length < 10) return false

        // Phải khác correct option đủ nhiều
        val diffWithCorrect = SemanticSimilarityHelper.similarity(distractor, correctOption)
        if (diffWithCorrect > 0.75) {
            logw("Distractor quá giống correct: sim=${String.format("%.2f", diffWithCorrect)}")
            return false
        }

        // Phải cùng topic với source fact
        val topicSim = SemanticSimilarityHelper.tokenBasedSimilarity(distractor, sourceFact)
        if (topicSim < 0.20) {
            logw("Distractor không cùng topic: sim=$topicSim")
            return false
        }

        return true
    }

    // ─────────────────────────────────────────────────────────────
    // Explanation building
    // ─────────────────────────────────────────────────────────────

    private fun buildExplanation(
        factStatement: String,
        correctOption: String,
        language: String
    ): String {
        val snippet = if (factStatement.length > 80) {
            factStatement.take(80).replace(Regex("""\s+\S*$"""), "") + "..."
        } else {
            factStatement
        }

        return if (language == "vi") {
            "Đáp án đúng dựa trên: \"$snippet\""
        } else {
            "The answer is supported by: \"$snippet\""
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Templates
    // ─────────────────────────────────────────────────────────────

    private data class McqTemplate(
        val pattern: String,
        val contentTypes: Set<String>
    )

    private val EN_MCQ_TEMPLATES = listOf(
        McqTemplate("Which statement is true about {concept}?", setOf("general", "definition")),
        McqTemplate("What is correct about {concept}?", setOf("definition", "property")),
        McqTemplate("Which description best matches {concept}?", setOf("definition", "function")),
        McqTemplate("What does the text indicate about {concept}?", setOf("function", "process")),
        McqTemplate("Which statement correctly explains {concept}?", setOf("definition", "comparison")),
        McqTemplate("What is the primary characteristic of {concept}?", setOf("property")),
        McqTemplate("How does {concept} primarily function?", setOf("function", "process")),
        McqTemplate("Which option accurately reflects the concept of {concept}?", setOf("general")),
        McqTemplate("Based on the content, what is true about {concept}?", setOf("general")),
        McqTemplate("Which statement best reflects the information about {concept}?", setOf("comparison"))
    )

    private val VI_MCQ_TEMPLATES = listOf(
        McqTemplate("Phát biểu nào đúng về {concept}?", setOf("general", "definition")),
        McqTemplate("Điều gì chính xác về {concept}?", setOf("definition", "property")),
        McqTemplate("Mô tả nào phù hợp nhất với {concept}?", setOf("definition", "function")),
        McqTemplate("Theo nội dung, điều gì được chỉ ra về {concept}?", setOf("function", "process")),
        McqTemplate("Phát biểu nào giải thích đúng về {concept}?", setOf("definition", "comparison")),
        McqTemplate("Thuộc tính chính của {concept} là gì?", setOf("property")),
        McqTemplate("Chức năng chính của {concept} là gì?", setOf("function", "process")),
        McqTemplate("Lựa chọn nào phản ánh đúng khái niệm {concept}?", setOf("general")),
        McqTemplate("Theo nội dung, điều gì đúng về {concept}?", setOf("general")),
        McqTemplate("Phát biểu nào phản ánh tốt nhất thông tin về {concept}?", setOf("comparison"))
    )

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }
}
