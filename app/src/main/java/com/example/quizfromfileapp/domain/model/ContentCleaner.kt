package com.example.quizfromfileapp.domain.model

/**
 * Bộ làm sạch text — pipeline xử lý nội dung trước khi sinh quiz.
 *
 * Quy trình 6 bước:
 * 1. Chuẩn hóa xuống dòng
 * 2. Bỏ noise pattern (số trang, header, footer, ký hiệu)
 * 3. Bỏ dòng tiêu đề rác / mục lục
 * 4. Nối dòng xuống hàng giữa chừng
 * 5. Tách thành đoạn/câu có nghĩa
 * 6. Chỉ giữ các đoạn đủ chất lượng
 */
object ContentCleaner {

    private const val MIN_SEGMENT_CHARS = 30
    private const val MAX_SEGMENT_CHARS = 600
    private const val MIN_LINE_CHARS = 10
    private const val MIN_WORD_COUNT = 4

    private val noisePatterns = listOf(
        Regex("""^\\s*\\d+\\s*$"""),                           // dòng chỉ có số
        Regex("""^\\s*[-–—•·\\*]+\\s*$"""),                    // dòng chỉ có bullet
        Regex("""^\\s*Page\\s+\\d+\\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Trang\\s+\\d+\\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Copyright\\s+.*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*©\\s*\\d{4}.*$"""),
        Regex("""^\\s*ISBN.*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*[A-Z]{3,}\\s+\\d{4}.*$"""),             // viết tắt năm
    )

    private val headingPatterns = listOf(
        Regex("""^\\s*(Chapter|Section|Part|Unit|Chương|Phần|Mục)\\s+\\d+.*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*(Introduction|Conclusion|Summary|References|Bibliography)\\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*(References|Literature|Works\\s+Cited)\\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Table\\s+of\\s+Contents$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Mục\\s+lục$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Tài\\s+liệu\\s+tham\\s+khảo$""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Table\\s+\\d+.*""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Figure\\s+\\d+.*""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Hình\\s+\\d+.*""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*Bảng\\s+\\d+.*""", RegexOption.IGNORE_CASE),
    )

    private val tocPatterns = listOf(
        Regex("""^\\s*\\d+(\\.\\d+)+\\s+\\S"""),   // 1.1, 2.3.1, ...
        Regex("""^\\s*\\d+\\.\\s+\\S{3,}\\s*$"""), // "1. Title", "2. Title"
        // Số La Mã + dấu chấm (i. ii. iii. / I. II.) — literal dot, không có ")" thừa
        Regex("""^\\s*[ivxlcdm]+\\.""", RegexOption.IGNORE_CASE),
        Regex("""^\\s*[a-z]\\.\\s+\\S{3,}""", RegexOption.IGNORE_CASE), // a. b. c.
    )

    private val knowledgeKeywords = setOf(
        "là", "có", "được", "không", "phải", "hay", "theo", "trong",
        "vì", "nên", "khi", "nhưng", "tuy", "nhiên", "và", "hoặc",
        "để", "với", "cho", "đã", "sẽ", "đang", "đều", "cũng",
        "mỗi", "tất", "các", "bởi", "vào", "ra", "lên", "xuống",
        "nghĩa", "nghĩ", "định", "nghĩa", "khái", "niệm", "hệ", "thống",
        "quá", "trình", "phát", "triển", "thay", "đổi", "ảnh", "hưởng",
        "yếu", "tố", "tác", "động", "cách", "phương", "pháp", "nguyên",
        "lý", "đặc", "điểm", "tính", "chất", "cấu", "trúc", "chức",
        "năng", "vai", "trò", "quan", "hệ", "liên", "quan", "hợp",
        "ứng", "dụng", "lĩnh", "vực", "thực", "hiện", "xây", "dựng"
    )

    /**
     * Làm sạch toàn bộ text và trả về CleanedContent.
     */
    fun clean(rawText: String): CleanedContent {
        val originalLines = rawText.lines()
        val afterNormalization = normalizeLineEndings(rawText)
        val afterNoiseRemoval = removeNoisePatterns(afterNormalization)
        val afterHeadingRemoval = removeHeadingAndToc(afterNoiseRemoval)
        val rejoined = rejoinBrokenLines(afterHeadingRemoval)
        val cleanedText = joinParagraphs(afterHeadingRemoval).trim()

        var removedCount = 0
        for (line in originalLines) {
            if (isRemovedLine(line)) removedCount++
        }

        // Non-PDF: segments không có provenance
        val segments = extractSegments(cleanedText, pageInfo = null)

        return CleanedContent(
            originalText = rawText,
            cleanedText = cleanedText,
            segments = segments,
            removedLineCount = removedCount
        )
    }

    /**
     * Làm sạch text TỪ PDF (có provenance per-page).
     * MỖI segment đều giữ sourcePage, sourceSnippet.
     *
     * Quy trình:
     * 1. Với mỗi trang, tạo merged text
     * 2. Clean merged text của trang đó
     * 3. Extract segments từ merged text đã clean, gắn provenance
     * 4. Trả CleanedContent với segments có đầy đủ provenance
     * 5. cleanedText dùng fullMergedTextBlockCleaned (nếu được truyền vào)
     *    hoặc build từ per-page cleaned text
     *
     * NGUYÊN TẮC ACCURACY-FIRST:
     * - KHÔNG cắt ký tự sớm
     * - GIỮ LẠI tất cả text đã merge
     * - MỖI segment đều truy vết được về trang nguồn
     * - cleanedText ưu tiên dùng pre-computed block (fullMergedTextBlockCleaned)
     */
    fun cleanFromPdfPages(
        pdfResults: List<PdfPageResult>,
        fullMergedTextBlockCleaned: String? = null
    ): CleanedContent {
        val allSegments = mutableListOf<ContentSegment>()
        var removedCount = 0

        for (pageResult in pdfResults) {
            val pageIdx = pageResult.pageIndex
            val mergedText = pageResult.mergedText

            // Clean text của trang này
            val pageNormalized = normalizeLineEndings(mergedText)
            val pageNoNoise = removeNoisePatterns(pageNormalized)
            val pageNoHeadings = removeHeadingAndToc(pageNoNoise)
            val pageRejoined = rejoinBrokenLines(pageNoHeadings)
            val pageCleaned = joinParagraphs(pageNoHeadings).trim()

            // Đếm dòng bị loại
            for (line in mergedText.lines()) {
                if (isRemovedLine(line)) removedCount++
            }

            // Nếu trang clean xong có nội dung, extract segments
            if (pageCleaned.isNotBlank()) {
                // Extract segments với provenance
                val pageSegments = extractSegments(
                    text = pageCleaned,
                    pageInfo = PageInfo(
                        pageStart = pageIdx,
                        pageEnd = pageIdx,
                        sourceSnippet = mergedText.take(200)
                    )
                )
                allSegments.addAll(pageSegments)
            }
        }

        // cleanedText: ưu tiên dùng pre-computed block, fallback tự build
        val finalCleanedText = fullMergedTextBlockCleaned?.takeIf { it.isNotBlank() }
            ?: pdfResults.joinToString("\n\n") { page ->
                val normalized = normalizeLineEndings(page.mergedText)
                val noNoise = removeNoisePatterns(normalized)
                val noHeadings = removeHeadingAndToc(noNoise)
                joinParagraphs(noHeadings).trim()
            }.trim()

        return CleanedContent(
            originalText = pdfResults.joinToString("\n\n") { it.mergedText },
            cleanedText = finalCleanedText,
            segments = allSegments,
            removedLineCount = removedCount
        )
    }

    /**
     * Thông tin provenance cho segment.
     */
    private data class PageInfo(
        val pageStart: Int,
        val pageEnd: Int,
        val sourceSnippet: String
    )

    /**
     * Chỉ làm sạch text thuần (không tách segment).
     */
    fun cleanTextOnly(rawText: String): String {
        val afterNormalization = normalizeLineEndings(rawText)
        val afterNoiseRemoval = removeNoisePatterns(afterNormalization)
        val afterHeadingRemoval = removeHeadingAndToc(afterNoiseRemoval)
        return afterHeadingRemoval.trim()
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 1: Chuẩn hóa xuống dòng
    // ─────────────────────────────────────────────────────────────

    private fun normalizeLineEndings(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\t", " ")
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 2: Bỏ noise pattern (số trang, header, footer)
    // ─────────────────────────────────────────────────────────────

    private fun removeNoisePatterns(text: String): String {
        var result = text

        // Bỏ dòng chỉ có số (số trang)
        result = result.lines()
            .filter { line -> !noisePatterns.any { it.matches(line.trim()) } }
            .joinToString("\n")

        // Bỏ nhiều khoảng trắng liên tiếp
        result = result.replace(Regex("""[ \t]{2,}"""), " ")

        // Bỏ dòng chỉ có ký hiệu đặc biệt
        result = result.lines()
            .filter { line -> !isOnlySymbols(line.trim()) }
            .joinToString("\n")

        // Bỏ dòng quá ngắn vô nghĩa
        result = result.lines()
            .filter { line -> line.trim().length >= MIN_LINE_CHARS }
            .joinToString("\n")

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 3: Bỏ dòng tiêu đề rác / mục lục
    // ─────────────────────────────────────────────────────────────

    private fun removeHeadingAndToc(text: String): String {
        val lines = text.lines()
        val filtered = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Bỏ nếu là dòng tiêu đề rác (IN HOA dài, kết thúc :)
            if (isUselessHeading(trimmed)) continue

            // Bỏ nếu là dòng mục lục
            if (isTocLine(trimmed)) continue

            // Bỏ nếu là dòng numbering không có nội dung thực
            if (isPureNumberingLine(trimmed)) continue

            filtered.add(line)
        }

        return filtered.joinToString("\n")
    }

    private fun isUselessHeading(line: String): Boolean {
        // Tất cả in hoa, quá ngắn, kết thúc bằng dấu hai chấm → heading rác
        if (line.length < 40 && line.all { it.isUpperCase() || it.isWhitespace() || it == ':' }) {
            return true
        }
        // Khớp với heading pattern
        if (headingPatterns.any { it.matches(line) }) return true
        return false
    }

    private fun isTocLine(line: String): Boolean {
        if (tocPatterns.any { it.matches(line) }) {
            // Nếu dòng quá dài (có nội dung thực) thì không phải mục lục
            if (line.trim().length > 60) return false
            return true
        }
        return false
    }

    private fun isPureNumberingLine(line: String): Boolean {
        // "2. NUMBERING SYSTEMS" → heading rác
        if (line.matches(Regex("""^\d+\.\s+[A-Z\s]{3,}$"""))) return true
        // "2.1 Introduction" → heading
        if (line.matches(Regex("""^\d+(\.\d+)*\s+[A-Z][a-zA-Z\s]{0,30}$"""))) return true
        return false
    }

    private fun isOnlySymbols(line: String): Boolean {
        if (line.isBlank()) return true
        return line.all { it in ".,;:!?()-—–'\"•·*#@$%^&+=<>[]{}|/\\~`" || it.isWhitespace() }
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 4: Nối dòng xuống hàng giữa chừng
    // ─────────────────────────────────────────────────────────────

    private fun rejoinBrokenLines(text: String): String {
        val lines = text.lines()
        val result = mutableListOf<String>()
        val buffer = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                // Dòng trắng = ngắt đoạn
                if (buffer.isNotEmpty()) {
                    result.add(buffer.toString().replace(Regex("""\s+"""), " ").trim())
                    buffer.clear()
                }
                continue
            }

            // Nếu kết thúc bằng dấu câu → đây là câu hoàn chỉnh
            if (trimmed.last() in ".!?") {
                if (buffer.isNotEmpty()) {
                    buffer.append(" ")
                }
                buffer.append(trimmed)
                result.add(buffer.toString().replace(Regex("""\s+"""), " ").trim())
                buffer.clear()
            } else {
                // Dòng chưa kết thúc → nối với dòng tiếp theo
                if (buffer.isNotEmpty()) {
                    buffer.append(" ")
                }
                buffer.append(trimmed)
            }
        }

        if (buffer.isNotEmpty()) {
            result.add(buffer.toString().replace(Regex("""\s+"""), " ").trim())
        }

        return result.joinToString("\n\n")
    }

    private fun joinParagraphs(text: String): String {
        return text.lines()
            .joinToString(" ") { it.trim() }
            .replace(Regex("""\n{3,}"""), "\n\n")
    }

    // ─────────────────────────────────────────────────────────────
    // Bước 5 & 6: Tách thành đoạn/câu + chỉ giữ đoạn chất lượng
    // ─────────────────────────────────────────────────────────────

    private fun extractSegments(text: String, pageInfo: PageInfo?): List<ContentSegment> {
        val segments = mutableListOf<ContentSegment>()
        val paragraphs = text.split(Regex("""\n\n+"""))

        for (paragraph in paragraphs) {
            val cleaned = paragraph.replace(Regex("""\s+"""), " ").trim()
            if (cleaned.isBlank()) continue

            if (cleaned.length in MIN_SEGMENT_CHARS..MAX_SEGMENT_CHARS) {
                segments.add(createSegment(cleaned, ContentSegment.TYPE_PARAGRAPH, pageInfo))
                continue
            }

            if (cleaned.length > MAX_SEGMENT_CHARS) {
                val sentences = splitIntoSentences(cleaned)
                val group = StringBuilder()
                for (sentence in sentences) {
                    if (group.length + sentence.length > MAX_SEGMENT_CHARS && group.isNotEmpty()) {
                        val joined = group.toString().replace(Regex("""\s+"""), " ").trim()
                        if (joined.length >= MIN_SEGMENT_CHARS) {
                            segments.add(createSegment(joined, ContentSegment.TYPE_PARAGRAPH, pageInfo))
                        }
                        group.clear()
                    }
                    if (group.isNotEmpty()) group.append(" ")
                    group.append(sentence)
                }
                if (group.isNotEmpty()) {
                    val joined = group.toString().replace(Regex("""\s+"""), " ").trim()
                    if (joined.length >= MIN_SEGMENT_CHARS) {
                        segments.add(createSegment(joined, ContentSegment.TYPE_PARAGRAPH, pageInfo))
                    }
                }
                continue
            }

            val sentences = splitIntoSentences(cleaned)
            for (sentence in sentences) {
                if (sentence.length >= MIN_SEGMENT_CHARS) {
                    segments.add(createSegment(sentence.trim(), ContentSegment.TYPE_SENTENCE, pageInfo))
                }
            }
        }

        return segments.filter { isQualitySegment(it) }
    }

    private fun createSegment(text: String, type: String, pageInfo: PageInfo?): ContentSegment {
        val wordCount = text.split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        return ContentSegment(
            text = text,
            type = type,
            wordCount = wordCount,
            hasKnowledge = hasKnowledgeIndicator(text),
            sourcePageStart = pageInfo?.pageStart,
            sourcePageEnd = pageInfo?.pageEnd,
            sourceType = ContentSegment.SOURCE_TYPE_MERGED,
            sourceSnippet = pageInfo?.sourceSnippet ?: text.take(200)
        )
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text
            .split(Regex("""(?<=[.!?])\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun hasKnowledgeIndicator(text: String): Boolean {
        val words = text.lowercase().split(Regex("""\s+"""))
        val knowledgeWordCount = words.count { it in knowledgeKeywords }
        // Có ít nhất 2 từ khóa kiến thức hoặc có dấu câu hoàn chỉnh
        return knowledgeWordCount >= 2 || text.last() in ".!?"
    }

    private fun isQualitySegment(segment: ContentSegment): Boolean {
        val text = segment.text

        if (text.length < MIN_SEGMENT_CHARS) return false
        if (text.length > MAX_SEGMENT_CHARS * 2) return false
        if (segment.wordCount < MIN_WORD_COUNT) return false
        if (text.all { it.isDigit() || it.isWhitespace() || it in ".,-/:()" }) return false
        if (text.matches(Regex("""^\d+(\.\d+)*\s+.*$"""))) return false

        return true
    }

    private fun isRemovedLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length < MIN_LINE_CHARS) return true
        if (isOnlySymbols(trimmed)) return true
        if (noisePatterns.any { it.matches(trimmed) }) return true
        if (isUselessHeading(trimmed)) return true
        if (isTocLine(trimmed)) return true
        if (isPureNumberingLine(trimmed)) return true
        return false
    }
}
