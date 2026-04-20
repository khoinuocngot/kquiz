package com.example.quizfromfileapp.ui.screens.importfile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizfromfileapp.data.helper.FileMetadataHelper
import com.example.quizfromfileapp.data.model.ImportedFile
import com.example.quizfromfileapp.ui.components.PremiumButton
import com.example.quizfromfileapp.ui.components.PremiumIconButton
import com.example.quizfromfileapp.ui.components.PremiumSecondaryButton
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFileScreen(
    sharedViewModel: AppSharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: () -> Unit,
    viewModel: FileImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            viewModel.processSelectedUri(context, uri)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is FileImportUiState.Error) {
            snackbarHostState.showSnackbar((uiState as FileImportUiState.Error).message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.ImportFileTitle,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(innerPadding)
                .padding(AppSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(AppSpacing.lg))

            // Hero section
            HeroSection()

            Spacer(Modifier.height(AppSpacing.xxl))

            // Tip card
            TipCard()

            Spacer(Modifier.height(AppSpacing.xl))

            // Select file action
            SelectFileAction(
                onSelectFile = {
                    filePickerLauncher.launch(FileMetadataHelper.supportedMimeTypes())
                }
            )

            Spacer(Modifier.height(AppSpacing.lg))

            // File state
            when (val state = uiState) {
                is FileImportUiState.Idle -> IdleFileState()
                is FileImportUiState.FileSelected -> FileInfoCard(
                    file = state.file,
                    onClear = { viewModel.clearSelection() }
                )
                is FileImportUiState.Error -> IdleFileState()
            }

            Spacer(Modifier.weight(1f))

            // Bottom buttons
            val hasFile = uiState is FileImportUiState.FileSelected
            PremiumButton(
                text = AppStringsVi.ImportFileNext,
                onClick = {
                    if (hasFile) {
                        val file = (uiState as FileImportUiState.FileSelected).file
                        sharedViewModel.setSelectedFile(file)
                        onNavigateToProcessing()
                    }
                },
                enabled = hasFile,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppSpacing.md))

            PremiumSecondaryButton(
                text = AppStringsVi.ActionBack,
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppSpacing.lg))
        }
    }
}

@Composable
private fun HeroSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(AppColors.Primary.copy(alpha = 0.12f), AppColors.PrimaryLight.copy(alpha = 0.08f))
                    ),
                    shape = RoundedCornerShape(AppRadius.cardLarge)
                )
                .border(1.5.dp, AppColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(AppRadius.cardLarge)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = AppStringsVi.ImportFileHero,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AppSpacing.sm))

        Text(
            text = AppStringsVi.ImportFileSub,
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.lg)
        )
    }
}

@Composable
private fun TipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.md),
        colors = CardDefaults.cardColors(containerColor = AppColors.InfoContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppColors.InfoBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AppColors.Info.copy(alpha = 0.15f), RoundedCornerShape(AppRadius.sm)),
                contentAlignment = Alignment.Center
            ) {
                Text("💡", style = AppTypography.titleSmall)
            }
            Spacer(Modifier.width(AppSpacing.md))
            Text(
                text = AppStringsVi.ImportFileTip,
                style = AppTypography.bodySmall,
                color = AppColors.OnSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectFileAction(onSelectFile: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectFile),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, AppColors.Primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(AppSpacing.md))
            Text(
                text = AppStringsVi.ImportFileSelect,
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )
        }
    }
}

@Composable
private fun IdleFileState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppColors.Outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AppColors.OnSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = AppStringsVi.ImportFileEmpty,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FileInfoCard(file: ImportedFile, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, AppColors.Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(AppColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(AppRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = file.mimeType.iconForMimeType(),
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = file.mimeType,
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                    Text(" • ", style = AppTypography.bodySmall, color = AppColors.OnSurfaceVariant)
                    Text(
                        text = file.formattedSize,
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = AppStringsVi.Delete,
                    tint = AppColors.OnSurfaceVariant
                )
            }
        }
    }
}

private fun String.iconForMimeType(): ImageVector = when {
    this == "text/plain" -> Icons.Default.Description
    this == "application/pdf" -> Icons.Default.PictureAsPdf
    this.startsWith("image/") -> Icons.Default.Image
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}
