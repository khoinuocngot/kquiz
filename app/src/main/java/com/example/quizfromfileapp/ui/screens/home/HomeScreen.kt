package com.example.quizfromfileapp.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.ui.components.DailyGoalConfigDialog
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.ui.components.AudioToggleWidget
import com.example.quizfromfileapp.ui.components.DailyGoalWidget
import com.example.quizfromfileapp.ui.components.PremiumSectionHeader
import com.example.quizfromfileapp.ui.components.StudyTypeChip
import com.example.quizfromfileapp.ui.components.XpLevelWidget
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import com.example.quizfromfileapp.ui.theme.masteryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToImportFile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStudySetList: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOrganization: () -> Unit,
    onNavigateToSmartReview: () -> Unit,
    onNavigateToImportStudySet: () -> Unit,
    onShowQuickImport: () -> Unit,
    onNavigateToStudySet: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }

    // Handle gamification events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.LevelUp -> {
                    snackbarHostState.showSnackbar("Chúc mừng bạn đã lên cấp ${event.level}! 🎉")
                }
                is HomeEvent.DailyGoalComplete -> {
                    snackbarHostState.showSnackbar("Chúc mừng bạn đã hoàn thành mục tiêu hôm nay! 🎉")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStringsVi.AppName,
                            style = AppTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = getGreeting(),
                            style = AppTypography.bodySmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { showAudioDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Âm thanh",
                            tint = AppColors.OnSurfaceVariant
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = AppStringsVi.NavAbout,
                            tint = AppColors.OnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = AppColors.OnSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGapLarge)
        ) {
            item { Spacer(Modifier.height(AppSpacing.lg)) }

            // Hero Section
            item {
                HomeHeroSection(
                    onShowQuickImport = onShowQuickImport,
                    totalSets = uiState.totalSets,
                    totalCards = uiState.totalCards
                )
            }

            // Gamification: XP + Level
            if (uiState.totalXp > 0 || uiState.currentLevel > 1) {
                item {
                    XpLevelWidget(
                        totalXp = uiState.totalXp,
                        currentLevel = uiState.currentLevel,
                        xpProgressInLevel = uiState.xpProgressInLevel,
                        modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)
                    )
                }
            }

            // Gamification: Daily Goal
            item {
                DailyGoalWidget(
                    current = uiState.dailyGoalCurrent,
                    target = uiState.dailyGoalTarget,
                    onConfigureClick = { showDailyGoalDialog = true },
                    modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)
                )
            }

            // Streak & Progress Widget
            if (uiState.currentStreak > 0 || uiState.todayCards > 0 || uiState.needsReviewCount > 0) {
                item {
                    HomeStreakWidget(
                        currentStreak = uiState.currentStreak,
                        maxStreak = uiState.maxStreak,
                        todayCards = uiState.todayCards,
                        needsReviewCount = uiState.needsReviewCount,
                        onSmartReviewClick = onNavigateToSmartReview,
                        onOrganizeClick = onNavigateToOrganization
                    )
                }
            }

            // Quick Actions
            item {
                HomeQuickActionsSection(
                    onNavigateToStudySetList = onNavigateToStudySetList,
                    onNavigateToImportFile = onNavigateToImportFile,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToImportStudySet = onNavigateToImportStudySet,
                    onShowQuickImport = onShowQuickImport,
                    onImportFileClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Phương thức này bộ tạo câu hỏi sẽ không được thông minh như AI bạn nhé!"
                            )
                        }
                    }
                )
            }

            // Recent Sets
            if (uiState.recentSets.isNotEmpty()) {
                item {
                    HomeRecentSection(
                        recentSets = uiState.recentSets,
                        onNavigateToStudySetList = onNavigateToStudySetList,
                        onNavigateToStudySet = onNavigateToStudySet
                    )
                }
            } else if (!uiState.isLoading) {
                item {
                    HomeEmptyRecentSection(onShowQuickImport = onShowQuickImport)
                }
            }

            item { Spacer(Modifier.height(AppSpacing.huge)) }
        }
    }

    // Audio Settings Dialog
    if (showAudioDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAudioDialog = false }) {
                    Text("Xong", color = AppColors.Primary)
                }
            },
            title = {
                Text("Cài đặt âm thanh", style = AppTypography.titleMedium, fontWeight = FontWeight.SemiBold)
            },
            text = {
                AudioToggleWidget(audioManager = AppContainer.audioManager)
            },
            containerColor = AppColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.dialog)
        )
    }

    // Daily Goal Config Dialog
    if (showDailyGoalDialog) {
        DailyGoalConfigDialog(
            currentGoal = uiState.dailyGoalTarget,
            onGoalSelected = { goal ->
                viewModel.setDailyGoal(goal)
                showDailyGoalDialog = false
            },
            onDismiss = { showDailyGoalDialog = false }
        )
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Sáng tốt lành!"
        hour < 17 -> "Chiều vui vẻ!"
        hour < 21 -> "Tối an lành!"
        else -> "Đêm muộn rồi nè!"
    }
}

@Composable
private fun HomeHeroSection(
    onShowQuickImport: () -> Unit,
    totalSets: Int,
    totalCards: Int
) {
    Box(
        modifier = Modifier
            .padding(horizontal = AppSpacing.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.card))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xxl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Học nhanh.",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Nhớ lâu.",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        text = AppStringsVi.HomeGreetingSub,
                        style = AppTypography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Stats mini
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (totalSets > 0) {
                        Text(
                            text = "$totalSets",
                            style = AppTypography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "bộ học",
                            style = AppTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$totalCards",
                            style = AppTypography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "thẻ",
                            style = AppTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    } else {
                        Text(
                            text = "Sẵn sàng",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "tạo bộ học đầu tiên",
                            style = AppTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.button))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onShowQuickImport)
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    text = AppStringsVi.HomeCreateNew,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionsSection(
    onNavigateToStudySetList: () -> Unit,
    onNavigateToImportFile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToImportStudySet: () -> Unit,
    onShowQuickImport: () -> Unit,
    onImportFileClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
        PremiumSectionHeader(title = AppStringsVi.HomeStartLearning)

        Spacer(Modifier.height(AppSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            HomeActionCard(
                title = AppStringsVi.QuickImportTitle,
                subtitle = "Từ văn bản",
                icon = Icons.AutoMirrored.Filled.TextSnippet,
                accentColor = AppColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onShowQuickImport
            )
            HomeActionCard(
                title = AppStringsVi.ImportFileTitle,
                subtitle = "PDF, TXT, Ảnh",
                icon = Icons.Default.Description,
                accentColor = AppColors.Secondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    onImportFileClick()
                    onNavigateToImportFile()
                }
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            HomeActionCard(
                title = AppStringsVi.NavStudySets,
                subtitle = AppStringsVi.HomeMyStudySets,
                icon = Icons.Default.School,
                accentColor = AppColors.Success,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToStudySetList
            )
            HomeActionCard(
                title = "Nhập bộ học",
                subtitle = "Từ file .studyset",
                icon = Icons.Default.Layers,
                accentColor = AppColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToImportStudySet
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))

        HomeActionCard(
            title = AppStringsVi.NavHistory,
            subtitle = AppStringsVi.HistoryTitle,
            icon = Icons.Default.History,
            accentColor = AppColors.Warning,
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToHistory
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.height(AppSpacing.md))

            Text(
                text = title,
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = AppTypography.bodySmall,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeRecentSection(
    recentSets: List<StudySetEntity>,
    onNavigateToStudySetList: () -> Unit,
    onNavigateToStudySet: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bộ học gần đây",
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.chip))
                    .background(AppColors.Primary.copy(alpha = 0.08f))
                    .clickable { onNavigateToStudySetList() }
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Xem tất cả",
                    style = AppTypography.labelMedium,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(AppSpacing.xxs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.Primary
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            contentPadding = PaddingValues(end = AppSpacing.screenPadding)
        ) {
            items(items = recentSets, key = { it.id }) { set ->
                RecentSetCard(
                    set = set,
                    onClick = { onNavigateToStudySet(set.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentSetCard(
    set: StudySetEntity,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.forLanguageTag("vi-VN")) }
    val formattedDate = remember(set.updatedAt) { dateFormatter.format(Date(set.updatedAt)) }
    val sourceColor = when (set.sourceType) {
        StudySetEntity.SOURCE_TYPE_QUICK_IMPORT -> AppColors.Success
        StudySetEntity.SOURCE_TYPE_FILE_IMPORT -> AppColors.Info
        else -> AppColors.OnSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column {
            // Accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(sourceColor)
            )
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                // Pin/Favorite indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (set.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (set.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = AppColors.Warning,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = set.title,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (set.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = set.description,
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
                        text = "${set.cardCount} thẻ",
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = AppTypography.labelSmall,
                        color = AppColors.OnSurfaceMuted
                    )
                }

                // Mastery proxy bar (visual only - real mastery comes in Phase 6)
                if (set.cardCount > 0) {
                    Spacer(Modifier.height(AppSpacing.sm))
                    LinearProgressIndicator(
                        progress = { 0.1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AppColors.Primary.copy(alpha = 0.5f),
                        trackColor = AppColors.Primary.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// Streak & Progress Widget
@Composable
private fun HomeStreakWidget(
    currentStreak: Int,
    maxStreak: Int,
    todayCards: Int,
    needsReviewCount: Int,
    onSmartReviewClick: () -> Unit,
    onOrganizeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding)
    ) {
        // Streak Banner
        if (currentStreak > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.card),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Primary.copy(alpha = 0.08f)
                ),
                onClick = onOrganizeClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$currentStreak ngày liên tiếp",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "Best: $maxStreak ngày • Hôm nay: $todayCards thẻ",
                            style = AppTypography.bodySmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppColors.OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.md))
        }

        // Smart Review Banner
        if (needsReviewCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.card),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Warning.copy(alpha = 0.08f)
                ),
                onClick = onSmartReviewClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(AppSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$needsReviewCount thẻ cần ôn",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "Smart Review giúp bạn ôn lại thẻ yếu",
                            style = AppTypography.bodySmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppColors.OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.md))
        }

        // Organize / Folders quick access
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.card),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.Primary.copy(alpha = 0.04f)
            ),
            onClick = onOrganizeClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tổ chức bộ học",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "Sắp xếp theo thư mục, gắn tag",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.OnSurfaceVariant
                )
            }
        }
    }
}

// Empty state for Recent Sets section
@Composable
private fun HomeEmptyRecentSection(onShowQuickImport: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bộ học gần đây",
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
        }

        Spacer(Modifier.height(AppSpacing.md))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.card),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            border = BorderStroke(1.dp, AppColors.Outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .background(AppColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = AppColors.Primary.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(AppSpacing.md))
                Text(
                    text = AppStringsVi.HomeEmptyMySets,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    text = AppStringsVi.HomeEmptyMySetsSub,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
