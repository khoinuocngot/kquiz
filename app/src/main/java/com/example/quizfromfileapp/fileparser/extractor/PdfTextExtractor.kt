package com.example.quizfromfileapp.fileparser.extractor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.PdfExtractionResult
import com.example.quizfromfileapp.domain.model.PdfPageResult
import com.example.quizfromfileapp.domain.model.RawExtractedContent
import com.example.quizfromfileapp.ocr.MlKitOcrEngine
import com.example.quizfromfileapp.ocr.PdfPageRenderer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "PdfTextExtractor"

private const val EMPTY_PAGE_THRESHOLD = 50

private const val SIMILARITY_THRESHOLD = 0.75

/**
 * Extractor cho PDF — xử lý TỪNG TRANG với 2 nguồn text:
 * 1. Text layer: đọc bằng PDFBox
 * 2. OCR: render page bằng PdfRenderer → ML Kit
 *
 * Sau đó merge 2 nguồn, tránh duplicate.
 *
 * Luồng:
 * 1. Đọc text layer từng trang bằng PDFBox
 * 2. Render + OCR tất cả các trang (OCR_ALL_PAGES = true)
 * 3. Merge per-page: textLayer + OCR (normalize + similarity check)
 * 4. Ghép tất cả merged text thành rawText
 * 5. Trả RawExtractedContent + PdfExtractionResult
 */
class PdfTextExtractor(private val context: Context) : FileTextExtractor {

    private val pageRenderer by lazy { PdfPageRenderer(context) }
    private val ocrEngine by lazy { MlKitOcrEngine(context) }

    override fun canHandle(mimeType: String): Boolean = mimeType == "application/pdf"

    override suspend fun extract(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): Result<ExtractedContent> = withContext(Dispatchers.IO) {
        Log.d(TAG, "extract: START uri=$uri fileName=$fileName")

        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            Log.e(TAG, "extract: PDFBoxResourceLoader.init thất bại: ${e.message}")
            return@withContext Result.failure(
                IllegalStateException("Không thể khởi tạo thư viện đọc PDF: ${e.message}")
            )
        }

        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    IllegalStateException("Không thể mở file PDF"))

            document = PDDocument.load(inputStream).also {
                Log.d(TAG, "extract: PDDocument loaded, pages=${it.numberOfPages}")
            }
            inputStream.close()

            val totalPages = document.numberOfPages
            Log.d(TAG, "extract: totalPages=$totalPages")

            // ── Bước 1: Đọc text layer từng trang ──────────────────
            Log.d(TAG, "extract: === Bước 1: Đọc text layer từng trang ===")
            val textLayerByPage = readTextLayerPerPage(document)
            val textLayerPagesCount = textLayerByPage.values.count { it.isNotBlank() }
            val textLayerTotalChars = textLayerByPage.values.sumOf { it.length }
            Log.d(TAG, "extract: text layer → $textLayerPagesCount trang có nội dung, " +
                    "tổng $textLayerTotalChars ký tự")

            // ── Bước 2: Render + OCR tất cả trang ──────────────────
            Log.d(TAG, "extract: === Bước 2: Render + OCR tất cả $totalPages trang ===")
            val ocrByPage = ocrAllPages(uri, totalPages)
            val ocrPagesCount = ocrByPage.values.count { it.isNotBlank() }
            val ocrTotalChars = ocrByPage.values.sumOf { it.length }
            Log.d(TAG, "extract: OCR → $ocrPagesCount trang có kết quả, " +
                    "tổng $ocrTotalChars ký tự")

            // ── Bước 3: Merge per-page ────────────────────────────
            Log.d(TAG, "extract: === Bước 3: Merge text layer + OCR từng trang ===")
            val mergedByPage = mutableMapOf<Int, String>()

            for (pageIdx in 1..totalPages) {
                val textLayer = textLayerByPage[pageIdx] ?: ""
                val ocrText = ocrByPage[pageIdx] ?: ""

                val merged = mergeTexts(textLayer, ocrText)
                mergedByPage[pageIdx] = merged

                val layerLen = textLayer.length
                val ocrLen = ocrText.length
                val mergedLen = merged.length
                val diff = mergedLen - maxOf(layerLen, ocrLen)

                Log.d(TAG, "extract: trang $pageIdx → " +
                        "layer=$layerLen | ocr=$ocrLen | merged=$mergedLen " +
                        "(diff=+$diff)")
            }

            // ── Bước 4: Ghép tất cả thành raw text ────────────────
            val fullTextLayer = buildFullText(textLayerByPage, totalPages)
            val fullOcrText = buildFullText(ocrByPage, totalPages)
            val fullMergedText = buildFullText(mergedByPage, totalPages)

            // ── NEW: Build fullMergedTextBlock (không [PAGE N], join "\n\n", bỏ trang rỗng) ──
            val fullMergedTextBlock = buildFullMergedTextBlock(mergedByPage, totalPages)
            val fullMergedTextBlockCharCount = fullMergedTextBlock.length

            // Clean bản block (dùng ContentCleaner thuần, không extract segment)
            val fullMergedTextBlockCleaned = try {
                com.example.quizfromfileapp.domain.model.ContentCleaner.cleanTextOnly(fullMergedTextBlock)
            } catch (e: Exception) {
                Log.w(TAG, "buildFullBlock: cleanTextOnly thất bại, dùng raw block: ${e.message}")
                fullMergedTextBlock
            }
            val fullMergedTextBlockCleanedCharCount = fullMergedTextBlockCleaned.length
            val fullMergedTextBlockCleanedPreview = if (fullMergedTextBlockCleaned.length <= 500) {
                fullMergedTextBlockCleaned
            } else {
                fullMergedTextBlockCleaned.take(500) + "..."
            }

            Log.d(TAG, "extract: === Kết quả tổng hợp ===")
            Log.d(TAG, "extract: fullTextLayer              = ${fullTextLayer.length} ký tự")
            Log.d(TAG, "extract: fullOcrText                = ${fullOcrText.length} ký tự")
            Log.d(TAG, "extract: fullMergedText             = ${fullMergedText.length} ký tự")
            Log.d(TAG, "extract: fullMergedTextBlock        = ${fullMergedTextBlockCharCount} ký tự")
            Log.d(TAG, "extract: fullMergedTextBlockCleaned = ${fullMergedTextBlockCleanedCharCount} ký tự")
            Log.d(TAG, "extract: textLayerPages  = $textLayerPagesCount")
            Log.d(TAG, "extract: ocrPages        = $ocrPagesCount")

            if (fullMergedText.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Không trích xuất được nội dung từ PDF này. " +
                            "File có thể chỉ chứa ảnh không đọc được.")
                )
            }

            // ── Tạo PdfExtractionResult ──────────────────────────
            val pageResults = (1..totalPages).map { pageIdx ->
                PdfPageResult(
                    pageIndex = pageIdx,
                    textLayerText = textLayerByPage[pageIdx] ?: "",
                    ocrText = ocrByPage[pageIdx] ?: "",
                    mergedText = mergedByPage[pageIdx] ?: "",
                    textLayerCharCount = (textLayerByPage[pageIdx] ?: "").length,
                    ocrCharCount = (ocrByPage[pageIdx] ?: "").length,
                    mergedCharCount = (mergedByPage[pageIdx] ?: "").length
                )
            }

            val pdfResult = PdfExtractionResult(
                sourceUri = uri.toString(),
                fileName = fileName,
                totalPages = totalPages,
                pages = pageResults,
                fullTextLayer = fullTextLayer,
                fullOcrText = fullOcrText,
                fullMergedText = fullMergedText,
                textLayerTotalChars = textLayerTotalChars,
                ocrTotalChars = ocrTotalChars,
                mergedTotalChars = fullMergedText.length,
                pagesWithTextLayer = textLayerPagesCount,
                pagesWithOcr = ocrPagesCount,
                pagesNeedingMerge = pageResults.count { it.hasTextLayer && it.hasOcr },
                // ── NEW: full merged block ──
                fullMergedTextBlock = fullMergedTextBlock,
                fullMergedTextBlockCleaned = fullMergedTextBlockCleaned,
                fullMergedTextBlockCharCount = fullMergedTextBlockCharCount,
                fullMergedTextBlockCleanedCharCount = fullMergedTextBlockCleanedCharCount,
                fullMergedTextBlockCleanedPreview = fullMergedTextBlockCleanedPreview
            )

            // ── Bước 5: Clean merged text VỚI PROVENANCE ──────────
            Log.d(TAG, "extract: === Bước 5: Clean VỚI provenance per-page ===")
            val cleanedResult = try {
                com.example.quizfromfileapp.domain.model.ContentCleaner.cleanFromPdfPages(
                    pageResults,
                    fullMergedTextBlockCleaned = fullMergedTextBlockCleaned
                )
            } catch (e: Exception) {
                Log.e(TAG, "extract: ContentCleaner.cleanFromPdfPages thất bại: ${e.message}", e)
                return@withContext Result.failure(
                    IllegalStateException("Lỗi làm sạch nội dung: ${e.message}"))
            }

            // ── Tạo RawExtractedContent ────────────────────────────
            val raw = RawExtractedContent(
                sourceUri = uri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                // Dùng fullMergedTextBlock — block lớn không truncate, không [PAGE N]
                // Đảm bảo cả primary path (cleanedContent.segments) và
                // fallback path (extractFallbackSegments) đều dùng nội dung đầy đủ
                rawText = fullMergedTextBlock,
                rawCharCount = fullMergedTextBlockCharCount,
                totalPages = totalPages,
                extractedPages = textLayerPagesCount,
                ocrPages = ocrPagesCount,
                emptyPages = emptyList(),
                pageCharCounts = pageResults.associate { it.pageIndex to it.mergedCharCount },
                // Extra: lưu pdf extraction result để UI dùng
                pdfExtractionResult = pdfResult
            )

            val content = ExtractedContent.fromRaw(raw, cleanedResult.cleanedText, cleanedResult)

            Log.d(TAG, "extract: SUCCESS ✅")
            Log.d(TAG, "extract:   rawCharCount     = ${content.rawCharCount}")
            Log.d(TAG, "extract:   cleanedCharCount = ${content.cleanedCharCount}")
            Log.d(TAG, "extract:   segmentCount    = ${content.segmentCount}")
            Log.d(TAG, "extract:   pagesWithText   = ${pdfResult.pagesWithTextLayer}")
            Log.d(TAG, "extract:   pagesWithOcr    = ${pdfResult.pagesWithOcr}")

            Result.success(content)

        } catch (e: Exception) {
            Log.e(TAG, "extract: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(IllegalStateException("Lỗi khi đọc PDF: ${e.message}"))
        } finally {
            try { document?.close() } catch (_: Exception) { }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  BƯỚC 1: Đọc text layer từng trang bằng PDFBox
    // ═══════════════════════════════════════════════════════════════

    private fun readTextLayerPerPage(document: PDDocument): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val stripper = PDFTextStripper()
        stripper.sortByPosition = true

        val totalPages = document.numberOfPages
        for (pageIdx in 1..totalPages) {
            try {
                stripper.startPage = pageIdx
                stripper.endPage = pageIdx
                val text = stripper.getText(document).trim()
                result[pageIdx] = text
                Log.d(TAG, "readTextLayer: trang $pageIdx → ${text.length} ký tự")
            } catch (e: Exception) {
                Log.w(TAG, "readTextLayer: trang $pageIdx lỗi: ${e.message}")
                result[pageIdx] = ""
            }
        }
        return result
    }

    // ═══════════════════════════════════════════════════════════════
    //  BƯỚC 2: Render + OCR tất cả các trang
    // ═══════════════════════════════════════════════════════════════

    private suspend fun ocrAllPages(uri: Uri, totalPages: Int): Map<Int, String> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<Int, String>()

            // Xử lý tuần tự từng trang (PdfRenderer không thread-safe)
            for (pageIdx in 1..totalPages) {
                val bitmap = pageRenderer.renderPage(uri, pageIdx - 1)
                if (bitmap == null) {
                    result[pageIdx] = ""
                    continue
                }

                val ocrText = try {
                    val ocrResult = ocrEngine.recognizeTextFromBitmap(bitmap)
                    ocrResult.getOrNull()?.trim() ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "ocrAllPages: trang $pageIdx OCR lỗi: ${e.message}")
                    ""
                } finally {
                    try { bitmap.recycle() } catch (_: Exception) { }
                }

                result[pageIdx] = ocrText
                Log.d(TAG, "ocrAllPages: trang $pageIdx → ${ocrText.length} ký tự OCR")
            }

            result
        }

    // ═══════════════════════════════════════════════════════════════
    //  BƯỚC 3: Merge text layer + OCR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Merge text layer và OCR text của cùng 1 trang.
     *
     * Chiến lược:
     * - Nếu cả 2 đều rỗng → trả ""
     * - Nếu 1 có, 1 rỗng → trả cái có
     * - Nếu cả 2 đều có:
     *   1. Normalize cả 2
     *   2. Tính similarity
     *   3. Nếu similarity ≥ ngưỡng → text layer đủ, chỉ thêm phần OCR khác biệt
     *   4. Nếu similarity < ngưỡng → ghép cả 2 (text layer trước, OCR sau)
     */
    private fun mergeTexts(textLayer: String, ocrText: String): String {
        val layer = textLayer.trim()
        val ocr = ocrText.trim()

        // Trường hợp cơ bản
        when {
            layer.isBlank() && ocr.isBlank() -> return ""
            layer.isBlank() -> return ocr
            ocr.isBlank() -> return layer
        }

        // Normalize để so sánh
        val normLayer = normalizeForComparison(layer)
        val normOcr = normalizeForComparison(ocr)

        // Tính similarity
        val similarity = computeSimilarity(normLayer, normOcr)

        Log.v(TAG, "mergeTexts: similarity=${"%.2f".format(similarity)}, " +
                "layer=${layer.length}, ocr=${ocr.length}")

        return if (similarity >= SIMILARITY_THRESHOLD) {
            // Text layer đủ rồi, chỉ bổ sung phần OCR không trùng
            val uniqueOcr = extractUniqueContent(ocr, layer)
            if (uniqueOcr.isBlank()) {
                layer
            } else {
                "$layer\n$uniqueOcr"
            }
        } else {
            // Khác nhau nhiều → ghép cả 2
            if (layer.length >= ocr.length) {
                "$layer\n$ocr"
            } else {
                "$ocr\n$layer"
            }
        }
    }

    /**
     * Normalize text để so sánh similarity.
     * - Lowercase
     * - Bỏ dấu câu, ký hiệu
     * - Normalize khoảng trắng
     */
    private fun normalizeForComparison(text: String): String {
        return text
            .lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Tính Jaccard similarity giữa 2 text đã normalize.
     */
    private fun computeSimilarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0

        val wordsA = a.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
        val wordsB = b.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()

        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0

        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size

        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    /**
     * Trích phần OCR không bị trùng với text layer.
     * Dùng sentence-level comparison.
     */
    private fun extractUniqueContent(ocrText: String, textLayer: String): String {
        val layerLower = textLayer.lowercase()
        val ocrSentences = ocrText
            .split(Regex("""[.!?\n]"""))
            .map { it.trim() }
            .filter { it.length > 15 } // Chỉ giữ câu có nghĩa

        val uniqueSentences = ocrSentences.filter { sentence ->
            val sentenceLower = sentence.lowercase()
            // Bỏ câu trùng hoàn toàn
            !layerLower.contains(sentenceLower) &&
                    // Bỏ câu gần trùng (similarity cao)
                    computeSimilarity(
                        normalizeForComparison(sentence),
                        normalizeForComparison(textLayer)
                    ) < SIMILARITY_THRESHOLD
        }

        return uniqueSentences.joinToString(". ").let {
            if (it.length > ocrText.length / 2) it else ""
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  BƯỚC 4: Ghép text từng trang thành full text
    // ═══════════════════════════════════════════════════════════════

    private fun buildFullText(textByPage: Map<Int, String>, totalPages: Int): String {
        val sb = StringBuilder()
        for (pageIdx in 1..totalPages) {
            val text = textByPage[pageIdx] ?: ""
            if (text.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append("[PAGE $pageIdx]\n")
                sb.append(text)
            }
        }
        return sb.toString()
    }

    /**
     * Ghép mergedText của TẤT CẢ trang thành MỘT block lớn.
     * - Lấy pages sorted theo pageIndex
     * - Lấy mergedText từng page
     * - Bỏ trang rỗng
     * - Join bằng "\n\n"
     * - Không truncate, không thêm marker
     */
    private fun buildFullMergedTextBlock(
        mergedByPage: Map<Int, String>,
        totalPages: Int
    ): String {
        val nonEmptyPages = (1..totalPages)
            .filter { pageIdx -> (mergedByPage[pageIdx] ?: "").isNotBlank() }
            .sorted()

        return nonEmptyPages
            .joinToString("\n\n") { pageIdx -> mergedByPage[pageIdx] ?: "" }
    }
}
