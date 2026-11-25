package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
data class TibbDonutSegment(
    val value: Float,
    val color: Color,
    val label: String,
)

@Composable
fun TibbDonutGauge(
    progress: Float,
    centerValue: String,
    modifier: Modifier = Modifier,
    centerCaption: String? = null,
    size: Dp = 68.dp,
    strokeWidth: Dp = 7.dp,
    activeColor: Color? = null,
    trackColor: Color? = null,
    centerValueFontSize: TextUnit = 15.sp,
    centerTextColor: Color? = null,
    segments: List<TibbDonutSegment>? = null,
    accessibilityLabel: String = "$centerValue ${centerCaption.orEmpty()}".trim(),
) {
    val c = LocalTibbColors.current
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "donut_progress",
    )
    val resolvedTrack = trackColor ?: c.surfaceMuted
    val resolvedActive = activeColor ?: c.accent
    val resolvedInk = centerTextColor ?: c.ink

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = resolvedTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            if (!segments.isNullOrEmpty()) {
                val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
                var startAngle = -90f
                val gap = if (segments.count { it.value > 0f } > 1) 4f else 0f
                segments.forEach { seg ->
                    if (seg.value > 0f) {
                        val sweep = ((seg.value / total) * 360f - gap).coerceAtLeast(3f)
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle + gap / 2f,
                            sweepAngle = sweep * animatedProgress.coerceAtLeast(0.05f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                        startAngle += (seg.value / total) * 360f
                    }
                }
            } else if (animatedProgress > 0f) {
                drawArc(
                    color = resolvedActive,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = TabularNumberStyle.copy(
                    fontSize = centerValueFontSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = resolvedInk,
                maxLines = 1,
            )
            if (!centerCaption.isNullOrBlank()) {
                Text(
                    text = centerCaption,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = resolvedInk.copy(alpha = 0.68f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Immutable
data class TibbRangeZone(
    val label: String,
    val weight: Float,
    val color: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TibbLinearRangeBar(
    zones: List<TibbRangeZone>,
    markerFraction: Float?,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    markerColor: Color? = null,
    showLegend: Boolean = true,
    accessibilityLabel: String = "Range indicator",
) {
    val c = LocalTibbColors.current
    val totalWeight = zones.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
    val animatedMarker by animateFloatAsState(
        targetValue = (markerFraction ?: 0f).coerceIn(0.02f, 0.98f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "range_marker",
    )
    val resolvedMarkerColor = markerColor ?: c.ink

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + 10.dp),
        ) {
            val barH = height.toPx()
            val barTop = (size.height - barH) / 2f
            val gapPx = 3.dp.toPx()
            val totalGap = gapPx * (zones.size - 1).coerceAtLeast(0)
            val usableW = (size.width - totalGap).coerceAtLeast(1f)
            var x = 0f

            zones.forEach { zone ->
                val w = (zone.weight / totalWeight) * usableW
                drawRoundRect(
                    color = zone.color.copy(alpha = if (c.isDark) 0.55f else 0.72f),
                    topLeft = Offset(x, barTop),
                    size = Size(w, barH),
                    cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                )
                x += w + gapPx
            }

            if (markerFraction != null) {
                val markerX = animatedMarker * size.width
                val centerY = size.height / 2f
                drawCircle(
                    color = if (c.isDark) c.surfaceElevated else Color.White,
                    radius = barH * 0.85f,
                    center = Offset(markerX, centerY),
                )
                drawCircle(
                    color = resolvedMarkerColor,
                    radius = barH * 0.58f,
                    center = Offset(markerX, centerY),
                )
            }
        }

        if (showLegend && zones.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                zones.forEach { zone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(zone.color),
                        )
                        Text(
                            text = zone.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Immutable
data class TibbBarItem(
    val label: String,
    val value: Float,
    val formattedValue: String = "",
    val color: Color? = null,
    val isHighlighted: Boolean = false,
)

@Composable
fun TibbBarComparisonChart(
    items: List<TibbBarItem>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 112.dp,
    goalValue: Float? = null,
    goalLabel: String? = null,
    defaultBarColor: Color? = null,
    highlightBarColor: Color? = null,
    accessibilityLabel: String = "Bar comparison chart",
) {
    val c = LocalTibbColors.current
    if (items.isEmpty()) return

    val maxVal = max(
        items.maxOfOrNull { it.value } ?: 1f,
        goalValue ?: 0f,
    ).coerceAtLeast(1f)
    val baseColor = defaultBarColor ?: c.accentPastel
    val activeColor = highlightBarColor ?: c.accent
    val gridColor = c.border.copy(alpha = if (c.isDark) 0.25f else 0.14f)
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bar_scale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!goalLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = goalLabel,
                    style = TabularNumberStyle.copy(fontSize = 10.5.sp),
                    color = c.muted,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            // Subtle horizontal reference gridlines
            listOf(0.25f, 0.62f, 1.0f).forEach { frac ->
                val y = size.height * (1f - frac)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (frac < 1f) dashEffect else null,
                )
            }

            if (goalValue != null && goalValue > 0f) {
                val goalY = size.height * (1f - (goalValue / maxVal).coerceIn(0.05f, 1f))
                drawLine(
                    color = activeColor.copy(alpha = 0.55f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect,
                )
            }

            val count = items.size
            val slotW = size.width / count.toFloat()
            val barW = min(slotW * 0.54f, 26.dp.toPx())
            val radiusPx = 7.dp.toPx()

            items.forEachIndexed { idx, item ->
                val norm = (item.value / maxVal).coerceIn(0f, 1f) * animatedScale
                val barH = max(norm * (size.height - 6.dp.toPx()), if (item.value > 0f) 4.dp.toPx() else 2.dp.toPx())
                val x = slotW * idx + (slotW - barW) / 2f
                val y = size.height - barH
                val barColor = item.color ?: if (item.isHighlighted) activeColor else baseColor.copy(alpha = 0.72f)

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (item.formattedValue.isNotBlank()) {
                        Text(
                            text = item.formattedValue,
                            style = TabularNumberStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (item.isHighlighted) c.ink else c.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (item.isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (item.isHighlighted) c.ink else c.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Immutable
data class TibbTrendPoint(
    val label: String,
    val primaryValue: Float,
    val secondaryValue: Float? = null,
)

@Composable
fun TibbTrendLineChart(
    points: List<TibbTrendPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 118.dp,
    primaryColor: Color? = null,
    secondaryColor: Color? = null,
    targetMin: Float? = null,
    targetMax: Float? = null,
    unitSuffix: String = "",
    accessibilityLabel: String = "Trend chart",
) {
    val c = LocalTibbColors.current
    if (points.isEmpty()) return

    val pColor = primaryColor ?: c.accent
    val sColor = secondaryColor ?: c.wellnessBlue
    val allValues = buildList {
        points.forEach {
            add(it.primaryValue)
            it.secondaryValue?.let { sec -> add(sec) }
        }
        targetMin?.let { add(it) }
        targetMax?.let { add(it) }
    }
    val rawMin = allValues.minOrNull() ?: 0f
    val rawMax = allValues.maxOrNull() ?: 100f
    val pad = ((rawMax - rawMin) * 0.16f).coerceAtLeast(6f)
    val minY = (rawMin - pad).coerceAtLeast(0f)
    val maxY = rawMax + pad
    val spanY = (maxY - minY).coerceAtLeast(1f)
    val gridColor = c.border.copy(alpha = if (c.isDark) 0.25f else 0.14f)
    val nodeCenterBg = c.surfaceElevated

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val latest = points.last()
            val latestStr = buildString {
                append(latest.primaryValue.roundToInt())
                latest.secondaryValue?.let { append("/${it.roundToInt()}") }
                if (unitSuffix.isNotBlank()) append(" $unitSuffix")
            }
            Text(
                text = "Latest: $latestStr",
                style = TabularNumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = pColor,
            )
            Text(
                text = "${rawMin.roundToInt()}–${rawMax.roundToInt()}${if (unitSuffix.isNotBlank()) " $unitSuffix" else ""}",
                style = TabularNumberStyle.copy(fontSize = 11.sp),
                color = c.muted,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val topPad = 10.dp.toPx()
            val botPad = 10.dp.toPx()
            val sidePad = 8.dp.toPx()
            val usableW = (size.width - sidePad * 2).coerceAtLeast(1f)
            val usableH = (size.height - topPad - botPad).coerceAtLeast(1f)

            fun mapY(v: Float): Float = topPad + (1f - ((v - minY) / spanY).coerceIn(0f, 1f)) * usableH
            fun mapX(idx: Int): Float = if (points.size == 1) size.width / 2f else sidePad + (idx.toFloat() / (points.size - 1)) * usableW

            // Target healthy shaded band
            if (targetMin != null && targetMax != null) {
                val bandTop = mapY(targetMax)
                val bandBottom = mapY(targetMin)
                drawRect(
                    color = c.safeGreen.copy(alpha = if (c.isDark) 0.14f else 0.10f),
                    topLeft = Offset(0f, bandTop),
                    size = Size(size.width, (bandBottom - bandTop).coerceAtLeast(2f)),
                )
            }

            // Subtle dashed horizontal gridlines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = topPad + frac * usableH
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
            }

            // Secondary line (e.g. Diastolic BP)
            val secondaryOffsets = points.mapIndexedNotNull { idx, pt ->
                pt.secondaryValue?.let { Offset(mapX(idx), mapY(it)) }
            }
            if (secondaryOffsets.size >= 2) {
                val secPath = Path()
                secondaryOffsets.forEachIndexed { idx, pt ->
                    if (idx == 0) secPath.moveTo(pt.x, pt.y)
                    else {
                        val prev = secondaryOffsets[idx - 1]
                        val midX = (prev.x + pt.x) / 2f
                        secPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                    }
                }
                drawPath(
                    path = secPath,
                    color = sColor.copy(alpha = 0.85f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                secondaryOffsets.forEach { pt ->
                    drawCircle(color = nodeCenterBg, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = sColor, radius = 2.6.dp.toPx(), center = pt)
                }
            }

            // Primary line + smooth area fill
            val primaryOffsets = points.mapIndexed { idx, pt -> Offset(mapX(idx), mapY(pt.primaryValue)) }
            if (primaryOffsets.size >= 2) {
                val linePath = Path()
                val fillPath = Path()
                primaryOffsets.forEachIndexed { idx, pt ->
                    if (idx == 0) {
                        linePath.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, size.height - botPad)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        val prev = primaryOffsets[idx - 1]
                        val midX = (prev.x + pt.x) / 2f
                        linePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                        fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                    }
                }
                fillPath.lineTo(primaryOffsets.last().x, size.height - botPad)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(pColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = topPad,
                        endY = size.height - botPad,
                    ),
                )
                drawPath(
                    path = linePath,
                    color = pColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            primaryOffsets.forEachIndexed { idx, pt ->
                val isLast = idx == primaryOffsets.lastIndex
                drawCircle(color = nodeCenterBg, radius = if (isLast) 5.5.dp.toPx() else 4.2.dp.toPx(), center = pt)
                drawCircle(color = pColor, radius = if (isLast) 3.8.dp.toPx() else 2.8.dp.toPx(), center = pt)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (idx == points.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (idx == points.lastIndex) c.ink else c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun TibbMetricProgressRow(
    label: String,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val animPct by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(420),
        label = "metric_row_pct",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = c.ink)
            Text(
                "$percent%",
                style = TabularNumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = color,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surfaceMuted),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animPct)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color),
            )
        }
    }
}
