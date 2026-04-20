package com.example.quizfromfileapp.ui.screens.studyset.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Chế độ lọc thẻ trong Flashcard mode.
 */
enum class FlashcardFilter(val displayName: String) {
    ALL("Tất cả thẻ"),
    STARRED("Đã ghim"),
    NEED_REVIEW("Cần ôn lại"),
    MASTERED("Đã thành thạo"),
    UNMASTERED("Chưa thành thạo")
}

/**
 * Chế độ sắp xếp thẻ.
 */
enum class FlashcardSortMode(val displayName: String) {
    ORIGINAL("Thứ tự gốc"),
    RANDOM("Ngẫu nhiên"),
    NEED_REVIEW_FIRST("Cần ôn trước"),
    STARRED_FIRST("Đã ghim trước")
}

/**
 * Trạng thái một lần học (session).
 */
data class FlashcardSessionSummary(
    val totalCards: Int = 0,
    val remembered: Int = 0,
    val needReview: Int = 0,
    val starredCount: Int = 0,
    val wrongCardCount: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L
) {
    val isComplete: Boolean get() = endTime > 0L
    val durationSeconds: Long get() = if (isComplete) (endTime - startTime) / 1000 else 0L
    val durationFormatted: String
        get() {
            val totalSec = durationSeconds
            val min = totalSec / 60
            val sec = totalSec % 60
            return if (min > 0) "${min}p ${sec}gi" else "${sec}gi"
        }
}

/**
 * Trạng thái UI của FlashcardScreen nâng cao.
 */
data class FlashcardUiState(
    val allCards: List<FlashcardEntity> = emptyList(),
    val filteredCards: List<FlashcardEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isShuffled: Boolean = false,
    val studySetType: String = "",
    val selectedFilter: FlashcardFilter = FlashcardFilter.ALL,
    val selectedSortMode: FlashcardSortMode = FlashcardSortMode.ORIGINAL,
    val sessionSummary: FlashcardSessionSummary = FlashcardSessionSummary(),
    val showSessionSummary: Boolean = false,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    // Per-card review state within the session
    val reviewedCardIds: Set<Long> = emptySet(),
    val wrongCardIds: Set<Long> = emptySet()
) {
    val currentCard: FlashcardEntity?
        get() = filteredCards.getOrNull(currentIndex)

    val progress: Float
        get() = if (filteredCards.isEmpty()) 0f else (currentIndex + 1).toFloat() / filteredCards.size

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isFirst: Boolean
        get() = currentIndex == 0

    val isLast: Boolean
        get() = currentIndex == filteredCards.size - 1

    val hasCards: Boolean
        get() = filteredCards.isNotEmpty()

    /** Check if a card (by its index in filteredCards) was marked wrong in this session */
    fun isCardMarkedWrong(index: Int): Boolean {
        val card = filteredCards.getOrNull(index) ?: return false
        return card.id in wrongCardIds
    }

    /** Check if a card (by its index in filteredCards) was reviewed in this session */
    fun isCardReviewed(index: Int): Boolean {
        val card = filteredCards.getOrNull(index) ?: return false
        return card.id in reviewedCardIds
    }
}

/**
 * ViewModel nâng cao cho FlashcardScreen.
 * Hỗ trợ: lọc, sắp xếp, shuffle, tracking mastery, session summary.
 */
class FlashcardViewModel : ViewModel() {

    private val repository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var allCardsRaw: List<FlashcardEntity> = emptyList()
    private var studySetId: Long = 0L

    fun loadCards(studySetId: Long) {
        this.studySetId = studySetId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val studySet = repository.getStudySetById(studySetId)
            val cards = repository.getFlashcardsByStudySetId(studySetId)
            allCardsRaw = cards

            val studySetType = studySet?.studySetType ?: StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION
            val filtered = applyFilterAndSort(cards, _uiState.value.selectedFilter, _uiState.value.selectedSortMode)
            val starredCount = cards.count { it.isStarred }

            _uiState.value = FlashcardUiState(
                allCards = cards,
                filteredCards = filtered,
                isLoading = false,
                currentIndex = 0,
                isFlipped = false,
                isShuffled = false,
                studySetType = studySetType,
                sessionSummary = FlashcardSessionSummary(
                    totalCards = filtered.size,
                    starredCount = starredCount
                )
            )
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.currentIndex < state.filteredCards.size - 1) {
            val nextIdx = state.currentIndex + 1
            _uiState.value = state.copy(currentIndex = nextIdx, isFlipped = false)
            checkSessionComplete(nextIdx, state.filteredCards.size)
        }
    }

    fun prevCard() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1, isFlipped = false)
        }
    }

    fun goToCard(index: Int) {
        val state = _uiState.value
        if (index in state.filteredCards.indices) {
            _uiState.value = state.copy(currentIndex = index, isFlipped = false)
        }
    }

    fun markCorrect() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val updated = card.withReview(correct = true)
        viewModelScope.launch {
            repository.updateFlashcard(updated)
            allCardsRaw = allCardsRaw.map { if (it.id == card.id) updated else it }
            val newFiltered = applyFilterAndSort(allCardsRaw, state.selectedFilter, state.selectedSortMode)
            _uiState.value = state.copy(
                allCards = allCardsRaw,
                filteredCards = newFiltered,
                correctCount = state.correctCount + 1,
                reviewedCardIds = state.reviewedCardIds + card.id,
                sessionSummary = state.sessionSummary.copy(
                    remembered = state.sessionSummary.remembered + 1
                )
            )
        }
    }

    fun markIncorrect() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val updated = card.withReview(correct = false)
        viewModelScope.launch {
            repository.updateFlashcard(updated)
            allCardsRaw = allCardsRaw.map { if (it.id == card.id) updated else it }
            val newFiltered = applyFilterAndSort(allCardsRaw, state.selectedFilter, state.selectedSortMode)
            _uiState.value = state.copy(
                allCards = allCardsRaw,
                filteredCards = newFiltered,
                wrongCount = state.wrongCount + 1,
                wrongCardIds = state.wrongCardIds + card.id,
                reviewedCardIds = state.reviewedCardIds + card.id,
                sessionSummary = state.sessionSummary.copy(
                    needReview = state.sessionSummary.needReview + 1,
                    wrongCardCount = state.wrongCardIds.size + 1
                )
            )
        }
    }

    fun toggleStar() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val updated = card.copy(isStarred = !card.isStarred)
        viewModelScope.launch {
            repository.updateFlashcard(updated)
            allCardsRaw = allCardsRaw.map { if (it.id == card.id) updated else it }
            val newFiltered = applyFilterAndSort(allCardsRaw, state.selectedFilter, state.selectedSortMode)
            val newStarredCount = allCardsRaw.count { it.isStarred }
            _uiState.value = state.copy(
                allCards = allCardsRaw,
                filteredCards = newFiltered,
                sessionSummary = state.sessionSummary.copy(starredCount = newStarredCount)
            )
        }
    }

    fun setFilter(filter: FlashcardFilter) {
        val state = _uiState.value
        val newFiltered = applyFilterAndSort(allCardsRaw, filter, state.selectedSortMode)
        _uiState.value = state.copy(
            selectedFilter = filter,
            filteredCards = newFiltered,
            currentIndex = 0,
            isFlipped = false,
            sessionSummary = state.sessionSummary.copy(totalCards = newFiltered.size)
        )
    }

    fun setSortMode(sortMode: FlashcardSortMode) {
        val state = _uiState.value
        val newFiltered = applyFilterAndSort(allCardsRaw, state.selectedFilter, sortMode)
        val isShuffled = sortMode == FlashcardSortMode.RANDOM
        _uiState.value = state.copy(
            selectedSortMode = sortMode,
            filteredCards = newFiltered,
            isShuffled = isShuffled,
            currentIndex = 0,
            isFlipped = false
        )
    }

    fun shuffleCards() {
        setSortMode(FlashcardSortMode.RANDOM)
    }

    fun resetOrder() {
        setSortMode(FlashcardSortMode.ORIGINAL)
    }

    fun dismissSessionSummary() {
        _uiState.value = _uiState.value.copy(showSessionSummary = false)
    }

    fun restartSession() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentIndex = 0,
            isFlipped = false,
            correctCount = 0,
            wrongCount = 0,
            wrongCardIds = emptySet(),
            reviewedCardIds = emptySet(),
            showSessionSummary = false,
            sessionSummary = FlashcardSessionSummary(
                totalCards = state.filteredCards.size,
                starredCount = allCardsRaw.count { it.isStarred }
            )
        )
    }

    /**
     * Bắt đầu phiên học chỉ với các thẻ đã đánh dấu sai.
     * Tạo filtered list từ wrongCardIds và bắt đầu session mới.
     */
    fun reviewWrongCards() {
        val state = _uiState.value
        if (state.wrongCardIds.isEmpty()) return

        val wrongCards = allCardsRaw.filter { it.id in state.wrongCardIds }
        if (wrongCards.isEmpty()) return

        _uiState.value = state.copy(
            filteredCards = wrongCards,
            currentIndex = 0,
            isFlipped = false,
            correctCount = 0,
            wrongCount = 0,
            wrongCardIds = emptySet(),
            reviewedCardIds = emptySet(),
            showSessionSummary = false,
            sessionSummary = FlashcardSessionSummary(
                totalCards = wrongCards.size,
                starredCount = wrongCards.count { it.isStarred }
            )
        )
    }

    private fun applyFilterAndSort(
        cards: List<FlashcardEntity>,
        filter: FlashcardFilter,
        sortMode: FlashcardSortMode
    ): List<FlashcardEntity> {
        var result = when (filter) {
            FlashcardFilter.ALL -> cards
            FlashcardFilter.STARRED -> cards.filter { it.isStarred }
            FlashcardFilter.NEED_REVIEW -> cards.filter { it.masteryLevel < 3 }
            FlashcardFilter.MASTERED -> cards.filter { it.masteryLevel >= 4 }
            FlashcardFilter.UNMASTERED -> cards.filter { it.masteryLevel < 4 }
        }

        result = when (sortMode) {
            FlashcardSortMode.ORIGINAL -> result
            FlashcardSortMode.RANDOM -> result.shuffled()
            FlashcardSortMode.NEED_REVIEW_FIRST -> result.sortedBy { it.masteryLevel }
            FlashcardSortMode.STARRED_FIRST -> result.sortedByDescending { it.isStarred }
        }

        return result
    }

    private fun checkSessionComplete(currentIdx: Int, total: Int) {
        if (currentIdx == total - 1) {
            val state = _uiState.value
            _uiState.value = state.copy(
                showSessionSummary = true,
                sessionSummary = state.sessionSummary.copy(
                    endTime = System.currentTimeMillis(),
                    wrongCardCount = state.wrongCardIds.size
                )
            )
        }
    }
}
