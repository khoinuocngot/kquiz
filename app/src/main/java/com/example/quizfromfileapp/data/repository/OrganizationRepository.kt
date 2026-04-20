package com.example.quizfromfileapp.data.repository

import com.example.quizfromfileapp.data.local.AppDatabase
import com.example.quizfromfileapp.data.local.dao.FolderDao
import com.example.quizfromfileapp.data.local.dao.StudyStatsDao
import com.example.quizfromfileapp.data.local.dao.TagDao
import com.example.quizfromfileapp.data.local.entity.DailyStudyStatsEntity
import com.example.quizfromfileapp.data.local.entity.FolderEntity
import com.example.quizfromfileapp.data.local.entity.StudySetTagCrossRef
import com.example.quizfromfileapp.data.local.entity.TagEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntityRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository cho Folder, Tag, và Study Stats.
 * Tất cả folder/tag được quản lý ở đây thay vì StudySetRepository.
 */
class OrganizationRepository(private val database: AppDatabase) {

    private val folderDao: FolderDao = database.folderDao()
    private val tagDao: TagDao = database.tagDao()
    private val studyStatsDao: StudyStatsDao = database.studyStatsDao()
    private val studySetDao = database.studySetDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ═══════════════════════════════════════════════════════════════
    // FOLDER
    // ═══════════════════════════════════════════════════════════════

    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.observeAll()

    suspend fun getAllFoldersSync(): List<FolderEntity> = folderDao.getAll()

    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getById(id)

    fun observeFolder(id: Long): Flow<FolderEntity?> = folderDao.observeById(id)

    suspend fun createFolder(name: String, description: String = "", colorHex: String = "#5B6CFF"): Long {
        val position = folderDao.getNextPosition()
        val folder = FolderEntity(
            name = name.trim(),
            description = description.trim(),
            colorHex = colorHex,
            position = position
        )
        return folderDao.insert(folder)
    }

    suspend fun updateFolder(id: Long, name: String, description: String = "", colorHex: String? = null) {
        val folder = folderDao.getById(id) ?: return
        val updated = folder.copy(
            name = name.trim(),
            description = description.trim(),
            colorHex = colorHex ?: folder.colorHex,
            updatedAt = System.currentTimeMillis()
        )
        folderDao.update(updated)
    }

    suspend fun deleteFolder(id: Long) {
        // Unassign all study sets from this folder first
        studySetDao.getById(id)?.let {
            // Note: folderId lives in StudySetEntityRoom
        }
        folderDao.deleteById(id)
    }

    suspend fun assignStudySetToFolder(studySetId: Long, folderId: Long?) {
        val set = studySetDao.getById(studySetId) ?: return
        studySetDao.update(set.copy(folderId = folderId))
    }

    suspend fun getStudySetCountInFolder(folderId: Long): Int {
        return folderDao.countStudySetsInFolder(folderId)
    }

    // ═══════════════════════════════════════════════════════════════
    // TAG
    // ═══════════════════════════════════════════════════════════════

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun getAllTagsSync(): List<TagEntity> = tagDao.getAll()

    suspend fun getTagById(id: Long): TagEntity? = tagDao.getById(id)

    suspend fun createTag(name: String, colorHex: String = "#8B5CF6"): Long {
        val tag = TagEntity(name = name.trim(), colorHex = colorHex)
        return tagDao.insert(tag)
    }

    suspend fun updateTag(id: Long, name: String, colorHex: String? = null) {
        val tag = tagDao.getById(id) ?: return
        val updated = tag.copy(
            name = name.trim(),
            colorHex = colorHex ?: tag.colorHex
        )
        tagDao.update(updated)
    }

    suspend fun deleteTag(id: Long) {
        tagDao.deleteById(id)
    }

    fun getTagsForStudySet(studySetId: Long): Flow<List<TagEntity>> {
        return tagDao.getTagsForStudySet(studySetId)
    }

    suspend fun getTagsForStudySetSync(studySetId: Long): List<TagEntity> {
        return tagDao.getTagsForStudySetSync(studySetId)
    }

    suspend fun addTagToStudySet(studySetId: Long, tagId: Long) {
        tagDao.insertTagCrossRef(StudySetTagCrossRef(studySetId, tagId))
    }

    suspend fun removeTagFromStudySet(studySetId: Long, tagId: Long) {
        tagDao.removeTagFromStudySet(studySetId, tagId)
    }

    suspend fun setTagsForStudySet(studySetId: Long, tagIds: List<Long>) {
        tagDao.removeAllTagsFromStudySet(studySetId)
        tagIds.forEach { tagId ->
            tagDao.insertTagCrossRef(StudySetTagCrossRef(studySetId, tagId))
        }
    }

    suspend fun isTagOnStudySet(studySetId: Long, tagId: Long): Boolean {
        return tagDao.isTagOnStudySet(studySetId, tagId)
    }

    suspend fun getStudySetCountWithTag(tagId: Long): Int {
        return tagDao.countStudySetsWithTag(tagId)
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDY STATS
    // ═══════════════════════════════════════════════════════════════

    private fun todayString(): String = dateFormat.format(Date())

    private fun yesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    private fun daysAgoString(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    /**
     * Gọi mỗi khi user học xong một session.
     * Tự động cập nhật streak và stats ngày hôm nay.
     */
    suspend fun recordStudySession(cardsReviewed: Int, studyTimeMs: Long) {
        val today = todayString()
        val yesterday = yesterdayString()

        // Get yesterday's stats to calculate streak
        val yesterdayStats = studyStatsDao.getByDate(yesterday)
        val yesterdayStreak = yesterdayStats?.streakCount ?: 0

        // Get today's existing stats
        val todayStats = studyStatsDao.getByDate(today)

        // Determine new streak
        val newStreak = if (todayStats != null) {
            // Already studied today, streak unchanged
            todayStats.streakCount
        } else {
            // First study session today
            if (yesterdayStats != null && yesterdayStreak > 0) {
                yesterdayStreak + 1
            } else {
                1
            }
        }

        val entity = DailyStudyStatsEntity(
            date = today,
            streakCount = newStreak,
            cardsReviewed = (todayStats?.cardsReviewed ?: 0) + cardsReviewed,
            studySetsCount = if (todayStats == null) 1 else todayStats.studySetsCount,
            totalStudyTimeMs = (todayStats?.totalStudyTimeMs ?: 0L) + studyTimeMs
        )
        studyStatsDao.insertOrUpdate(entity)
    }

    suspend fun getTodayStats(): DailyStudyStatsEntity? {
        return studyStatsDao.getByDate(todayString())
    }

    fun observeTodayStats(): Flow<DailyStudyStatsEntity?> {
        return studyStatsDao.observeByDate(todayString())
    }

    fun observeRecentStats(days: Int = 7): Flow<List<DailyStudyStatsEntity>> {
        return studyStatsDao.observeRecent(days)
    }

    suspend fun getRecentStats(days: Int = 7): List<DailyStudyStatsEntity> {
        return studyStatsDao.getRecent(days)
    }

    suspend fun getCurrentStreak(): Int {
        val today = getTodayStats()
        return today?.streakCount ?: 0
    }

    suspend fun getMaxStreak(): Int {
        return studyStatsDao.getMaxStreak() ?: 0
    }

    suspend fun getTotalCardsReviewed(): Int {
        return studyStatsDao.getTotalCardsReviewed() ?: 0
    }

    suspend fun getCardsReviewedThisWeek(): Int {
        val weekAgo = daysAgoString(7)
        return studyStatsDao.getCardsReviewedSince(weekAgo) ?: 0
    }

    suspend fun getNeedsReviewCount(): Int {
        return database.flashcardDao().getNeedsReviewCount()
    }

    /**
     * Lấy danh sách thẻ cần ôn ưu tiên (Smart Review).
     * Priority:
     * 1. masteryLevel == 0 (chưa học bao giờ)
     * 2. masteryLevel thấp nhất
     * 3. lâu chưa ôn nhất (lastReviewedAt null hoặc cũ nhất)
     * Giới hạn maxCards.
     */
    suspend fun getPriorityReviewCards(studySetId: Long? = null, maxCards: Int = 20): List<com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom> {
        return if (studySetId != null) {
            database.flashcardDao().getPriorityReview(studySetId).take(maxCards)
        } else {
            database.flashcardDao().getNeedsReviewCards(maxCards)
        }
    }
}
