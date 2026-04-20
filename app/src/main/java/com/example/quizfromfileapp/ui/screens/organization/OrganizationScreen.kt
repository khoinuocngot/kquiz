package com.example.quizfromfileapp.ui.screens.organization

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.FolderEntity
import com.example.quizfromfileapp.data.local.entity.TagEntity
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrganizationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = "Tổ chức bộ học",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) viewModel.showCreateFolderDialog()
                    else viewModel.showCreateTagDialog()
                },
                containerColor = AppColors.Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo mới", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppColors.Surface,
                contentColor = AppColors.OnSurface,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AppColors.Primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStringsVi.FolderTitle, style = AppTypography.labelMedium)
                        }
                    },
                    selectedContentColor = AppColors.Primary,
                    unselectedContentColor = AppColors.OnSurfaceVariant
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStringsVi.TagTitle, style = AppTypography.labelMedium)
                        }
                    },
                    selectedContentColor = AppColors.Primary,
                    unselectedContentColor = AppColors.OnSurfaceVariant
                )
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
                selectedTab == 0 -> FoldersListContent(
                    folders = uiState.folders,
                    onEdit = viewModel::showEditFolderDialog,
                    onDelete = viewModel::showDeleteFolderDialog
                )
                selectedTab == 1 -> TagsListContent(
                    tags = uiState.tags,
                    onEdit = viewModel::showEditTagDialog,
                    onDelete = viewModel::showDeleteTagDialog
                )
            }
        }
    }

    // ─── Folder Dialog ────────────────────────────────────────
    if (uiState.showFolderDialog) {
        FolderDialog(
            isEditing = uiState.editingFolder != null,
            name = uiState.folderName,
            description = uiState.folderDescription,
            color = uiState.folderColor,
            nameError = uiState.folderNameError,
            onNameChange = viewModel::updateFolderName,
            onDescriptionChange = viewModel::updateFolderDescription,
            onColorChange = viewModel::updateFolderColor,
            onSave = viewModel::saveFolder,
            onDismiss = viewModel::dismissFolderDialog
        )
    }

    // ─── Tag Dialog ───────────────────────────────────────────
    if (uiState.showTagDialog) {
        TagDialog(
            isEditing = uiState.editingTag != null,
            name = uiState.tagName,
            color = uiState.tagColor,
            nameError = uiState.tagNameError,
            onNameChange = viewModel::updateTagName,
            onColorChange = viewModel::updateTagColor,
            onSave = viewModel::saveTag,
            onDismiss = viewModel::dismissTagDialog
        )
    }

    // ─── Delete Folder Dialog ─────────────────────────────────
    if (uiState.showDeleteFolderDialog) {
        val folder = uiState.folders.find { it.folder.id == uiState.deletingFolderId }?.folder
        if (folder != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteFolderDialog,
                title = { Text(AppStringsVi.FolderDelete, style = AppTypography.titleMedium, fontWeight = FontWeight.SemiBold) },
                text = { Text(AppStringsVi.FolderDeleteSub.replace("{name}", folder.name), style = AppTypography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteFolder) {
                        Text(AppStringsVi.Delete, color = AppColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteFolderDialog) {
                        Text(AppStringsVi.Cancel, color = AppColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }

    // ─── Delete Tag Dialog ────────────────────────────────────
    if (uiState.showDeleteTagDialog) {
        val tag = uiState.tags.find { it.tag.id == uiState.deletingTagId }?.tag
        if (tag != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteTagDialog,
                title = { Text(AppStringsVi.TagDelete, style = AppTypography.titleMedium, fontWeight = FontWeight.SemiBold) },
                text = { Text(AppStringsVi.TagDeleteSub.replace("{name}", tag.name), style = AppTypography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteTag) {
                        Text(AppStringsVi.Delete, color = AppColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteTagDialog) {
                        Text(AppStringsVi.Cancel, color = AppColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FOLDERS LIST
// ═══════════════════════════════════════════════════════════════
@Composable
private fun FoldersListContent(
    folders: List<FolderWithCount>,
    onEdit: (FolderEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PremiumEmptyState(
                icon = Icons.Default.Folder,
                title = AppStringsVi.FolderEmpty,
                subtitle = AppStringsVi.FolderEmptySub
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(folders, key = { it.folder.id }) { item ->
                FolderCard(
                    folder = item.folder,
                    studySetCount = item.studySetCount,
                    onEdit = { onEdit(item.folder) },
                    onDelete = { onDelete(item.folder.id) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntity,
    studySetCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val folderColor = try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch (_: Exception) { AppColors.Primary }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(90.dp)
                    .background(folderColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(AppSpacing.md)
            ) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = folder.name,
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = AppStringsVi.Edit, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = AppStringsVi.Delete, tint = AppColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
                if (folder.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = folder.description,
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    text = "$studySetCount ${AppStringsVi.FolderSets}",
                    style = AppTypography.labelSmall,
                    color = AppColors.OnSurfaceMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAGS LIST
// ═══════════════════════════════════════════════════════════════
@Composable
private fun TagsListContent(
    tags: List<TagWithCount>,
    onEdit: (TagEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (tags.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PremiumEmptyState(
                icon = Icons.Default.Label,
                title = AppStringsVi.TagEmpty,
                subtitle = AppStringsVi.TagEmptySub
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(tags, key = { it.tag.id }) { item ->
                TagCard(
                    tag = item.tag,
                    studySetCount = item.studySetCount,
                    onEdit = { onEdit(item.tag) },
                    onDelete = { onDelete(item.tag.id) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TagCard(
    tag: TagEntity,
    studySetCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (_: Exception) { AppColors.Accent }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tagColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(AppSpacing.md))
                    Column {
                        Text(
                            text = tag.name,
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "$studySetCount ${AppStringsVi.FolderSets}",
                            style = AppTypography.labelSmall,
                            color = AppColors.OnSurfaceMuted
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = AppStringsVi.Edit, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = AppStringsVi.Delete, tint = AppColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
    }
}

// ═══════════════════════════════════════════════════════════════
// FOLDER DIALOG
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderDialog(
    isEditing: Boolean,
    name: String,
    description: String,
    color: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FolderEntity.DEFAULT_COLORS

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.dialog),
        containerColor = AppColors.Surface,
        title = {
            Text(
                if (isEditing) AppStringsVi.FolderEdit else AppStringsVi.FolderCreate,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(AppStringsVi.FolderNameHint) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = AppColors.Error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(AppStringsVi.FolderDescHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    )
                )
                Text(
                    "Màu sắc",
                    style = AppTypography.labelMedium,
                    color = AppColors.OnSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    colors.forEach { colorHex ->
                        val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { AppColors.Primary }
                        val isSelected = color == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .clickable { onColorChange(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(AppStringsVi.Save, style = AppTypography.labelLarge, color = AppColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsVi.Cancel, style = AppTypography.labelLarge, color = AppColors.OnSurfaceVariant)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// TAG DIALOG
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagDialog(
    isEditing: Boolean,
    name: String,
    color: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TagEntity.DEFAULT_COLORS

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.dialog),
        containerColor = AppColors.Surface,
        title = {
            Text(
                if (isEditing) AppStringsVi.TagEdit else AppStringsVi.TagCreate,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(AppStringsVi.TagNameHint) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = AppColors.Error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    )
                )
                Text(
                    "Màu sắc",
                    style = AppTypography.labelMedium,
                    color = AppColors.OnSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    colors.forEach { colorHex ->
                        val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { AppColors.Accent }
                        val isSelected = color == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .clickable { onColorChange(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(AppStringsVi.Save, style = AppTypography.labelLarge, color = AppColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsVi.Cancel, style = AppTypography.labelLarge, color = AppColors.OnSurfaceVariant)
            }
        }
    )
}
