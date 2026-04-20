package com.example.quizfromfileapp.data.helper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.quizfromfileapp.data.model.ImportedFile

private const val TAG = "FileMetadataHelper"

/**
 * Helper đọc metadata (tên, MIME type, kích thước) từ Uri mà không cần quyền đọc file.
 */
object FileMetadataHelper {

    private val SUPPORTED_MIME_TYPES = setOf(
        "text/plain",
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/jpg",
        "image/webp"
    )

    /**
     * Kiểm tra MIME type có được hỗ trợ không.
     */
    fun isSupported(mimeType: String): Boolean = mimeType in SUPPORTED_MIME_TYPES

    /**
     * Trả về danh sách MIME type được hỗ trợ cho document picker.
     */
    fun supportedMimeTypes(): Array<String> = SUPPORTED_MIME_TYPES.toTypedArray()

    /**
     * Đọc metadata từ Uri sử dụng ContentResolver.
     * Không cần bất kỳ quyền Android nào — chỉ dùng persisted URI permission.
     */
    fun extractFromUri(context: Context, uri: Uri): ImportedFile? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    Log.w(TAG, "extractFromUri: cursor rỗng cho uri=$uri")
                    return null
                }

                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment ?: "unknown"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val mimeType = context.contentResolver.getType(uri)

                Log.d(TAG, "extractFromUri: name=$name, mimeType=$mimeType, size=$size")
                if (mimeType == null) {
                    Log.w(TAG, "extractFromUri: getType trả về null cho uri=$uri")
                    return null
                }

                ImportedFile(
                    uri = uri.toString(),
                    name = name,
                    mimeType = mimeType,
                    sizeBytes = size
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "extractFromUri: SecurityException — không có quyền đọc uri=$uri: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "extractFromUri: EXCEPTION ${e.javaClass.simpleName} cho uri=$uri: ${e.message}", e)
            null
        }
    }

    /**
     * Lấy tên file từ Uri (fallback khi không đọc được cursor).
     */
    fun extractNameFromUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) return@use cursor.getString(nameIndex)
                }
                uri.lastPathSegment ?: "unknown"
            } ?: uri.lastPathSegment ?: "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "extractNameFromUri: EXCEPTION: ${e.message}")
            uri.lastPathSegment ?: "unknown"
        }
    }
}
