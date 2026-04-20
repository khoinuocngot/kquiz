package com.example.quizfromfileapp.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.R
import com.example.quizfromfileapp.domain.model.QuizQuestion
import com.example.quizfromfileapp.domain.model.QuizSession
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import com.example.quizfromfileapp.ui.screens.LlmGenerationState
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    sharedViewModel: AppSharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: QuizViewModel = viewModel(
        factory = QuizViewModelFactory(
            application = context.applicationContext as android.app.Application,
            sharedViewModel = sharedViewModel
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val llmState by viewModel.llmGenerationState.collectAsState()
    val llmProgressMessage by viewModel.llmProgressMessage.collectAsState()
    val llmErrorMessage by viewModel.llmErrorMessage.collectAsState()
    val currentIndex by sharedViewModel.currentQuestionIndex.collectAsState()
    val userAnswers by sharedViewModel.userAnswers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.generateQuiz()
    }

    LaunchedEffect(uiState) {
        if (uiState is QuizUiState.Error) {
            snackbarHostState.showSnackbar((uiState as QuizUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quiz_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            when (val state = uiState) {
                is QuizUiState.Loading -> {
                    LoadingView(
                        llmState = llmState,
                        llmProgressMessage = llmProgressMessage,
                        llmErrorMessage = llmErrorMessage
                    )
                }

                is QuizUiState.Ready -> {
                    QuizContentView(
                        session = state.session,
                        currentIndex = currentIndex,
                        userAnswers = userAnswers,
                        llmState = llmState,
                        onSelectAnswer = { qIndex, aIndex ->
                            viewModel.selectAnswer(qIndex, aIndex)
                        },
                        onPrevious = { viewModel.previousQuestion() },
                        onNext = { viewModel.nextQuestion() },
                        onSubmit = onNavigateToResult,
                        onQuit = onNavigateBack
                    )
                }

                is QuizUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.generateQuiz() },
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView(
    llmState: LlmGenerationState,
    llmProgressMessage: String?,
    llmErrorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon thể hiện trạng thái
        val icon = when (llmState) {
            is LlmGenerationState.FallbackUsed -> Icons.Default.CloudOff
            is LlmGenerationState.Idle -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
            else -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }

        if (llmState !is LlmGenerationState.FallbackUsed) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tiêu đề
        val title = when (llmState) {
            is LlmGenerationState.Idle -> stringResource(R.string.quiz_loading)
            is LlmGenerationState.Generating -> stringResource(R.string.quiz_loading_llm)
            is LlmGenerationState.Success -> stringResource(R.string.quiz_loading)
            is LlmGenerationState.Failed -> stringResource(R.string.quiz_loading_llm_fallback)
            is LlmGenerationState.FallbackUsed -> stringResource(R.string.quiz_loading_rule_based)
        }
        Text(
            text = title,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when (llmState) {
                is LlmGenerationState.FallbackUsed -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress message
        val message = llmProgressMessage ?: when (llmState) {
            is LlmGenerationState.Idle -> "Đang chuẩn bị..."
            is LlmGenerationState.Generating -> "Đang sinh quiz..."
            is LlmGenerationState.Success -> "Hoàn thành!"
            is LlmGenerationState.Failed -> "Lỗi: ${llmState.reason}"
            is LlmGenerationState.FallbackUsed -> "Đang dùng rule-based..."
        }
        Text(
            text = message,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Fallback notice
        if (llmState is LlmGenerationState.FallbackUsed) {
            Spacer(modifier = Modifier.height(12.dp))
            FallbackNoticeCard(
                reason = llmErrorMessage ?: "Local AI không khả dụng",
                actualQuestionCount = llmState.questionCount
            )
        }
    }
}

@Composable
private fun FallbackNoticeCard(reason: String, actualQuestionCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Local AI không khả dụng",
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            if (actualQuestionCount >= 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Có sẵn: $actualQuestionCount câu (rule-based)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuizContentView(
    session: QuizSession,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    llmState: LlmGenerationState,
    onSelectAnswer: (Int, Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onQuit: () -> Unit
) {
    val currentQuestion = session.questions.getOrNull(currentIndex)
    val answeredCount = userAnswers.size
    val totalCount = session.questions.size

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            // Generation mode badge
            if (llmState is LlmGenerationState.Success || llmState is LlmGenerationState.FallbackUsed) {
                GenerationModeBadge(llmState)
                Spacer(modifier = Modifier.height(8.dp))
            }

            QuestionProgressCard(
                currentIndex = currentIndex,
                totalCount = totalCount,
                answeredCount = answeredCount
            )

            session.generationWarning?.let { warning ->
                Spacer(modifier = Modifier.height(12.dp))
                WarningCard(warning = warning)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (currentQuestion != null) {
                QuestionCard(
                    question = currentQuestion,
                    questionIndex = currentIndex,
                    selectedAnswer = userAnswers[currentIndex],
                    onSelectAnswer = { aIndex ->
                        onSelectAnswer(currentIndex, aIndex)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NavigationButtons(
            currentIndex = currentIndex,
            totalCount = totalCount,
            answeredCount = answeredCount,
            onPrevious = onPrevious,
            onNext = onNext,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun GenerationModeBadge(llmState: LlmGenerationState) {
    val (text, icon, color) = when (llmState) {
        is LlmGenerationState.Success -> Triple(
            "Sinh bằng Local AI",
            Icons.Default.AutoAwesome,
            MaterialTheme.colorScheme.primary
        )
        is LlmGenerationState.FallbackUsed -> Triple(
            "Rule-based (Local AI unavailable)",
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.tertiary
        )
        else -> return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuestionProgressCard(
    currentIndex: Int,
    totalCount: Int,
    answeredCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.quiz_progress, currentIndex + 1, totalCount),
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.quiz_answered, answeredCount),
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WarningCard(warning: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuizQuestion,
    questionIndex: Int,
    selectedAnswer: Int?,
    onSelectAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = question.question,
                style = AppTypography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        question.options.forEachIndexed { index, option ->
            AnswerOption(
                label = option,
                index = index,
                isSelected = selectedAnswer == index,
                onClick = { onSelectAnswer(index) }
            )
            if (index < question.options.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AnswerOption(
    label: String,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val labels = listOf("A", "B", "C", "D")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels.getOrNull(index) ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = AppTypography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    currentIndex: Int,
    totalCount: Int,
    answeredCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    val isLastQuestion = currentIndex == totalCount - 1
    val allAnswered = answeredCount == totalCount

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentIndex > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.quiz_btn_prev))
            }
        }

        if (isLastQuestion) {
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                enabled = allAnswered
            ) {
                Text(text = stringResource(R.string.quiz_btn_submit))
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.quiz_btn_next))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (!isLastQuestion) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.quiz_btn_submit_now))
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.quiz_error_generic),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.quiz_error_retry))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.quiz_btn_back))
        }
    }
}
