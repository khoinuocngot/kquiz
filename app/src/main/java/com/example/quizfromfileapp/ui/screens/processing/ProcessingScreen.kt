package com.example.quizfromfileapp.ui.screens.processing

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.R
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.OcrConfidence
import com.example.quizfromfileapp.domain.model.PdfExtractionResult
import com.example.quizfromfileapp.domain.model.PdfPageResult
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    sharedViewModel: AppSharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQuizConfig: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ProcessingViewModel = viewModel(
        factory = ProcessingViewModelFactory(context, sharedViewModel)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startExtraction()
    }

    LaunchedEffect(uiState) {
        if (uiState is ProcessingUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ProcessingUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.processing_title)) },
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
                is ProcessingUiState.Idle -> {
                    IdleView(onNavigateBack = onNavigateBack)
                }

                is ProcessingUiState.Extracting -> {
                    ExtractingView(fileName = state.fileName)
                }

                is ProcessingUiState.Success -> {
                    SuccessView(
                        content = state.content,
                        onNavigateToQuizConfig = onNavigateToQuizConfig,
                        onNavigateBack = onNavigateBack
                    )
                }

                is ProcessingUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onNavigateBack = onNavigateBack,
                        onRetry = { viewModel.startExtraction() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleView(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.height(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.processing_error_no_file),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text(stringResource(R.string.processing_btn_back))
        }
    }
}

@Composable
private fun ExtractingView(fileName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.processing_extracting),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessView(
    content: ExtractedContent,
    onNavigateToQuizConfig: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0=Stats, 1=Cleaned, 2=Raw

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Thẻ thông tin chính ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.processing_success),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = content.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    if (!content.hasEnoughSegments) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Cảnh báo nội dung ít",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Stats row ──────────────────────────────────
                val pdfRes = content.pdfExtractionResult
                if (pdfRes != null) {
                    // PDF: hiển thị stats của merged
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadge(
                            label = "Text Layer",
                            value = "${pdfRes.textLayerTotalChars}",
                            unit = "ký tự",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "OCR",
                            value = "${pdfRes.ocrTotalChars}",
                            unit = "ký tự",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Merged",
                            value = "${pdfRes.mergedTotalChars}",
                            unit = "ký tự",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip("${pdfRes.totalPages} trang", Modifier.weight(1f))
                        InfoChip("${pdfRes.pagesWithTextLayer} text layer", Modifier.weight(1f))
                        InfoChip("${pdfRes.pagesWithOcr} OCR", Modifier.weight(1f))
                    }
                } else {
                    // Non-PDF: hiển thị raw/cleaned
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadge(
                            label = "Raw",
                            value = "${content.rawCharCount}",
                            unit = "ký tự",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Cleaned",
                            value = "${content.cleanedCharCount}",
                            unit = "ký tự",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Segments",
                            value = "${content.segmentCount}",
                            unit = "đoạn",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content.totalPages?.let { InfoChip("$it trang", Modifier.weight(1f)) }
                        content.extractedPages?.let { InfoChip("$it extracted", Modifier.weight(1f)) }
                        if ((content.ocrPages ?: 0) > 0) {
                            InfoChip("${content.ocrPages} OCR",
                                Modifier.weight(1f),
                                isWarning = true)
                        }
                    }
                }

                // ── Extraction quality note ──────────────────
                // pdfRes already declared at line 235
                val qualityNote = when {
                    pdfRes != null -> {
                        val gain = pdfRes.mergedTotalChars - pdfRes.textLayerTotalChars
                        val gainPercent = if (pdfRes.textLayerTotalChars > 0)
                            "${gain * 100 / pdfRes.textLayerTotalChars}%"
                        else "100%"
                        if (gain > 0) "✓ OCR bổ sung thêm $gain ký tự (+$gainPercent) từ text trong ảnh"
                        else "✓ Không có text trong ảnh cần trích xuất"
                    }
                    (content.ocrPages ?: 0) > 0 ->
                        "⚠ OCR bổ sung cho ${content.ocrPages} trang thiếu text"
                    content.extractedPages != null && content.totalPages != null &&
                            content.extractedPages < content.totalPages ->
                        "⚠ Chỉ extract được ${content.extractedPages}/${content.totalPages} trang"
                    else -> "✓ Trích xuất đầy đủ"
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = qualityNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // ── Cảnh báo nội dung ít ───────────────────────────
        if (!content.hasEnoughSegments) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nội dung không đủ để tạo nhiều câu hỏi. Hãy chọn file có nhiều text rõ ràng hơn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Tab chọn: Stats / Text Layer / OCR / Merged / Cleaned / FullBlock (PDF) hoặc Stats / Cleaned / Raw (khác) ──
        val pdfRes = content.pdfExtractionResult
        val isPdf = pdfRes != null

        if (isPdf) {
            // PDF: 6 tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabModifier = Modifier.weight(1f)
                TabButtonBox(
                    label = "Stats",
                    subtitle = "thống kê",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "TextLayer",
                    subtitle = "${pdfRes!!.textLayerTotalChars}",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "OCR",
                    subtitle = "${pdfRes.ocrTotalChars}",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "Merged",
                    subtitle = "${pdfRes.mergedTotalChars}",
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "Cleaned",
                    subtitle = "${content.cleanedCharCount}",
                    isSelected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "FullBlock",
                    subtitle = "${pdfRes.fullMergedTextBlockCleanedCharCount}",
                    isSelected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    modifier = tabModifier
                )
            }
        } else {
            // TXT/Image: 3 tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabModifier = Modifier.weight(1f)
                TabButtonBox(
                    label = "Stats",
                    subtitle = "thống kê",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "Cleaned",
                    subtitle = "${content.cleanedCharCount} ký tự",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = tabModifier
                )
                TabButtonBox(
                    label = "Raw",
                    subtitle = "${content.rawCharCount} ký tự",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = tabModifier
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Nội dung ───────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            when {
                isPdf && selectedTab == 0 -> PdfStatsTabView(pdfRes!!)
                isPdf && selectedTab == 1 -> ContentTabView(
                    label = "Text Layer (đầu tiên 3000 ký tự)",
                    text = pdfRes!!.fullTextLayer.take(3000)
                )
                isPdf && selectedTab == 2 -> ContentTabView(
                    label = "OCR (đầu tiên 3000 ký tự)",
                    text = pdfRes.fullOcrText.take(3000)
                )
                isPdf && selectedTab == 3 -> ContentTabView(
                    label = "Merged (đầu tiên 3000 ký tự)",
                    text = pdfRes!!.fullMergedText.take(3000)
                )
                isPdf && selectedTab == 4 -> ContentTabView(
                    label = "Cleaned (đầu tiên 3000 ký tự)",
                    text = content.cleanedText.take(3000)
                )
                isPdf && selectedTab == 5 -> FullBlockTabView(pdfRes!!)
                !isPdf && selectedTab == 0 -> StatsTabView(content)
                !isPdf && selectedTab == 1 -> ContentTabView(
                    label = "Cleaned (đầu tiên 3000 ký tự)",
                    text = content.cleanedText.take(3000)
                )
                !isPdf && selectedTab == 2 -> ContentTabView(
                    label = "Raw (đầu tiên 3000 ký tự)",
                    text = content.rawText.take(3000)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToQuizConfig,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.processing_btn_next))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.processing_btn_back))
        }
    }
}

@Composable
private fun StatsTabView(content: ExtractedContent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Thống kê trích xuất",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        StatRow("Tên file", content.fileName)
        StatRow("Loại file", content.mimeType)
        StatRow("Raw ký tự", "${content.rawCharCount}")
        StatRow("Cleaned ký tự", "${content.cleanedCharCount}")
        StatRow("Đã mất", "${content.rawCharCount - content.cleanedCharCount} " +
                "(${if (content.rawCharCount > 0)
            "${(content.rawCharCount - content.cleanedCharCount) * 100 / content.rawCharCount}%"
            else "0%"})")

        content.totalPages?.let { StatRow("Tổng trang", "$it") }
        content.extractedPages?.let { StatRow("Trang extracted", "$it") }
        if ((content.ocrPages ?: 0) > 0) {
            StatRow("Trang OCR", "${content.ocrPages}", isWarning = true)
        }
        StatRow("Segments hợp lệ", "${content.segmentCount}")
        StatRow("Dòng đã lọc", "${content.removedLineCount}")
        StatRow("Đủ cho quiz?", if (content.hasEnoughSegments) "✓ Có" else "✗ Không")
    }
}

@Composable
private fun PdfStatsTabView(pdfResult: com.example.quizfromfileapp.domain.model.PdfExtractionResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Thống kê trích xuất PDF",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        // ── Tổng quan ──
        Text(
            text = "Tổng quan",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        StatRow("Tổng trang", "${pdfResult.totalPages}")
        StatRow("Trang có text layer", "${pdfResult.pagesWithTextLayer}")
        StatRow("Trang có OCR", "${pdfResult.pagesWithOcr}")
        StatRow("Trang có cả 2", "${pdfResult.pagesWithBoth}")
        StatRow("Trang không có gì", "${pdfResult.pagesWithNeither}", pdfResult.pagesWithNeither > 0)

        Spacer(modifier = Modifier.height(4.dp))

        // ── Ký tự ──
        Text(
            text = "Ký tự",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        StatRow("Text layer", "${pdfResult.textLayerTotalChars}")
        StatRow("OCR", "${pdfResult.ocrTotalChars}")
        StatRow("Merged", "${pdfResult.mergedTotalChars}")
        StatRow("Tăng thêm từ OCR", "+${pdfResult.mergedTotalChars - pdfResult.textLayerTotalChars}",
            pdfResult.mergedTotalChars - pdfResult.textLayerTotalChars > 0)

        Spacer(modifier = Modifier.height(4.dp))

        // ── Full Block ──
        Text(
            text = "Full Merged Block",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        StatRow("Block thô", "${pdfResult.fullMergedTextBlockCharCount} ký tự")
        StatRow("Block sạch", "${pdfResult.fullMergedTextBlockCleanedCharCount} ký tự")

        Spacer(modifier = Modifier.height(4.dp))

        // ── Chi tiết từng trang ──
        Text(
            text = "Chi tiết từng trang",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        for (page in pdfResult.pages) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Trang ${page.pageIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Layer: ${page.textLayerCharCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (page.hasTextLayer)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "OCR: ${page.ocrCharCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (page.hasOcr)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Merged: ${page.mergedCharCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab hiển thị Full Merged Block (tab số 5).
 * - Preview 500 ký tự đầu của block sạch
 * - Char count tổng
 * - Ghi chú đây là block dùng cho quiz generator
 */
@Composable
private fun FullBlockTabView(pdfResult: com.example.quizfromfileapp.domain.model.PdfExtractionResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Full Merged Block",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Block lớn dùng cho quiz generator (không [PAGE N], không truncate)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Stats ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Block thô", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${pdfResult.fullMergedTextBlockCharCount} ký tự",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Block sạch", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${pdfResult.fullMergedTextBlockCleanedCharCount} ký tự",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Preview (đầu 1000 ký tự) ──
        Text(
            text = "Preview block sạch (đầu 1000 ký tự):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = pdfResult.fullMergedTextBlockCleanedPreview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isWarning)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContentTabView(label: String, text: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun InfoChip(text: String, modifier: Modifier = Modifier, isWarning: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isWarning)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TabButtonBox(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit
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
            modifier = Modifier.height(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Xử lý thất bại",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.processing_btn_back))
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.processing_btn_retry))
            }
        }
    }
}

class ProcessingViewModelFactory(
    private val context: Context,
    private val sharedViewModel: AppSharedViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProcessingViewModel::class.java)) {
            return ProcessingViewModel(context, sharedViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
