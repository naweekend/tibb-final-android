package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    padding: Dp = 20.dp,
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
    shape: Shape = RoundedCornerShape(24.dp),
    padding: Dp = 20.dp,
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
    shape: Shape = RoundedCornerShape(28.dp),
    padding: Dp = 20.dp,
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
    shape: Shape = RoundedCornerShape(24.dp),
    padding: Dp = 20.dp,
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
                            c.glowCenter.copy(alpha = if (c.isDark) 0.90f else 0.95f),
                            c.glowCenter.copy(alpha = 0.30f),
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

@Composable
fun TibbPrimaryPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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
            disabledContainerColor = (containerColor ?: c.ctaBg).copy(alpha = 0.45f),
            disabledContentColor = (contentColor ?: c.ctaInk).copy(alpha = 0.6f),
        ),
        contentPadding = contentPadding,
        modifier = modifier,
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
        modifier = modifier,
    ) {
        content()
    }
}
