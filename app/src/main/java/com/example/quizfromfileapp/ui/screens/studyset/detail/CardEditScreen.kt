package com.example.quizfromfileapp.ui.screens.studyset.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.data.local.entity.FlashcardEntity
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditScreen(
    studySetId: Long,
    cardId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { CardEditViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(studySetId, cardId) {
        viewModel.loadCard(studySetId, cardId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            scope.launch {
                snackbarHostState.showSnackbar(AppStringsVi.CardEditSaved)
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.CardEditTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actionsContent = {
                    if (!uiState.isLoading) {
                        IconButton(
                            onClick = { viewModel.saveCard() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = AppStringsVi.Save,
                                tint = if (uiState.term.isNotBlank()) AppColors.Primary else AppColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (uiState.card == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStringsVi.DetailNotFound,
                    style = AppTypography.bodyLarge,
                    color = AppColors.OnSurfaceVariant
                )
            }
        } else {
            CardEditForm(
                term = uiState.term,
                definition = uiState.definition,
                explanation = uiState.explanation,
                onTermChange = viewModel::updateTerm,
                onDefinitionChange = viewModel::updateDefinition,
                onExplanationChange = viewModel::updateExplanation,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CardEditForm(
    term: String,
    definition: String,
    explanation: String,
    onTermChange: (String) -> Unit,
    onDefinitionChange: (String) -> Unit,
    onExplanationChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.screenPadding)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // Term / Question field
        OutlinedTextField(
            value = term,
            onValueChange = onTermChange,
            label = { Text(AppStringsVi.CardEditTerm) },
            placeholder = { Text(AppStringsVi.CardEditTermHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.input),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                focusedLabelColor = AppColors.Primary
            ),
            textStyle = AppTypography.bodyLarge,
            minLines = 2,
            maxLines = 4
        )

        // Definition / Answer field
        OutlinedTextField(
            value = definition,
            onValueChange = onDefinitionChange,
            label = { Text(AppStringsVi.CardEditDefinition) },
            placeholder = { Text(AppStringsVi.CardEditDefinitionHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.input),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                focusedLabelColor = AppColors.Primary
            ),
            textStyle = AppTypography.bodyLarge,
            minLines = 3,
            maxLines = 6
        )

        // Explanation field (optional)
        OutlinedTextField(
            value = explanation,
            onValueChange = onExplanationChange,
            label = { Text(AppStringsVi.CardEditExplanation) },
            placeholder = { Text(AppStringsVi.CardEditExplanationHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.input),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Tertiary,
                focusedLabelColor = AppColors.Tertiary
            ),
            textStyle = AppTypography.bodyMedium,
            minLines = 2,
            maxLines = 4
        )

        Spacer(Modifier.height(AppSpacing.lg))
    }
}
