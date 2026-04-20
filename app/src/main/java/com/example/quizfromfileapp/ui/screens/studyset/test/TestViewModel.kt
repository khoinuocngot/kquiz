package com.example.quizfromfileapp.ui.screens.studyset.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Kiểu câu hỏi trong test.
 */
enum class TestQuestionType(val displayName: String, val compatibleWithQA: Boolean) {
    TERM_TO_DEFINITION(AppStringsVi.TestConfigQA, true),
    DEFINITION_TO_TERM(AppStringsVi.TestConfigAQ, false),
    MIXED(AppStringsVi.TestConfigMixed, false)
}

/**
 * Nguồn câu hỏi.
 */
enum class TestSource(val displayName: String) {
    ALL(AppStringsVi.TestConfigAll),
    STARRED(AppStringsVi.TestConfigStarred),
    NEED_REVIEW(AppStringsVi.TestConfigReview),
    UNMASTERED(AppStringsVi.TestConfigUnmastered)
}

/**
 * Hướng câu hỏi.
 */
enum class TestQuestionDirection(val displayName: String) {
    PROMPT_TO_ANSWER("Câu hỏi → Đáp án"),
    MIXED("Hỗn hợp")
}

/**
 * Cấu hình test nâng cao.
 */
data class TestConfig(
    val questionCount: Int = 10,
    val questionType: TestQuestionType = TestQuestionType.MIXED,
    val source: TestSource = TestSource.ALL,
    val randomize: Boolean = true,
    val questionDirection: TestQuestionDirection = TestQuestionDirection.MIXED,
    val timerEnabled: Boolean = false,
    val timerSeconds: Int = 300 // 5 phút mặc định
)

/**
 * Câu hỏi trong test (MCQ).
 */
data class TestQuestion(
    val cardId: Long,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val isTermQuestion: Boolean,
    val isQuestionAnswer: Boolean,
    val sourcePage: Int? = null,
    val sourceSnippet: String? = null,
    val explanation: String? = null
)

/**
 * Kết quả một câu hỏi trong review.
 */
data class QuestionResult(
    val question: TestQuestion,
    val selectedIndex: Int?,
    val isCorrect: Boolean
)

/**
 * Lịch sử một bài test.
 */
data class TestHistoryItem(
    val id: Long = 0,
    val studySetId: Long,
    val studySetTitle: String,
    val scorePercent: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val flaggedCount: Int,
    val sourceUsed: String,
    val timeUsedSeconds: Int,
    val timerEnabled: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Trạng thái UI của Test Mode nâng cao.
 */
data class TestUiState(
    val cards: List<FlashcardEntity> = emptyList(),
    val config: TestConfig = TestConfig(),
    val questions: List<TestQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<Int, Int> = emptyMap(), // questionIndex -> selectedOptionIndex
    val flaggedQuestions: Set<Long> = emptySet(), // các cardId được đánh dấu
    val isLoading: Boolean = true,
    val isSubmitted: Boolean = false,
    val isReviewMode: Boolean = false,
    val results: List<QuestionResult> = emptyList(),
    val studySetType: String = StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION,
    val studySetTitle: String = "",
    val remainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val showSubmitConfirmation: Boolean = false,
    val wrongQuestionIds: List<Long> = emptyList()
) {
    val scorePercent: Int
        get() {
            if (questions.isEmpty()) return 0
            val correct = answers.count { (qIdx, selectedIdx) ->
                questions[qIdx].correctIndex == selectedIdx
            }
            return (correct * 100) / questions.size
        }

    val correctCount: Int
        get() = answers.count { (qIdx, selectedIdx) ->
            questions[qIdx].correctIndex == selectedIdx
        }

    val unansweredCount: Int
        get() = questions.size - answers.size

    val answeredCount: Int
        get() = answers.size

    val unansweredIndices: List<Int>
        get() = questions.indices.filter { !answers.containsKey(it) }

    val flaggedCount: Int
        get() = flaggedQuestions.size

    val allAnswered: Boolean
        get() = answers.size == questions.size

    val wrongResults: List<QuestionResult>
        get() = results.filter { !it.isCorrect }

    val flaggedResults: List<QuestionResult>
        get() = results.filter { it.question.cardId in flaggedQuestions }

    val scoreMessage: String
        get() = when {
            scorePercent >= 90 -> AppStringsVi.TestScoreMsgExcellent
            scorePercent >= 80 -> AppStringsVi.TestScoreMsgGood
            scorePercent >= 60 -> AppStringsVi.TestScoreMsgMedium
            scorePercent >= 40 -> AppStringsVi.TestScoreMsgLow
            else -> AppStringsVi.TestScoreMsgVeryLow
        }

    val scoreColor: TestScoreLevel
        get() = when {
            scorePercent >= 80 -> TestScoreLevel.GOOD
            scorePercent >= 50 -> TestScoreLevel.MEDIUM
            else -> TestScoreLevel.POOR
        }
}

enum class TestScoreLevel { GOOD, MEDIUM, POOR }

class TestViewModel(
    private val studySetId: Long = 0L
) : ViewModel() {

    private val repository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var testStartTime: Long = 0L
    private var testStartSeconds: Int = 0

    fun loadTestConfig(studySetId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val studySet = repository.getStudySetById(studySetId)
            val cards = repository.getFlashcardsByStudySetId(studySetId)
            val studySetType = studySet?.studySetType ?: StudySetEntity.STUDY_SET_TYPE_TERM_DEFINITION
            val title = studySet?.title ?: ""
            _uiState.value = _uiState.value.copy(
                cards = cards,
                isLoading = false,
                studySetType = studySetType,
                studySetTitle = title
            )
        }
    }

    fun updateQuestionCount(count: Int) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(questionCount = count.coerceIn(2, 20))
        )
    }

    fun updateQuestionType(type: TestQuestionType) {
        val state = _uiState.value
        val isQA = state.studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
        val safeType = if (isQA && !type.compatibleWithQA) TestQuestionType.TERM_TO_DEFINITION else type
        _uiState.value = state.copy(config = state.config.copy(questionType = safeType))
    }

    fun updateSource(source: TestSource) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(source = source))
    }

    fun updateRandomize(randomize: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(randomize = randomize))
    }

    fun updateQuestionDirection(direction: TestQuestionDirection) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(questionDirection = direction))
    }

    fun updateTimerEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(timerEnabled = enabled)
        )
    }

    fun updateTimerSeconds(seconds: Int) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(timerSeconds = seconds.coerceIn(60, 3600))
        )
    }

    fun startTest() {
        val state = _uiState.value
        val sourceCards = getSourceCards(state.cards, state.config.source)
        val questions = generateTestQuestions(sourceCards, state.config, state.studySetType)

        testStartTime = System.currentTimeMillis()
        testStartSeconds = state.config.timerSeconds

        _uiState.value = state.copy(
            questions = questions,
            currentIndex = 0,
            answers = emptyMap(),
            flaggedQuestions = emptySet(),
            isSubmitted = false,
            isReviewMode = false,
            results = emptyList(),
            remainingSeconds = state.config.timerSeconds,
            isTimerRunning = state.config.timerEnabled,
            showSubmitConfirmation = false,
            wrongQuestionIds = emptyList()
        )

        if (state.config.timerEnabled) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && !_uiState.value.isSubmitted) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = _uiState.value.remainingSeconds - 1
                )
            }
            if (_uiState.value.remainingSeconds <= 0 && !_uiState.value.isSubmitted) {
                submitTest()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
    }

    private fun getTimeUsedSeconds(): Int {
        return testStartSeconds - _uiState.value.remainingSeconds
    }

    private fun getSourceCards(allCards: List<FlashcardEntity>, source: TestSource): List<FlashcardEntity> {
        return when (source) {
            TestSource.ALL -> allCards
            TestSource.STARRED -> allCards.filter { it.isStarred }
            TestSource.NEED_REVIEW -> allCards.filter { it.masteryLevel < 3 }
            TestSource.UNMASTERED -> allCards.filter { it.masteryLevel < 4 }
        }
    }

    private fun generateTestQuestions(
        cards: List<FlashcardEntity>,
        config: TestConfig,
        studySetType: String
    ): List<TestQuestion> {
        val pool = if (config.randomize) cards.shuffled() else cards
        val selected = pool.take(config.questionCount)
        val isQuestionAnswer = studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER

        return selected.flatMap { card ->
            if (isQuestionAnswer) {
                listOfNotNull(createTermQuestion(card, cards, isQuestionAnswer))
            } else {
                when (config.questionDirection) {
                    TestQuestionDirection.PROMPT_TO_ANSWER -> listOfNotNull(
                        createTermQuestion(card, cards, isQuestionAnswer)
                    )
                    TestQuestionDirection.MIXED -> listOfNotNull(
                        if ((0..1).random() == 0) createTermQuestion(card, cards, isQuestionAnswer)
                        else createDefQuestion(card, cards, isQuestionAnswer)
                    )
                }
            }
        }.take(config.questionCount)
    }

    private fun createTermQuestion(
        card: FlashcardEntity,
        allCards: List<FlashcardEntity>,
        isQuestionAnswer: Boolean
    ): TestQuestion? {
        val distractors = allCards
            .filter { it.id != card.id }
            .shuffled()
            .take(3)
            .map { it.definition }
        if (distractors.size < 3) return null
        val options = (distractors + card.definition).shuffled()
        return TestQuestion(
            cardId = card.id,
            questionText = card.term,
            options = options,
            correctIndex = options.indexOf(card.definition),
            isTermQuestion = true,
            isQuestionAnswer = isQuestionAnswer,
            sourcePage = card.sourcePageStart,
            sourceSnippet = card.sourceSnippet.takeIf { it.isNotBlank() },
            explanation = card.explanation.takeIf { it.isNotBlank() }
        )
    }

    private fun createDefQuestion(
        card: FlashcardEntity,
        allCards: List<FlashcardEntity>,
        isQuestionAnswer: Boolean
    ): TestQuestion? {
        val distractors = allCards
            .filter { it.id != card.id }
            .shuffled()
            .take(3)
            .map { it.term }
        if (distractors.size < 3) return null
        val options = (distractors + card.term).shuffled()
        return TestQuestion(
            cardId = card.id,
            questionText = card.definition,
            options = options,
            correctIndex = options.indexOf(card.term),
            isTermQuestion = false,
            isQuestionAnswer = isQuestionAnswer,
            sourcePage = card.sourcePageStart,
            sourceSnippet = card.sourceSnippet.takeIf { it.isNotBlank() },
            explanation = card.explanation.takeIf { it.isNotBlank() }
        )
    }

    fun selectOption(optionIndex: Int) {
        val state = _uiState.value
        if (state.isSubmitted) return
        _uiState.value = state.copy(
            answers = state.answers + (state.currentIndex to optionIndex)
        )
    }

    fun toggleFlag() {
        val state = _uiState.value
        val currentCardId = state.questions.getOrNull(state.currentIndex)?.cardId ?: return
        val newFlagged = if (currentCardId in state.flaggedQuestions) {
            state.flaggedQuestions - currentCardId
        } else {
            state.flaggedQuestions + currentCardId
        }
        _uiState.value = state.copy(flaggedQuestions = newFlagged)
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.questions.size - 1) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun prevQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun goToQuestion(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun requestSubmit() {
        val state = _uiState.value
        if (!state.allAnswered) {
            _uiState.value = state.copy(showSubmitConfirmation = true)
        } else {
            submitTest()
        }
    }

    fun dismissSubmitConfirmation() {
        _uiState.value = _uiState.value.copy(showSubmitConfirmation = false)
    }

    fun submitTest() {
        stopTimer()
        val state = _uiState.value
        val results = state.questions.mapIndexed { index, question ->
            val selected = state.answers[index]
            QuestionResult(
                question = question,
                selectedIndex = selected,
                isCorrect = selected == question.correctIndex
            )
        }
        val wrongIds = results.filter { !it.isCorrect }.map { it.question.cardId }

        _uiState.value = state.copy(
            isSubmitted = true,
            isReviewMode = true,
            results = results,
            wrongQuestionIds = wrongIds,
            showSubmitConfirmation = false
        )

        // Cập nhật mastery cho các thẻ
        viewModelScope.launch {
            results.forEach { result ->
                val card = state.cards.find { it.id == result.question.cardId }
                if (card != null) {
                    val updated = card.withReview(correct = result.isCorrect)
                    repository.updateFlashcard(updated)
                }
            }
        }
    }

    fun retakeTest() {
        startTest()
    }

    fun getCardById(cardId: Long): FlashcardEntity? {
        return _uiState.value.cards.find { it.id == cardId }
    }

    fun getTestHistoryItem(): TestHistoryItem? {
        val state = _uiState.value
        if (!state.isSubmitted) return null
        return TestHistoryItem(
            studySetId = studySetId,
            studySetTitle = state.studySetTitle,
            scorePercent = state.scorePercent,
            correctCount = state.correctCount,
            totalQuestions = state.questions.size,
            wrongCount = state.wrongResults.size,
            unansweredCount = state.unansweredCount,
            flaggedCount = state.flaggedCount,
            sourceUsed = state.config.source.displayName,
            timeUsedSeconds = getTimeUsedSeconds(),
            timerEnabled = state.config.timerEnabled
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun provideFactory(studySetId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TestViewModel(studySetId) as T
                }
            }
        }
    }
}
