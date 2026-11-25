package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.DailyWellnessLog
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.TriageTier
import com.example.data.model.WellnessGoal
import com.example.engine.HelpTopic
import com.example.engine.RemediesAndPlans
import com.example.engine.TrilingualStrings
import com.example.ui.components.SectionHelpButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.TibbSegmentedTabs
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessCoachScreen(
    locale: AppLocale,
    activePerson: PatientProfile?,
    todayLog: DailyWellnessLog?,
    onGeneratePlan: (WellnessGoal, Int) -> Unit,
    onUpdatePlan: (PersonalizedWellnessPlan) -> Unit,
    onUpdateWellness: (DailyWellnessLog) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    if (activePerson == null || todayLog == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.onboardingSubtitle, color = c.muted)
        }
        return
    }

    var subTab by remember { mutableIntStateOf(0) } // 0 = Diet Plan, 1 = Workout, 2 = Remedies
    var selectedGoal by remember(activePerson.id) { mutableStateOf(activePerson.preferredGoal) }
    var variationSeed by remember { mutableIntStateOf(0) }
    var remedyFilter by remember { mutableStateOf("ALL") }
    var expandedRemedyId by remember { mutableStateOf<String?>(null) }

    var workoutSec by remember { mutableIntStateOf(0) }
    var workoutRunning by remember { mutableStateOf(false) }
    LaunchedEffect(workoutRunning) {
        while (workoutRunning) {
            delay(1000L)
            workoutSec++
        }
    }

    val bmi = remember(activePerson) {
        RemediesAndPlans.computeBmiAssessment(activePerson.weightKg, activePerson.heightCm, activePerson.ageMonths)
    }
    val plan = todayLog.plan ?: remember(activePerson.id, selectedGoal, variationSeed) {
        RemediesAndPlans.buildPersonalizedPlanOffline(activePerson, selectedGoal, variationSeed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Compact BMI & BLUE (Wellness) Classification Header
        TibbGlassCard(padding = 14.dp, borderColor = c.wellnessBlue.copy(alpha = 0.45f)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.wellnessBlueSoft)
                        .border(1.dp, c.wellnessBlue.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("coach_blue_wellness_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(c.wellnessBlue),
                    )
                    Text(
                        text = s.blueWellnessLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = c.wellnessBlue,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isUr) "جنوبی ایشیائی بی ایم آئی" else "South Asian BMI",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (bmi.bmi != null) {
                            Text("BMI ${bmi.bmi}", style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.accent)
                        }
                        SectionHelpButton(topic = HelpTopic.COACH_BMI, locale = locale)
                    }
                }
                Text(if (isUr) bmi.labelUr else bmi.labelEn, style = MaterialTheme.typography.labelMedium, color = c.accent)
                Text(if (isUr) bmi.adviceUr else bmi.adviceEn, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // Unified Subtabs
        TibbSegmentedTabs(
            options = listOf(
                0 to (if (isUr) "غذا پلان" else "Diet Plan"),
                1 to (if (isUr) "ورزش" else "Workout"),
                2 to (if (isUr) "گھریلو علاج" else "Remedies"),
            ),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "coach_subtab",
        )

        when (subTab) {
            0 -> {
                // Collapsed-by-default Goal Picker
                TibbExpandableSection(
                    title = if (isUr) "صحت کا ہدف تبدیل کریں" else "Wellness Goal",
                    summary = if (isUr) selectedGoal.titleUr else selectedGoal.titleEn,
                    icon = Icons.Rounded.Flag,
                    initiallyExpanded = false,
                    headerTestTag = "coach_goal_expand_btn",
                    trailingHeaderContent = {
                        SectionHelpButton(topic = HelpTopic.COACH_DIET, locale = locale)
                    },
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WellnessGoal.entries.forEach { g ->
                            val sel = selectedGoal == g
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.ctaBg else c.surfaceMuted)
                                    .border(1.dp, if (sel) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                    .clickable {
                                        selectedGoal = g
                                        onGeneratePlan(g, variationSeed)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(if (isUr) g.titleUr else g.titleEn, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                            }
                        }
                    }
                }

                // Single Primary Action CTA
                TibbPrimaryPillButton(
                    onClick = {
                        variationSeed++
                        onGeneratePlan(selectedGoal, variationSeed)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coach_rotate_plan_btn"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isUr) "نیا دیسی مینو بنائیں" else "Generate / Rotate Desi Menu")
                }

                // Clean Meal Cards
                plan.dietMeals.forEachIndexed { slotIdx, meal ->
                    val eaten = todayLog.mealsChecked.contains(meal.slot)
                    TibbGlassCard(padding = 14.dp, borderColor = if (eaten) c.safeGreen else c.border) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${meal.slot} · ~${meal.caloriesApprox} kcal",
                                        style = TabularNumberStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = c.accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (meal.alternatives.size > 1) {
                                    TibbSecondaryPillButton(
                                        onClick = {
                                            val nextIdx = (meal.selectedOptionIndex + 1) % meal.alternatives.size
                                            val nextOpt = meal.alternatives[nextIdx]
                                            val updatedMeals = plan.dietMeals.toMutableList()
                                            updatedMeals[slotIdx] = meal.copy(
                                                title_en = nextOpt.title_en,
                                                title_ur = nextOpt.title_ur,
                                                portions_en = nextOpt.portions_en,
                                                portions_ur = nextOpt.portions_ur,
                                                caloriesApprox = nextOpt.caloriesApprox,
                                                selectedOptionIndex = nextIdx,
                                            )
                                            onUpdatePlan(plan.copy(dietMeals = updatedMeals))
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("meal_swap_${meal.slot.lowercase().replace(' ', '_')}"),
                                    ) {
                                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Swap", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }

                            Text(
                                text = if (isUr) meal.title_ur else meal.title_en,
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                                color = c.ink,
                            )
                            Text(
                                text = if (isUr) meal.portions_ur else meal.portions_en,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.muted,
                            )

                            TibbSecondaryPillButton(
                                onClick = {
                                    val nextChecked = if (eaten) todayLog.mealsChecked - meal.slot else todayLog.mealsChecked + meal.slot
                                    onUpdateWellness(todayLog.copy(mealsChecked = nextChecked))
                                },
                                borderColor = if (eaten) c.safeGreen else c.border,
                                contentColor = if (eaten) c.safeGreen else c.ink,
                                modifier = Modifier.testTag("meal_eaten_${meal.slot.lowercase().replace(' ', '_')}"),
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (eaten) "Eaten Today" else "Mark Eaten")
                            }
                        }
                    }
                }

                // Collapsed-by-default Condition & Allergy Safeguards
                val notes = if (isUr) plan.avoidNotes_ur else plan.avoidNotes_en
                if (notes.isNotEmpty()) {
                    TibbExpandableSection(
                        title = if (isUr) "احتیاطی غذائی اصول" else "Condition & Allergy Safeguards",
                        summary = "${notes.size} safety notes",
                        icon = Icons.Rounded.Warning,
                        accentColor = c.amber,
                        initiallyExpanded = false,
                        headerTestTag = "coach_safeguards_expand_btn",
                    ) {
                        notes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }
                }
            }

            1 -> {
                TibbGlassCard(padding = 18.dp, borderColor = c.accent.copy(alpha = 0.4f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = c.accent)
                                Text("Workout Timer", style = MaterialTheme.typography.titleLarge, color = c.ink)
                            }
                            SectionHelpButton(topic = HelpTopic.COACH_WORKOUT, locale = locale)
                        }
                        val mm = workoutSec / 60
                        val ss = workoutSec % 60
                        Text(
                            text = "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}",
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize, fontWeight = FontWeight.Bold),
                            color = c.ink,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TibbPrimaryPillButton(
                                onClick = { workoutRunning = !workoutRunning },
                                modifier = Modifier.testTag("workout_timer_toggle"),
                            ) {
                                Icon(if (workoutRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (workoutRunning) "Pause" else "Start Session")
                            }
                            TibbSecondaryPillButton(
                                onClick = {
                                    workoutRunning = false
                                    workoutSec = 0
                                },
                            ) { Text("Reset") }
                            TibbSecondaryPillButton(
                                onClick = { onUpdateWellness(todayLog.copy(workoutDone = !todayLog.workoutDone)) },
                                borderColor = if (todayLog.workoutDone) c.safeGreen else c.border,
                                contentColor = if (todayLog.workoutDone) c.safeGreen else c.ink,
                                modifier = Modifier.testTag("workout_mark_done_btn"),
                            ) {
                                Text(if (todayLog.workoutDone) "Done Today" else "Mark Done")
                            }
                        }
                    }
                }

                // Collapsed-by-default Exercise Routine List
                TibbExpandableSection(
                    title = if (isUr) "آج کی ورزش کے مراحل" else "Daily Exercise Routine",
                    summary = "${plan.workoutRoutine.size} exercises",
                    icon = Icons.Rounded.FitnessCenter,
                    initiallyExpanded = false,
                    headerTestTag = "coach_workout_routine_expand",
                ) {
                    plan.workoutRoutine.forEach { ex ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surfaceMuted.copy(alpha = 0.65f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(if (isUr) ex.name_ur else ex.name_en, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                                Text(ex.intensity, style = MaterialTheme.typography.labelSmall, color = c.accent)
                            }
                            Text(ex.durationOrReps, style = TabularNumberStyle, color = c.accent)
                            Text(ex.tip_en, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                }
            }

            2 -> {
                val categories = listOf(
                    "ALL" to "All",
                    "FEVER_THROAT" to "Fever & Throat",
                    "STOMACH_HYDRATION" to "Stomach & ORS",
                    "HEAT_SKIN" to "Heat & Skin",
                    "PAIN_RECOVERY" to "Pain & Sprains",
                    "STRESS_SLEEP" to "Sleep & Stress",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isUr) "مستند گھریلو علاج" else "Verified Home Remedies",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    SectionHelpButton(topic = HelpTopic.COACH_REMEDIES, locale = locale)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { (key, lbl) ->
                        val sel = remedyFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surface)
                                .border(1.dp, if (sel) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                .clickable { remedyFilter = key }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                        }
                    }
                }

                val list = remember(remedyFilter) {
                    if (remedyFilter == "ALL") RemediesAndPlans.HOME_REMEDIES_PAKISTAN
                    else RemediesAndPlans.HOME_REMEDIES_PAKISTAN.filter { it.category == remedyFilter }
                }

                // Progressive disclosure: each remedy collapsed by default into an expandable card
                list.forEach { rem ->
                    val title = if (isUr) rem.title_ur else rem.title_en
                    val ingredients = if (isUr) rem.ingredients_ur else rem.ingredients_en
                    TibbExpandableSection(
                        title = title,
                        summary = ingredients,
                        icon = Icons.Rounded.Spa,
                        expanded = expandedRemedyId == rem.title_en,
                        onExpandedChange = { open -> expandedRemedyId = if (open) rem.title_en else null },
                    ) {
                        val steps = if (isUr) rem.steps_ur else rem.steps_en
                        steps.forEachIndexed { idx, step ->
                            Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.amberSoft)
                                .padding(10.dp),
                        ) {
                            Text(
                                text = if (isUr) rem.caution_ur else rem.caution_en,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.amber,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
