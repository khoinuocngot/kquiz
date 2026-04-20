package com.example.quizfromfileapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizfromfileapp.data.local.entity.StudySetEntityRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySetDao {

    // ─── Observe all ────────────────────────────────────────────────────
    @Query("SELECT * FROM study_sets ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<StudySetEntityRoom>>

    // ─── Get by ID ────────────────────────────────────────────────────
    @Query("SELECT * FROM study_sets WHERE id = :id")
    suspend fun getById(id: Long): StudySetEntityRoom?

    @Query("SELECT * FROM study_sets WHERE id = :id")
    fun observeById(id: Long): Flow<StudySetEntityRoom?>

    @Query("SELECT * FROM study_sets WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): StudySetEntityRoom?

    // ─── Insert ──────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudySetEntityRoom): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StudySetEntityRoom>)

    // ─── Update ──────────────────────────────────────────────────────
    @Update
    suspend fun update(entity: StudySetEntityRoom)

    // ─── Delete ──────────────────────────────────────────────────────
    @Delete
    suspend fun delete(entity: StudySetEntityRoom)

    @Query("DELETE FROM study_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM study_sets")
    suspend fun deleteAll()

    // ─── Partial updates ──────────────────────────────────────────────
    @Query("UPDATE study_sets SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE study_sets SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE study_sets SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE study_sets SET cardCount = :cardCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCardCount(id: Long, cardCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE study_sets SET description = :description, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String, updatedAt: Long = System.currentTimeMillis())

    // ─── Search & Filter ─────────────────────────────────────────────
    @Query("SELECT * FROM study_sets WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun search(query: String): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinned(): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE isFavorite = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavorites(): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE sourceType = :sourceType ORDER BY isPinned DESC, updatedAt DESC")
    fun getBySourceType(sourceType: String): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE cardCount > 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getWithCards(): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE folderId = :folderId ORDER BY isPinned DESC, updatedAt DESC")
    fun getByFolderId(folderId: Long): Flow<List<StudySetEntityRoom>>

    @Query("SELECT * FROM study_sets WHERE folderId IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getUnfoldered(): Flow<List<StudySetEntityRoom>>

    @Query("UPDATE study_sets SET folderId = :folderId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFolderId(id: Long, folderId: Long?, updatedAt: Long = System.currentTimeMillis())

    // ─── Stats ───────────────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM study_sets")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM study_sets WHERE cardCount > 0")
    suspend fun countWithCards(): Int

    // ─── Bulk insert (for migration) ─────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entities: List<StudySetEntityRoom>): List<Long>
}
