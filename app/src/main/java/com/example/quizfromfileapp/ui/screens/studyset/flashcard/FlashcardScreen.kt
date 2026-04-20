package com.example.quizfromfileapp.ui.screens.studyset.flashcard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.ui.components.FlashcardActionBar
import com.example.quizfromfileapp.ui.components.FlashcardBackContent
import com.example.quizfromfileapp.ui.components.FlashcardFilterChips
import com.example.quizfromfileapp.ui.components.FlashcardFrontContent
import com.example.quizfromfileapp.ui.components.FlashcardProgressHeader
import com.example.quizfromfileapp.ui.components.FlashcardSessionSummaryCard
import com.example.quizfromfileapp.ui.components.PremiumFlipCard
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.components.TtsSpeakerButton
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════
// FLASHSCREEN — LIGHT PREMIUM
// Premium flashcard experience with smooth flip animation,
// swipe navigation, and polished UI
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(studySetId) {
        viewModel.loadCards(studySetId)
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = if (uiState.selectedFilter == FlashcardFilter.ALL) {
                    "Thẻ ghi nhớ (${uiState.filteredCards.size})"
                } else {
                    "${uiState.selectedFilter.displayName} (${uiState.filteredCards.size})"
                },
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actionsContent = {
                    val card = uiState.currentCard
                    val voiceEnabled by AppContainer.audioManager.voiceEnabled.collectAsState()
                    if (card != null) {
                        TtsSpeakerButton(
                            onClick = {
                                val textToSpeak = if (!uiState.isFlipped) card.term else card.definition
                                AppContainer.audioManager.speak(textToSpeak, flush = true)
                            },
                            isEnabled = voiceEnabled
                        )
                        PremiumIconButton(
                            icon = if (card.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            onClick = viewModel::toggleStar,
                            tint = if (card.isStarred) AppColors.Accent else AppColors.OnSurfaceVariant,
                            contentDescription = if (card.isStarred) AppStringsVi.FlashcardUnstarred else AppStringsVi.FlashcardStarred,
                            buttonSize = 40
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(modifier = Modifier.padding(innerPadding))
            }
            uiState.allCards.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier.padding(innerPadding),
                    onAddCards = onNavigateBack
                )
            }
            else -> {
                FlashcardMainContent(
                    uiState = uiState,
                    onFlip = viewModel::flipCard,
                    onNext = viewModel::nextCard,
                    onPrev = viewModel::prevCard,
                    onMarkCorrect = viewModel::markCorrect,
                    onMarkIncorrect = viewModel::markIncorrect,
                    onGoToCard = viewModel::goToCard,
                    onShuffle = viewModel::shuffleCards,
                    onResetOrder = viewModel::resetOrder,
                    onShowFilterSheet = { showFilterSheet = true },
                    onShowSortSheet = { showSortSheet = true },
                    onStarToggle = viewModel::toggleStar,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterSortSheet(
            title = AppStringsVi.FlashcardFilter,
            options = FlashcardFilter.entries.map { it.displayName },
            selectedIndex = FlashcardFilter.entries.indexOf(uiState.selectedFilter),
            onSelect = { index ->
                viewModel.setFilter(FlashcardFilter.entries[index])
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Sort bottom sheet
    if (showSortSheet) {
        FilterSortSheet(
            title = AppStringsVi.FlashcardSort,
            options = FlashcardSortMode.entries.map { it.displayName },
            selectedIndex = FlashcardSortMode.entries.indexOf(uiState.selectedSortMode),
            onSelect = { index ->
                viewModel.setSortMode(FlashcardSortMode.entries[index])
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    // Session summary
    if (uiState.showSessionSummary) {
        SessionSummaryDialog(
            summary = uiState.sessionSummary,
            onRestart = viewModel::restartSession,
            onReviewWrong = if (uiState.wrongCardIds.isNotEmpty()) viewModel::reviewWrongCards else null,
            onDismiss = viewModel::dismissSessionSummary
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// MAIN CONTENT
// ═══════════════════════════════════════════════════════════════
@Composable
private fun FlashcardMainContent(
    uiState: FlashcardUiState,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onMarkCorrect: () -> Unit,
    onMarkIncorrect: () -> Unit,
    onGoToCard: (Int) -> Unit,
    onShuffle: () -> Unit,
    onResetOrder: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onShowSortSheet: () -> Unit,
    onStarToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.filteredCards.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(AppColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(AppRadius.xl))
                        .background(AppColors.PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(AppSpacing.xl))
                Text(
                    AppStringsVi.EmptyNoResults,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
                Spacer(Modifier.height(AppSpacing.xs))
                Text(
                    AppStringsVi.EmptyNoResultsSub,
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
            }
        }
        return
    }

    val card = uiState.currentCard ?: return
    val isQA = uiState.studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
    val typeLabel = if (isQA) AppStringsVi.StudySetTypeQA else AppStringsVi.StudySetTypeTermDef
    val typeColor = if (isQA) AppColors.Secondary else AppColors.Primary

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(AppSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Filter & sort chips
        FlashcardFilterChips(
            filterLabel = uiState.selectedFilter.displayName,
            sortLabel = uiState.selectedSortMode.displayName,
            isFilterActive = uiState.selectedFilter != FlashcardFilter.ALL,
            isSortActive = uiState.selectedSortMode != FlashcardSortMode.ORIGINAL,
            isShuffled = uiState.isShuffled,
            onFilterClick = onShowFilterSheet,
            onSortClick = onShowSortSheet,
            onShuffleClick = onShuffle,
            onResetClick = onResetOrder
        )

        Spacer(Modifier.height(AppSpacing.md))

        // Progress header
        FlashcardProgressHeader(
            currentIndex = uiState.currentIndex + 1,
            totalCount = uiState.filteredCards.size,
            correctCount = uiState.correctCount,
            wrongCount = uiState.wrongCount
        )

        Spacer(Modifier.height(AppSpacing.lg))

        // Question palette dots
        CardPalette(
            totalQuestions = uiState.filteredCards.size,
            currentIndex = uiState.currentIndex,
            wrongCardIds = uiState.wrongCardIds,
            allCards = uiState.filteredCards,
            onQuestionClick = onGoToCard
        )

        Spacer(Modifier.height(AppSpacing.xl))

        // FLIP CARD — main focal point
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffset) > 100) {
                                if (dragOffset < 0) onNext() else onPrev()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Swipe hint arrows
            val arrowAlpha = (abs(dragOffset) / 120f).coerceAtMost(0.5f)
            if (dragOffset < -20) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppColors.Primary.copy(alpha = arrowAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(24.dp)
                )
            } else if (dragOffset > 20) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = AppColors.Primary.copy(alpha = arrowAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(24.dp)
                )
            }

            PremiumFlipCard(
                isFlipped = uiState.isFlipped,
                onFlip = onFlip,
                frontContent = {
                    FlashcardFrontContent(
                        term = card.term,
                        typeLabel = typeLabel,
                        typeColor = typeColor,
                        masteryLevel = card.masteryLevel,
                        isStarred = card.isStarred
                    )
                },
                backContent = {
                    FlashcardBackContent(
                        definition = card.definition,
                        explanation = card.explanation.takeIf { it.isNotBlank() },
                        sourcePage = card.sourcePageStart?.toString(),
                        sourceSnippet = card.sourceSnippet.takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (dragOffset * 0.25f).dp)
            )
        }

        Spacer(Modifier.height(AppSpacing.xl))

        // Action bar
        FlashcardActionBar(
            onPrev = onPrev,
            onNext = onNext,
            onMarkIncorrect = onMarkIncorrect,
            onMarkCorrect = onMarkCorrect,
            onStarToggle = onStarToggle,
            isStarred = card.isStarred,
            isFirst = uiState.isFirst,
            isLast = uiState.isLast
        )

        Spacer(Modifier.height(AppSpacing.lg))
    }
}

// ═══════════════════════════════════════════════════════════════
// QUESTION PALETTE — color-coded dots
// ═══════════════════════════════════════════════════════════════
@Composable
private fun CardPalette(
    totalQuestions: Int,
    currentIndex: Int,
    wrongCardIds: Set<Long>,
    allCards: List<FlashcardEntity>,
    onQuestionClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        modifier = Modifier.fillMaxWidth()
    ) {
        items((0 until totalQuestions).toList()) { index ->
            val card = allCards.getOrNull(index) ?: return@items
            val isCurrent = index == currentIndex
            val isWrong = card.id in wrongCardIds

            val bgColor = when {
                isCurrent && isWrong -> AppColors.Error
                isCurrent -> AppColors.Primary
                isWrong -> AppColors.Warning.copy(alpha = 0.7f)
                else -> AppColors.PrimaryContainer
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onQuestionClick(index) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// LOADING STATE
// ═══════════════════════════════════════════════════════════════
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            trackColor = AppColors.PrimaryContainer,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════
@Composable
private fun EmptyContent(
    modifier: Modifier = Modifier,
    onAddCards: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(AppSpacing.huge)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(AppRadius.xl))
                    .background(AppColors.PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TextSnippet,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(AppSpacing.xl))
            Text(
                AppStringsVi.FlashcardNoCards,
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                AppStringsVi.FlashcardNoCardsSub,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FILTER / SORT BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = AppRadius.bottomSheet, topEnd = AppRadius.bottomSheet),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .padding(horizontal = AppSpacing.screenPadding)
                .padding(bottom = AppSpacing.xxxl)
        ) {
            Text(
                text = title,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface,
                modifier = Modifier.padding(bottom = AppSpacing.lg)
            )
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.xs)
                        .clickable { onSelect(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.08f)
                        else AppColors.Surface
                    ),
                    shape = RoundedCornerShape(AppRadius.md),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
                    else androidx.compose.foundation.BorderStroke(1.dp, AppColors.OutlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AppColors.Primary
                                    else AppColors.OutlineVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(AppSpacing.md))
                        Text(
                            text = option,
                            style = AppTypography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AppColors.Primary else AppColors.OnSurface
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SESSION SUMMARY DIALOG — enhanced with duration & wrong-answer count
// ═══════════════════════════════════════════════════════════════
@Composable
private fun SessionSummaryDialog(
    summary: FlashcardSessionSummary,
    onRestart: () -> Unit,
    onReviewWrong: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val scorePercent = if (summary.totalCards > 0) {
        (summary.remembered * 100) / summary.totalCards
    } else 0
    val scoreColor = AppColors.scoreColor(scorePercent)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.dialog),
        containerColor = AppColors.Surface,
        title = {
            Text(
                AppStringsVi.FlashcardEndSession,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
        },
        text = {
            Column {
                Text(
                    AppStringsVi.FlashcardEndSessionSub,
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(AppSpacing.lg))

                // Score ring
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$scorePercent%",
                            style = AppTypography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStat(label = AppStringsVi.SessionRemembered, value = "${summary.remembered}", color = AppColors.Success)
                    SummaryStat(label = AppStringsVi.SessionNeedReview, value = "${summary.needReview}", color = AppColors.Warning)
                    SummaryStat(label = AppStringsVi.LearnTotalCards, value = "${summary.totalCards}", color = AppColors.OnSurface)
                }

                // Duration
                if (summary.isComplete) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AppColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(
                            "${AppStringsVi.FlashcardDuration}: ${summary.durationFormatted}",
                            style = AppTypography.labelSmall,
                            color = AppColors.OnSurfaceMuted
                        )
                    }
                }

                // Wrong-answer count
                if (summary.wrongCardCount > 0) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(
                            "${summary.wrongCardCount} ${AppStringsVi.LearnWrongAnswers}",
                            style = AppTypography.labelMedium,
                            color = AppColors.Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (summary.starredCount > 0) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(
                            "${summary.starredCount} ${AppStringsVi.SessionStarred}",
                            style = AppTypography.bodySmall,
                            color = AppColors.Accent
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(AppStringsVi.FlashcardLearnAgain, style = AppTypography.labelLarge, color = AppColors.Primary)
                }
            }
        },
        dismissButton = {
            Row {
                if (onReviewWrong != null) {
                    TextButton(onClick = onReviewWrong) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = AppColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(AppSpacing.xs))
                            Text(
                                "${AppStringsVi.FlashcardWrongReview} (${summary.wrongCardCount})",
                                style = AppTypography.labelLarge,
                                color = AppColors.Warning
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(AppStringsVi.FlashcardClose, style = AppTypography.labelLarge, color = AppColors.OnSurfaceVariant)
                }
            }
        }
    )
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = AppTypography.bodySmall,
            color = AppColors.OnSurfaceVariant
        )
    }
}
