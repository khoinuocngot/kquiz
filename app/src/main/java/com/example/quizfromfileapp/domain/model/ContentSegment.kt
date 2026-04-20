package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Một đoạn nội dung đã được làm sạch và phân tích.
 * MỖI segment đều giữ PROVENANCE đầy đủ — truy vết về trang nguồn.
 *
 * @param id              ID duy nhất của segment
 * @param text            Nội dung văn bản của đoạn
 * @param type            Loại đoạn: PARAGRAPH hoặc SENTENCE
 * @param wordCount       Số từ trong đoạn
 * @param hasKnowledge    Đoạn có chứa thông tin kiến thức không
 * @param sourcePageStart Trang bắt đầu (1-based), null cho non-PDF
 * @param sourcePageEnd   Trang kết thúc (1-based), null cho non-PDF
 * @param sourceType      Loại nguồn: TEXT_LAYER / OCR / MERGED / UNKNOWN
 * @param sourceSnippet  Đoạn trích nguyên gốc từ nguồn (để debug/truy vết)
 */
@Serializable
data class ContentSegment(
    val id: String = UUID.randomUUID().toString(),

    val text: String,

    val type: String,

    val wordCount: Int,

    val hasKnowledge: Boolean = true,

    /** Trang bắt đầu (1-based), null cho non-PDF */
    val sourcePageStart: Int? = null,

    /** Trang kết thúc (1-based), null cho non-PDF */
    val sourcePageEnd: Int? = null,

    /** Loại nguồn: TEXT_LAYER / OCR / MERGED / UNKNOWN */
    val sourceType: String = SOURCE_TYPE_MERGED,

    /** Snippet gốc để truy vết (tối đa 200 ký tự) */
    val sourceSnippet: String = text.take(200)
) {
    companion object {
        const val TYPE_PARAGRAPH = "PARAGRAPH"
        const val TYPE_SENTENCE = "SENTENCE"

        const val SOURCE_TYPE_TEXT_LAYER = "TEXT_LAYER"
        const val SOURCE_TYPE_OCR = "OCR"
        const val SOURCE_TYPE_MERGED = "MERGED"
        const val SOURCE_TYPE_UNKNOWN = "UNKNOWN"
    }

    val sourcePageLabel: String
        get() = when {
            sourcePageStart != null && sourcePageEnd != null && sourcePageStart != sourcePageEnd ->
                "Trang $sourcePageStart–$sourcePageEnd"
            sourcePageStart != null -> "Trang $sourcePageStart"
            else -> "Nguồn không xác định"
        }
}
