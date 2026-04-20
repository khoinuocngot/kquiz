package com.example.quizfromfileapp.ui.screens.studyset.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearnQuestion(
    val cardId: Long,
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>,
    val correctIndex: Int,
    val isTermQuestion: Boolean,
    val isQuestionAnswer: Boolean,
    val isMultipleChoice: Boolean = false
)

data class LearnUiState(
    val cards: List<FlashcardEntity> = emptyList(),
    val questions: List<LearnQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val selectedOption: Int? = null,
    val isAnswered: Boolean = false,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val wrongCardIds: Set<Long> = emptySet(),
    val wrongCardIndices: Set<Int> = emptySet(),
    val isSessionComplete: Boolean = false,
    val studySetType: String = StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION
) {
    val scorePercent: Int
        get() {
            val total = correctCount + wrongCount
            if (total == 0) return 0
            return (correctCount * 100) / total
        }
    val totalAnswered: Int get() = correctCount + wrongCount
}

class LearnViewModel : ViewModel() {

    private val repository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private var studySetId: Long = 0L

    fun loadStudySet(studySetId: Long) {
        this.studySetId = studySetId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val studySet = repository.getStudySetById(studySetId)
            val cards = repository.getFlashcardsByStudySetId(studySetId)
            val studySetType = studySet?.studySetType ?: StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION
            if (cards.size >= 4) {
                val questions = generateQuestions(cards, studySetType)
                _uiState.value = LearnUiState(
                    cards = cards,
                    questions = questions,
                    isLoading = false,
                    studySetType = studySetType
                )
            } else {
                _uiState.value = LearnUiState(
                    cards = cards,
                    isLoading = false,
                    studySetType = studySetType
                )
            }
        }
    }

    private fun generateQuestions(cards: List<FlashcardEntity>, studySetType: String): List<LearnQuestion> {
        val sorted = cards.sortedBy { it.masteryLevel }
        val selected = sorted.take(minOf(cards.size, cards.size)).shuffled()
        val isQuestionAnswer = studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER

        return selected.flatMap { card: FlashcardEntity ->
            when {
                card.isMultipleChoice -> {
                    listOfNotNull(createMcqQuestion(card))
                }
                isQuestionAnswer -> {
                    listOfNotNull(createQATermQuestion(card, cards, isQuestionAnswer))
                }
                else -> {
                    listOfNotNull(
                        createQATermQuestion(card, cards, isQuestionAnswer),
                        createQADefQuestion(card, cards, isQuestionAnswer)
                    )
                }
            }
        }.shuffled()
    }

    private fun createMcqQuestion(card: FlashcardEntity): LearnQuestion? {
        val choices = try {
            parseJsonChoices(card.choices)
        } catch (_: Exception) {
            return null
        }
        if (choices.size < 2) return null

        val correctIndex = card.correctChoiceIndex.coerceIn(0, choices.size - 1)
        return LearnQuestion(
            cardId = card.id,
            questionText = card.term,
            correctAnswer = choices.getOrNull(correctIndex) ?: "",
            options = choices,
            correctIndex = correctIndex,
            isTermQuestion = true,
            isQuestionAnswer = false,
            isMultipleChoice = true
        )
    }

    private fun parseJsonChoices(choicesJson: String): List<String> {
        val trimmed = choicesJson.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val content = trimmed.substring(1, trimmed.length - 1)
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        var inQuote = false
        var escaped = false

        for (ch in content) {
            when {
                escaped -> {
                    current.append(ch)
                    escaped = false
                }
                ch == '\\' && inQuote -> {
                    escaped = true
                }
                ch == '"' -> {
                    inQuote = !inQuote
                }
                ch == '[' || ch == '{' -> {
                    if (inQuote) current.append(ch) else depth++
                }
                ch == ']' || ch == '}' -> {
                    if (inQuote) current.append(ch) else depth--
                }
                ch == ',' && !inQuote && depth == 0 -> {
                    result.add(current.toString().trim().trim('"').trim())
                    current = StringBuilder()
                }
                else -> {
                    if (inQuote || depth > 0) current.append(ch)
                }
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim().trim('"').trim())
        }
        return result.filter { it.isNotBlank() }
    }

    private fun createQATermQuestion(
        card: FlashcardEntity,
        allCards: List<FlashcardEntity>,
        isQuestionAnswer: Boolean
    ): LearnQuestion? {
        val distractors = allCards.filter { it.id != card.id }.shuffled().take(3).map { it.definition }
        if (distractors.size < 3) return null
        val options = (distractors + card.definition).shuffled()
        return LearnQuestion(
            cardId = card.id,
            questionText = card.term,
            correctAnswer = card.definition,
            options = options,
            correctIndex = options.indexOf(card.definition),
            isTermQuestion = true,
            isQuestionAnswer = isQuestionAnswer,
            isMultipleChoice = false
        )
    }

    private fun createQADefQuestion(
        card: FlashcardEntity,
        allCards: List<FlashcardEntity>,
        isQuestionAnswer: Boolean
    ): LearnQuestion? {
        val distractors = allCards.filter { it.id != card.id }.shuffled().take(3).map { it.term }
        if (distractors.size < 3) return null
        val options = (distractors + card.term).shuffled()
        return LearnQuestion(
            cardId = card.id,
            questionText = card.definition,
            correctAnswer = card.term,
            options = options,
            correctIndex = options.indexOf(card.term),
            isTermQuestion = false,
            isQuestionAnswer = isQuestionAnswer,
            isMultipleChoice = false
        )
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        if (state.isAnswered) return
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = index == question.correctIndex
        viewModelScope.launch {
            val card = state.cards.find { it.id == question.cardId }
            if (card != null) {
                repository.updateFlashcard(card.withReview(isCorrect))
            }
        }
        val newWrongCardIds = if (!isCorrect) state.wrongCardIds + question.cardId else state.wrongCardIds
        val newWrongIndices = if (!isCorrect) state.wrongCardIndices + state.currentIndex else state.wrongCardIndices
        _uiState.value = state.copy(
            selectedOption = index,
            isAnswered = true,
            correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount,
            wrongCount = if (!isCorrect) state.wrongCount + 1 else state.wrongCount,
            wrongCardIds = newWrongCardIds,
            wrongCardIndices = newWrongIndices
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.questions.size - 1) {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                selectedOption = null,
                isAnswered = false
            )
        } else {
            _uiState.value = state.copy(isSessionComplete = true)
        }
    }

    fun restart() {
        val state = _uiState.value
        if (state.cards.size >= 4) {
            val questions = generateQuestions(state.cards, state.studySetType)
            _uiState.value = LearnUiState(
                cards = state.cards,
                questions = questions,
                isLoading = false,
                studySetType = state.studySetType
            )
        }
    }

    fun startWrongReviewMode() {
        val state = _uiState.value
        val wrongCards = state.cards.filter { it.id in state.wrongCardIds }
        if (wrongCards.isEmpty()) return

        val isQA = state.studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
        val wrongQuestions = wrongCards.flatMap { card: FlashcardEntity ->
            when {
                card.isMultipleChoice -> listOfNotNull(createMcqQuestion(card))
                else -> listOfNotNull(createQATermQuestion(card, wrongCards, isQA))
            }
        }.shuffled()

        if (wrongQuestions.isNotEmpty()) {
            _uiState.value = state.copy(
                questions = wrongQuestions,
                currentIndex = 0,
                selectedOption = null,
                isAnswered = false,
                correctCount = 0,
                wrongCount = 0,
                wrongCardIds = emptySet(),
                wrongCardIndices = emptySet(),
                isSessionComplete = false
            )
        }
    }

    fun exitWrongReviewMode() {
        val state = _uiState.value
        _uiState.value = state.copy(isSessionComplete = true)
    }
}
