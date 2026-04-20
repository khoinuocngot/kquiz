package com.example.quizfromfileapp.ui.screens.quizconfig

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import com.example.quizfromfileapp.domain.model.QuizGenerationMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.R
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizConfigScreen(
    sharedViewModel: AppSharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    val selectedFile = sharedViewModel.selectedFile.value
    val extractedContent = sharedViewModel.extractedContent.value

    var questionCount by remember {
        mutableIntStateOf(sharedViewModel.quizConfig.value.questionCount)
    }
    var selectedDifficulty by remember {
        mutableStateOf(sharedViewModel.quizConfig.value.difficulty)
    }
    var selectedQuestionType by remember {
        mutableStateOf(sharedViewModel.quizConfig.value.questionType)
    }

    val hasError = extractedContent == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quiz_config_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FileInfoCard(
                fileName = selectedFile?.name ?: stringResource(R.string.quiz_config_no_file),
                mimeType = selectedFile?.mimeType ?: "",
                charCount = extractedContent?.rawCharCount
            )

            if (hasError) {
                Spacer(modifier = Modifier.height(24.dp))
                NoContentCard()
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(32.dp))

                QuestionCountCard(
                    count = questionCount,
                    onCountChange = { questionCount = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                DifficultyCard(
                    selected = selectedDifficulty,
                    onSelect = { selectedDifficulty = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                QuestionTypeCard(
                    selected = selectedQuestionType,
                    onSelect = { selectedQuestionType = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                GenerationModeCard(
                    selected = sharedViewModel.generationMode.value,
                    onSelect = { sharedViewModel.setGenerationMode(it) }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val config = QuizConfig(
                        questionCount = questionCount,
                        difficulty = selectedDifficulty,
                        questionType = selectedQuestionType
                    )
                    sharedViewModel.setQuizConfig(config)
                    onNavigateToQuiz()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasError
            ) {
                Text(stringResource(R.string.quiz_config_btn_create))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.quiz_config_btn_back))
            }
        }
    }
}

@Composable
private fun FileInfoCard(
    fileName: String,
    mimeType: String,
    charCount: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (charCount != null) {
                        "$mimeType • $charCount ký tự"
                    } else {
                        mimeType
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            if (charCount != null) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NoContentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.quiz_config_no_content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun QuestionCountCard(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.quiz_config_question_count_label),
                style = MaterialTheme.typography.titleMedium
            )
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = count.toFloat(),
            onValueChange = { onCountChange(it.roundToInt()) },
            valueRange = QuizConfig.MIN_QUESTIONS.toFloat()..QuizConfig.MAX_QUESTIONS.toFloat(),
            steps = QuizConfig.MAX_QUESTIONS - QuizConfig.MIN_QUESTIONS - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${QuizConfig.MIN_QUESTIONS}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${QuizConfig.MAX_QUESTIONS}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DifficultyCard(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.quiz_config_difficulty_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        QuizConfig.DIFFICULTY_OPTIONS.forEachIndexed { index, option ->
            DifficultyOption(
                option = option,
                description = when (option) {
                    "Dễ" -> stringResource(R.string.quiz_config_difficulty_easy_desc)
                    "Trung bình" -> stringResource(R.string.quiz_config_difficulty_medium_desc)
                    else -> stringResource(R.string.quiz_config_difficulty_hard_desc)
                },
                isSelected = selected == option,
                onClick = { onSelect(option) }
            )
            if (index < QuizConfig.DIFFICULTY_OPTIONS.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DifficultyOption(
    option: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OptionCard(
        label = option,
        description = description,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
private fun QuestionTypeCard(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.quiz_config_type_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MVP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hiện chỉ hỗ trợ tốt cho \"Trắc nghiệm 4 đáp án\". Các loại khác sẽ tự động chuyển sang trắc nghiệm 4 đáp án.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        QuizConfig.QUESTION_TYPE_OPTIONS.forEachIndexed { index, option ->
            OptionCard(
                label = option,
                description = when (option) {
                    "Trắc nghiệm 4 đáp án" -> stringResource(R.string.quiz_config_type_mc4_desc)
                    "Đúng / Sai" -> stringResource(R.string.quiz_config_type_tf_desc)
                    else -> stringResource(R.string.quiz_config_type_fill_desc)
                },
                isSelected = selected == option,
                onClick = { onSelect(option) }
            )
            if (index < QuizConfig.QUESTION_TYPE_OPTIONS.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GenerationModeCard(
    selected: QuizGenerationMode,
    onSelect: (QuizGenerationMode) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.quiz_config_generation_mode_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\"LLM-assisted\" dùng AI trên thiết bị để sinh câu hỏi tự nhiên hơn. " +
                                "Nếu AI không khả dụng, sẽ tự động dùng rule-based. " +
                                "Lưu ý: Local AI có thể chậm hơn rule-based và cần tải model lần đầu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        QuizGenerationMode.entries.forEachIndexed { index, mode ->
            OptionCard(
                label = mode.displayName,
                description = mode.description,
                isSelected = selected == mode,
                onClick = { onSelect(mode) }
            )
            if (index < QuizGenerationMode.entries.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OptionCard(
    label: String,
    description: String,
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
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
