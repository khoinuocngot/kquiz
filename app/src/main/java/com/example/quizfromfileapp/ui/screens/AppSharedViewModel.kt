package com.example.quizfromfileapp.ui.screens

import androidx.lifecycle.ViewModel
import com.example.quizfromfileapp.data.model.ImportedFile
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.domain.model.QuizGenerationMode
import com.example.quizfromfileapp.domain.model.QuizSession
import com.example.quizfromfileapp.quizgenerator.LlmGenerationUiState
import com.example.quizfromfileapp.quizgenerator.LlmStateCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSharedViewModel : ViewModel(), LlmStateCallback {

    private val _selectedFile = MutableStateFlow<ImportedFile?>(null)
    val selectedFile: StateFlow<ImportedFile?> = _selectedFile.asStateFlow()

    private val _extractedContent = MutableStateFlow<ExtractedContent?>(null)
    val extractedContent: StateFlow<ExtractedContent?> = _extractedContent.asStateFlow()

    private val _quizConfig = MutableStateFlow(QuizConfig())
    val quizConfig: StateFlow<QuizConfig> = _quizConfig.asStateFlow()

    private val _quizSession = MutableStateFlow<QuizSession?>(null)
    val quizSession: StateFlow<QuizSession?> = _quizSession.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // ── LLM Generation State ──────────────────────────────
    private val _llmGenerationState = MutableStateFlow<LlmGenerationState>(LlmGenerationState.Idle)
    val llmGenerationState: StateFlow<LlmGenerationState> = _llmGenerationState.asStateFlow()

    private val _generationMode = MutableStateFlow(QuizGenerationMode.DEFAULT)
    val generationMode: StateFlow<QuizGenerationMode> = _generationMode.asStateFlow()

    // ── LLM metadata ──────────────────────────────
    private val _llmErrorMessage = MutableStateFlow<String?>(null)
    val llmErrorMessage: StateFlow<String?> = _llmErrorMessage.asStateFlow()

    private val _usedFallback = MutableStateFlow(false)
    val usedFallback: StateFlow<Boolean> = _usedFallback.asStateFlow()

    private val _llmProgressMessage = MutableStateFlow<String?>(null)
    val llmProgressMessage: StateFlow<String?> = _llmProgressMessage.asStateFlow()

    fun setSelectedFile(file: ImportedFile) {
        _selectedFile.value = file
    }

    fun clearSelectedFile() {
        _selectedFile.value = null
    }

    fun setExtractedContent(content: ExtractedContent) {
        _extractedContent.value = content
    }

    fun clearExtractedContent() {
        _extractedContent.value = null
    }

    fun setQuizConfig(config: QuizConfig) {
        _quizConfig.value = config
        _generationMode.value = config.generationMode
    }

    fun clearQuizConfig() {
        _quizConfig.value = QuizConfig()
        _generationMode.value = QuizGenerationMode.DEFAULT
    }

    fun setQuizSession(session: QuizSession) {
        _quizSession.value = session
    }

    fun clearQuizSession() {
        _quizSession.value = null
    }

    fun setUserAnswer(questionIndex: Int, answerIndex: Int) {
        _userAnswers.value = _userAnswers.value + (questionIndex to answerIndex)
    }

    fun clearUserAnswers() {
        _userAnswers.value = emptyMap()
    }

    fun setCurrentQuestionIndex(index: Int) {
        _currentQuestionIndex.value = index
    }

    // ── LLM State Methods ──────────────────────────────

    fun setLlmGenerating() {
        _llmGenerationState.value = LlmGenerationState.Generating
    }

    fun setLlmSuccess(questionCount: Int) {
        _llmGenerationState.value = LlmGenerationState.Success(questionCount)
    }

    fun setLlmFailed(reason: String) {
        _llmGenerationState.value = LlmGenerationState.Failed(reason)
    }

    fun setLlmFallbackUsed(questionCount: Int) {
        _llmGenerationState.value = LlmGenerationState.FallbackUsed(questionCount)
    }

    fun clearLlmState() {
        _llmGenerationState.value = LlmGenerationState.Idle
        _llmErrorMessage.value = null
        _usedFallback.value = false
        _llmProgressMessage.value = null
    }

    fun setGenerationMode(mode: QuizGenerationMode) {
        _generationMode.value = mode
        _quizConfig.value = _quizConfig.value.copy(generationMode = mode)
    }

    // ── LlmStateCallback implementation ──────────────────────────────

    override fun onLlmStateChanged(state: LlmGenerationUiState) {
        when (state) {
            is LlmGenerationUiState.Idle -> {
                _llmGenerationState.value = LlmGenerationState.Idle
                _llmProgressMessage.value = null
            }

            is LlmGenerationUiState.CheckingAvailability -> {
                _llmGenerationState.value = LlmGenerationState.Generating
                _llmProgressMessage.value = "Đang kiểm tra local AI..."
                _llmErrorMessage.value = null
                _usedFallback.value = false
            }

            is LlmGenerationUiState.Generating -> {
                _llmGenerationState.value = LlmGenerationState.Generating
                _llmProgressMessage.value = state.message
            }

            is LlmGenerationUiState.LlmSuccess -> {
                _llmGenerationState.value = LlmGenerationState.Success(state.questionCount)
                _llmProgressMessage.value = state.message
                _llmErrorMessage.value = null
                _usedFallback.value = false
            }

            is LlmGenerationUiState.LlmSuccessPartial -> {
                _llmGenerationState.value = LlmGenerationState.Success(state.llmCount)
                _llmProgressMessage.value = state.message
                _llmErrorMessage.value = null
                _usedFallback.value = false
            }

            is LlmGenerationUiState.FallbackTriggered -> {
                _llmGenerationState.value = LlmGenerationState.FallbackUsed(-1)
                _llmErrorMessage.value = state.reason
                _llmProgressMessage.value = "Fallback: ${state.reason}"
                _usedFallback.value = true
            }
        }
    }

    fun clearAll() {
        _selectedFile.value = null
        _extractedContent.value = null
        _quizConfig.value = QuizConfig()
        _quizSession.value = null
        _userAnswers.value = emptyMap()
        _currentQuestionIndex.value = 0
        _llmGenerationState.value = LlmGenerationState.Idle
        _generationMode.value = QuizGenerationMode.DEFAULT
        _llmErrorMessage.value = null
        _usedFallback.value = false
        _llmProgressMessage.value = null
    }
}

/**
 * Trạng thái LLM generation — dùng cho UI hiển thị loading/fallback.
 */
sealed class LlmGenerationState {
    /** Chưa bắt đầu generation */
    data object Idle : LlmGenerationState()

    /** Đang sinh quiz (LLM hoặc rule-based) */
    data object Generating : LlmGenerationState()

    /** LLM thành công — hiển thị số câu hỏi */
    data class Success(val questionCount: Int) : LlmGenerationState()

    /** LLM thất bại — hiển thị lý do */
    data class Failed(val reason: String) : LlmGenerationState()

    /**
     * Đã fallback sang rule-based.
     * questionCount = -1 có nghĩa là chưa biết (sẽ update sau).
     */
    data class FallbackUsed(val questionCount: Int) : LlmGenerationState()
}
