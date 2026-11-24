package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppLocale
import com.example.data.model.DailyWellnessLog
import com.example.data.model.EpisodeVitals
import com.example.data.model.MeasurementMethod
import com.example.data.model.PatientProfile
import com.example.data.model.StayHomeVerdict
import com.example.data.model.SugarMode
import com.example.engine.AndroidSensorsManager
import com.example.engine.AudioFeedbackManager
import com.example.engine.HealthMath
import com.example.engine.HelpTopic
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.CyclicSighingPacer
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.StabilizationTimers
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun VitalsEstimatorScreen(
    locale: AppLocale,
    activePerson: PatientProfile?,
    todayLog: DailyWellnessLog?,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    onLogVitals: (EpisodeVitals, String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    var subTab by remember { mutableIntStateOf(0) } // 0 = BP & Sugar, 1 = Breath & Pulse Tap, 2 = Camera Finger Pulse

    // Subtab 0: BP & Sugar
    var bpSysText by remember { mutableStateOf(todayLog?.lastBpSystolic?.toString().orEmpty()) }
    var bpDiaText by remember { mutableStateOf(todayLog?.lastBpDiastolic?.toString().orEmpty()) }
    var sugarText by remember { mutableStateOf(todayLog?.lastSugarMgDl?.toString().orEmpty()) }
    var sugarMode by remember { mutableStateOf(SugarMode.RANDOM) }
    var hasOrganWarning by remember { mutableStateOf(false) }
    var hasShockSigns by remember { mutableStateOf(false) }
    var isConscious by remember { mutableStateOf(true) }

    val bpSys = bpSysText.toIntOrNull()
    val bpDia = bpDiaText.toIntOrNull()
    val sugarVal = sugarText.toIntOrNull()
    val bpInterp = remember(bpSys, bpDia) { triageEngine.interpretBloodPressure(bpSys, bpDia) }
    val sugarInterp = remember(sugarVal, sugarMode) { triageEngine.interpretBloodSugar(sugarVal, sugarMode) }
    val stayHome = remember(bpSys, bpDia, sugarVal, hasOrganWarning, hasShockSigns, isConscious) {
        triageEngine.evaluateStayHomeVsHospital(bpSys, bpDia, sugarVal, hasOrganWarning, hasShockSigns, isConscious)
    }

    // Subtab 1: 60s Tap Counter
    var tapIsBreath by remember { mutableStateOf(false) }
    var tapRunning by remember { mutableStateOf(false) }
    var tapElapsedSec by remember { mutableIntStateOf(0) }
    var tapCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(tapRunning) {
        while (tapRunning && tapElapsedSec < 60) {
            delay(1000L)
            tapElapsedSec++
        }
        if (tapElapsedSec >= 60) tapRunning = false
    }
    val liveTapRate = if (tapElapsedSec >= 3 && tapCount > 0) {
        ((tapCount.toDouble() / tapElapsedSec.toDouble()) * 60.0).roundToInt()
    } else null
    val imciThreshold = remember(activePerson) {
        triageEngine.getFastBreathingThreshold(activePerson?.ageMonths ?: 300)
    }

    // Subtab 2: CameraX PPG Pulse
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var ppgScanning by remember { mutableStateOf(false) }
    var ppgElapsedSec by remember { mutableIntStateOf(0) }
    val ppgFrames = remember { mutableStateListOf<HealthMath.PpgSampleFrame>() }
    val ppgSparkline = remember { mutableStateListOf<Float>() }
    var liveFingerStatus by remember { mutableStateOf(HealthMath.PpgFrameStatus.NO_FINGER) }
    var ppgResult by remember { mutableStateOf<HealthMath.PpgAnalysisOutput?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    LaunchedEffect(ppgScanning) {
        if (!ppgScanning) return@LaunchedEffect
        ppgElapsedSec = 0
        while (ppgScanning && ppgElapsedSec < 30) {
            delay(1000L)
            ppgElapsedSec++
        }
        if (ppgScanning) {
            ppgScanning = false
            ppgResult = HealthMath.analyzePpgSamples(ppgFrames.toList())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero Summary Card
        TibbGlassCard(padding = 16.dp, borderColor = c.accent.copy(alpha = 0.4f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.vitalsTitle, style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge, color = c.ink)
                    Text(s.vitalsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val lastHr = (ppgResult as? HealthMath.PpgAnalysisOutput.Ok)?.bpm ?: todayLog?.restingHr
                    Text(
                        text = lastHr?.let { "$it bpm" } ?: "-- bpm",
                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold),
                        color = c.accent,
                    )
                    Text("Pulse Estimate", style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }

        // Subtab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                0 to s.vitalsTabBpSugar,
                1 to s.vitalsTabTap,
                2 to s.vitalsTabCamera,
            ).forEach { (idx, lbl) ->
                val sel = subTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) c.ctaBg else Color.Transparent)
                        .clickable {
                            if (idx != 2) ppgScanning = false
                            subTab = idx
                        }
                        .padding(vertical = 9.dp)
                        .testTag("vitals_subtab_$idx"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink, maxLines = 1)
                }
            }
        }

        when (subTab) {
            0 -> {
                TibbGlassCard(padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = s.vitalsTabBpSugar,
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink,
                            )
                            SectionHelpButton(topic = HelpTopic.VITALS_BP_SUGAR, locale = locale)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = bpSysText,
                                onValueChange = { bpSysText = it.filter(Char::isDigit) },
                                label = { Text("Upper BP (Sys)") },
                                supportingText = { Text(s.upperBpHint) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("vitals_bp_sys_input"),
                            )
                            OutlinedTextField(
                                value = bpDiaText,
                                onValueChange = { bpDiaText = it.filter(Char::isDigit) },
                                label = { Text("Lower BP (Dia)") },
                                supportingText = { Text(s.lowerBpHint) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("vitals_bp_dia_input"),
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(SugarMode.FASTING to s.sugarTypeFasting, SugarMode.RANDOM to s.sugarTypeRandom).forEach { (m, lbl) ->
                                val sel = sugarMode == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { sugarMode = m }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = sugarText,
                            onValueChange = { sugarText = it.filter(Char::isDigit) },
                            label = { Text(s.sugarLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("vitals_sugar_input"),
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasOrganWarning = !hasOrganWarning }) {
                                Checkbox(checked = hasOrganWarning, onCheckedChange = { hasOrganWarning = it })
                                Text("Chest pain, vision blur, numbness, or severe headache", style = MaterialTheme.typography.bodySmall, color = c.ink)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasShockSigns = !hasShockSigns }) {
                                Checkbox(checked = hasShockSigns, onCheckedChange = { hasShockSigns = it })
                                Text("Cold/clammy skin, dizziness, or fainting", style = MaterialTheme.typography.bodySmall, color = c.ink)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isConscious = !isConscious }) {
                                Checkbox(checked = isConscious, onCheckedChange = { isConscious = it })
                                Text("Awake and able to swallow safely", style = MaterialTheme.typography.bodySmall, color = c.ink)
                            }
                        }

                        if (bpSys != null && bpDia != null) {
                            Text(if (isUr) bpInterp.labelUr else bpInterp.labelEn, style = MaterialTheme.typography.titleMedium, color = c.accent)
                            Text(if (isUr) bpInterp.guidanceUr else bpInterp.guidanceEn, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                        if (sugarVal != null) {
                            Text(if (isUr) sugarInterp.labelUr else sugarInterp.labelEn, style = MaterialTheme.typography.titleMedium, color = c.accent)
                            Text(if (isUr) sugarInterp.guidanceUr else sugarInterp.guidanceEn, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }

                        val verdictColor = when (stayHome.verdict) {
                            StayHomeVerdict.EMERGENCY_1122 -> c.dangerRed
                            StayHomeVerdict.RECHECK_AND_CLINIC -> c.amber
                            StayHomeVerdict.STAY_HOME -> c.safeGreen
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(verdictColor.copy(alpha = 0.14f))
                                .border(1.dp, verdictColor, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (isUr) stayHome.title_ur else stayHome.title_en, style = MaterialTheme.typography.titleMedium, color = verdictColor)
                                val steps = if (isUr) stayHome.actionSteps_ur else stayHome.actionSteps_en
                                steps.forEach { st -> Text("• $st", style = MaterialTheme.typography.bodySmall, color = c.ink) }
                            }
                        }

                        TibbPrimaryPillButton(
                            enabled = (bpSys != null && bpDia != null) || sugarVal != null,
                            onClick = {
                                onLogVitals(
                                    EpisodeVitals(
                                        bpSystolic = bpSys,
                                        bpDiastolic = bpDia,
                                        bloodGlucoseMgDl = sugarVal,
                                        sugarMode = sugarVal?.let { sugarMode },
                                        measurementMethod = MeasurementMethod.MANUAL,
                                    ),
                                    "Manual BP & Sugar Reading",
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("vitals_save_bp_sugar_btn"),
                        ) {
                            Text(s.saveVitalsBtn)
                        }
                    }
                }

                if ((bpSys ?: 0) >= 130 || (bpDia ?: 0) >= 85) {
                    CyclicSighingPacer(locale = locale, audioManager = audioManager)
                }
                if (sugarVal != null && sugarVal < 70) {
                    StabilizationTimers(locale = locale)
                }
            }

            1 -> {
                TibbGlassCard(padding = 18.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.vitalsTabTap, style = MaterialTheme.typography.titleMedium, color = c.ink)
                            SectionHelpButton(topic = HelpTopic.VITALS_TAP, locale = locale)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(false to s.pulseCounterLabel, true to s.breathCounterLabel).forEach { (isBr, lbl) ->
                                val sel = tapIsBreath == isBr
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable {
                                            tapIsBreath = isBr
                                            tapRunning = false
                                            tapElapsedSec = 0
                                            tapCount = 0
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                                }
                            }
                        }

                        Text("${tapElapsedSec}s / 60s · Taps: $tapCount", style = TabularNumberStyle, color = c.accent)

                        Box(
                            modifier = Modifier
                                .size(156.dp)
                                .clip(CircleShape)
                                .background(c.accent.copy(alpha = 0.22f))
                                .border(2.dp, c.accent, CircleShape)
                                .clickable {
                                    if (!tapRunning) tapRunning = true
                                    tapCount++
                                }
                                .testTag("vitals_tap_circle"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = c.accent, modifier = Modifier.size(28.dp))
                                Text(
                                    text = liveTapRate?.let { "$it /min" } ?: "TAP",
                                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold),
                                    color = c.ink,
                                )
                                Text(s.tapCircleHint, style = MaterialTheme.typography.labelSmall, color = c.muted)
                            }
                        }

                        if (tapIsBreath && liveTapRate != null && liveTapRate >= imciThreshold) {
                            Text(
                                text = "WHO Fast-Breathing Alert (>= $imciThreshold/min for patient age)",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.dangerRed,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TibbSecondaryPillButton(
                                onClick = {
                                    tapRunning = false
                                    tapElapsedSec = 0
                                    tapCount = 0
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.resetCounterBtn)
                            }
                            TibbPrimaryPillButton(
                                enabled = liveTapRate != null,
                                onClick = {
                                    liveTapRate?.let { rate ->
                                        val vitals = if (tapIsBreath) {
                                            EpisodeVitals(respiratoryRate = rate, measurementMethod = MeasurementMethod.TAP_TIMER)
                                        } else {
                                            EpisodeVitals(heartRate = rate, measurementMethod = MeasurementMethod.TAP_TIMER)
                                        }
                                        onLogVitals(vitals, if (tapIsBreath) "60s Breath Rate" else "60s Pulse Counter")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("vitals_save_tap_btn"),
                            ) {
                                Text(s.saveVitalsBtn)
                            }
                        }
                    }
                }
            }

            2 -> {
                TibbGlassCard(padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.vitalsTabCamera, style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                            SectionHelpButton(topic = HelpTopic.VITALS_CAMERA_PULSE, locale = locale)
                        }
                        Text(s.cameraDisclaimer, style = MaterialTheme.typography.bodySmall, color = c.amber)

                        if (ppgScanning && hasCameraPerm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black),
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        val future = ProcessCameraProvider.getInstance(ctx)
                                        future.addListener({
                                            val provider = future.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            val analysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()
                                            val startNs = System.nanoTime()
                                            analysis.setAnalyzer(cameraExecutor) { proxy: ImageProxy ->
                                                val (r, g, b) = AndroidSensorsManager.extractCenterRgbFromYuv(proxy)
                                                val tSec = (System.nanoTime() - startNs) / 1_000_000_000.0
                                                val status = HealthMath.assessPpgFrameQuality(r, g, b)
                                                liveFingerStatus = status
                                                if (ppgFrames.size < 1200) {
                                                    ppgFrames.add(HealthMath.PpgSampleFrame(tSec, r, g, b))
                                                    ppgSparkline.add(r.toFloat())
                                                    if (ppgSparkline.size > 90) ppgSparkline.removeAt(0)
                                                }
                                                proxy.close()
                                            }
                                            runCatching {
                                                provider.unbindAll()
                                                val cam = provider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview,
                                                    analysis,
                                                )
                                                cam.cameraControl.enableTorch(true)
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "${liveFingerStatus.name.replace('_', ' ')} · ${ppgElapsedSec}s/30s",
                                        style = TabularNumberStyle.copy(color = Color.White),
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { ppgElapsedSec / 30f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                color = c.accent,
                                trackColor = c.surfaceMuted,
                            )

                            // Live PPG Waveform Sparkline
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(c.surfaceMuted),
                            ) {
                                if (ppgSparkline.size >= 4) {
                                    val minV = ppgSparkline.minOrNull() ?: 0f
                                    val maxV = (ppgSparkline.maxOrNull() ?: 1f).coerceAtLeast(minV + 0.5f)
                                    val path = Path()
                                    ppgSparkline.forEachIndexed { idx, v ->
                                        val x = idx.toFloat() / (ppgSparkline.size - 1).coerceAtLeast(1) * size.width
                                        val normY = 1f - ((v - minV) / (maxV - minV)).coerceIn(0f, 1f)
                                        val y = 8f + normY * (size.height - 16f)
                                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(path, color = c.accent, style = Stroke(width = 2.5.dp.toPx()))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TibbPrimaryPillButton(
                                onClick = {
                                    if (!hasCameraPerm) {
                                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                    } else if (ppgScanning) {
                                        ppgScanning = false
                                        ppgResult = HealthMath.analyzePpgSamples(ppgFrames.toList())
                                    } else {
                                        ppgFrames.clear()
                                        ppgSparkline.clear()
                                        ppgResult = null
                                        ppgScanning = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("vitals_camera_scan_btn"),
                            ) {
                                Icon(if (ppgScanning) Icons.Rounded.Stop else Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (ppgScanning) s.stopCameraScanBtn else s.startCameraScanBtn)
                            }
                        }

                        when (val out = ppgResult) {
                            is HealthMath.PpgAnalysisOutput.Ok -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(c.safeGreen.copy(alpha = 0.14f))
                                        .border(1.dp, c.safeGreen, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = c.safeGreen)
                                            Text("Pulse: ${out.bpm} BPM · SQI ${out.sqi}% · HRV ${out.hrvMs} ms", style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.ink)
                                        }
                                        TibbPrimaryPillButton(
                                            onClick = {
                                                onLogVitals(
                                                    EpisodeVitals(
                                                        heartRate = out.bpm,
                                                        signalQualityIndex = out.sqi,
                                                        measurementMethod = MeasurementMethod.CAMERA_PPG,
                                                    ),
                                                    "Camera Optical PPG Pulse (${out.sqi}% SQI)",
                                                )
                                            },
                                            modifier = Modifier.testTag("vitals_save_camera_btn"),
                                        ) {
                                            Text(s.saveVitalsBtn)
                                        }
                                    }
                                }
                            }
                            is HealthMath.PpgAnalysisOutput.Rejected -> {
                                Text("${out.reason}: ${out.message}", style = MaterialTheme.typography.bodySmall, color = c.dangerRed)
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
