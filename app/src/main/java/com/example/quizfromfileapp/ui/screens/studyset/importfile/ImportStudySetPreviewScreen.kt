package com.example.quizfromfileapp.ui.screens.studyset.importfile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.repository.StudySetExportData
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.domain.usecase.ImportResult
import com.example.quizfromfileapp.domain.usecase.ImportStudySetUseCase
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSize
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
// SCREEN: Import Study Set Preview
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStudySetPreviewScreen(
    onNavigateBack: () -> Unit,
    onImportSuccess: (Long) -> Unit,
    viewModel: ImportPreviewViewModel = remember { ImportPreviewViewModel() }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.stateFlow.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.pickFile(context, it) }
    }

    // Auto-open picker on first load
    LaunchedEffect(Unit) {
        filePicker.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
    }

    // Handle import success
    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            val msg = if (result.isDuplicate) {
                "Đã nhập \"${result.title}\" (bản sao) — ${result.cardCount} thẻ"
            } else {
                "Đã nhập \"${result.title}\" — ${result.cardCount} thẻ"
            }
            snackbarHostState.showSnackbar(msg)
            kotlinx.coroutines.delay(800)
            onImportSuccess(result.studySetId)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar("Lỗi: $error")
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nhập bộ học",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent(step = uiState.loadingStep)
                }

                uiState.parseResult != null -> {
                    val previewData = uiState.parseResult!!
                    ImportPreviewContent(
                        exportData = previewData,
                        onConfirm = { viewModel.doImport() },
                        onCancel = {
                            viewModel.reset()
                            filePicker.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        },
                        isImporting = uiState.isImporting
                    )
                }

                else -> {
                    EmptyFilePickerContent(
                        onPickFile = {
                            filePicker.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// LOADING CONTENT
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingContent(step: ImportLoadingStep) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(AppSpacing.lg))
        Text(
            when (step) {
                ImportLoadingStep.READING_FILE -> "Đang đọc file…"
                ImportLoadingStep.PARSING_FILE -> "Đang phân tích dữ liệu…"
                ImportLoadingStep.IMPORTING -> "Đang nhập vào máy…"
            },
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// EMPTY STATE: Chưa chọn file
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyFilePickerContent(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPaddingLarge)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AppColors.Primary.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(AppSpacing.xxl))

        Text(
            "Chọn file để nhập",
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            "Chọn file có định dạng .studyset hoặc .json đã xuất từ app này để nhập vào máy.",
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.xxl)
        )

        Spacer(Modifier.height(AppSpacing.sectionGapLarge))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.SurfaceVariant
            ),
            shape = RoundedCornerShape(AppRadius.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.cardPaddingLarge)
            ) {
                Text(
                    "Hỗ trợ:",
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(AppSpacing.md))
                FileFormatRow(
                    icon = Icons.Default.Description,
                    label = ".studyset",
                    desc = "Định dạng chuẩn của app"
                )
                Spacer(Modifier.height(AppSpacing.sm))
                FileFormatRow(
                    icon = Icons.Default.Description,
                    label = ".json",
                    desc = "Backup / file cũ"
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.sectionGapLarge))

        Button(
            onClick = onPickFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary
            ),
            shape = RoundedCornerShape(AppRadius.button)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(AppSpacing.sm))
            Text(
                "Chọn file",
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(AppSpacing.xxl))
    }
}

@Composable
private fun FileFormatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.Primary
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            label,
            style = AppTypography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            "— $desc",
            style = AppTypography.bodySmall,
            color = AppColors.OnSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// PREVIEW CONTENT: Xác nhận import
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ImportPreviewContent(
    exportData: StudySetExportData,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isImporting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPaddingLarge)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AppColors.Success
            )
            Spacer(Modifier.width(AppSpacing.sm))
            Text(
                "Xem trước file",
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Thông tin study set
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                shape = RoundedCornerShape(AppRadius.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, AppColors.Outline)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.cardPaddingLarge)) {
                    Text(
                        exportData.studySet.title,
                        style = AppTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )

                    if (exportData.studySet.description.isNotBlank()) {
                        Spacer(Modifier.height(AppSpacing.sm))
                        Text(
                            exportData.studySet.description,
                            style = AppTypography.bodyMedium,
                            color = AppColors.OnSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(AppSpacing.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip(
                            icon = Icons.Default.Layers,
                            value = "${exportData.flashcards.size}",
                            label = "thẻ"
                        )
                        StatChip(
                            icon = Icons.Default.Description,
                            value = exportData.studySet.studySetTypeLabel,
                            label = "loại"
                        )
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Preview thẻ
            Text(
                "Xem trước thẻ",
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurfaceVariant
            )

            Spacer(Modifier.height(AppSpacing.md))

            exportData.flashcards.take(3).forEachIndexed { index, card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < 2) AppSpacing.sm else 0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(AppRadius.sm),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, AppColors.OutlineVariant)
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.cardPadding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "#${index + 1}",
                                style = AppTypography.labelSmall,
                                color = AppColors.OnSurfaceMuted,
                                modifier = Modifier.padding(end = AppSpacing.sm)
                            )
                            Text(
                                card.term.take(60) + if (card.term.length > 60) "…" else "",
                                style = AppTypography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.OnSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(AppSpacing.xs))
                        Text(
                            card.definition.take(100) + if (card.definition.length > 100) "…" else "",
                            style = AppTypography.bodySmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                }
            }

            if (exportData.flashcards.size > 3) {
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    "+ ${exportData.flashcards.size - 3} thẻ khác",
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Cảnh báo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.PrimaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(AppRadius.sm),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.Primary
                    )
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        "Nếu bộ học trùng tên, app sẽ tự tạo bản sao mới.",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(AppSize.buttonHeight),
                shape = RoundedCornerShape(AppRadius.button),
                border = BorderStroke(1.dp, AppColors.Outline)
            ) {
                Text(
                    "Hủy",
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onConfirm,
                enabled = !isImporting,
                modifier = Modifier
                    .weight(1f)
                    .height(AppSize.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(AppRadius.button)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AppColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Nhập vào máy",
                        style = AppTypography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                AppColors.SurfaceVariant,
                RoundedCornerShape(AppRadius.chip)
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.Primary
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            value,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.width(AppSpacing.xxs))
        Text(
            label,
            style = AppTypography.labelSmall,
            color = AppColors.OnSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ══════════════════════════════════════════════════════════════════════

enum class ImportLoadingStep {
    READING_FILE, PARSING_FILE, IMPORTING
}

data class ImportPreviewUiState(
    val isLoading: Boolean = false,
    val loadingStep: ImportLoadingStep = ImportLoadingStep.READING_FILE,
    val parseResult: StudySetExportData? = null,
    val importResult: ImportResult? = null,
    val isImporting: Boolean = false,
    val error: String? = null
)

class ImportPreviewViewModel(
    private val importUseCase: ImportStudySetUseCase = AppContainer.importStudySetUseCase
) : ViewModel() {

    private val _stateFlow = kotlinx.coroutines.flow.MutableStateFlow(ImportPreviewUiState())
    val stateFlow = _stateFlow.asStateFlow()

    // Compatibility getter
    val importResult: ImportResult? get() = _stateFlow.value.importResult
    val error: String? get() = _stateFlow.value.error

    fun pickFile(context: Any, uri: Uri) {
        _stateFlow.value = _stateFlow.value.copy(
            isLoading = true,
            loadingStep = ImportLoadingStep.READING_FILE,
            parseResult = null,
            importResult = null,
            error = null
        )

        viewModelScope.launch {
            try {
                val contentResolver = when (context) {
                    is android.content.Context -> context.contentResolver
                    else -> return@launch
                }

                val readResult = importUseCase.readFileContent(contentResolver, uri)
                if (readResult.isFailure) {
                    _stateFlow.value = _stateFlow.value.copy(
                        isLoading = false,
                        error = readResult.exceptionOrNull()?.message ?: "Lỗi khi đọc file"
                    )
                    return@launch
                }

                val content = readResult.getOrThrow()

                _stateFlow.value = _stateFlow.value.copy(
                    loadingStep = ImportLoadingStep.PARSING_FILE
                )

                val parseResult = importUseCase.parseFile(content)
                if (parseResult.isFailure) {
                    _stateFlow.value = _stateFlow.value.copy(
                        isLoading = false,
                        error = parseResult.exceptionOrNull()?.message ?: "File không hợp lệ"
                    )
                    return@launch
                }

                _stateFlow.value = _stateFlow.value.copy(
                    isLoading = false,
                    parseResult = parseResult.getOrThrow()
                )
            } catch (e: Exception) {
                _stateFlow.value = _stateFlow.value.copy(
                    isLoading = false,
                    error = e.message ?: "Lỗi không xác định"
                )
            }
        }
    }

    fun doImport() {
        val parseResult = _stateFlow.value.parseResult ?: return

        _stateFlow.value = _stateFlow.value.copy(
            isImporting = true,
            loadingStep = ImportLoadingStep.IMPORTING
        )

        viewModelScope.launch {
            val result = importUseCase.doImport(parseResult)
            if (result.isSuccess) {
                _stateFlow.value = _stateFlow.value.copy(
                    isImporting = false,
                    importResult = result.getOrNull()
                )
            } else {
                _stateFlow.value = _stateFlow.value.copy(
                    isImporting = false,
                    error = result.exceptionOrNull()?.message ?: "Lỗi khi nhập dữ liệu"
                )
            }
        }
    }

    fun clearError() {
        _stateFlow.value = _stateFlow.value.copy(error = null)
    }

    fun reset() {
        _stateFlow.value = ImportPreviewUiState()
    }
}
