package com.example.quizfromfileapp.ui.screens.studyset.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.repository.StudySetRepository
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardEditUiState(
    val card: FlashcardEntity? = null,
    val term: String = "",
    val definition: String = "",
    val explanation: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

class CardEditViewModel : ViewModel() {

    private val repository: StudySetRepository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(CardEditUiState())
    val uiState: StateFlow<CardEditUiState> = _uiState.asStateFlow()

    private var studySetId: Long = -1

    fun loadCard(studySetId: Long, cardId: Long) {
        this.studySetId = studySetId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val card = repository.getFlashcardById(cardId)
            if (card != null) {
                _uiState.value = CardEditUiState(
                    card = card,
                    term = card.term,
                    definition = card.definition,
                    explanation = card.explanation,
                    isLoading = false
                )
            } else {
                _uiState.value = CardEditUiState(
                    card = null,
                    isLoading = false
                )
            }
        }
    }

    fun updateTerm(value: String) {
        _uiState.value = _uiState.value.copy(term = value)
    }

    fun updateDefinition(value: String) {
        _uiState.value = _uiState.value.copy(definition = value)
    }

    fun updateExplanation(value: String) {
        _uiState.value = _uiState.value.copy(explanation = value)
    }

    fun saveCard() {
        val card = _uiState.value.card ?: return
        val state = _uiState.value

        if (state.term.isBlank()) {
            _uiState.value = state.copy(error = "Thuật ngữ không được để trống")
            return
        }
        if (state.definition.isBlank()) {
            _uiState.value = state.copy(error = "Định nghĩa không được để trống")
            return
        }

        viewModelScope.launch {
            try {
                val updated = card.copy(
                    term = state.term.trim(),
                    definition = state.definition.trim(),
                    explanation = state.explanation.trim()
                )
                repository.updateFlashcard(updated)
                _uiState.value = state.copy(isSaved = true)
            } catch (e: Exception) {
                _uiState.value = state.copy(error = "Lưu thất bại: ${e.message}")
            }
        }
    }
}
