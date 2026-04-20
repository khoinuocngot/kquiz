package com.example.quizfromfileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quizfromfileapp.ui.theme.AppColors
import com.example.quizfromfileapp.ui.theme.AppMotion
import com.example.quizfromfileapp.ui.theme.AppRadius
import com.example.quizfromfileapp.ui.theme.AppSpacing
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import com.example.quizfromfileapp.ui.theme.AppTypography

// ═══════════════════════════════════════════════════════════════
// PREMIUM PRIMARY BUTTON
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconEnd: Boolean = false,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(AppMotion.Fast),
        label = "btnScale"
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AppRadius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = AppColors.Primary.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md + 2.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        ButtonContent(text = text, icon = icon, iconEnd = iconEnd, isLoading = isLoading)
    }
}

@Composable
fun PremiumButtonRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconEnd: Boolean = false,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(AppMotion.Fast),
        label = "btnScale"
    )
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AppRadius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = AppColors.Primary.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md + 2.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        ButtonContent(text = text, icon = icon, iconEnd = iconEnd, isLoading = isLoading)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, iconEnd: Boolean, isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        Spacer(Modifier.width(AppSpacing.sm))
    } else {
        if (icon != null && !iconEnd) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.sm))
        }
        Text(text, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (icon != null && iconEnd) {
            Spacer(Modifier.width(AppSpacing.sm))
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM SECONDARY BUTTON
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconEnd: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(AppRadius.button),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
        border = androidx.compose.foundation.BorderStroke(1.5.dp,
            if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md + 2.dp)
    ) {
        if (icon != null && !iconEnd) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.sm))
        }
        Text(text, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (icon != null && iconEnd) {
            Spacer(Modifier.width(AppSpacing.sm))
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PremiumSecondaryButtonRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconEnd: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(AppRadius.button),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Primary),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md + 2.dp)
    ) {
        if (icon != null && !iconEnd) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.sm))
        }
        Text(text, style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (icon != null && iconEnd) {
            Spacer(Modifier.width(AppSpacing.sm))
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM GHOST BUTTON
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconEnd: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Primary),
        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        if (icon != null && !iconEnd) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(text, style = AppTypography.labelLarge, fontWeight = FontWeight.Medium)
        if (icon != null && iconEnd) {
            Spacer(Modifier.width(AppSpacing.xs))
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM ICON BUTTON
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppColors.OnSurfaceVariant,
    contentDescription: String? = null,
    buttonSize: Int = 40
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) AppColors.SurfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(AppMotion.Fast),
        label = "iconBtnBg"
    )
    Box(
        modifier = modifier
            .size(buttonSize.dp)
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size((buttonSize * 0.55f).dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM TOP APP BAR
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionsContent: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title, style = AppTypography.titleLarge, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                PremiumIconButton(
                    icon = navigationIcon,
                    onClick = onNavigationClick,
                    contentDescription = AppStringsVi.ActionBack,
                    tint = Color.White,
                    buttonSize = 40
                )
            }
        },
        actions = { actionsContent?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.Primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopBarSurface(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionsContent: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title, style = AppTypography.titleLarge, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                PremiumIconButton(
                    icon = navigationIcon,
                    onClick = onNavigationClick,
                    contentDescription = AppStringsVi.ActionBack,
                    tint = AppColors.OnSurface,
                    buttonSize = 40
                )
            }
        },
        actions = { actionsContent?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.OnSurface,
            navigationIconContentColor = AppColors.OnSurface,
            actionIconContentColor = AppColors.OnSurfaceVariant
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM NAVIGATION BAR
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumNavigationBar(
    selectedIndex: Int,
    items: List<Pair<String, ImageVector>>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = AppColors.Surface,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(index) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = AppTypography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.Primary,
                    selectedTextColor = AppColors.Primary,
                    unselectedIconColor = AppColors.OnSurfaceVariant,
                    unselectedTextColor = AppColors.OnSurfaceVariant,
                    indicatorColor = AppColors.PrimaryContainer
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM CARD
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    ) {
        Column(content = content, modifier = Modifier)
    }
}

@Composable
fun PremiumCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content, modifier = Modifier)
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM SEARCH BAR
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Tìm kiếm…",
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.input))
            .background(AppColors.SurfaceVariant)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
        textStyle = AppTypography.bodyMedium.copy(color = AppColors.OnSurface),
        singleLine = true,
        cursorBrush = SolidColor(AppColors.Primary),
        keyboardOptions = KeyboardOptions(
            imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(AppSpacing.sm))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTypography.bodyMedium,
                            color = AppColors.OnSurfaceMuted
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    PremiumIconButton(
                        icon = Icons.Default.Clear,
                        onClick = { onQueryChange("") },
                        tint = AppColors.OnSurfaceVariant,
                        buttonSize = 32
                    )
                }
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM FILTER CHIPS
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTypography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        },
        leadingIcon = if (icon != null && isSelected) {
            { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.PrimaryContainer,
            selectedLabelColor = AppColors.Primary,
            selectedLeadingIconColor = AppColors.Primary,
            containerColor = AppColors.SurfaceVariant,
            labelColor = AppColors.OnSurfaceVariant,
            iconColor = AppColors.OnSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.Transparent,
            selectedBorderColor = AppColors.Primary.copy(alpha = 0.3f),
            enabled = true,
            selected = isSelected
        ),
        shape = RoundedCornerShape(AppRadius.chip)
    )
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM SECTION HEADER
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTypography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        if (actionLabel != null && onAction != null) {
            PremiumGhostButton(text = actionLabel, onClick = onAction)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM DIVIDER
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.OutlineVariant)
    )
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM EMPTY STATE
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(AppRadius.xl))
                .background(AppColors.PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(AppSpacing.xl))
        Text(
            text = title,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = subtitle,
                style = AppTypography.bodyMedium,
                color = AppColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(AppSpacing.xl))
            action.invoke()
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM LOADING STATE
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumLoadingState(
    modifier: Modifier = Modifier,
    message: String = AppStringsVi.Loading
) {
    Column(
        modifier = modifier.padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            trackColor = AppColors.PrimaryContainer,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(AppSpacing.lg))
        Text(
            text = message,
            style = AppTypography.bodyMedium,
            color = AppColors.OnSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM INFO BANNER
// ═══════════════════════════════════════════════════════════════
@Composable
fun PremiumInfoBanner(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    bannerType: BannerType = BannerType.Info
) {
    val (bgColor, iconColor) = when (bannerType) {
        BannerType.Info -> AppColors.SurfaceVariant to AppColors.Info
        BannerType.Warning -> AppColors.WarningContainer to AppColors.Warning
        BannerType.Success -> AppColors.SuccessContainer to AppColors.Success
        BannerType.Error -> AppColors.ErrorContainer to AppColors.Error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(AppRadius.sm),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp,
            when (bannerType) {
                BannerType.Info -> AppColors.Outline
                BannerType.Warning -> AppColors.WarningBorder
                BannerType.Success -> AppColors.SuccessBorder
                BannerType.Error -> AppColors.ErrorBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.sm))
            Text(text, style = AppTypography.bodySmall, color = AppColors.OnSurface)
        }
    }
}

enum class BannerType { Info, Warning, Success, Error }
