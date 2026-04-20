package com.example.quizfromfileapp.quizgenerator

import com.example.quizfromfileapp.domain.model.ContentQualityFilter
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.DeclarativeSentenceExtractor
import com.example.quizfromfileapp.domain.model.DistractorBuilder
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.GenericFallbackBlacklist
import com.example.quizfromfileapp.domain.model.HeadingCaptionFilter
import com.example.quizfromfileapp.domain.model.InstructionSentenceFilter
import com.example.quizfromfileapp.domain.model.LanguageDetector
import com.example.quizfromfileapp.domain.model.OptionQualityEvaluator
import com.example.quizfromfileapp.domain.model.QuestionFingerprint
import com.example.quizfromfileapp.domain.model.QuestionQualityEvaluator
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.domain.model.QuizQuestion
import com.example.quizfromfileapp.domain.model.QuizSession
import com.example.quizfromfileapp.domain.model.QuizUniquenessValidator

/**
 * Bộ sinh quiz chất lượng cao — ACCURACY-FIRST.
 *
 * Mỗi câu hỏi đều truy vết được về:
 * - Trang nguồn (sourcePage)
 * - Đoạn trích gốc (sourceSnippet)
 * - Loại nguồn (sourceType: TEXT_LAYER / OCR / MERGED)
 */
class SmartQuizGenerator : QuizGenerator {

    /** Session-level validator */
    private val sessionValidator = QuizUniquenessValidator()

    override suspend fun generate(content: ExtractedContent, config: QuizConfig): QuizSession {
        sessionValidator.reset()

        val isEnglish = LanguageDetector.isEnglish(
            // Ưu tiên fullMergedTextBlockCleaned (PDF) hoặc cleanedText (non-PDF)
            content.pdfExtractionResult?.fullMergedTextBlockCleaned
                ?: content.cleanedText
        )

        // Lấy segments GIỮ NGUYÊN ContentSegment objects (có provenance)
        val rawSegments = content.cleanedContent?.segments ?: emptyList()

        // Lọc nghiêm ngặt: giữ nguyên segment objects (không chỉ text)
        val qualitySegments = rawSegments
            .filter { ContentQualityFilter.isQualitySegment(it.text) }
            .filterNot { HeadingCaptionFilter.isHeadingOrCaption(it.text) }
            .filterNot { InstructionSentenceFilter.isInstructional(it.text) }
            .filter { DeclarativeSentenceExtractor.extractDeclarativeSentences(it.text).isNotEmpty() }

        // Xác định "toàn bộ corpus" — block đầy đủ cho generator và fallback
        val fullCorpus = when {
            // PDF: ưu tiên dùng fullMergedTextBlockCleaned
            content.pdfExtractionResult != null ->
                content.pdfExtractionResult.fullMergedTextBlockCleaned.ifBlank {
                    content.cleanedText
                }
            // Non-PDF: dùng cleanedText
            else -> content.cleanedText
        }

        val result = generateMultipleChoice(
            qualitySegments = qualitySegments,
            rawText = fullCorpus,
            count = config.questionCount,
            isEnglish = isEnglish
        )

        val actualCount = result.questions.size
        val requestedCount = config.questionCount
        val warning = if (actualCount < requestedCount) {
            if (isEnglish) {
                "Only $actualCount high-quality questions could be generated from this content."
            } else {
                "Nội dung phù hợp chỉ đủ tạo $actualCount câu hỏi chất lượng."
            }
        } else null

        return QuizSession(
            fileName = content.fileName,
            questionCount = actualCount,
            difficulty = config.difficulty,
            questionType = "Trắc nghiệm 4 đáp án",
            questions = result.questions,
            generationWarning = warning
        )
    }

    private data class GenerationResult(
        val questions: List<QuizQuestion>,
        val actualCount: Int
    )

    // ─────────────────────────────────────────────────────────────
    // Sinh trắc nghiệm — dùng ContentSegment objects
    // ─────────────────────────────────────────────────────────────

    private fun generateMultipleChoice(
        qualitySegments: List<ContentSegment>,
        rawText: String,
        count: Int,
        isEnglish: Boolean
    ): GenerationResult {
        // Chuyển sang danh sách text để generator dùng
        val sourceTexts = qualitySegments.map { it.text }

        val sourceSegments = if (qualitySegments.isNotEmpty()) {
            qualitySegments
        } else {
            extractFallbackSegments(rawText)
        }

        if (sourceSegments.isEmpty()) {
            return GenerationResult(questions = emptyList(), actualCount = 0)
        }

        val selected = selectDiverseSegments(sourceSegments, count * 3)
        val questions = mutableListOf<QuizQuestion>()

        // Vòng 1
        for ((i, segment) in selected.withIndex()) {
            if (questions.size >= count) break

            val declaratives = DeclarativeSentenceExtractor.extractDeclarativeSentences(segment.text)
            if (declaratives.isEmpty()) continue

            if (sessionValidator.isSegmentUsed(segment.text)) continue

            val q = createUniqueQuestion(
                segment = segment,
                declaratives = declaratives,
                index = i,
                allSegments = sourceTexts,
                isEnglish = isEnglish
            )

            if (q != null) {
                val reason = sessionValidator.validate(
                    segment = segment.text,
                    question = q.question,
                    options = q.options
                )
                if (reason == null) {
                    sessionValidator.markUsed(segment.text, q.question, q.options)
                    questions.add(q)
                }
            }
        }

        // Vòng 2
        val remaining = sourceSegments
            .filter { !sessionValidator.isSegmentUsed(it.text) }
            .filter { DeclarativeSentenceExtractor.extractDeclarativeSentences(it.text).isNotEmpty() }
            .shuffled()

        for (segment in remaining) {
            if (questions.size >= count) break

            val declaratives = DeclarativeSentenceExtractor.extractDeclarativeSentences(segment.text)
            if (declaratives.isEmpty()) continue
            if (sessionValidator.isSegmentUsed(segment.text)) continue

            val q = createUniqueQuestion(
                segment = segment,
                declaratives = declaratives,
                index = questions.size,
                allSegments = sourceTexts,
                isEnglish = isEnglish
            )

            if (q != null) {
                val reason = sessionValidator.validate(segment.text, q.question, q.options)
                if (reason == null) {
                    sessionValidator.markUsed(segment.text, q.question, q.options)
                    questions.add(q)
                }
            }
        }

        return GenerationResult(questions, questions.size)
    }

    // ─────────────────────────────────────────────────────────────
    // Tạo câu hỏi — GIỮ PROVENANCE
    // ─────────────────────────────────────────────────────────────

    private fun createUniqueQuestion(
        segment: ContentSegment,
        declaratives: List<String>,
        index: Int,
        allSegments: List<String>,
        isEnglish: Boolean
    ): QuizQuestion? {
        val correctSentence = pickCorrectSentence(declaratives)
        if (correctSentence.isBlank()) return null

        if (GenericFallbackBlacklist.isForbiddenOption(correctSentence)) return null

        val snippet = truncate(correctSentence, 130)
        val template = englishTemplates.random()
        val questionText = template.replace("{q}", snippet)

        if (GenericFallbackBlacklist.isForbiddenQuestion(questionText)) return null

        val distractors = DistractorBuilder.buildDistractors(
            correctSentence = correctSentence,
            sourceSegments = allSegments,
            sourceSegment = segment.text,
            count = 3
        )

        if (distractors.any { GenericFallbackBlacklist.isForbiddenOption(it) }) return null

        val allOptions = (distractors + correctSentence).distinct()
        val ranked = OptionQualityEvaluator.filterAndRank(allOptions, questionText)
        if (ranked.size < 4) return null

        val chosen = ranked.take(4).map { it.first }
        if (GenericFallbackBlacklist.hasForbiddenOptions(chosen)) return null

        val shuffled = chosen.shuffled()
        val correctIdx = shuffled.indexOf(correctSentence).coerceAtLeast(0)
        val explanation = buildExplanation(correctSentence)

        // TRUYỀN PROVENANCE vào QuizQuestion
        return QuizQuestion(
            id = "sq_${index}_${System.currentTimeMillis()}",
            question = questionText,
            options = shuffled,
            correctAnswerIndex = correctIdx,
            explanation = explanation,
            sourcePageStart = segment.sourcePageStart,
            sourcePageEnd = segment.sourcePageEnd,
            sourceSnippet = segment.sourceSnippet.take(200),
            sourceType = segment.sourceType
        )
    }

    private fun pickCorrectSentence(sentences: List<String>): String {
        val candidates = sentences
            .filter { it.length in 35..120 }
            .filterNot { GenericFallbackBlacklist.isForbiddenOption(it) }
            .filterNot { QuestionFingerprint.questionCore(it).length < 15 }
            .sortedByDescending { it.length }

        return candidates.firstOrNull()
            ?: sentences.filter { it.length in 25..150 }
                .filterNot { GenericFallbackBlacklist.isForbiddenOption(it) }
                .firstOrNull()
            ?: sentences.firstOrNull()
            ?: ""
    }

    private fun buildExplanation(correctSentence: String): String {
        val snippet = if (correctSentence.length > 90) {
            correctSentence.take(90).replace(Regex("""\s+\S*$"""), "") + "..."
        } else {
            correctSentence
        }
        return "This answer is directly supported by the passage.\n\"$snippet\""
    }

    private val englishTemplates = listOf(
        "According to the passage, which statement is correct?\n\"{q}\"",
        "What does the passage explain about this topic?\n\"{q}\"",
        "Based on the passage, which description is accurate?\n\"{q}\"",
        "Which statement is supported by the passage?\n\"{q}\"",
        "What is the main point made in this passage?\n\"{q}\"",
        "According to the content, which of the following is true?\n\"{q}\"",
        "Which explanation matches the information in the text?\n\"{q}\"",
        "Based on the passage, what can be said about this concept?\n\"{q}\"",
        "What does the passage describe about this subject?\n\"{q}\"",
        "Which statement best reflects the information given?\n\"{q}\"",
    )

    private fun selectDiverseSegments(segments: List<ContentSegment>, needed: Int): List<ContentSegment> {
        val unique = mutableListOf<ContentSegment>()
        for (seg in segments) {
            val isDup = unique.any { contentSimilarity(it.text, seg.text) > 0.65 }
            if (!isDup) unique.add(seg)
        }
        return unique.shuffled().take(needed)
    }

    private fun contentSimilarity(a: String, b: String): Double {
        val wordsA = a.lowercase().split(Regex("""\s+""")).filter { it.length > 3 }.toSet()
        val wordsB = b.lowercase().split(Regex("""\s+""")).filter { it.length > 3 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return intersection.toDouble() / union
    }

    private fun extractFallbackSegments(text: String): List<ContentSegment> {
        val result = mutableListOf<ContentSegment>()

        val paragraphs = text.split(Regex("""\n\s*\n"""))
        for (para in paragraphs) {
            val cleaned = para.replace("\n", " ").replace(Regex("""\s{2,}"""), " ").trim()

            if (!ContentQualityFilter.isQualitySegment(cleaned)) continue
            if (HeadingCaptionFilter.isHeadingOrCaption(cleaned)) continue
            if (InstructionSentenceFilter.isInstructional(cleaned)) continue

            val declaratives = DeclarativeSentenceExtractor.extractDeclarativeSentences(cleaned)
            if (declaratives.isEmpty()) {
                val sentences = cleaned.split(Regex("""(?<=[.!?])\s+"""))
                for (s in sentences) {
                    val trimmed = s.trim()
                    if (ContentQualityFilter.isQualitySegment(trimmed) &&
                        !HeadingCaptionFilter.isHeadingOrCaption(trimmed) &&
                        !InstructionSentenceFilter.isInstructional(trimmed) &&
                        DeclarativeSentenceExtractor.extractDeclarativeSentences(trimmed).isNotEmpty()
                    ) {
                        result.add(ContentSegment(
                            text = trimmed,
                            type = ContentSegment.TYPE_SENTENCE,
                            wordCount = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }.size,
                            hasKnowledge = true,
                            sourcePageStart = null,
                            sourcePageEnd = null,
                            sourceType = ContentSegment.SOURCE_TYPE_UNKNOWN,
                            sourceSnippet = trimmed.take(200)
                        ))
                    }
                }
            } else {
                result.add(ContentSegment(
                    text = cleaned,
                    type = ContentSegment.TYPE_PARAGRAPH,
                    wordCount = cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }.size,
                    hasKnowledge = true,
                    sourcePageStart = null,
                    sourcePageEnd = null,
                    sourceType = ContentSegment.SOURCE_TYPE_UNKNOWN,
                    sourceSnippet = cleaned.take(200)
                ))
            }
        }

        return result.distinctBy { it.text }
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length > maxLen) {
            text.take(maxLen - 3) + "..."
        } else {
            text
        }
    }
}
