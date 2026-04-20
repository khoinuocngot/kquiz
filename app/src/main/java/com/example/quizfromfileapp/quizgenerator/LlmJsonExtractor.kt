package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Trích xuất JSON sạch từ output của LLM.
 *
 * LLM thường trả về:
 * - ```json ... ``` (markdown code fence)
 * - ``` ... ``` (generic code fence)
 * - Lời giải thích trước/sau JSON
 * - Text thường lẫn trong JSON
 *
 * LlmJsonExtractor:
 * 1. Strip markdown fences
 * 2. Tìm boundary của JSON object/array hợp lệ
 * 3. Validate JSON structure
 * 4. Trả về JSON sạch hoặc null
 *
 * Nếu không extract được → parse sẽ fail và caller fallback.
 */
object LlmJsonExtractor {

    private const val TAG = "LlmJsonExtractor"

    /**
     * Trích xuất JSON sạch từ response string.
     *
     * Thứ tự xử lý:
     * 1. Strip ```json ... ``` hoặc ``` ... ```
     * 2. Strip lời giải thích trước/sau JSON
     * 3. Tìm JSON object/array hợp lệ đầu tiên
     * 4. Validate bracket balance
     * 5. Trim whitespace
     *
     * @param rawResponse  Raw string từ LLM
     * @return             JSON string sạch, hoặc null nếu không tìm được
     */
    fun extractCleanJson(rawResponse: String): String? {
        if (rawResponse.isBlank()) {
            logd("extractCleanJson: input rỗng")
            return null
        }

        val cleaned = rawResponse.trim()
        logd("Input length: ${cleaned.length}")

        // Bước 1: Strip markdown fences
        val step1 = stripMarkdownFences(cleaned)
        logd("After fence strip: ${step1.length} chars")

        // Bước 2: Strip lời giải thích trước JSON
        val step2 = stripPreamble(step1)
        logd("After preamble strip: ${step2.length} chars")

        // Bước 3: Strip lời kết luận sau JSON
        val step3 = stripPostamble(step2)
        logd("After postamble strip: ${step3.length} chars")

        // Bước 4: Tìm JSON object/array hợp lệ đầu tiên
        val step4 = findFirstJsonObject(step3)
        if (step4 == null) {
            loge("Không tìm được JSON object/array trong response")
            return null
        }

        // Bước 5: Validate bracket balance
        if (!isBalanced(step4)) {
            loge("JSON không cân bằng brackets")
            return null
        }

        // Bước 6: Trim
        val final = step4.trim()
        logd("Final JSON length: ${final.length}")

        // Bước 7: Quick validation — phải bắt đầu bằng { hoặc [
        if (!final.startsWith("{") && !final.startsWith("[")) {
            loge("JSON không bắt đầu bằng { hoặc [: ${final.take(30)}")
            return null
        }

        return final
    }

    // ─────────────────────────────────────────────────────────────
    // Strip Markdown Fences
    // ─────────────────────────────────────────────────────────────

    /**
     * Strip các loại markdown code fence.
     *
     * Handle:
     * - ```json ... ```
     * - ```json\n ... ```
     * - ``` ... ```
     * - ```json
     * -   ... (indented code block)
     */
    private fun stripMarkdownFences(text: String): String {
        var result = text

        // Strip ```json ... ```
        result = stripPattern(result, """```json\s*(.*?)\s*```""")

        // Strip ```javascript ... ``` (LLM hay dùng)
        result = stripPattern(result, """```javascript\s*(.*?)\s*```""")

        // Strip ``` ... ``` generic
        result = stripPattern(result, """```\s*(.*?)\s*```""")

        // Strip ``` ở đầu (không có closing fence)
        if (result.trimStart().startsWith("```")) {
            result = result.trimStart().substringAfter("```").substringAfter("\n").trimStart()
        }

        // Strip ``` ở cuối
        val trimmed = result.trimEnd()
        if (trimmed.endsWith("```")) {
            result = trimmed.substringBeforeLast("```").trimEnd()
        }

        return result
    }

    private fun stripPattern(
        text: String,
        pattern: String
    ): String {
        return try {
            val regex = Regex(pattern, setOf(RegexOption.DOT_MATCHES_ALL))
            regex.replace(text, "$1").trim()
        } catch (_: Exception) {
            text
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Strip Preamble (lời giải thích TRƯỚC JSON)
    // ─────────────────────────────────────────────────────────────

    /**
     * Strip lời giải thích trước JSON.
     *
     * LLM thường viết:
     * "Đây là JSON: { ... }"
     * "Here is the JSON: { ... }"
     * "JSON: { ... }"
     * "Here are the questions: { ... }"
     *
     * Chúng ta tìm ký tự { hoặc [ đầu tiên và cắt từ đó.
     */
    private fun stripPreamble(text: String): String {
        val firstBrace = text.indexOfFirst { it == '{' || it == '[' }
        return if (firstBrace < 0) {
            text
        } else if (firstBrace == 0) {
            text
        } else {
            val preamble = text.substring(0, firstBrace)
            // Nếu preamble chỉ là whitespace và dấu xuống dòng, strip luôn
            val withoutNewline = preamble.trimEnd()
            // Nếu preamble có nhiều hơn 1 dòng có nội dung thực, giữ lại
            val lines = withoutNewline.lines()
            val meaningfulLines = lines.takeWhile { it.isBlank() || it.matches(Regex(".*\\p{L}.*")) }
            if (meaningfulLines.size <= 1) {
                text.substring(firstBrace)
            } else {
                text
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Strip Postamble (lời kết luận SAU JSON)
    // ─────────────────────────────────────────────────────────────

    /**
     * Strip lời kết luận sau JSON.
     *
     * LLM thường viết:
     * "{ ... }"
     * "Tôi đã tạo 3 câu hỏi theo yêu cầu."
     * "I generated 3 questions as requested."
     * "Hãy cho tôi biết nếu cần thêm."
     *
     * Cách xử lý: tìm JSON boundary và cắt phần excess.
     */
    private fun stripPostamble(text: String): String {
        // Tìm vị trí đóng của JSON object/array cuối cùng
        val jsonEnd = findJsonEnd(text)
        if (jsonEnd < 0) return text

        // Lấy phần từ đầu đến jsonEnd
        val jsonPart = text.substring(0, jsonEnd + 1)

        // Kiểm tra phần sau jsonEnd có phải là lời kết luận không
        val afterJson = text.substring(jsonEnd + 1).trim()
        if (afterJson.isEmpty()) return jsonPart

        // Nếu afterJson chứa ký tự { hoặc [ → có thể còn JSON khác
        if (afterJson.contains("{") || afterJson.contains("[")) {
            // Giữ nguyên vì có thể là multi-JSON response
            return jsonPart
        }

        // Kiểm tra afterJson có phải là text thuần không
        val isPureText = afterJson.length < 50 || afterJson.lines().all { line ->
            line.isBlank() || !line.contains("{") && !line.contains("[")
        }

        return if (isPureText) {
            jsonPart
        } else {
            text
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Find First JSON Object
    // ─────────────────────────────────────────────────────────────

    /**
     * Tìm JSON object/array hợp lệ đầu tiên trong text.
     *
     * Strategy:
     * 1. Tìm vị trí { hoặc [ đầu tiên
     * 2. Verify bracket balance đến cuối text
     * 3. Nếu không cân bằng, cắt bớt excess text
     */
    private fun findFirstJsonObject(text: String): String? {
        val startPos = text.indexOfFirst { it == '{' || it == '[' }
        if (startPos < 0) return null

        val startChar = text[startPos]

        // Tìm JSON end bằng bracket matching
        var depth = 0
        var inString = false
        var escaped = false

        for (i in startPos until text.length) {
            val c = text[i]

            if (escaped) {
                escaped = false
                continue
            }

            when (c) {
                '\\' -> {
                    if (inString) escaped = true
                }
                '"' -> {
                    if (!escaped) inString = !inString
                }
                '{', '[' -> {
                    if (!inString) depth++
                }
                '}', ']' -> {
                    if (!inString) {
                        depth--
                        if (depth == 0) {
                            return text.substring(startPos, i + 1)
                        }
                    }
                }
            }
        }

        // Nếu không tìm được end hợp lệ, thử fallback
        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            // Thử strip text sau bằng heuristic
            val jsonEnd = findJsonEnd(text)
            if (jsonEnd >= 0) {
                return text.substring(startPos, jsonEnd + 1)
            }
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────
    // JSON Boundary Detection
    // ─────────────────────────────────────────────────────────────

    /**
     * Tìm vị trí đóng của JSON object/array cuối cùng trong text.
     * Dùng cho stripPostamble.
     */
    private fun findJsonEnd(text: String): Int {
        var depth = 0
        var inString = false
        var escaped = false
        var lastEnd = -1

        for (i in text.indices) {
            val c = text[i]

            if (escaped) {
                escaped = false
                continue
            }

            when (c) {
                '\\' -> {
                    if (inString) escaped = true
                }
                '"' -> {
                    if (!escaped) inString = !inString
                }
                '{', '[' -> {
                    if (!inString) depth++
                }
                '}', ']' -> {
                    if (!inString) {
                        depth--
                        if (depth == 0) lastEnd = i
                    }
                }
            }
        }

        return lastEnd
    }

    /**
     * Kiểm tra bracket balance.
     */
    private fun isBalanced(text: String): Boolean {
        var depth = 0
        var inString = false
        var escaped = false

        for (c in text) {
            if (escaped) {
                escaped = false
                continue
            }

            when (c) {
                '\\' -> {
                    if (inString) escaped = true
                }
                '"' -> {
                    if (!escaped) inString = !inString
                }
                '{', '[' -> {
                    if (!inString) depth++
                }
                '}', ']' -> {
                    if (!inString) {
                        depth--
                        if (depth < 0) return false
                    }
                }
            }
        }

        return depth == 0
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }

    private fun loge(message: String) {
        Log.e(TAG, message)
    }
}
