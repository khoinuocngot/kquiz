package com.example.quizfromfileapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
// LIGHT PREMIUM SHADOWS
// Very subtle, modern. Light apps rely on surface contrast + borders
// rather than heavy drop shadows.
// ═══════════════════════════════════════════════════════════════
object AppShadows {
    private val SoftShadow = Color(0x08000000)
    private val LightShadow = Color(0x05000000)

    @Composable
    fun card(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(AppRadius.card)) =
        modifier.shadow(3.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)

    @Composable
    fun elevated(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(AppRadius.card)) =
        modifier.shadow(6.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)

    @Composable
    fun button(modifier: Modifier) =
        modifier.shadow(2.dp, RoundedCornerShape(AppRadius.button), ambientColor = LightShadow, spotColor = LightShadow)
}
