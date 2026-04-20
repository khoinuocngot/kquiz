package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

// ═══════════════════════════════════════════════════════════════
// PREMIUM HERO SECTION — Home screen top
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumHeroSection(
    title: String,
    subtitle: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.cardLarge),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                    )
                )
                .padding(AppSpacing.xxl)
        ) {
            Column {
                Text(
                    text = title,
                    style = AppTypography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = AppTypography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(AppSpacing.xl))
                PremiumButtonRow(
                    text = AppStringsVi.HomeCreateNew,
                    onClick = onActionClick,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    iconEnd = true,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM QUICK ACTION CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = AppColors.Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(AppSpacing.md))
            Text(
                text = title,
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM STAT CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = AppColors.Primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column {
                Text(
                    text = value,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                Text(
                    text = label,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM STUDY SET CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumStudySetCard(
    title: String,
    description: String?,
    cardCount: Int,
    masteredCount: Int,
    lastUpdated: String,
    accentColor: Color = AppColors.Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (cardCount > 0) masteredCount.toFloat() / cardCount else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(AppSpacing.cardPadding)
            ) {
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(
                        text = description,
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(AppSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$cardCount ${AppStringsVi.StudySetCards}",
                        style = AppTypography.labelMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                    Text(
                        text = lastUpdated,
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = AppTypography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM FLASHCARD SURFACE
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumFlashcardSurface(
    term: String,
    definition: String,
    isFlipped: Boolean,
    isStarred: Boolean,
    masteryLevel: Int = 0,
    explanation: String? = null,
    sourcePage: String? = null,
    sourceSnippet: String? = null,
    onFlip: () -> Unit,
    onStarToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onFlip),
        shape = RoundedCornerShape(AppRadius.flashcard),
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped) AppColors.SurfaceVariant else AppColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isFlipped) AppColors.Outline else AppColors.Outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xxl)
        ) {
            if (!isFlipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onStarToggle != null) {
                        IconButton(
                            onClick = onStarToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star",
                                tint = if (isStarred) AppColors.Accent else AppColors.OnSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else { Spacer(Modifier.size(36.dp)) }

                    MasteryDots(level = masteryLevel)

                    Text(
                        text = AppStringsVi.FlashcardTapToFlip,
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }
                Spacer(Modifier.height(AppSpacing.xxl))
                Text(
                    text = term,
                    style = AppTypography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = AppStringsVi.FlashcardBack,
                    style = AppTypography.labelMedium,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(AppSpacing.md))
                Text(
                    text = definition,
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.OnSurface
                )
                if (!explanation.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Text(
                        text = "Giải thích",
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
                if (!sourcePage.isNullOrBlank() || !sourceSnippet.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Row {
                        if (!sourcePage.isNullOrBlank()) {
                            Text(
                                text = "Trang: $sourcePage",
                                style = AppTypography.labelSmall,
                                color = AppColors.OnSurfaceMuted
                            )
                        }
                        if (!sourceSnippet.isNullOrBlank()) {
                            if (!sourcePage.isNullOrBlank()) Spacer(Modifier.width(AppSpacing.md))
                            Text(
                                text = sourceSnippet.take(50),
                                style = AppTypography.labelSmall,
                                color = AppColors.OnSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(AppSpacing.lg))
                Text(
                    text = AppStringsVi.FlashcardTapToFlip2,
                    style = AppTypography.labelSmall,
                    color = AppColors.OnSurfaceMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM OPTION CARD — MCQ option
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumOptionCard(
    letter: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean? = null,
    isAnswered: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect == true -> AppColors.Success
            isAnswered && isSelected && isCorrect == false -> AppColors.Error
            isSelected -> AppColors.Primary
            else -> AppColors.Outline
        },
        animationSpec = tween(AppMotion.Normal),
        label = "borderColor"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect == true -> AppColors.SuccessContainer
            isAnswered && isSelected && isCorrect == false -> AppColors.ErrorContainer
            isSelected -> AppColors.PrimaryContainer
            else -> AppColors.Surface
        },
        animationSpec = tween(AppMotion.Normal),
        label = "bgColor"
    )

    val letterBg by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect == true -> AppColors.Success
            isAnswered && isSelected && isCorrect == false -> AppColors.Error
            isSelected -> AppColors.Primary
            else -> AppColors.SurfaceVariant
        },
        animationSpec = tween(AppMotion.Normal),
        label = "letterBg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isAnswered, onClick = onClick),
        shape = RoundedCornerShape(AppRadius.optionCard),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected || (isAnswered && isCorrect == true)) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .background(letterBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected || (isAnswered && isCorrect == true)) Color.White
                    else AppColors.OnSurfaceVariant
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Text(
                text = text,
                style = AppTypography.bodyLarge,
                color = when {
                    isAnswered && isCorrect == true -> AppColors.Success
                    isAnswered && isSelected && isCorrect == false -> AppColors.Error
                    isSelected -> AppColors.Primary
                    else -> AppColors.OnSurface
                },
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isAnswered) {
                Icon(
                    imageVector = if (isCorrect == true) Icons.Default.Check else if (isSelected) Icons.Default.Close else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect == true) AppColors.Success else AppColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM PROGRESS HEADER
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumProgressHeader(
    currentIndex: Int,
    totalCount: Int,
    correctCount: Int,
    wrongCount: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    timerLabel: String? = null,
    flaggedCount: Int = 0
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${AppStringsVi.TestProgress} ${currentIndex + 1} ${AppStringsVi.FlashcardOf} $totalCount",
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                if (timerLabel != null) {
                    Text(
                        text = timerLabel,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Primary
                    )
                }
                if (flaggedCount > 0) {
                    Text(
                        text = "$flaggedCount ${AppStringsVi.TestFlagged}",
                        style = AppTypography.labelSmall,
                        color = AppColors.Warning
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppColors.Primary,
            trackColor = AppColors.PrimaryContainer
        )

        Spacer(Modifier.height(AppSpacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.Success))
                Spacer(Modifier.width(AppSpacing.xs))
                Text("$correctCount ${AppStringsVi.FlashcardCorrect}", style = AppTypography.labelSmall, color = AppColors.Success)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.Error))
                Spacer(Modifier.width(AppSpacing.xs))
                Text("$wrongCount ${AppStringsVi.FlashcardWrong}", style = AppTypography.labelSmall, color = AppColors.Error)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM QUESTION PALETTE
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumQuestionPalette(
    totalQuestions: Int,
    currentIndex: Int,
    answeredIndices: Set<Int>,
    flaggedIndices: Set<Int>,
    onQuestionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items((0 until totalQuestions).toList()) { index ->
            val isCurrent = index == currentIndex
            val isAnswered = index in answeredIndices
            val isFlagged = index in flaggedIndices

            val bgColor = when {
                isCurrent -> AppColors.Primary
                isAnswered -> AppColors.SuccessContainer
                else -> AppColors.SurfaceVariant
            }
            val textColor = when {
                isCurrent -> Color.White
                isAnswered -> AppColors.Success
                else -> AppColors.OnSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onQuestionClick(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isFlagged) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(1.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AppColors.Warning)
                    )
                }
                Text(
                    text = "${index + 1}",
                    style = AppTypography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM ACTION CARD — Detail screen
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = AppColors.Primary,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isHighlighted) AppColors.PrimaryContainer else AppColors.Surface

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isHighlighted) {
            androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Primary.copy(alpha = 0.3f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(AppSpacing.md))
            Text(
                text = title,
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(AppSpacing.xxs))
            Text(
                text = subtitle,
                style = AppTypography.bodySmall,
                color = AppColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MASTERY DOTS
// ═══════════════════════════════════════════════════════════════
@Composable
fun MasteryDots(level: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            val filled = index < level
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) AppColors.masteryColor(level)
                        else AppColors.OnSurfaceMuted.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM RESULT HERO CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumResultHeroCard(
    scorePercent: Int,
    correctCount: Int,
    totalCount: Int,
    wrongCount: Int,
    flaggedCount: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = AppColors.scoreColor(scorePercent)
    val message = when {
        scorePercent >= 90 -> AppStringsVi.TestExcellent
        scorePercent >= 70 -> AppStringsVi.TestGood
        scorePercent >= 50 -> AppStringsVi.TestMedium
        else -> AppStringsVi.TestLow
    }

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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$scorePercent%",
                        style = AppTypography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            Text(
                text = message,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )

            Spacer(Modifier.height(AppSpacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStat(label = AppStringsVi.TestCorrect, value = "$correctCount", color = AppColors.Success)
                ResultStat(label = AppStringsVi.TestWrong, value = "$wrongCount", color = AppColors.Error)
                ResultStat(label = AppStringsVi.TestTotal, value = "$totalCount", color = AppColors.OnSurface)
            }

            if (flaggedCount > 0) {
                Spacer(Modifier.height(AppSpacing.md))
                Text(
                    text = "$flaggedCount câu đánh dấu",
                    style = AppTypography.labelMedium,
                    color = AppColors.Warning
                )
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
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
