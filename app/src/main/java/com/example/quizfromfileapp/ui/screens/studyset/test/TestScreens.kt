package com.example.quizfromfileapp.ui.screens.studyset.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.ui.components.PremiumTopBar
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.components.PremiumButton
import com.example.quizfromfileapp.ui.components.PremiumSecondaryButton
import com.example.quizfromfileapp.ui.components.PremiumGhostButton
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumEmptyState
import com.example.quizfromfileapp.ui.components.PremiumCard
import com.example.quizfromfileapp.ui.components.PremiumOptionCard
import com.example.quizfromfileapp.ui.components.PremiumQuestionPalette
import com.example.quizfromfileapp.ui.components.PremiumResultHeroCard
import com.example.quizfromfileapp.ui.components.PremiumProgressHeader
import com.example.quizfromfileapp.ui.components.BannerType
import com.example.quizfromfileapp.ui.components.PremiumInfoBanner
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import com.example.quizfromfileapp.ui.theme.AppMotion

// ══════════════════════════════════════════════════════
// TEST CONFIG SCREEN
// ══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConfigScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    onStartTest: () -> Unit,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studySetId) {
        viewModel.loadTestConfig(studySetId)
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.TestTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (uiState.cards.size < 2) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                PremiumEmptyState(
                    icon = Icons.Default.Close,
                    title = AppStringsVi.TestNeedMinCards,
                    subtitle = "${AppStringsVi.HomeTotalCards} ${uiState.cards.size}",
                    action = {
                        PremiumSecondaryButton(
                            text = AppStringsVi.ActionBack,
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                )
            }
        } else {
            TestConfigContent(
                uiState = uiState,
                viewModel = viewModel,
                onStartTest = {
                    viewModel.startTest()
                    onStartTest()
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestConfigContent(
    uiState: TestUiState,
    viewModel: TestViewModel,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxQuestions = minOf(uiState.cards.size, 20)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.screenPadding)
    ) {
        Text(
            AppStringsVi.TestConfig,
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${uiState.cards.size} thẻ có sẵn",
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurfaceVariant
        )

        // ─── Nguồn câu hỏi ───
        Spacer(Modifier.height(AppSpacing.sectionGap))
        Text(
            AppStringsVi.TestConfigSource,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(AppSpacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TestSource.entries) { source ->
                FilterChip(
                    selected = uiState.config.source == source,
                    onClick = { viewModel.updateSource(source) },
                    label = { Text(source.displayName, style = AppTypography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = AppColors.Primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = AppColors.Primary,
                        enabled = true,
                        selected = uiState.config.source == source
                    )
                )
            }
        }

        // ─── Số câu hỏi ───
        Spacer(Modifier.height(AppSpacing.sectionGap))
        Text(
            AppStringsVi.TestConfigQuestionCount,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            listOf(5, 10, 15, 20).filter { it <= maxQuestions }.forEach { count ->
                QuestionCountChip(
                    count = count,
                    isSelected = uiState.config.questionCount == count,
                    onClick = { viewModel.updateQuestionCount(count) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (maxQuestions < 20) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Tối đa $maxQuestions câu",
                style = AppTypography.bodySmall,
                color = AppColors.OnSurfaceVariant
            )
        }

        // ─── Loại câu hỏi ───
        Spacer(Modifier.height(AppSpacing.sectionGap))
        Text(
            AppStringsVi.TestConfigQuestionType,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(AppSpacing.sm))

        val isQA = uiState.studySetType == StudySetEntity.STUDY_SET_TYPE_QUESTION_ANSWER
        val availableTypes = TestQuestionType.entries.filter { it.compatibleWithQA || !isQA }

        if (isQA) {
            PremiumInfoBanner(
                icon = Icons.Default.Info,
                text = "Chế độ ${AppStringsVi.StudySetTypeQA}: luôn hỏi theo ${AppStringsVi.LearnDirectionQA}",
                bannerType = BannerType.Info
            )
            Spacer(Modifier.height(AppSpacing.sm))
        }

        availableTypes.forEach { type ->
            TestTypeOption(
                type = type,
                isSelected = uiState.config.questionType == type,
                onClick = { viewModel.updateQuestionType(type) }
            )
            Spacer(Modifier.height(AppSpacing.sm))
        }

        // ─── Tùy chọn bổ sung ───
        Spacer(Modifier.height(AppSpacing.sectionGap))
        Text(
            AppStringsVi.TestConfigOptions,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(AppSpacing.sm))

        // Randomize
        OptionToggleRow(
            icon = Icons.Default.Shuffle,
            label = AppStringsVi.TestConfigRandomQuestion,
            description = AppStringsVi.TestConfigRandomDesc,
            checked = uiState.config.randomize,
            onCheckedChange = viewModel::updateRandomize
        )

        Spacer(Modifier.height(AppSpacing.sm))

        // Timer
        OptionToggleRow(
            icon = Icons.Default.Timer,
            label = AppStringsVi.TestTimerOn,
            description = AppStringsVi.TestConfigTimerDesc,
            checked = uiState.config.timerEnabled,
            onCheckedChange = viewModel::updateTimerEnabled
        )

        if (uiState.config.timerEnabled) {
            Spacer(Modifier.height(AppSpacing.sm))
            TimerConfigRow(
                seconds = uiState.config.timerSeconds,
                onSecondsChange = viewModel::updateTimerSeconds
            )
        }

        Spacer(Modifier.height(AppSpacing.sectionGap))

        // Summary card
        PremiumCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${uiState.config.questionCount} câu hỏi",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        "${uiState.config.source.displayName} • ${uiState.config.questionType.displayName}",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                    if (uiState.config.timerEnabled) {
                        Text(
                            "⏱ ${formatSeconds(uiState.config.timerSeconds)}",
                            style = AppTypography.bodySmall,
                            color = AppColors.Warning
                        )
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AppColors.Primary)
            }
        }

        Spacer(Modifier.height(AppSpacing.sectionGapLarge))

        PremiumButton(
            text = AppStringsVi.TestStartTest,
            onClick = onStartTest
        )
    }
}

@Composable
private fun OptionToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = AppTypography.bodyMedium, fontWeight = FontWeight.Medium, color = AppColors.OnSurface)
                Text(description, style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.Primary,
                    checkedTrackColor = AppColors.Primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun TimerConfigRow(seconds: Int, onSecondsChange: (Int) -> Unit) {
    val options = listOf(60 to "1 phút", 120 to "2 phút", 300 to "5 phút", 600 to "10 phút", 900 to "15 phút")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (sec, label) ->
            val isSelected = seconds == sec
            PremiumCard(
                modifier = Modifier.clickable { onSecondsChange(sec) }
            ) {
                Text(
                    text = label,
                    style = AppTypography.labelMedium,
                    color = if (isSelected) AppColors.Primary else AppColors.OnSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuestionCountChip(count: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.Primary else AppColors.Surface,
        animationSpec = tween(AppMotion.Fast),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.Primary else AppColors.OutlineVariant,
        animationSpec = tween(AppMotion.Fast),
        label = "chipBorder"
    )
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "$count",
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else AppColors.OnSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestTypeOption(type: TestQuestionType, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface,
        animationSpec = tween(AppMotion.Fast),
        label = "typeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.Primary else AppColors.OutlineVariant,
        animationSpec = tween(AppMotion.Fast),
        label = "typeBorder"
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(AppSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AppColors.Primary else AppColors.OutlineVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }
            Spacer(Modifier.width(AppSpacing.md))
            Text(
                text = type.displayName,
                style = AppTypography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AppColors.Primary else AppColors.OnSurface
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// TEST SCREEN
// ══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = AppStringsVi.TestTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actionsContent = {
                    val currentCardId = uiState.questions.getOrNull(uiState.currentIndex)?.cardId
                    val isFlagged = currentCardId != null && currentCardId in uiState.flaggedQuestions
                    PremiumIconButton(
                        icon = Icons.Default.Flag,
                        onClick = viewModel::toggleFlag,
                        contentDescription = AppStringsVi.TestFlagQuestion,
                        tint = if (isFlagged) AppColors.Warning else AppColors.OnPrimary,
                        buttonSize = 40
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            TestQuestionContent(
                uiState = uiState,
                viewModel = viewModel,
                onSubmit = onSubmit,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Submit confirmation dialog
    if (uiState.showSubmitConfirmation) {
        SubmitConfirmationDialog(
            unansweredCount = uiState.unansweredCount,
            onConfirm = {
                viewModel.dismissSubmitConfirmation()
                viewModel.submitTest()
                onSubmit()
            },
            onDismiss = viewModel::dismissSubmitConfirmation
        )
    }
}

@Composable
private fun TestQuestionContent(
    uiState: TestUiState,
    viewModel: TestViewModel,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.questions[uiState.currentIndex]
    val totalQuestions = uiState.questions.size
    val allAnswered = uiState.allAnswered
    val selectedAnswer = uiState.answers[uiState.currentIndex]
    val isFlagged = question.cardId in uiState.flaggedQuestions

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppSpacing.screenPadding)
    ) {
        // Progress header
        val progress = (uiState.currentIndex + 1).toFloat() / totalQuestions
        val timerLabel = if (uiState.config.timerEnabled && uiState.remainingSeconds > 0) {
            val color = when {
                uiState.remainingSeconds <= 60 -> AppColors.Error
                uiState.remainingSeconds <= 120 -> AppColors.Warning
                else -> AppColors.Primary
            }
            formatSeconds(uiState.remainingSeconds)
        } else null

        PremiumProgressHeader(
            currentIndex = uiState.currentIndex,
            totalCount = totalQuestions,
            correctCount = uiState.answeredCount,
            wrongCount = 0,
            progress = progress,
            timerLabel = timerLabel,
            flaggedCount = uiState.flaggedCount
        )

        Spacer(Modifier.height(AppSpacing.sm))

        // Question palette grid
        PremiumQuestionPalette(
            totalQuestions = totalQuestions,
            currentIndex = uiState.currentIndex,
            answeredIndices = uiState.answers.keys,
            flaggedIndices = uiState.questions.mapIndexedNotNull { index, q -> if (q.cardId in uiState.flaggedQuestions) index else null }.toSet(),
            onQuestionClick = viewModel::goToQuestion
        )

        Spacer(Modifier.height(AppSpacing.lg))

        // Question type badge
        QuestionTypeBadge(question = question, isFlagged = isFlagged)

        Spacer(Modifier.height(AppSpacing.md))

        // Question card
        QuestionCard(question = question)

        Spacer(Modifier.height(AppSpacing.md))

        // Options
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            question.options.forEachIndexed { index, option ->
                PremiumOptionCard(
                    letter = ('A' + index).toString(),
                    text = option,
                    isSelected = selectedAnswer == index,
                    isCorrect = null,
                    isAnswered = false,
                    onClick = { viewModel.selectOption(index) }
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        // Bottom action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            PremiumSecondaryButton(
                text = AppStringsVi.Prev,
                onClick = viewModel::prevQuestion,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                enabled = uiState.currentIndex > 0,
                modifier = Modifier.weight(1f)
            )
            if (uiState.currentIndex == totalQuestions - 1) {
                PremiumButton(
                    text = AppStringsVi.TestSubmit,
                    onClick = viewModel::requestSubmit,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PremiumButton(
                    text = AppStringsVi.Next,
                    onClick = viewModel::nextQuestion,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.md))
    }
}

@Composable
private fun QuestionTypeBadge(question: TestQuestion, isFlagged: Boolean) {
    val bgColor = when {
        question.isQuestionAnswer -> AppColors.Primary.copy(alpha = 0.1f)
        question.isTermQuestion -> AppColors.Primary.copy(alpha = 0.08f)
        else -> AppColors.Tertiary.copy(alpha = 0.1f)
    }
    val textColor = when {
        question.isQuestionAnswer -> AppColors.Primary
        question.isTermQuestion -> AppColors.Primary
        else -> AppColors.Tertiary
    }
    val label = when {
        question.isQuestionAnswer -> AppStringsVi.StudySetTypeQA
        question.isTermQuestion -> AppStringsVi.TestDirectionTermDef
        else -> AppStringsVi.TestDirectionMixed
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.chip))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
        if (isFlagged) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Flag, contentDescription = AppStringsVi.TestFlagged, tint = AppColors.Warning, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun QuestionCard(question: TestQuestion) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = question.questionText,
            style = AppTypography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.xl)
        )
    }
}

@Composable
private fun SubmitConfirmationDialog(
    unansweredCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(AppStringsVi.TestConfirmSubmit, style = AppTypography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(AppStringsVi.TestConfirmSub, style = AppTypography.bodyMedium)
        },
        confirmButton = {
            PremiumButton(text = AppStringsVi.TestConfirmYes, onClick = onConfirm)
        },
        dismissButton = {
            PremiumGhostButton(text = AppStringsVi.TestConfirmNo, onClick = onDismiss)
        }
    )
}

// ══════════════════════════════════════════════════════
// TEST RESULT SCREEN
// ══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    studySetId: Long,
    onRetake: () -> Unit,
    onBackToStudySet: () -> Unit,
    onReviewWrongAnswers: () -> Unit,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = AppStringsVi.TestResult,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBackToStudySet
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Hero score card
            PremiumResultHeroCard(
                scorePercent = uiState.scorePercent,
                correctCount = uiState.correctCount,
                totalCount = uiState.questions.size,
                wrongCount = uiState.wrongResults.size,
                flaggedCount = uiState.flaggedResults.size,
                modifier = Modifier.padding(AppSpacing.screenPadding)
            )

            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppColors.Surface,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            AppStringsVi.TestDetailTab,
                            style = AppTypography.labelMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) AppColors.Primary else AppColors.OnSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            AppStringsVi.TestAnswerTab,
                            style = AppTypography.labelMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 1) AppColors.Primary else AppColors.OnSurfaceVariant
                        )
                    }
                )
            }

            // Tab content
            when (selectedTab) {
                0 -> ResultDetailTab(uiState = uiState)
                1 -> ResultAnswersTab(uiState = uiState)
            }

            // Bottom buttons
            ResultBottomActions(
                uiState = uiState,
                onRetake = {
                    viewModel.retakeTest()
                    onRetake()
                },
                onReviewWrongAnswers = onReviewWrongAnswers,
                onBackToStudySet = onBackToStudySet
            )
        }
    }
}

@Composable
private fun ResultDetailTab(uiState: TestUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        itemsIndexed(uiState.results) { index, result ->
            ResultQuestionItem(
                index = index + 1,
                question = result.question.questionText,
                isCorrect = result.isCorrect,
                selectedAnswer = result.selectedIndex?.let { result.question.options.getOrNull(it) },
                correctAnswer = result.question.options.getOrNull(result.question.correctIndex) ?: "",
                explanation = result.question.explanation,
                sourcePage = result.question.sourcePage,
                sourceSnippet = result.question.sourceSnippet,
                isFlagged = result.question.cardId in uiState.wrongQuestionIds && result.question.cardId in uiState.flaggedQuestions
            )
        }
    }
}

@Composable
private fun ResultQuestionItem(
    index: Int,
    question: String,
    isCorrect: Boolean,
    selectedAnswer: String?,
    correctAnswer: String,
    explanation: String?,
    sourcePage: Int?,
    sourceSnippet: String?,
    isFlagged: Boolean
) {
    val accentColor = if (isCorrect) AppColors.Success else AppColors.Error
    val bgColor = accentColor.copy(alpha = 0.05f)

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            style = AppTypography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text("${AppStringsVi.TestProgress} $index", style = AppTypography.labelMedium, fontWeight = FontWeight.Medium, color = AppColors.OnSurface)
                    if (isFlagged) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Flag, contentDescription = AppStringsVi.TestFlagged, tint = AppColors.Warning, modifier = Modifier.size(14.dp))
                    }
                }
                Icon(
                    if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                question,
                style = AppTypography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!isCorrect && selectedAnswer != null) {
                Spacer(Modifier.height(4.dp))
                Text(AppStringsVi.TestYourAnswer.format(selectedAnswer), style = AppTypography.bodySmall, color = AppColors.Error)
            }
            Spacer(Modifier.height(2.dp))
            Text("${AppStringsVi.TestCorrect}: $correctAnswer", style = AppTypography.bodySmall, color = AppColors.Success)
            if (!explanation.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    explanation,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (sourcePage != null || !sourceSnippet.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                val sourceText = buildString {
                    sourcePage?.let { append(AppStringsVi.FlashcardSourcePage + " $it") }
                    if (!sourceSnippet.isNullOrBlank()) {
                        if (isNotEmpty()) append(" $AppStringsVi.Of ")
                        append(sourceSnippet.take(50) + if (sourceSnippet.length > 50) "…" else "")
                    }
                }
                Text(
                    "${AppStringsVi.FlashcardSource}: $sourceText",
                    style = AppTypography.labelSmall,
                    color = AppColors.OnSurfaceMuted
                )
            }
        }
    }
}

@Composable
private fun ResultAnswersTab(uiState: TestUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        itemsIndexed(uiState.results) { index, result ->
            AnswerReviewCard(index = index + 1, result = result)
        }
    }
}

@Composable
private fun AnswerReviewCard(index: Int, result: QuestionResult) {
    val accentColor = if (result.isCorrect) AppColors.Success else AppColors.Error
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${AppStringsVi.TestProgress} $index", style = AppTypography.labelMedium, fontWeight = FontWeight.Medium, color = AppColors.OnSurface)
                Icon(
                    if (result.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(result.question.questionText, style = AppTypography.bodyMedium, fontWeight = FontWeight.Medium, color = AppColors.OnSurface)
            Spacer(Modifier.height(AppSpacing.sm))

            result.question.options.forEachIndexed { optIndex, option ->
                val isCorrectOpt = optIndex == result.question.correctIndex
                val isSelected = result.selectedIndex == optIndex
                val optionBgColor = when {
                    isCorrectOpt -> AppColors.Success.copy(alpha = 0.1f)
                    isSelected && !isCorrectOpt -> AppColors.Error.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(optionBgColor)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${('A' + optIndex)}.", style = AppTypography.bodySmall, fontWeight = FontWeight.Medium, color = AppColors.OnSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = AppTypography.bodySmall,
                        color = when {
                            isCorrectOpt -> AppColors.Success
                            isSelected && !isCorrectOpt -> AppColors.Error
                            else -> AppColors.OnSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrectOpt) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(16.dp))
                    } else if (isSelected) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (!result.question.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(AppRadius.chip),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = result.question.explanation ?: "",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant,
                        modifier = Modifier.padding(AppSpacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultBottomActions(
    uiState: TestUiState,
    onRetake: () -> Unit,
    onReviewWrongAnswers: () -> Unit,
    onBackToStudySet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(AppSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (uiState.wrongResults.isNotEmpty()) {
            PremiumSecondaryButton(
                text = "${AppStringsVi.TestReviewWrong} (${uiState.wrongResults.size})",
                onClick = onReviewWrongAnswers,
                icon = Icons.Default.Refresh
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            PremiumSecondaryButton(
                text = AppStringsVi.ActionRetry,
                onClick = onRetake,
                icon = Icons.Default.Refresh,
                modifier = Modifier.weight(1f)
            )
            PremiumButton(
                text = AppStringsVi.LearnBackToStudySet,
                onClick = onBackToStudySet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// REVIEW WRONG ANSWERS SCREEN
// ══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewWrongAnswersScreen(
    studySetId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val wrongResults = uiState.wrongResults
    if (wrongResults.isEmpty()) {
        Scaffold(
            topBar = {
                PremiumTopBarSurface(
                    title = AppStringsVi.TestReviewWrong,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                PremiumEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = AppStringsVi.TestLow,
                    subtitle = AppStringsVi.TestNoWrongAnswers
                )
            }
        }
        return
    }

    val currentResult = wrongResults.getOrNull(currentIndex)
    if (currentResult == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

        Scaffold(
            topBar = {
                PremiumTopBarSurface(
                    title = "${AppStringsVi.TestReviewWrong} (${currentIndex + 1}/${wrongResults.size})",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack
                )
            },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AppSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / wrongResults.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AppColors.Warning,
                trackColor = AppColors.Warning.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(AppSpacing.lg))

            // Question card
            PremiumCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentResult.question.questionText,
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(AppSpacing.xl)
                )
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Answer reveal
            val rotation by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = tween(AppMotion.Normal),
                label = "flipRotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 16f * density
                    }
                    .clip(RoundedCornerShape(AppRadius.card))
                    .clickable { isFlipped = !isFlipped },
                colors = CardDefaults.cardColors(
                    containerColor = if (rotation > 90f) AppColors.Success.copy(alpha = 0.06f)
                    else AppColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (rotation > 90f) AppColors.Success.copy(alpha = 0.4f)
                    else AppColors.OutlineVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                AppStringsVi.FlashcardBack,
                                style = AppTypography.labelMedium,
                                color = AppColors.OnSurfaceVariant
                            )
                            Spacer(Modifier.height(AppSpacing.md))
                            Text(
                                AppStringsVi.TestTapToReveal,
                                style = AppTypography.bodyMedium,
                                color = AppColors.OnSurfaceVariant
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                AppStringsVi.FlashcardBack.uppercase(),
                                style = AppTypography.labelMedium,
                                color = AppColors.Success,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(AppSpacing.md))
                            Text(
                                text = currentResult.question.options.getOrNull(currentResult.question.correctIndex) ?: "",
                                style = AppTypography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (!currentResult.question.explanation.isNullOrBlank()) {
                                Spacer(Modifier.height(AppSpacing.md))
                                Text(
                                    currentResult.question.explanation ?: "",
                                    style = AppTypography.bodyMedium,
                                    color = AppColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(AppSpacing.md))
                            Text(
                                AppStringsVi.TestYouSelected.format(
                                    currentResult.selectedIndex?.let { currentResult.question.options.getOrNull(it) }
                                        ?: AppStringsVi.TestYouSkipped
                                ),
                                style = AppTypography.bodySmall,
                                color = AppColors.Error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Navigation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                PremiumSecondaryButton(
                    text = AppStringsVi.Prev,
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--
                            isFlipped = false
                        }
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                )
                if (currentIndex < wrongResults.size - 1) {
                    PremiumButton(
                        text = AppStringsVi.Next,
                        onClick = {
                            currentIndex++
                            isFlipped = false
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PremiumButton(
                        text = AppStringsVi.Done,
                        onClick = onNavigateBack,
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// UTILITY
// ══════════════════════════════════════════════════════
private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
