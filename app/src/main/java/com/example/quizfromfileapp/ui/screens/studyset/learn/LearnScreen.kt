package com.example.quizfromfileapp.ui.screens.studyset.learn

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.ui.components.TtsSpeakerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumGhostButton
import com.example.quizfromfileapp.ui.components.PremiumOptionCard
import com.example.quizfromfileapp.ui.components.PremiumProgressHeader
import com.example.quizfromfileapp.ui.components.PremiumResultHeroCard
import com.example.quizfromfileapp.ui.components.PremiumSecondaryButton
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppSuccess
import com.example.quizfromfileapp.ui.theme.AppTypography
import com.example.quizfromfileapp.ui.theme.AppWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LearnViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isReviewMode by remember { mutableStateOf(false) }

    LaunchedEffect(studySetId) {
        viewModel.loadStudySet(studySetId)
    }

    // Reset review mode when study set changes
    LaunchedEffect(studySetId) {
        isReviewMode = false
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = if (isReviewMode) AppStringsVi.FlashcardWrongReview else AppStringsVi.LearnTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = {
                    if (isReviewMode) {
                        isReviewMode = false
                        viewModel.exitWrongReviewMode()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        containerColor = AppColors.Background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.cards.size < 4 -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumEmptyState(
                        icon = Icons.Default.Refresh,
                        title = AppStringsVi.LearnMinCards,
                        subtitle = "${AppStringsVi.LearnMinCardsSub} ${uiState.cards.size} ${AppStringsVi.StudySetCards}",
                        action = {
                            PremiumGhostButton(text = AppStringsVi.ActionBack, onClick = onNavigateBack)
                        }
                    )
                }
            }
            uiState.isSessionComplete -> {
                LearnSessionComplete(
                    correctCount = uiState.correctCount,
                    totalAnswered = uiState.totalAnswered,
                    scorePercent = uiState.scorePercent,
                    wrongCount = uiState.wrongCount,
                    wrongCardCount = uiState.wrongCardIds.size,
                    onRestart = viewModel::restart,
                    onReviewWrong = {
                        viewModel.startWrongReviewMode()
                        isReviewMode = true
                    },
                    onBack = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LearnQuestionView(
                    uiState = uiState,
                    isReviewMode = isReviewMode,
                    onSelectOption = viewModel::selectOption,
                    onNext = viewModel::nextQuestion,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun LearnQuestionView(
    uiState: LearnUiState,
    isReviewMode: Boolean,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.questions[uiState.currentIndex]
    val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size

    Column(modifier = modifier.fillMaxSize().padding(AppSpacing.screenPadding)) {
        // Progress header
        PremiumProgressHeader(
            currentIndex = uiState.currentIndex,
            totalCount = uiState.questions.size,
            correctCount = uiState.correctCount,
            wrongCount = uiState.wrongCount,
            progress = progress
        )

        Spacer(Modifier.height(AppSpacing.lg))

        // Question type badge
        val isQuestionAnswer = question.isQuestionAnswer
        Card(
            shape = RoundedCornerShape(AppRadius.chip),
            colors = CardDefaults.cardColors(
                containerColor = if (isReviewMode) AppColors.Warning else AppColors.Primary
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isReviewMode) {
                    Text(
                        text = AppStringsVi.FlashcardWrongReview,
                        style = AppTypography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = when {
                            question.isMultipleChoice -> AppStringsVi.LearnDirectionMCQ
                            question.isTermQuestion -> AppStringsVi.LearnDirectionTermDef
                            else -> AppStringsVi.LearnDirectionDefTerm
                        },
                        style = AppTypography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        // Question card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(AppRadius.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(AppSpacing.xl)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                val voiceEnabled by AppContainer.audioManager.voiceEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = question.questionText,
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.OnSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    TtsSpeakerButton(
                        onClick = { AppContainer.audioManager.speak(question.questionText, flush = true) },
                        isEnabled = voiceEnabled
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        // Options
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(question.options) { index, option ->
                PremiumOptionCard(
                    letter = ('A' + index).toString(),
                    text = option,
                    isSelected = uiState.selectedOption == index,
                    isCorrect = if (uiState.isAnswered) index == question.correctIndex else null,
                    isAnswered = uiState.isAnswered,
                    onClick = { onSelectOption(index) }
                )
            }
        }

        // Next button
        if (uiState.isAnswered) {
            Spacer(Modifier.height(AppSpacing.md))
            com.example.quizfromfileapp.ui.components.PremiumButton(
                text = if (uiState.currentIndex < uiState.questions.size - 1) AppStringsVi.LearnNextQuestion else AppStringsVi.LearnSessionOver,
                onClick = onNext,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(AppSpacing.md))
    }
}

@Composable
private fun LearnSessionComplete(
    correctCount: Int,
    totalAnswered: Int,
    scorePercent: Int,
    wrongCount: Int,
    wrongCardCount: Int,
    onRestart: () -> Unit,
    onReviewWrong: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(AppSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(AppSpacing.xxl))

        PremiumResultHeroCard(
            scorePercent = scorePercent,
            correctCount = correctCount,
            totalCount = totalAnswered,
            wrongCount = wrongCount,
            flaggedCount = 0,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(AppSpacing.xxl))

        // Wrong-answer review button (only if there are wrong answers)
        if (wrongCardCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.button),
                colors = CardDefaults.cardColors(containerColor = AppColors.WarningContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.WarningBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = "$wrongCardCount ${AppStringsVi.LearnWrongAnswers} — ${AppStringsVi.LearnWrongReviewSub}",
                        style = AppTypography.bodyMedium,
                        color = AppColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.md))
            com.example.quizfromfileapp.ui.components.PremiumButton(
                text = "${AppStringsVi.LearnWrongAnswersReview} ($wrongCardCount)",
                onClick = onReviewWrong,
                icon = Icons.Default.Warning,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppSpacing.md))
        }

        com.example.quizfromfileapp.ui.components.PremiumButton(
            text = AppStringsVi.FlashcardLearnAgain,
            onClick = onRestart,
            icon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(AppSpacing.md))

        PremiumSecondaryButton(
            text = AppStringsVi.LearnBackToStudySet,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
