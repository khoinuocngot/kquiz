package com.example.quizfromfileapp.ui.screens.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.domain.usecase.GenerateQuizUseCase
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import com.example.quizfromfileapp.ui.screens.LlmGenerationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuizUiState {
    data object Loading : QuizUiState()
    data class Ready(val session: com.example.quizfromfileapp.domain.model.QuizSession) : QuizUiState()
    data class Error(val message: String) : QuizUiState()
}

class QuizViewModel(
    application: Application,
    private val sharedViewModel: AppSharedViewModel
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    val llmGenerationState: StateFlow<LlmGenerationState> = sharedViewModel.llmGenerationState
    val llmProgressMessage: StateFlow<String?> = sharedViewModel.llmProgressMessage
    val llmErrorMessage: StateFlow<String?> = sharedViewModel.llmErrorMessage

    fun generateQuiz() {
        val content = sharedViewModel.extractedContent.value
        val config = sharedViewModel.quizConfig.value

        if (content == null) {
            _uiState.value = QuizUiState.Error("Chưa có nội dung")
            return
        }

        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            try {
                val useCase = GenerateQuizUseCase(
                    context = getApplication<Application>(),
                    callback = sharedViewModel
                )
                val session = useCase.generate(content, config)
                sharedViewModel.setQuizSession(session)
                sharedViewModel.clearUserAnswers()
                sharedViewModel.setCurrentQuestionIndex(0)
                _uiState.value = QuizUiState.Ready(session)
            } catch (e: Exception) {
                _uiState.value = QuizUiState.Error("Lỗi khi tạo quiz: ${e.message}")
            }
        }
    }

    fun selectAnswer(questionIndex: Int, answerIndex: Int) {
        sharedViewModel.setUserAnswer(questionIndex, answerIndex)
    }

    fun goToQuestion(index: Int) {
        val session = (_uiState.value as? QuizUiState.Ready)?.session ?: return
        if (index in session.questions.indices) {
            sharedViewModel.setCurrentQuestionIndex(index)
        }
    }

    fun nextQuestion() {
        val session = (_uiState.value as? QuizUiState.Ready)?.session ?: return
        val current = sharedViewModel.currentQuestionIndex.value
        if (current < session.questions.size - 1) {
            sharedViewModel.setCurrentQuestionIndex(current + 1)
        }
    }

    fun previousQuestion() {
        val current = sharedViewModel.currentQuestionIndex.value
        if (current > 0) {
            sharedViewModel.setCurrentQuestionIndex(current - 1)
        }
    }

    fun getUserAnswer(questionIndex: Int): Int? {
        return sharedViewModel.userAnswers.value[questionIndex]
    }

    fun getCurrentQuestion(): com.example.quizfromfileapp.domain.model.QuizQuestion? {
        val session = (_uiState.value as? QuizUiState.Ready)?.session ?: return null
        val index = sharedViewModel.currentQuestionIndex.value
        return session.questions.getOrNull(index)
    }

    fun getScore(): Pair<Int, Int> {
        val session = (_uiState.value as? QuizUiState.Ready)?.session ?: return (0 to 0)
        val answers = sharedViewModel.userAnswers.value
        var correct = 0
        session.questions.forEachIndexed { index, question ->
            if (answers[index] == question.correctAnswerIndex) {
                correct++
            }
        }
        return correct to session.questions.size
    }
}
