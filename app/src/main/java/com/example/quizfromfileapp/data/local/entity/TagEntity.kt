package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity cho Tag — nhãn gắn trên bộ học.
 *
 * @param id        ID duy nhất (auto-generate)
 * @param name      Tên tag (duy nhất)
 * @param colorHex  Màu hiển thị (hex string)
 * @param createdAt Timestamp tạo
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#8B5CF6",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT_COLORS = listOf(
            "#8B5CF6", // Violet (default)
            "#5B6CFF", // Blue
            "#14B8A6", // Teal
            "#F59E0B", // Amber
            "#EF4444", // Red
            "#22C55E", // Green
            "#EC4899", // Pink
            "#6366F1"  // Indigo
        )
    }
}
