package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Xoay vòng template câu hỏi để tránh lặp.
 *
 * Nguyên tắc:
 * - Giữ tối đa 2 câu cùng template trong 1 quiz
 * - Đa dạng hóa: summary, detail, implication, function, property, comparison
 * - Template rotation thông minh: chọn template phù hợp với content type
 *
 * Dùng trong LLM prompt để gợi ý model xoay vòng,
 * và trong post-processing để detect lặp template.
 */
object QuestionTemplateRotator {

    private const val TAG = "QuestionTemplateRotator"

    /**
     * Template tiếng Anh — đa dạng hóa theo content type.
     */
    val EN_TEMPLATES = listOf(
        // Summary / Main idea
        QuestionTemplate(
            id = "en_main_idea",
            label = "Main idea",
            questionHint = "What is the main idea of this passage?",
            example = "What is the primary concept described in the passage about {topic}?",
            preferredFor = setOf("definition", "concept", "overview", "introduction")
        ),
        QuestionTemplate(
            id = "en_summarize",
            label = "Summarize",
            questionHint = "Which statement best summarizes the idea?",
            example = "Which of the following best summarizes the information about {topic}?",
            preferredFor = setOf("definition", "overview", "summary")
        ),
        // Detail / Factual
        QuestionTemplate(
            id = "en_detail",
            label = "Detail",
            questionHint = "What is true according to the passage?",
            example = "According to the passage, what is true about {topic}?",
            preferredFor = setOf("detail", "fact", "property", "characteristic")
        ),
        QuestionTemplate(
            id = "en_function",
            label = "Function/Role",
            questionHint = "What does the passage say about the function?",
            example = "What does the passage indicate about the function of {topic}?",
            preferredFor = setOf("function", "role", "purpose", "usage", "mechanism")
        ),
        // Implication / Inference
        QuestionTemplate(
            id = "en_implication",
            label = "Implication",
            questionHint = "What does the passage imply about...?",
            example = "What does the passage imply about the nature of {topic}?",
            preferredFor = setOf("implication", "inference", "conclusion", "theory")
        ),
        QuestionTemplate(
            id = "en_accuracy",
            label = "Accuracy",
            questionHint = "Which description is most accurate?",
            example = "Which description most accurately reflects the concept of {topic}?",
            preferredFor = setOf("definition", "comparison", "contrast")
        ),
        // Comparison / Distinction
        QuestionTemplate(
            id = "en_distinction",
            label = "Distinction",
            questionHint = "What distinguishes X from Y?",
            example = "What distinguishes {topic} from other similar concepts?",
            preferredFor = setOf("comparison", "contrast", "distinction", "difference")
        ),
        // Process / Mechanism
        QuestionTemplate(
            id = "en_process",
            label = "Process",
            questionHint = "How does X work?",
            example = "How does {topic} operate according to the passage?",
            preferredFor = setOf("process", "mechanism", "workflow", "algorithm", "procedure")
        ),
        // Property / Attribute
        QuestionTemplate(
            id = "en_property",
            label = "Property",
            questionHint = "Which statement is correct about the property?",
            example = "Which of the following correctly describes the properties of {topic}?",
            preferredFor = setOf("property", "attribute", "characteristic", "feature")
        ),
        // Application
        QuestionTemplate(
            id = "en_application",
            label = "Application",
            questionHint = "What reflects the concept described?",
            example = "Which of the following best reflects the concept of {topic}?",
            preferredFor = setOf("example", "application", "instance", "use case")
        )
    )

    /**
     * Template tiếng Việt — đa dạng hóa theo content type.
     */
    val VI_TEMPLATES = listOf(
        // Summary / Main idea
        QuestionTemplate(
            id = "vi_main_idea",
            label = "Ý chính",
            questionHint = "Đâu là ý chính của đoạn văn?",
            example = "Đâu là ý chính được nêu trong phần về {topic}?",
            preferredFor = setOf("định nghĩa", "khái niệm", "tổng quan", "giới thiệu")
        ),
        QuestionTemplate(
            id = "vi_summarize",
            label = "Tóm tắt",
            questionHint = "Phát biểu nào tóm tắt đúng ý?",
            example = "Phát biểu nào dưới đây tóm tắt đúng thông tin về {topic}?",
            preferredFor = setOf("định nghĩa", "tổng quan", "tóm tắt")
        ),
        // Detail / Factual
        QuestionTemplate(
            id = "vi_detail",
            label = "Chi tiết",
            questionHint = "Điều gì đúng theo nội dung?",
            example = "Theo nội dung, điều nào đúng về {topic}?",
            preferredFor = setOf("chi tiết", "sự thật", "tính chất", "đặc điểm")
        ),
        QuestionTemplate(
            id = "vi_function",
            label = "Chức năng",
            questionHint = "Đoạn văn nói gì về chức năng?",
            example = "Đoạn văn cho biết chức năng của {topic} là gì?",
            preferredFor = setOf("chức năng", "vai trò", "mục đích", "cách dùng", "cơ chế")
        ),
        // Implication / Inference
        QuestionTemplate(
            id = "vi_implication",
            label = "Hàm ý",
            questionHint = "Đoạn văn ngụ ý gì về...?",
            example = "Đoạn văn ngụ ý điều gì về bản chất của {topic}?",
            preferredFor = setOf("hàm ý", "suy luận", "kết luận", "lý thuyết")
        ),
        QuestionTemplate(
            id = "vi_accuracy",
            label = "Độ chính xác",
            questionHint = "Mô tả nào chính xác nhất?",
            example = "Mô tả nào phản ánh chính xác nhất khái niệm {topic}?",
            preferredFor = setOf("định nghĩa", "so sánh", "đối lập")
        ),
        // Comparison / Distinction
        QuestionTemplate(
            id = "vi_distinction",
            label = "Phân biệt",
            questionHint = "Điều gì phân biệt X với Y?",
            example = "Đâu là điểm khác biệt chính của {topic} so với các khái niệm khác?",
            preferredFor = setOf("so sánh", "đối lập", "phân biệt", "khác nhau")
        ),
        // Process / Mechanism
        QuestionTemplate(
            id = "vi_process",
            label = "Quy trình",
            questionHint = "Cách thức hoạt động như thế nào?",
            example = "Theo đoạn văn, {topic} hoạt động theo cơ chế nào?",
            preferredFor = setOf("quy trình", "cơ chế", "luồng công việc", "thuật toán", "thủ tục")
        ),
        // Property / Attribute
        QuestionTemplate(
            id = "vi_property",
            label = "Thuộc tính",
            questionHint = "Phát biểu nào đúng về thuộc tính?",
            example = "Phát biểu nào mô tả đúng các thuộc tính của {topic}?",
            preferredFor = setOf("thuộc tính", "đặc điểm", "tính chất", "đặc trưng")
        ),
        // Application
        QuestionTemplate(
            id = "vi_application",
            label = "Ứng dụng",
            questionHint = "Ví dụ nào phản ánh khái niệm?",
            example = "Ví dụ nào dưới đây phản ánh đúng khái niệm {topic}?",
            preferredFor = setOf("ví dụ", "ứng dụng", "trường hợp", "sử dụng")
        )
    )

    // ─────────────────────────────────────────────────────────────
    // Template selection
    // ─────────────────────────────────────────────────────────────

    /**
     * Chọn template cho câu hỏi tiếp theo.
     *
     * @param language        "vi" hoặc "en"
     * @param segment         Segment nguồn (để chọn template phù hợp content type)
     * @param usedTemplateIds Template IDs đã dùng (để tránh lặp)
     * @return Template phù hợp
     */
    fun selectTemplate(
        language: String,
        segment: String,
        usedTemplateIds: Set<String>
    ): QuestionTemplate {
        val templates = if (language == "vi") VI_TEMPLATES else EN_TEMPLATES

        // Tìm template phù hợp với content type
        val contentType = detectContentType(segment)
        val suitable = templates.filter { t ->
            t.preferredFor.any { contentType.contains(it, ignoreCase = true) }
        }

        // Ưu tiên template chưa dùng, phù hợp content type
        val unusedSuitable = suitable.filter { it.id !in usedTemplateIds }
        if (unusedSuitable.isNotEmpty()) {
            return unusedSuitable.random()
        }

        // Thử template chưa dùng bất kỳ
        val unused = templates.filter { it.id !in usedTemplateIds }
        if (unused.isNotEmpty()) {
            return unused.random()
        }

        // Đã dùng tất cả → lấy ngẫu nhiên (quay vòng)
        return templates.random()
    }

    /**
     * Chọn template cho LLM prompt — gợi ý đa dạng hóa.
     *
     * @param batchIndex     Số thứ tự batch (0, 1, 2...)
     * @param batchSize      Số câu mỗi batch
     * @param language       "vi" hoặc "en"
     * @return Gợi ý template cho batch này
     */
    fun suggestTemplatesForBatch(
        batchIndex: Int,
        batchSize: Int,
        language: String
    ): List<String> {
        val templates = if (language == "vi") VI_TEMPLATES else EN_TEMPLATES
        val suggestions = mutableListOf<String>()

        // Xác định content type gợi ý dựa trên batch index
        val suggestedTypes = when (batchIndex % 5) {
            0 -> listOf("definition", "concept", "định nghĩa", "khái niệm")
            1 -> listOf("function", "role", "purpose", "chức năng", "vai trò")
            2 -> listOf("property", "characteristic", "feature", "thuộc tính", "đặc điểm")
            3 -> listOf("detail", "fact", "specific", "chi tiết", "sự thật")
            else -> listOf("process", "mechanism", "workflow", "quy trình", "cơ chế")
        }

        for (i in 0 until batchSize) {
            val type = suggestedTypes[i % suggestedTypes.size]
            val matching = templates.find { t ->
                t.preferredFor.any { it.equals(type, ignoreCase = true) }
            }
            if (matching != null) {
                suggestions.add("${i + 1}. ${matching.example.replace("{topic}", "nội dung được cung cấp")}")
            }
        }

        return suggestions
    }

    /**
     * Detect content type từ segment text.
     */
    private fun detectContentType(segment: String): String {
        val lower = segment.lowercase()

        return when {
            lower.contains("definition") || lower.contains("defined as") ||
            lower.contains("means") || lower.contains("refers to") ||
            lower.contains("định nghĩa") || lower.contains("là gì") ||
            lower.contains("được hiểu là") -> "definition"

            lower.contains("function") || lower.contains("used for") ||
            lower.contains("purpose") || lower.contains("role") ||
            lower.contains("chức năng") || lower.contains("vai trò") ||
            lower.contains("dùng để") || lower.contains("mục đích") -> "function"

            lower.contains("process") || lower.contains("how it works") ||
            lower.contains("algorithm") || lower.contains("step") ||
            lower.contains("quy trình") || lower.contains("cơ chế") ||
            lower.contains("hoạt động") || lower.contains("các bước") -> "process"

            lower.contains("property") || lower.contains("characteristic") ||
            lower.contains("feature") || lower.contains("attribute") ||
            lower.contains("thuộc tính") || lower.contains("đặc điểm") ||
            lower.contains("tính chất") -> "property"

            lower.contains("compare") || lower.contains("different from") ||
            lower.contains("versus") || lower.contains("unlike") ||
            lower.contains("so sánh") || lower.contains("khác với") ||
            lower.contains("đối lập") -> "comparison"

            lower.contains("example") || lower.contains("for instance") ||
            lower.contains("such as") || lower.contains("ví dụ") ||
            lower.contains("chẳng hạn") -> "example"

            else -> "general"
        }
    }

    /**
     * Đếm số câu dùng cùng template trong quiz.
     */
    fun countTemplateUsage(
        questions: List<Pair<String, String>>,
        templateId: String
    ): Int {
        return questions.count { it.second == templateId }
    }

    /**
     * Kiểm tra xem có quá nhiều câu cùng template không.
     * Cho phép tối đa 2 câu cùng template.
     */
    fun hasTooManySameTemplate(
        questions: List<Pair<String, String>>,
        templateId: String,
        maxAllowed: Int = 2
    ): Boolean {
        return countTemplateUsage(questions, templateId) >= maxAllowed
    }
}

/**
 * Một template câu hỏi.
 *
 * @param id             ID template duy nhất
 * @param label          Nhãn ngắn (hiển thị trong debug)
 * @param questionHint   Gợi ý dạng câu hỏi (không chứa topic)
 * @param example        Ví dụ đầy đủ (chứa {topic} placeholder)
 * @param preferredFor   Content types phù hợp với template này
 */
data class QuestionTemplate(
    val id: String,
    val label: String,
    val questionHint: String,
    val example: String,
    val preferredFor: Set<String>
)
