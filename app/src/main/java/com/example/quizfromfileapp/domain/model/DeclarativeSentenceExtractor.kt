package com.example.quizfromfileapp.domain.model

/**
 * Trích xuất declarative sentences từ segment.
 *
 * Declarative sentence = câu mô tả kiến thức thật sự, KHÔNG phải:
 * - Fragment (mảnh câu không hoàn chỉnh)
 * - Heading/caption
 * - Câu hỏi / câu mệnh lệnh
 * - Bullet list item
 *
 * Chỉ giữ lại câu:
 * - Có động từ thường (is, are, provide, describe, explain...)
 * - Có chủ ngữ + vị ngữ rõ ràng
 * - Độ dài hợp lý (30–200 ký tự)
 * - Không phải list item ngắn
 */
object DeclarativeSentenceExtractor {

    /** Động từ thường xuất hiện trong câu declarative */
    private val declarativeVerbs = setOf(
        "is", "are", "was", "were", "been", "being",
        "have", "has", "had", "do", "does", "did",
        "will", "would", "can", "could", "should", "may", "might", "must",
        "provide", "provides", "provided", "providing",
        "describe", "describes", "described", "describing",
        "explain", "explains", "explained", "explaining",
        "show", "shows", "shown", "showed", "showing",
        "include", "includes", "included", "including",
        "contain", "contains", "contained", "containing",
        "affect", "affects", "affected", "affecting",
        "result", "results", "resulted", "resulting",
        "occur", "occurs", "occurred", "occurring",
        "develop", "develops", "developed", "developing",
        "create", "creates", "created", "creating",
        "support", "supports", "supported", "supporting",
        "determine", "determines", "determined", "determining",
        "increase", "increases", "increased", "increasing",
        "decrease", "decreases", "decreased", "decreasing",
        "change", "changes", "changed", "changing",
        "apply", "applies", "applied", "applying",
        "require", "requires", "required", "requiring",
        "enable", "enables", "enabled", "enabling",
        "use", "uses", "used", "using",
        "make", "makes", "made", "making",
        "give", "gives", "gave", "giving",
        "help", "helps", "helped", "helping",
        "allow", "allows", "allowed", "allowing",
        "represent", "represents", "represented",
        "consist", "consists", "consisted",
        "ensure", "ensures", "ensured",
        "maintain", "maintains", "maintained",
        "serve", "serves", "served",
        "form", "forms", "formed", "forming",
        "define", "defines", "defined",
        "identify", "identifies", "identified",
        "relate", "relates", "related",
        "associate", "associates", "associated",
        "function", "functions", "functioned",
        "operate", "operates", "operated",
        "transmit", "transmits", "transmitted",
        "communicate", "communicates", "communicated",
        "deliver", "delivers", "delivered",
        "establish", "establishes", "established",
    )

    /** Stopwords tiếng Anh — bỏ qua khi đếm meaningful words */
    private val englishStopwords = setOf(
        "the", "a", "an", "of", "in", "to", "for", "with", "on", "at", "by",
        "from", "as", "is", "was", "are", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "that", "this", "it", "its", "which", "what", "who", "how", "but", "or",
        "not", "and", "also", "than", "then", "when", "where", "why", "so"
    )

    /**
     * Trích tất cả declarative sentences từ một segment.
     *
     * @param segment đoạn văn bản đầu vào
     * @return danh sách câu declarative có chất lượng
     */
    fun extractDeclarativeSentences(segment: String): List<String> {
        val raw = segment
            .replace("\n", " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        val candidates = raw
            .split(Regex("""(?<=[.!?])\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return candidates
            .filter { isDeclarativeSentence(it) }
            .map { cleanSentence(it) }
            .filter { it.length in 30..200 }
            .filterNot { isLowQualitySentence(it) }
            .distinct()
    }

    /**
     * Kiểm tra một câu có phải là declarative sentence không.
     */
    fun isDeclarativeSentence(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 15) return false
        if (trimmed.length > 300) return false

        // Loại câu hỏi
        if (trimmed.endsWith("?")) return false

        // Loại câu mệnh lệnh (bắt đầu bằng động từ nguyên mẫu không có chủ ngữ)
        if (isImperative(trimmed)) return false

        // Loại list item ngắn
        if (isListItem(trimmed)) return false

        // Phải có động từ declarative
        if (!hasDeclarativeVerb(trimmed)) return false

        // Phải có chủ ngữ (từ có nghĩa trước động từ)
        if (!hasSubject(trimmed)) return false

        return true
    }

    /**
     * Trả về lý do reject hoặc null.
     */
    fun rejectReason(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.length < 15 -> "Câu quá ngắn (< 15 ký tự)"
            trimmed.length > 300 -> "Câu quá dài (> 300 ký tự)"
            trimmed.endsWith("?") -> "Là câu hỏi"
            isImperative(trimmed) -> "Là câu mệnh lệnh"
            isListItem(trimmed) -> "Là list item ngắn"
            !hasDeclarativeVerb(trimmed) -> "Không có động từ declarative"
            !hasSubject(trimmed) -> "Không có chủ ngữ rõ ràng"
            else -> null
        }
    }

    private fun isImperative(text: String): Boolean {
        val first = text.trim().split(Regex("""\s+""")).firstOrNull() ?: return false
        val imperativeVerbs = setOf(
            "note", "see", "find", "use", "apply", "consider", "remember",
            "keep", "make", "let", "take", "give", "avoid", "ensure", "verify"
        )
        // Imperative = động từ nguyên mẫu không có chủ ngữ đằng trước
        if (first.lowercase() in imperativeVerbs) {
            // Nếu từ thứ 2 là danh từ → có thể là imperative
            val words = text.trim().split(Regex("""\s+"""))
            if (words.size >= 2 && words[1].first().isLowerCase()) {
                return true
            }
        }
        return false
    }

    private fun isListItem(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 80) return false
        // Bắt đầu bằng bullet marker
        if (trimmed.matches(Regex("""^[•·\-*–—]\s+.*"""))) return true
        // Bắt đầu bằng số + dấu "1." "2." "a)"
        if (trimmed.matches(Regex("""^\\d+[\.\)]\s+\\S.*"""))) return true
        if (trimmed.matches(Regex("""^[a-z][\.\)]\s+\\S.*""", RegexOption.IGNORE_CASE))) return true
        return false
    }

    private fun hasDeclarativeVerb(text: String): Boolean {
        val lower = text.lowercase()
        return declarativeVerbs.any { verb ->
            lower.contains(" $verb ") || lower.contains(" $verb.") ||
                    lower.contains(" $verb,") || lower.startsWith("$verb ") ||
                    lower.startsWith("the $verb ")
        }
    }

    private fun hasSubject(text: String): Boolean {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size < 3) return false
        val meaningfulCount = words.count {
            it.length >= 3 && it.lowercase() !in englishStopwords
        }
        return meaningfulCount >= 2
    }

    private fun isLowQualitySentence(text: String): Boolean {
        val lower = text.lowercase()
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }

        // Toàn stopwords
        if (words.all { it.lowercase() in englishStopwords || it.length < 3 }) return true

        // Heading trong nội dung (khớp pattern)
        if (lower.contains(Regex("""(?i)(figure|table|example)\\s+\\d+""")))
            return true

        // Quá nhiều ký hiệu
        val symbolRatio = text.count { it in ".,;:!?()-—" }.toDouble() / text.length
        if (symbolRatio > 0.4) return true

        // Bắt đầu bằng "The" nhưng ngắn (< 40 ký tự) → có thể là heading
        if (text.trim().startsWith("The ") && text.length < 40) return true

        return false
    }

    private fun cleanSentence(text: String): String {
        return text
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""^\s*\(\s*"""), "(")
            .replace(Regex("""\s*\)\s*$"""), ")")
            .trim()
    }

    /**
     * Trích đoạn text nhỏ từ segment phục vụ trả lời.
     * Ưu tiên câu declarative dài vừa.
     */
    fun extractBestAnswerSnippet(segment: String): String {
        val sentences = extractDeclarativeSentences(segment)
        // Ưu tiên câu 40-120 ký tự
        val best = sentences.firstOrNull { it.length in 40..120 }
            ?: sentences.firstOrNull { it.length in 30..150 }
            ?: sentences.firstOrNull()
            ?: segment.take(100)
        return best
    }
}
