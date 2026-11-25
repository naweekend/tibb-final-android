package com.example.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
    val auraColor = if (c.isDark) c.accent.copy(alpha = 0.10f) else LightLavender.copy(alpha = 0.65f)
    val warmAura = if (c.isDark) c.accentPastel.copy(alpha = 0.05f) else IvoryBgTop.copy(alpha = 0.70f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgMid, c.bgBottom)))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(auraColor, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.14f),
                        radius = size.minDimension * 0.72f,
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    radius = size.minDimension * 0.72f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(warmAura, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.78f),
                        radius = size.minDimension * 0.68f,
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.78f),
                    radius = size.minDimension * 0.68f,
                )
            },
        content = content,
    )
}

@Composable
fun TibbGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    padding: Dp = 18.dp,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    val bg = backgroundColor ?: c.surfaceElevated
    val br = borderColor ?: if (c.isDark) c.border else c.border.copy(alpha = 0.06f)
    Box(
        modifier = modifier
            .shadow(
                elevation = if (c.isDark) 0.dp else elevation,
                shape = shape,
                ambientColor = Color(0xFF242326).copy(alpha = 0.08f),
                spotColor = Color(0xFF6452B5).copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(bg)
            .border(0.75.dp, br, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbMistCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .shadow(
                elevation = if (c.isDark) 0.dp else 8.dp,
                shape = shape,
                ambientColor = c.accentPastel.copy(alpha = 0.25f),
                spotColor = c.accent.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.mistTop, c.mistBottom)))
            .border(0.75.dp, if (c.isDark) c.border else Color.White.copy(alpha = 0.55f), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbEspressoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    padding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.16f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.espressoTop, c.espressoBottom)))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun TibbThoughtGlowCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    padding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = modifier
            .shadow(
                elevation = if (c.isDark) 0.dp else 4.dp,
                shape = shape,
                ambientColor = c.accentPastel.copy(alpha = 0.22f),
                spotColor = c.accentPastel.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(c.glowEdge)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.glowCenter.copy(alpha = if (c.isDark) 0.80f else 0.92f),
                            c.glowCenter.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.55f, size.height * 0.45f),
                        radius = size.minDimension * 0.72f,
                    ),
                    center = Offset(size.width * 0.55f, size.height * 0.45f),
                    radius = size.minDimension * 0.72f,
                )
            }
            .border(0.75.dp, c.border.copy(alpha = if (c.isDark) 0.2f else 0.06f), shape)
            .padding(padding),
        content = content,
    )
}

/**
 * Progressive-disclosure expandable section collapsed by default.
 * Provides floating rounded geometry, soft pastel icon pill, animated chevron,
 * and spring-driven expand/collapse motion.
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
    val resolvedBg = containerColor ?: c.surfaceElevated
    val resolvedBorder = borderColor ?: if (isExpanded) {
        resolvedAccent.copy(alpha = 0.28f)
    } else if (c.isDark) {
        c.border
    } else {
        c.border.copy(alpha = 0.06f)
    }
    val resolvedTitleColor = titleColor ?: c.ink
    val resolvedSummaryColor = summaryColor ?: c.muted
    val shape = RoundedCornerShape(24.dp)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "expand_chevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (c.isDark) 0.dp else 4.dp,
                shape = shape,
                ambientColor = Color(0xFF242326).copy(alpha = 0.07f),
                spotColor = Color(0xFF6452B5).copy(alpha = 0.07f),
            )
            .clip(shape)
            .background(resolvedBg)
            .border(0.75.dp, resolvedBorder, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clickable {
                    val next = !isExpanded
                    internalExpanded = next
                    onExpandedChange?.invoke(next)
                }
                .let { if (headerTestTag != null) it.testTag(headerTestTag) else it }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(resolvedAccent.copy(alpha = if (c.isDark) 0.18f else 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(19.dp),
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(color = resolvedBorder.copy(alpha = 0.6f))
                content()
            }
        }
    }
}

/**
 * Unified compact segmented control for 2-3 sibling modes with smooth active color animation.
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
            .background(c.lilacGrey.copy(alpha = if (c.isDark) 0.55f else 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (idx, label) ->
            val isSelected = selected == idx
            val pillColor by animateColorAsState(
                targetValue = if (isSelected) c.ctaBg else Color.Transparent,
                animationSpec = tween(180),
                label = "seg_pill_bg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) c.ctaInk else c.ink.copy(alpha = 0.72f),
                animationSpec = tween(180),
                label = "seg_pill_ink",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillColor)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("${tagPrefix}_$idx"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalTibbColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "primary_pill_scale",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (c.isDark) 0.dp else 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: c.ctaBg,
            contentColor = contentColor ?: c.ctaInk,
            disabledContainerColor = (containerColor ?: c.ctaBg).copy(alpha = 0.42f),
            disabledContentColor = (contentColor ?: c.ctaInk).copy(alpha = 0.58f),
        ),
        contentPadding = contentPadding,
        modifier = modifier
            .scale(scale)
            .heightIn(min = 46.dp),
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
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalTibbColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "secondary_pill_scale",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.75.dp, borderColor ?: c.accentPastel.copy(alpha = if (c.isDark) 0.25f else 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor ?: c.secondaryPillBg.copy(alpha = 0.65f),
            contentColor = contentColor ?: c.secondaryPillInk,
            disabledContainerColor = c.surfaceMuted.copy(alpha = 0.5f),
            disabledContentColor = c.muted,
        ),
        contentPadding = contentPadding,
        modifier = modifier
            .scale(scale)
            .heightIn(min = 46.dp),
    ) {
        content()
    }
}
