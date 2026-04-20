package com.example.quizfromfileapp.quizgenerator

import com.example.quizfromfileapp.domain.model.ContentSegment

/**
 * Xây dựng prompt tối ưu cho local on-device LLM.
 *
 * Hỗ trợ 2 chiến lược:
 *
 * 1. Direct MCQ (bước đơn):
 *    Prompt → LLM → JSON MCQ
 *    Dùng cho fallback nhanh
 *
 * 2. Two-Step Fact → MCQ (bước kép):
 *    Bước 1: Prompt FACT → LLM → JSON facts
 *    Bước 2: Prompt MCQ từ facts → LLM → JSON MCQ
 *    Chất lượng cao hơn, ít copy nguồn hơn
 *
 * Nguyên tắc thiết kế:
 * 1. Strict JSON only — không markdown, không code fence, không giải thích ngoài JSON
 * 2. Quality rules rõ ràng — reject generic/paraphrase/heading questions
 * 3. Format chuẩn cứng — LLM phải tuân theo schema
 * 4. Batch nhỏ — 2 câu mỗi batch cho local model yếu
 * 5. Ngôn ngữ theo source content
 */
object LlmPromptBuilder {

    private const val MAX_SEGMENTS_PER_BATCH = 6
    private const val MIN_SEGMENT_CHARS = 40
    private const val SEGMENT_SNIPPET_LIMIT = 180
    const val OPTIMAL_QUESTIONS_PER_BATCH = 2

    // ═══════════════════════════════════════════════════════════════
    // Bước 1: FACT EXTRACTION PROMPT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build prompt để trích xuất facts từ segments.
     *
     * Output: JSON array of facts
     * Schema: {"facts":[{"concept":"...","factStatement":"...","sourceSnippet":"..."}]}
     */
    fun buildFactExtractionPrompt(
        segments: List<ContentSegment>,
        language: String
    ): String {
        val qualitySegments = segments
            .filter { it.text.length >= MIN_SEGMENT_CHARS }
            .take(MAX_SEGMENTS_PER_BATCH)

        if (qualitySegments.isEmpty()) return ""

        return if (language == "vi") {
            buildVietnameseFactPrompt(qualitySegments)
        } else {
            buildEnglishFactPrompt(qualitySegments)
        }
    }

    private fun buildEnglishFactPrompt(segments: List<ContentSegment>): String {
        val segmentList = segments.mapIndexed { index, seg ->
            val snippet = seg.text.take(SEGMENT_SNIPPET_LIMIT)
            val pageLabel = if (seg.sourcePageStart != null) " [Page ${seg.sourcePageStart}]" else ""
            "[$index] $snippet$pageLabel"
        }.joinToString("\n\n")

        return """
|Extract atomic KNOWLEDGE FACTS from the content below.

RULES:
1. PURE JSON ONLY — no markdown, no explanation, no code fence
2. Start with { and end with }
3. Only extract declarative KNOWLEDGE statements — not examples, exercises, formulas, instructions, or headings
4. Each fact must be: a single clear statement about a concept, 30-150 characters, specific (not generic)
5. Extract the CONCEPT (1-3 key words) and the FACT STATEMENT (what is true about it)
6. DO NOT extract:
   - Examples (sentences starting with "for example", "such as", "e.g.")
   - Exercises and questions
   - Formulas, equations, or code
   - Numbered lists or bullet points
   - Captions, headings, or footnotes
   - Instruction sentences ("to do X, first...")
   - Generic statements with no specific knowledge

CONTENT:
$segmentList

OUTPUT SCHEMA:
{"facts":[{"concept":"...","factStatement":"...","sourceSnippet":"..."}]}

Only include facts that are genuine knowledge claims. Each factStatement must be a standalone truth.
""".trim()
    }

    private fun buildVietnameseFactPrompt(segments: List<ContentSegment>): String {
        val segmentList = segments.mapIndexed { index, seg ->
            val snippet = seg.text.take(SEGMENT_SNIPPET_LIMIT)
            val pageLabel = if (seg.sourcePageStart != null) " [Trang ${seg.sourcePageStart}]" else ""
            "[$index] $snippet$pageLabel"
        }.joinToString("\n\n")

        return """
|Trích xuất các MỆNH ĐỀ KIẾN THỨC TỪ nội dung bên dưới.

QUY TẮC:
1. CHỈ JSON thuần — không markdown, không giải thích, không code fence
2. Bắt đầu bằng { và kết thúc bằng }
3. Chỉ trích declarative KNOWLEDGE statements — không phải ví dụ, bài tập, công thức, chỉ dẫn, hay tiêu đề
4. Mỗi fact phải là: một câu khẳng định rõ ràng về khái niệm, 30-150 ký tự, cụ thể (không chung chung)
5. Trích CONCEPT (1-3 từ khóa) và FACT STATEMENT (điều gì đúng về nó)
6. KHÔNG trích:
   - Ví dụ (câu bắt đầu "ví dụ", "chẳng hạn", "như là")
   - Bài tập và câu hỏi
   - Công thức, phương trình, code
   - Danh sách số hoặc bullet points
   - Chú thích, tiêu đề, ghi chú
   - Câu chỉ dẫn ("để làm X, trước tiên...")
   - Câu chung chung không có kiến thức cụ thể

NỘI DUNG:
$segmentList

SCHEMA ĐẦU RA:
{"facts":[{"concept":"...","factStatement":"...","sourceSnippet":"..."}]}

Chỉ bao gồm facts là các khẳng định kiến thức thực sự. Mỗi factStatement phải là một sự thật độc lập.
""".trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // Bước 2: MCQ FROM FACTS PROMPT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build prompt để sinh MCQ từ facts đã trích xuất.
     *
     * Output: JSON array of MCQ questions
     * Schema: {"questions":[{"question":"...","options":[...],"correctAnswerIndex":0,"explanation":"...","sourceSnippet":"...","sourcePageStart":1,"sourcePageEnd":1}]}
     */
    fun buildMcqFromFactsPrompt(
        facts: List<FactItem>,
        targetCount: Int,
        language: String
    ): String {
        if (facts.isEmpty()) return ""

        val clampedCount = targetCount.coerceIn(1, OPTIMAL_QUESTIONS_PER_BATCH)

        return if (language == "vi") {
            buildVietnameseMcqPrompt(facts, clampedCount)
        } else {
            buildEnglishMcqPrompt(facts, clampedCount)
        }
    }

    private fun buildEnglishMcqPrompt(facts: List<FactItem>, targetCount: Int): String {
        val factList = facts.take(8).mapIndexed { index, fact ->
            val pageLabel = if (fact.sourcePageStart != null) " [Page ${fact.sourcePageStart}]" else ""
            "[$index] Concept: ${fact.concept}\n    Fact: ${fact.factStatement}$pageLabel"
        }.joinToString("\n\n")

        val templateHints = QuestionTemplateRotator.suggestTemplatesForBatch(0, targetCount, "en")
        val templateSection = if (templateHints.isNotEmpty()) {
            "\nTEMPLATE HINTS (rotate, do NOT repeat same template):\n${templateHints.joinToString("\n")}"
        } else ""

        return """
|Generate $targetCount multiple-choice quiz questions (4 options each) from the FACTS below.

STRICT ANTI-PLAGIARISM RULES:
1. PURE JSON ONLY — no markdown, no code fence, no explanation outside JSON
2. Start with { and end with } — nothing before or after
3. CORRECT ANSWER MUST NEVER COPY THE FACT VERBATIM
   - Correct answer must be SHORTER than factStatement (at least 30% shorter)
   - Correct answer must REWORD THE FACT CONCEPT, not paste the sentence
   - WRONG: "Raster graphics is a type of digital image represented as a rectangular grid of pixels." (verbatim)
   - RIGHT: "A digital image format using pixel arrays" or "Image represented via rectangular pixel grids"
4. DISTRACTORS must be on the SAME TOPIC, plausibly wrong
   - Distractors must share the same concept/domain
   - Wrong due to minor inaccuracy, not complete nonsense
   - WRONG: "A type of food preparation method" (completely unrelated)
   - RIGHT: (if correct is "vector graphics") → "Bitmap raster format" (same graphics domain, slightly wrong)
5. DO NOT paraphrase a fact verbatim as the correct answer
6. DO NOT create questions that only rephrase the fact statement
7. DO NOT use list-style options
8. DO NOT repeat question templates

GOOD QUESTION STRUCTURE:
- Ask about SPECIFIC KNOWLEDGE from the facts (not "what is this about")
- Correct answer: concise paraphrase of the fact (SHORTENED, REWORDED)
- 3 Distractors: same topic, each wrong in a different way
- Explanation: 1-2 sentences, explain WHY the correct answer is correct

FACTS (use these as your source):
$factList
$templateSection

OUTPUT SCHEMA:
{"questions":[{"question":"...","options":["...","...","...","..."],"correctAnswerIndex":0,"explanation":"...","sourceSnippet":"...","sourcePageStart":1,"sourcePageEnd":1}]}
""".trim()
    }

    private fun buildVietnameseMcqPrompt(facts: List<FactItem>, targetCount: Int): String {
        val factList = facts.take(8).mapIndexed { index, fact ->
            val pageLabel = if (fact.sourcePageStart != null) " [Trang ${fact.sourcePageStart}]" else ""
            "[$index] Khái niệm: ${fact.concept}\n    Fact: ${fact.factStatement}$pageLabel"
        }.joinToString("\n\n")

        val templateHints = QuestionTemplateRotator.suggestTemplatesForBatch(0, targetCount, "vi")
        val templateSection = if (templateHints.isNotEmpty()) {
            "\nGỢI Ý TEMPLATE (xoay vòng, KHÔNG lặp cùng template):\n${templateHints.joinToString("\n")}"
        } else ""

        return """
|Tạo $targetCount câu hỏi trắc nghiệm 4 đáp án từ các FACTS bên dưới.

QUY TẮC CHỐNG COPY (BẮT BUỘC):
1. CHỈ JSON thuần — không markdown, không code fence, không giải thích ngoài JSON
2. Bắt đầu bằng { và kết thúc bằng } — không có gì trước hoặc sau
3. ĐÁP ÁN ĐÚNG TUYỆT ĐỐI KHÔNG COPY FACT NGUYÊN VĂN
   - Đáp án đúng phải NGẮN HƠN factStatement (ít nhất 30% ngắn hơn)
   - Đáp án đúng phải DIỄN ĐẠT LẠI Ý TƯỞNG, không paste câu gốc
   - SAI: "Raster graphics là loại hình ảnh kỹ thuật số được biểu diễn dưới dạng lưới hình chữ nhật các pixel." (verbatim)
   - ĐÚNG: "Định dạng ảnh kỹ thuật số dùng mảng pixel" hoặc "Hình ảnh biểu diễn qua lưới pixel hình chữ nhật"
4. DISTRACTOR phải cùng CHỦ ĐỀ, sai vừa phải
   - Distractor phải chia sẻ cùng concept/domain
   - Sai do thiếu chính xác nhẹ, không phải vô nghĩa hoàn toàn
   - SAI: "Một loại phương pháp chuẩn bị thực phẩm" (hoàn toàn không liên quan)
   - ĐÚNG: (nếu đúng là "vector graphics") → "Định dạng raster bitmap" (cùng lĩnh vực graphics, sai nhẹ)
5. KHÔNG paraphrase nguyên fact thành đáp án đúng
6. KHÔNG tạo câu hỏi chỉ đổi vài từ so với fact
7. KHÔNG dùng options dạng danh sách
8. KHÔNG lặp cùng template

CẤU TRÚC CÂU HỎI TỐT:
- Hỏi về KIẾN THỨC CỤ THỂ từ facts (không phải "đoạn văn nói về gì")
- Đáp án đúng: paraphrase ngắn gọn của fact (RÚT GỌN, DIỄN ĐẠT LẠI)
- 3 Distractors: cùng chủ đề, mỗi cái sai theo cách khác nhau
- Explanation: 1-2 câu, giải thích TẠI SAO đáp án đúng mới đúng

FACTS (dùng làm nguồn):
$factList
$templateSection

SCHEMA ĐẦU RA:
{"questions":[{"question":"...","options":["...","...","...","..."],"correctAnswerIndex":0,"explanation":"...","sourceSnippet":"...","sourcePageStart":1,"sourcePageEnd":1}]}
""".trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // LEGACY: Direct MCQ prompt (giữ lại cho fallback nhanh)
    // ═══════════════════════════════════════════════════════════════

    fun buildPrompt(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): String {
        val qualitySegments = segments
            .filter { it.text.length >= MIN_SEGMENT_CHARS }
            .take(MAX_SEGMENTS_PER_BATCH)

        if (qualitySegments.isEmpty()) return ""
        val clampedCount = targetCount.coerceIn(1, OPTIMAL_QUESTIONS_PER_BATCH)
        return if (language == "vi") {
            buildVietnameseDirectPrompt(qualitySegments, clampedCount)
        } else {
            buildEnglishDirectPrompt(qualitySegments, clampedCount)
        }
    }

    private fun buildVietnameseDirectPrompt(segments: List<ContentSegment>, targetCount: Int): String {
        val segmentList = segments.mapIndexed { index, seg ->
            val snippet = seg.text.take(SEGMENT_SNIPPET_LIMIT)
            val pageLabel = if (seg.sourcePageStart != null) " [Trang ${seg.sourcePageStart}]" else ""
            "[$index] $snippet$pageLabel"
        }.joinToString("\n\n")

        val templateHints = QuestionTemplateRotator.suggestTemplatesForBatch(0, targetCount, "vi")
        val templateSection = if (templateHints.isNotEmpty()) {
            "\nGỢI Ý TEMPLATE (xoay vòng, KHÔNG lặp cùng template):\n${templateHints.joinToString("\n")}"
        } else ""

        return """
|Tạo $targetCount câu hỏi trắc nghiệm 4 đáp án từ nội dung bên dưới.

QUY TẮC CHỐNG COPY (BẮT BUỘC TUÂN THỦ):
1. PURE JSON ONLY — không markdown, không code fence, không lời giải thích ngoài JSON
2. Response bắt đầu bằng { và kết thúc bằng } — KHÔNG có gì khác trước hoặc sau
3. ĐÁP ÁN ĐÚNG TUYỆT ĐỐI KHÔNG ĐƯỢC COPY NGUYÊN NGUỒN
   - Đáp án đúng phải ngắn hơn sourceSnippet rõ ràng (ít nhất 30% ngắn hơn)
   - Đáp án đúng phải DIỄN ĐẠT LẠI Ý, không phải copy thẳng
4. DISTRACTOR phải cùng chủ đề, sai vừa phải, không quá vô lý
5. KHÔNG paraphrase nguyên câu nguồn thành đáp án đúng
6. KHÔNG tạo câu hỏi chỉ đổi vài từ so với sourceSnippet
7. KHÔNG dùng heading, mục lục, caption, chú thích làm nguồn
8. KHÔNG tạo 2 câu cùng template trong cùng batch
9. KHÔNG tạo câu hỏi "Đâu là...?" hoặc "Cái nào là...?" với options là danh sách
10. Question diversity: đa dạng hóa — summary, detail, function, property, implication

CẤU TRÚC CÂU HỎI TỐT:
- Đặt câu hỏi về CHI TIẾT CỤ THỂ từ nội dung (không phải "đoạn văn nói về gì")
- Đáp án đúng: ngắn gọn, diễn đạt lại ý chính (KHÔNG copy nguyên câu dài)
- 3 Distractors: cùng chủ đề, mỗi cái sai theo cách khác nhau
- Explanation: 1-2 câu, nêu VÌ SAO đáp án đúng mới đúng

NỘI DUNG:
$segmentList
$templateSection

YÊU CẦU ĐẦU RA (STRICT):
- Chỉ JSON thuần, bắt đầu từ ký tự đầu tiên
- Không có ```json hay ``` trước hoặc sau
- Số lượng câu hỏi trong mảng: $targetCount
- Schema: {"questions":[{"question":"...","options":["...","...","...","..."],"correctAnswerIndex":0,"explanation":"...","sourceSnippet":"...","sourcePageStart":1,"sourcePageEnd":1}]}
""".trim()
    }

    private fun buildEnglishDirectPrompt(segments: List<ContentSegment>, targetCount: Int): String {
        val segmentList = segments.mapIndexed { index, seg ->
            val snippet = seg.text.take(SEGMENT_SNIPPET_LIMIT)
            val pageLabel = if (seg.sourcePageStart != null) " [Page ${seg.sourcePageStart}]" else ""
            "[$index] $snippet$pageLabel"
        }.joinToString("\n\n")

        val templateHints = QuestionTemplateRotator.suggestTemplatesForBatch(0, targetCount, "en")
        val templateSection = if (templateHints.isNotEmpty()) {
            "\nTEMPLATE HINTS (rotate, do NOT repeat same template):\n${templateHints.joinToString("\n")}"
        } else ""

        return """
|Generate $targetCount multiple-choice quiz questions (4 options each) from the content below.

STRICT ANTI-PLAGIARISM RULES (MANDATORY):
1. PURE JSON ONLY — no markdown, no code fence, no explanation outside JSON
2. Response must start with { and end with } — nothing before or after
3. CORRECT ANSWER MUST NEVER COPY THE SOURCE VERBATIM
   - Correct answer must be SHORTER than the sourceSnippet (at least 30% shorter)
   - Correct answer must REWORD THE IDEA, not paste the sentence
   - WRONG: "Raster graphics is a type of digital image represented as a rectangular grid of pixels." (verbatim)
   - RIGHT: "A digital image format using a pixel grid" or "Image representation via rectangular pixel arrays"
4. DISTRACTORS must be on the SAME TOPIC, plausibly wrong
   - Distractors must share the same context as the question
   - Wrong due to minor inaccuracy, not complete nonsense
   - WRONG: "A type of food preparation method" (completely unrelated)
   - RIGHT: (if correct is "vector graphics") → "Bitmap raster format" (same domain, slightly wrong)
5. DO NOT paraphrase a source sentence verbatim as the correct answer
6. DO NOT create questions that only change a few words from the sourceSnippet
7. DO NOT use headings, table of contents, captions, or footnotes as sources
8. DO NOT create 2 questions with the same template in the same batch
9. DO NOT create "Which is..." or "What is..." questions with list-style options
10. Question diversity: vary types — summary, detail, function, property, implication, application

GOOD QUESTION STRUCTURE:
- Ask about SPECIFIC DETAILS from content (not "what is this passage about")
- Correct answer: concise, rewording the main point (DO NOT copy a long sentence verbatim)
- 3 Distractors: same topic, each wrong in a different way
- Explanation: 1-2 sentences, explain WHY the correct answer is right

CONTENT:
$segmentList
$templateSection

OUTPUT REQUIREMENTS (STRICT):
- Pure JSON starting from the very first character
- No ```json or ``` before/after
- Number of questions in array: $targetCount
- Schema: {"questions":[{"question":"...","options":["...","...","...","..."],"correctAnswerIndex":0,"explanation":"...","sourceSnippet":"...","sourcePageStart":1,"sourcePageEnd":1}]}
""".trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════

    fun maxSegmentsPerBatch(): Int = MAX_SEGMENTS_PER_BATCH
    fun optimalQuestionsPerBatch(): Int = OPTIMAL_QUESTIONS_PER_BATCH
}
