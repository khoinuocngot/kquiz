package com.example.quizfromfileapp.domain.usecase

import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import com.example.quizfromfileapp.data.repository.QuizHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetQuizHistoryUseCase(
    private val repository: QuizHistoryRepository
) {
    operator fun invoke(): Flow<List<QuizHistoryEntity>> = repository.getAllHistory()
}

class SaveQuizHistoryUseCase(
    private val repository: QuizHistoryRepository
) {
    suspend operator fun invoke(item: QuizHistoryEntity): Long = repository.insert(item)
}

class DeleteQuizHistoryUseCase(
    private val repository: QuizHistoryRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteById(id)
}

class ClearQuizHistoryUseCase(
    private val repository: QuizHistoryRepository
) {
    suspend operator fun invoke() = repository.clearAll()
}
