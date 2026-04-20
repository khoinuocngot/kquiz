package com.example.quizfromfileapp.ui.screens.studyset.detail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.ui.components.AddCardBottomSheet
import com.example.quizfromfileapp.ui.components.BulkActionBar
import com.example.quizfromfileapp.ui.components.BulkCardCheckbox
import com.example.quizfromfileapp.ui.components.BulkDeleteConfirmDialog
import com.example.quizfromfileapp.ui.components.ExportBottomSheet
import com.example.quizfromfileapp.ui.components.PremiumActionCard
import com.example.quizfromfileapp.ui.components.PremiumButton
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumSearchBar
import com.example.quizfromfileapp.ui.components.PremiumSectionHeader
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.components.StudyTypeChip
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppSuccess
import com.example.quizfromfileapp.ui.theme.AppTypography
import com.example.quizfromfileapp.ui.theme.AppWarning
import com.example.quizfromfileapp.ui.theme.masteryColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySetDetailScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToFlashcards: (studySetId: Long) -> Unit,
    onNavigateToLearn: (studySetId: Long) -> Unit,
    onNavigateToTestConfig: (studySetId: Long) -> Unit,
    onEditCard: (studySetId: Long, cardId: Long) -> Unit,
    viewModel: StudySetDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteSetDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showAddCardSheet by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(studySetId) {
        viewModel.setContext(context)
        viewModel.loadStudySet(studySetId)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudySetDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is StudySetDetailEvent.ShareText -> {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(sendIntent, "Chia sẻ bộ học")
                    )
                }
                is StudySetDetailEvent.ExportFile -> {
                    try {
                        context.startActivity(event.intent)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Không thể mở file. Lưu vào bộ nhớ trong.")
                    }
                }
                is StudySetDetailEvent.ExportSuccess -> {
                    snackbarHostState.showSnackbar("Đã xuất: ${event.fileName}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isBulkMode) {
                // Bulk mode top bar
                androidx.compose.material3.Surface(
                    color = AppColors.Surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::exitBulkMode) {
                            Icon(Icons.Default.Close, "Thoát", tint = AppColors.OnSurface)
                        }
                        Text(
                            text = AppStringsVi.BulkSelected.format(uiState.selectedCount),
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                PremiumTopBarSurface(
                    title = uiState.studySet?.title ?: AppStringsVi.StudySetDetail,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack,
                    actionsContent = {
                        Box {
                            PremiumIconButton(
                                icon = Icons.Default.MoreVert,
                                onClick = { showMenu = true },
                                tint = AppColors.OnSurface,
                                contentDescription = "Tùy chọn"
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            AppStringsVi.DetailEditInfo,
                                            style = AppTypography.bodyMedium,
                                            color = AppColors.OnSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.startEditing()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(AppStringsVi.BulkSelect, style = AppTypography.bodyMedium, color = AppColors.OnSurface)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Checklist, contentDescription = null, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.enterBulkMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(AppStringsVi.ExportShare, style = AppTypography.bodyMedium, color = AppColors.OnSurface)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = AppColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        showExportSheet = true
                                    }
                                )
                                uiState.studySet?.let { set ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (set.isPinned) AppStringsVi.DetailUnpin else AppStringsVi.DetailPin,
                                                style = AppTypography.bodyMedium,
                                                color = AppColors.OnSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (set.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = null,
                                                tint = if (set.isPinned) AppColors.Primary else AppColors.OnSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.togglePin()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (set.isFavorite) AppStringsVi.DetailUnfavorite else AppStringsVi.DetailFavorite,
                                                style = AppTypography.bodyMedium,
                                                color = AppColors.OnSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (set.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = null,
                                                tint = if (set.isFavorite) AppColors.Warning else AppColors.OnSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.toggleFavorite()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(AppStringsVi.DetailDelete, style = AppTypography.bodyMedium, color = AppColors.Error)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteSetDialog = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
                uiState.studySet == null -> {
                    PremiumEmptyState(
                        icon = Icons.Default.Layers,
                        title = AppStringsVi.DetailNotFound,
                        subtitle = AppStringsVi.DetailNotFoundSub,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    StudySetDetailContent(
                        uiState = uiState,
                        onNavigateToFlashcards = { onNavigateToFlashcards(studySetId) },
                        onNavigateToLearn = { onNavigateToLearn(studySetId) },
                        onNavigateToTestConfig = { onNavigateToTestConfig(studySetId) },
                        onToggleStar = viewModel::toggleStar,
                        onDeleteCard = { card -> viewModel.showDeleteCard(card.id) },
                        onEditCard = { card -> onEditCard(studySetId, card.id) },
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onClearSearch = viewModel::clearSearch,
                        onToggleSelection = viewModel::toggleCardSelection,
                        onAddCard = { showAddCardSheet = true },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bulk action bar at bottom
                    if (uiState.isBulkMode) {
                        BulkActionBar(
                            selectedCount = uiState.selectedCount,
                            totalCount = uiState.totalCards,
                            isSelectAll = uiState.allSelected,
                            onSelectAll = viewModel::selectAllCards,
                            onDeselectAll = viewModel::deselectAllCards,
                            onBulkStar = viewModel::bulkStarSelected,
                            onBulkUnstar = viewModel::bulkUnstarSelected,
                            onBulkDelete = { showBulkDeleteDialog = true },
                            onExit = viewModel::exitBulkMode,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    // Edit dialog
    if (uiState.isEditing) {
        EditDialog(
            title = uiState.editTitle,
            description = uiState.editDescription,
            onTitleChange = viewModel::updateEditTitle,
            onDescriptionChange = viewModel::updateEditDescription,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::cancelEdit
        )
    }

    // Delete card dialog
    uiState.deleteCardId?.let { cardId ->
        val card = uiState.flashcards.find { it.id == cardId }
        if (card != null) {
            AlertDialog(
                onDismissRequest = viewModel::hideDeleteCard,
                title = {
                    Text(
                        AppStringsVi.DialogDeleteCard,
                        fontWeight = FontWeight.SemiBold,
                        style = AppTypography.titleMedium
                    )
                },
                text = {
                    Text(
                        AppStringsVi.DialogDeleteCardSub.replace("{term}", card.term),
                        style = AppTypography.bodyMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteCard) {
                        Text(
                            AppStringsVi.Delete,
                            color = AppColors.Error,
                            style = AppTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::hideDeleteCard) {
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
    }

    // Delete study set dialog
    if (showDeleteSetDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSetDialog = false },
            title = {
                Text(
                    AppStringsVi.DialogDeleteStudySet,
                    fontWeight = FontWeight.SemiBold,
                    style = AppTypography.titleMedium
                )
            },
            text = {
                Text(
                    AppStringsVi.DialogDeleteStudySetSub.replace("{name}", uiState.studySet?.title ?: ""),
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSetDialog = false
                    onNavigateBack()
                }) {
                    Text(
                        AppStringsVi.Delete,
                        color = AppColors.Error,
                        style = AppTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSetDialog = false }) {
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

    // Bulk delete confirm dialog
    if (showBulkDeleteDialog) {
        BulkDeleteConfirmDialog(
            count = uiState.selectedCount,
            onConfirm = {
                showBulkDeleteDialog = false
                viewModel.bulkDeleteSelected { }
            },
            onDismiss = { showBulkDeleteDialog = false }
        )
    }

    // Export bottom sheet
    val exportSheetState = rememberModalBottomSheetState()
    ExportBottomSheet(
        isVisible = showExportSheet,
        onDismiss = { showExportSheet = false },
        onExportStudySet = {
            showExportSheet = false
            scope.launch { viewModel.exportToStudySet() }
        },
        onExportJson = {
            showExportSheet = false
            scope.launch { viewModel.exportToJson() }
        },
        onExportCsv = {
            showExportSheet = false
            scope.launch { viewModel.exportToCsv() }
        },
        onExportTxt = {
            showExportSheet = false
            scope.launch { viewModel.exportToTxt() }
        },
        onShareText = {
            showExportSheet = false
            scope.launch { viewModel.shareAsText() }
        },
        isExporting = uiState.isExporting,
        sheetState = exportSheetState
    )

    // Add card bottom sheet
    val addCardSheetState = rememberModalBottomSheetState()
    AddCardBottomSheet(
        isVisible = showAddCardSheet,
        onDismiss = { showAddCardSheet = false },
        onAdd = { term, definition ->
            viewModel.addCard(term, definition)
        },
        sheetState = addCardSheetState
    )
}

@Composable
private fun StudySetDetailContent(
    uiState: StudySetDetailUiState,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToTestConfig: () -> Unit,
    onToggleStar: (FlashcardEntity) -> Unit,
    onDeleteCard: (FlashcardEntity) -> Unit,
    onEditCard: (FlashcardEntity) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val set = uiState.studySet ?: return
    val displayedCards = uiState.displayedCards
    val isSearching = uiState.searchQuery.isNotBlank()

    LazyColumn(modifier = modifier) {
        item { StudySetHeader(set = set, avgMasteryPercent = uiState.avgMasteryPercent) }
        item { StatsSection(uiState = uiState) }
        item { MasteryProgressSection(uiState = uiState) }
        item {
            ActionButtonsSection(
                cardCount = set.cardCount,
                onNavigateToFlashcards = onNavigateToFlashcards,
                onNavigateToLearn = onNavigateToLearn,
                onNavigateToTestConfig = onNavigateToTestConfig
            )
        }

        // Search bar
        item {
            PremiumSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = AppStringsVi.CardSearchPlaceholder,
                modifier = Modifier.padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.md)
            )
        }

        // Search result chips
        if (isSearching) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = onClearSearch,
                        label = {
                            Text(
                                "${displayedCards.size} ${AppStringsVi.StudySetCards}",
                                style = AppTypography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Primary.copy(alpha = 0.12f),
                            selectedLabelColor = AppColors.Primary
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = AppStringsVi.CardSearchClear,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                Spacer(Modifier.height(AppSpacing.sm))
            }
        }

        if (displayedCards.isNotEmpty()) {
            // Header row with add button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding)
                        .padding(top = AppSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearching) {
                        Text(
                            text = "${AppStringsVi.DetailLabel} (${displayedCards.size})",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface
                        )
                    } else {
                        Text(
                            text = AppStringsVi.DetailCardPreview.format(set.cardCount),
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        if (!uiState.isBulkMode) {
                            androidx.compose.material3.TextButton(onClick = onAddCard) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = AppStringsVi.BulkCardAdd,
                                    modifier = Modifier.size(18.dp),
                                    tint = AppColors.Primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    AppStringsVi.BulkCardAdd,
                                    style = AppTypography.labelMedium,
                                    color = AppColors.Primary
                                )
                            }
                        }
                        if (!isSearching && displayedCards.size > uiState.previewCardCount && !uiState.isBulkMode) {
                            androidx.compose.material3.TextButton(onClick = onNavigateToFlashcards) {
                                Text(
                                    AppStringsVi.DetailSeeAll,
                                    style = AppTypography.labelMedium,
                                    color = AppColors.Primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.Primary
                                )
                            }
                        }
                    }
                }
            }

            val cardsToShow = if (isSearching) displayedCards else displayedCards.take(uiState.previewCardCount)
            items(
                items = cardsToShow,
                key = { "card_${it.id}" }
            ) { card ->
                if (uiState.isBulkMode) {
                    BulkCardPreviewItem(
                        card = card,
                        isSelected = uiState.selectedCardIds.contains(card.id),
                        onToggle = { onToggleSelection(card.id) },
                        modifier = Modifier.padding(horizontal = AppSpacing.screenPadding, vertical = 4.dp)
                    )
                } else {
                    FlashcardPreviewItem(
                        card = card,
                        onToggleStar = { onToggleStar(card) },
                        onDelete = { onDeleteCard(card) },
                        onEdit = { onEditCard(card) },
                        modifier = Modifier.padding(horizontal = AppSpacing.screenPadding, vertical = 4.dp)
                    )
                }
            }

            if (!isSearching) {
                item { Spacer(Modifier.height(AppSpacing.sectionGapLarge)) }
            }
        } else if (isSearching) {
            item {
                Box(modifier = Modifier.padding(AppSpacing.sectionGap)) {
                    PremiumEmptyState(
                        icon = Icons.Default.Search,
                        title = AppStringsVi.CardSearchNoResult,
                        subtitle = AppStringsVi.CardSearchNoResultSub
                    )
                }
            }
        } else {
            // Empty state with add button
            item {
                Box(modifier = Modifier.padding(AppSpacing.sectionGap)) {
                    PremiumEmptyState(
                        icon = Icons.Default.Layers,
                        title = AppStringsVi.StudySetEmpty,
                        subtitle = "Thêm thẻ đầu tiên nào!",
                        action = {
                            PremiumButton(
                                text = AppStringsVi.BulkCardAdd,
                                onClick = onAddCard,
                                modifier = Modifier.padding(top = AppSpacing.md)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudySetHeader(set: StudySetEntity, avgMasteryPercent: Int) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("vi-VN")) }
    val formattedDate = remember(set.updatedAt) { dateFormatter.format(Date(set.updatedAt)) }
    val isQA = set.studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
    val sourceColor = when (set.sourceType) {
        StudySetEntity.SOURCE_TYPE_QUICK_IMPORT -> AppSuccess
        StudySetEntity.SOURCE_TYPE_FILE_IMPORT -> AppColors.Info
        else -> AppColors.OnSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.screenPadding)
    ) {
        // Pin/Favorite badges
        if (set.isPinned || set.isFavorite) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (set.isPinned) {
                    BadgeChip(
                        icon = Icons.Filled.PushPin,
                        label = AppStringsVi.DetailPinned,
                        color = AppColors.Primary
                    )
                }
                if (set.isFavorite) {
                    BadgeChip(
                        icon = Icons.Filled.Star,
                        label = AppStringsVi.DetailFavorited,
                        color = AppColors.Warning
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.sm))
        }

        Text(
            text = set.title,
            style = AppTypography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface
        )

        if (set.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = set.description,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(AppSpacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudyTypeChip(isQuestionAnswer = isQA)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.chip))
                    .background(sourceColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = set.sourceTypeLabel,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = sourceColor
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))

        Text(
            text = AppStringsVi.DetailUpdated.format(formattedDate),
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceMuted
        )
    }
}

@Composable
private fun BadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.chip))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatsSection(uiState: StudySetDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            StatPill(
                label = AppStringsVi.DetailTotal,
                value = "${uiState.totalCards}",
                icon = Icons.Default.Layers,
                color = AppColors.Primary,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                label = AppStringsVi.DetailStarred,
                value = "${uiState.starredCount}",
                icon = Icons.Default.Star,
                color = AppColors.Warning,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                label = AppStringsVi.DetailNeedsStudy,
                value = "${uiState.needsReviewCount}",
                icon = Icons.Default.School,
                color = AppColors.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(AppRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = AppTypography.titleMedium,
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
}

@Composable
private fun MasteryProgressSection(uiState: StudySetDetailUiState) {
    val progress by animateFloatAsState(
        targetValue = uiState.avgMasteryPercent / 100f,
        animationSpec = tween(600),
        label = "masteryProgress"
    )
    val progressColor = masteryColor((uiState.avgMasteryPercent / 20f).toInt().coerceIn(0, 5))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding)
            .padding(top = AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStringsVi.DetailProgress,
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Text(
                text = "${uiState.avgMasteryPercent}%",
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
        Spacer(Modifier.height(AppSpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${uiState.masteredCount} ${AppStringsVi.DetailMastered}",
                style = AppTypography.labelSmall,
                color = AppColors.Success
            )
            Text(
                text = "${uiState.needsReviewCount} ${AppStringsVi.DetailNeedsReview}",
                style = AppTypography.labelSmall,
                color = AppColors.Error
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    cardCount: Int,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToTestConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding)
            .padding(top = AppSpacing.md)
    ) {
        Text(
            text = AppStringsVi.DetailChooseMode,
            style = AppTypography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            modifier = Modifier.padding(bottom = AppSpacing.sm)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            PremiumActionCard(
                title = AppStringsVi.DetailFlashcards,
                subtitle = AppStringsVi.FlashcardTapToFlip,
                icon = Icons.Default.Layers,
                iconTint = AppColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFlashcards
            )
            PremiumActionCard(
                title = AppStringsVi.DetailLearn,
                subtitle = AppStringsVi.LearnTitle,
                icon = Icons.Default.School,
                iconTint = AppSuccess,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToLearn
            )
            PremiumActionCard(
                title = AppStringsVi.DetailTest,
                subtitle = AppStringsVi.TestTitle,
                icon = Icons.Default.Quiz,
                iconTint = AppColors.Warning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTestConfig
            )
        }
    }
}

@Composable
private fun FlashcardPreviewItem(
    card: FlashcardEntity,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (card.isStarred) AppColors.Warning.copy(alpha = 0.3f) else AppColors.Outline

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.term,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (card.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = AppStringsVi.FlashcardStarred,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onToggleStar() },
                        tint = if (card.isStarred) AppColors.Warning else AppColors.OnSurfaceMuted
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = AppStringsVi.Edit,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onEdit() },
                        tint = AppColors.OnSurfaceMuted.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = AppStringsVi.Delete,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDelete() },
                        tint = AppColors.Error.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = card.definition,
                style = AppTypography.bodySmall,
                color = AppColors.OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (card.masteryLevel > 0) {
                Spacer(Modifier.height(AppSpacing.sm))
                val masteryProgress = card.masteryLevel / 5f
                val masteryColorAnimated by animateFloatAsState(
                    targetValue = masteryProgress,
                    animationSpec = tween(AppMotion.Normal),
                    label = "masteryAnim"
                )
                LinearProgressIndicator(
                    progress = { masteryProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = masteryColor(card.masteryLevel),
                    trackColor = AppColors.Outline.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun BulkCardPreviewItem(
    card: FlashcardEntity,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AppColors.Primary else AppColors.Outline

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                AppColors.Primary.copy(alpha = 0.06f)
            else
                AppColors.Surface
        ),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulkCardCheckbox(
                isSelected = isSelected,
                onToggle = onToggle
            )

            Spacer(Modifier.width(AppSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.term,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.definition,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                AppStringsVi.DialogRenameStudySet,
                fontWeight = FontWeight.SemiBold,
                style = AppTypography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(AppStringsVi.CardEditTitleHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    ),
                    textStyle = AppTypography.bodyMedium
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(AppStringsVi.CardEditDescHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    ),
                    textStyle = AppTypography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = title.isNotBlank()) {
                Text(
                    AppStringsVi.Save,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (title.isNotBlank()) AppColors.Primary else AppColors.OnSurfaceVariant
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

private val CircleShape = androidx.compose.foundation.shape.RoundedCornerShape(50)
