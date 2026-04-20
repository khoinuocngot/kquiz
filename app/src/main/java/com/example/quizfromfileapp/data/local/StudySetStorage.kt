package com.example.quizfromfileapp.data.local

import android.content.Context
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local JSON storage cho StudySet và Flashcard.
 *
 * Architecture:
 * - study_sets.json: danh sách StudySetEntity
 * - flashcards_{studySetId}.json: flashcards theo từng StudySet
 */
class StudySetStorage(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val studySetsFile: File
        get() = File(context.filesDir, "study_sets.json")

    private val _studySetsFlow = MutableStateFlow<List<StudySetEntity>>(emptyList())
    val studySetsFlow: Flow<List<StudySetEntity>> = _studySetsFlow.asStateFlow()

    private var nextSetId = 1L
    private var nextCardId = 1L

    init {
        loadStudySets()
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDY SET CRUD
    // ═══════════════════════════════════════════════════════════════

    private fun loadStudySets() {
        try {
            if (studySetsFile.exists()) {
                val content = studySetsFile.readText()
                if (content.isNotBlank()) {
                    val sets: List<StudySetEntity> = json.decodeFromString(content)
                    _studySetsFlow.value = sets
                    nextSetId = (sets.maxOfOrNull { it.id } ?: 0) + 1
                }
            }
        } catch (e: Exception) {
            _studySetsFlow.value = emptyList()
        }
    }

    private fun saveStudySets(sets: List<StudySetEntity>) {
        try {
            studySetsFile.writeText(json.encodeToString(sets))
        } catch (_: Exception) { }
    }

    suspend fun insertStudySet(entity: StudySetEntity): Long = withContext(Dispatchers.IO) {
        val newEntity = entity.copy(id = nextSetId++)
        val updated = _studySetsFlow.value + newEntity
        _studySetsFlow.value = updated
        saveStudySets(updated)
        newEntity.id
    }

    suspend fun updateStudySet(entity: StudySetEntity) = withContext(Dispatchers.IO) {
        val updated = _studySetsFlow.value.map {
            if (it.id == entity.id) entity.copy(updatedAt = System.currentTimeMillis()) else it
        }
        _studySetsFlow.value = updated
        saveStudySets(updated)
    }

    suspend fun deleteStudySet(id: Long) = withContext(Dispatchers.IO) {
        val updated = _studySetsFlow.value.filter { it.id != id }
        _studySetsFlow.value = updated
        saveStudySets(updated)
        // Xóa flashcards của set này
        getFlashcardsFile(id).delete()
    }

    suspend fun getStudySetById(id: Long): StudySetEntity? = withContext(Dispatchers.IO) {
        _studySetsFlow.value.find { it.id == id }
    }

    // ═══════════════════════════════════════════════════════════════
    // FLASHCARD CRUD
    // ═══════════════════════════════════════════════════════════════

    private fun getFlashcardsFile(studySetId: Long): File {
        return File(context.filesDir, "flashcards_$studySetId.json")
    }

    private fun loadFlashcards(studySetId: Long): List<FlashcardEntity> {
        val file = getFlashcardsFile(studySetId)
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            if (content.isNotBlank()) json.decodeFromString(content) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveFlashcards(studySetId: Long, cards: List<FlashcardEntity>) {
        try {
            getFlashcardsFile(studySetId).writeText(json.encodeToString(cards))
        } catch (_: Exception) { }
    }

    suspend fun insertFlashcard(entity: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        val cards = loadFlashcards(entity.studySetId).toMutableList()
        val newEntity = entity.copy(id = nextCardId++)
        cards.add(newEntity)
        saveFlashcards(entity.studySetId, cards)

        // Cập nhật cardCount của StudySet
        val set = _studySetsFlow.value.find { it.id == entity.studySetId }
        if (set != null) {
            updateStudySet(set.copy(cardCount = cards.size))
        }

        newEntity.id
    }

    suspend fun insertFlashcards(entities: List<FlashcardEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val studySetId = entities.first().studySetId
        val cards = loadFlashcards(studySetId).toMutableList()
        for (entity in entities) {
            cards.add(entity.copy(id = nextCardId++))
        }
        saveFlashcards(studySetId, cards)

        val set = _studySetsFlow.value.find { it.id == studySetId }
        if (set != null) {
            updateStudySet(set.copy(cardCount = cards.size))
        }
    }

    suspend fun getFlashcardsByStudySetId(studySetId: Long): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        loadFlashcards(studySetId)
    }

    suspend fun updateFlashcard(entity: FlashcardEntity) = withContext(Dispatchers.IO) {
        val cards = loadFlashcards(entity.studySetId).toMutableList()
        val idx = cards.indexOfFirst { it.id == entity.id }
        if (idx >= 0) {
            cards[idx] = entity
            saveFlashcards(entity.studySetId, cards)
        }
    }

    suspend fun updateFlashcards(entities: List<FlashcardEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val studySetId = entities.first().studySetId
        val cards = loadFlashcards(studySetId).toMutableList()
        for (entity in entities) {
            val idx = cards.indexOfFirst { it.id == entity.id }
            if (idx >= 0) cards[idx] = entity
        }
        saveFlashcards(studySetId, cards)
    }

    suspend fun deleteFlashcard(id: Long, studySetId: Long) = withContext(Dispatchers.IO) {
        val cards = loadFlashcards(studySetId).toMutableList()
        cards.removeIf { it.id == id }
        saveFlashcards(studySetId, cards)

        val set = _studySetsFlow.value.find { it.id == studySetId }
        if (set != null) {
            updateStudySet(set.copy(cardCount = cards.size))
        }
    }
}
