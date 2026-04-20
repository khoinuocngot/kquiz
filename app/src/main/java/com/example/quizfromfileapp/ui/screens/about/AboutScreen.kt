package com.example.quizfromfileapp.ui.screens.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.BuildConfig
import com.example.quizfromfileapp.R
import com.example.quizfromfileapp.ui.components.PremiumTopBarSurface
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            PremiumTopBarSurface(
                title = AppStringsVi.AboutTitle,
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
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIdentitySection()

            Spacer(Modifier.height(AppSpacing.xxl))

            DeveloperSection(context = context, snackbarHostState = snackbarHostState)

            Spacer(Modifier.height(AppSpacing.xxl))

            CreditsSection()

            Spacer(Modifier.height(AppSpacing.xxl))

            TechStackSection()

            Spacer(Modifier.height(AppSpacing.xxl))

            VersionSection()

            Spacer(Modifier.height(AppSpacing.xxl))
        }
    }
}

@Composable
private fun AppIdentitySection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline.copy(alpha = 0.3f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.kquiz_logo),
                contentDescription = "K Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppRadius.xl)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(AppSpacing.xl))

        Text(
            text = AppStringsVi.AboutAppName,
            style = AppTypography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface
        )

        Spacer(Modifier.height(AppSpacing.sm))

        Text(
            text = AppStringsVi.AboutTagline,
            style = AppTypography.bodyLarge,
            color = AppColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeveloperSection(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl)
        ) {
            Text(
                text = AppStringsVi.AboutDeveloper,
                style = AppTypography.labelMedium,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeveloperPhoto()

                Spacer(Modifier.width(AppSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStringsVi.AboutName,
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "Nhà phát triển ứng dụng",
                        style = AppTypography.bodySmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(AppColors.Primary.copy(alpha = 0.06f))
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    text = AppStringsVi.AboutEmail,
                    style = AppTypography.bodyMedium,
                    color = AppColors.OnSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContentCopy,
                    label = AppStringsVi.AboutCopyEmail,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("email", AppStringsVi.AboutEmail)
                        clipboard.setPrimaryClip(clip)
                    }
                )

                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Email,
                    label = AppStringsVi.AboutSendEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${AppStringsVi.AboutEmail}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("email", AppStringsVi.AboutEmail)
                            clipboard.setPrimaryClip(clip)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeveloperPhoto() {
        Card(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(AppRadius.lg),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline.copy(alpha = 0.2f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.dev_khoi_photo),
                contentDescription = "Ảnh nhà phát triển - ${AppStringsVi.AboutName}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppRadius.lg)),
                contentScale = ContentScale.Fit
            )
        }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.md),
        colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.md, horizontal = AppSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(AppSpacing.xs))
            Text(
                text = label,
                style = AppTypography.labelMedium,
                color = AppColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CreditsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Accent.copy(alpha = 0.04f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Accent.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AppColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    text = "Made with love in Vietnam",
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = "Cảm ơn bạn đã tin tưởng và sử dụng app. Chúc bạn học thật tốt nha!",
                style = AppTypography.bodySmall,
                color = AppColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TechStackSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl)
        ) {
            Text(
                text = AppStringsVi.AboutTechStack,
                style = AppTypography.labelMedium,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(AppSpacing.md))

            AppStringsVi.AboutTechItems.forEach { tech ->
                TechItemRow(icon = Icons.Default.Code, label = tech)
                Spacer(Modifier.height(AppSpacing.sm))
            }
        }
    }
}

@Composable
private fun TechItemRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(AppSpacing.md))
        Text(
            text = label,
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurface
        )
    }
}

@Composable
private fun VersionSection() {
    val versionName = BuildConfig.VERSION_NAME

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.SuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColors.Success,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column {
                Text(
                    text = AppStringsVi.AboutVersion,
                    style = AppTypography.labelMedium,
                    color = AppColors.OnSurfaceVariant
                )
                Text(
                    text = "v$versionName",
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
            }
        }
    }
}
