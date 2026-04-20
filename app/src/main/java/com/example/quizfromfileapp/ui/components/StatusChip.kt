package com.example.quizfromfileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppPrimary
import com.example.quizfromfileapp.ui.theme.AppSuccess
import com.example.quizfromfileapp.ui.theme.AppTertiary
import com.example.quizfromfileapp.ui.theme.AppWarning

/**
 * Trạng thái mastery của một thẻ.
 */
enum class CardStatus(val label: String, val color: Color) {
    NEW("Mới", AppPrimary),
    LEARNING("Đang học", AppWarning),
    MASTERED("Đã thành thạo", AppSuccess),
    REVIEW("Cần ôn lại", AppTertiary)
}

/**
 * Chip hiển thị trạng thái thẻ với icon và label.
 * Dùng trong card list và preview.
 */
@Composable
fun StatusChip(
    status: CardStatus,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (status) {
        CardStatus.NEW -> Icons.Filled.Circle to "Mới"
        CardStatus.LEARNING -> Icons.Filled.Refresh to "Đang học"
        CardStatus.MASTERED -> Icons.Filled.CheckCircle to "Đã thành thạo"
        CardStatus.REVIEW -> Icons.Filled.Star to "Cần ôn lại"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(status.color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = status.color
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = status.color
            )
        }
    }
}

/**
 * Study set type chip — hiển thị loại bộ học.
 */
@Composable
fun StudyTypeChip(
    isQuestionAnswer: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isQuestionAnswer) MaterialTheme.colorScheme.secondary else AppPrimary
    val label = if (isQuestionAnswer) "Câu hỏi - Đáp án" else "Thuật ngữ - Định nghĩa"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Compact dot indicator for mastery level (0-5).
 * Shows 5 dots, filled up to current level.
 */
@Composable
fun MasteryDots(
    level: Int,
    maxLevel: Int = 5,
    modifier: Modifier = Modifier
) {
    val color = when {
        level == 0 -> MaterialTheme.colorScheme.outlineVariant
        level <= 2 -> AppWarning
        level <= 3 -> MaterialTheme.colorScheme.tertiary
        else -> AppSuccess
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLevel) { index ->
            Box(
                modifier = Modifier
                    .size(if (index < level) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index < level) color else MaterialTheme.colorScheme.outlineVariant
                    )
            )
            if (index < maxLevel - 1) {
                Spacer(Modifier.width(3.dp))
            }
        }
    }
}
