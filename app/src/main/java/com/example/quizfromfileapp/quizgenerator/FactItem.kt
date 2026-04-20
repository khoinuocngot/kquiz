package com.example.quizfromfileapp.quizgenerator

/**
 * Một atomic fact được trích xuất từ source segment.
 *
 * Thay vì dùng nguyên segment làm nguồn cho MCQ,
 * ta trích fact trước — đây là đơn vị kiến thức nhỏ nhất
 * có thể hỏi được.
 *
 * Fact phải:
 * - Là một mệnh đề kiến thức rõ ràng
 * - Không phải example/exercise/instruction/formula
 * - Có concept xác định
 * - Có confidence để lọc
 *
 * @param id              ID duy nhất
 * @param concept         Chủ đề/concept chính (VD: "raster graphics", "machine learning")
 * @param factStatement   Mệnh đề kiến thức ngắn, rõ (1-2 câu)
 * @param sourceSnippet   Đoạn nguồn gốc (để truy vết)
 * @param sourcePageStart Trang bắt đầu
 * @param sourcePageEnd   Trang kết thúc
 * @param sourceType      Loại nguồn: TEXT_LAYER / OCR / MERGED
 * @param confidence       Độ tin cậy 0.0 → 1.0 (FactQualityScorer tính)
 */
data class FactItem(
    val id: String = java.util.UUID.randomUUID().toString(),

    /** Chủ đề/concept chính của fact (VD: "raster graphics", "gradient descent") */
    val concept: String,

    /** Mệnh đề kiến thức ngắn, rõ, là 1 atomic fact */
    val factStatement: String,

    /** Snippet nguồn gốc để truy vết */
    val sourceSnippet: String,

    /** Trang bắt đầu (1-based), null cho non-PDF */
    val sourcePageStart: Int? = null,

    /** Trang kết thúc (1-based), null cho non-PDF */
    val sourcePageEnd: Int? = null,

    /** Loại nguồn: TEXT_LAYER / OCR / MERGED */
    val sourceType: String = "MERGED",

    /** Độ tin cậy 0.0 → 1.0 */
    val confidence: Float = 0.5f
) {
    val sourcePageLabel: String
        get() = when {
            sourcePageStart != null && sourcePageEnd != null && sourcePageStart != sourcePageEnd ->
                "Trang $sourcePageStart–$sourcePageEnd"
            sourcePageStart != null -> "Trang $sourcePageStart"
            else -> "Nguồn không xác định"
        }
}
