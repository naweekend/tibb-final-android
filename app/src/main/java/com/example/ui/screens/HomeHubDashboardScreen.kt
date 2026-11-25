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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.DailyDoctorCheckIn
import com.example.data.model.DailyWellnessLog
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.SelectedLocationState
import com.example.data.model.StoredAlarm
import com.example.data.model.TriageTier
import com.example.data.model.UnitSystem
import com.example.engine.HealthMath
import com.example.engine.HelpTopic
import com.example.engine.RemediesAndPlans
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.PakistanLocationPickerDialog
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.TibbBarComparisonChart
import com.example.ui.components.TibbBarItem
import com.example.ui.components.TibbDonutGauge
import com.example.ui.components.TibbMetricProgressRow
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.SoftCharcoal
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHubDashboardScreen(
    locale: AppLocale,
    unitSystem: UnitSystem,
    profiles: List<PatientProfile>,
    activePerson: PatientProfile?,
    todayLog: DailyWellnessLog?,
    wellnessLogs: List<DailyWellnessLog> = emptyList(),
    alarms: List<StoredAlarm>,
    triageEngine: TriageEngine,
    userGpsCoords: GeoCoords?,
    selectedLocation: SelectedLocationState?,
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
    onConfirmLocation: (SelectedLocationState) -> Unit,
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
    var activityChartMetric by remember { mutableIntStateOf(0) } // 0 = Steps, 1 = Water, 2 = Sleep
    var expandedSection by remember { mutableStateOf<String?>(null) } // Accordion: all collapsed by default
    var showCheckInDialog by remember { mutableStateOf(false) }
    var showBellDialog by remember { mutableStateOf(false) }
    var showScoreBreakdown by remember { mutableStateOf(false) }
    var showSleepEditor by remember { mutableStateOf(false) }
    var bedTimeText by remember { mutableStateOf("23:00") }
    var wakeTimeText by remember { mutableStateOf("06:30") }

    // Hospital Lookup state
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedDistrict by remember(activePerson.id, selectedLocation) {
        mutableStateOf(selectedLocation?.district ?: activePerson.frequentPlaces.firstOrNull()?.district ?: "")
    }
    var activeCoords by remember(activePerson.id, userGpsCoords, selectedLocation) {
        val plc = activePerson.frequentPlaces.firstOrNull()
        mutableStateOf(
            userGpsCoords
                ?: selectedLocation?.let { GeoCoords(it.lat, it.lng) }
                ?: if (plc?.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
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
        // 1. Calm Greeting & Readiness Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c.accentSoft)
                        .border(0.75.dp, c.accentPastel.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activePerson.name.trim().take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = c.accent,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isUr) "السلام علیکم، ${activePerson.name}" else "Assalam-o-Alaikum, ${activePerson.name}",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.headlineSmall) else MaterialTheme.typography.headlineSmall,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            bmiCheck.bmi?.let { append("BMI $it (${bmiCheck.category}) · ") }
                            append(HealthMath.formatWeightDisplay(activePerson.weightKg, unitSystem))
                        },
                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                        color = c.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditPerson,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(c.surfaceElevated)
                        .border(0.75.dp, c.border, CircleShape)
                        .testTag("home_edit_person_btn"),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = s.editProfileBtn, tint = c.muted, modifier = Modifier.size(18.dp))
                }
                Box {
                    IconButton(
                        onClick = { showBellDialog = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(c.surfaceElevated)
                            .border(0.75.dp, c.border, CircleShape)
                            .testTag("home_bell_btn"),
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Due Reminders", tint = c.ink, modifier = Modifier.size(19.dp))
                    }
                    if (dueTodayAlarms.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(17.dp)
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

        // 2. Primary Hero Card (Visual Readiness Donut + Priority Next Action)
        TibbMistCard(padding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (isUr) "آج کے صحت اشارے" else "Today's Focus",
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                                color = c.mistInk,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            SectionHelpButton(topic = HelpTopic.DAILY_INSIGHTS_SCORE, locale = locale, onMistSurface = true)
                        }
                        Text(
                            text = if (isUr) "تفصیل کے لیے گول اسکور پر ٹیپ کریں" else "Tap readiness ring for score breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.mistInk.copy(alpha = 0.68f),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showScoreBreakdown = true }
                            .padding(2.dp)
                            .testTag("home_readiness_pill"),
                    ) {
                        TibbDonutGauge(
                            progress = readiness.score / 100f,
                            centerValue = "${readiness.score}%",
                            centerCaption = if (isUr) "اسکور" else "Ready",
                            size = 64.dp,
                            strokeWidth = 6.dp,
                            activeColor = c.ctaBg,
                            trackColor = c.mistInk.copy(alpha = 0.14f),
                            centerTextColor = c.mistInk,
                            accessibilityLabel = "Readiness score ${readiness.score} percent",
                        )
                    }
                }

                // Compact segmented toggle inside Hero
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.mistInk.copy(alpha = 0.09f))
                        .padding(3.dp),
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
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else Color.Transparent)
                                .clickable { insightTab = idx }
                                .padding(horizontal = 8.dp, vertical = 7.dp)
                                .testTag(if (idx == 0) "home_tab_reminders" else "home_tab_recommended"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) c.ctaInk else c.mistInk.copy(alpha = 0.80f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Text(
                    text = if (insightTab == 0) {
                        dueTodayAlarms.firstOrNull()?.let {
                            if (isUr) "اگلی دوا: ${it.medicineLabel}" else "Due Today: ${it.medicineLabel} · ${HealthMath.formatTime12h(it.time24h)}"
                        } ?: if (isUr) "آج کی تمام یاد دہانیاں مکمل ہیں" else "All active reminders completed — Ready for a checkup"
                    } else {
                        if (isUr) "ذاتی غذا اور ورزش کا پلان" else "Your Personalized Diet & WHO Checkup"
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.headlineSmall) else MaterialTheme.typography.headlineSmall,
                    color = c.mistInk,
                )

                Text(
                    text = if (insightTab == 0) {
                        if (isUr) "ایک وقت میں ایک آسان سوال سے معائنہ کریں یا دوا پر نشان لگائیں۔"
                        else "Calm, step-by-step symptom checkup or mark due medicine taken."
                    } else {
                        if (isUr) bmiCheck.adviceUr else bmiCheck.adviceEn
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                    color = c.mistInk.copy(alpha = 0.80f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                TibbPrimaryPillButton(
                    onClick = {
                        if (insightTab == 0 && dueTodayAlarms.isNotEmpty()) {
                            onMarkAlarmTaken(dueTodayAlarms.first().id)
                        } else if (insightTab == 1) {
                            onNavigateTab(MainNavTab.COACH)
                        } else {
                            onStartStructuredCheckup()
                        }
                    },
                    containerColor = c.ctaBg,
                    contentColor = c.ctaInk,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_featured_cta"),
                ) {
                    Text(
                        text = if (insightTab == 0 && dueTodayAlarms.isNotEmpty()) s.alarmsMarkTaken
                        else if (insightTab == 0) s.agentModeBtn
                        else s.navCoach,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 3. Clean 2x2 Key Metrics Summary (Proportional, uncluttered cards)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val twoCols = maxWidth >= 340.dp
            if (twoCols) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    ) {
                        StepsSummaryCard(
                            todayLog = todayLog,
                            sensorStepsActive = sensorStepsActive,
                            locale = locale,
                            onClick = {
                                expandedSection = if (expandedSection == "activity") null else "activity"
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ProgressGoalsSummaryCard(
                            met = activeGoalsCount,
                            total = 5,
                            locale = locale,
                            onOpenCoach = { onNavigateTab(MainNavTab.COACH) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    ) {
                        CheckInSummaryCard(
                            todayLog = todayLog,
                            locale = locale,
                            onClick = { showCheckInDialog = true },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        WellnessSummaryCard(
                            todayLog = todayLog,
                            locale = locale,
                            onOpenVitals = { onNavigateTab(MainNavTab.VITALS) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepsSummaryCard(
                        todayLog = todayLog,
                        sensorStepsActive = sensorStepsActive,
                        locale = locale,
                        onClick = { expandedSection = if (expandedSection == "activity") null else "activity" },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProgressGoalsSummaryCard(activeGoalsCount, 5, locale, { onNavigateTab(MainNavTab.COACH) }, Modifier.fillMaxWidth())
                    CheckInSummaryCard(todayLog, locale, { showCheckInDialog = true }, Modifier.fillMaxWidth())
                    WellnessSummaryCard(todayLog, locale, { onNavigateTab(MainNavTab.VITALS) }, Modifier.fillMaxWidth())
                }
            }
        }

        // 4. Progressive Disclosure Accordion Sections (All collapsed by default!)
        TibbExpandableSection(
            title = if (isUr) "پانی، قدم اور نیند" else "Hydration, Steps & Sleep",
            summary = "${TriageTier.BLUE.shortLabel(locale)} · ${todayLog.waterMl}/${todayLog.waterGoalMl} ml · ${todayLog.steps} steps · ${todayLog.sleepHours}h",
            icon = Icons.Rounded.WaterDrop,
            accentColor = c.wellnessBlue,
            expanded = expandedSection == "activity",
            onExpandedChange = { open -> expandedSection = if (open) "activity" else null },
            headerTestTag = "home_expand_activity",
            trailingHeaderContent = {
                SectionHelpButton(topic = HelpTopic.HYDRATION, locale = locale)
            },
        ) {
            // Hydration controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isUr) "پانی و نمکیات" else "Daily Hydration", style = MaterialTheme.typography.titleSmall, color = c.ink)
                Text("${todayLog.waterMl} / ${todayLog.waterGoalMl} ml", style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.accent)
            }
            LinearProgressIndicator(
                progress = { (todayLog.waterMl.toFloat() / todayLog.waterGoalMl.coerceAtLeast(500)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = c.wellnessBlue,
                trackColor = c.surfaceMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TibbSecondaryPillButton(
                    onClick = { onUpdateWellness(todayLog.copy(waterMl = (todayLog.waterMl - 250).coerceAtLeast(0))) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).testTag("water_minus_250"),
                ) { Text("-250 ml", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                TibbPrimaryPillButton(
                    onClick = { onUpdateWellness(todayLog.copy(waterMl = todayLog.waterMl + 250)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).testTag("water_plus_250"),
                ) { Text("+250 ml", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                TibbSecondaryPillButton(
                    onClick = { onUpdateWellness(todayLog.copy(waterMl = todayLog.waterMl + 500)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).testTag("water_plus_500"),
                ) { Text("+500 ml", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            }

            // Steps & Sensor controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isUr) "قدم اور سینسر" else "Steps & Motion Sensor", style = MaterialTheme.typography.titleSmall, color = c.ink)
                Text("${todayLog.steps} / ${todayLog.stepGoal}", style = TabularNumberStyle, color = c.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TibbSecondaryPillButton(
                    onClick = {
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
                    borderColor = if (sensorStepsActive) c.safeGreen else c.border,
                    contentColor = if (sensorStepsActive) c.safeGreen else c.ink,
                    modifier = Modifier.weight(1f).testTag("bento_step_sensor_btn"),
                ) {
                    Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sensorStepsActive) "Sensor On" else "Start Sensor", style = MaterialTheme.typography.labelSmall)
                }
                TibbSecondaryPillButton(
                    onClick = { onUpdateWellness(todayLog.copy(steps = todayLog.steps + 500)) },
                    modifier = Modifier.weight(1f).testTag("bento_add_500_steps"),
                ) {
                    Text("+500 Steps", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (sensorStepsActive) {
                Text(
                    text = if (stepSensitivityHigh) "Sensitivity: High (Tap to toggle)" else "Sensitivity: Normal (Tap to toggle)",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                    modifier = Modifier.clickable { onToggleStepSensor(!stepSensitivityHigh) },
                )
            }

            // Sleep Editor toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isUr) "نیند کا وقت" else "Sleep Duration (${todayLog.sleepHours} hrs)", style = MaterialTheme.typography.titleSmall, color = c.ink)
                TextButton(
                    onClick = { showSleepEditor = !showSleepEditor },
                    modifier = Modifier.testTag("bento_edit_sleep_btn"),
                ) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showSleepEditor) "Hide" else "Edit Sleep")
                }
            }
            if (showSleepEditor) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bedTimeText,
                        onValueChange = { bedTimeText = it },
                        label = { Text("Bed (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = wakeTimeText,
                        onValueChange = { wakeTimeText = it },
                        label = { Text("Wake (HH:MM)") },
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

            // Recent Activity & Hydration Trend Chart
            val trendLogs = remember(wellnessLogs, todayLog) {
                val base = if (wellnessLogs.isNotEmpty()) wellnessLogs else listOf(todayLog)
                base.sortedBy { it.dateKey }.takeLast(7)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceMuted.copy(alpha = 0.55f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isUr) "حالیہ روزانہ رجحان (${trendLogs.size} دن)" else "Recent Trend (${trendLogs.size}d)",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.ink,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            0 to (if (isUr) "قدم" else "Steps"),
                            1 to (if (isUr) "پانی" else "Water"),
                            2 to (if (isUr) "نیند" else "Sleep"),
                        ).forEach { (idx, lbl) ->
                            val sel = activityChartMetric == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.ctaBg else c.surfaceElevated)
                                    .clickable { activityChartMetric = idx }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.muted)
                            }
                        }
                    }
                }

                val barItems = trendLogs.map { lg ->
                    val shortDay = lg.dateKey.takeLast(5)
                    val isToday = lg.dateKey == todayLog.dateKey
                    when (activityChartMetric) {
                        0 -> TibbBarItem(
                            label = if (isToday) "Today" else shortDay,
                            value = lg.steps.toFloat(),
                            formattedValue = if (lg.steps >= 1000) "${(lg.steps / 1000f * 10).toInt() / 10f}k" else "${lg.steps}",
                            isHighlighted = isToday,
                        )
                        1 -> TibbBarItem(
                            label = if (isToday) "Today" else shortDay,
                            value = lg.waterMl.toFloat(),
                            formattedValue = "${lg.waterMl}",
                            color = if (isToday) c.wellnessBlue else c.wellnessBlue.copy(alpha = 0.45f),
                            isHighlighted = isToday,
                        )
                        else -> TibbBarItem(
                            label = if (isToday) "Today" else shortDay,
                            value = lg.sleepHours.toFloat(),
                            formattedValue = "${lg.sleepHours}h",
                            color = if (isToday) c.safeGreen else c.safeGreen.copy(alpha = 0.45f),
                            isHighlighted = isToday,
                        )
                    }
                }
                val goalVal = when (activityChartMetric) {
                    0 -> todayLog.stepGoal.toFloat()
                    1 -> todayLog.waterGoalMl.toFloat()
                    else -> 8f
                }
                val goalTxt = when (activityChartMetric) {
                    0 -> "Goal ${todayLog.stepGoal}"
                    1 -> "Goal ${todayLog.waterGoalMl} ml"
                    else -> "Target 8h"
                }
                TibbBarComparisonChart(
                    items = barItems,
                    chartHeight = 96.dp,
                    goalValue = goalVal,
                    goalLabel = goalTxt,
                    accessibilityLabel = "Daily activity comparison chart",
                )
            }
        }

        // Section 2: Nearby Hospitals & 1122 (Collapsed by default with clean summary + 1122 badge)
        val locationSummary = remember(selectedLocation, selectedDistrict, activeCoords) {
            when {
                selectedLocation != null -> "${selectedLocation.name} · ${selectedLocation.district}"
                selectedDistrict.isNotBlank() -> selectedDistrict
                activeCoords != null -> "GPS Location"
                else -> if (isUr) "پاکستان کا نقشہ یا قریبی کلینک کھولیں" else "Tap to pick Pakistan map location & clinics"
            }
        }
        TibbExpandableSection(
            title = s.nearestFacilitiesTitle,
            summary = locationSummary,
            icon = Icons.Rounded.LocalHospital,
            accentColor = c.espressoAccent,
            containerColor = c.espressoBottom,
            borderColor = Color.White.copy(alpha = 0.12f),
            titleColor = c.espressoInk,
            summaryColor = c.espressoMuted,
            expanded = expandedSection == "hospitals",
            onExpandedChange = { open -> expandedSection = if (open) "hospitals" else null },
            headerTestTag = "home_expand_hospitals",
            trailingHeaderContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.dangerRed)
                            .clickable { onOpenDialer("1122") }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_call_1122"),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("1122", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                    SectionHelpButton(topic = HelpTopic.NEAREST_HOSPITALS, locale = locale, onDarkSurface = true)
                }
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.espressoAccent)
                        .clickable { showMapPicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("home_open_map_picker"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.Map, contentDescription = "Open Pakistan Map", tint = SoftCharcoal, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (isUr) "پاکستان نقشہ (545+)" else "Pakistan Map (545+)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SoftCharcoal,
                        )
                    }
                }

                activePerson.frequentPlaces.forEach { plc ->
                    val sel = selectedDistrict.equals(plc.district, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (sel) c.espressoAccent.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.12f))
                            .clickable {
                                selectedDistrict = plc.district
                                activeCoords = if (plc.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${plc.label} (${plc.areaOrLandmark.ifBlank { plc.district }})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) SoftCharcoal else c.espressoInk,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) onRequestGps()
                            else locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("home_hospitals_gps_btn"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = c.espressoAccent, modifier = Modifier.size(14.dp))
                        Text(if (gpsLocating) "GPS..." else "Use GPS", style = MaterialTheme.typography.labelSmall, color = c.espressoInk)
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { districtMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("home_district_dropdown"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            selectedDistrict.ifBlank { s.allDistrictsOption },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.espressoInk,
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
                    color = c.espressoMuted,
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
                            Text(if (isUr) fac.name_ur else fac.name_en, style = MaterialTheme.typography.labelLarge, color = c.espressoInk)
                            Text(
                                text = "${fac.tehsil}, ${fac.district}" + (fac.distanceKm?.let { " · $it km" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.espressoMuted,
                            )
                        }
                        TibbSecondaryPillButton(
                            onClick = { onOpenDialer(fac.phone) },
                            borderColor = c.espressoAccent,
                            contentColor = c.espressoAccent,
                        ) {
                            Text(s.callBtn)
                        }
                    }
                }
            }
        }

        // Section 3: Family & Quick Shortcuts (Collapsed by default)
        TibbExpandableSection(
            title = if (isUr) "خاندان اور رپورٹس شارٹ کٹس" else "Family & Vault Shortcuts",
            summary = if (isUr) "${profiles.size} ممبرز · رپورٹس اور الارم" else "${profiles.size} profile(s) · Saved Reports & Alarms",
            icon = Icons.Rounded.People,
            expanded = expandedSection == "family",
            onExpandedChange = { open -> expandedSection = if (open) "family" else null },
            headerTestTag = "home_expand_family",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                profiles.forEach { p ->
                    val sel = p.id == activePerson.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) c.accentSoft else c.surfaceMuted)
                            .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(14.dp))
                            .clickable { onSelectPerson(p.id) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Column {
                            Text(p.name, style = MaterialTheme.typography.labelLarge, color = c.ink)
                            Text(if (isUr) p.relationship.labelUr else p.relationship.labelEn, style = MaterialTheme.typography.labelSmall, color = c.muted)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            ) {
                TibbSecondaryPillButton(
                    onClick = { onNavigateTab(MainNavTab.RECORDS) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = s.navRecords,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                TibbSecondaryPillButton(
                    onClick = { onNavigateTab(MainNavTab.ALARMS_LABS) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = s.navAlarmsReports,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
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
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TibbDonutGauge(
                        progress = readiness.score / 100f,
                        centerValue = "${readiness.score}%",
                        size = 52.dp,
                        strokeWidth = 5.5.dp,
                    )
                    Column {
                        Text("Daily Readiness", style = MaterialTheme.typography.titleLarge)
                        Text("Combined wellness score", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TibbMetricProgressRow(label = "Hydration", percent = readiness.hydrationPct, color = c.wellnessBlue)
                    TibbMetricProgressRow(label = "Steps Progress", percent = readiness.stepsPct, color = c.accent)
                    TibbMetricProgressRow(label = "Sleep Balance", percent = readiness.sleepPct, color = c.safeGreen)
                    if (readiness.hasScheduledAlarms) {
                        TibbMetricProgressRow(label = "Medicine Adherence", percent = readiness.adherencePct, color = c.urgentYellow)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showScoreBreakdown = false }) { Text("OK") } },
        )
    }

    if (showMapPicker) {
        PakistanLocationPickerDialog(
            locale = locale,
            triageEngine = triageEngine,
            initialSelection = selectedLocation,
            userGpsCoords = userGpsCoords,
            gpsLocating = gpsLocating,
            onRequestGps = onRequestGps,
            onConfirm = { picked ->
                selectedDistrict = picked.district
                activeCoords = GeoCoords(picked.lat, picked.lng)
                onConfirmLocation(picked)
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
        )
    }
}

@Composable
private fun StepsSummaryCard(
    todayLog: DailyWellnessLog,
    sensorStepsActive: Boolean,
    locale: AppLocale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val pct = (todayLog.steps.toFloat() / todayLog.stepGoal.coerceAtLeast(1000)).coerceIn(0f, 1f)
    TibbGlassCard(modifier = modifier.clickable(onClick = onClick), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isUr) "قدم" else "Steps",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SectionHelpButton(topic = HelpTopic.BENTO_STEPS, locale = locale)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TibbDonutGauge(
                    progress = pct,
                    centerValue = "${(pct * 100).toInt()}%",
                    size = 46.dp,
                    strokeWidth = 5.dp,
                    centerValueFontSize = MaterialTheme.typography.labelSmall.fontSize,
                )
                Column {
                    Text(
                        text = "${todayLog.steps}",
                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold),
                        color = c.ink,
                    )
                    Text(
                        text = if (sensorStepsActive) "Sensor live" else "of ${todayLog.stepGoal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sensorStepsActive) c.safeGreen else c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressGoalsSummaryCard(
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.wellnessBlue, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isUr) "آج کے اہداف" else "Goals",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SectionHelpButton(topic = HelpTopic.BENTO_GOALS, locale = locale)
            }
            Text(
                "$met / $total",
                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.Bold),
                color = c.ink,
            )
            // Segmented 5-step goal bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(total) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (idx < met) c.wellnessBlue else c.surfaceMuted),
                    )
                }
            }
            Text(
                if (isUr) "BLUE تندرستی · غذا و ورزش" else "BLUE Wellness · Diet ->",
                style = MaterialTheme.typography.labelSmall,
                color = c.wellnessBlue,
            )
        }
    }
}

@Composable
private fun CheckInSummaryCard(
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.MedicalServices, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isUr) "چیک ان" else "Check-In",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SectionHelpButton(topic = HelpTopic.BENTO_CHECKIN, locale = locale)
            }
            val check = todayLog.dailyCheckIn
            Text(
                text = if (check != null) {
                    "${check.energyLevel ?: "Steady"} · ${check.sleepHours ?: todayLog.sleepHours}h"
                } else {
                    if (isUr) "آج کا حال درج کریں" else "Log energy & sleep"
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (check != null) "Update ->" else "Tap to answer ->",
                style = MaterialTheme.typography.labelSmall,
                color = c.accent,
            )
        }
    }
}

@Composable
private fun WellnessSummaryCard(
    todayLog: DailyWellnessLog,
    locale: AppLocale,
    onOpenVitals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    TibbGlassCard(
        modifier = modifier
            .clickable(onClick = onOpenVitals)
            .testTag("bento_open_vitals_btn"),
        padding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isUr) "نبض و بی پی" else "Vitals",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SectionHelpButton(topic = HelpTopic.BENTO_WELLNESS, locale = locale)
            }
            Text(
                text = "HR ${todayLog.restingHr?.let { "$it" } ?: "--"} bpm",
                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold),
                color = c.ink,
            )
            Text(
                text = "BP ${todayLog.lastBpSystolic ?: "--"}/${todayLog.lastBpDiastolic ?: "--"} · Sugar ${todayLog.lastSugarMgDl ?: "--"}",
                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Log Vitals ->",
                style = MaterialTheme.typography.labelSmall,
                color = c.accent,
            )
        }
    }
}
