package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.DeclarativeSentenceExtractor
import com.example.quizfromfileapp.domain.model.HeadingCaptionFilter
import com.example.quizfromfileapp.domain.model.InstructionSentenceFilter

/**
 * Trích atomic facts từ source segments.
 *
 * Chiến lược 2 bước mới:
 * Bước 1: Trích fact từ segment
 *   - Chỉ lấy declarative sentences (knowledge, not instructions)
 *   - Reject: examples, exercises, instructions, formulas, headings, captions
 *   - FactStatement phải ngắn, rõ, là 1 mệnh đề kiến thức
 *
 * Bước 2: Extract concept từ fact
 *   - Lấy 1-3 từ chủ đạo làm concept
 *   - Dùng cho distractor generation
 *
 * Input:  List<ContentSegment> (đã quality filter)
 * Output: List<FactItem> (đã extract + score)
 */
object FactExtractor {

    private const val TAG = "FactExtractor"

    // Ngưỡng reject
    private const val MIN_FACT_LENGTH = 30
    private const val MAX_FACT_LENGTH = 200
    private const val MIN_WORDS_FOR_FACT = 5

    // Patterns cấm — reject segment nếu chủ yếu thuộc các loại này
    private val FORMULA_PATTERNS = listOf(
        Regex("""^\s*[\d\+\-\*\/\=\(\)\[\]\{\}\\\^]+[\s\d\+\-\*\/\=\(\)\[\]\{\}]*$"""),  // Pure formula line
        Regex("""\d+\s*[=]\s*\d+"""),  // equation like "x = 5"
        Regex("""\b(H[2]O|CO[2]|NaCl|O[2]|N[2]|H[2]|Fe|C[6]H[12]O[6])\b"""),  // chemical formulas
        Regex("""\$\$.*\$\$"""),  // LaTeX display math
        Regex("""\$[^$]+\$"""),  // LaTeX inline math
        Regex("""\b\d+\.\d+\.\d+\.\d+\b"""),  // version numbers
        Regex("""^\s*\d+\.\s*\d+\.\s*$""", RegexOption.IGNORE_CASE),  // numbered steps like "1. 2."
        Regex("""\b(function|def|class|import|return|if|else|for|while)\s*[\(\{]""", RegexOption.IGNORE_CASE)  // code snippets
    )

    private val EXAMPLE_MARKERS_EN = setOf(
        "for example", "for instance", "such as", "e.g.", "eg.", "like",
        "consider the case", "a typical", "this includes", "including"
    )

    private val EXAMPLE_MARKERS_VI = setOf(
        "ví dụ", "chẳng hạn", "chẳng hạn như", "như là", "bao gồm",
        "cụ thể là", "điển hình như", "chẳng hạn như là"
    )

    /**
     * Trích facts từ danh sách segments.
     *
     * @param segments Danh sách segments đã quality-filtered
     * @param language "vi" hoặc "en"
     * @return Danh sách FactItem đã extract và score
     */
    fun extractFacts(
        segments: List<ContentSegment>,
        language: String = "vi"
    ): List<FactItem> {
        if (segments.isEmpty()) {
            logd("extractFacts: segments rỗng → return empty")
            return emptyList()
        }

        logd("extractFacts: bắt đầu với ${segments.size} segments")

        val facts = mutableListOf<FactItem>()

        for (segment in segments) {
            val extracted = extractFromSegment(segment, language)
            facts.addAll(extracted)
        }

        // Deduplicate by factStatement
        val unique = facts.distinctBy {
            SemanticSimilarityHelper.normalizeForComparison(it.factStatement)
        }

        logd("extractFacts: ${segments.size} segments → ${facts.size} facts → ${unique.size} unique")
        return unique
    }

    /**
     * Trích facts từ 1 segment.
     */
    private fun extractFromSegment(
        segment: ContentSegment,
        language: String
    ): List<FactItem> {
        val text = segment.text

        // Skip nếu là heading/caption
        if (HeadingCaptionFilter.isHeadingOrCaption(text)) {
            logd("Skip heading/caption: '${text.take(40)}'")
            return emptyList()
        }

        // Skip nếu là instruction
        if (InstructionSentenceFilter.isInstructional(text)) {
            logd("Skip instruction: '${text.take(40)}'")
            return emptyList()
        }

        // Skip nếu chủ yếu là formula
        if (isMostlyFormula(text)) {
            logd("Skip formula: '${text.take(40)}'")
            return emptyList()
        }

        // Trích declarative sentences
        val declaratives = DeclarativeSentenceExtractor.extractDeclarativeSentences(text)
        if (declaratives.isEmpty()) {
            return emptyList()
        }

        val facts = mutableListOf<FactItem>()

        for (sentence in declaratives) {
            val fact = buildFactItem(sentence, segment, language)
            if (fact != null) {
                facts.add(fact)
            }
        }

        return facts
    }

    /**
     * Tạo FactItem từ một declarative sentence.
     */
    private fun buildFactItem(
        sentence: String,
        segment: ContentSegment,
        language: String
    ): FactItem? {
        val trimmed = sentence.trim()

        // Reject: quá ngắn
        if (trimmed.length < MIN_FACT_LENGTH) {
            logd("Reject fact quá ngắn: '${trimmed.take(40)}'")
            return null
        }

        // Reject: quá dài
        if (trimmed.length > MAX_FACT_LENGTH) {
            logd("Reject fact quá dài: '${trimmed.take(60)}...'")
            return null
        }

        // Reject: ít từ
        val wordCount = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        if (wordCount < MIN_WORDS_FOR_FACT) {
            logd("Reject fact ít từ: $wordCount words")
            return null
        }

        // Reject: chủ yếu là example
        if (isExampleSentence(trimmed, language)) {
            logd("Reject example sentence: '${trimmed.take(40)}'")
            return null
        }

        // Reject: chủ yếu là exercise/question
        if (isExerciseSentence(trimmed)) {
            logd("Reject exercise: '${trimmed.take(40)}'")
            return null
        }

        // Reject: nhiều số thứ tự / list items
        if (isListOrNumbered(trimmed)) {
            logd("Reject list/numbered: '${trimmed.take(40)}'")
            return null
        }

        // Extract concept
        val concept = extractConcept(trimmed, language)

        // Calculate confidence
        val confidence = FactQualityScorer.scoreFact(
            factStatement = trimmed,
            concept = concept,
            sourceSnippet = segment.text,
            language = language
        )

        // Reject: confidence quá thấp
        if (confidence < FactQualityScorer.MIN_FACT_CONFIDENCE) {
            logd("Reject fact confidence thấp: ${String.format("%.2f", confidence)} — '${trimmed.take(40)}'")
            return null
        }

        return FactItem(
            concept = concept,
            factStatement = trimmed,
            sourceSnippet = segment.sourceSnippet,
            sourcePageStart = segment.sourcePageStart,
            sourcePageEnd = segment.sourcePageEnd,
            sourceType = segment.sourceType,
            confidence = confidence
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Detection helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra segment có phải chủ yếu là formula không.
     */
    private fun isMostlyFormula(text: String): Boolean {
        // Loại bỏ text thuần, chỉ giữ các ký tự toán/chemistry
        val stripped = text.replace(Regex("""[\p{L}\p{M}]+"""), "")
        val symbolRatio = stripped.replace(Regex("""[\s,.\-:;]"""), "").length.toDouble() / text.length.coerceAtLeast(1)
        if (symbolRatio > 0.4) return true

        // Check formula patterns
        for (pattern in FORMULA_PATTERNS) {
            if (pattern.containsMatchIn(text)) return true
        }

        return false
    }

    /**
     * Kiểm tra sentence có phải là example không.
     */
    private fun isExampleSentence(sentence: String, language: String): Boolean {
        val lower = sentence.lowercase()
        val markers = if (language == "vi") EXAMPLE_MARKERS_VI else EXAMPLE_MARKERS_EN

        // Nếu sentence bắt đầu bằng example marker
        if (markers.any { lower.startsWith(it) }) return true

        // Nếu sentence chủ yếu là list examples (nhiều "such as" / "ví dụ")
        val markerCount = markers.count { lower.contains(it) }
        if (markerCount >= 2) return true

        return false
    }

    /**
     * Kiểm tra sentence có phải là exercise/question không.
     */
    private fun isExerciseSentence(sentence: String): Boolean {
        val lower = sentence.lowercase()

        val exercisePatterns = listOf(
            Regex("""^(exercise|problem|question|task|solve|find|calculate|prove|show that).*[\?]$""", RegexOption.IGNORE_CASE),
            Regex("""^(bài\s*tập|bài\s*giải|giải\s*bài|chứng\s*minh|tính\s*toán).*[\?]$""", RegexOption.IGNORE_CASE),
            Regex("""^\s*\d+\.\s+"""),  // "1. Solve the equation..."
            Regex("""^\s*\([a-z]\)\s+""", RegexOption.IGNORE_CASE),  // "(a) This..."
            Regex("""^\s*\([ivx]+\)\s+""", RegexOption.IGNORE_CASE)  // "(i) First..."
        )

        for (pattern in exercisePatterns) {
            if (pattern.containsMatchIn(lower)) return true
        }

        return false
    }

    /**
     * Kiểm tra có phải list/numbered items không.
     */
    private fun isListOrNumbered(text: String): Boolean {
        // Nhiều dấu chấm/dấu phẩy liên tiếp → list
        if (Regex("""\b(\d+\.\s*){3,}""").containsMatchIn(text)) return true

        // Nhiều bullet points
        val bulletCount = text.count { it == '•' || it == '-' || it == '*' }
        if (bulletCount >= 3) return true

        return false
    }

    // ─────────────────────────────────────────────────────────────
    // Concept extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Trích concept từ fact statement.
     * Concept = 1-3 từ chủ đạo, thường ở đầu câu hoặc danh từ chính.
     */
    private fun extractConcept(factStatement: String, language: String): String {
        val trimmed = factStatement.trim()

        // Strategy 1: Lấy first meaningful noun phrase
        val words = trimmed.split(Regex("""[\s]+"""))
        if (words.isEmpty()) return trimmed.take(30)

        // Skip common opening words
        val skipWords = setOf(
            "the", "a", "an", "this", "these", "it", "that", "is", "are",
            "was", "were", "be", "been", "being",
            "và", "là", "của", "được", "có", "một", "các", "những", "này", "đó"
        )

        val meaningfulWords = words.dropWhile {
            skipWords.contains(it.lowercase())
        }.take(10)

        // Lấy noun phrase đầu tiên (1-3 từ liên tiếp không phải stop words)
        val conceptWords = mutableListOf<String>()
        var foundStopWord = false

        for (word in meaningfulWords) {
            val lower = word.lowercase().replace(Regex("""[^\p{L}]"""), "")

            if (skipWords.contains(lower)) {
                if (conceptWords.isNotEmpty() && !foundStopWord) {
                    foundStopWord = true
                    continue
                }
                break
            }

            conceptWords.add(word)
            if (conceptWords.size >= 3) break
        }

        // Nếu không lấy được → fallback lấy first 2-3 meaningful words
        if (conceptWords.isEmpty()) {
            val fallback = words.filter {
                val lower = it.lowercase().replace(Regex("""[^\p{L}]"""), "")
                lower.length >= 3 && !skipWords.contains(lower)
            }.take(3)
            return fallback.joinToString(" ").take(40)
        }

        return conceptWords.joinToString(" ").take(40)
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }
}
