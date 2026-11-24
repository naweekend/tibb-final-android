package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.DailyDoctorCheckIn
import com.example.data.model.DailyWellnessLog
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.StoredAlarm
import com.example.data.model.TriageTier
import com.example.data.model.UnitSystem
import com.example.engine.HealthMath
import com.example.engine.HelpTopic
import com.example.engine.RemediesAndPlans
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.SectionHelpButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbEspressoCard
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbMistCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.TibbThoughtGlowCard
import com.example.ui.theme.urduTextStyle
import com.example.viewmodel.MainNavTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeHubDashboardScreen(
    locale: AppLocale,
    unitSystem: UnitSystem,
    profiles: List<PatientProfile>,
    activePerson: PatientProfile?,
    todayLog: DailyWellnessLog?,
    alarms: List<StoredAlarm>,
    triageEngine: TriageEngine,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    sensorStepsActive: Boolean,
    stepSensitivityHigh: Boolean,
    onSelectPerson: (String) -> Unit,
    onAddPerson: () -> Unit,
    onEditPerson: () -> Unit,
    onUpdateWellness: (DailyWellnessLog) -> Unit,
    onSubmitCheckIn: (DailyDoctorCheckIn) -> Unit,
    onToggleStepSensor: (Boolean?) -> Unit,
    onMarkAlarmTaken: (String) -> Unit,
    onRequestGps: () -> Unit,
    onOpenDialer: (String) -> Unit,
    onNavigateTab: (MainNavTab) -> Unit,
    onStartStructuredCheckup: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    if (activePerson == null || todayLog == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            TibbGlassCard(padding = 24.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(s.onboardingTitle, style = MaterialTheme.typography.titleLarge, color = c.ink)
                    Text(s.onboardingSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    TibbPrimaryPillButton(onClick = onAddPerson, modifier = Modifier.testTag("empty_add_person_btn")) {
                        Text(s.addPersonBtn)
                    }
                }
            }
        }
        return
    }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val personAlarms = remember(alarms, activePerson.id) {
        alarms.filter { it.patientId == activePerson.id && it.active }
    }
    val dueTodayAlarms = remember(personAlarms, todayKey) {
        personAlarms.filter { it.lastTakenDate != todayKey }
    }
    val readiness = remember(todayLog, personAlarms, dueTodayAlarms) {
        HealthMath.computeDailyReadinessScore(
            waterMl = todayLog.waterMl,
            waterGoalMl = todayLog.waterGoalMl,
            steps = todayLog.steps,
            stepGoal = todayLog.stepGoal,
            sleepHours = todayLog.sleepHours,
            activeAlarmsCount = personAlarms.size,
            dueAlarmsCount = dueTodayAlarms.size,
        )
    }
    val bmiCheck = remember(activePerson) {
        RemediesAndPlans.computeBmiAssessment(activePerson.weightKg, activePerson.heightCm, activePerson.ageMonths)
    }

    var insightTab by remember { mutableIntStateOf(0) } // 0 = Reminders, 1 = Recommended
    var showCheckInDialog by remember { mutableStateOf(false) }
    var showBellDialog by remember { mutableStateOf(false) }
    var showScoreBreakdown by remember { mutableStateOf(false) }
    var showSleepEditor by remember { mutableStateOf(false) }
    var bedTimeText by remember { mutableStateOf("23:00") }
    var wakeTimeText by remember { mutableStateOf("06:30") }

    // Hospital Lookup state
    var selectedDistrict by remember(activePerson.id) {
        mutableStateOf(activePerson.frequentPlaces.firstOrNull()?.district ?: "")
    }
    var activeCoords by remember(activePerson.id, userGpsCoords) {
        val plc = activePerson.frequentPlaces.firstOrNull()
        mutableStateOf(
            userGpsCoords ?: if (plc?.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
        )
    }
    var districtMenuOpen by remember { mutableStateOf(false) }
    val districts = remember { triageEngine.getAvailableDistricts() }
    val (facilities, needsLocation) = remember(activeCoords, selectedDistrict) {
        triageEngine.sortFacilities(
            tier = TriageTier.AMBER,
            isPediatric = activePerson.ageMonths <= 60,
            userCoords = activeCoords,
            districtFilter = selectedDistrict,
        )
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) onRequestGps()
    }

    val motionPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleStepSensor(null)
    }

    val activeGoalsCount = listOf(
        todayLog.waterMl >= todayLog.waterGoalMl,
        todayLog.steps >= todayLog.stepGoal,
        todayLog.mealsChecked.size >= 2,
        todayLog.workoutDone,
        personAlarms.isNotEmpty() && dueTodayAlarms.isEmpty(),
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Greeting Header + Avatar + BMI Badge + Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.25f))
                        .border(1.dp, c.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activePerson.name.trim().take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.ink,
                    )
                }
                Column {
                    Text(
                        text = if (isUr) "السلام علیکم، ${activePerson.name}" else "Assalam-o-Alaikum, ${activePerson.name}",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (bmiCheck.bmi != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(c.accentSoft)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "BMI ${bmiCheck.bmi} · ${bmiCheck.category}",
                                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                    color = c.accent,
                                )
                            }
                        }
                        Text(
                            text = HealthMath.formatWeightDisplay(activePerson.weightKg, unitSystem),
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                            color = c.muted,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onEditPerson,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c.surface)
                        .border(1.dp, c.border, CircleShape)
                        .testTag("home_edit_person_btn"),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = s.editProfileBtn, tint = c.ink, modifier = Modifier.size(18.dp))
                }
                Box {
                    IconButton(
                        onClick = { showBellDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.surface)
                            .border(1.dp, c.border, CircleShape)
                            .testTag("home_bell_btn"),
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Due Reminders", tint = c.ink, modifier = Modifier.size(20.dp))
                    }
                    if (dueTodayAlarms.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(c.dangerRed),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${dueTodayAlarms.size}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }

        // Daily Insights Hero & Readiness Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isUr) "آج کے صحت اشارے" else "Daily Insights",
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.headlineMedium) else MaterialTheme.typography.headlineMedium,
                    color = c.ink,
                )
                SectionHelpButton(topic = HelpTopic.DAILY_INSIGHTS_SCORE, locale = locale)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.surface)
                    .border(1.dp, c.accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                    .clickable { showScoreBreakdown = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("home_readiness_pill"),
            ) {
                Text(
                    text = "Score ${readiness.score}%",
                    style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                    color = c.accent,
                )
            }
        }

        // [Reminders | Recommended] Switcher
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
                0 to (if (isUr) "یاد دہانیاں (${dueTodayAlarms.size})" else "Reminders (${dueTodayAlarms.size})"),
                1 to (if (isUr) "تجویز کردہ" else "Recommended"),
            ).forEach { (idx, label) ->
                val sel = insightTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) c.ctaBg else Color.Transparent)
                        .clickable { insightTab = idx }
                        .padding(vertical = 9.dp)
                        .testTag(if (idx == 0) "home_tab_reminders" else "home_tab_recommended"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) c.ctaInk else c.ink)
                }
            }
        }

        // Featured Misty Sage Card
        TibbMistCard(padding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (insightTab == 0) {
                        dueTodayAlarms.firstOrNull()?.let {
                            if (isUr) "اگلی دوا: ${it.medicineLabel}" else "Due Today: ${it.medicineLabel} · ${HealthMath.formatTime12h(it.time24h)}"
                        } ?: if (isUr) "آج کی تمام یاد دہانیاں مکمل ہیں" else "All active reminders completed — Ready for a calm health checkup"
                    } else {
                        if (isUr) "ڈاکٹر معائنہ یا غذا و ورزش پلان" else "Personalized Desi Meal Plan & WHO Checkup"
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                    color = c.mistInk,
                )
                Text(
                    text = if (insightTab == 0) {
                        if (isUr) "فوری علامات چیک کریں یا مقررہ دوا پر نشان لگائیں۔"
                        else "Answer one symptom question at a time or tap Mark Taken on due medicines."
                    } else {
                        if (isUr) bmiCheck.adviceUr else bmiCheck.adviceEn
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                    color = c.mistInk.copy(alpha = 0.85f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TibbPrimaryPillButton(
                        onClick = {
                            if (insightTab == 0 && dueTodayAlarms.isNotEmpty()) {
                                onMarkAlarmTaken(dueTodayAlarms.first().id)
                            } else {
                                onStartStructuredCheckup()
                            }
                        },
                        containerColor = c.mistInk,
                        contentColor = Color(0xFFF6F4EC),
                        modifier = Modifier.testTag("home_featured_cta"),
                    ) {
                        Text(
                            if (insightTab == 0 && dueTodayAlarms.isNotEmpty()) s.alarmsMarkTaken
                            else if (insightTab == 0) s.structuredCheckupBtn
                            else s.navCoach
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    if (insightTab == 1) {
                        TibbSecondaryPillButton(
                            onClick = { onNavigateTab(MainNavTab.COACH) },
                            borderColor = c.mistInk.copy(alpha = 0.4f),
                            contentColor = c.mistInk,
                        ) {
                            Text(s.navCoach)
                        }
                    }
                }
            }
        }

        // 2x2 Bento Grid (Adaptive)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val twoCols = maxWidth >= 340.dp
            if (twoCols) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StepsBentoCard(
                            todayLog = todayLog,
                            sensorStepsActive = sensorStepsActive,
                            stepSensitivityHigh = stepSensitivityHigh,
                            locale = locale,
                            onToggleSensor = {
                                if (!sensorStepsActive && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACTIVITY_RECOGNITION,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) onToggleStepSensor(null)
                                    else motionPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                } else {
                                    onToggleStepSensor(null)
                                }
                            },
                            onToggleSensitivity = { onToggleStepSensor(!stepSensitivityHigh) },
                            onAddManualSteps = { onUpdateWellness(todayLog.copy(steps = todayLog.steps + 500)) },
                            modifier = Modifier.weight(1f),
                        )
                        ProgressGoalsBentoCard(
                            met = activeGoalsCount,
                            total = 5,
                            locale = locale,
                            onOpenCoach = { onNavigateTab(MainNavTab.COACH) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        CheckInBentoCard(
                            todayLog = todayLog,
                            locale = locale,
                            onClick = { showCheckInDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                        WellnessBentoCard(
                            todayLog = todayLog,
                            locale = locale,
                            onEditSleep = { showSleepEditor = !showSleepEditor },
                            onOpenVitals = { onNavigateTab(MainNavTab.VITALS) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepsBentoCard(
                        todayLog = todayLog,
                        sensorStepsActive = sensorStepsActive,
                        stepSensitivityHigh = stepSensitivityHigh,
                        locale = locale,
                        onToggleSensor = { onToggleStepSensor(null) },
                        onToggleSensitivity = { onToggleStepSensor(!stepSensitivityHigh) },
                        onAddManualSteps = { onUpdateWellness(todayLog.copy(steps = todayLog.steps + 500)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProgressGoalsBentoCard(activeGoalsCount, 5, locale, { onNavigateTab(MainNavTab.COACH) }, Modifier.fillMaxWidth())
                    CheckInBentoCard(todayLog, locale, { showCheckInDialog = true }, Modifier.fillMaxWidth())
                    WellnessBentoCard(todayLog, locale, { showSleepEditor = !showSleepEditor }, { onNavigateTab(MainNavTab.VITALS) }, Modifier.fillMaxWidth())
                }
            }
        }

        // Inline Midnight-Safe Sleep Editor
        if (showSleepEditor) {
            TibbGlassCard(padding = 16.dp, borderColor = c.accent.copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = c.accent)
                        Text("Bedtime & Wake-Up Calculator (Midnight-Safe)", style = MaterialTheme.typography.titleMedium, color = c.ink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bedTimeText,
                            onValueChange = { bedTimeText = it },
                            label = { Text("Bed (HH:MM 24h)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = wakeTimeText,
                            onValueChange = { wakeTimeText = it },
                            label = { Text("Wake (HH:MM 24h)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val hrs = HealthMath.computeSleepDurationHours(bedTimeText, wakeTimeText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Computed: $hrs hrs", style = TabularNumberStyle, color = c.accent)
                        TibbPrimaryPillButton(
                            onClick = {
                                onUpdateWellness(todayLog.copy(sleepHours = hrs))
                                showSleepEditor = false
                            },
                            modifier = Modifier.testTag("save_sleep_duration_btn"),
                        ) {
                            Text("Save Sleep")
                        }
                    }
                }
            }
        }

        // Hydration Tracker Card
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = c.accent)
                        Text(if (isUr) "پانی و نمکیات (Hydration)" else "Daily Hydration", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        SectionHelpButton(topic = HelpTopic.HYDRATION, locale = locale)
                    }
                    Text(
                        text = "${todayLog.waterMl} / ${todayLog.waterGoalMl} ml",
                        style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                        color = c.accent,
                    )
                }
                LinearProgressIndicator(
                    progress = { (todayLog.waterMl.toFloat() / todayLog.waterGoalMl.coerceAtLeast(500)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                    color = c.accent,
                    trackColor = c.surfaceMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TibbSecondaryPillButton(
                        onClick = { onUpdateWellness(todayLog.copy(waterMl = (todayLog.waterMl - 250).coerceAtLeast(0))) },
                        modifier = Modifier.testTag("water_minus_250"),
                    ) { Text("-250 ml") }
                    TibbPrimaryPillButton(
                        onClick = { onUpdateWellness(todayLog.copy(waterMl = todayLog.waterMl + 250)) },
                        modifier = Modifier.weight(1f).testTag("water_plus_250"),
                    ) { Text("+250 ml") }
                    TibbSecondaryPillButton(
                        onClick = { onUpdateWellness(todayLog.copy(waterMl = todayLog.waterMl + 500)) },
                        modifier = Modifier.testTag("water_plus_500"),
                    ) { Text("+500 ml") }
                }
            }
        }

        // Verified Hospitals Espresso Card
        TibbEspressoCard(padding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = Color(0xFF95CEA4))
                        Text(s.nearestFacilitiesTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFF2EFE4), modifier = Modifier.weight(1f, fill = false))
                        SectionHelpButton(topic = HelpTopic.NEAREST_HOSPITALS, locale = locale, onDarkSurface = true)
                    }
                    TibbPrimaryPillButton(
                        onClick = { onOpenDialer("1122") },
                        containerColor = c.dangerRed,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("home_call_1122"),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("1122")
                    }
                }

                // Saved Places + GPS + District Selector
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activePerson.frequentPlaces.forEach { plc ->
                        val sel = selectedDistrict.equals(plc.district, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) Color(0xFF95CEA4) else Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    selectedDistrict = plc.district
                                    activeCoords = if (plc.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                "${plc.label} (${plc.district})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) Color(0xFF102620) else Color(0xFFF2EFE4),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) onRequestGps()
                                else locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("home_hospitals_gps_btn"),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFF95CEA4), modifier = Modifier.size(14.dp))
                            Text(if (gpsLocating) "GPS..." else "Use GPS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF2EFE4))
                        }
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { districtMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("home_district_dropdown"),
                        ) {
                            Text(
                                selectedDistrict.ifBlank { s.allDistrictsOption },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2EFE4),
                            )
                        }
                        DropdownMenu(expanded = districtMenuOpen, onDismissRequest = { districtMenuOpen = false }) {
                            districts.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        selectedDistrict = d
                                        activeCoords = null
                                        districtMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (needsLocation) {
                    Text(
                        text = s.locationRequiredBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB6C9BE),
                    )
                } else {
                    facilities.take(3).forEach { fac ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isUr) fac.name_ur else fac.name_en, style = MaterialTheme.typography.labelLarge, color = Color(0xFFF2EFE4))
                                Text(
                                    text = "${fac.tehsil}, ${fac.district}" + (fac.distanceKm?.let { " · $it km" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB6C9BE),
                                )
                            }
                            TibbSecondaryPillButton(
                                onClick = { onOpenDialer(fac.phone) },
                                borderColor = Color(0xFF95CEA4),
                                contentColor = Color(0xFF95CEA4),
                            ) {
                                Text(s.callBtn)
                            }
                        }
                    }
                }
            }
        }

        // Family Pulse Strip & Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            profiles.forEach { p ->
                val sel = p.id == activePerson.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) c.accent.copy(alpha = 0.22f) else c.surface)
                        .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(16.dp))
                        .clickable { onSelectPerson(p.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(p.name, style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Text(if (isUr) p.relationship.labelUr else p.relationship.labelEn, style = MaterialTheme.typography.labelSmall, color = c.muted)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TibbSecondaryPillButton(
                onClick = { onNavigateTab(MainNavTab.RECORDS) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.navRecords)
            }
            TibbSecondaryPillButton(
                onClick = { onNavigateTab(MainNavTab.ALARMS_LABS) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.navAlarmsReports)
            }
        }

        Spacer(Modifier.height(14.dp))
    }

    if (showCheckInDialog) {
        DailyCheckInDialog(
            person = activePerson,
            locale = locale,
            unitSystem = unitSystem,
            existingCheckIn = todayLog.dailyCheckIn,
            currentSleepHours = todayLog.sleepHours,
            onDismiss = { showCheckInDialog = false },
            onSave = {
                onSubmitCheckIn(it)
                showCheckInDialog = false
            },
        )
    }

    if (showBellDialog) {
        AlertDialog(
            onDismissRequest = { showBellDialog = false },
            title = { Text(if (isUr) "آج کی باقی یاد دہانیاں" else "Due Reminders Today") },
            text = {
                if (dueTodayAlarms.isEmpty()) {
                    Text(if (isUr) "کوئی باقی دوا نہیں ہے" else "All active reminders for ${activePerson.name} are completed today.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dueTodayAlarms.forEach { alm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alm.medicineLabel, style = MaterialTheme.typography.titleMedium)
                                    Text(HealthMath.formatTime12h(alm.time24h), style = MaterialTheme.typography.bodySmall, color = c.muted)
                                }
                                TibbPrimaryPillButton(onClick = { onMarkAlarmTaken(alm.id) }) {
                                    Text(s.alarmsMarkTaken)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBellDialog = false }) { Text("Close") } },
        )
    }

    if (showScoreBreakdown) {
        AlertDialog(
            onDismissRequest = { showScoreBreakdown = false },
            title = { Text("Readiness Score: ${readiness.score}%") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Hydration: ${readiness.hydrationPct}%")
                    Text("• Steps Progress: ${readiness.stepsPct}%")
                    Text("• Sleep Balance: ${readiness.sleepPct}%")
                    if (readiness.hasScheduledAlarms) {
                        Text("• Medicine Adherence: ${readiness.adherencePct}%")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showScoreBreakdown = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun StepsBentoCard(
    todayLog: DailyWellnessLog,
    sensorStepsActive: Boolean,
    stepSensitivityHigh: Boolean,
    locale: AppLocale,
    onToggleSensor: () -> Unit,
    onToggleSensitivity: () -> Unit,
    onAddManualSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val pct = (todayLog.steps.toFloat() / todayLog.stepGoal.coerceAtLeast(1000)).coerceIn(0f, 1f)
    TibbGlassCard(modifier = modifier, padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(if (isUr) "روزانہ قدم" else "Daily Steps", style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1)
                }
                SectionHelpButton(topic = HelpTopic.BENTO_STEPS, locale = locale)
            }
            Box(modifier = Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    drawArc(
                        color = c.surfaceMuted,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = c.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * pct,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text(
                    "${todayLog.steps}",
                    style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                    color = c.ink,
                )
            }
            Text("Goal ${todayLog.stepGoal}", style = MaterialTheme.typography.labelSmall, color = c.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TibbSecondaryPillButton(
                    onClick = onToggleSensor,
                    borderColor = if (sensorStepsActive) c.safeGreen else c.border,
                    contentColor = if (sensorStepsActive) c.safeGreen else c.ink,
                    modifier = Modifier.weight(1f).testTag("bento_step_sensor_btn"),
                ) {
                    Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sensorStepsActive) "Live" else "Sensor", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                TibbSecondaryPillButton(
                    onClick = onAddManualSteps,
                    modifier = Modifier.testTag("bento_add_500_steps"),
                ) {
                    Text("+500", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (sensorStepsActive) {
                Text(
                    text = if (stepSensitivityHigh) "Sensitivity: High (Tap to toggle)" else "Sensitivity: Normal (Tap to toggle)",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                    modifier = Modifier.clickable(onClick = onToggleSensitivity),
                )
            }
        }
    }
}

@Composable
private fun ProgressGoalsBentoCard(
    met: Int,
    total: Int,
    locale: AppLocale,
    onOpenCoach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    TibbGlassCard(modifier = modifier.clickable(onClick = onOpenCoach), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.safeGreen, modifier = Modifier.size(18.dp))
                    Text(if (isUr) "آج کے اہداف" else "Progress Goals", style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1)
                }
                SectionHelpButton(topic = HelpTopic.BENTO_GOALS, locale = locale)
            }
            Text(
                "$met / $total",
                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold),
                color = c.ink,
            )
            LinearProgressIndicator(
                progress = { met / total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = c.safeGreen,
                trackColor = c.surfaceMuted,
            )
            Text(
                if (isUr) "غذا و ورزش کھولیں" else "Open Diet & Workout ->",
                style = MaterialTheme.typography.labelSmall,
                color = c.accent,
            )
        }
    }
}

@Composable
private fun CheckInBentoCard(
    todayLog: DailyWellnessLog,
    locale: AppLocale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    TibbThoughtGlowCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("bento_checkin_card"),
        padding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.MedicalServices, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(if (isUr) "روزانہ چیک ان" else "Your Check-In", style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1)
                }
                SectionHelpButton(topic = HelpTopic.BENTO_CHECKIN, locale = locale)
            }
            val check = todayLog.dailyCheckIn
            Text(
                text = if (check != null) {
                    "Energy: ${check.energyLevel ?: "Steady"} · Sleep ${check.sleepHours ?: todayLog.sleepHours}h"
                } else {
                    if (isUr) "وزن، نیند اور توانائی درج کریں" else "Log weight, sleep & energy today"
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
            )
            Text(
                text = if (check != null) "Tap to update" else "Tap to answer",
                style = MaterialTheme.typography.labelSmall,
                color = c.accent,
            )
        }
    }
}

@Composable
private fun WellnessBentoCard(
    todayLog: DailyWellnessLog,
    locale: AppLocale,
    onEditSleep: () -> Unit,
    onOpenVitals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    TibbGlassCard(modifier = modifier, padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(if (isUr) "صحت اور نیند" else "Daily Wellness", style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1)
                }
                SectionHelpButton(topic = HelpTopic.BENTO_WELLNESS, locale = locale)
            }
            Text("HR: ${todayLog.restingHr?.let { "$it bpm" } ?: "--"}", style = TabularNumberStyle, color = c.ink)
            Text(
                "BP: ${todayLog.lastBpSystolic ?: "--"}/${todayLog.lastBpDiastolic ?: "--"} · Sugar ${todayLog.lastSugarMgDl ?: "--"}",
                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                color = c.muted,
            )
            Text("Sleep: ${todayLog.sleepHours} hrs", style = TabularNumberStyle, color = c.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Edit Sleep",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                    modifier = Modifier.clickable(onClick = onEditSleep).padding(vertical = 4.dp).testTag("bento_edit_sleep_btn"),
                )
                Text("·", color = c.muted, modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Vitals ->",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.ink,
                    modifier = Modifier.clickable(onClick = onOpenVitals).padding(vertical = 4.dp).testTag("bento_open_vitals_btn"),
                )
            }
        }
    }
}
