package com.example.quizfromfileapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    // ─── Get by StudySet ─────────────────────────────────────────────
    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId ORDER BY position ASC, id ASC")
    fun observeByStudySetId(studySetId: Long): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId ORDER BY position ASC, id ASC")
    suspend fun getByStudySetId(studySetId: Long): List<FlashcardEntityRoom>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getById(id: Long): FlashcardEntityRoom?

    // ─── Insert ──────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FlashcardEntityRoom): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FlashcardEntityRoom>)

    // ─── Update ──────────────────────────────────────────────────────
    @Update
    suspend fun update(entity: FlashcardEntityRoom)

    @Update
    suspend fun updateAll(entities: List<FlashcardEntityRoom>)

    // ─── Delete ──────────────────────────────────────────────────────
    @Delete
    suspend fun delete(entity: FlashcardEntityRoom)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM flashcards WHERE studySetId = :studySetId")
    suspend fun deleteByStudySetId(studySetId: Long)

    // ─── Partial updates ──────────────────────────────────────────────
    @Query("UPDATE flashcards SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE flashcards SET masteryLevel = :level, timesReviewed = :timesReviewed, timesCorrect = :timesCorrect, lastReviewedAt = :lastReviewedAt WHERE id = :id")
    suspend fun updateMastery(
        id: Long,
        level: Int,
        timesReviewed: Int,
        timesCorrect: Int,
        lastReviewedAt: Long
    )

    @Query("UPDATE flashcards SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    // ─── Filter queries ──────────────────────────────────────────────
    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND isStarred = 1 ORDER BY position ASC")
    fun getStarred(studySetId: Long): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND masteryLevel < :maxLevel ORDER BY masteryLevel ASC, id ASC")
    fun getNeedsReview(studySetId: Long, maxLevel: Int = 3): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND masteryLevel >= :minLevel ORDER BY masteryLevel DESC")
    fun getMastered(studySetId: Long, minLevel: Int = 4): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND masteryLevel = 0 ORDER BY id ASC")
    fun getNew(studySetId: Long): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND lastReviewedAt IS NOT NULL ORDER BY lastReviewedAt DESC")
    fun getRecentlyReviewed(studySetId: Long): Flow<List<FlashcardEntityRoom>>

    // ─── Search ─────────────────────────────────────────────────────
    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND (term LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%') ORDER BY position ASC")
    fun searchInSet(studySetId: Long, query: String): Flow<List<FlashcardEntityRoom>>

    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId AND (term LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%') ORDER BY position ASC")
    suspend fun searchInSetSync(studySetId: Long, query: String): List<FlashcardEntityRoom>

    // ─── Stats ───────────────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM flashcards WHERE studySetId = :studySetId")
    suspend fun countByStudySet(studySetId: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE studySetId = :studySetId AND masteryLevel >= :level")
    suspend fun countMasteredBySet(studySetId: Long, level: Int = 4): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE studySetId = :studySetId AND masteryLevel < :level")
    suspend fun countNeedsReviewBySet(studySetId: Long, level: Int = 3): Int

    @Query("SELECT AVG(masteryLevel * 1.0 / :maxLevel) FROM flashcards WHERE studySetId = :studySetId AND timesReviewed > 0")
    suspend fun averageMastery(studySetId: Long, maxLevel: Int = 5): Float?

    // ─── Shuffled ────────────────────────────────────────────────────
    @Query("SELECT * FROM flashcards WHERE studySetId = :studySetId ORDER BY RANDOM()")
    suspend fun getShuffled(studySetId: Long): List<FlashcardEntityRoom>

    // ─── Priority review ────────────────────────────────────────────
    @Query("""
        SELECT * FROM flashcards
        WHERE studySetId = :studySetId
        ORDER BY
            CASE WHEN masteryLevel = 0 THEN 0 WHEN timesReviewed = 0 THEN 1 ELSE 2 END,
            masteryLevel ASC,
            RANDOM()
    """)
    suspend fun getPriorityReview(studySetId: Long): List<FlashcardEntityRoom>

    @Query("""
        SELECT * FROM flashcards
        ORDER BY
            CASE WHEN masteryLevel = 0 THEN 0 WHEN timesReviewed = 0 THEN 1 ELSE 2 END,
            masteryLevel ASC,
            RANDOM()
        LIMIT :limit
    """)
    suspend fun getNeedsReviewCards(limit: Int): List<FlashcardEntityRoom>

    @Query("SELECT COUNT(*) FROM flashcards WHERE masteryLevel < :level")
    suspend fun getNeedsReviewCount(level: Int = 3): Int

    // ─── Bulk insert (for migration) ─────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWithId(entities: List<FlashcardEntityRoom>)

    // ─── Position management ─────────────────────────────────────────
    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM flashcards WHERE studySetId = :studySetId")
    suspend fun getNextPosition(studySetId: Long): Int
}
