package com.example.quizfromfileapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.quizfromfileapp.data.local.entity.DailyStudyStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository xử lý gamification: XP, Level, Daily Goal.
 *
 * XP System:
 * - Flashcard browse: +2 XP per card reviewed
 * - Learn correct: +5 XP per correct answer
 * - Test submit: +10 XP bonus
 * - Smart review: +3 XP per card
 *
 * Level: level up every 100 XP
 */
class GamificationRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ─── XP STATE ────────────────────────────────────────────────────

    private val _totalXp = MutableStateFlow(prefs.getInt(KEY_TOTAL_XP, 0))
    val totalXp: Flow<Int> = _totalXp.asStateFlow()

    private val _currentLevel = MutableStateFlow(calculateLevel(prefs.getInt(KEY_TOTAL_XP, 0)))
    val currentLevel: Flow<Int> = _currentLevel.asStateFlow()

    // Daily goal state
    private val _dailyGoalProgress = MutableStateFlow(getDailyCardsToday())
    val dailyGoalProgress: Flow<Int> = _dailyGoalProgress.asStateFlow()

    val dailyGoalTarget: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)

    val dailyGoalComplete: Boolean
        get() = _dailyGoalProgress.value >= dailyGoalTarget

    // Level-up event (one-shot)
    private val _levelUpEvent = MutableStateFlow<Int?>(null)
    val levelUpEvent: Flow<Int?> = _levelUpEvent.asStateFlow()

    // Daily goal complete event
    private val _dailyGoalCompleteEvent = MutableStateFlow(false)
    val dailyGoalCompleteEvent: Flow<Boolean> = _dailyGoalCompleteEvent.asStateFlow()

    // ─── XP EARNING ──────────────────────────────────────────────────

    /**
     * Add XP for a card review (flashcard browse).
     * +2 XP per card
     */
    fun addXpForFlashcardReview(cardCount: Int = 1) {
        addXp(cardCount * XP_PER_FLASHCARD)
    }

    /**
     * Add XP for correct answer in Learn mode.
     * +5 XP per correct answer
     */
    fun addXpForLearnCorrect(correctCount: Int = 1) {
        addXp(correctCount * XP_PER_LEARN_CORRECT)
    }

    /**
     * Add XP for completing a test.
     * +10 XP bonus per test
     */
    fun addXpForTestSubmit() {
        addXp(XP_PER_TEST)
    }

    /**
     * Add XP for smart review.
     * +3 XP per card reviewed
     */
    fun addXpForSmartReview(cardCount: Int = 1) {
        addXp(cardCount * XP_PER_SMART_REVIEW)
    }

    private fun addXp(amount: Int) {
        if (amount <= 0) return

        val previousLevel = _currentLevel.value
        val newTotal = _totalXp.value + amount
        val newLevel = calculateLevel(newTotal)

        prefs.edit()
            .putInt(KEY_TOTAL_XP, newTotal)
            .apply()

        _totalXp.value = newTotal
        _currentLevel.value = newLevel

        // Trigger level-up event if leveled up
        if (newLevel > previousLevel) {
            _levelUpEvent.value = newLevel
        }
    }

    // ─── DAILY GOAL ──────────────────────────────────────────────────

    /**
     * Called when user completes a card review session.
     * Increments daily progress and checks for goal completion.
     */
    fun recordDailyProgress(cardsReviewed: Int) {
        val today = dateFormat.format(Date())
        val yesterday = getYesterdayString()

        val current = _dailyGoalProgress.value
        val newProgress = current + cardsReviewed
        _dailyGoalProgress.value = newProgress

        // Save to prefs
        prefs.edit()
            .putInt(KEY_DAILY_CARDS_PREFIX + today, newProgress)
            .putString(KEY_DAILY_DATE_PREFIX + today, today)
            .apply()

        // Check if daily goal just completed
        if (newProgress >= dailyGoalTarget && current < dailyGoalTarget) {
            _dailyGoalCompleteEvent.value = true
        }
    }

    /**
     * Refresh daily progress from prefs (call on app resume).
     */
    fun refreshDailyProgress() {
        _dailyGoalProgress.value = getDailyCardsToday()
    }

    private fun getDailyCardsToday(): Int {
        val today = dateFormat.format(Date())
        return prefs.getInt(KEY_DAILY_CARDS_PREFIX + today, 0)
    }

    private fun getYesterdayString(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    // ─── DAILY GOAL CONFIG ────────────────────────────────────────────

    fun setDailyGoal(target: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, target).apply()
    }

    fun getDailyGoal(): Int {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    }

    // ─── STATS ────────────────────────────────────────────────────────

    fun getTotalXp(): Int = _totalXp.value

    fun getCurrentLevel(): Int = _currentLevel.value

    fun getXpForNextLevel(): Int {
        val level = _currentLevel.value
        return level * XP_PER_LEVEL
    }

    fun getXpProgressInLevel(): Float {
        val totalXp = _totalXp.value
        val level = _currentLevel.value
        val xpAtLevelStart = (level - 1) * XP_PER_LEVEL
        val xpInCurrentLevel = totalXp - xpAtLevelStart
        return (xpInCurrentLevel.toFloat() / XP_PER_LEVEL).coerceIn(0f, 1f)
    }

    fun clearLevelUpEvent() {
        _levelUpEvent.value = null
    }

    fun clearDailyGoalEvent() {
        _dailyGoalCompleteEvent.value = false
    }

    // ─── HELPERS ─────────────────────────────────────────────────────

    private fun calculateLevel(totalXp: Int): Int {
        return (totalXp / XP_PER_LEVEL) + 1
    }

    companion object {
        private const val PREFS_NAME = "gamification_prefs"

        // XP keys
        private const val KEY_TOTAL_XP = "total_xp"

        // Daily goal keys
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_DAILY_CARDS_PREFIX = "daily_cards_"
        private const val KEY_DAILY_DATE_PREFIX = "daily_date_"

        // XP values
        const val XP_PER_FLASHCARD = 2
        const val XP_PER_LEARN_CORRECT = 5
        const val XP_PER_TEST = 10
        const val XP_PER_SMART_REVIEW = 3
        const val XP_PER_LEVEL = 100

        // Default daily goal
        const val DEFAULT_DAILY_GOAL = 10

        // Available daily goal options
        val DAILY_GOAL_OPTIONS = listOf(5, 10, 20, 30)
    }
}
