package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity cho Streak — theo dõi chuỗi ngày học liên tiếp.
 *
 * @param id              ID duy nhất (auto-generate)
 * @param date            Ngày format yyyy-MM-dd
 * @param streakCount     Số ngày học liên tiếp tính đến ngày này
 * @param cardsReviewed   Số thẻ đã học trong ngày
 * @param studySetsCount  Số bộ học đã học trong ngày
 * @param totalStudyTimeMs Tổng thời gian học (ms)
 */
@Entity(tableName = "daily_study_stats")
data class DailyStudyStatsEntity(
    @PrimaryKey
    val date: String, // format: "yyyy-MM-dd"
    val streakCount: Int = 0,
    val cardsReviewed: Int = 0,
    val studySetsCount: Int = 0,
    val totalStudyTimeMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
