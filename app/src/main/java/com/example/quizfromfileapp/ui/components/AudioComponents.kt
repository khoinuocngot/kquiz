package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.data.repository.StudyAudioManager
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppTypography

/**
 * Widget nhỏ gọn để bật/tắt voice và sfx.
 * Có thể đặt trong menu hoặc settings.
 */
@Composable
fun AudioToggleWidget(
    audioManager: StudyAudioManager,
    modifier: Modifier = Modifier
) {
    val voiceEnabled by audioManager.voiceEnabled.collectAsState()
    val sfxEnabled by audioManager.sfxEnabled.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(
                text = "Âm thanh",
                style = AppTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Spacer(Modifier.height(AppSpacing.md))

            AudioToggleRow(
                icon = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                iconTint = if (voiceEnabled) AppColors.Primary else AppColors.OnSurfaceVariant,
                title = "Giọng đọc",
                subtitle = "Đọc nội dung thẻ học",
                isEnabled = voiceEnabled,
                onToggle = { audioManager.setVoiceEnabled(!voiceEnabled) }
            )

            Spacer(Modifier.height(AppSpacing.md))

            AudioToggleRow(
                icon = Icons.Default.MusicNote,
                iconTint = if (sfxEnabled) AppColors.Primary else AppColors.OnSurfaceVariant,
                title = "Âm thanh hiệu ứng",
                subtitle = "Tiếng khi chọn đáp án, lật thẻ",
                isEnabled = sfxEnabled,
                onToggle = { audioManager.setSfxEnabled(!sfxEnabled) }
            )
        }
    }
}

/**
 * Một dòng bật/tắt cho audio.
 */
@Composable
private fun AudioToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isEnabled) AppColors.PrimaryContainer.copy(alpha = 0.4f) else AppColors.SurfaceVariant.copy(alpha = 0.5f),
        label = "toggleBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(bgColor)
            .clickable(onClick = onToggle)
            .padding(AppSpacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isEnabled) AppColors.PrimaryContainer else AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(AppSpacing.sm + 2.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurface
            )
            Text(
                text = subtitle,
                style = AppTypography.labelSmall,
                color = AppColors.OnSurfaceVariant
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.Primary,
                checkedTrackColor = AppColors.PrimaryContainer,
                uncheckedThumbColor = AppColors.OnSurfaceVariant,
                uncheckedTrackColor = AppColors.SurfaceVariant
            )
        )
    }
}

/**
 * Nút TTS nhỏ gọn để đặt bên cạnh text.
 * Hiện icon loa, bấm để đọc.
 */
@Composable
fun TtsSpeakerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "Đọc to",
            tint = if (isEnabled) AppColors.Primary else AppColors.OnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
