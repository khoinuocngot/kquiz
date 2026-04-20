package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.data.repository.GamificationRepository
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

/**
 * Dialog cài đặt Daily Goal.
 */
@Composable
fun DailyGoalConfigDialog(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = GamificationRepository.DAILY_GOAL_OPTIONS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    AppStringsVi.DailyGoalConfig,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(
                    "Chọn số thẻ bạn muốn học mỗi ngày:",
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(AppSpacing.sm))

                options.forEach { goal ->
                    GoalOptionItem(
                        goal = goal,
                        isSelected = goal == currentGoal,
                        onClick = { onGoalSelected(goal) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    AppStringsVi.Done,
                    style = AppTypography.titleSmall,
                    color = AppColors.Primary
                )
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(AppRadius.dialog)
    )
}

@Composable
private fun GoalOptionItem(
    goal: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface
    val borderColor = if (isSelected) AppColors.Primary else AppColors.Outline

    val (label, emoji) = when (goal) {
        5 -> Pair(AppStringsVi.DailyGoal5, "🌱")
        10 -> Pair(AppStringsVi.DailyGoal10, "🌿")
        20 -> Pair(AppStringsVi.DailyGoal20, "🌳")
        30 -> Pair(AppStringsVi.DailyGoal30, "🔥")
        else -> Pair("$goal thẻ", "📚")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.md),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$emoji  $label",
                style = AppTypography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = AppColors.OnSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Widget hiển thị daily goal progress trên Home screen.
 */
@Composable
fun DailyGoalWidget(
    current: Int,
    target: Int,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    val isComplete = current >= target

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onConfigureClick),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                AppColors.Success.copy(alpha = 0.08f)
            else
                AppColors.Primary.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isComplete) "🎉" else "🎯",
                        style = AppTypography.titleMedium
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Column {
                        Text(
                            text = if (isComplete) AppStringsVi.DailyGoalComplete
                                   else AppStringsVi.DailyGoal,
                            style = AppTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = AppStringsVi.DailyGoalProgress.format(current, target),
                            style = AppTypography.bodySmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) AppColors.Success else AppColors.Primary
                )
            }

            if (!isComplete) {
                Spacer(Modifier.height(AppSpacing.sm))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppColors.Primary,
                    trackColor = AppColors.Primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Widget hiển thị XP và Level trên Home screen.
 */
@Composable
fun XpLevelWidget(
    totalXp: Int,
    currentLevel: Int,
    xpProgressInLevel: Float,
    modifier: Modifier = Modifier
) {
    val progressColor = AppColors.Warning

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Warning.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Level badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(AppColors.Warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentLevel",
                        style = AppTypography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Warning
                    )
                }
                Spacer(Modifier.width(AppSpacing.md))
                Column {
                    Text(
                        text = AppStringsVi.LevelLabel + " $currentLevel",
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "$totalXp XP",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${100 - ((xpProgressInLevel * 100).toInt())} XP",
                    style = AppTypography.labelSmall,
                    color = AppColors.Warning
                )
                Text(
                    text = "→ Lv${currentLevel + 1}",
                    style = AppTypography.labelSmall,
                    color = AppColors.OnSurfaceMuted
                )
            }
        }

        // XP progress bar
        LinearProgressIndicator(
            progress = { xpProgressInLevel },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = AppSpacing.md)
                .padding(bottom = AppSpacing.md),
            color = AppColors.Warning,
            trackColor = AppColors.Warning.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}
