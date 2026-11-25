package com.example.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TibbGradientScaffoldBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom))),
        content = content,
    )
}

@Composable
fun TibbGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    padding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    val bg = backgroundColor ?: c.surface
    val br = borderColor ?: c.border
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, br, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbMistCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.mistTop, c.mistBottom)))
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbEspressoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.espressoTop, c.espressoBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbThoughtGlowCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    padding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.glowEdge)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.glowCenter.copy(alpha = if (c.isDark) 0.78f else 0.90f),
                            c.glowCenter.copy(alpha = 0.24f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.52f, size.height * 0.48f),
                        radius = size.minDimension * 0.65f,
                    ),
                    center = Offset(size.width * 0.52f, size.height * 0.48f),
                    radius = size.minDimension * 0.65f,
                )
            }
            .border(1.dp, c.border, shape)
            .padding(padding),
        content = content,
    )
}

/**
 * Progressive-disclosure expandable section collapsed by default.
 * Provides consistent header sizing, optional summary line, trailing help slot,
 * animated chevron, and spring-driven expand/collapse motion.
 */
@Composable
fun TibbExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    containerColor: Color? = null,
    borderColor: Color? = null,
    titleColor: Color? = null,
    summaryColor: Color? = null,
    initiallyExpanded: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    headerTestTag: String? = null,
    trailingHeaderContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    var internalExpanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val isExpanded = expanded ?: internalExpanded

    val resolvedAccent = accentColor ?: c.accent
    val resolvedBg = containerColor ?: c.surface
    val resolvedBorder = borderColor ?: if (isExpanded) resolvedAccent.copy(alpha = 0.35f) else c.border
    val resolvedTitleColor = titleColor ?: c.ink
    val resolvedSummaryColor = summaryColor ?: c.muted

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "expand_chevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(resolvedBg)
            .border(1.dp, resolvedBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable {
                    val next = !isExpanded
                    internalExpanded = next
                    onExpandedChange?.invoke(next)
                }
                .let { if (headerTestTag != null) it.testTag(headerTestTag) else it }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(resolvedAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = resolvedTitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = resolvedSummaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailingHeaderContent != null) {
                trailingHeaderContent()
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
                tint = resolvedSummaryColor,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(130)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(color = resolvedBorder.copy(alpha = 0.7f))
                content()
            }
        }
    }
}

/**
 * Unified compact segmented control for 2-3 sibling modes.
 */
@Composable
fun TibbSegmentedTabs(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "seg_tab",
) {
    val c = LocalTibbColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(999.dp))
            .background(c.surfaceMuted)
            .border(1.dp, c.border, RoundedCornerShape(999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (idx, label) ->
            val isSelected = selected == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) c.ctaBg else Color.Transparent)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("${tagPrefix}_$idx"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) c.ctaInk else c.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TibbPrimaryPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalTibbColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: c.ctaBg,
            contentColor = contentColor ?: c.ctaInk,
            disabledContainerColor = (containerColor ?: c.ctaBg).copy(alpha = 0.42f),
            disabledContentColor = (contentColor ?: c.ctaInk).copy(alpha = 0.55f),
        ),
        contentPadding = contentPadding,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        content()
    }
}

@Composable
fun TibbSecondaryPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalTibbColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor ?: c.border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor ?: c.ink,
        ),
        contentPadding = contentPadding,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        content()
    }
}
