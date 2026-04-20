package com.example.quizfromfileapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.quizfromfileapp.data.local.entity.DailyStudyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyStatsDao {

    @Query("SELECT * FROM daily_study_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStudyStatsEntity?

    @Query("SELECT * FROM daily_study_stats WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyStudyStatsEntity?>

    @Query("SELECT * FROM daily_study_stats ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 7): Flow<List<DailyStudyStatsEntity>>

    @Query("SELECT * FROM daily_study_stats ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 7): List<DailyStudyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyStudyStatsEntity)

    @Query("""
        UPDATE daily_study_stats
        SET cardsReviewed = cardsReviewed + :cards,
            totalStudyTimeMs = totalStudyTimeMs + :timeMs,
            studySetsCount = studySetsCount + :setsAdded
        WHERE date = :date
    """)
    suspend fun incrementStats(date: String, cards: Int, timeMs: Long, setsAdded: Int)

    @Query("UPDATE daily_study_stats SET streakCount = :streak WHERE date = :date")
    suspend fun updateStreak(date: String, streak: Int)

    @Query("SELECT * FROM daily_study_stats WHERE date < :beforeDate ORDER BY date DESC LIMIT :limit")
    suspend fun getBefore(beforeDate: String, limit: Int = 1): List<DailyStudyStatsEntity>

    @Query("SELECT MAX(streakCount) FROM daily_study_stats")
    suspend fun getMaxStreak(): Int?

    @Query("SELECT SUM(cardsReviewed) FROM daily_study_stats")
    suspend fun getTotalCardsReviewed(): Int?

    @Query("SELECT SUM(cardsReviewed) FROM daily_study_stats WHERE date >= :fromDate")
    suspend fun getCardsReviewedSince(fromDate: String): Int?
}
