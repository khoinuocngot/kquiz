package com.example.quizfromfileapp.quizgenerator

import android.content.Context
import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Implementation thật cho LocalLlmQuizGenerator — hỗ trợ 2 chiến lược:
 *
 * 1. TWO-STEP (ưu tiên):
 *    Bước 1: FactExtractor → LLM trích facts từ segments
 *    Bước 2: McqFromFactGenerator → LLM sinh MCQ từ facts
 *    → Chất lượng cao hơn, ít copy nguồn hơn
 *
 * 2. DIRECT (fallback):
 *    segment → LLM → MCQ trực tiếp
 *    → Dùng khi two-step fail hoặc model không hỗ trợ
 *
 * Pipeline quality đầy đủ:
 * 1. Fact extraction prompt (LLM)
 * 2. Fact parsing + FactQualityScorer
 * 3. MCQ from facts prompt (LLM)
 * 4. MCQ parsing + LlmQuizResponseParser
 * 5. LlmQuestionPostProcessor (quality pipeline)
 *
 * KHÔNG crash — mọi lỗi đều return empty để HybridQuizGenerator fallback.
 */
class OnDeviceLlmQuizGenerator(
    private val context: Context,
    private val llmStateCallback: LlmStateCallback? = null
) : LocalLlmQuizGenerator {

    private val llmManager = LocalLlmManager(context)

    private companion object {
        const val TAG = "OnDeviceLlmQuizGenerator"
        /** Số fact tối đa dùng để sinh MCQ mỗi batch */
        const val MAX_FACTS_PER_MCQ_BATCH = 8
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val available = LlmAvailabilityChecker.isModelAvailable(context)
            logd("isAvailable: $available")
            available
        } catch (e: Exception) {
            loge("isAvailable lỗi: ${e.message}")
            false
        }
    }

    override suspend fun generateQuestions(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        if (segments.isEmpty()) {
            logw("generateQuestions: segments rỗng → return empty")
            return emptyList()
        }

        if (targetCount <= 0) {
            logw("generateQuestions: targetCount=$targetCount → return empty")
            return emptyList()
        }

        return try {
            // Thử 2-step trước, fallback sang direct nếu fail
            generateWithTwoStep(segments, targetCount, language)
        } catch (e: Exception) {
            logw("Two-step thất bại: ${e.message} → fallback direct")
            generateWithDirectBatches(segments, targetCount, language)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TWO-STEP APPROACH (ưu tiên)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Chiến lược 2 bước:
     *
     * Bước 1: Trích facts từ segments (LLM call)
     * Bước 2: Sinh MCQ từ facts (LLM call)
     *
     * Ưu điểm:
     * - Question phải hỏi vào fact, không phải paraphrase source
     * - Correct option phải paraphrase fact (ngắn hơn, khác nguồn)
     * - Distractor cùng concept với fact
     */
    private suspend fun generateWithTwoStep(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        logd("=== TWO-STEP APPROACH START ===")

        llmStateCallback?.onLlmStateChanged(
            LlmGenerationUiState.Generating("Bước 1: Trích xuất facts...")
        )

        // Bước 1: Trích facts từ tất cả segments
        val allFacts = extractFactsWithLlm(segments, language)
        if (allFacts.isEmpty()) {
            logw("Two-step: không trích được fact nào → fallback direct")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Không trích được facts từ nội dung")
            )
            return emptyList()
        }

        logd("Two-step: trích được ${allFacts.size} facts")

        // Bước 2: Sinh MCQ từ facts
        llmStateCallback?.onLlmStateChanged(
            LlmGenerationUiState.Generating("Bước 2: Sinh câu hỏi từ facts...")
        )

        val questions = generateMcqsFromFactsWithLlm(allFacts, targetCount, language)

        if (questions.isEmpty()) {
            logw("Two-step: không sinh được MCQ → fallback direct")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Không sinh được câu hỏi từ facts")
            )
            return emptyList()
        }

        logd("=== TWO-STEP APPROACH END: ${questions.size} questions ===")
        return questions
    }

    /**
     * Bước 1: Trích facts từ segments bằng LLM.
     */
    private suspend fun extractFactsWithLlm(
        segments: List<ContentSegment>,
        language: String
    ): List<FactItem> {
        val allFacts = mutableListOf<FactItem>()

        // Gom segments thành batches để extract facts
        val batches = segments.chunked(LlmPromptBuilder.maxSegmentsPerBatch())

        for ((batchIdx, batch) in batches.withIndex()) {
            logd("Extract batch ${batchIdx + 1}/${batches.size}: ${batch.size} segments")

            val prompt = LlmPromptBuilder.buildFactExtractionPrompt(batch, language)
            if (prompt.isBlank()) continue

            val rawResponse: String
            try {
                rawResponse = llmManager.generateResponse(prompt)
            } catch (e: Exception) {
                logw("Fact extraction batch ${batchIdx + 1} fail: ${e.message}")
                continue
            }

            val facts = FactJsonParser.parse(rawResponse)
            if (facts.isEmpty()) {
                logw("Fact extraction batch ${batchIdx + 1} parse rỗng")
                continue
            }

            // Map source info từ segments vào facts
            val mappedFacts = facts.mapIndexed { idx, fact ->
                val sourceSegment = batch.getOrNull(idx % batch.size)
                fact.copy(
                    sourcePageStart = fact.sourcePageStart ?: sourceSegment?.sourcePageStart,
                    sourcePageEnd = fact.sourcePageEnd ?: sourceSegment?.sourcePageEnd,
                    sourceType = fact.sourceType,
                    sourceSnippet = fact.sourceSnippet.ifBlank { sourceSegment?.sourceSnippet ?: "" }
                )
            }

            allFacts.addAll(mappedFacts)
            logd("Fact extraction batch ${batchIdx + 1}: ${facts.size} facts")
        }

        if (allFacts.isEmpty()) return emptyList()

        // Deduplicate
        val uniqueFacts = allFacts.distinctBy {
            SemanticSimilarityHelper.normalizeForComparison(it.factStatement)
        }

        // Score và filter bằng FactQualityScorer
        val scoredFacts = uniqueFacts.map { fact ->
            fact.copy(confidence = FactQualityScorer.scoreFact(
                factStatement = fact.factStatement,
                concept = fact.concept,
                sourceSnippet = fact.sourceSnippet,
                language = "vi"  // TODO: detect language
            ))
        }

        val qualityFacts = FactQualityScorer.filterAndRank(scoredFacts)
        logd("After dedup+score: ${qualityFacts.size}/${allFacts.size} quality facts")

        return qualityFacts
    }

    /**
     * Bước 2: Sinh MCQ từ facts bằng LLM.
     */
    private suspend fun generateMcqsFromFactsWithLlm(
        facts: List<FactItem>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        val allQuestions = mutableListOf<QuizQuestion>()
        var remainingCount = targetCount
        var batchNum = 0

        while (remainingCount > 0 && batchNum < LlmGenerationConfig.MAX_BATCHES) {
            batchNum++

            val batchFacts = facts
                .filter { !allQuestions.any { q -> q.sourceSnippet.contains(it.factStatement.take(20)) } }
                .take(MAX_FACTS_PER_MCQ_BATCH)

            if (batchFacts.size < 1) {
                logw("Hết facts cho batch $batchNum")
                break
            }

            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.Generating("Sinh batch $batchNum...")
            )

            val questions = generateMcqBatchFromFacts(batchFacts, targetCount.coerceAtMost(2), language)
            if (questions.isEmpty()) {
                logw("MCQ batch $batchNum fail → thử batch khác")
                if (batchNum >= LlmGenerationConfig.MAX_BATCHES) break
                continue
            }

            allQuestions.addAll(questions)
            remainingCount = targetCount - allQuestions.size
            logd("MCQ batch $batchNum: ${questions.size} questions, tổng: ${allQuestions.size}/$targetCount")

            if (remainingCount <= 0 || batchNum >= LlmGenerationConfig.MAX_BATCHES) break
        }

        // Quality post-processing
        val sourceSnippets = facts.map { it.sourceSnippet }
        val finalQuestions = LlmQuestionPostProcessor.process(
            questions = allQuestions,
            segments = emptyList(),  // Facts không cần segments
            sourceSnippets = sourceSnippets
        )

        // Report quality
        if (finalQuestions.isEmpty()) {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Tất cả MCQ từ facts bị reject")
            )
        } else if (finalQuestions.size < targetCount) {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccessPartial(
                    llmCount = finalQuestions.size,
                    targetCount = targetCount,
                    message = "Từ facts: ${finalQuestions.size}/$targetCount câu chất lượng"
                )
            )
        } else {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccess(
                    questionCount = finalQuestions.size,
                    message = "Từ facts: ${finalQuestions.size} câu"
                )
            )
        }

        return finalQuestions
    }

    /**
     * Sinh một batch MCQ từ facts.
     */
    private suspend fun generateMcqBatchFromFacts(
        facts: List<FactItem>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        val prompt = LlmPromptBuilder.buildMcqFromFactsPrompt(facts, targetCount, language)
        if (prompt.isBlank()) {
            logw("MCQ prompt rỗng")
            return emptyList()
        }

        val rawResponse: String
        try {
            rawResponse = llmManager.generateResponse(prompt)
        } catch (e: Exception) {
            logw("MCQ inference fail: ${e.message}")
            return emptyList()
        }

        val cleanJson = LlmJsonExtractor.extractCleanJson(rawResponse)
        if (cleanJson == null) {
            logw("MCQ JSON extract fail")
            return emptyList()
        }

        val parsedQuestions = LlmQuizResponseParser.parse(cleanJson)
        if (parsedQuestions.isEmpty()) {
            logw("MCQ parse rỗng")
            return emptyList()
        }

        // Post-process với McqQualityScorer
        val qualityQuestions = McqQualityScorer.filterQuestions(parsedQuestions)
        logd("MCQ batch: ${parsedQuestions.size} parsed → ${qualityQuestions.size} after quality filter")

        return qualityQuestions
    }

    // ═══════════════════════════════════════════════════════════════
    // DIRECT APPROACH (fallback)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Chiến lược direct: segments → MCQ trực tiếp.
     * Dùng khi two-step fail.
     */
    private suspend fun generateWithDirectBatches(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        logd("=== DIRECT APPROACH (fallback) ===")

        val allQuestions = mutableListOf<QuizQuestion>()
        var remainingCount = targetCount
        var currentSegments = segments.toList().shuffled()
        var batchNum = 0

        val topicGroups = SegmentTopicGrouper.groupByTopic(segments)
        val batchQuestionCount = LlmPromptBuilder.optimalQuestionsPerBatch()

        while (remainingCount > 0 && batchNum < LlmGenerationConfig.MAX_BATCHES) {
            batchNum++

            val usedSnippets = allQuestions.map { it.sourceSnippet.take(30) }.toSet()
            val unusedSegments = currentSegments.filter { seg ->
                val snippet30 = seg.sourceSnippet.take(30)
                val text30 = seg.text.take(30)
                !usedSnippets.contains(snippet30) && !usedSnippets.contains(text30)
            }

            val batchSegments = when {
                unusedSegments.size >= 2 -> unusedSegments.take(LlmGenerationConfig.MAX_SEGMENTS_PER_BATCH)
                currentSegments.size >= 2 -> currentSegments.take(LlmGenerationConfig.MAX_SEGMENTS_PER_BATCH)
                else -> break
            }

            if (batchSegments.isEmpty()) break

            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.Generating("Direct batch $batchNum...")
            )

            val batchQuestions = generateDirectBatch(
                segments = batchSegments,
                targetCount = batchQuestionCount,
                language = language,
                topicGroups = topicGroups
            )

            if (batchQuestions.isEmpty()) {
                logw("Direct batch $batchNum fail")
                if (batchNum >= LlmGenerationConfig.MAX_BATCHES) break
                currentSegments = currentSegments.shuffled()
                continue
            }

            allQuestions.addAll(batchQuestions)
            remainingCount = targetCount - allQuestions.size
            logd("Direct batch $batchNum: ${batchQuestions.size} → tổng: ${allQuestions.size}/$targetCount")

            if (remainingCount <= 0 || batchNum >= LlmGenerationConfig.MAX_BATCHES) break
            currentSegments = currentSegments.shuffled()
        }

        val finalQuestions = deduplicateAndQualityFilter(allQuestions, segments, topicGroups)

        if (finalQuestions.isEmpty()) {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Direct generation không tạo được câu hỏi chất lượng")
            )
        } else if (finalQuestions.size.toDouble() / targetCount < 0.5) {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccessPartial(
                    llmCount = finalQuestions.size,
                    targetCount = targetCount,
                    message = "Direct: ${finalQuestions.size}/$targetCount câu chất lượng"
                )
            )
        } else {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccess(
                    questionCount = finalQuestions.size,
                    message = "Direct: ${finalQuestions.size} câu"
                )
            )
        }

        return finalQuestions
    }

    /**
     * Sinh một batch MCQ trực tiếp từ segments.
     */
    private suspend fun generateDirectBatch(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String,
        topicGroups: List<SegmentTopicGroup>
    ): List<QuizQuestion> {
        val prompt = LlmPromptBuilder.buildPrompt(
            segments = segments,
            targetCount = targetCount,
            language = language
        )
        if (prompt.isBlank()) return emptyList()

        val rawResponse: String
        try {
            rawResponse = llmManager.generateResponse(prompt)
        } catch (e: Exception) {
            logw("Direct inference lỗi: ${e.message}")
            return emptyList()
        }

        val cleanJson = LlmJsonExtractor.extractCleanJson(rawResponse)
        if (cleanJson == null) return emptyList()

        val parsedQuestions = LlmQuizResponseParser.parse(cleanJson)
        if (parsedQuestions.isEmpty()) return emptyList()

        val sourceSnippets = segments.map { it.text }
        val processedQuestions = LlmQuestionPostProcessor.process(
            questions = parsedQuestions,
            segments = segments,
            sourceSnippets = sourceSnippets
        )

        logd("Direct batch: ${parsedQuestions.size} parsed → ${processedQuestions.size} after post-process")
        return processedQuestions
    }

    // ═══════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════

    private fun deduplicateAndQualityFilter(
        questions: List<QuizQuestion>,
        allSegments: List<ContentSegment>,
        topicGroups: List<SegmentTopicGroup>
    ): List<QuizQuestion> {
        val seenTexts = mutableSetOf<String>()
        val seenSnippets = mutableSetOf<String>()
        val result = mutableListOf<QuizQuestion>()

        for (q in questions) {
            val normQuestion = SemanticSimilarityHelper.normalizeForComparison(q.question)
            val snippet30 = q.sourceSnippet.take(30)

            val isDuplicate = seenTexts.contains(normQuestion) ||
                    seenSnippets.contains(snippet30) ||
                    result.any { prev ->
                        SemanticSimilarityHelper.similarity(q.question, prev.question) > 0.60
                    }

            if (!isDuplicate) {
                seenTexts.add(normQuestion)
                seenSnippets.add(snippet30)
                result.add(q)
            } else {
                logd("Dedup: bỏ '${q.question.take(40)}'")
            }
        }

        return LlmQuestionPostProcessor.process(
            questions = result,
            segments = allSegments,
            sourceSnippets = allSegments.map { it.text }
        )
    }

    fun close() {
        llmManager.close()
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }

    private fun loge(message: String) {
        Log.e(TAG, message)
    }
}
