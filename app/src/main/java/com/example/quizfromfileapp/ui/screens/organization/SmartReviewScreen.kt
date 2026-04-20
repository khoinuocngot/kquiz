package com.example.quizfromfileapp.ui.screens.organization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumFlipCard
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmartReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Record session stats when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.recordSession()
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.SmartReviewTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }
            uiState.cards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumEmptyState(
                        icon = Icons.Default.Star,
                        title = AppStringsVi.SmartReviewEmpty,
                        subtitle = AppStringsVi.SmartReviewEmptySub
                    )
                }
            }
            uiState.isSessionComplete -> {
                SmartReviewCompleteContent(
                    uiState = uiState,
                    onRestart = viewModel::restartSession,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                SmartReviewMainContent(
                    uiState = uiState,
                    onFlip = viewModel::flipCard,
                    onNext = viewModel::nextCard,
                    onPrev = viewModel::prevCard,
                    onMarkCorrect = viewModel::markCorrect,
                    onMarkIncorrect = viewModel::markIncorrect,
                    onGoToCard = viewModel::goToCard,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun SmartReviewMainContent(
    uiState: SmartReviewUiState,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onMarkCorrect: () -> Unit,
    onMarkIncorrect: () -> Unit,
    onGoToCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val card = uiState.currentCard ?: return
    val typeLabel = AppStringsVi.StudySetTypeTermDef
    val typeColor = AppColors.Primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Priority badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(AppRadius.chip),
                colors = CardDefaults.cardColors(containerColor = AppColors.Warning.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        AppStringsVi.SmartReviewDesc,
                        style = AppTypography.labelSmall,
                        color = AppColors.Warning
                    )
                }
            }
            Text(
                "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                style = AppTypography.labelMedium,
                color = AppColors.OnSurfaceVariant
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))

        // Progress bar
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppColors.Warning,
            trackColor = AppColors.Warning.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )

        Spacer(Modifier.height(AppSpacing.sm))

        // Palette dots
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(uiState.cards) { index, flashcard ->
                val isCurrent = index == uiState.currentIndex
                val isWrong = flashcard.id in uiState.wrongCardIds
                val isReviewed = flashcard.id in uiState.reviewedCardIds
                val dotColor = when {
                    isCurrent && isWrong -> AppColors.Error
                    isCurrent -> AppColors.Warning
                    isWrong -> AppColors.Error.copy(alpha = 0.5f)
                    isReviewed -> AppColors.Success.copy(alpha = 0.5f)
                    else -> AppColors.Warning.copy(alpha = 0.25f)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .clickable { onGoToCard(index) }
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PremiumFlipCard(
                isFlipped = uiState.isFlipped,
                onFlip = onFlip,
                frontContent = {
                    SmartReviewFrontContent(
                        term = card.term,
                        typeColor = typeColor
                    )
                },
                backContent = {
                    SmartReviewBackContent(
                        definition = card.definition,
                        explanation = card.explanation.takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Action bar
        if (uiState.isFlipped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onMarkIncorrect),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(containerColor = AppColors.ErrorContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.ErrorBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(AppSpacing.sm))
                        Text(AppStringsVi.FlashcardNeedReview, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.Error)
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onMarkCorrect),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(containerColor = AppColors.SuccessContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.SuccessBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(AppSpacing.sm))
                        Text(AppStringsVi.FlashcardRemembered, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !uiState.isFirst, onClick = onPrev),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!uiState.isFirst) AppColors.Surface else AppColors.SurfaceVariant
                    ),
                    border = if (!uiState.isFirst) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = if (!uiState.isFirst) AppColors.OnSurface else AppColors.OnSurfaceMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(AppStringsVi.Prev, style = AppTypography.titleSmall, color = if (!uiState.isFirst) AppColors.OnSurface else AppColors.OnSurfaceMuted)
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onFlip),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Primary)
                ) {
                    Text(
                        AppStringsVi.FlashcardTapToFlip,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.md))
    }
}

@Composable
private fun SmartReviewFrontContent(term: String, typeColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = AppStringsVi.FlashcardFront,
            style = AppTypography.labelMedium,
            color = typeColor,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(AppSpacing.lg))
        Text(
            text = term,
            style = AppTypography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            textAlign = TextAlign.Center,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(AppSpacing.xxl))
        Text(
            AppStringsVi.FlashcardHintFlip,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceMuted
        )
    }
}

@Composable
private fun SmartReviewBackContent(definition: String, explanation: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = AppStringsVi.FlashcardBackLabel,
            style = AppTypography.labelMedium,
            color = AppColors.Success,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = definition,
            style = AppTypography.titleLarge.copy(lineHeight = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = AppColors.OnSurface
        )
        if (!explanation.isNullOrBlank()) {
            Spacer(Modifier.height(AppSpacing.lg))
            Text(
                text = AppStringsVi.FlashcardExplanation,
                style = AppTypography.labelMedium,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = explanation,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurface
            )
        }
        Spacer(Modifier.height(AppSpacing.xxl))
        Text(
            AppStringsVi.FlashcardHintFlipBack,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SmartReviewCompleteContent(
    uiState: SmartReviewUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scorePercent = uiState.sessionScorePercent
    val scoreColor = AppColors.scoreColor(scorePercent)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(AppSpacing.xxl))

        // Score circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$scorePercent%",
                style = AppTypography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = when {
                scorePercent >= 80 -> AppStringsVi.TestExcellent
                scorePercent >= 50 -> AppStringsVi.TestGood
                else -> AppStringsVi.TestLow
            },
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface
        )

        Spacer(Modifier.height(AppSpacing.xxl))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.correctCount}",
                    style = AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Success
                )
                Text(AppStringsVi.SessionRemembered, style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.wrongCount}",
                    style = AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Error
                )
                Text(AppStringsVi.SessionNeedReview, style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.reviewedCount}",
                    style = AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                Text(AppStringsVi.FlashcardOfLabel, style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
            }
        }

        Spacer(Modifier.height(AppSpacing.xxl))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.button),
            colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    "Đã ghi nhận ${uiState.reviewedCount} thẻ vào lịch sử học tập!",
                    style = AppTypography.bodyMedium,
                    color = AppColors.Primary
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.xxl))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRestart),
            shape = RoundedCornerShape(AppRadius.button),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Text(AppStringsVi.FlashcardLearnAgain, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.Primary)
            }
        }
    }
}
