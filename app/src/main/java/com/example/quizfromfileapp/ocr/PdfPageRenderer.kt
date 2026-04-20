package com.example.quizfromfileapp.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PdfPageRenderer"

/**
 * Render từng trang PDF thành Bitmap để OCR.
 *
 * Dùng PdfRenderer (API 21+) thay vì PdfBox render
 * vì PdfRenderer nhẹ hơn và không cần native library.
 *
 * Mỗi trang được render ở độ phân giải 2x để OCR chính xác hơn.
 */
class PdfPageRenderer(private val context: Context) {

    companion object {
        private const val RENDER_SCALE = 2 // render ở 2x resolution
        private const val MAX_PAGE_SIZE = 4096 // max dimension pixels
    }

    /**
     * Render một trang PDF thành Bitmap.
     *
     * @param uri URI của file PDF
     * @param pageIndex chỉ số trang (0-based)
     * @return Bitmap của trang đó
     */
    suspend fun renderPage(uri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var bitmap: Bitmap? = null

        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) {
                Log.e(TAG, "renderPage: openFileDescriptor trả về null cho page $pageIndex")
                return@withContext null
            }

            renderer = PdfRenderer(fileDescriptor)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                Log.w(TAG, "renderPage: pageIndex $pageIndex ngoài phạm vi (0-${renderer.pageCount - 1})")
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val width = (page.width * RENDER_SCALE).coerceAtMost(MAX_PAGE_SIZE)
            val height = (page.height * RENDER_SCALE).coerceAtMost(MAX_PAGE_SIZE)

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            Log.d(TAG, "renderPage: page $pageIndex rendered → ${bitmap.width}x${bitmap.height}")
            bitmap

        } catch (e: Exception) {
            Log.e(TAG, "renderPage: EXCEPTION page $pageIndex: ${e.message}", e)
            null
        } finally {
            try { renderer?.close() } catch (_: Exception) { }
            try { fileDescriptor?.close() } catch (_: Exception) { }
        }
    }

    /**
     * Render nhiều trang liên tiếp.
     *
     * @param uri URI của file PDF
     * @param pageIndices danh sách chỉ số trang
     * @return Map<pageIndex, Bitmap>
     */
    suspend fun renderPages(uri: Uri, pageIndices: List<Int>): Map<Int, Bitmap> =
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<Int, Bitmap>()
            for (index in pageIndices) {
                val bitmap = renderPage(uri, index)
                if (bitmap != null) {
                    results[index] = bitmap
                }
            }
            results
        }

    /**
     * Lấy số trang của PDF mà không cần render.
     */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) return@withContext 0
            renderer = PdfRenderer(fileDescriptor)
            renderer.pageCount
        } catch (e: Exception) {
            Log.e(TAG, "getPageCount: EXCEPTION: ${e.message}", e)
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) { }
            try { fileDescriptor?.close() } catch (_: Exception) { }
        }
    }

    /**
     * Render bitmap ra file tạm (để debug hoặc lưu).
     */
    suspend fun saveBitmapToTemp(bitmap: Bitmap, prefix: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                val file = File.createTempFile("${prefix}_", ".png", cacheDir)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                Log.d(TAG, "saveBitmapToTemp: ${file.absolutePath} (${file.length()} bytes)")
                file
            } catch (e: Exception) {
                Log.e(TAG, "saveBitmapToTemp: EXCEPTION: ${e.message}", e)
                null
            }
        }
}
