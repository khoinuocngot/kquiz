package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Kết quả làm sạch nội dung trước khi sinh quiz.
 * ĐÃ CÓ provenance trên mỗi segment.
 *
 * @param originalText     Text gốc sau khi trích xuất
 * @param cleanedText      Text đã làm sạch (loại heading rác, numbering, ký hiệu)
 * @param segments         Danh sách các đoạn có nghĩa, MỖI đoạn đều có sourcePage/sourceType
 * @param removedLineCount Số dòng đã bị loại bỏ (để debug / hiển thị)
 */
@Serializable
data class CleanedContent(
    val originalText: String,
    val cleanedText: String,
    val segments: List<ContentSegment>,
    val removedLineCount: Int
) {
    val validSegmentCount: Int get() = segments.size

    val hasEnoughContent: Boolean get() = segments.size >= 3

    /** Trả về segments cho quiz generator — có truy vết trang */
    val segmentsWithProvenance: List<ContentSegment>
        get() = segments

    /** Stats theo trang */
    fun segmentsByPage(): Map<Int, List<ContentSegment>> {
        return segments.groupBy { it.sourcePageStart ?: -1 }
    }

    /** Stats theo loại nguồn */
    fun segmentsBySourceType(): Map<String, List<ContentSegment>> {
        return segments.groupBy { it.sourceType }
    }
}
