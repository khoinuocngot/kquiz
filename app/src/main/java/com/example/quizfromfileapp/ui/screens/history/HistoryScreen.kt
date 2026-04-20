package com.example.quizfromfileapp.ui.screens.history

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumSectionHeader
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSize
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.HistoryTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actionsContent = {
                    if (uiState.items.isNotEmpty()) {
                        PremiumIconButton(
                            icon = Icons.Default.DeleteSweep,
                            onClick = { showClearDialog = true },
                            tint = AppColors.OnSurfaceVariant,
                            contentDescription = AppStringsVi.HistoryClearAll,
                            buttonSize = 40
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView(modifier = Modifier.padding(innerPadding))
                }
                uiState.items.isEmpty() -> {
                    EmptyHistoryView(
                        onNavigateBack = onNavigateBack,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                else -> {
                    HistoryListView(
                        items = uiState.items,
                        onDeleteItem = { viewModel.deleteItem(it) },
                        onClearAll = { showClearDialog = true },
                        onNavigateBack = onNavigateBack,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = AppStringsVi.HistoryClearConfirm,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = AppStringsVi.HistoryClearSub,
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = AppStringsVi.HistoryClearYes,
                        style = AppTypography.labelLarge,
                        color = AppColors.Error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        text = AppStringsVi.HistoryClearNo,
                        style = AppTypography.labelLarge,
                        color = AppColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(AppRadius.dialog)
        )
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = AppColors.Primary,
            trackColor = AppColors.Primary.copy(alpha = 0.15f),
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun EmptyHistoryView(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PremiumEmptyState(
            icon = Icons.Default.History,
            title = AppStringsVi.HistoryEmpty,
            subtitle = AppStringsVi.HistoryEmptySub,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun HistoryListView(
    items: List<QuizHistoryEntity>,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            item { Spacer(modifier = Modifier.height(AppSpacing.sm)) }

            item {
                PremiumSectionHeader(
                    title = "${items.size} kết quả",
                    modifier = Modifier.padding(vertical = AppSpacing.xs)
                )
            }

            items(
                items = items,
                key = { it.id }
            ) { item ->
                HistoryItemCard(
                    item = item,
                    onDelete = { onDeleteItem(item.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(AppSpacing.sm)) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.md)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.button),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Primary
                ),
                border = BorderStroke(1.5.dp, AppColors.Primary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    text = AppStringsVi.ActionBack,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: QuizHistoryEntity,
    onDelete: () -> Unit
) {
    val scoreColor = when {
        item.scorePercent >= 80 -> AppColors.ScoreExcellent
        item.scorePercent >= 50 -> AppColors.ScoreMedium
        else -> AppColors.ScoreLow
    }

    val scoreBgColor = when {
        item.scorePercent >= 80 -> AppColors.SuccessSurface
        item.scorePercent >= 50 -> AppColors.WarningSurface
        else -> AppColors.ErrorSurface
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface
        ),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadge(
                scorePercent = item.scorePercent,
                scoreColor = scoreColor,
                scoreBgColor = scoreBgColor
            )

            Spacer(modifier = Modifier.width(AppSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "${item.correctCount} / ${item.totalQuestions} đúng",
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyChip(text = item.difficulty)
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = dateFormatter.format(Date(item.createdAt)),
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }
            }

            PremiumIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                tint = AppColors.ErrorLight,
                contentDescription = AppStringsVi.Delete,
                buttonSize = 40
            )
        }
    }
}

@Composable
private fun ScoreBadge(
    scorePercent: Int,
    scoreColor: Color,
    scoreBgColor: Color
) {
    Card(
        modifier = Modifier.size(AppSize.avatarMedium),
        shape = RoundedCornerShape(AppRadius.md),
        colors = CardDefaults.cardColors(containerColor = scoreBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$scorePercent%",
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DifficultyChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.chip))
            .background(AppColors.Primary.copy(alpha = 0.1f))
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    ) {
        Text(
            text = text,
            style = AppTypography.labelSmall,
            color = AppColors.Primary,
            fontWeight = FontWeight.Medium
        )
    }
}
