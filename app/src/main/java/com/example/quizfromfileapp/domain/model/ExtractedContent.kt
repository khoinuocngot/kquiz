package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Kết quả trích xuất text từ file — PHIÊN BẢN MỚI.
 *
 * Tách RÕ RÀNG 3 loại text:
 * - rawText:  nội dung trích xuất THÔ, đầy đủ nhất có thể (trước khi clean)
 * - cleanedText: nội dung đã làm sạch để sinh quiz
 * - previewText: chỉ hiển thị UI (500 ký tự)
 *
 * Metadata trích xuất:
 * - totalPages / extractedPages / ocrPages
 * - rawCharCount / cleanedCharCount
 *
 * NGUYÊN TẮC:
 * - rawText KHÔNG BAO GIỜ bị ghi đè bởi cleanedText
 * - ContentCleaner chỉ tạo cleanedText từ rawText
 * - Quiz generator dùng cleanedText/segments, không dùng rawText trực tiếp
 */
@Serializable
data class ExtractedContent(
    val sourceUri: String,
    val fileName: String,
    val mimeType: String,

    /** Nội dung trích xuất THÔ — đầy đủ nhất, KHÔNG bị clean */
    val rawText: String,

    /** Nội dung đã làm sạch để sinh quiz */
    val cleanedText: String,

    /** Chỉ để hiển thị preview trong UI */
    val previewText: String,

    /** Tổng số ký tự raw */
    val rawCharCount: Int,

    /** Tổng số ký tự sau khi clean */
    val cleanedCharCount: Int,

    /** Tổng số trang (PDF) hoặc null */
    val totalPages: Int? = null,

    /** Số trang đã extract text (PDF) */
    val extractedPages: Int? = null,

    /** Số trang đã OCR (PDF scan) */
    val ocrPages: Int? = null,

    /** Kết quả làm sạch nâng cao — chứa segments cho quiz generator */
    val cleanedContent: CleanedContent? = null,

    /** Chi tiết trích xuất PDF (per-page text layer + OCR + merged) */
    val pdfExtractionResult: PdfExtractionResult? = null
) {
    companion object {
        private const val PREVIEW_LENGTH = 500

        /**
         * Tạo ExtractedContent từ RawExtractedContent.
         * rawText được giữ nguyên, cleanedText chỉ được tạo từ nó.
         */
        fun fromRaw(
            raw: RawExtractedContent,
            cleanedText: String,
            cleanedContent: CleanedContent?
        ): ExtractedContent {
            val preview = if (cleanedText.length <= PREVIEW_LENGTH) {
                cleanedText
            } else {
                cleanedText.take(PREVIEW_LENGTH) + "..."
            }
            return ExtractedContent(
                sourceUri = raw.sourceUri,
                fileName = raw.fileName,
                mimeType = raw.mimeType,
                rawText = raw.rawText,
                cleanedText = cleanedText,
                previewText = preview,
                rawCharCount = raw.rawCharCount,
                cleanedCharCount = cleanedText.length,
                totalPages = raw.totalPages,
                extractedPages = raw.extractedPages,
                ocrPages = raw.ocrPages,
                cleanedContent = cleanedContent,
                pdfExtractionResult = raw.pdfExtractionResult
            )
        }
    }

    val hasEnoughSegments: Boolean
        get() = (cleanedContent?.validSegmentCount ?: 0) >= 3

    val segmentCount: Int
        get() = cleanedContent?.validSegmentCount ?: 0

    val removedLineCount: Int
        get() = cleanedContent?.removedLineCount ?: 0

    /**
     * Trả về summary cho debug UI.
     */
    fun extractionSummary(): String {
        val parts = mutableListOf<String>()
        parts.add("$rawCharCount ký tự raw")
        parts.add("$cleanedCharCount ký tự cleaned")
        totalPages?.let { parts.add("$it trang") }
        extractedPages?.let { parts.add("$it trang extracted") }
        ocrPages?.let { if (it > 0) parts.add("$it trang OCR") }
        return parts.joinToString(" • ")
    }
}

/**
 * Kết quả trích xuất THÔ — trước khi clean.
 * Model này lưu toàn bộ metadata của quá trình trích xuất.
 *
 * KHÔNG chứa cleanedText — clean là bước riêng sau.
 */
@Serializable
data class RawExtractedContent(
    val sourceUri: String,
    val fileName: String,
    val mimeType: String,

    /** Nội dung trích xuất THÔ — đầy đủ, chưa clean */
    val rawText: String,

    /** Tổng số ký tự raw */
    val rawCharCount: Int,

    /** Tổng số trang (PDF) hoặc null nếu không phải PDF */
    val totalPages: Int? = null,

    /** Số trang đã extract text thành công (PDF text layer) */
    val extractedPages: Int? = null,

    /** Số trang đã OCR bổ sung (PDF scan / trang thiếu text) */
    val ocrPages: Int? = null,

    /** Danh sách số trang bị thiếu text (cần OCR) */
    val emptyPages: List<Int> = emptyList(),

    /** Log chi tiết từng trang (số ký tự mỗi trang) — để debug */
    val pageCharCounts: Map<Int, Int> = emptyMap(),

    /** Kết quả chi tiết trích xuất PDF (per-page results) */
    val pdfExtractionResult: com.example.quizfromfileapp.domain.model.PdfExtractionResult? = null
) {
    val isFullyExtracted: Boolean
        get() = emptyPages.isEmpty() && ocrPages == 0

    val extractionQualityNote: String
        get() = when {
            ocrPages != null && ocrPages > 0 ->
                "OCR bổ sung cho $ocrPages trang thiếu text"
            emptyPages.isNotEmpty() ->
                "${emptyPages.size} trang không có text (có thể là ảnh)"
            extractedPages != null && totalPages != null && extractedPages < totalPages ->
                "Chỉ extract được $extractedPages/$totalPages trang"
            else -> "Trích xuất đầy đủ"
        }
}
