package com.example.quizfromfileapp.data.repository

import android.content.Context
import com.example.quizfromfileapp.data.local.AppDatabase
import com.example.quizfromfileapp.data.local.dao.FlashcardDao
import com.example.quizfromfileapp.data.local.dao.StudySetDao
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntityRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Room-based repository cho StudySet và Flashcard.
 *
 * API giữ nguyên tương thích 100% với StudySetRepository cũ.
 *
 * Chuyển đổi giữa Room entity (internal) và JSON entity (public API).
 * Khi khởi tạo, tự động migrate data từ JSON file cũ (nếu có).
 */
class StudySetRepositoryRoom(private val database: AppDatabase) {

    private val studySetDao: StudySetDao = database.studySetDao()
    private val flashcardDao: FlashcardDao = database.flashcardDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDY SET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Observe all study sets — returns Flow of JSON-compatible entities.
     */
    fun getAllStudySets(): Flow<List<StudySetEntity>> {
        return studySetDao.observeAll().map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    /**
     * Observe a single study set by ID.
     */
    fun observeStudySet(id: Long): Flow<StudySetEntity?> {
        return studySetDao.observeById(id).map { it?.toJsonEntity() }
    }

    /**
     * Get study set by ID (suspend).
     */
    suspend fun getStudySetById(id: Long): StudySetEntity? {
        return studySetDao.getById(id)?.toJsonEntity()
    }

    /**
     * Get study set by title (suspend). Dùng để kiểm tra trùng title khi import.
     */
    suspend fun getStudySetByTitle(title: String): StudySetEntity? {
        return studySetDao.getByTitle(title)?.toJsonEntity()
    }

    /**
     * Create a new study set. Returns the new ID.
     */
    suspend fun createStudySet(entity: StudySetEntity): Long {
        val room = entity.toRoom()
        return studySetDao.insert(room)
    }

    /**
     * Insert study set (alias for createStudySet).
     */
    suspend fun insertStudySet(entity: StudySetEntity): Long {
        return createStudySet(entity)
    }

    /**
     * Update an existing study set.
     */
    suspend fun updateStudySet(entity: StudySetEntity) {
        val room = entity.toRoom().copy(updatedAt = System.currentTimeMillis())
    studySetDao.update(room)
    }

    /**
     * Update only the title of a study set.
     */
    suspend fun updateStudySetTitle(id: Long, newTitle: String) {
        studySetDao.updateTitle(id, newTitle)
    }

    /**
     * Delete a study set and all its flashcards (CASCADE).
     */
    suspend fun deleteStudySet(id: Long) {
        studySetDao.deleteById(id)
    }

    /**
     * Pin / unpin a study set.
     */
    suspend fun setPinned(id: Long, pinned: Boolean) {
        studySetDao.updatePinned(id, pinned)
    }

    /**
     * Favorite / unfavorite a study set.
     */
    suspend fun setFavorite(id: Long, favorite: Boolean) {
        studySetDao.updateFavorite(id, favorite)
    }

    /**
     * Update card count for a study set.
     */
    suspend fun updateCardCount(studySetId: Long, count: Int) {
        studySetDao.updateCardCount(studySetId, count)
    }

    /**
     * Search study sets by title or description.
     */
    fun searchStudySets(query: String): Flow<List<StudySetEntity>> {
        return studySetDao.search(query).map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    /**
     * Get study sets with cards only.
     */
    fun getStudySetsWithCards(): Flow<List<StudySetEntity>> {
        return studySetDao.getWithCards().map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    /**
     * Get pinned study sets.
     */
    fun getPinnedStudySets(): Flow<List<StudySetEntity>> {
        return studySetDao.getPinned().map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    /**
     * Get favorite study sets.
     */
    fun getFavoriteStudySets(): Flow<List<StudySetEntity>> {
        return studySetDao.getFavorites().map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FLASHCARD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all flashcards for a study set.
     */
    suspend fun getFlashcardsByStudySetId(studySetId: Long): List<FlashcardEntity> {
        return flashcardDao.getByStudySetId(studySetId).map { it.toJsonEntity() }
    }

    /**
     * Observe flashcards for a study set.
     */
    fun observeFlashcards(studySetId: Long): Flow<List<FlashcardEntity>> {
        return flashcardDao.observeByStudySetId(studySetId).map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    /**
     * Add a single flashcard.
     */
    suspend fun addFlashcard(entity: FlashcardEntity): Long {
        val position = flashcardDao.getNextPosition(entity.studySetId)
        val room = entity.toRoom().copy(position = position)
        val id = flashcardDao.insert(room)
        // Update card count
        val count = flashcardDao.countByStudySet(entity.studySetId)
        studySetDao.updateCardCount(entity.studySetId, count)
        return id
    }

    /**
     * Add multiple flashcards at once (bulk insert).
     */
    suspend fun addFlashcards(entities: List<FlashcardEntity>) {
        if (entities.isEmpty()) return
        val studySetId = entities.first().studySetId

        // Assign positions
        var nextPos = flashcardDao.getNextPosition(studySetId)
        val rooms = entities.mapIndexed { index, entity ->
            entity.toRoom().copy(position = nextPos + index)
        }
        flashcardDao.insertAll(rooms)

        // Update card count
        val count = flashcardDao.countByStudySet(studySetId)
        studySetDao.updateCardCount(studySetId, count)
    }

    /**
     * Update a single flashcard.
     */
    suspend fun updateFlashcard(entity: FlashcardEntity) {
        flashcardDao.update(entity.toRoom())
    }

    /**
     * Update multiple flashcards at once.
     */
    suspend fun updateFlashcards(entities: List<FlashcardEntity>) {
        if (entities.isEmpty()) return
        flashcardDao.updateAll(entities.map { it.toRoom() })
    }

    /**
     * Update mastery stats for a flashcard.
     */
    suspend fun updateFlashcardMastery(
        id: Long,
        masteryLevel: Int,
        timesReviewed: Int,
        timesCorrect: Int,
        lastReviewedAt: Long
    ) {
        flashcardDao.updateMastery(id, masteryLevel, timesReviewed, timesCorrect, lastReviewedAt)
    }

    /**
     * Toggle starred state for a flashcard.
     */
    suspend fun toggleFlashcardStarred(id: Long, starred: Boolean) {
        flashcardDao.updateStarred(id, starred)
    }

    /**
     * Delete a single flashcard.
     */
    suspend fun deleteFlashcard(id: Long, studySetId: Long) {
        flashcardDao.deleteById(id)
        // Update card count
        val count = flashcardDao.countByStudySet(studySetId)
        studySetDao.updateCardCount(studySetId, count)
    }

    /**
     * Get a single flashcard by ID.
     */
    suspend fun getFlashcardById(id: Long): FlashcardEntity? {
        return flashcardDao.getById(id)?.toJsonEntity()
    }

    /**
     * Get shuffled flashcards for a study set.
     */
    suspend fun getShuffledFlashcards(studySetId: Long): List<FlashcardEntity> {
        return flashcardDao.getShuffled(studySetId).map { it.toJsonEntity() }
    }

    /**
     * Get priority review flashcards (low mastery first, then random).
     */
    suspend fun getPriorityReviewFlashcards(studySetId: Long): List<FlashcardEntity> {
        return flashcardDao.getPriorityReview(studySetId).map { it.toJsonEntity() }
    }

    /**
     * Search flashcards within a study set.
     */
    suspend fun searchFlashcards(studySetId: Long, query: String): List<FlashcardEntity> {
        return flashcardDao.searchInSetSync(studySetId, query).map { it.toJsonEntity() }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATS
    // ═══════════════════════════════════════════════════════════════

    suspend fun getMasteredCount(studySetId: Long): Int {
        return flashcardDao.countMasteredBySet(studySetId)
    }

    suspend fun getNeedsReviewCount(studySetId: Long): Int {
        return flashcardDao.countNeedsReviewBySet(studySetId)
    }

    suspend fun getAverageMastery(studySetId: Long): Float {
        return flashcardDao.averageMastery(studySetId) ?: 0f
    }

    suspend fun getTotalCardCount(studySetId: Long): Int {
        return flashcardDao.countByStudySet(studySetId)
    }

    suspend fun setFolderId(studySetId: Long, folderId: Long?) {
        val set = studySetDao.getById(studySetId) ?: return
        studySetDao.updateFolderId(studySetId, folderId)
    }

    fun getStudySetsByFolder(folderId: Long): Flow<List<StudySetEntity>> {
        return studySetDao.getByFolderId(folderId).map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    fun getUnfolderedStudySets(): Flow<List<StudySetEntity>> {
        return studySetDao.getUnfoldered().map { rooms ->
            rooms.map { it.toJsonEntity() }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MIGRATION FROM JSON
    // ═══════════════════════════════════════════════════════════════

    /**
     * Migrate all data from legacy JSON storage.
     * Call this once on first launch after Room is set up.
     */
    suspend fun migrateFromJson(context: Context) {
        withContext(Dispatchers.IO) {
            val studySetsFile = File(context.filesDir, "study_sets.json")
            if (!studySetsFile.exists()) return@withContext

            try {
                val jsonContent = studySetsFile.readText()
                if (jsonContent.isBlank()) {
                    studySetsFile.delete()
                    return@withContext
                }

                val legacySets: List<StudySetEntity> = json.decodeFromString(jsonContent)

                // Migrate study sets
                val roomSets = legacySets.map { it.toRoom() }
                studySetDao.insertAll(roomSets)

                // Migrate flashcards for each set
                for (legacySet in legacySets) {
                    val flashcardsFile = File(context.filesDir, "flashcards_${legacySet.id}.json")
                    if (flashcardsFile.exists()) {
                        try {
                            val cardsContent = flashcardsFile.readText()
                            if (cardsContent.isNotBlank()) {
                                val legacyCards: List<FlashcardEntity> = json.decodeFromString(cardsContent)
                                val roomCards = legacyCards.mapIndexed { index, card ->
                                    card.toRoom().copy(
                                        id = 0, // let Room auto-generate
                                        studySetId = legacySet.id,
                                        position = index
                                    )
                                }
                                flashcardDao.insertAllWithId(roomCards)
                            }
                        } catch (_: Exception) {
                            // Skip if can't read flashcards file
                        }
                        // Delete old flashcards file
                        flashcardsFile.delete()
                    }
                }

                // Delete old study sets file
                studySetsFile.delete()

                // Re-sync card counts
                for (set in legacySets) {
                    val count = flashcardDao.countByStudySet(set.id)
                    if (count > 0) {
                        studySetDao.updateCardCount(set.id, count)
                    }
                }
            } catch (e: Exception) {
                // Migration failed — log but don't crash
                e.printStackTrace()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun StudySetEntity.toRoom(): StudySetEntityRoom {
        return StudySetEntityRoom(
            id = id,
            title = title,
            description = description,
            cardCount = cardCount,
            sourceType = sourceType,
            sourceFileName = sourceFileName,
            studySetType = studySetType,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun FlashcardEntity.toRoom(): FlashcardEntityRoom {
        return FlashcardEntityRoom(
            id = id,
            studySetId = studySetId,
            term = term,
            definition = definition,
            explanation = explanation,
            choices = choices,
            correctChoiceIndex = correctChoiceIndex,
            sourceSnippet = sourceSnippet,
            sourcePageStart = sourcePageStart,
            sourcePageEnd = sourcePageEnd,
            isStarred = isStarred,
            masteryLevel = masteryLevel,
            timesReviewed = timesReviewed,
            timesCorrect = timesCorrect,
            lastReviewedAt = lastReviewedAt,
            createdAt = createdAt
        )
    }

    private fun FlashcardEntityRoom.toJsonEntity(): FlashcardEntity {
        return FlashcardEntity(
            id = id,
            studySetId = studySetId,
            term = term,
            definition = definition,
            explanation = explanation,
            choices = choices,
            correctChoiceIndex = correctChoiceIndex,
            sourceSnippet = sourceSnippet,
            sourcePageStart = sourcePageStart,
            sourcePageEnd = sourcePageEnd,
            isStarred = isStarred,
            masteryLevel = masteryLevel,
            timesReviewed = timesReviewed,
            timesCorrect = timesCorrect,
            lastReviewedAt = lastReviewedAt,
            createdAt = createdAt
        )
    }
}
