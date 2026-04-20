package com.example.quizfromfileapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.quizfromfileapp.data.local.entity.StudySetTagCrossRef
import com.example.quizfromfileapp.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TagEntity): Long

    @Update
    suspend fun update(entity: TagEntity)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteByName(name: String)

    // ─── Cross-ref operations ─────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRef(crossRef: StudySetTagCrossRef)

    @Delete
    suspend fun deleteTagCrossRef(crossRef: StudySetTagCrossRef)

    @Query("DELETE FROM study_set_tags WHERE studySetId = :studySetId AND tagId = :tagId")
    suspend fun removeTagFromStudySet(studySetId: Long, tagId: Long)

    @Query("DELETE FROM study_set_tags WHERE studySetId = :studySetId")
    suspend fun removeAllTagsFromStudySet(studySetId: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN study_set_tags st ON t.id = st.tagId WHERE st.studySetId = :studySetId ORDER BY t.name ASC")
    fun getTagsForStudySet(studySetId: Long): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tags t INNER JOIN study_set_tags st ON t.id = st.tagId WHERE st.studySetId = :studySetId ORDER BY t.name ASC")
    suspend fun getTagsForStudySetSync(studySetId: Long): List<TagEntity>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN study_set_tags st ON t.id = st.tagId
        INNER JOIN study_sets s ON s.id = st.studySetId
        WHERE s.folderId = :folderId
        GROUP BY t.id
        ORDER BY t.name ASC
    """)
    fun getTagsInFolder(folderId: Long): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM study_set_tags WHERE tagId = :tagId")
    suspend fun countStudySetsWithTag(tagId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM study_set_tags WHERE studySetId = :studySetId AND tagId = :tagId)")
    suspend fun isTagOnStudySet(studySetId: Long, tagId: Long): Boolean
}
