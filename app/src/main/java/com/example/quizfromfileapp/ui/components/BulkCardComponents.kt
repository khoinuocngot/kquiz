package com.example.quizfromfileapp.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

private val CircleShape = RoundedCornerShape(50)

@Composable
fun BulkActionBar(
    selectedCount: Int,
    totalCount: Int,
    isSelectAll: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onBulkStar: () -> Unit,
    onBulkUnstar: () -> Unit,
    onBulkDelete: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = AppRadius.card, topEnd = AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Column {
                    Text(
                        text = AppStringsVi.BulkSelected.format(selectedCount),
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "$selectedCount / $totalCount",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = AppStringsVi.BulkExit,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onExit),
                    tint = AppColors.OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = if (isSelectAll) onDeselectAll else onSelectAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button),
                    border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = if (isSelectAll) Icons.Default.Check else Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.Primary
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = if (isSelectAll) AppStringsVi.BulkDeselectAll else AppStringsVi.BulkSelectAll,
                        style = AppTypography.labelMedium,
                        color = AppColors.Primary
                    )
                }

                Button(
                    onClick = onBulkStar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Warning.copy(alpha = 0.1f),
                        contentColor = AppColors.Warning
                    ),
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = AppStringsVi.BulkStar,
                        style = AppTypography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Button(
                    onClick = onBulkUnstar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.OnSurfaceVariant.copy(alpha = 0.1f),
                        contentColor = AppColors.OnSurfaceVariant
                    ),
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = AppStringsVi.BulkUnstar,
                        style = AppTypography.labelMedium
                    )
                }

                Button(
                    onClick = onBulkDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Error.copy(alpha = 0.1f),
                        contentColor = AppColors.Error
                    ),
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = AppStringsVi.BulkDelete,
                        style = AppTypography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BulkCardCheckbox(
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) AppColors.Primary else Color.Transparent
    val borderColor = if (isSelected) AppColors.Primary else AppColors.Outline

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onToggle)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun BulkDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                AppStringsVi.BulkDeleteConfirm.format(count),
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                AppStringsVi.BulkDeleteSub,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    AppStringsVi.BulkDeleteConfirmBtn,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    AppStringsVi.Cancel,
                    style = AppTypography.titleSmall,
                    color = AppColors.OnSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(AppRadius.dialog)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (term: String, definition: String) -> Unit,
    studySetType: String = "TERM_DEFINITION",
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!isVisible) return

    var term by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }

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
            Text(
                text = AppStringsVi.BulkCardAddTitle,
                style = AppTypography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )

            Spacer(Modifier.height(AppSpacing.lg))

            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text(AppStringsVi.CardEditTerm) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.input),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary
                ),
                textStyle = AppTypography.bodyMedium
            )

            Spacer(Modifier.height(AppSpacing.md))

            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = { Text(AppStringsVi.CardEditDefinition) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.input),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary
                ),
                textStyle = AppTypography.bodyMedium
            )

            Spacer(Modifier.height(AppSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button)
                ) {
                    Text(
                        AppStringsVi.Cancel,
                        style = AppTypography.titleSmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (term.isNotBlank() && definition.isNotBlank()) {
                            onAdd(term, definition)
                            term = ""
                            definition = ""
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.button),
                    enabled = term.isNotBlank() && definition.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        AppStringsVi.BulkCardAdd,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
