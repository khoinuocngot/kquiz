package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Kết quả trích xuất của MỘT trang PDF.
 * Mỗi trang có 2 nguồn text: text layer + OCR.
 */
@Serializable
data class PdfPageResult(
    val pageIndex: Int,
    val textLayerText: String,
    val ocrText: String,
    val mergedText: String,
    val textLayerCharCount: Int,
    val ocrCharCount: Int,
    val mergedCharCount: Int,
    val previewSnippet: String = ""
) {
    val hasTextLayer: Boolean get() = textLayerCharCount > 0
    val hasOcr: Boolean get() = ocrCharCount > 0
    val hasBoth: Boolean get() = hasTextLayer && hasOcr
    val isEmpty: Boolean get() = textLayerCharCount == 0 && ocrCharCount == 0

    val ocrConfidence: OcrConfidence
        get() = when {
            !hasOcr -> OcrConfidence.NONE
            hasTextLayer -> when {
                ocrCharCount > textLayerCharCount -> OcrConfidence.HIGH
                ocrCharCount > textLayerCharCount / 2 -> OcrConfidence.MEDIUM
                else -> OcrConfidence.LOW
            }
            else -> when {
                ocrCharCount > 200 -> OcrConfidence.HIGH
                ocrCharCount > 50 -> OcrConfidence.MEDIUM
                else -> OcrConfidence.LOW
            }
        }
}

/** Độ tự tin của OCR trên một trang */
@Serializable
enum class OcrConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

/**
 * Kết quả trích xuất toàn bộ PDF.
 *
 * Chứa 3 loại text tổng hợp:
 * - fullTextLayer / fullOcrText / fullMergedText: concat có [PAGE N] markers (dùng debug / display)
 * - fullMergedTextBlock: concat KHÔNG marker — BẢN THÔ, dùng cho quiz generator
 * - fullMergedTextBlockCleaned: concat đã clean — BẢN SẠCH, ưu tiên cho quiz generator
 *
 * NGUYÊN TẮC:
 * - fullMergedTextBlock KHÔNG bị truncate, giữ nguyên 100% merged text
 * - fullMergedTextBlockCleaned là bản clean đầy đủ, dùng trước cho generator
 * - segments vẫn giữ sourcePageStart/sourcePageEnd để truy vết nguồn
 */
@Serializable
data class PdfExtractionResult(
    val sourceUri: String,
    val fileName: String,
    val totalPages: Int,
    val pages: List<PdfPageResult>,

    // 3 loại text cũ (giữ nguyên, có [PAGE N] markers — dùng debug/display)
    val fullTextLayer: String,
    val fullOcrText: String,
    val fullMergedText: String,

    // Char counts
    val textLayerTotalChars: Int,
    val ocrTotalChars: Int,
    val mergedTotalChars: Int,
    val pagesWithTextLayer: Int,
    val pagesWithOcr: Int,
    val pagesNeedingMerge: Int,

    // ── NEW: full merged block (dùng cho quiz generator) ──
    /** Toàn bộ mergedText ghép thành 1 block — BẢN THÔ, không clean, không truncate, không [PAGE N] */
    val fullMergedTextBlock: String,

    /** Toàn bộ mergedText đã clean ghép thành 1 block — BẢN SẠCH, dùng cho quiz generator */
    val fullMergedTextBlockCleaned: String,

    /** Char count của fullMergedTextBlock */
    val fullMergedTextBlockCharCount: Int,

    /** Char count của fullMergedTextBlockCleaned */
    val fullMergedTextBlockCleanedCharCount: Int,

    /** Preview của fullMergedTextBlockCleaned (500 ký tự đầu) */
    val fullMergedTextBlockCleanedPreview: String = ""
) {
    val pagesWithBoth: Int get() = pages.count { it.hasBoth }
    val pagesWithNeither: Int get() = pages.count { it.isEmpty }
}
