package com.example.quizfromfileapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ══════════════════════════════════════════════════════
// LIGHT PREMIUM COLOR SCHEME
// Clean, modern, study-focused
// ══════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    // Backgrounds
    background = AppColors.Background,
    onBackground = AppColors.OnSurface,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.OnSurfaceVariant,

    // Primary — Indigo/Blue
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryContainer,
    onPrimaryContainer = AppColors.OnPrimaryContainer,

    // Secondary — Teal
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AppColors.SecondaryContainer,
    onSecondaryContainer = AppColors.OnSecondaryContainer,

    // Tertiary — Violet
    tertiary = AppColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.AccentContainer,
    onTertiaryContainer = Color(0xFF4C1D95),

    // Error
    error = AppColors.Error,
    onError = Color.White,
    errorContainer = AppColors.ErrorContainer,
    onErrorContainer = AppColors.OnErrorContainer,

    // Borders & Outlines
    outline = AppColors.Outline,
    outlineVariant = AppColors.OutlineVariant,

    // Inverse (used for dialogs on dark surfaces)
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB),
    inversePrimary = AppColors.PrimaryLight,

    // Scrim
    scrim = Color(0x52000000)
)

// ══════════════════════════════════════════════════════
// THEME COMPOSABLE — LIGHT PREMIUM ONLY
// ══════════════════════════════════════════════════════
@Composable
fun QuizFromFileAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColors.Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography.m3,
        content = content
    )
}
