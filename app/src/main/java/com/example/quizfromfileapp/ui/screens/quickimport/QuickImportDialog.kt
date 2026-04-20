package com.example.quizfromfileapp.ui.screens.quickimport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.quickimport.ParseResult
import com.example.quizfromfileapp.data.quickimport.ParsedCard
import com.example.quizfromfileapp.data.quickimport.PromptTemplateProvider
import com.example.quizfromfileapp.data.quickimport.PromptTemplateType
import com.example.quizfromfileapp.data.quickimport.QuickImportConfig
import com.example.quizfromfileapp.data.quickimport.StudySetType
import com.example.quizfromfileapp.ui.components.BannerType
import com.example.quizfromfileapp.ui.components.PremiumButton
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumInfoBanner
import com.example.quizfromfileapp.ui.components.PremiumSecondaryButton
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppSuccess
import com.example.quizfromfileapp.ui.theme.AppTypography
import com.example.quizfromfileapp.ui.theme.AppWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickImportDialog(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSuccess: (studySetId: Long) -> Unit,
    viewModel: QuickImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.createdStudySetId != null) {
            onSuccess(uiState.createdStudySetId!!)
            viewModel.reset()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMsg.collect { msg ->
            msg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbar()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(AppRadius.bottomSheet)
    ) {
        QuickImportContent(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onDismiss = {
                viewModel.reset()
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickImportContent(
    viewModel: QuickImportViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Tab state hoisted to đồng bộ với studySetType
    // Mặc định = MULTIPLE_CHOICE_BANK (index 2)
    var selectedPromptTab by remember(uiState.studySetType) {
        mutableIntStateOf(uiState.studySetType.toPromptTemplateType().ordinal)
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(AppStringsVi.QuickImportTitle, style = AppTypography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.OnSurface)
                        Text(AppStringsVi.QuickImportSubtitle, style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface, titleContentColor = AppColors.OnSurface),
                actions = {
                    PremiumIconButton(icon = Icons.Default.Close, contentDescription = "Đóng", onClick = onDismiss)
                }
            )

            StepIndicatorDots()

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    StepLabel(label = AppStringsVi.QuickImportStep1)

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text(AppStringsVi.QuickImportTitleField) },
                        placeholder = { Text(AppStringsVi.QuickImportTitleHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.input),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Outline)
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text(AppStringsVi.QuickImportDescField) },
                        placeholder = { Text(AppStringsVi.QuickImportDescHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.input),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Outline)
                    )

                    StepLabel(label = AppStringsVi.StudySetTypeTermDef)
                    StudySetTypeSelector(
                        selected = uiState.studySetType,
                        onSelect = { type ->
                            viewModel.updateStudySetType(type)
                            // Sync prompt tab với loại vừa chọn
                            selectedPromptTab = type.toPromptTemplateType().ordinal
                        }
                    )

                    StepLabel(label = "Định dạng")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        TermDefDropdown(
                            label = "Phân cách T-D",
                            selected = uiState.termDefDelimiter,
                            onSelect = viewModel::updateTermDefDelimiter,
                            customValue = uiState.cardDelimiterCustom,
                            onCustomChange = viewModel::updateCardDelimiterCustom,
                            modifier = Modifier.weight(1f)
                        )
                        CardDelimiterDropdown(
                            label = "Phân cách thẻ",
                            selected = uiState.cardDelimiterMode,
                            onSelect = viewModel::updateCardDelimiterMode,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    StepLabel(label = "Nội dung")
                    RawContentField(
                        value = uiState.rawText,
                        onValueChange = viewModel::updateRawText,
                        termDefDelimiter = uiState.termDefDelimiter,
                        studySetType = uiState.studySetType
                    )

                    PromptHelperSection(
                        selectedPromptTab = selectedPromptTab,
                        onTabChange = { tabIndex ->
                            selectedPromptTab = tabIndex
                            // Đồng bộ ngược: khi user đổi tab prompt thủ công → cập nhật studySetType
                            val targetType = PromptTemplateType.entries.getOrNull(tabIndex)
                            targetType?.let { viewModel.updateStudySetType(it.toStudySetType()) }
                        },
                        onCopyPrompt = { prompt ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("prompt", prompt))
                            viewModel.showSnackbar("Copy prompt xong rồi nè")
                        }
                    )

                    StepLabel(label = AppStringsVi.QuickImportStep4)
                    PreviewSection(uiState = uiState)

                    PremiumInfoBanner(
                        icon = Icons.Default.Lightbulb,
                        text = "Mẹo: Mỗi dòng = 1 thẻ. Phần đầu = thuật ngữ/câu hỏi, phần sau = định nghĩa/đáp án, cách nhau bằng dấu phân cách bạn đã chọn ở trên (mặc định: Tab). Dòng trống bị bỏ qua.",
                        bannerType = BannerType.Info
                    )

                    Spacer(Modifier.height(80.dp))
                }

                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        BottomBar(uiState = uiState, onDismiss = onDismiss, onCreate = viewModel::importStudySet)
    }
}

// ══════════════════════════════════════════════════════════════════════
// PROMPT HELPER SECTION
// 3 tabs: Thuật ngữ - Định nghĩa | Câu hỏi - Đáp án | Trắc nghiệm
// Tab state được hoist lên QuickImportContent để đồng bộ với studySetType
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun PromptHelperSection(
    selectedPromptTab: Int,
    onTabChange: (Int) -> Unit,
    onCopyPrompt: (String) -> Unit
) {
    val promptTypes = PromptTemplateType.entries
    val tabLabels = promptTypes.map { it.tabLabel }

    Column {
        Text(
            text = "Prompt hỗ trợ",
            style = AppTypography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            text = "Copy prompt này rồi dán qua ChatGPT để nhờ nó format dữ liệu đúng kiểu app",
            style = AppTypography.bodySmall,
            color = AppColors.OnSurfaceVariant
        )
        Spacer(Modifier.height(AppSpacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.card),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, AppColors.Outline)
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                SecondaryTabRow(
                    selectedTabIndex = selectedPromptTab,
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Primary
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedPromptTab == index,
                            onClick = { onTabChange(index) },
                            text = {
                                Text(
                                    label,
                                    style = AppTypography.labelSmall,
                                    fontWeight = if (selectedPromptTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(AppSpacing.md))

                val currentType = promptTypes.getOrNull(selectedPromptTab) ?: PromptTemplateType.TERM_DEFINITION
                val currentPrompt = PromptTemplateProvider.getQuickImportPrompt(currentType)
                PromptHelperCodeCard(
                    promptType = currentType,
                    prompt = currentPrompt,
                    onCopy = { onCopyPrompt(currentPrompt) }
                )

                Spacer(Modifier.height(AppSpacing.sm))

                Text(
                    text = "Nhớ dán dữ liệu gốc của bạn ngay bên dưới prompt khi gửi ChatGPT nha",
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceMuted
                )
            }
        }
    }
}

/**
 * Code block card — nền xám nhạt, monospace font, border trái xanh.
 * Copy button nổi bật top-right.
 */
@Composable
private fun PromptHelperCodeCard(
    promptType: PromptTemplateType,
    prompt: String,
    onCopy: () -> Unit
) {
    val codeBgColor = Color(0xFFF5F7FF)
    val codeBorderColor = Color(0xFF5B6CFF).copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(codeBgColor)
            .border(
                width = 1.dp,
                color = codeBorderColor,
                shape = RoundedCornerShape(AppRadius.md)
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF5B6CFF).copy(alpha = 0.08f))
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(
                            text = promptType.tabLabel,
                            style = AppTypography.labelSmall,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Card(
                        modifier = Modifier.clickable(onClick = onCopy),
                        shape = RoundedCornerShape(AppRadius.sm),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Primary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy prompt",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Copy toàn bộ prompt",
                                style = AppTypography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 300.dp)
                    .padding(AppSpacing.md)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = prompt,
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                            color = AppColors.OnSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorDots() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val isActive = index == 0
            Box(modifier = Modifier.size(if (isActive) 10.dp else 8.dp).clip(CircleShape).background(if (isActive) AppColors.Primary else AppColors.Outline))
            if (index < 4) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun RawContentField(
    value: String,
    onValueChange: (String) -> Unit,
    termDefDelimiter: QuickImportConfig.TermDefDelimiter,
    studySetType: StudySetType
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Column {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
            shape = RoundedCornerShape(AppRadius.input),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, AppColors.Outline)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().verticalScroll(verticalScrollState).horizontalScroll(horizontalScrollState).padding(AppSpacing.md)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = AppColors.OnSurface),
                    cursorBrush = SolidColor(AppColors.Primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false)
                )
                if (value.isEmpty()) {
                    QuickImportPlaceholderHint(termDefDelimiter = termDefDelimiter, studySetType = studySetType)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = delimiterCaptionLine(termDefDelimiter), style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
    }
}

private fun delimiterCaptionLine(delimiter: QuickImportConfig.TermDefDelimiter): String = when (delimiter) {
    QuickImportConfig.TermDefDelimiter.TAB -> "Mỗi dòng = 1 thẻ. Giữa thuật ngữ và định nghĩa: nhấn phím Tab (không dùng nhiều dấu cách)."
    QuickImportConfig.TermDefDelimiter.COMMA -> "Mỗi dòng = 1 thẻ. Dùng dấu phẩy (,) giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.COLON -> "Mỗi dòng = 1 thẻ. Dùng dấu hai chấm (:) giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.SEMICOLON -> "Mỗi dòng = 1 thẻ. Dùng dấu chấm phẩy (;) giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.PIPE -> "Mỗi dòng = 1 thẻ. Dùng dấu gạch đứng (|) giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.ARROW -> "Mỗi dòng = 1 thẻ. Dùng mũi tên -> giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.EQUALS -> "Mỗi dòng = 1 thẻ. Dùng dấu bằng (=) giữa thuật ngữ và định nghĩa."
    QuickImportConfig.TermDefDelimiter.CUSTOM -> "Mỗi dòng = 1 thẻ. Dùng đúng chuỗi phân cách tùy chỉnh bạn đã nhập."
}

@Composable
private fun QuickImportPlaceholderHint(
    termDefDelimiter: QuickImportConfig.TermDefDelimiter,
    studySetType: StudySetType
) {
    val bodyStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = AppColors.OnSurfaceVariant.copy(alpha = 0.6f)
    )
    val delimiterLabel = when (termDefDelimiter) {
        QuickImportConfig.TermDefDelimiter.TAB -> "TAB"
        QuickImportConfig.TermDefDelimiter.COMMA -> ","
        QuickImportConfig.TermDefDelimiter.COLON -> ":"
        QuickImportConfig.TermDefDelimiter.SEMICOLON -> ";"
        QuickImportConfig.TermDefDelimiter.PIPE -> "|"
        QuickImportConfig.TermDefDelimiter.ARROW -> "→"
        QuickImportConfig.TermDefDelimiter.EQUALS -> "="
        QuickImportConfig.TermDefDelimiter.CUSTOM -> "..."
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Dán nội dung vào đây…",
            style = bodyStyle.copy(color = AppColors.OnSurfaceMuted.copy(alpha = 0.7f))
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "Dòng mẫu: ",
                style = bodyStyle.copy(fontSize = 12.sp, color = AppColors.OnSurfaceVariant.copy(alpha = 0.7f))
            )
            Text(
                when (studySetType) {
                    StudySetType.TERM_DEFINITION -> "Thuật ngữ $delimiterLabel Định nghĩa"
                    StudySetType.QUESTION_ANSWER -> "Câu hỏi $delimiterLabel Đáp án"
                    StudySetType.MULTIPLE_CHOICE_BANK -> "Câu hỏi A. lựa chọn A B. lựa chọn B C. lựa chọn C D. lựa chọn D$delimiterLabel Đáp án đúng"
                },
                style = bodyStyle.copy(
                    fontSize = 12.sp,
                    color = AppColors.Primary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun PreviewSection(uiState: QuickImportUiState) {
    val result = uiState.parseResult
    when {
        uiState.rawText.isBlank() -> EmptyPreviewState()
        result == null -> ParsingState()
        result.isEmpty && result.validCount == 0 -> EmptyPreviewState()
        else -> ValidPreviewContent(result = result, hasInvalid = uiState.hasInvalidLines)
    }
}

@Composable private fun EmptyPreviewState() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(AppRadius.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppColors.Outline)) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(32.dp), tint = AppColors.OnSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text(AppStringsVi.QuickImportEmptyRaw, style = AppTypography.bodyMedium, color = AppColors.OnSurfaceVariant)
        }
    }
}

@Composable private fun ParsingState() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(AppRadius.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppColors.Outline)) {
        Row(modifier = Modifier.fillMaxWidth().padding(AppSpacing.xl), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.Primary, trackColor = AppColors.PrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(AppStringsVi.Loading, style = AppTypography.bodyMedium, color = AppColors.OnSurface)
        }
    }
}

@Composable private fun ValidPreviewContent(result: ParseResult, hasInvalid: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SummaryRow(result = result)
        if (hasInvalid) InvalidWarningRow(invalidCount = result.invalidCount)
        if (result.validCards.isNotEmpty()) {
            Text("Thẻ hợp lệ (${result.validCount})", style = AppTypography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppColors.OnSurface)
            result.validCards.take(8).forEach { card -> ValidCardItem(card = card) }
            if (result.validCount > 8) Text("+ ${result.validCount - 8} thẻ khác", style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable private fun SummaryRow(result: ParseResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadius.md), colors = CardDefaults.cardColors(containerColor = AppColors.SuccessContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppColors.SuccessBorder)) {
        Row(modifier = Modifier.fillMaxWidth().padding(AppSpacing.md), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${result.validCount}", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Success)
                Text(AppStringsVi.QuickImportValidCards, style = AppTypography.labelSmall, color = AppColors.OnSurfaceVariant)
            }
            if (result.invalidCount > 0) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${result.invalidCount}", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Warning)
                Text(AppStringsVi.QuickImportInvalidLines, style = AppTypography.labelSmall, color = AppColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable private fun InvalidWarningRow(invalidCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadius.sm), colors = CardDefaults.cardColors(containerColor = AppColors.WarningContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppColors.WarningBorder)) {
        Row(modifier = Modifier.padding(AppSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.sm))
            Text("$invalidCount dòng không parse được. Kiểm tra lại định dạng.", style = AppTypography.bodySmall, color = AppColors.OnSurface)
        }
    }
}

@Composable private fun ValidCardItem(card: ParsedCard) {
    val isMcq = card.choices.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.sm),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppColors.OutlineVariant)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Row 1: Question/term
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    card.term,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isMcq) {
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = "MCQ",
                        style = AppTypography.labelSmall,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isMcq && card.choices.isNotEmpty()) {
                // MCQ: show options and highlight correct answer
                Spacer(Modifier.height(AppSpacing.sm))
                card.choices.forEachIndexed { index, option ->
                    val isCorrect = index == card.correctChoiceIndex
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCorrect) AppColors.Success.copy(alpha = 0.15f)
                                    else AppColors.OnSurfaceVariant.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.substringBefore(". "),
                                style = AppTypography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                                color = if (isCorrect) AppColors.Success else AppColors.OnSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = option.substringAfter(". "),
                            style = AppTypography.bodySmall,
                            color = if (isCorrect) AppColors.Success else AppColors.OnSurfaceVariant,
                            fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Normal: show definition/answer
                Spacer(Modifier.height(2.dp))
                Text(
                    card.definition,
                    style = AppTypography.bodySmall,
                    color = AppColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable private fun BottomBar(uiState: QuickImportUiState, onDismiss: () -> Unit, onCreate: () -> Unit) {
    val canCreate = uiState.title.isNotBlank() && (uiState.parseResult?.validCount ?: 0) > 0
    val hasRawText = uiState.rawText.isNotBlank()
    val validCount = uiState.parseResult?.validCount ?: 0

    Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface).padding(AppSpacing.md)) {
        if (hasRawText && validCount > 0) {
            Text("$validCount thẻ sẵn sàng tạo", style = AppTypography.bodySmall, color = AppColors.Success, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(AppSpacing.sm))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            PremiumSecondaryButton(text = AppStringsVi.Cancel, onClick = onDismiss, modifier = Modifier.weight(1f))
            PremiumButton(text = AppStringsVi.QuickImportCreate, onClick = onCreate, enabled = canCreate, modifier = Modifier.weight(1f))
        }
    }
}

@Composable private fun StudySetTypeSelector(selected: StudySetType, onSelect: (StudySetType) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        StudySetType.entries.forEach { type ->
            val isSelected = type == selected
            Card(
                modifier = Modifier.weight(1f).clickable { onSelect(type) },
                shape = RoundedCornerShape(AppRadius.md),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) AppColors.PrimaryContainer else AppColors.Surface),
                border = if (isSelected) BorderStroke(2.dp, AppColors.Primary) else BorderStroke(1.dp, AppColors.Outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(type.selectorLabel, style = AppTypography.labelSmall, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) AppColors.Primary else AppColors.OnSurface, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TermDefDropdown(
    label: String, selected: QuickImportConfig.TermDefDelimiter, onSelect: (QuickImportConfig.TermDefDelimiter) -> Unit,
    customValue: String, onCustomChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.input),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Outline)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QuickImportConfig.TermDefDelimiter.entries.forEach { delim ->
                DropdownMenuItem(text = { Text(delim.displayName) }, onClick = { onSelect(delim); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CardDelimiterDropdown(
    label: String, selected: QuickImportConfig.CardDelimiterMode, onSelect: (QuickImportConfig.CardDelimiterMode) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.input),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Outline)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QuickImportConfig.CardDelimiterMode.entries.forEach { mode ->
                DropdownMenuItem(text = { Text(mode.displayName) }, onClick = { onSelect(mode); expanded = false })
            }
        }
    }
}

@Composable private fun StepLabel(label: String) {
    Text(label, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.OnSurface)
}
