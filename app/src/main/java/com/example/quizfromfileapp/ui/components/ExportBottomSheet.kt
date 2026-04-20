package com.example.quizfromfileapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

enum class ExportOptionType {
    STUDYSET, JSON, CSV, TXT, SHARE_TEXT
}

data class ExportOption(
    val type: ExportOptionType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onExportStudySet: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportTxt: () -> Unit,
    onShareText: () -> Unit,
    isExporting: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!isVisible) return

    val options = remember {
        listOf(
            ExportOption(
                type = ExportOptionType.STUDYSET,
                title = AppStringsVi.ExportStudySet,
                description = AppStringsVi.ExportStudySetDesc,
                icon = Icons.Default.Layers,
                color = AppColors.Primary
            ),
            ExportOption(
                type = ExportOptionType.JSON,
                title = AppStringsVi.ExportJson,
                description = AppStringsVi.ExportJsonDesc,
                icon = Icons.Default.Code,
                color = AppColors.Success
            ),
            ExportOption(
                type = ExportOptionType.CSV,
                title = AppStringsVi.ExportCsv,
                description = AppStringsVi.ExportCsvDesc,
                icon = Icons.Default.TableChart,
                color = AppColors.Secondary
            ),
            ExportOption(
                type = ExportOptionType.TXT,
                title = AppStringsVi.ExportTxt,
                description = AppStringsVi.ExportTxtDesc,
                icon = Icons.AutoMirrored.Filled.TextSnippet,
                color = AppColors.Accent
            ),
            ExportOption(
                type = ExportOptionType.SHARE_TEXT,
                title = AppStringsVi.ShareText,
                description = AppStringsVi.ShareTextDesc,
                icon = Icons.Default.Share,
                color = AppColors.Warning
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = AppRadius.card, topEnd = AppRadius.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .padding(bottom = AppSpacing.xxl)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStringsVi.ExportTitle,
                    style = AppTypography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Options
            options.forEach { option ->
                ExportOptionItem(
                    option = option,
                    isLoading = isExporting,
                    onClick = {
                        when (option.type) {
                            ExportOptionType.STUDYSET -> onExportStudySet()
                            ExportOptionType.JSON -> onExportJson()
                            ExportOptionType.CSV -> onExportCsv()
                            ExportOptionType.TXT -> onExportTxt()
                            ExportOptionType.SHARE_TEXT -> onShareText()
                        }
                    }
                )
                Spacer(Modifier.height(AppSpacing.sm))
            }

            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

@Composable
private fun ExportOptionItem(
    option: ExportOption,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = option.color.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, option.color.copy(alpha = 0.2f)),
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(option.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = option.color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(AppSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
                Text(
                    text = option.description,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = option.color.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}
