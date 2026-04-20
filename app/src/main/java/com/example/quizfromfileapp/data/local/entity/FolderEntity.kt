package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity cho Folder — nhóm bộ học theo môn/chủ đề.
 *
 * @param id          ID duy nhất (auto-generate)
 * @param name        Tên folder
 * @param description Mô tả (optional)
 * @param colorHex    Màu hiển thị (hex string, e.g. "#5B6CFF")
 * @param position    Thứ tự sắp xếp
 * @param createdAt   Timestamp tạo
 * @param updatedAt   Timestamp cập nhật cuối
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#5B6CFF",
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT_COLORS = listOf(
            "#5B6CFF", // Primary blue
            "#14B8A6", // Teal
            "#8B5CF6", // Violet
            "#F59E0B", // Amber
            "#EF4444", // Red
            "#22C55E", // Green
            "#EC4899", // Pink
            "#6366F1"  // Indigo
        )
    }
}
