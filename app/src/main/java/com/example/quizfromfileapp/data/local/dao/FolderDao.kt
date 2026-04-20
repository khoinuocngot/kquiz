package com.example.quizfromfileapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizfromfileapp.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY position ASC, name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY position ASC, name ASC")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observeById(id: Long): Flow<FolderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FolderEntity): Long

    @Update
    suspend fun update(entity: FolderEntity)

    @Delete
    suspend fun delete(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE folders SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET description = :description, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET colorHex = :colorHex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateColor(id: Long, colorHex: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM folders")
    suspend fun getNextPosition(): Int

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM study_sets WHERE folderId = :folderId")
    suspend fun countStudySetsInFolder(folderId: Long): Int
}
