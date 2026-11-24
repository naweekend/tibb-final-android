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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalHospital
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.DailyWellnessLog
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.WellnessGoal
import com.example.engine.RemediesAndPlans
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
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

    var subTab by remember { mutableIntStateOf(0) } // 0 = Diet Plan, 1 = Workout & Steps, 2 = Home Remedies
    var selectedGoal by remember(activePerson.id) { mutableStateOf(activePerson.preferredGoal) }
    var variationSeed by remember { mutableIntStateOf(0) }
    var remedyFilter by remember { mutableStateOf("ALL") }

    // Interactive Workout Stopwatch
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
        // BMI Hero Summary Card
        TibbGlassCard(padding = 16.dp, borderColor = c.accent.copy(alpha = 0.45f)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isUr) "جنوبی ایشیائی بی ایم آئی اور ہدف" else "South Asian BMI & Goal",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                    if (bmi.bmi != null) {
                        Text("BMI ${bmi.bmi}", style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.accent)
                    }
                }
                Text(if (isUr) bmi.labelUr else bmi.labelEn, style = MaterialTheme.typography.labelLarge, color = c.accent)
                Text(if (isUr) bmi.adviceUr else bmi.adviceEn, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // Subtabs
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
                0 to (if (isUr) "غذا پلان" else "Diet Plan"),
                1 to (if (isUr) "ورزش" else "Workout"),
                2 to (if (isUr) "گھریلو علاج" else "Remedies"),
            ).forEach { (idx, lbl) ->
                val sel = subTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) c.ctaBg else Color.Transparent)
                        .clickable { subTab = idx }
                        .padding(vertical = 9.dp)
                        .testTag("coach_subtab_$idx"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelMedium, color = if (sel) c.ctaInk else c.ink)
                }
            }
        }

        when (subTab) {
            0 -> {
                // Goal Selector + Rotate Button
                Text(if (isUr) "صحت کا ہدف منتخب کریں" else "Select Wellness Goal", style = MaterialTheme.typography.labelLarge, color = c.ink)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WellnessGoal.entries.forEach { g ->
                        val sel = selectedGoal == g
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surface)
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

                // Meal Cards with Swap + Mark Eaten
                plan.dietMeals.forEachIndexed { slotIdx, meal ->
                    val eaten = todayLog.mealsChecked.contains(meal.slot)
                    TibbGlassCard(padding = 16.dp, borderColor = if (eaten) c.safeGreen else c.border) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                                    Text("${meal.slot} · ~${meal.caloriesApprox} kcal", style = TabularNumberStyle.copy(fontWeight = FontWeight.SemiBold), color = c.accent)
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
                                        modifier = Modifier.testTag("meal_swap_${meal.slot.lowercase().replace(' ', '_')}"),
                                    ) {
                                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Swap (${meal.selectedOptionIndex + 1}/${meal.alternatives.size})", style = MaterialTheme.typography.labelSmall)
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

                // Medical / Allergy Safeguard Notes
                TibbGlassCard(padding = 14.dp, borderColor = c.amber.copy(alpha = 0.45f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.amber, modifier = Modifier.size(16.dp))
                            Text(if (isUr) "احتیاطی غذائی اصول" else "Condition & Allergy Safeguards", style = MaterialTheme.typography.titleMedium, color = c.amber)
                        }
                        val notes = if (isUr) plan.avoidNotes_ur else plan.avoidNotes_en
                        notes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }
                }
            }

            1 -> {
                // Live Workout Stopwatch
                TibbGlassCard(padding = 18.dp, borderColor = c.accent.copy(alpha = 0.45f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = c.accent)
                            Text("Workout Session Timer", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        }
                        val mm = workoutSec / 60
                        val ss = workoutSec % 60
                        Text(
                            text = "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}",
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize, fontWeight = FontWeight.Bold),
                            color = c.ink,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                plan.workoutRoutine.forEach { ex ->
                    TibbGlassCard(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(if (isUr) ex.name_ur else ex.name_en, style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
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
                    "HEAT_SKIN" to "Heat, Burns & Cuts",
                    "PAIN_RECOVERY" to "Pain & Sprains",
                    "STRESS_SLEEP" to "Sleep & Stress",
                )
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

                list.forEach { rem ->
                    TibbGlassCard(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Spa, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isUr) rem.title_ur else rem.title_en,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                                    color = c.ink,
                                )
                            }
                            Text(
                                text = if (isUr) rem.ingredients_ur else rem.ingredients_en,
                                style = MaterialTheme.typography.labelMedium,
                                color = c.accent,
                            )
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
        }
        Spacer(Modifier.height(14.dp))
    }
}
