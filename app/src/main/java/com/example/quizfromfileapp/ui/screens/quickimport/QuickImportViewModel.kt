package com.example.quizfromfileapp.ui.screens.quickimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.data.quickimport.ParseResult
import com.example.quizfromfileapp.data.quickimport.QuickImportConfig
import com.example.quizfromfileapp.data.quickimport.QuickImportParser
import com.example.quizfromfileapp.data.quickimport.StudySetType
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuickImportUiState(
    val title: String = "",
    val description: String = "",
    val rawText: String = "",
    val termDefDelimiter: QuickImportConfig.TermDefDelimiter = QuickImportConfig.TermDefDelimiter.TAB,
    val cardDelimiterMode: QuickImportConfig.CardDelimiterMode = QuickImportConfig.CardDelimiterMode.ONE_PER_LINE,
    val cardDelimiterCustom: String = "",
    val studySetType: StudySetType = StudySetType.MULTIPLE_CHOICE_BANK,
    val parseResult: ParseResult? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val createdStudySetId: Long? = null,
    val errorMessage: String? = null
) {
    val canCreate: Boolean
        get() = title.isNotBlank() && rawText.isNotBlank() &&
                (parseResult?.validCount ?: 0) > 0

    val hasInvalidLines: Boolean
        get() = (parseResult?.invalidCount ?: 0) > 0

    val invalidWarning: String?
        get() {
            val count = parseResult?.invalidCount ?: 0
            return if (count > 0) "Có $count dòng không hợp lệ và sẽ không được nhập" else null
        }
}

class QuickImportViewModel : ViewModel() {

    private val repository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(QuickImportUiState())
    val uiState: StateFlow<QuickImportUiState> = _uiState.asStateFlow()

    private val _snackbarMsg = MutableStateFlow<String?>(null)
    val snackbarMsg: StateFlow<String?> = _snackbarMsg.asStateFlow()

    private var parseJob: Job? = null

    // Debounce parse — chỉ parse khi user ngừng gõ 400ms
    private fun scheduleParse() {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            delay(400)
            performParse()
        }
    }

    private fun performParse() {
        val state = _uiState.value
        val config = buildConfig(state)
        val result = QuickImportParser.parse(config)
        _uiState.value = state.copy(parseResult = result, errorMessage = null)
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    /**
     * Mỗi khi rawText thay đổi → auto-parse sau debounce 400ms.
     * KHÔNG chặn UI, KHÔNG parse trên main thread cho text rất dài.
     */
    fun updateRawText(text: String) {
        _uiState.value = _uiState.value.copy(rawText = text)
        if (text.isNotBlank()) {
            scheduleParse()
        } else {
            // Xóa hết preview khi text trống
            _uiState.value = _uiState.value.copy(
                parseResult = ParseResult(
                    validCards = emptyList(),
                    invalidLines = emptyList(),
                    totalLines = 0,
                    rawLineCount = 0,
                    rawCharCount = 0
                )
            )
        }
    }

    fun updateTermDefDelimiter(delimiter: QuickImportConfig.TermDefDelimiter) {
        _uiState.value = _uiState.value.copy(termDefDelimiter = delimiter)
        if (_uiState.value.rawText.isNotBlank()) {
            scheduleParse()
        }
    }

    fun updateCardDelimiterMode(mode: QuickImportConfig.CardDelimiterMode) {
        _uiState.value = _uiState.value.copy(cardDelimiterMode = mode)
        if (_uiState.value.rawText.isNotBlank()) {
            scheduleParse()
        }
    }

    fun updateCardDelimiterCustom(custom: String) {
        _uiState.value = _uiState.value.copy(cardDelimiterCustom = custom)
        if (_uiState.value.rawText.isNotBlank()) {
            scheduleParse()
        }
    }

    fun updateStudySetType(type: StudySetType) {
        _uiState.value = _uiState.value.copy(studySetType = type)
    }

    fun importStudySet() {
        val state = _uiState.value

        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vui lòng nhập tiêu đề bộ học")
            return
        }

        if (state.rawText.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vui lòng nhập nội dung")
            return
        }

        val result = state.parseResult
        if (result == null || result.validCount == 0) {
            _uiState.value = state.copy(errorMessage = "Không có thẻ hợp lệ nào để tạo")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val studySet = StudySetEntity(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    cardCount = result.validCount,
                    sourceType = StudySetEntity.SOURCE_TYPE_QUICK_IMPORT,
                    sourceFileName = "",
                    studySetType = when (state.studySetType) {
                        StudySetType.TERM_DEFINITION -> StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION
                        StudySetType.QUESTION_ANSWER -> StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
                        StudySetType.MULTIPLE_CHOICE_BANK -> StudySetEntity.STUDY_SET_TYPE_MULTIPLE_CHOICE
                    }
                )
                val studySetId = repository.createStudySet(studySet)

                val flashcards = result.validCards.map { parsed ->
                    val choicesJson = if (parsed.choices.isNotEmpty()) {
                        parsed.choices.toJsonString()
                    } else ""

                    FlashcardEntity(
                        studySetId = studySetId,
                        term = parsed.term,
                        definition = parsed.definition,
                        sourceSnippet = parsed.rawLine,
                        choices = choicesJson,
                        correctChoiceIndex = parsed.correctChoiceIndex
                    )
                }
                repository.addFlashcards(flashcards)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    createdStudySetId = studySetId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi khi tạo bộ học: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun reset() {
        parseJob?.cancel()
        _uiState.value = QuickImportUiState()
        _snackbarMsg.value = null
    }

    fun showSnackbar(msg: String) {
        _snackbarMsg.value = msg
    }

    fun clearSnackbar() {
        _snackbarMsg.value = null
    }

    private fun buildConfig(state: QuickImportUiState): QuickImportConfig {
        return QuickImportConfig(
            title = state.title,
            description = state.description,
            rawText = state.rawText,
            termDefDelimiter = state.termDefDelimiter,
            cardDelimiterMode = state.cardDelimiterMode,
            cardDelimiterCustom = state.cardDelimiterCustom,
            studySetType = state.studySetType
        )
    }

    private fun List<String>.toJsonString(): String {
        return "[" + this.joinToString(",") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } + "]"
    }
}
