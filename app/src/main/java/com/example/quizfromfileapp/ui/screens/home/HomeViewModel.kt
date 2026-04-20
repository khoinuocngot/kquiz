package com.example.quizfromfileapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.data.repository.GamificationRepository
import com.example.quizfromfileapp.data.repository.OrganizationRepository
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

data class HomeUiState(
    val studySets: List<StudySetEntity> = emptyList(),
    val recentSets: List<StudySetEntity> = emptyList(),
    val isLoading: Boolean = true,
    val totalSets: Int = 0,
    val totalCards: Int = 0,
    val avgMasteryPercent: Int = 0,
    val hasStudySets: Boolean = false,
    // Streak & Progress
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val todayCards: Int = 0,
    val needsReviewCount: Int = 0,
    // Gamification
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val xpProgressInLevel: Float = 0f,
    val dailyGoalCurrent: Int = 0,
    val dailyGoalTarget: Int = 10
)

sealed class HomeEvent {
    data class LevelUp(val level: Int) : HomeEvent()
    object DailyGoalComplete : HomeEvent()
}

class HomeViewModel : ViewModel() {

    private val repository: StudySetRepository = AppContainer.studySetRepository
    private val orgRepository: OrganizationRepository = AppContainer.organizationRepository
    private val gamificationRepository: GamificationRepository = AppContainer.gamificationRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadData()
        observeGamification()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllStudySets().collectLatest { sets ->
                val totalCards = sets.sumOf { it.cardCount }
                val hasSets = sets.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    studySets = sets,
                    recentSets = sets.take(3),
                    isLoading = false,
                    totalSets = sets.size,
                    totalCards = totalCards,
                    hasStudySets = hasSets
                )
            }
        }

        viewModelScope.launch {
            val stats = orgRepository.getTodayStats() ?: com.example.quizfromfileapp.data.local.entity.DailyStudyStatsEntity(
                date = "",
                streakCount = 0,
                cardsReviewed = 0
            )
            val streak = orgRepository.getCurrentStreak()
            val needsReview = orgRepository.getNeedsReviewCount()
            _uiState.value = _uiState.value.copy(
                currentStreak = streak,
                maxStreak = stats.streakCount.coerceAtLeast(streak),
                todayCards = stats.cardsReviewed,
                needsReviewCount = needsReview,
                dailyGoalCurrent = stats.cardsReviewed,
                dailyGoalTarget = gamificationRepository.getDailyGoal()
            )
        }
    }

    private fun observeGamification() {
        viewModelScope.launch {
            gamificationRepository.totalXp.collect { xp ->
                _uiState.value = _uiState.value.copy(
                    totalXp = xp,
                    currentLevel = gamificationRepository.getCurrentLevel(),
                    xpProgressInLevel = gamificationRepository.getXpProgressInLevel()
                )
            }
        }

        viewModelScope.launch {
            gamificationRepository.dailyGoalProgress.collect { current ->
                _uiState.value = _uiState.value.copy(
                    dailyGoalCurrent = current,
                    dailyGoalTarget = gamificationRepository.getDailyGoal()
                )
            }
        }

        viewModelScope.launch {
            gamificationRepository.levelUpEvent.collect { level ->
                if (level != null) {
                    _events.emit(HomeEvent.LevelUp(level))
                    gamificationRepository.clearLevelUpEvent()
                }
            }
        }

        viewModelScope.launch {
            gamificationRepository.dailyGoalCompleteEvent.collect { complete ->
                if (complete) {
                    _events.emit(HomeEvent.DailyGoalComplete)
                    gamificationRepository.clearDailyGoalEvent()
                }
            }
        }
    }

    fun setDailyGoal(goal: Int) {
        gamificationRepository.setDailyGoal(goal)
        _uiState.value = _uiState.value.copy(dailyGoalTarget = goal)
    }

    fun refresh() {
        gamificationRepository.refreshDailyProgress()
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadData()
    }
}
