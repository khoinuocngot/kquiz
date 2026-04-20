package com.example.quizfromfileapp.data.local

import android.content.Context
import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class QuizHistoryStorage(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File
        get() = File(context.filesDir, "quiz_history.json")

    private val _historyFlow = MutableStateFlow<List<QuizHistoryEntity>>(emptyList())
    val historyFlow: Flow<List<QuizHistoryEntity>> = _historyFlow.asStateFlow()

    private var nextId = 1L

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val items: List<QuizHistoryEntity> = json.decodeFromString(content)
                    _historyFlow.value = items
                    nextId = (items.maxOfOrNull { it.id } ?: 0) + 1
                }
            }
        } catch (e: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    private fun saveToDisk(items: List<QuizHistoryEntity>) {
        try {
            file.writeText(json.encodeToString(items))
        } catch (_: Exception) { }
    }

    suspend fun insert(item: QuizHistoryEntity): Long {
        val newItem = item.copy(id = nextId++)
        val updated = listOf(newItem) + _historyFlow.value
        _historyFlow.value = updated
        saveToDisk(updated)
        return newItem.id
    }

    suspend fun deleteById(id: Long) {
        val updated = _historyFlow.value.filter { it.id != id }
        _historyFlow.value = updated
        saveToDisk(updated)
    }

    suspend fun clearAll() {
        _historyFlow.value = emptyList()
        saveToDisk(emptyList())
    }
}
