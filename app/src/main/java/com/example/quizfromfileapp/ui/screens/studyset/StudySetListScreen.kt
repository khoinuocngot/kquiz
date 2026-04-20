package com.example.quizfromfileapp.ui.screens.studyset

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumSearchBar
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySetListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (studySetId: Long) -> Unit,
    onShowQuickImport: () -> Unit,
    viewModel: StudySetListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    // Dialog states
    var deleteSetId by remember { mutableStateOf<Long?>(null) }
    var renameSetId by remember { mutableStateOf<Long?>(null) }
    var duplicateSetId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStringsVi.HomeMyStudySets,
                            style = AppTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                        )
                        if (uiState.filteredItems.isNotEmpty()) {
                            Text(
                                text = "${uiState.filteredItems.size} ${AppStringsVi.StudySetCards}",
                                style = AppTypography.bodySmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    PremiumIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = AppStringsVi.ActionBack,
                        tint = AppColors.OnSurface,
                        buttonSize = 40
                    )
                },
                actions = {
                    Box {
                        PremiumIconButton(
                            icon = Icons.AutoMirrored.Filled.Sort,
                            onClick = { showSortMenu = true },
                            contentDescription = AppStringsVi.SortNewest,
                            tint = AppColors.OnSurface,
                            buttonSize = 40
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (uiState.sortOption == option) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (uiState.sortOption == option) AppColors.Primary else AppColors.OnSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AppColors.OnSurface,
                    navigationIconContentColor = AppColors.OnSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowQuickImport,
                containerColor = AppColors.Primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStringsVi.HomeCreateNew,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(AppSpacing.sm))

            PremiumSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                placeholder = AppStringsVi.SearchPlaceholder,
                modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)
            )

            Spacer(Modifier.height(AppSpacing.sm))

            if (uiState.searchQuery.isNotBlank() || uiState.sortOption != SortOption.RECENTLY_UPDATED) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (uiState.searchQuery.isNotBlank()) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.updateSearchQuery("") },
                                label = { Text("Tìm: \"${uiState.searchQuery}\"", style = AppTypography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.PrimaryContainer,
                                    selectedLabelColor = AppColors.Primary
                                )
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateSortOption(SortOption.RECENTLY_UPDATED) },
                            label = { Text("Sắp: ${uiState.sortOption.displayName}", style = AppTypography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.PrimaryContainer,
                                selectedLabelColor = AppColors.Primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(AppSpacing.sm))
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
                uiState.filteredItems.isEmpty() && uiState.allItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PremiumEmptyState(
                            icon = Icons.Default.School,
                            title = AppStringsVi.HomeEmptyMySets,
                            subtitle = AppStringsVi.HomeEmptyMySetsSub,
                            action = {
                                TextButton(onClick = onShowQuickImport) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(AppSpacing.xs))
                                    Text(AppStringsVi.HomeCreateNew, style = AppTypography.titleSmall)
                                }
                            }
                        )
                    }
                }
                uiState.filteredItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PremiumEmptyState(
                            icon = Icons.Default.Search,
                            title = AppStringsVi.EmptyNoSearch,
                            subtitle = AppStringsVi.EmptyNoSearchSub
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = AppSpacing.screenPadding,
                            vertical = AppSpacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(items = uiState.filteredItems, key = { it.id }) { set ->
                            StudySetCardItem(
                                studySet = set,
                                masteryPercent = uiState.masteryPercents[set.id] ?: 0,
                                onClick = { onNavigateToDetail(set.id) },
                                onDelete = { deleteSetId = set.id },
                                onRename = { renameSetId = set.id },
                                onDuplicate = { duplicateSetId = set.id },
                                onTogglePin = { viewModel.togglePin(set.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(set.id) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Delete dialog
    deleteSetId?.let { setId ->
        val setToDelete = uiState.allItems.find { it.id == setId }
        if (setToDelete != null) {
            AlertDialog(
                onDismissRequest = { deleteSetId = null },
                title = {
                    Text(
                        AppStringsVi.DialogDeleteStudySet,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                },
                text = {
                    Text(
                        AppStringsVi.DialogDeleteStudySetSub.replace("{name}", setToDelete.title),
                        color = AppColors.OnSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteStudySet(setId)
                        deleteSetId = null
                    }) {
                        Text(AppStringsVi.DialogDeleteConfirm, color = AppColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteSetId = null }) {
                        Text(AppStringsVi.DialogDeleteCancel, color = AppColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }

    // Rename dialog
    renameSetId?.let { setId ->
        val setToRename = uiState.allItems.find { it.id == setId }
        if (setToRename != null) {
            var newTitle by remember { mutableStateOf(setToRename.title) }
            AlertDialog(
                onDismissRequest = { renameSetId = null },
                title = {
                    Text(AppStringsVi.DialogRenameStudySet, fontWeight = FontWeight.SemiBold, color = AppColors.OnSurface)
                },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(AppStringsVi.DialogRenameHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(AppRadius.input),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.Outline
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.renameStudySet(setId, newTitle.trim())
                            }
                            renameSetId = null
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text(AppStringsVi.DialogRenameConfirm, color = if (newTitle.isNotBlank()) AppColors.Primary else AppColors.OnSurfaceVariant)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameSetId = null }) {
                        Text(AppStringsVi.Cancel, color = AppColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }

    // Duplicate dialog
    duplicateSetId?.let { setId ->
        val setToDuplicate = uiState.allItems.find { it.id == setId }
        if (setToDuplicate != null) {
            AlertDialog(
                onDismissRequest = { duplicateSetId = null },
                title = {
                    Text(AppStringsVi.DialogDuplicateStudySet, fontWeight = FontWeight.SemiBold, color = AppColors.OnSurface)
                },
                text = {
                    Text(
                        AppStringsVi.DialogDuplicateSub.replace("{name}", setToDuplicate.title),
                        color = AppColors.OnSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.duplicateStudySet(setId)
                        duplicateSetId = null
                    }) {
                        Text(AppStringsVi.DialogDuplicateConfirm, color = AppColors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { duplicateSetId = null }) {
                        Text(AppStringsVi.Cancel, color = AppColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun StudySetCardItem(
    studySet: StudySetEntity,
    masteryPercent: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("vi", "VN")) }
    val formattedDate = remember(studySet.updatedAt) { dateFormatter.format(Date(studySet.updatedAt)) }

    val animatedProgress by animateFloatAsState(
        targetValue = masteryPercent / 100f,
        animationSpec = tween(600),
        label = "progress"
    )
    val progressColor = when {
        masteryPercent >= 80 -> AppColors.Success
        masteryPercent >= 40 -> AppColors.Warning
        else -> AppColors.Primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(110.dp)
                    .background(if (studySet.isPinned) AppColors.Primary else AppColors.OnSurfaceVariant.copy(alpha = 0.3f))
            )
            Column(modifier = Modifier.weight(1f).padding(AppSpacing.cardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (studySet.isPinned) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = AppStringsVi.StudySetListPin,
                                modifier = Modifier.size(14.dp),
                                tint = AppColors.Primary
                            )
                        }
                        if (studySet.isFavorite) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = AppStringsVi.StudySetListFav,
                                modifier = Modifier.size(14.dp),
                                tint = AppColors.Warning
                            )
                        }
                        Text(
                            text = studySet.title,
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        Card(
                            modifier = Modifier.clickable(onClick = onTogglePin).size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = if (studySet.isPinned) AppColors.Primary.copy(alpha = 0.12f) else AppColors.SurfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (studySet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (studySet.isPinned) AppStringsVi.StudySetListUnpin else AppStringsVi.StudySetListPin,
                                    tint = if (studySet.isPinned) AppColors.Primary else AppColors.OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Card(
                            modifier = Modifier.clickable(onClick = onToggleFavorite).size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = if (studySet.isFavorite) AppColors.Warning.copy(alpha = 0.12f) else AppColors.SurfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (studySet.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = if (studySet.isFavorite) AppStringsVi.StudySetListUnfav else AppStringsVi.StudySetListFav,
                                    tint = if (studySet.isFavorite) AppColors.Warning else AppColors.OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Card(
                            modifier = Modifier.clickable(onClick = onRename).size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = AppStringsVi.Rename, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Card(
                            modifier = Modifier.clickable(onClick = onDuplicate).size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ContentCopy, contentDescription = AppStringsVi.Duplicate, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Card(
                            modifier = Modifier.clickable(onClick = onDelete).size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.ErrorContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Delete, contentDescription = AppStringsVi.Delete, tint = AppColors.Error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                if (!studySet.description.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(
                        text = studySet.description,
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(AppSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${studySet.cardCount} ${AppStringsVi.StudySetCards}",
                        style = AppTypography.labelMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }

                Spacer(Modifier.height(AppSpacing.sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = "$masteryPercent%",
                        style = AppTypography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = progressColor
                    )
                }
            }
        }
    }
}
