package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.GeoCoords
import com.example.data.model.LocationCategory
import com.example.data.model.PakistanLocationRecord
import com.example.data.model.PakistanProvince
import com.example.data.model.SelectedLocationState
import com.example.data.model.TriageTier
import com.example.engine.TriageEngine
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min

private const val PK_LAT_MIN = 23.45
private const val PK_LAT_MAX = 37.15
private const val PK_LNG_MIN = 60.85
private const val PK_LNG_MAX = 77.85
private val PK_COS_LAT = cos(Math.toRadians(30.3))

private data class ProvincePolygon(
    val province: PakistanProvince,
    val points: List<Pair<Double, Double>>, // (lat, lng)
    val labelLat: Double,
    val labelLng: Double,
)

private val PAKISTAN_PROVINCE_POLYGONS = listOf(
    ProvincePolygon(
        province = PakistanProvince.BALOCHISTAN,
        points = listOf(
            25.15 to 61.62,
            25.45 to 61.85,
            26.85 to 63.15,
            27.60 to 63.00,
            29.10 to 61.20,
            29.85 to 60.90,
            29.90 to 62.65,
            29.55 to 64.95,
            30.25 to 66.35,
            31.20 to 67.70,
            32.02 to 69.32,
            31.65 to 69.95,
            30.40 to 70.10,
            29.25 to 69.50,
            28.45 to 68.15,
            27.80 to 67.45,
            26.20 to 67.35,
            25.25 to 66.92,
            24.92 to 66.62,
            25.42 to 64.65,
            25.25 to 63.45,
            25.12 to 62.32,
        ),
        labelLat = 28.45,
        labelLng = 65.50,
    ),
    ProvincePolygon(
        province = PakistanProvince.SINDH,
        points = listOf(
            24.85 to 66.75,
            25.25 to 66.92,
            26.20 to 67.35,
            27.80 to 67.45,
            28.45 to 68.15,
            28.40 to 69.65,
            27.75 to 69.95,
            26.40 to 70.18,
            25.45 to 70.85,
            24.45 to 71.05,
            24.15 to 69.75,
            23.72 to 68.15,
            24.30 to 67.35,
        ),
        labelLat = 26.20,
        labelLng = 68.45,
    ),
    ProvincePolygon(
        province = PakistanProvince.PUNJAB,
        points = listOf(
            28.40 to 69.65,
            28.45 to 70.35,
            29.25 to 69.50,
            30.40 to 70.10,
            31.65 to 69.95,
            32.65 to 71.15,
            33.15 to 71.85,
            33.85 to 72.32,
            33.92 to 73.55,
            33.10 to 73.78,
            32.65 to 74.68,
            32.20 to 75.38,
            31.55 to 74.60,
            30.80 to 74.38,
            29.95 to 73.32,
            28.95 to 72.70,
            27.75 to 71.00,
            27.75 to 69.95,
        ),
        labelLat = 30.85,
        labelLng = 72.35,
    ),
    ProvincePolygon(
        province = PakistanProvince.KHYBER_PAKHTUNKHWA,
        points = listOf(
            31.65 to 69.95,
            32.02 to 69.32,
            33.12 to 69.90,
            33.92 to 70.05,
            34.12 to 71.10,
            35.25 to 71.35,
            36.85 to 72.55,
            36.88 to 73.65,
            35.70 to 73.85,
            35.05 to 74.05,
            34.05 to 73.55,
            33.92 to 72.95,
            33.85 to 72.32,
            33.15 to 71.85,
            32.65 to 71.15,
        ),
        labelLat = 34.45,
        labelLng = 71.75,
    ),
    ProvincePolygon(
        province = PakistanProvince.ISLAMABAD,
        points = listOf(
            33.58 to 72.82,
            33.79 to 72.88,
            33.82 to 73.26,
            33.59 to 73.28,
        ),
        labelLat = 33.70,
        labelLng = 73.06,
    ),
    ProvincePolygon(
        province = PakistanProvince.GILGIT_BALTISTAN,
        points = listOf(
            35.05 to 74.05,
            35.70 to 73.85,
            36.88 to 73.65,
            37.05 to 74.65,
            36.45 to 75.85,
            35.65 to 77.65,
            34.65 to 77.05,
            34.50 to 75.50,
            34.95 to 74.45,
        ),
        labelLat = 35.90,
        labelLng = 75.20,
    ),
    ProvincePolygon(
        province = PakistanProvince.AZAD_KASHMIR,
        points = listOf(
            33.05 to 73.65,
            33.92 to 73.55,
            34.70 to 73.75,
            34.95 to 74.45,
            34.35 to 74.30,
            33.65 to 74.15,
            32.85 to 74.35,
            32.65 to 74.05,
        ),
        labelLat = 33.85,
        labelLng = 73.92,
    ),
)

private val INDUS_RIVER_COORDS = listOf(
    35.30 to 75.60,
    35.92 to 74.30,
    35.42 to 74.10,
    34.15 to 72.70,
    33.62 to 72.22,
    32.56 to 71.54,
    31.82 to 70.91,
    30.05 to 70.72,
    28.98 to 70.35,
    27.71 to 68.85,
    26.75 to 67.85,
    25.38 to 68.37,
    24.15 to 67.62,
)

private data class MapViewport(
    val left: Float,
    val top: Float,
    val baseWidth: Float,
    val baseHeight: Float,
    val zoom: Float,
    val pan: Offset,
) {
    fun project(lat: Double, lng: Double): Offset {
        val normX = ((lng - PK_LNG_MIN) / (PK_LNG_MAX - PK_LNG_MIN)).toFloat()
        val normY = ((PK_LAT_MAX - lat) / (PK_LAT_MAX - PK_LAT_MIN)).toFloat()
        val centerX = left + baseWidth * 0.5f
        val centerY = top + baseHeight * 0.5f
        val rawX = left + normX * baseWidth
        val rawY = top + normY * baseHeight
        return Offset(
            x = centerX + (rawX - centerX) * zoom + pan.x,
            y = centerY + (rawY - centerY) * zoom + pan.y,
        )
    }

    fun unproject(screenPos: Offset): Pair<Double, Double> {
        val centerX = left + baseWidth * 0.5f
        val centerY = top + baseHeight * 0.5f
        val rawX = ((screenPos.x - pan.x - centerX) / zoom) + centerX
        val rawY = ((screenPos.y - pan.y - centerY) / zoom) + centerY
        val normX = ((rawX - left) / baseWidth).toDouble()
        val normY = ((rawY - top) / baseHeight).toDouble()
        val lng = (PK_LNG_MIN + normX * (PK_LNG_MAX - PK_LNG_MIN)).coerceIn(PK_LNG_MIN, PK_LNG_MAX)
        val lat = (PK_LAT_MAX - normY * (PK_LAT_MAX - PK_LAT_MIN)).coerceIn(PK_LAT_MIN, PK_LAT_MAX)
        return lat to lng
    }
}

private fun computeViewport(canvasSize: IntSize, zoom: Float, pan: Offset): MapViewport {
    val w = canvasSize.width.toFloat().coerceAtLeast(1f)
    val h = canvasSize.height.toFloat().coerceAtLeast(1f)
    val pad = 18f
    val availW = (w - pad * 2f).coerceAtLeast(1f)
    val availH = (h - pad * 2f).coerceAtLeast(1f)
    val geoAspect = (((PK_LNG_MAX - PK_LNG_MIN) * PK_COS_LAT) / (PK_LAT_MAX - PK_LAT_MIN)).toFloat()
    val boxAspect = availW / availH
    val baseW: Float
    val baseH: Float
    if (boxAspect > geoAspect) {
        baseH = availH
        baseW = baseH * geoAspect
    } else {
        baseW = availW
        baseH = baseW / geoAspect
    }
    return MapViewport(
        left = (w - baseW) * 0.5f,
        top = (h - baseH) * 0.5f,
        baseWidth = baseW,
        baseHeight = baseH,
        zoom = zoom,
        pan = pan,
    )
}

@Composable
fun PakistanOfflineMapCanvas(
    allLocations: List<PakistanLocationRecord>,
    filteredLocations: List<PakistanLocationRecord>,
    selectedLat: Double?,
    selectedLng: Double?,
    selectedProvince: PakistanProvince?,
    zoom: Float,
    pan: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    onSelectExistingLocation: (PakistanLocationRecord) -> Unit,
    onPinTappedAt: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    var canvasSize by remember { mutableStateOf(IntSize(800, 560)) }

    val latestZoom by rememberUpdatedState(zoom)
    val latestPan by rememberUpdatedState(pan)
    val latestLocations by rememberUpdatedState(filteredLocations)
    val latestCanvasSize by rememberUpdatedState(canvasSize)

    val displayLocations = remember(allLocations, filteredLocations, selectedProvince, zoom) {
        val baseSet = linkedMapOf<String, PakistanLocationRecord>()
        // Always include Major Cities for continental orientation
        allLocations.filter { it.category == LocationCategory.MAJOR_CITY }.forEach {
            baseSet[it.id] = it
        }
        // Add District HQs when slightly zoomed or province filtered
        if (zoom >= 1.6f || selectedProvince != null) {
            filteredLocations.filter { it.category == LocationCategory.DISTRICT_HQ }.forEach {
                baseSet[it.id] = it
            }
        }
        // Add remaining filtered towns, neighborhoods, localities, landmarks smoothly
        val limit = when {
            zoom >= 4.0f -> 360
            zoom >= 2.0f -> 180
            filteredLocations.size <= 65 -> filteredLocations.size
            else -> 75
        }
        filteredLocations.take(limit).forEach { baseSet[it.id] = it }
        baseSet.values.toList()
    }
    val latestDisplay by rememberUpdatedState(displayLocations)

    val labelPaint = remember(c.ink) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 24f
            color = android.graphics.Color.argb(
                220,
                (c.ink.red * 255).toInt(),
                (c.ink.green * 255).toInt(),
                (c.ink.blue * 255).toInt(),
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val provPaint = remember(c.muted) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.argb(
                145,
                (c.muted.red * 255).toInt(),
                (c.muted.green * 255).toInt(),
                (c.muted.blue * 255).toInt(),
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceMuted)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panDelta, zoomChange, _ ->
                    val oldZoom = latestZoom
                    val newZoom = (oldZoom * zoomChange).coerceIn(1.0f, 14.0f)
                    val actualScale = newZoom / oldZoom
                    val sz = latestCanvasSize
                    val cx = sz.width * 0.5f
                    val cy = sz.height * 0.5f
                    val maxPanX = sz.width * 0.55f * newZoom
                    val maxPanY = sz.height * 0.55f * newZoom
                    val rawPan = Offset(
                        x = (latestPan.x + (centroid.x - cx) * (1f - actualScale)) * actualScale + panDelta.x,
                        y = (latestPan.y + (centroid.y - cy) * (1f - actualScale)) * actualScale + panDelta.y,
                    )
                    val clampedPan = if (newZoom <= 1.02f) Offset.Zero else Offset(
                        x = rawPan.x.coerceIn(-maxPanX, maxPanX),
                        y = rawPan.y.coerceIn(-maxPanY, maxPanY),
                    )
                    onTransformChange(newZoom, clampedPan)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val vp = computeViewport(latestCanvasSize, latestZoom, latestPan)
                    val hitRadiusPx = 30f
                    var nearestLoc: PakistanLocationRecord? = null
                    var bestPx = Float.MAX_VALUE
                    for (loc in latestDisplay.ifEmpty { latestLocations }) {
                        val pt = vp.project(loc.lat, loc.lng)
                        val dist = hypot(pt.x - tapOffset.x, pt.y - tapOffset.y)
                        if (dist < hitRadiusPx && dist < bestPx) {
                            bestPx = dist
                            nearestLoc = loc
                        }
                    }
                    if (nearestLoc != null) {
                        onSelectExistingLocation(nearestLoc)
                    } else {
                        val (lat, lng) = vp.unproject(tapOffset)
                        onPinTappedAt(lat, lng)
                    }
                }
            }
            .testTag("pk_offline_map_canvas"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            val vp = computeViewport(canvasSize, zoom, pan)

            // Subtle lat/lng graticule grid
            val gridStroke = c.border.copy(alpha = 0.45f)
            for (latGrid in listOf(24.0, 26.0, 28.0, 30.0, 32.0, 34.0, 36.0)) {
                val p1 = vp.project(latGrid, PK_LNG_MIN)
                val p2 = vp.project(latGrid, PK_LNG_MAX)
                drawLine(gridStroke, p1, p2, strokeWidth = 1f)
            }
            for (lngGrid in listOf(62.0, 64.0, 66.0, 68.0, 70.0, 72.0, 74.0, 76.0)) {
                val p1 = vp.project(PK_LAT_MIN, lngGrid)
                val p2 = vp.project(PK_LAT_MAX, lngGrid)
                drawLine(gridStroke, p1, p2, strokeWidth = 1f)
            }

            // Province & Territory polygons
            PAKISTAN_PROVINCE_POLYGONS.forEach { poly ->
                val isSelectedProv = selectedProvince == poly.province
                val path = Path().apply {
                    poly.points.forEachIndexed { idx, (lat, lng) ->
                        val pt = vp.project(lat, lng)
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                    close()
                }
                val fillCol = when {
                    isSelectedProv -> c.accent.copy(alpha = 0.28f)
                    poly.province == PakistanProvince.PUNJAB -> c.safeGreen.copy(alpha = 0.13f)
                    poly.province == PakistanProvince.SINDH -> c.wellnessBlue.copy(alpha = 0.13f)
                    poly.province == PakistanProvince.KHYBER_PAKHTUNKHWA -> c.accent.copy(alpha = 0.12f)
                    poly.province == PakistanProvince.BALOCHISTAN -> c.urgentYellow.copy(alpha = 0.11f)
                    poly.province == PakistanProvince.GILGIT_BALTISTAN -> Color(0xFF7B9FE8).copy(alpha = 0.15f)
                    poly.province == PakistanProvince.AZAD_KASHMIR -> Color(0xFF5DC49B).copy(alpha = 0.18f)
                    else -> c.dangerRed.copy(alpha = 0.22f)
                }
                drawPath(path = path, color = fillCol)
                drawPath(
                    path = path,
                    color = if (isSelectedProv) c.accent else c.accent.copy(alpha = 0.52f),
                    style = Stroke(
                        width = if (isSelectedProv) 3.2f else 1.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Indus River ribbon
            val indusPath = Path().apply {
                INDUS_RIVER_COORDS.forEachIndexed { idx, (lat, lng) ->
                    val pt = vp.project(lat, lng)
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = indusPath,
                color = c.wellnessBlue.copy(alpha = 0.48f),
                style = Stroke(
                    width = (2.2f * min(zoom, 3f)),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(12f),
                ),
            )

            // Province labels
            PAKISTAN_PROVINCE_POLYGONS.forEach { poly ->
                val center = vp.project(poly.labelLat, poly.labelLng)
                if (center.x in -40f..(size.width + 40f) && center.y in -20f..(size.height + 20f)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        poly.province.shortCode,
                        center.x,
                        center.y,
                        provPaint,
                    )
                }
            }

            // Location markers
            displayLocations.forEach { loc ->
                val pt = vp.project(loc.lat, loc.lng)
                if (pt.x < -20f || pt.x > size.width + 20f || pt.y < -20f || pt.y > size.height + 20f) return@forEach
                val isSelected = selectedLat != null && selectedLng != null &&
                    hypot(loc.lat - selectedLat, loc.lng - selectedLng) < 0.008
                val dotColor = when (loc.category) {
                    LocationCategory.MAJOR_CITY -> c.accent
                    LocationCategory.DISTRICT_HQ -> c.safeGreen
                    LocationCategory.LANDMARK, LocationCategory.PUBLIC_AREA -> c.wellnessBlue
                    else -> c.urgentYellow
                }
                val radius = when (loc.category) {
                    LocationCategory.MAJOR_CITY -> if (zoom >= 2f) 8.5f else 6.5f
                    LocationCategory.DISTRICT_HQ -> if (zoom >= 2f) 6.5f else 4.8f
                    else -> if (zoom >= 3.5f) 5.5f else 3.8f
                }
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = radius + 2.2f,
                    center = pt,
                )
                drawCircle(
                    color = if (isSelected) c.dangerRed else dotColor,
                    radius = radius,
                    center = pt,
                )

                val showLabel = loc.category == LocationCategory.MAJOR_CITY ||
                    (zoom >= 2.3f && loc.category == LocationCategory.DISTRICT_HQ) ||
                    zoom >= 4.2f ||
                    isSelected
                if (showLabel) {
                    drawContext.canvas.nativeCanvas.drawText(
                        loc.nameEn.take(22),
                        pt.x + radius + 5f,
                        pt.y + 6f,
                        labelPaint,
                    )
                }
            }

            // Active Pin Marker
            if (selectedLat != null && selectedLng != null) {
                val pinPt = vp.project(selectedLat, selectedLng)
                drawCircle(
                    color = c.dangerRed.copy(alpha = 0.24f),
                    radius = 24f,
                    center = pinPt,
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = pinPt,
                )
                drawCircle(
                    color = c.dangerRed,
                    radius = 8.5f,
                    center = pinPt,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakistanLocationPickerDialog(
    locale: AppLocale,
    triageEngine: TriageEngine,
    initialSelection: SelectedLocationState?,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
    onConfirm: (SelectedLocationState) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val allLocations = remember { triageEngine.getPakistanLocations() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedProvince by remember { mutableStateOf<PakistanProvince?>(null) }
    var selectedCategory by remember { mutableStateOf<LocationCategory?>(null) }

    var activeSelection by remember {
        mutableStateOf(
            initialSelection ?: run {
                val def = allLocations.firstOrNull { it.nameEn == "Islamabad" } ?: allLocations.first()
                SelectedLocationState(
                    name = def.nameEn,
                    district = def.district,
                    province = def.province.displayNameEn,
                    lat = def.lat,
                    lng = def.lng,
                    isCustomPin = false,
                )
            }
        )
    }

    var mapZoom by remember { mutableFloatStateOf(1.1f) }
    var mapPan by remember { mutableStateOf(Offset.Zero) }
    var waitingForGpsSnap by remember { mutableStateOf(false) }

    LaunchedEffect(userGpsCoords, waitingForGpsSnap) {
        if (waitingForGpsSnap && userGpsCoords != null) {
            val nearest = triageEngine.findNearestPakistanLocation(userGpsCoords.lat, userGpsCoords.lng)
            activeSelection = SelectedLocationState(
                name = nearest?.let { "GPS near ${it.nameEn}" } ?: "GPS Location",
                district = nearest?.district ?: "Pakistan",
                province = nearest?.province?.displayNameEn ?: "",
                lat = userGpsCoords.lat,
                lng = userGpsCoords.lng,
                isCustomPin = true,
            )
            waitingForGpsSnap = false
        }
    }

    val filteredLocations = remember(searchQuery, selectedProvince, selectedCategory) {
        triageEngine.searchPakistanLocations(
            query = searchQuery,
            province = selectedProvince,
            category = selectedCategory,
        )
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            waitingForGpsSnap = true
            onRequestGps()
        }
    }

    fun focusOnCoords(lat: Double, lng: Double, newZoom: Float = mapZoom) {
        mapZoom = newZoom.coerceIn(1f, 14f)
        // Reset pan cleanly near center when picking from list at high zoom
        mapPan = Offset.Zero
    }

    fun selectNamedLocation(loc: PakistanLocationRecord) {
        activeSelection = SelectedLocationState(
            name = if (isUr) loc.nameUr else loc.nameEn,
            district = loc.district,
            province = loc.province.displayNameEn,
            lat = loc.lat,
            lng = loc.lng,
            isCustomPin = false,
        )
    }

    fun setCustomPinCoords(lat: Double, lng: Double) {
        val clampedLat = lat.coerceIn(PK_LAT_MIN, PK_LAT_MAX)
        val clampedLng = lng.coerceIn(PK_LNG_MIN, PK_LNG_MAX)
        val nearest = triageEngine.findNearestPakistanLocation(clampedLat, clampedLng)
        activeSelection = SelectedLocationState(
            name = nearest?.let { if (isUr) "پن (قریب ${it.nameUr})" else "Pinned near ${it.nameEn}" } ?: "Custom Pin",
            district = nearest?.district ?: "Pakistan",
            province = nearest?.province?.displayNameEn ?: "",
            lat = clampedLat,
            lng = clampedLng,
            isCustomPin = true,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TibbGradientScaffoldBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Map, contentDescription = null, tint = c.accent)
                            Column {
                                Text(
                                    text = if (isUr) "پاکستان آف لائن نقشہ اور مقام" else "Pakistan Offline Map & Locations",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = c.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (isUr) "${allLocations.size} مقامات · تمام صوبے (انٹرنیٹ کے بغیر)" else "${allLocations.size} verified locations · Tap map or search",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.safeGreen,
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp).testTag("pk_map_close_btn"),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Map", tint = c.muted)
                        }
                    }

                    // Search Bar + GPS button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    if (isUr) "شہر، ضلع، قصبہ یا علاقہ تلاش کریں..." else "Search city, district, town, locality...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("pk_location_search"),
                        )
                        Box(
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.accentSoft)
                                .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        waitingForGpsSnap = true
                                        onRequestGps()
                                    } else {
                                        locPermLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("pk_map_gps_btn"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = "Use GPS", tint = c.accent, modifier = Modifier.size(18.dp))
                                Text(if (gpsLocating) "..." else "GPS", style = MaterialTheme.typography.labelMedium, color = c.ink)
                            }
                        }
                    }

                    // Province filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChipPill(
                            label = if (isUr) "سب (${allLocations.size})" else "All Pakistan (${allLocations.size})",
                            selected = selectedProvince == null,
                            testTag = "pk_prov_all",
                            onClick = { selectedProvince = null },
                        )
                        PakistanProvince.entries.forEach { prov ->
                            FilterChipPill(
                                label = if (isUr) prov.displayNameUr else prov.displayNameEn,
                                selected = selectedProvince == prov,
                                testTag = "pk_prov_${prov.shortCode.lowercase()}",
                                onClick = {
                                    selectedProvince = if (selectedProvince == prov) null else prov
                                },
                            )
                        }
                    }

                    // Category filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChipPill(
                            label = if (isUr) "تمام اقسام" else "All Types",
                            selected = selectedCategory == null,
                            testTag = "pk_cat_all",
                            onClick = { selectedCategory = null },
                        )
                        LocationCategory.entries.forEach { cat ->
                            FilterChipPill(
                                label = if (isUr) cat.labelUr else cat.labelEn,
                                selected = selectedCategory == cat,
                                testTag = "pk_cat_${cat.name.lowercase()}",
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                            )
                        }
                    }

                    // Interactive Offline Map + Controls Overlay
                    Box(modifier = Modifier.fillMaxWidth().height(235.dp)) {
                        PakistanOfflineMapCanvas(
                            allLocations = allLocations,
                            filteredLocations = filteredLocations,
                            selectedLat = activeSelection.lat,
                            selectedLng = activeSelection.lng,
                            selectedProvince = selectedProvince,
                            zoom = mapZoom,
                            pan = mapPan,
                            onTransformChange = { z, p ->
                                mapZoom = z
                                mapPan = p
                            },
                            onSelectExistingLocation = { loc -> selectNamedLocation(loc) },
                            onPinTappedAt = { lat, lng -> setCustomPinCoords(lat, lng) },
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Zoom Controls (Top Right)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surface.copy(alpha = 0.90f))
                                .border(1.dp, c.border, RoundedCornerShape(14.dp)),
                        ) {
                            IconButton(
                                onClick = { mapZoom = (mapZoom * 1.45f).coerceIn(1f, 14f) },
                                modifier = Modifier.size(48.dp).testTag("pk_map_zoom_in"),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = c.ink)
                            }
                            IconButton(
                                onClick = {
                                    val nextZ = (mapZoom / 1.45f).coerceIn(1f, 14f)
                                    mapZoom = nextZ
                                    if (nextZ <= 1.05f) mapPan = Offset.Zero
                                },
                                modifier = Modifier.size(48.dp).testTag("pk_map_zoom_out"),
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = c.ink)
                            }
                            IconButton(
                                onClick = { focusOnCoords(activeSelection.lat, activeSelection.lng, 1.05f) },
                                modifier = Modifier.size(48.dp).testTag("pk_map_reset_view"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset Map View", tint = c.accent)
                            }
                        }

                        // Marker Nudge Controls (Bottom Left)
                        val nudgeStep = (0.18 / mapZoom.toDouble()).coerceIn(0.01, 0.2)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surface.copy(alpha = 0.90f))
                                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { setCustomPinCoords(activeSelection.lat, activeSelection.lng - nudgeStep) },
                                modifier = Modifier.size(48.dp).testTag("pk_pin_nudge_west"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Nudge Pin West", tint = c.ink)
                            }
                            IconButton(
                                onClick = { setCustomPinCoords(activeSelection.lat + nudgeStep, activeSelection.lng) },
                                modifier = Modifier.size(48.dp).testTag("pk_pin_nudge_north"),
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Nudge Pin North", tint = c.ink)
                            }
                            IconButton(
                                onClick = { setCustomPinCoords(activeSelection.lat - nudgeStep, activeSelection.lng) },
                                modifier = Modifier.size(48.dp).testTag("pk_pin_nudge_south"),
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Nudge Pin South", tint = c.ink)
                            }
                            IconButton(
                                onClick = { setCustomPinCoords(activeSelection.lat, activeSelection.lng + nudgeStep) },
                                modifier = Modifier.size(48.dp).testTag("pk_pin_nudge_east"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Nudge Pin East", tint = c.ink)
                            }
                        }

                        // Zoom readout pill (Top Left)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surface.copy(alpha = 0.88f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fx · Tap map to pin", mapZoom),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.muted,
                            )
                        }
                    }

                    // Active Selection Summary Card
                    val nearestFac = remember(activeSelection) {
                        triageEngine.sortFacilities(
                            tier = TriageTier.AMBER,
                            isPediatric = false,
                            userCoords = GeoCoords(activeSelection.lat, activeSelection.lng),
                            districtFilter = activeSelection.district,
                        ).first.firstOrNull()
                    }
                    TibbGlassCard(padding = 10.dp, borderColor = c.accent.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = c.dangerRed, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${activeSelection.name} · ${activeSelection.district}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = c.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = buildString {
                                        if (activeSelection.province.isNotBlank()) append("${activeSelection.province} · ")
                                        append(String.format(Locale.US, "%.4f°N, %.4f°E", activeSelection.lat, activeSelection.lng))
                                        nearestFac?.distanceKm?.let { append(" · Nearest clinic: $it km") }
                                    },
                                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                    color = c.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Search Results List
                    Text(
                        text = if (isUr) "مقامات کی فہرست (${filteredLocations.size} نتائج)" else "Locations (${filteredLocations.size} matching)",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.muted,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.surfaceMuted.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredLocations.take(80), key = { it.id }) { loc ->
                            val isSelected = !activeSelection.isCustomPin &&
                                kotlin.math.abs(activeSelection.lat - loc.lat) < 0.0005 &&
                                kotlin.math.abs(activeSelection.lng - loc.lng) < 0.0005
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) c.accentSoft else c.surface)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) c.accent else c.border,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { selectNamedLocation(loc) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("pk_loc_item_${loc.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUr) loc.nameUr else loc.nameEn,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = c.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${loc.district} · ${loc.province.shortCode} · ${if (isUr) loc.category.labelUr else loc.category.labelEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = c.accent, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        text = String.format(Locale.US, "%.2f, %.2f", loc.lat, loc.lng),
                                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                        color = c.muted,
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TibbSecondaryPillButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pk_map_cancel_btn"),
                        ) {
                            Text(if (isUr) "منسوخ" else "Cancel")
                        }
                        TibbPrimaryPillButton(
                            onClick = { onConfirm(activeSelection) },
                            modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("confirm_location_btn"),
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isUr) "مقام کی تصدیق کریں" else "Confirm Location")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val c = LocalTibbColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.ctaBg else c.surfaceMuted)
            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) c.ctaInk else c.ink,
            maxLines = 1,
        )
    }
}
