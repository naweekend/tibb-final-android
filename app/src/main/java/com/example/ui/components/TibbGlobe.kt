package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTibbColors
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class GlobeDot(val x: Float, val y: Float, val z: Float, val highlighted: Boolean)

/**
 * Animated 3D rotating Fibonacci-lattice botanical globe rendered natively on Compose Canvas.
 * Matches TibbGlobe.tsx from the web app without any HTML/Canvas dependencies.
 */
@Composable
fun TibbGlobe(
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
) {
    val c = LocalTibbColors.current
    val transition = rememberInfiniteTransition(label = "tibb_globe")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globe_rotation",
    )

    val dots = remember {
        val total = 320
        val goldenRatio = (1.0 + sqrt(5.0)) / 2.0
        List(total) { i ->
            val t = (i + 0.5) / total
            val phi = acos(1.0 - 2.0 * t)
            val theta = (2.0 * PI * i) / goldenRatio
            val x = (sin(phi) * cos(theta)).toFloat()
            val y = cos(phi).toFloat()
            val z = (sin(phi) * sin(theta)).toFloat()
            // Highlight approximate South Asia / Pakistan arc dots gently
            val highlighted = (i % 19 == 0) || (y in 0.1f..0.48f && z > 0.35f && i % 5 == 0)
            GlobeDot(x, y, z, highlighted)
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Ambient radial aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c.accentPastel.copy(alpha = if (c.isDark) 0.20f else 0.34f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = r * 1.32f,
            ),
            center = Offset(cx, cy),
            radius = r * 1.32f,
        )

        // Subtle equator & meridian rings
        drawCircle(
            color = c.accent.copy(alpha = if (c.isDark) 0.22f else 0.28f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2.dp.toPx()),
        )

        val cosA = cos(angle)
        val sinA = sin(angle)
        val tilt = 0.28f
        val cosT = cos(tilt)
        val sinT = sin(tilt)

        val projected = dots.map { d ->
            val rx = d.x * cosA - d.z * sinA
            val rz = d.x * sinA + d.z * cosA
            val ry = d.y * cosT - rz * sinT
            val fz = d.y * sinT + rz * cosT
            Triple(rx, ry, fz) to d.highlighted
        }.sortedBy { it.first.third }

        val dotBase = if (c.isDark) c.ink else c.ctaBg
        val highlightColor = c.accent

        for ((coords, highlighted) in projected) {
            val (px, py, pz) = coords
            val depth = ((pz + 1f) / 2f).coerceIn(0f, 1f)
            val alpha = 0.12f + depth * (if (highlighted) 0.85f else 0.62f)
            val radius = (if (highlighted) 2.9.dp else 1.85.dp).toPx() * (0.55f + 0.55f * depth)
            drawCircle(
                color = (if (highlighted && pz > 0f) highlightColor else dotBase).copy(alpha = alpha),
                radius = radius,
                center = Offset(cx + px * r, cy - py * r),
            )
        }
    }
}
