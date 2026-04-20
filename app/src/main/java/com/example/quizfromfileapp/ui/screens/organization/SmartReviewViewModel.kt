package com.example.quizfromfileapp.ui.screens.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom
import com.example.quizfromfileapp.data.repository.OrganizationRepository
import com.example.quizfromfileapp.data.repository.StudySetRepository
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SmartReviewUiState(
    val cards: List<FlashcardEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val reviewedCardIds: Set<Long> = emptySet(),
    val wrongCardIds: Set<Long> = emptySet(),
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isSessionComplete: Boolean = false,
    val sessionStartTime: Long = 0L
) {
    val currentCard: FlashcardEntity?
        get() = cards.getOrNull(currentIndex)

    val progress: Float
        get() = if (cards.isEmpty()) 0f else (currentIndex + 1).toFloat() / cards.size

    val isFirst: Boolean
        get() = currentIndex == 0

    val isLast: Boolean
        get() = currentIndex == cards.size - 1

    val reviewedCount: Int
        get() = reviewedCardIds.size

    val sessionScorePercent: Int
        get() {
            val total = correctCount + wrongCount
            return if (total == 0) 0 else (correctCount * 100) / total
        }

    val cardsToReviewCount: Int
        get() = wrongCardIds.size
}

class SmartReviewViewModel : ViewModel() {

    private val orgRepo: OrganizationRepository = AppContainer.organizationRepository
    private val studySetRepo: StudySetRepository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(SmartReviewUiState())
    val uiState: StateFlow<SmartReviewUiState> = _uiState.asStateFlow()

    init {
        loadPriorityCards()
    }

    private fun loadPriorityCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Get all study sets
            val studySets = studySetRepo.getAllStudySets().first()

            // Collect priority cards from all sets
            val priorityCards = mutableListOf<FlashcardEntity>()
            for (set in studySets) {
                val cards = studySetRepo.getFlashcardsByStudySetId(set.id)
                // Filter: only cards that need review (mastery < 3 or never reviewed)
                val needsReview = cards.filter { it.masteryLevel < 3 || it.timesReviewed == 0 }
                // Sort by priority: mastery asc, then by lastReviewedAt
                val sorted = needsReview.sortedWith(
                    compareBy<FlashcardEntity> { it.masteryLevel }
                        .thenBy { it.lastReviewedAt ?: 0L }
                )
                priorityCards.addAll(sorted)
            }

            // Limit to 20 cards max
            val selected = priorityCards.take(20)

            _uiState.value = _uiState.value.copy(
                cards = selected,
                isLoading = false,
                currentIndex = 0,
                isFlipped = false,
                reviewedCardIds = emptySet(),
                wrongCardIds = emptySet(),
                correctCount = 0,
                wrongCount = 0,
                isSessionComplete = false,
                sessionStartTime = System.currentTimeMillis()
            )
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.currentIndex < state.cards.size - 1) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1, isFlipped = false)
        } else {
            _uiState.value = state.copy(isSessionComplete = true)
        }
    }

    fun prevCard() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1, isFlipped = false)
        }
    }

    fun goToCard(index: Int) {
        if (index in _uiState.value.cards.indices) {
            _uiState.value = _uiState.value.copy(currentIndex = index, isFlipped = false)
        }
    }

    fun markCorrect() {
        val state = _uiState.value
        val card = state.currentCard ?: return

        viewModelScope.launch {
            // Update mastery
            val updated = card.withReview(correct = true)
            studySetRepo.updateFlashcard(updated)
        }

        _uiState.value = state.copy(
            reviewedCardIds = state.reviewedCardIds + card.id,
            correctCount = state.correctCount + 1
        )
        nextCard()
    }

    fun markIncorrect() {
        val state = _uiState.value
        val card = state.currentCard ?: return

        viewModelScope.launch {
            val updated = card.withReview(correct = false)
            studySetRepo.updateFlashcard(updated)
        }

        _uiState.value = state.copy(
            reviewedCardIds = state.reviewedCardIds + card.id,
            wrongCardIds = state.wrongCardIds + card.id,
            wrongCount = state.wrongCount + 1
        )
        nextCard()
    }

    fun restartSession() {
        loadPriorityCards()
    }

    fun recordSession() {
        val state = _uiState.value
        if (state.reviewedCount > 0) {
            val sessionTimeMs = System.currentTimeMillis() - state.sessionStartTime
            viewModelScope.launch {
                orgRepo.recordStudySession(
                    cardsReviewed = state.reviewedCount,
                    studyTimeMs = sessionTimeMs
                )
            }
        }
    }
}
