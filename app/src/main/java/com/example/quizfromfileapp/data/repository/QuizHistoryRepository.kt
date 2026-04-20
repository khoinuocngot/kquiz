package com.example.quizfromfileapp.data.repository

import com.example.quizfromfileapp.data.local.QuizHistoryStorage
import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import kotlinx.coroutines.flow.Flow

class QuizHistoryRepository(
    private val storage: QuizHistoryStorage
) {
    fun getAllHistory(): Flow<List<QuizHistoryEntity>> = storage.historyFlow

    suspend fun insert(item: QuizHistoryEntity): Long = storage.insert(item)

    suspend fun deleteById(id: Long) = storage.deleteById(id)

    suspend fun clearAll() = storage.clearAll()
}
