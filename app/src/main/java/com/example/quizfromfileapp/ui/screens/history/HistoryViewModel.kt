package com.example.quizfromfileapp.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.domain.usecase.ClearQuizHistoryUseCase
import com.example.quizfromfileapp.domain.usecase.DeleteQuizHistoryUseCase
import com.example.quizfromfileapp.domain.usecase.GetQuizHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<QuizHistoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

class HistoryViewModel : ViewModel() {
    private val getUseCase: GetQuizHistoryUseCase = AppContainer.getQuizHistoryUseCase
    private val deleteUseCase: DeleteQuizHistoryUseCase = AppContainer.deleteQuizHistoryUseCase
    private val clearUseCase: ClearQuizHistoryUseCase = AppContainer.clearQuizHistoryUseCase

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getUseCase().collect { items ->
                _uiState.value = HistoryUiState(items = items, isLoading = false)
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            deleteUseCase(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            clearUseCase()
        }
    }
}
