package com.example.quizfromfileapp.data.model

import kotlinx.serialization.Serializable

/**
 * Mô hình dữ liệu cho file đã import từ máy.
 *
 * @param uri Chuỗi URI của file (từ document picker)
 * @param name Tên file hiển thị
 * @param mimeType Loại MIME của file
 * @param sizeBytes Kích thước file tính bằng byte
 */
@Serializable
data class ImportedFile(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
}
