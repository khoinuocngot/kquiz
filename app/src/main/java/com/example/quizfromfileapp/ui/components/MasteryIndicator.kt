package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppPrimary
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppSuccess
import com.example.quizfromfileapp.ui.theme.AppWarning
import com.example.quizfromfileapp.ui.theme.masteryColor

/**
 * Premium mastery progress bar with animated segments.
 * Shows 5 segments colored by mastery level.
 */
@Composable
fun MasteryIndicator(
    level: Int,
    maxLevel: Int = 5,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.toFloat(),
        animationSpec = tween(400),
        label = "masteryLevel"
    )

    Column(modifier = modifier) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mức độ thành thạo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$level / $maxLevel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = masteryColor(level)
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(maxLevel) { index ->
                val segmentProgress = (animatedLevel - index).coerceIn(0f, 1f)
                val segmentColor by animateColorAsState(
                    targetValue = when {
                        animatedLevel <= index -> MaterialTheme.colorScheme.outlineVariant
                        else -> masteryColor(index + 1)
                    },
                    animationSpec = tween(300),
                    label = "segmentColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(segmentColor)
                )
            }
        }
    }
}

/**
 * Compact mastery bar used inside card lists.
 */
@Composable
fun CompactMasteryBar(
    level: Int,
    maxLevel: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLevel) { index ->
            val color = if (index < level) masteryColor(level) else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Overall mastery percentage for a study set.
 */
@Composable
fun SetMasteryCard(
    masteredCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val percent = if (totalCount > 0) (masteredCount * 100 / totalCount) else 0
    val color = when {
        percent >= 80 -> AppSuccess
        percent >= 50 -> AppWarning
        else -> AppPrimary
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(color.copy(alpha = 0.08f))
            .padding(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "Thành thạo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
