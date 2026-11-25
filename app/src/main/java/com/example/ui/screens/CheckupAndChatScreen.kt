package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PregnantWoman
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.EmergencyCategory
import com.example.data.model.EpisodeVitals
import com.example.data.model.GeoCoords
import com.example.data.model.MedicalGuidanceAssessment
import com.example.data.model.PatientProfile
import com.example.data.model.SelectedLocationState
import com.example.data.model.StructuredAssessmentInput
import com.example.data.model.SymptomCategoryNode
import com.example.engine.AudioFeedbackManager
import com.example.engine.HelpTopic
import com.example.engine.LocalMedicalGuidanceEngine
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.OfflineUrgencyClassificationsCard
import com.example.ui.components.SectionHelpButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.TibbSegmentedTabs
import com.example.ui.theme.urduTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckupAndChatScreen(
    locale: AppLocale,
    activePerson: PatientProfile?,
    checkupSubMode: Int, // 0 = Symptom Checker, 1 = Medical Library & Scope
    activeGuidanceAssessment: MedicalGuidanceAssessment?,
    guidanceEngine: LocalMedicalGuidanceEngine,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    userGpsCoords: GeoCoords?,
    selectedLocation: SelectedLocationState?,
    gpsLocating: Boolean,
    onSetSubMode: (Int) -> Unit,
    onCompleteStructuredAssessment: (StructuredAssessmentInput, MedicalGuidanceAssessment) -> Unit,
    onClearGuidanceAssessment: () -> Unit,
    onRequestGps: () -> Unit,
    onConfirmLocation: (SelectedLocationState) -> Unit,
    onOpenDialer: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onOpenWellnessCoach: () -> Unit = {},
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    val isRoman = locale == AppLocale.ROMAN

    if (activePerson == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.onboardingSubtitle, color = c.muted)
        }
        return
    }

    val categories = remember(activePerson.id, activePerson.ageMonths, activePerson.isPregnantOrLactating) {
        guidanceEngine.visibleCategories(activePerson)
    }

    var step by remember(activePerson.id) { mutableIntStateOf(1) }
    var conditionQuery by remember(activePerson.id) { mutableStateOf("") }
    var selectedCategoryId by remember(activePerson.id) { mutableStateOf<String?>(null) }
    var selectedSymptomIds by remember(activePerson.id) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedOptionsByQ by remember(activePerson.id) { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var rrText by remember(activePerson.id) { mutableStateOf("") }
    var hrText by remember(activePerson.id) { mutableStateOf("") }
    var bpSysText by remember(activePerson.id) { mutableStateOf("") }
    var bpDiaText by remember(activePerson.id) { mutableStateOf("") }
    var sugarText by remember(activePerson.id) { mutableStateOf("") }

    fun resetWizard() {
        step = 1
        conditionQuery = ""
        selectedCategoryId = null
        selectedSymptomIds = emptySet()
        selectedOptionsByQ = emptyMap()
        rrText = ""
        hrText = ""
        bpSysText = ""
        bpDiaText = ""
        sugarText = ""
        onClearGuidanceAssessment()
    }

    fun buildInput(directEmergency: EmergencyCategory? = null): StructuredAssessmentInput {
        val vitals = EpisodeVitals(
            respiratoryRate = rrText.trim().toIntOrNull(),
            heartRate = hrText.trim().toIntOrNull(),
            bpSystolic = bpSysText.trim().toIntOrNull(),
            bpDiastolic = bpDiaText.trim().toIntOrNull(),
            bloodGlucoseMgDl = sugarText.trim().toIntOrNull(),
        )
        return StructuredAssessmentInput(
            patient = activePerson,
            categoryId = selectedCategoryId.orEmpty(),
            selectedSymptomIds = selectedSymptomIds,
            selectedOptionIdsByQuestion = selectedOptionsByQ,
            vitals = vitals,
            district = selectedLocation?.district ?: activePerson.frequentPlaces.firstOrNull()?.district,
            userCoords = userGpsCoords ?: selectedLocation?.let { GeoCoords(it.lat, it.lng) },
            directEmergencyCategory = directEmergency,
        )
    }

    BackHandler(enabled = activeGuidanceAssessment != null || step > 1 || checkupSubMode == 1) {
        when {
            checkupSubMode == 1 -> onSetSubMode(0)
            activeGuidanceAssessment != null -> onClearGuidanceAssessment()
            step > 1 -> step--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TibbSegmentedTabs(
            options = listOf(0 to s.agentModeBtn, 1 to s.structuredCheckupBtn),
            selected = checkupSubMode,
            onSelect = onSetSubMode,
            tagPrefix = "checkup_mode",
        )

        if (checkupSubMode == 1) {
            MedicalLibraryView(
                locale = locale,
                guidanceEngine = guidanceEngine,
                categories = categories,
            )
            Spacer(Modifier.height(12.dp))
            return
        }

        if (activeGuidanceAssessment != null) {
            MedicalGuidanceResultView(
                assessment = activeGuidanceAssessment,
                patient = activePerson,
                locale = locale,
                triageEngine = triageEngine,
                audioManager = audioManager,
                userGpsCoords = userGpsCoords,
                selectedLocation = selectedLocation,
                gpsLocating = gpsLocating,
                onRequestGps = onRequestGps,
                onConfirmLocation = onConfirmLocation,
                onOpenDialer = onOpenDialer,
                onShowToast = onShowToast,
                onChangeSelections = { onClearGuidanceAssessment() },
                onStartNewAssessment = { resetWizard() },
                onOpenWellnessCoach = onOpenWellnessCoach,
            )
            return
        }

        // Top Offline Status + Help row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OfflineStatusBadge(
                label = s.activeEngineOffline,
                modifier = Modifier.weight(1f, fill = false),
            )
            SectionHelpButton(topic = HelpTopic.CHECKUP_CHAT_LIVE, locale = locale)
        }

        // Wizard Step Indicator
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (step) {
                        1 -> if (isUr) "مرحلہ 1 از 3: علامت کا شعبہ منتخب کریں" else "Step 1 of 3: Choose Symptom Area"
                        2 -> if (isUr) "مرحلہ 2 از 3: مخصوص علامات منتخب کریں" else "Step 2 of 3: Select Specific Symptoms"
                        else -> if (isUr) "مرحلہ 3 از 3: اضافی سوالات اور وائٹلز" else "Step 3 of 3: Follow-up & Safety Check"
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                    color = c.ink,
                )
                Text("${step}/3", style = MaterialTheme.typography.labelMedium, color = c.accent)
            }
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = c.accent,
                trackColor = c.surfaceMuted,
            )
        }

        when (step) {
            1 -> {
                // Emergency Bypass Accordion (Always first for critical safety)
                TibbExpandableSection(
                    title = if (isUr) "شدید ایمرجنسی؟ فوری 1122 رہنمائی" else "Urgent Emergency? Instant 1122 Bypass",
                    summary = if (isUr) "سانس رکنا، سینے کا شدید درد، فالج، خون یا زہر" else "Chest pain, choking, stroke, heavy bleeding, poisoning",
                    icon = Icons.Rounded.Emergency,
                    accentColor = c.dangerRed,
                    containerColor = c.dangerSoft,
                    borderColor = c.dangerRed.copy(alpha = 0.55f),
                    titleColor = c.dangerRed,
                    initiallyExpanded = false,
                    headerTestTag = "checker_emergency_bypass",
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EmergencyCategory.entries.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(c.dangerRed)
                                    .clickable {
                                        val input = buildInput(directEmergency = cat)
                                        val result = guidanceEngine.evaluateGuidance(input)
                                        onCompleteStructuredAssessment(input, result)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("emerg_bypass_${cat.name.lowercase()}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = emergencyLabel(cat, isUr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                        }
                    }
                }

                // Offline Clinical Triage: Four Colour-Coded Urgency Classifications
                TibbExpandableSection(
                    title = s.offlineTriageClassificationsTitle,
                    summary = "RED · YELLOW · GREEN · BLUE",
                    icon = Icons.Rounded.MonitorHeart,
                    accentColor = c.wellnessBlue,
                    initiallyExpanded = false,
                    headerTestTag = "checker_urgency_legend_expand",
                ) {
                    OfflineUrgencyClassificationsCard(locale = locale)
                }

                val totalCondCount = remember { guidanceEngine.getAllConditions().size }
                OutlinedTextField(
                    value = conditionQuery,
                    onValueChange = { conditionQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (conditionQuery.isNotEmpty()) {
                            IconButton(onClick = { conditionQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (isUr) "$totalCondCount امراض یا علامات تلاش کریں (مثلاً ڈینگی، خارش، کھانسی)" else "Quick search $totalCondCount+ conditions or symptoms (e.g. gout, scabies, sinus)...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checker_quick_search"),
                )

                if (conditionQuery.trim().length >= 2) {
                    val allowedCatIds = remember(categories) { categories.map { it.id }.toSet() }
                    val quickMatches = remember(conditionQuery, allowedCatIds) {
                        guidanceEngine.searchConditions(conditionQuery)
                            .filter { it.categoryId in allowedCatIds }
                            .take(8)
                    }
                    if (quickMatches.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isUr) "براہِ راست متعلقہ کیفیات (${quickMatches.size})" else "Matching Conditions — tap to start checkup",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.accent,
                            )
                            quickMatches.forEach { cond ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(c.surfaceElevated)
                                        .border(1.dp, c.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedCategoryId = cond.categoryId
                                            selectedSymptomIds = guidanceEngine.getStarterSymptomsForCondition(cond)
                                            selectedOptionsByQ = emptyMap()
                                            step = 2
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("checker_quick_cond_${cond.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isUr) cond.nameUr else cond.nameEn,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = c.ink,
                                        )
                                        Text(
                                            text = if (isUr) cond.subcategoryUr.ifBlank { cond.categoryLabelUr } else cond.subcategoryEn.ifBlank { cond.categoryLabelEn },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.muted,
                                        )
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (isUr) "کوئی براہِ راست نتیجہ نہیں ملا — نیچے سے شعبہ منتخب کریں۔" else "No direct condition match — select a body category below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                }

                Text(
                    text = if (isUr) "${activePerson.name} کے لیے کون سا مسئلہ ہے؟" else "What brings ${activePerson.name} in today?",
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.headlineSmall) else MaterialTheme.typography.headlineSmall,
                    color = c.ink,
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val twoCols = maxWidth >= 360.dp
                    if (twoCols) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    pair.forEach { cat ->
                                        CategorySelectionCard(
                                            category = cat,
                                            selected = selectedCategoryId == cat.id,
                                            locale = locale,
                                            onClick = {
                                                if (selectedCategoryId != cat.id) {
                                                    selectedCategoryId = cat.id
                                                    selectedSymptomIds = emptySet()
                                                    selectedOptionsByQ = emptyMap()
                                                }
                                                step = 2
                                            },
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categories.forEach { cat ->
                                CategorySelectionCard(
                                    category = cat,
                                    selected = selectedCategoryId == cat.id,
                                    locale = locale,
                                    onClick = {
                                        if (selectedCategoryId != cat.id) {
                                            selectedCategoryId = cat.id
                                            selectedSymptomIds = emptySet()
                                            selectedOptionsByQ = emptyMap()
                                        }
                                        step = 2
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                val activeCat = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
                val visibleSyms = remember(activeCat.id, activePerson.id, activePerson.ageMonths, activePerson.isPregnantOrLactating) {
                    guidanceEngine.visibleSymptoms(activeCat, activePerson)
                }

                TibbGlassCard(padding = 16.dp, backgroundColor = c.surfaceElevated) {
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
                            Icon(categoryIconForKey(activeCat.iconKey), contentDescription = null, tint = c.accent)
                            Column {
                                Text(
                                    text = if (isUr) activeCat.titleUr else if (isRoman) activeCat.titleRoman else activeCat.titleEn,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                                    color = c.ink,
                                )
                                Text(
                                    text = if (isUr) "موجود تمام علامات منتخب کریں" else "Select all symptoms present right now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.muted,
                                )
                            }
                        }
                        TibbSecondaryPillButton(
                            onClick = { step = 1 },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(if (isUr) "تبدیل" else "Change", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                visibleSyms.forEach { sym ->
                    val checked = sym.id in selectedSymptomIds
                    val borderCol = when {
                        checked && sym.isRedFlagSymptom -> c.dangerRed
                        checked -> c.accent
                        sym.isRedFlagSymptom -> c.dangerRed.copy(alpha = 0.45f)
                        else -> c.border
                    }
                    val bgCol = when {
                        checked && sym.isRedFlagSymptom -> c.dangerSoft
                        checked -> c.accentSoft
                        else -> c.surfaceElevated
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(bgCol)
                            .border(if (checked || sym.isRedFlagSymptom) 1.2.dp else 0.75.dp, borderCol, RoundedCornerShape(18.dp))
                            .clickable {
                                selectedSymptomIds = if (checked) selectedSymptomIds - sym.id else selectedSymptomIds + sym.id
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("sym_pick_${sym.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (isUr) sym.labelUr else sym.labelEn,
                            tint = if (sym.isRedFlagSymptom) c.dangerRed else if (checked) c.accent else c.muted,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isUr) sym.labelUr else if (isRoman) sym.labelRoman else sym.labelEn,
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodyMedium) else MaterialTheme.typography.bodyMedium,
                                color = c.ink,
                            )
                            if (sym.isRedFlagSymptom) {
                                Text(
                                    text = if (isUr) "خطرناک علامت (فوری ایمرجنسی)" else "Red-flag warning sign",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.dangerRed,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TibbSecondaryPillButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isUr) "واپس" else "Back")
                    }
                    TibbPrimaryPillButton(
                        enabled = selectedSymptomIds.isNotEmpty(),
                        onClick = {
                            val quickInput = buildInput()
                            if (guidanceEngine.checkEmergencyTriggers(quickInput).isNotEmpty()) {
                                val redAssessment = guidanceEngine.evaluateGuidance(quickInput)
                                onCompleteStructuredAssessment(quickInput, redAssessment)
                            } else {
                                step = 3
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("checker_step2_next_btn"),
                    ) {
                        Text(if (isUr) "آگے بڑھیں" else "Continue")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            else -> {
                val activeCatId = selectedCategoryId ?: categories.first().id
                val questions = remember(activeCatId, selectedSymptomIds, activePerson.id) {
                    guidanceEngine.visibleFollowUpQuestions(activeCatId, selectedSymptomIds, activePerson)
                }

                questions.forEach { q ->
                    val chosen = selectedOptionsByQ[q.id].orEmpty()
                    TibbGlassCard(padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isUr) q.promptUr else if (isRoman) q.promptRoman else q.promptEn,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleSmall) else MaterialTheme.typography.titleSmall,
                                    color = c.ink,
                                    modifier = Modifier.weight(1f),
                                )
                                if (q.required) {
                                    Text(
                                        text = if (isUr) "لازمی" else "Required",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.accent,
                                    )
                                }
                            }
                            q.options.forEach { opt ->
                                val sel = opt.id in chosen
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                sel && opt.isEmergencyTrigger -> c.dangerSoft
                                                sel -> c.accentSoft
                                                else -> c.surfaceMuted
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                sel && opt.isEmergencyTrigger -> c.dangerRed
                                                sel -> c.accent
                                                else -> c.border
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable {
                                            val nextSet = if (q.multiSelect) {
                                                if (sel) chosen - opt.id else chosen + opt.id
                                            } else {
                                                setOf(opt.id)
                                            }
                                            selectedOptionsByQ = selectedOptionsByQ + (q.id to nextSet)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("opt_pick_${opt.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = when {
                                            q.multiSelect && sel -> Icons.Rounded.CheckBox
                                            q.multiSelect -> Icons.Rounded.CheckBoxOutlineBlank
                                            sel -> Icons.Rounded.RadioButtonChecked
                                            else -> Icons.Rounded.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                        tint = if (opt.isEmergencyTrigger) c.dangerRed else if (sel) c.accent else c.muted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = if (isUr) opt.labelUr else if (isRoman) opt.labelRoman else opt.labelEn,
                                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                        color = if (opt.isEmergencyTrigger && sel) c.dangerRed else c.ink,
                                    )
                                }
                            }
                        }
                    }
                }

                // Instant Red Alert if any selected option is an emergency trigger
                val previewInput = buildInput()
                val previewTriggers = guidanceEngine.checkEmergencyTriggers(previewInput)
                if (previewTriggers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.dangerSoft)
                            .border(1.5.dp, c.dangerRed, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.dangerRed)
                            Text(
                                text = if (isUr) "خطرناک علامت منتخب ہوئی ہے — فوری نتیجہ دیکھنے کے لیے بٹن دبائیں۔" else "Emergency trigger detected — tap Evaluate to view immediate 1122 & First Aid guidance.",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.dangerRed,
                            )
                        }
                    }
                }

                // Collapsed Optional Vitals Accordion
                TibbExpandableSection(
                    title = if (isUr) "اختیاری وائٹلز (سانس، بی پی، شوگر)" else "Optional Vital Signs (BP, Sugar, Breathing)",
                    summary = if (isUr) "اگر مشین یا ناپ موجود ہو تو درج کریں" else "Enter if measured (helps catch silent emergencies)",
                    icon = Icons.Rounded.MonitorHeart,
                    initiallyExpanded = false,
                    headerTestTag = "checker_optional_vitals_expand",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rrText,
                            onValueChange = { rrText = it },
                            label = { Text("Breaths/min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = hrText,
                            onValueChange = { hrText = it },
                            label = { Text("Pulse BPM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bpSysText,
                            onValueChange = { bpSysText = it },
                            label = { Text("Upper BP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = bpDiaText,
                            onValueChange = { bpDiaText = it },
                            label = { Text("Lower BP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = sugarText,
                        onValueChange = { sugarText = it },
                        label = { Text("Blood Sugar (mg/dL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TibbSecondaryPillButton(
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isUr) "واپس" else "Back")
                    }
                    TibbPrimaryPillButton(
                        onClick = {
                            val input = buildInput()
                            val result = guidanceEngine.evaluateGuidance(input)
                            onCompleteStructuredAssessment(input, result)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("checker_evaluate_btn"),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isUr) "محفوظ رہنمائی حاصل کریں" else "Get Safe Guidance")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CategorySelectionCard(
    category: SymptomCategoryNode,
    selected: Boolean,
    locale: AppLocale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val isRoman = locale == AppLocale.ROMAN
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) c.accentSoft else c.surface)
            .border(1.2.dp, if (selected) c.accent else c.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("cat_card_${category.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = categoryIconForKey(category.iconKey),
            contentDescription = if (isUr) category.titleUr else category.titleEn,
            tint = c.accent,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = if (isUr) category.titleUr else if (isRoman) category.titleRoman else category.titleEn,
            style = if (isUr) urduTextStyle(MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) else MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (isUr) category.subtitleUr else category.subtitleEn,
            style = MaterialTheme.typography.bodySmall,
            color = c.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun categoryIconForKey(key: String): ImageVector = when (key) {
    "AIR" -> Icons.Rounded.Air
    "THERMOSTAT" -> Icons.Rounded.Thermostat
    "HEART" -> Icons.Rounded.Favorite
    "STOMACH" -> Icons.Rounded.LocalDining
    "BRAIN" -> Icons.Rounded.Psychology
    "SKIN", "ALLERGY" -> Icons.Rounded.Healing
    "VISIBILITY" -> Icons.Rounded.Visibility
    "BONE", "HEALING" -> Icons.Rounded.MedicalServices
    "WATER" -> Icons.Rounded.WaterDrop
    "BIOTECH" -> Icons.Rounded.Biotech
    "MONITOR" -> Icons.Rounded.MonitorHeart
    "CHILD" -> Icons.Rounded.ChildCare
    "ELDERLY" -> Icons.Rounded.Elderly
    "PREGNANT" -> Icons.Rounded.PregnantWoman
    else -> Icons.Rounded.MedicalServices
}

private fun emergencyLabel(cat: EmergencyCategory, isUr: Boolean): String = when (cat) {
    EmergencyCategory.CHEST_PAIN -> if (isUr) "سینے میں شدید درد" else "Chest Pain"
    EmergencyCategory.STROKE_FAST -> if (isUr) "فالج (FAST)" else "Stroke"
    EmergencyCategory.BREATHING_DISTRESS -> if (isUr) "سانس رکنا" else "Breathing Crisis"
    EmergencyCategory.UNCONTROLLED_BLEEDING -> if (isUr) "شدید خون" else "Heavy Bleeding"
    EmergencyCategory.SEIZURE_CONVULSIONS -> if (isUr) "دورہ / جھٹکے" else "Seizure"
    EmergencyCategory.SEVERE_BURN -> if (isUr) "شدید جلنا" else "Severe Burn"
    EmergencyCategory.POISONING_OVERDOSE -> if (isUr) "زہر / اوورڈوز" else "Poisoning"
    EmergencyCategory.HEATSTROKE -> if (isUr) "ہیٹ اسٹروک" else "Heatstroke"
    EmergencyCategory.CHOKING -> if (isUr) "گلا بند / دم گھٹنا" else "Choking"
    EmergencyCategory.UNCONSCIOUS_FAINTING -> if (isUr) "بے ہوشی" else "Unconscious"
    EmergencyCategory.MATERNAL_EMERGENCY -> if (isUr) "حمل ایمرجنسی" else "Maternal Crisis"
    EmergencyCategory.PEDIATRIC_DANGER -> if (isUr) "بچہ خطرناک علامت" else "Infant Danger"
    EmergencyCategory.ANAPHYLAXIS -> if (isUr) "شدید الرجی سوجن" else "Anaphylaxis"
    EmergencyCategory.SERIOUS_HEAD_INJURY -> if (isUr) "سر کی چوٹ" else "Head Injury"
    EmergencyCategory.SEVERE_DEHYDRATION -> if (isUr) "شدید پانی کی کمی" else "Severe Dehydration"
    EmergencyCategory.ACUTE_SEVERE_ABDOMEN -> if (isUr) "شدید پیٹ درد" else "Acute Abdomen"
}
