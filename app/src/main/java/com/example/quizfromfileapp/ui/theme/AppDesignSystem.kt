package com.example.quizfromfileapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM TYPOGRAPHY
// Clean, readable, professional
// ═══════════════════════════════════════════════════════════════
object AppTypography {
    // Display — 30sp for hero sections
    val displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp)
    val displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp)
    val displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp)

    // Headlines — screen titles
    val headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp)
    val headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp)
    val headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)

    // Titles — section headers, card titles
    val titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp)
    val titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
    val titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)

    // Body — primary content
    val bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp)
    val bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp)
    val bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp)

    // Labels — chips, badges, captions
    val labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)

    val m3: Typography get() = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall
    )
}

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM SPACING SYSTEM
// ═══════════════════════════════════════════════════════════════
object AppSpacing {
    const val UNIT = 4
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    val screenPadding = 20.dp
    val screenPaddingLarge = 24.dp
    val screenPaddingSmall = 16.dp

    val cardPadding = 16.dp
    val cardPaddingSmall = 12.dp
    val cardPaddingLarge = 20.dp

    val componentGap = 12.dp
    val sectionGap = 24.dp
    val sectionGapLarge = 32.dp

    val iconXs = 16.dp
    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconXl = 40.dp
}

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM SHAPES
// Soft, modern, rounded
// ═══════════════════════════════════════════════════════════════
object AppRadius {
    // Base
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val full = 999.dp

    // Semantic
    val card = 20.dp        // Large card radius
    val cardLarge = 24.dp    // Hero cards
    val cardElevated = 24.dp // Elevated card
    val input = 16.dp
    val button = 18.dp
    val chip = 999.dp       // Pill shape
    val bottomSheet = 28.dp
    val dialog = 24.dp
    val optionCard = 16.dp
    val flashcard = 24.dp
}

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM ELEVATION
// Light shadows, rely on surface contrast
// ═══════════════════════════════════════════════════════════════
object AppElevation {
    val none = 0.dp
    val low = 1.dp
    val medium = 2.dp
    val high = 4.dp
    val highest = 8.dp
}

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM SIZE
// ═══════════════════════════════════════════════════════════════
object AppSize {
    val buttonHeight = 52.dp
    val buttonHeightSmall = 40.dp
    val buttonHeightLarge = 56.dp
    val inputHeight = 52.dp
    val iconButton = 40.dp
    val avatarSmall = 32.dp
    val avatarMedium = 48.dp
    val avatarLarge = 64.dp
    val avatarXxl = 96.dp
    val cardMinHeight = 80.dp
    val progressBar = 6.dp
    val progressBarLarge = 8.dp
    val divider = 1.dp
    val paletteDot = 32.dp
}

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM MOTION
// ═══════════════════════════════════════════════════════════════
object AppMotion {
    val Instant = 80
    val Fast = 150
    val Normal = 250
    val Medium = 350
    val Slow = 450
    val CardFlip = 400
}
