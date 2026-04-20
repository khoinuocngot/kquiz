package com.example.quizfromfileapp.ui.screens.studyset.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.data.repository.StudySetExportFormat
import com.example.quizfromfileapp.data.repository.ExportRepository
import com.example.quizfromfileapp.data.repository.StudySetRepository
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class StudySetDetailUiState(
    val studySet: StudySetEntity? = null,
    val flashcards: List<FlashcardEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val deleteCardId: Long? = null,
    val previewCardCount: Int = 3,
    // Search
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    // Bulk edit
    val isBulkMode: Boolean = false,
    val selectedCardIds: Set<Long> = emptySet(),
    // Export
    val isExporting: Boolean = false,
    // Stats
    val needsReviewCount: Int = 0,
    val masteredCount: Int = 0,
    val starredCount: Int = 0,
    val avgMasteryPercent: Int = 0,
) {
    val totalCards: Int get() = flashcards.size

    val displayedCards: List<FlashcardEntity>
        get() = if (searchQuery.isBlank()) flashcards
                else flashcards.filter {
                    it.term.contains(searchQuery, ignoreCase = true) ||
                    it.definition.contains(searchQuery, ignoreCase = true)
                }

    val selectedCount: Int get() = selectedCardIds.size

    val allSelected: Boolean
        get() = flashcards.isNotEmpty() && selectedCardIds.size == flashcards.size
}

sealed class StudySetDetailEvent {
    data class ShowSnackbar(val message: String) : StudySetDetailEvent()
    data class ExportSuccess(val fileName: String) : StudySetDetailEvent()
    data class ShareText(val text: String) : StudySetDetailEvent()
    data class ExportFile(val intent: android.content.Intent) : StudySetDetailEvent()
}

class StudySetDetailViewModel : ViewModel() {

    private val repository: StudySetRepository = AppContainer.studySetRepository
    private val exportRepository: ExportRepository = AppContainer.exportRepository
    private var appContext: Context? = null

    private val _uiState = MutableStateFlow(StudySetDetailUiState())
    val uiState: StateFlow<StudySetDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StudySetDetailEvent>()
    val events: SharedFlow<StudySetDetailEvent> = _events.asSharedFlow()

    private var currentStudySetId: Long = -1

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun loadStudySet(studySetId: Long) {
        if (currentStudySetId == studySetId) return
        currentStudySetId = studySetId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Observe study set changes reactively
            launch {
                repository.observeStudySet(studySetId).collectLatest { set ->
                    _uiState.value = _uiState.value.copy(
                        studySet = set,
                        isLoading = false,
                        editTitle = set?.title ?: "",
                        editDescription = set?.description ?: ""
                    )
                }
            }

            // Observe flashcards reactively
            launch {
                repository.observeFlashcards(studySetId).collectLatest { cards ->
                    val mastered = cards.count { it.masteryLevel >= 4 }
                    val needsReview = cards.count { it.masteryLevel < 3 }
                    val starred = cards.count { it.isStarred }
                    val avgPercent = if (cards.isNotEmpty()) {
                        (cards.map { it.masteryLevel }.average() / 5 * 100).toInt()
                    } else 0

                    _uiState.value = _uiState.value.copy(
                        flashcards = cards,
                        masteredCount = mastered,
                        needsReviewCount = needsReview,
                        starredCount = starred,
                        avgMasteryPercent = avgPercent,
                        // Clear selection when cards change
                        selectedCardIds = _uiState.value.selectedCardIds
                            .filter { id -> cards.any { it.id == id } }
                            .toSet()
                    )
                }
            }
        }
    }

    fun startEditing() {
        val set = _uiState.value.studySet ?: return
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editTitle = set.title,
            editDescription = set.description
        )
    }

    fun updateEditTitle(title: String) {
        _uiState.value = _uiState.value.copy(editTitle = title)
    }

    fun updateEditDescription(desc: String) {
        _uiState.value = _uiState.value.copy(editDescription = desc)
    }

    fun saveEdit() {
        val set = _uiState.value.studySet ?: return
        val state = _uiState.value
        viewModelScope.launch {
            val updated = set.copy(
                title = state.editTitle.trim(),
                description = state.editDescription.trim()
            )
            repository.updateStudySet(updated)
            _uiState.value = _uiState.value.copy(isEditing = false)
        }
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    fun showDeleteCard(cardId: Long) {
        _uiState.value = _uiState.value.copy(deleteCardId = cardId)
    }

    fun hideDeleteCard() {
        _uiState.value = _uiState.value.copy(deleteCardId = null)
    }

    fun confirmDeleteCard() {
        val cardId = _uiState.value.deleteCardId ?: return
        val studySetId = _uiState.value.studySet?.id ?: return
        viewModelScope.launch {
            repository.deleteFlashcard(cardId, studySetId)
            _uiState.value = _uiState.value.copy(deleteCardId = null)
        }
    }

    fun toggleStar(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.toggleFlashcardStarred(card.id, !card.isStarred)
        }
    }

    suspend fun updateCard(entity: FlashcardEntity) {
        repository.updateFlashcard(entity)
    }

    fun togglePin() {
        val set = _uiState.value.studySet ?: return
        viewModelScope.launch {
            repository.setPinned(set.id, !set.isPinned)
        }
    }

    fun toggleFavorite() {
        val set = _uiState.value.studySet ?: return
        viewModelScope.launch {
            repository.setFavorite(set.id, !set.isFavorite)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    // ─── Bulk Edit ──────────────────────────────────────────────────

    fun enterBulkMode() {
        _uiState.value = _uiState.value.copy(isBulkMode = true, selectedCardIds = emptySet())
    }

    fun exitBulkMode() {
        _uiState.value = _uiState.value.copy(isBulkMode = false, selectedCardIds = emptySet())
    }

    fun toggleCardSelection(cardId: Long) {
        val current = _uiState.value.selectedCardIds
        val updated = if (current.contains(cardId)) {
            current - cardId
        } else {
            current + cardId
        }
        _uiState.value = _uiState.value.copy(selectedCardIds = updated)
    }

    fun selectAllCards() {
        val allIds = _uiState.value.flashcards.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedCardIds = allIds)
    }

    fun deselectAllCards() {
        _uiState.value = _uiState.value.copy(selectedCardIds = emptySet())
    }

    fun bulkStarSelected() {
        val selected = _uiState.value.selectedCardIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { cardId ->
                val card = _uiState.value.flashcards.find { it.id == cardId }
                if (card != null && !card.isStarred) {
                    repository.toggleFlashcardStarred(cardId, true)
                }
            }
            _events.emit(StudySetDetailEvent.ShowSnackbar("Đã ghim ${selected.size} thẻ rồi nè!"))
            exitBulkMode()
        }
    }

    fun bulkUnstarSelected() {
        val selected = _uiState.value.selectedCardIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { cardId ->
                val card = _uiState.value.flashcards.find { it.id == cardId }
                if (card != null && card.isStarred) {
                    repository.toggleFlashcardStarred(cardId, false)
                }
            }
            _events.emit(StudySetDetailEvent.ShowSnackbar("Đã bỏ ghim ${selected.size} thẻ rồi nè!"))
            exitBulkMode()
        }
    }

    fun bulkDeleteSelected(onDeleted: (Int) -> Unit) {
        val selected = _uiState.value.selectedCardIds
        val studySetId = _uiState.value.studySet?.id ?: return
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { cardId ->
                repository.deleteFlashcard(cardId, studySetId)
            }
            _events.emit(StudySetDetailEvent.ShowSnackbar("Đã xoá ${selected.size} thẻ rồi nè!"))
            onDeleted(selected.size)
            exitBulkMode()
        }
    }

    fun addCard(term: String, definition: String) {
        val studySetId = _uiState.value.studySet?.id ?: return
        viewModelScope.launch {
            val newCard = FlashcardEntity(
                studySetId = studySetId,
                term = term,
                definition = definition
            )
            repository.addFlashcard(newCard)
            _events.emit(StudySetDetailEvent.ShowSnackbar("Đã thêm thẻ mới rồi nè!"))
        }
    }

    // ─── Export ────────────────────────────────────────────────────

    suspend fun exportToStudySet() {
        val set = _uiState.value.studySet ?: return
        val cards = _uiState.value.flashcards
        if (cards.isEmpty()) {
            _events.emit(StudySetDetailEvent.ShowSnackbar("Bộ học này chưa có thẻ nào!"))
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true)
        val result = exportRepository.exportToStudySetFile(set, cards)
        _uiState.value = _uiState.value.copy(isExporting = false)
        result.fold(
            onSuccess = { exportResult ->
                val intent = exportRepository.createShareFileIntent(exportResult)
                _events.emit(StudySetDetailEvent.ExportFile(intent))
            },
            onFailure = {
                _events.emit(StudySetDetailEvent.ShowSnackbar("Xuất thất bại, thử lại nha!"))
            }
        )
    }

    suspend fun exportToJson() {
        val set = _uiState.value.studySet ?: return
        val cards = _uiState.value.flashcards
        if (cards.isEmpty()) {
            _events.emit(StudySetDetailEvent.ShowSnackbar("Bộ học này chưa có thẻ nào!"))
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true)
        val result = exportRepository.exportToJson(set, cards)
        _uiState.value = _uiState.value.copy(isExporting = false)
        result.fold(
            onSuccess = { exportResult ->
                val intent = exportRepository.createShareFileIntent(exportResult)
                _events.emit(StudySetDetailEvent.ExportFile(intent))
            },
            onFailure = {
                _events.emit(StudySetDetailEvent.ShowSnackbar("Xuất thất bại, thử lại nha!"))
            }
        )
    }

    suspend fun exportToCsv() {
        val set = _uiState.value.studySet ?: return
        val cards = _uiState.value.flashcards
        if (cards.isEmpty()) {
            _events.emit(StudySetDetailEvent.ShowSnackbar("Bộ học này chưa có thẻ nào!"))
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true)
        val result = exportRepository.exportToCsv(set, cards)
        _uiState.value = _uiState.value.copy(isExporting = false)
        result.fold(
            onSuccess = { exportResult ->
                val intent = exportRepository.createShareFileIntent(exportResult)
                _events.emit(StudySetDetailEvent.ExportFile(intent))
            },
            onFailure = {
                _events.emit(StudySetDetailEvent.ShowSnackbar("Xuất thất bại, thử lại nha!"))
            }
        )
    }

    suspend fun exportToTxt() {
        val set = _uiState.value.studySet ?: return
        val cards = _uiState.value.flashcards
        if (cards.isEmpty()) {
            _events.emit(StudySetDetailEvent.ShowSnackbar("Bộ học này chưa có thẻ nào!"))
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true)
        val result = exportRepository.exportToTxt(set, cards)
        _uiState.value = _uiState.value.copy(isExporting = false)
        result.fold(
            onSuccess = { exportResult ->
                val intent = exportRepository.createShareFileIntent(exportResult)
                _events.emit(StudySetDetailEvent.ExportFile(intent))
            },
            onFailure = {
                _events.emit(StudySetDetailEvent.ShowSnackbar("Xuất thất bại, thử lại nha!"))
            }
        )
    }

    suspend fun shareAsText() {
        val set = _uiState.value.studySet ?: return
        val cards = _uiState.value.flashcards
        if (cards.isEmpty()) {
            _events.emit(StudySetDetailEvent.ShowSnackbar("Bộ học này chưa có thẻ nào!"))
            return
        }
        val text = exportRepository.createShareText(set, cards)
        _events.emit(StudySetDetailEvent.ShareText(text))
    }
}
