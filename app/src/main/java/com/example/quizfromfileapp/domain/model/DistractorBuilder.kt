package com.example.quizfromfileapp.domain.model

/**
 * Bộ sinh distractor (đáp án nhiễu) chất lượng cao.
 *
 * NGUYÊN TẮC: KHÔNG dùng template. Chỉ dùng nội dung THỰC từ tài liệu.
 *
 * Cách sinh:
 * 1. Lấy declarative sentence từ segment khác → biến thành distractor
 * 2. Lấy câu declarative từ segment khác rồi thay thế keyword đặc trưng
 * 3. Đảo subject/object của câu đúng
 * 4. Nếu không tạo được distractor chất lượng → câu hỏi bị reject
 *
 * KHÔNG BAO GIỜ sinh:
 * - "A related concept: ..."
 * - "... and related factors"
 * - "... nhưng với một số đặc điểm khác biệt"
 * - "Một khái niệm liên quan: ..."
 */
object DistractorBuilder {

    private const val MIN_DISTRACTOR_WORDS = 4
    private const val MAX_DISTRACTOR_CHARS = 110

    /** Từ khóa đặc trưng cần thay thế để tạo distractor gần đúng */
    private val substituteableKeywords = mapOf(
        // Mạng
        "LAN" to "WAN",
        "WAN" to "LAN",
        "client" to "server",
        "server" to "client",
        "peer-to-peer" to "client-server",
        "client-server" to "peer-to-peer",
        "wireless" to "wired",
        "bandwidth" to "latency",
        "router" to "switch",
        "switch" to "router",
        "transmission" to "reception",
        "data link" to "network layer",
        "physical" to "data link",
        "network" to "transport",
        "transport" to "application",
        // Hệ thống
        "hardware" to "software",
        "software" to "hardware",
        "input" to "output",
        "CPU" to "memory",
        "memory" to "storage",
        "primary" to "secondary",
        "local" to "remote",
        "centralized" to "distributed",
        "synchronous" to "asynchronous",
        "serial" to "parallel",
        "analog" to "digital",
        // Tổng quát
        "source" to "destination",
        "sender" to "receiver",
        "producer" to "consumer",
        "origin" to "target",
        "before" to "after",
        "internal" to "external",
        "private" to "public",
        "static" to "dynamic",
        "local" to "global",
        "sequential" to "concurrent"
    )

    /**
     * Sinh danh sách distractor chất lượng từ các segment.
     *
     * @param correctSentence câu đúng (để lấy context)
     * @param sourceSegments tất cả segment trong tài liệu
     * @param sourceSegment segment nguồn của câu hỏi hiện tại (loại trừ)
     * @param count số distractor cần sinh
     * @return danh sách distractor
     */
    fun buildDistractors(
        correctSentence: String,
        sourceSegments: List<String>,
        sourceSegment: String,
        count: Int = 3
    ): List<String> {
        val result = mutableSetOf<String>()

        // ── Nguồn 1: Declarative sentences từ segment khác ──
        val otherSegments = sourceSegments.filter { it != sourceSegment }.shuffled()

        for (segment in otherSegments) {
            if (result.size >= count) break

            val sentences = DeclarativeSentenceExtractor.extractDeclarativeSentences(segment)
            for (sentence in sentences) {
                if (result.size >= count) break
                if (isGoodDistractor(sentence, correctSentence, result.toList())) {
                    result.add(sentence)
                }
            }
        }

        // ── Nguồn 2: Substitute keyword trong câu đúng ──
        if (result.size < count) {
            val substituted = substituteKeywords(correctSentence)
            for (sub in substituted) {
                if (result.size >= count) break
                if (isGoodDistractor(sub, correctSentence, result.toList())) {
                    result.add(sub)
                }
            }
        }

        // ── Nguồn 3: Đảo subject/object ──
        if (result.size < count) {
            val swapped = swapSubjectObject(correctSentence)
            for (s in swapped) {
                if (result.size >= count) break
                if (isGoodDistractor(s, correctSentence, result.toList())) {
                    result.add(s)
                }
            }
        }

        // ── Nguồn 4: Negation / modification của câu đúng ──
        if (result.size < count) {
            val modified = negateOrModify(correctSentence)
            for (m in modified) {
                if (result.size >= count) break
                if (isGoodDistractor(m, correctSentence, result.toList())) {
                    result.add(m)
                }
            }
        }

        return result.toList()
    }

    /**
     * Kiểm tra distractor có đủ chất lượng không.
     */
    private fun isGoodDistractor(
        candidate: String,
        correctSentence: String,
        existing: List<String>
    ): Boolean {
        val trimmed = candidate.trim()
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }

        // Quá ngắn
        if (words.size < MIN_DISTRACTOR_WORDS) return false

        // Quá dài
        if (trimmed.length > MAX_DISTRACTOR_CHARS) return false

        // Trùng hoàn toàn với câu đúng
        if (trimmed.equals(correctSentence, ignoreCase = true)) return false

        // Trùng với existing distractor
        if (existing.any { trimmed.equals(it, ignoreCase = true) }) return false

        // Quá giống câu đúng (> 70%)
        if (similarity(trimmed, correctSentence) > 0.70) return false

        // Quá giống existing distractor
        if (existing.any { similarity(trimmed, it) > 0.60 }) return false

        // Phải là declarative sentence thật sự
        if (!DeclarativeSentenceExtractor.isDeclarativeSentence(trimmed)) return false

        // Không phải heading rác
        if (HeadingCaptionFilter.isHeadingOrCaption(trimmed)) return false

        // Không phải option quality evaluator blacklist
        if (OptionQualityEvaluator.rejectReason(trimmed) != null) return false

        return true
    }

    // ─────────────────────────────────────────────────────────────
    // Substitute keyword
    // ─────────────────────────────────────────────────────────────

    private fun substituteKeywords(sentence: String): List<String> {
        val result = mutableListOf<String>()
        val lower = sentence.lowercase()

        for ((from, to) in substituteableKeywords) {
            if (lower.contains(from.lowercase())) {
                val substituted = sentence.replace(
                    Regex("""(?i)\b${Regex.escape(from)}\b"""),
                    to
                )
                if (substituted != sentence) {
                    result.add(substituted)
                }
            }
        }

        return result.shuffled()
    }

    // ─────────────────────────────────────────────────────────────
    // Swap subject/object
    // ─────────────────────────────────────────────────────────────

    private fun swapSubjectObject(sentence: String): List<String> {
        // Pattern: "A provides/enables/supports B"
        val providesPattern = Regex("""(?i)\b(\w+)\s+(provides?|enables?|supports?|delivers?|transmits?|communicates?)\s+(.*)""")
        val match = providesPattern.find(sentence)
        if (match != null) {
            val (subject, verb, object_) = match.destructured
            val swapped = "$object_ ${verb.lowercase()} by $subject"
            if (swapped.length in 20..110) {
                return listOf(swapped)
            }
        }

        // Pattern: "A is used by B"
        val usedByPattern = Regex("""(?i)\b(\w+)\s+is\s+used\s+by\s+(\w+.*)""")
        val match2 = usedByPattern.find(sentence)
        if (match2 != null) {
            val (thing, agent, rest) = match2.destructured
            val swapped = "$agent uses $thing"
            if (swapped.length in 20..110) {
                return listOf(swapped)
            }
        }

        return emptyList()
    }

    // ─────────────────────────────────────────────────────────────
    // Negate or modify
    // ─────────────────────────────────────────────────────────────

    private fun negateOrModify(sentence: String): List<String> {
        val result = mutableListOf<String>()
        val lower = sentence.lowercase()

        // "A provides B" → "A does not provide B"
        if (lower.contains(Regex("""(?i)\b(provides?|enables?|supports?)\b"""))) {
            val negated = sentence.replaceFirst(
                Regex("""(?i)\b(provides?|enables?|supports?)\b"""),
                "does not $1"
            )
            if (negated.length in 20..110) result.add(negated)
        }

        // "A is used for" → "A is not used for"
        if (lower.contains(Regex("""(?i)\b(is|are)\s+\w+\s+used\b"""))) {
            val negated = sentence.replaceFirst(
                Regex("""(?i)\b(is|are)(\s+\w+\s+)used\b"""),
                "$1$2NOT used"
            )
            if (negated.length in 20..110) result.add(negated)
        }

        // "A uses B" → "B uses A"
        if (lower.contains(Regex("""(?i)\b(\w+)\s+uses?\s+(\w+.*)"""))) {
            val match = Regex("""(?i)\b(\w+)\s+uses?\s+(\w+.*)""").find(sentence)
            if (match != null) {
                val (a, b) = match.destructured
                val reversed = "$b uses $a"
                if (reversed.length in 20..110) result.add(reversed)
            }
        }

        return result.shuffled()
    }

    // ─────────────────────────────────────────────────────────────
    // Similarity
    // ─────────────────────────────────────────────────────────────

    private fun similarity(a: String, b: String): Double {
        val wordsA = a.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
        val wordsB = b.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return intersection.toDouble() / union
    }
}
