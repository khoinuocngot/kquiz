package com.example.quizfromfileapp.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppPrimary
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSecondary
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppSuccess

/**
 * MCQ Option — một lựa chọn trong câu hỏi trắc nghiệm.
 */
@Composable
fun McqOptionItem(
    letter: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isAnswered && isCorrect == true -> AppSuccess.copy(alpha = 0.08f)
        isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        isSelected -> AppPrimary.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isAnswered && isCorrect == true -> AppSuccess
        isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.error
        isSelected -> AppPrimary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = when {
        isAnswered && isCorrect == true -> AppSuccess
        isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.error
        isSelected -> AppPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(AppRadius.md),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected || (isAnswered && isCorrect == true)) borderColor
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected || (isAnswered && isCorrect == true)) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isAnswered) {
                when {
                    isCorrect == true -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Đúng",
                        tint = AppSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    isSelected -> Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Sai",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * McqQuestionCard — hiển thị câu hỏi MCQ đã được tách.
 */
@Composable
fun McqQuestionCard(
    questionText: String,
    options: List<String>,
    selectedIndex: Int?,
    correctIndex: Int?,
    isAnswered: Boolean,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Question stem
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(AppRadius.md),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = questionText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.lg)
            )
        }

        // Options
        options.forEachIndexed { index, option ->
            val letter = ('A' + index).toString()
            McqOptionItem(
                letter = letter,
                text = option,
                isSelected = selectedIndex == index,
                isCorrect = if (isAnswered) index == correctIndex else null,
                isAnswered = isAnswered,
                onClick = { if (!isAnswered) onSelectOption(index) }
            )
        }
    }
}

/**
 * McqPreviewCard — preview MCQ trong danh sách thẻ.
 */
@Composable
fun McqPreviewCard(
    questionText: String,
    correctAnswer: String,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(AppRadius.sm),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Question
            Text(
                text = questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(AppSpacing.sm))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { index, option ->
                    val isCorrect = option == correctAnswer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCorrect) AppSuccess.copy(alpha = 0.06f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + index)}.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) AppSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCorrect) AppSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCorrect) {
                            Text("✓", color = AppSuccess, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * MCQ type indicator badge.
 */
@Composable
fun McqTypeBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.chip))
            .background(AppSecondary.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Quiz,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = AppSecondary
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Trắc nghiệm",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = AppSecondary
            )
        }
    }
}
