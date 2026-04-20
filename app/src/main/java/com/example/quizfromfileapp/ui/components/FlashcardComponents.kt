package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

// ═══════════════════════════════════════════════════════════════
// PREMIUM FLIP CARD
// Smooth 3D flip animation with front/back faces
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumFlipCard(
    isFlipped: Boolean,
    onFlip: () -> Unit,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = AppMotion.CardFlip, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "flipRotation"
    )

    val surfaceColor by animateColorAsState(
        targetValue = if (isFlipped) AppColors.SurfaceVariant else AppColors.Surface,
        animationSpec = tween(AppMotion.Normal),
        label = "surfaceColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.rotationY = rotation
                cameraDistance = 12f * density
                this.alpha = 1f
            }
            .clickable(onClick = onFlip)
    ) {
        if (rotation <= 90f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.flashcard),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    frontContent()
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(AppRadius.flashcard),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    backContent()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD FRONT CONTENT
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardFrontContent(
    term: String,
    typeLabel: String,
    typeColor: Color,
    masteryLevel: Int,
    isStarred: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.chip))
                    .background(typeColor.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = typeLabel,
                    style = AppTypography.labelSmall,
                    color = typeColor,
                    fontWeight = FontWeight.Medium
                )
            }
            MasteryDots(level = masteryLevel)
        }

        Spacer(Modifier.height(AppSpacing.huge))

        Text(
            text = term,
            style = AppTypography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            textAlign = TextAlign.Center,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(AppSpacing.xxl))

        Text(
            text = AppStringsVi.FlashcardHintFlip,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceMuted
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD BACK CONTENT
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardBackContent(
    definition: String,
    explanation: String?,
    sourcePage: String?,
    sourceSnippet: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = AppStringsVi.FlashcardBackLabel,
            style = AppTypography.labelMedium,
            color = AppColors.Primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(AppSpacing.md))

        Text(
            text = definition,
            style = AppTypography.titleLarge.copy(lineHeight = 30.sp),
            fontWeight = FontWeight.Normal,
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
                style = AppTypography.bodyMedium.copy(lineHeight = 22.sp),
                color = AppColors.OnSurface
            )
        }

        if (!sourcePage.isNullOrBlank() || !sourceSnippet.isNullOrBlank()) {
            Spacer(Modifier.height(AppSpacing.lg))
            Row {
                if (!sourcePage.isNullOrBlank()) {
                    Text(
                        text = "${AppStringsVi.FlashcardSourcePage} $sourcePage",
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }
                if (!sourceSnippet.isNullOrBlank()) {
                    if (!sourcePage.isNullOrBlank()) Spacer(Modifier.width(AppSpacing.md))
                    Text(
                        text = sourceSnippet.take(60),
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.xxl))
        Text(
            text = AppStringsVi.FlashcardHintFlipBack,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD PROGRESS HEADER
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardProgressHeader(
    currentIndex: Int,
    totalCount: Int,
    correctCount: Int,
    wrongCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) (currentIndex + 1).toFloat() / totalCount else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${AppStringsVi.FlashcardOfLabel} $currentIndex / $totalCount",
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                if (correctCount > 0 || wrongCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AppColors.Success))
                        Spacer(Modifier.width(4.dp))
                        Text("$correctCount", style = AppTypography.labelSmall, color = AppColors.Success)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AppColors.Warning))
                        Spacer(Modifier.width(4.dp))
                        Text("$wrongCount", style = AppTypography.labelSmall, color = AppColors.Warning)
                    }
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppColors.Primary,
            trackColor = AppColors.PrimaryContainer
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD ACTION BAR
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardActionBar(
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMarkIncorrect: () -> Unit,
    onMarkCorrect: () -> Unit,
    onStarToggle: () -> Unit,
    isStarred: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prev button
            Card(
                modifier = Modifier
                    .clickable(enabled = !isFirst, onClick = onPrev),
                shape = RoundedCornerShape(AppRadius.button),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isFirst) AppColors.Surface else AppColors.SurfaceVariant
                ),
                border = if (!isFirst) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = AppStringsVi.FlashcardPrev,
                        tint = if (!isFirst) AppColors.OnSurface else AppColors.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        AppStringsVi.FlashcardPrev,
                        style = AppTypography.labelLarge,
                        color = if (!isFirst) AppColors.OnSurface else AppColors.OnSurfaceMuted
                    )
                }
            }

            // Star toggle
            Card(
                modifier = Modifier.clickable(onClick = onStarToggle),
                shape = RoundedCornerShape(AppRadius.button),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStarred) AppColors.Accent.copy(alpha = 0.1f) else AppColors.SurfaceVariant
                ),
                border = if (isStarred) androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Accent.copy(alpha = 0.4f))
                else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
            ) {
                Icon(
                    imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isStarred) AppStringsVi.FlashcardUnstarred else AppStringsVi.FlashcardStarred,
                    tint = if (isStarred) AppColors.Accent else AppColors.OnSurfaceVariant,
                    modifier = Modifier.padding(AppSpacing.md)
                )
            }

            // Next button
            Card(
                modifier = Modifier
                    .clickable(enabled = !isLast, onClick = onNext),
                shape = RoundedCornerShape(AppRadius.button),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isLast) AppColors.Surface else AppColors.SurfaceVariant
                ),
                border = if (!isLast) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        AppStringsVi.FlashcardNext,
                        style = AppTypography.labelLarge,
                        color = if (!isLast) AppColors.OnSurface else AppColors.OnSurfaceMuted
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = AppStringsVi.FlashcardNext,
                        tint = if (!isLast) AppColors.OnSurface else AppColors.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        // Mark buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onMarkIncorrect),
                shape = RoundedCornerShape(AppRadius.button),
                colors = CardDefaults.cardColors(containerColor = AppColors.WarningContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.WarningBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        AppStringsVi.FlashcardNeedReview,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Warning
                    )
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
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        AppStringsVi.FlashcardRemembered,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Success
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD SESSION SUMMARY CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardSessionSummaryCard(
    totalCards: Int,
    remembered: Int,
    needReview: Int,
    starredCount: Int,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scorePercent = if (totalCards > 0) (remembered * 100) / totalCards else 0
    val scoreColor = AppColors.scoreColor(scorePercent)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.cardLarge),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$scorePercent%",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            Text(
                text = when {
                    scorePercent >= 80 -> AppStringsVi.TestExcellent
                    scorePercent >= 50 -> AppStringsVi.TestGood
                    else -> AppStringsVi.TestLow
                },
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )

            Spacer(Modifier.height(AppSpacing.xl))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(label = AppStringsVi.SessionRemembered, value = "$remembered", color = AppColors.Success)
                SummaryStatItem(label = AppStringsVi.SessionNeedReview, value = "$needReview", color = AppColors.Warning)
                SummaryStatItem(label = AppStringsVi.LearnTotalCards, value = "$totalCards", color = AppColors.OnSurface)
            }

            if (starredCount > 0) {
                Spacer(Modifier.height(AppSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$starredCount ${AppStringsVi.SessionStarred}",
                        style = AppTypography.labelMedium,
                        color = AppColors.Accent
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onRestart),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(AppSpacing.sm))
                        Text(
                            AppStringsVi.FlashcardLearnAgain,
                            style = AppTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Primary
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
                ) {
                Text(
                    AppStringsVi.FlashcardClose,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.md),
                    textAlign = TextAlign.Center
                )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FLASHCARD FILTER / SORT CHIPS ROW
// ═══════════════════════════════════════════════════════════════
@Composable
fun FlashcardFilterChips(
    filterLabel: String,
    sortLabel: String,
    isFilterActive: Boolean,
    isSortActive: Boolean,
    isShuffled: Boolean,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipCompact(
            label = filterLabel,
            isActive = isFilterActive,
            onClick = onFilterClick
        )
        FilterChipCompact(
            label = sortLabel,
            isActive = isSortActive,
            onClick = onSortClick
        )

        Spacer(Modifier.weight(1f))

        Card(
            modifier = Modifier.clickable(onClick = if (isShuffled) onResetClick else onShuffleClick),
            shape = RoundedCornerShape(AppRadius.chip),
            colors = CardDefaults.cardColors(
                containerColor = if (isShuffled) AppColors.Primary.copy(alpha = 0.1f) else AppColors.SurfaceVariant
            ),
            border = if (isShuffled) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
            else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isShuffled) Icons.Default.Refresh else Icons.Default.Shuffle,
                    contentDescription = if (isShuffled) AppStringsVi.FlashcardResetOrder else AppStringsVi.FlashcardShuffle,
                    tint = if (isShuffled) AppColors.Primary else AppColors.OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChipCompact(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.chip),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) AppColors.PrimaryContainer else AppColors.SurfaceVariant
        ),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
        else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) AppColors.Primary else AppColors.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
        )
    }
}
