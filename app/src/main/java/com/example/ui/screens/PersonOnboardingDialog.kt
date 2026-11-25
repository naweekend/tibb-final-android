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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.BloodGroup
import com.example.data.model.FrequentPlace
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.RelationshipType
import com.example.data.model.UnitSystem
import com.example.data.model.WellnessGoal
import com.example.data.model.WorkspaceMode
import com.example.engine.HealthMath
import com.example.engine.RemediesAndPlans
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import java.util.UUID
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonOnboardingDialog(
    locale: AppLocale,
    workspaceMode: WorkspaceMode,
    initialUnitSystem: UnitSystem,
    editingProfile: PatientProfile?,
    canCancel: Boolean,
    triageEngine: TriageEngine,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
    onSaveProfile: (PatientProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    BackHandler(enabled = canCancel) { onDismiss() }

    var step by remember { mutableIntStateOf(1) }

    // Step 1
    var relationship by remember { mutableStateOf(editingProfile?.relationship ?: RelationshipType.Self) }
    var name by remember { mutableStateOf(editingProfile?.name.orEmpty()) }
    var ageIsMonths by remember { mutableStateOf((editingProfile?.ageMonths ?: 300) < 24) }
    var ageText by remember {
        mutableStateOf(
            editingProfile?.let {
                if (it.ageMonths < 24) it.ageMonths.toString() else (it.ageMonths / 12).toString()
            } ?: "28"
        )
    }
    var guardianConsent by remember { mutableStateOf(true) }
    var sex by remember { mutableStateOf(editingProfile?.sex ?: "O") }
    var bloodGroup by remember { mutableStateOf(editingProfile?.bloodGroup ?: BloodGroup.Unknown) }

    // Step 2
    var unitSystem by remember { mutableStateOf(editingProfile?.preferredUnitSystem ?: initialUnitSystem) }
    var heightCmText by remember { mutableStateOf(editingProfile?.heightCm?.let { round(it).toInt().toString() } ?: "168") }
    var weightKgText by remember { mutableStateOf(editingProfile?.weightKg?.toString() ?: "66") }
    var feetText by remember {
        mutableStateOf(editingProfile?.heightCm?.let { HealthMath.cmToFtIn(it).first.toString() } ?: "5")
    }
    var inchesText by remember {
        mutableStateOf(editingProfile?.heightCm?.let { HealthMath.cmToFtIn(it).second.toString() } ?: "6")
    }
    var lbsText by remember {
        mutableStateOf(editingProfile?.weightKg?.let { HealthMath.kgToLbs(it).toString() } ?: "145")
    }
    var goal by remember { mutableStateOf(editingProfile?.preferredGoal ?: WellnessGoal.BALANCED) }
    val dietPrefs = remember {
        mutableStateListOf<String>().apply { addAll(editingProfile?.dietaryPreferences.orEmpty()) }
    }

    // Step 3
    val chronic = remember { mutableStateListOf<String>().apply { addAll(editingProfile?.chronicConditions.orEmpty()) } }
    var isPregnantOrLactating by remember { mutableStateOf(editingProfile?.isPregnantOrLactating ?: false) }
    val allergies = remember { mutableStateListOf<String>().apply { addAll(editingProfile?.allergies.orEmpty()) } }
    var customAllergy by remember { mutableStateOf("") }
    var dailyMedicines by remember { mutableStateOf(editingProfile?.dailyMedicinesNote.orEmpty()) }
    var priorLabNotes by remember { mutableStateOf(editingProfile?.priorLabNotes.orEmpty()) }
    var emergencyContact by remember { mutableStateOf(editingProfile?.emergencyContact.orEmpty()) }

    // Step 4
    val places = remember { mutableStateListOf<FrequentPlace>().apply { addAll(editingProfile?.frequentPlaces.orEmpty()) } }

    val resolvedAgeMonths = remember(ageText, ageIsMonths) {
        val raw = ageText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        if (ageIsMonths) raw else raw * 12
    }
    val resolvedHeightCm: Double? = remember(unitSystem, heightCmText, feetText, inchesText) {
        if (unitSystem == UnitSystem.METRIC) heightCmText.toDoubleOrNull()?.takeIf { it in 40.0..240.0 }
        else HealthMath.ftInToCm(feetText.toIntOrNull() ?: 0, inchesText.toIntOrNull() ?: 0).takeIf { it > 40.0 }
    }
    val resolvedWeightKg: Double? = remember(unitSystem, weightKgText, lbsText) {
        if (unitSystem == UnitSystem.METRIC) weightKgText.toDoubleOrNull()?.takeIf { it in 2.0..250.0 }
        else lbsText.toDoubleOrNull()?.let { HealthMath.lbsToKg(it) }?.takeIf { it in 2.0..250.0 }
    }
    val bmiCheck = remember(resolvedWeightKg, resolvedHeightCm, resolvedAgeMonths) {
        RemediesAndPlans.computeBmiAssessment(resolvedWeightKg, resolvedHeightCm, resolvedAgeMonths)
    }

    val needsGuardian = resolvedAgeMonths in 1 until (18 * 12)
    val canProceedStep1 = name.trim().length >= 2 && resolvedAgeMonths > 0 && (!needsGuardian || guardianConsent)

    fun switchUnits(next: UnitSystem) {
        if (next == unitSystem) return
        if (next == UnitSystem.IMPERIAL) {
            heightCmText.toDoubleOrNull()?.takeIf { it > 40 }?.let { cm ->
                val (f, i) = HealthMath.cmToFtIn(cm)
                feetText = f.toString()
                inchesText = i.toString()
            }
            weightKgText.toDoubleOrNull()?.takeIf { it > 2 }?.let { kg ->
                lbsText = HealthMath.kgToLbs(kg).toString()
            }
        } else {
            val cm = HealthMath.ftInToCm(feetText.toIntOrNull() ?: 5, inchesText.toIntOrNull() ?: 6)
            if (cm > 40) heightCmText = round(cm).toInt().toString()
            lbsText.toDoubleOrNull()?.takeIf { it > 5 }?.let { lbs ->
                weightKgText = HealthMath.lbsToKg(lbs).toString()
            }
        }
        unitSystem = next
    }

    fun finishSave() {
        val profile = PatientProfile(
            id = editingProfile?.id ?: "pat_${UUID.randomUUID().toString().take(10)}",
            accountId = editingProfile?.accountId ?: "acc_local_family_default",
            name = name.trim(),
            ageMonths = resolvedAgeMonths.coerceAtLeast(1),
            sex = sex,
            isPregnantOrLactating = if (sex == "F" && resolvedAgeMonths >= 12 * 12) isPregnantOrLactating else false,
            chronicConditions = chronic.toList(),
            allergies = allergies.toList(),
            workspace = editingProfile?.workspace ?: workspaceMode,
            relationship = relationship,
            bloodGroup = bloodGroup,
            heightCm = resolvedHeightCm,
            weightKg = resolvedWeightKg,
            baselineWeightKg = editingProfile?.baselineWeightKg ?: resolvedWeightKg,
            preferredUnitSystem = unitSystem,
            preferredGoal = goal,
            dietaryPreferences = dietPrefs.toList(),
            priorLabNotes = priorLabNotes.trim(),
            frequentPlaces = places.toList(),
            onboardingCompleted = true,
            dailyMedicinesNote = dailyMedicines.trim(),
            emergencyContact = emergencyContact.trim(),
            createdAtIso = editingProfile?.createdAtIso.orEmpty(),
        )
        onSaveProfile(profile)
    }

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
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Header & Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingProfile != null) s.editProfileBtn else s.onboardingTitle,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                            color = c.ink,
                        )
                        Text(
                            text = "Step $step / 4 · ${(step * 25)}%",
                            style = TabularNumberStyle.copy(color = c.accent),
                        )
                    }
                    if (canCancel) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("onboarding_close_btn")) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Onboarding", tint = c.muted)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = c.accent,
                    trackColor = c.surfaceMuted,
                )
                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (step) {
                        1 -> {
                            Text(s.onboardingSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)

                            Text(if (isUr) "رشتہ / حیثیت" else "Relationship", style = MaterialTheme.typography.labelLarge, color = c.ink)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RelationshipType.entries.forEach { rel ->
                                    val sel = relationship == rel
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (sel) c.ctaBg else c.surfaceMuted)
                                            .border(1.dp, if (sel) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                            .clickable { relationship = rel }
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                            .testTag("onboard_rel_${rel.name.lowercase()}"),
                                    ) {
                                        Text(
                                            text = if (isUr) rel.labelUr else rel.labelEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (sel) c.ctaInk else c.ink,
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(if (isUr) "پورا نام" else "Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboard_name_input"),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = ageText,
                                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) ageText = it },
                                    label = { Text(if (ageIsMonths) "Age (Months)" else "Age (Years)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("onboard_age_input"),
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(c.surfaceMuted)
                                        .padding(4.dp),
                                ) {
                                    listOf(false to "Yrs", true to "Mos").forEach { (mos, lbl) ->
                                        val sel = ageIsMonths == mos
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                                .clickable { ageIsMonths = mos }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                        ) {
                                            Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            if (needsGuardian) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.surfaceMuted)
                                        .clickable { guardianConsent = !guardianConsent }
                                        .padding(8.dp),
                                ) {
                                    Checkbox(checked = guardianConsent, onCheckedChange = { guardianConsent = it })
                                    Text(
                                        text = if (isUr) "والدین یا سرپرست کی اجازت سے ریکارڈ محفوظ کیا جا رہا ہے"
                                        else "Guardian / parent consent confirmed for minor (< 18 years).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.ink,
                                    )
                                }
                            }

                            Text(if (isUr) "جنس" else "Biological Sex", style = MaterialTheme.typography.labelLarge, color = c.ink)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("M" to "Male", "F" to "Female", "O" to "Other").forEach { (code, lbl) ->
                                    val sel = sex == code
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) c.ctaBg else c.surfaceMuted)
                                            .clickable { sex = code }
                                            .padding(horizontal = 8.dp, vertical = 12.dp)
                                            .testTag("onboard_sex_$code"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = lbl,
                                            color = if (sel) c.ctaInk else c.ink,
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }

                            Text(if (isUr) "بلڈ گروپ" else "Blood Group", style = MaterialTheme.typography.labelLarge, color = c.ink)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                BloodGroup.entries.forEach { bg ->
                                    val sel = bloodGroup == bg
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) c.accent.copy(alpha = 0.25f) else c.surfaceMuted)
                                            .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(12.dp))
                                            .clickable { bloodGroup = bg }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(bg.label, color = c.ink, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        2 -> {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = if (isUr) "جسم کی پیمائش اور ہدف" else "Height, Weight & Goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = c.ink,
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(c.surfaceMuted)
                                        .padding(3.dp),
                                ) {
                                    listOf(UnitSystem.METRIC to "cm / kg", UnitSystem.IMPERIAL to "ft+in / lbs").forEach { (u, lbl) ->
                                        val sel = unitSystem == u
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                                .clickable { switchUnits(u) }
                                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                                .testTag("onboard_unit_${u.name.lowercase()}"),
                                        ) {
                                            Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            if (unitSystem == UnitSystem.METRIC) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = heightCmText,
                                        onValueChange = { heightCmText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = weightKgText,
                                        onValueChange = { weightKgText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    if (maxWidth >= 440.dp) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = feetText,
                                                onValueChange = { feetText = it.filter(Char::isDigit) },
                                                label = { Text("Feet") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                            )
                                            OutlinedTextField(
                                                value = inchesText,
                                                onValueChange = { inchesText = it.filter(Char::isDigit) },
                                                label = { Text("Inches") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                            )
                                            OutlinedTextField(
                                                value = lbsText,
                                                onValueChange = { lbsText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                                label = { Text("Weight (lbs)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1.3f),
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = feetText,
                                                    onValueChange = { feetText = it.filter(Char::isDigit) },
                                                    label = { Text("Feet") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                OutlinedTextField(
                                                    value = inchesText,
                                                    onValueChange = { inchesText = it.filter(Char::isDigit) },
                                                    label = { Text("Inches") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                            OutlinedTextField(
                                                value = lbsText,
                                                onValueChange = { lbsText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                                label = { Text("Weight (lbs)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }

                            TibbGlassCard(padding = 14.dp, borderColor = c.accent.copy(alpha = 0.4f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isUr) bmiCheck.labelUr else bmiCheck.labelEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = c.accent,
                                    )
                                    Text(
                                        text = if (isUr) bmiCheck.adviceUr else bmiCheck.adviceEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.ink,
                                    )
                                }
                            }

                            Text(if (isUr) "صحت کا ہدف" else "Wellness Goal", style = MaterialTheme.typography.labelLarge, color = c.ink)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                WellnessGoal.entries.forEach { g ->
                                    val sel = goal == g
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (sel) c.ctaBg else c.surfaceMuted)
                                            .clickable { goal = g }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                    ) {
                                        Text(
                                            text = if (isUr) g.titleUr else g.titleEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (sel) c.ctaInk else c.ink,
                                        )
                                    }
                                }
                            }

                            Text(if (isUr) "غذائی ترجیحات" else "Dietary Preferences", style = MaterialTheme.typography.labelLarge, color = c.ink)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("Vegetarian", "Low-Salt", "Sugar-Free", "Dairy-Free", "High-Protein").forEach { pref ->
                                    val sel = dietPrefs.contains(pref)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (sel) c.accent.copy(alpha = 0.24f) else c.surfaceMuted)
                                            .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(999.dp))
                                            .clickable { if (sel) dietPrefs.remove(pref) else dietPrefs.add(pref) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(pref, style = MaterialTheme.typography.labelMedium, color = c.ink)
                                    }
                                }
                            }
                        }

                        3 -> OnboardingStep3Medical(
                            locale = locale,
                            sex = sex,
                            ageMonths = resolvedAgeMonths,
                            chronic = chronic,
                            isPregnantOrLactating = isPregnantOrLactating,
                            onTogglePregnant = { isPregnantOrLactating = it },
                            allergies = allergies,
                            customAllergy = customAllergy,
                            onCustomAllergyChange = { customAllergy = it },
                            dailyMedicines = dailyMedicines,
                            onDailyMedicinesChange = { dailyMedicines = it },
                            priorLabNotes = priorLabNotes,
                            onPriorLabNotesChange = { priorLabNotes = it },
                            emergencyContact = emergencyContact,
                            onEmergencyContactChange = { emergencyContact = it },
                        )

                        4 -> OnboardingStep4Places(
                            locale = locale,
                            triageEngine = triageEngine,
                            places = places,
                            userGpsCoords = userGpsCoords,
                            gpsLocating = gpsLocating,
                            onRequestGps = onRequestGps,
                        )
                    }
                }

                // Bottom Navigation Buttons
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (step > 1) {
                        TibbSecondaryPillButton(
                            onClick = { step-- },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 50.dp).testTag("onboarding_back_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                            Spacer(Modifier.padding(2.dp))
                            Text(if (isUr) "پیچھے" else "Back", textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    TibbPrimaryPillButton(
                        enabled = if (step == 1) canProceedStep1 else true,
                        onClick = {
                            if (step < 4) step++ else finishSave()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.4f).fillMaxHeight().heightIn(min = 50.dp).testTag("onboarding_next_save_btn"),
                    ) {
                        Text(
                            text = if (step < 4) (if (isUr) "اگلا مرحلہ" else "Continue")
                            else (if (isUr) "پروفائل محفوظ کریں" else "Save Profile"),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.padding(4.dp))
                        Icon(
                            imageVector = if (step < 4) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
