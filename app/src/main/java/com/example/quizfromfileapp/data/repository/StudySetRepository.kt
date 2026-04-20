package com.example.quizfromfileapp.data.repository

import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository cho StudySet và Flashcard.
 *
 * Backed by StudySetRepositoryRoom (Room database).
 * API kept backward compatible with existing code.
 */
class StudySetRepository(private val room: StudySetRepositoryRoom) {

    fun getAllStudySets(): Flow<List<StudySetEntity>> = room.getAllStudySets()

    suspend fun createStudySet(entity: StudySetEntity): Long = room.createStudySet(entity)

    suspend fun updateStudySet(entity: StudySetEntity) = room.updateStudySet(entity)

    suspend fun deleteStudySet(id: Long) = room.deleteStudySet(id)

    suspend fun getStudySetById(id: Long): StudySetEntity? = room.getStudySetById(id)

    suspend fun insertStudySet(entity: StudySetEntity): Long = room.insertStudySet(entity)

    suspend fun updateStudySetTitle(id: Long, newTitle: String) {
        room.updateStudySetTitle(id, newTitle)
    }

    suspend fun getFlashcardsByStudySetId(studySetId: Long): List<FlashcardEntity> =
        room.getFlashcardsByStudySetId(studySetId)

    suspend fun addFlashcard(entity: FlashcardEntity): Long = room.addFlashcard(entity)

    suspend fun addFlashcards(entities: List<FlashcardEntity>) = room.addFlashcards(entities)

    suspend fun updateFlashcard(entity: FlashcardEntity) = room.updateFlashcard(entity)

    suspend fun updateFlashcards(entities: List<FlashcardEntity>) = room.updateFlashcards(entities)

    suspend fun deleteFlashcard(id: Long, studySetId: Long) = room.deleteFlashcard(id, studySetId)

    fun observeStudySet(id: Long): Flow<StudySetEntity?> = room.observeStudySet(id)

    fun observeFlashcards(studySetId: Long): Flow<List<FlashcardEntity>> =
        room.observeFlashcards(studySetId)

    fun searchStudySets(query: String): Flow<List<StudySetEntity>> =
        room.searchStudySets(query)

    fun getStudySetsWithCards(): Flow<List<StudySetEntity>> =
        room.getStudySetsWithCards()

    fun getPinnedStudySets(): Flow<List<StudySetEntity>> =
        room.getPinnedStudySets()

    fun getFavoriteStudySets(): Flow<List<StudySetEntity>> =
        room.getFavoriteStudySets()

    suspend fun setPinned(id: Long, pinned: Boolean) = room.setPinned(id, pinned)

    suspend fun setFavorite(id: Long, favorite: Boolean) = room.setFavorite(id, favorite)

    suspend fun updateCardCount(studySetId: Long, count: Int) =
        room.updateCardCount(studySetId, count)

    suspend fun getFlashcardById(id: Long): FlashcardEntity? =
        room.getFlashcardById(id)

    suspend fun getShuffledFlashcards(studySetId: Long): List<FlashcardEntity> =
        room.getShuffledFlashcards(studySetId)

    suspend fun getPriorityReviewFlashcards(studySetId: Long): List<FlashcardEntity> =
        room.getPriorityReviewFlashcards(studySetId)

    suspend fun searchFlashcards(studySetId: Long, query: String): List<FlashcardEntity> =
        room.searchFlashcards(studySetId, query)

    suspend fun updateFlashcardMastery(
        id: Long,
        masteryLevel: Int,
        timesReviewed: Int,
        timesCorrect: Int,
        lastReviewedAt: Long
    ) = room.updateFlashcardMastery(id, masteryLevel, timesReviewed, timesCorrect, lastReviewedAt)

    suspend fun toggleFlashcardStarred(id: Long, starred: Boolean) =
        room.toggleFlashcardStarred(id, starred)

    suspend fun getMasteredCount(studySetId: Long): Int =
        room.getMasteredCount(studySetId)

    suspend fun getNeedsReviewCount(studySetId: Long): Int =
        room.getNeedsReviewCount(studySetId)

    suspend fun getAverageMastery(studySetId: Long): Float =
        room.getAverageMastery(studySetId)

    suspend fun getTotalCardCount(studySetId: Long): Int =
        room.getTotalCardCount(studySetId)

    suspend fun setFolderId(studySetId: Long, folderId: Long?) =
        room.setFolderId(studySetId, folderId)

    fun getStudySetsByFolder(folderId: Long): Flow<List<StudySetEntity>> =
        room.getStudySetsByFolder(folderId)

    fun getUnfolderedStudySets(): Flow<List<StudySetEntity>> =
        room.getUnfolderedStudySets()
}
