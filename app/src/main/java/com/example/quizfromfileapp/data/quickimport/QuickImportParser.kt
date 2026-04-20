package com.example.quizfromfileapp.data.quickimport

import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom

/**
 * Parser cho Quick Import text.
 *
 * Nguyên tắc:
 * - Chỉ tách cards theo newline thật (\n) hoặc custom card delimiter
 * - Chỉ tách term-definition theo delimiter đã chọn
 * - Không dựa vào line wrap của UI
 * - Trim chỉ áp dụng cho từng term/definition sau khi tách
 * - Không trim/format toàn bộ raw input trước khi parse
 */
object QuickImportParser {

    /**
     * Loại bỏ markdown code fence (```) khỏi text paste từ ChatGPT.
     * Xử lý:
     * - Dòng bắt đầu bằng ``` (có thể kèm ngôn ngữ: ```text, ```plain, ```)
     * - Dòng kết thúc bằng ```
     * - Giữ nguyên nội dung bên trong
     */
    fun sanitizeChatGptPastedText(text: String): String {
        val lines = text.lines()
        val result = mutableListOf<String>()
        var insideFence = false
        var fenceStartIndex = -1
        var fenceEndIndex = -1

        // Tìm vị trí đầu/cuối code fence
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (!insideFence) {
                if (trimmed.startsWith("```")) {
                    insideFence = true
                    fenceStartIndex = index
                } else {
                    result.add(line)
                }
            } else {
                if (trimmed == "```" || trimmed.endsWith("```")) {
                    insideFence = false
                    fenceEndIndex = index
                }
            }
        }

        // Nếu tìm thấy cặp fence, lấy nội dung bên trong
        if (fenceStartIndex >= 0 && fenceEndIndex >= 0) {
            result.clear()
            for (i in (fenceStartIndex + 1) until fenceEndIndex) {
                result.add(lines[i])
            }
        }

        // Loại bỏ dòng đầu tiên nếu là "text", "plain", ngôn ngữ
        if (result.isNotEmpty()) {
            val first = result.first().trim()
            if (first == "text" || first == "plain" || first == "csv" || first == "markdown" ||
                first.startsWith("```") || first.isBlank()) {
                result.removeAt(0)
            }
        }

        // Loại bỏ dòng cuối nếu là ```
        if (result.isNotEmpty() && result.last().trim() == "```") {
            result.removeAt(result.lastIndex)
        }

        return result.joinToString("\n")
    }

    /**
     * Normalize tất cả các loại newline về \n.
     * Giữ nguyên nội dung còn lại.
     */
    fun normalizeNewlines(text: String): String {
        return text
            .replace("\r\n", "\n")   // Windows CRLF -> LF
            .replace("\r", "\n")       // Old Mac CR -> LF
    }

    /**
     * Parse raw text thành danh sách card.
     * Input được sanitize (strip code fence) và normalize newline trước khi parse.
     */
    fun parse(config: QuickImportConfig): ParseResult {
        val rawText = config.rawText

        // Sanitize code fence trước
        val sanitized = sanitizeChatGptPastedText(rawText)
        // Normalize newlines
        val normalized = normalizeNewlines(sanitized)
        val rawLineCount = if (normalized.isBlank()) 0 else normalized.lines().size

        if (sanitized.isBlank()) {
            return ParseResult(
                validCards = emptyList(),
                invalidLines = emptyList(),
                totalLines = 0,
                rawLineCount = 0,
                rawCharCount = 0
            )
        }

        val lines = splitIntoLines(normalized, config)

        val validCards = mutableListOf<ParsedCard>()
        val invalidLines = mutableListOf<ParsedCard>()

        for ((index, line) in lines.withIndex()) {
            // Không trim toàn bộ line ở đây — trim chỉ trong parseLine
            val result = parseLine(line, config, index + 1)
            if (result.isValid && config.studySetType == StudySetType.MULTIPLE_CHOICE_BANK) {
                // Extract MCQ options from term for MULTIPLE_CHOICE_BANK
                val mcqData = extractMcqOptions(result.term, result.definition)
                validCards.add(result.copy(
                    term = mcqData.first,  // question only (no "A. B. C. D.")
                    definition = result.definition,  // the correct answer
                    choices = mcqData.second,
                    correctChoiceIndex = mcqData.third,
                    itemType = FlashcardEntityRoom.ITEM_TYPE_MULTIPLE_CHOICE
                ))
            } else if (result.isValid) {
                validCards.add(result)
            } else {
                invalidLines.add(result)
            }
        }

        return ParseResult(
            validCards = validCards,
            invalidLines = invalidLines,
            totalLines = lines.size,
            rawLineCount = rawLineCount,
            rawCharCount = sanitized.length
        )
    }

    /**
     * Split text thành các dòng theo card delimiter.
     * Chỉ dùng delimiter thật, không dựa vào UI line wrap.
     */
    private fun splitIntoLines(normalizedText: String, config: QuickImportConfig): List<String> {
        return when (config.cardDelimiterMode) {
            QuickImportConfig.CardDelimiterMode.ONE_PER_LINE -> {
                normalizedText
                    .split("\n")
                    .map { line -> line.trimEnd('\r') } // đề phòng
                    .filter { it.isNotBlank() }
                    // Trim từng dòng SAU khi lọc blank
                    .map { it.trim() }
            }
            QuickImportConfig.CardDelimiterMode.CUSTOM -> {
                val cardDel = config.cardDelimiterCustom
                if (cardDel.isBlank()) {
                    // Không có custom delimiter → fall back về newline
                    normalizedText
                        .split("\n")
                        .map { it.trimEnd('\r') }
                        .filter { it.isNotBlank() }
                        .map { it.trim() }
                } else {
                    normalizedText
                        .split(cardDel)
                        .filter { it.isNotBlank() }
                        .map { it.trim() }
                }
            }
        }
    }

    /**
     * Extract MCQ options (A. B. C. D.) from term and find correct answer index.
     *
     * Input term example:
     *   "Ai là hoàng đế đầu tiên của nhà Ngô? A. Đinh Bộ Lĩnh B. Lê Hoàn C. Lý Công Uẩn D. Trần Thái Tông"
     *
     * Returns Triple(questionText, choiceTexts, correctChoiceIndex) where:
     * - questionText: the question without option labels (e.g., "Ai là hoàng đế đầu tiên của nhà Ngô?")
     * - choiceTexts: list of options in order (e.g., ["A. Đinh Bộ Lĩnh", "B. Lê Hoàn", ...])
     * - correctChoiceIndex: 0-based index matching choiceTexts order
     *
     * Algorithm:
     * 1. Find all "X. " patterns in the term (where X is A-Z followed by period and space).
     * 2. Extract question text = text before the first option label.
     * 3. Extract each option's text between "X. " and the next letter option marker or end of string.
     * 4. Match the correct answer to the option that contains it (case-insensitive).
     */
    fun extractMcqOptions(term: String, correctAnswer: String): Triple<String, List<String>, Int> {
        // Regex: find patterns like "A. ", "B. ", "C. " etc.
        val optionPattern = Regex("""([A-Z])\.\s+(.*?)(?=(?:[A-Z]\. )|$)""", RegexOption.DOT_MATCHES_ALL)

        val matches = optionPattern.findAll(term).toList()

        if (matches.isEmpty()) {
            return Triple(term, emptyList(), -1)
        }

        // Extract question text = text before first option
        val firstMatchStart = matches.first().range.first
        val questionText = term.substring(0, firstMatchStart).trim()

        // Extract options in order
        val options = matches.map { match ->
            val label = match.groupValues[1] // "A", "B", etc.
            val text = match.groupValues[2].trim()
            "$label. $text"
        }

        // Normalize correct answer for comparison
        val normalizedAnswer = correctAnswer.trim().lowercase()
        val normalizedOptions = options.map { it.substringAfter(". ").lowercase().trim() }

        // Try exact match first, then contains match
        val correctIndex = normalizedOptions.indexOfFirst { opt ->
            opt == normalizedAnswer || opt.contains(normalizedAnswer) || normalizedAnswer.contains(opt)
        }

        return Triple(questionText, options, correctIndex)
    }

    /**
     * Parse một dòng thành ParsedCard.
     * Trim term và definition SAU KHI tách delimiter.
     */
    private fun parseLine(
        line: String,
        config: QuickImportConfig,
        lineNumber: Int
    ): ParsedCard {
        // Dòng trống sau khi split → skip (đã được lọc ở trên nhưng check lại)
        if (line.isBlank()) {
            return ParsedCard(
                term = "",
                definition = "",
                isValid = false,
                rawLine = line,
                errorMessage = "Dòng trống"
            )
        }

        // Tìm delimiter term-definition
        val termDefDel = when (config.termDefDelimiter) {
            QuickImportConfig.TermDefDelimiter.CUSTOM -> {
                config.cardDelimiterCustom.ifBlank { "" }
            }
            else -> config.termDefDelimiter.value
        }

        val (termPart, defPart) = if (termDefDel.isNotEmpty()) {
            splitByFirstDelimiter(line, termDefDel)
        } else {
            // Không có delimiter → toàn bộ là term, definition rỗng
            Pair(line, "")
        }

        // Trim term và definition SAU KHI tách
        val term = termPart.trim()
        val definition = defPart.trim()

        // Validate
        if (term.isBlank()) {
            return ParsedCard(
                term = term,
                definition = definition,
                isValid = false,
                rawLine = line,
                errorMessage = "Thiếu thuật ngữ (term)"
            )
        }

        if (definition.isBlank()) {
            return ParsedCard(
                term = term,
                definition = definition,
                isValid = false,
                rawLine = line,
                errorMessage = "Thiếu định nghĩa (definition)"
            )
        }

        val itemType = when (config.studySetType) {
            StudySetType.TERM_DEFINITION -> FlashcardEntityRoom.ITEM_TYPE_TERM_DEFINITION
            StudySetType.QUESTION_ANSWER -> FlashcardEntityRoom.ITEM_TYPE_QUESTION_ANSWER
            StudySetType.MULTIPLE_CHOICE_BANK -> FlashcardEntityRoom.ITEM_TYPE_MULTIPLE_CHOICE
        }

        return ParsedCard(
            term = term,
            definition = definition,
            isValid = true,
            rawLine = line,
            errorMessage = null,
            itemType = itemType
        )
    }

    /**
     * Split dòng bằng delimiter — CHỉ split ở delimiter ĐẦU TIÊN.
     * Term không chứa delimiter, definition bắt đầu sau delimiter.
     */
    private fun splitByFirstDelimiter(line: String, delimiter: String): Pair<String, String> {
        val index = line.indexOf(delimiter)
        return if (index >= 0) {
            val term = line.substring(0, index)
            val definition = line.substring(index + delimiter.length)
            Pair(term, definition)
        } else {
            // Không tìm thấy delimiter → toàn bộ là term
            Pair(line, "")
        }
    }

    /**
     * Tạo sample preview text.
     */
    fun createSampleText(sampleSize: Int = 5): String {
        val samples = listOf(
            "CPU\tBộ xử lý trung tâm của máy tính",
            "RAM\tBộ nhớ truy cập ngẫu nhiên, dùng để lưu dữ liệu tạm thời",
            "ROM\tBộ nhớ chỉ đọc, không thể thay đổi nội dung",
            "Hard Drive\tThiết bị lưu trữ dữ liệu vĩnh viễn trên máy tính",
            "GPU\tBộ xử lý đồ họa, chuyên xử lý hình ảnh và video"
        )
        return samples.take(sampleSize).joinToString("\n")
    }

    /**
     * Preview parse — cùng logic với parse().
     */
    fun previewParse(config: QuickImportConfig): ParseResult {
        return parse(config)
    }
}
