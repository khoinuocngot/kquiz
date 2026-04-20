package com.example.quizfromfileapp.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM COLOR PALETTE
// Clean, modern, study-focused
// ═══════════════════════════════════════════════════════════════
object AppColors {
    // Background layers
    val Background = Color(0xFFF6F8FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEF2FF)
    val SurfaceElevated = Color(0xFFFFFFFF)

    // Primary — Indigo/Blue
    val Primary = Color(0xFF5B6CFF)
    val PrimaryLight = Color(0xFF8B95FF)
    val PrimaryDark = Color(0xFF3D4FD9)
    val PrimaryContainer = Color(0xFFE7EBFF)
    val OnPrimaryContainer = Color(0xFF1A2B6E)

    // Secondary — Teal
    val Secondary = Color(0xFF14B8A6)
    val SecondaryLight = Color(0xFF4FD1C5)
    val SecondaryDark = Color(0xFF0D9488)
    val SecondaryContainer = Color(0xFFDDF8F4)
    val OnSecondaryContainer = Color(0xFF134E48)

    // Accent — Violet
    val Accent = Color(0xFF8B5CF6)
    val AccentLight = Color(0xFFB197FC)
    val AccentContainer = Color(0xFFF3EEFF)
    val Tertiary get() = Accent  // Alias for backward compat

    // Text
    val OnSurface = Color(0xFF1F2937)
    val OnSurfaceVariant = Color(0xFF6B7280)
    val OnSurfaceMuted = Color(0xFF9CA3AF)

    // Borders & Dividers
    val Outline = Color(0xFFE5E7EB)
    val OutlineVariant = Color(0xFFF3F4F6)

    // Semantic — Success
    val Success = Color(0xFF22C55E)
    val SuccessLight = Color(0xFF4ADE80)
    val SuccessDark = Color(0xFF16A34A)
    val SuccessContainer = Color(0xFFEAFBF0)
    val OnSuccessContainer = Color(0xFF14532D)
    val SuccessBorder = Color(0xFFBBF7D0)

    // Semantic — Warning
    val Warning = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFFFFF4DB)
    val OnWarningContainer = Color(0xFF78350F)
    val WarningBorder = Color(0xFFFDE68A)

    // Semantic — Error
    val Error = Color(0xFFEF4444)
    val ErrorLight = Color(0xFFF87171)
    val ErrorContainer = Color(0xFFFEECEC)
    val OnErrorContainer = Color(0xFF7F1D1D)
    val ErrorBorder = Color(0xFFFECACA)

    // Semantic — Info
    val Info = Color(0xFF3B82F6)
    val InfoContainer = Color(0xFFEFF6FF)
    val InfoBorder = Color(0xFFDBEAFE)

    // Aliases for backward compat (used by existing code)
    val SuccessSurface get() = SuccessContainer
    val WarningSurface get() = WarningContainer
    val ErrorSurface get() = ErrorContainer
    val InfoSurface get() = InfoContainer
    val OnPrimary get() = Color.White

    // Gradient
    val GradientStart = Color(0xFF5B6CFF)
    val GradientEnd = Color(0xFF8B95FF)

    // Score ring colors
    val ScoreExcellent = Color(0xFF22C55E)
    val ScoreGood = Color(0xFF14B8A6)
    val ScoreMedium = Color(0xFFF59E0B)
    val ScoreLow = Color(0xFFEF4444)

    // Mastery
    val Mastery0 = Color(0xFFD1D5DB)
    val Mastery1 = Color(0xFFFCA5A5)
    val Mastery2 = Color(0xFFFCD34D)
    val Mastery3 = Color(0xFF6EE7B7)
    val Mastery4 = Color(0xFF34D399)
    val Mastery5 = Color(0xFF10B981)

    fun masteryColor(level: Int): Color = when (level) {
        0 -> Mastery0; 1 -> Mastery1; 2 -> Mastery2
        3 -> Mastery3; 4 -> Mastery4; else -> Mastery5
    }

    fun scoreColor(percent: Int): Color = when {
        percent >= 80 -> ScoreExcellent
        percent >= 50 -> ScoreMedium
        else -> ScoreLow
    }

    fun masteryLabel(level: Int): String = when (level) {
        0 -> "Moi"; 1 -> "Bat dau"; 2 -> "Hoc"
        3 -> "Nho"; 4 -> "Thanh thao"; else -> "Xuat sac"
    }
}

// Backward compatibility aliases
val AppPrimary get() = AppColors.Primary
val AppPrimaryLight get() = AppColors.PrimaryLight
val AppPrimaryDark get() = AppColors.PrimaryDark
val AppSecondary get() = AppColors.Secondary
val AppSecondaryLight get() = AppColors.SecondaryLight
val AppSecondaryDark get() = AppColors.SecondaryDark
val AppTertiary get() = AppColors.Accent
val AppTertiaryLight get() = AppColors.AccentLight
val AppTertiaryDark get() = AppColors.Secondary
val AppSuccess get() = AppColors.Success
val AppSuccessLight get() = AppColors.SuccessLight
val AppSuccessBg get() = AppColors.SuccessContainer
val AppError get() = AppColors.Error
val AppErrorLight get() = AppColors.ErrorLight
val AppErrorBg get() = AppColors.ErrorContainer
val AppWarning get() = AppColors.Warning
val AppWarningBg get() = AppColors.WarningContainer
val AppGradientStart get() = AppColors.GradientStart
val AppGradientEnd get() = AppColors.GradientEnd
val ProgressExcellent get() = AppColors.ScoreExcellent
val ProgressGood get() = AppColors.ScoreGood
val ProgressMedium get() = AppColors.ScoreMedium
val ProgressLow get() = AppColors.ScoreLow
val MasteryLevel0 get() = AppColors.Mastery0
val MasteryLevel1 get() = AppColors.Mastery1
val MasteryLevel2 get() = AppColors.Mastery2
val MasteryLevel3 get() = AppColors.Mastery3
val MasteryLevel4 get() = AppColors.Mastery4
val MasteryLevel5 get() = AppColors.Mastery5

// Additional aliases for existing code
val SuccessSurface get() = AppColors.SuccessContainer
val ErrorSurface get() = AppColors.ErrorContainer
val WarningSurface get() = AppColors.WarningContainer
val InfoSurface get() = AppColors.InfoContainer
val OnPrimary get() = Color.White
val SurfaceDark get() = Color(0xFF121A1C)
val BackgroundDark get() = Color(0xFF0D1214)
val BorderDark get() = Color(0xFF2D3E47)
val CardDark get() = Color(0xFF1C2830)
val CardLight get() = AppColors.Surface
val SurfaceLight get() = AppColors.Surface
val BackgroundLight get() = AppColors.Background
val OnSurfaceLight get() = AppColors.OnSurface
val BorderLight get() = AppColors.Outline
val OnPrimaryContainer get() = AppColors.OnPrimaryContainer
val SecondaryDark get() = AppColors.SecondaryDark
val Tertiary get() = AppColors.Accent
val TertiaryLight get() = AppColors.AccentLight
val TertiaryDark get() = AppColors.Secondary

fun masteryColor(level: Int): Color = AppColors.masteryColor(level)
fun scoreColor(percent: Int): Color = AppColors.scoreColor(percent)
fun masteryLabel(level: Int): String = AppColors.masteryLabel(level)
