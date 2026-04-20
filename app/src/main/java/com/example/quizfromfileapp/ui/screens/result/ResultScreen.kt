package com.example.quizfromfileapp.ui.screens.result

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.R
import com.example.quizfromfileapp.data.local.entity.QuizHistoryEntity
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.domain.model.QuizQuestion
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    sharedViewModel: AppSharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val session by sharedViewModel.quizSession.collectAsState()
    val userAnswers by sharedViewModel.userAnswers.collectAsState()
    val quizConfig by sharedViewModel.quizConfig.collectAsState()

    var hasSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!hasSaved && session != null) {
            hasSaved = true
            val s = session!!
            val correct = s.questions.count { q ->
                userAnswers[s.questions.indexOf(q)] == q.correctAnswerIndex
            }
            val total = s.questions.size
            val percent = if (total > 0) (correct * 100 / total) else 0
            val entity = QuizHistoryEntity(
                fileName = s.fileName,
                scorePercent = percent,
                correctCount = correct,
                totalQuestions = total,
                difficulty = quizConfig.difficulty,
                questionType = quizConfig.questionType,
                createdAt = System.currentTimeMillis()
            )
            coroutineScope.launch(Dispatchers.IO) {
                AppContainer.saveQuizHistoryUseCase(entity)
            }
        }
    }

    val (correct, total) = if (session != null) {
        var c = 0
        session!!.questions.forEachIndexed { index, question ->
            if (userAnswers[index] == question.correctAnswerIndex) c++
        }
        c to session!!.questions.size
    } else {
        0 to 0
    }

    val scorePercent = if (total > 0) (correct * 100 / total) else 0
    val scoreComment = getScoreComment(scorePercent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ScoreCard(
                    correct = correct,
                    total = total,
                    scorePercent = scorePercent,
                    scoreComment = scoreComment,
                    warning = session?.generationWarning
                )
            }

            item {
                Text(
                    text = stringResource(R.string.result_review_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (session != null) {
                itemsIndexed(session!!.questions) { index, question ->
                    QuestionResultCard(
                        index = index,
                        question = question,
                        userAnswer = userAnswers[index],
                        totalQuestions = session!!.questions.size
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        sharedViewModel.clearUserAnswers()
                        sharedViewModel.setCurrentQuestionIndex(0)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.result_btn_redo))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        sharedViewModel.clearAll()
                        onNavigateToHome()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.result_btn_home))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun getScoreComment(percent: Int): String {
    return when {
        percent >= 81 -> stringResource(R.string.result_comment_81_100)
        percent >= 61 -> stringResource(R.string.result_comment_61_80)
        percent >= 41 -> stringResource(R.string.result_comment_41_60)
        percent >= 21 -> stringResource(R.string.result_comment_21_40)
        else -> stringResource(R.string.result_comment_0_20)
    }
}

@Composable
private fun ScoreCard(
    correct: Int,
    total: Int,
    scorePercent: Int,
    scoreComment: String,
    warning: String? = null
) {
    val scoreColor = when {
        scorePercent >= 80 -> Color(0xFF4CAF50)
        scorePercent >= 50 -> Color(0xFFFFA726)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = scoreColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = scoreComment,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$scorePercent%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.result_score_detail, correct, total),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (warning != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionResultCard(
    index: Int,
    question: QuizQuestion,
    userAnswer: Int?,
    totalQuestions: Int
) {
    val isCorrect = userAnswer == question.correctAnswerIndex
    val labels = listOf("A", "B", "C", "D")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                Color(0xFF1B5E20).copy(alpha = 0.08f)
            } else {
                Color(0xFFB71C1C).copy(alpha = 0.08f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE53935)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.result_question_label, index + 1, totalQuestions),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isCorrect) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (userAnswer != null) {
                val userAnswerText = question.options.getOrNull(userAnswer) ?: "—"
                val correctAnswerText = question.options.getOrNull(question.correctAnswerIndex) ?: "—"

                ResultOptionRow(
                    label = stringResource(R.string.result_your_answer),
                    text = "${labels.getOrNull(userAnswer) ?: ""}. $userAnswerText",
                    isCorrect = isCorrect
                )

                if (!isCorrect) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ResultOptionRow(
                        label = stringResource(R.string.result_correct_answer),
                        text = "${labels.getOrNull(question.correctAnswerIndex) ?: ""}. $correctAnswerText",
                        isCorrect = true
                    )
                }
            } else {
                ResultOptionRow(
                    label = stringResource(R.string.result_correct_answer),
                    text = "${labels.getOrNull(question.correctAnswerIndex) ?: ""}. ${question.options.getOrNull(question.correctAnswerIndex) ?: ""}",
                    isCorrect = true
                )
            }

            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val explanationParts = question.explanation.split("\n", limit = 2)
                val reasonText = explanationParts.getOrNull(0) ?: question.explanation
                val snippetText = explanationParts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.result_explanation),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (snippetText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"$snippetText\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultOptionRow(
    label: String,
    text: String,
    isCorrect: Boolean
) {
    val color = if (isCorrect) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
