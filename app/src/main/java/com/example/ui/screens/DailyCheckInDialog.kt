package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.DailyDoctorCheckIn
import com.example.data.model.PatientProfile
import com.example.data.model.UnitSystem
import com.example.engine.HealthMath
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbPrimaryPillButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyCheckInDialog(
    person: PatientProfile,
    locale: AppLocale,
    unitSystem: UnitSystem,
    existingCheckIn: DailyDoctorCheckIn?,
    currentSleepHours: Double,
    onDismiss: () -> Unit,
    onSave: (DailyDoctorCheckIn) -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val isImperial = unitSystem == UnitSystem.IMPERIAL

    var weightText by remember {
        val base = existingCheckIn?.currentWeightKg ?: person.weightKg
        mutableStateOf(
            if (base == null) ""
            else if (isImperial) HealthMath.kgToLbs(base).toString()
            else (round(base * 10.0) / 10.0).toString()
        )
    }
    var feeling by remember { mutableStateOf(existingCheckIn?.weightFeeling ?: "SAME") }
    var sleepText by remember { mutableStateOf((existingCheckIn?.sleepHours ?: currentSleepHours).toString()) }
    var sleepQuality by remember { mutableIntStateOf(existingCheckIn?.sleepQuality ?: HealthMath.sleepHoursToQuality(currentSleepHours)) }
    var energy by remember { mutableStateOf(existingCheckIn?.energyLevel ?: "STEADY") }
    var symptomsNote by remember { mutableStateOf(existingCheckIn?.symptomsNote.orEmpty()) }

    val parsedKg = weightText.toDoubleOrNull()?.let { raw ->
        if (isImperial) HealthMath.lbsToKg(raw) else raw
    }
    val deltaKg = if (parsedKg != null && person.baselineWeightKg != null) {
        round((parsedKg - person.baselineWeightKg) * 10.0) / 10.0
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isUr) "آج کا صحت چیک ان (${person.name})" else "Your Daily Check-In (${person.name})")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (isImperial) "Today's Weight (lbs)" else "Today's Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checkin_weight_input"),
                )
                if (deltaKg != null) {
                    val sign = if (deltaKg > 0) "+" else ""
                    Text(
                        text = "Change from baseline: $sign$deltaKg kg",
                        style = TabularNumberStyle.copy(color = c.accent, fontWeight = FontWeight.SemiBold),
                    )
                }

                Text("How does your weight feel?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOSING" to "Losing", "SAME" to "Same", "GAINING" to "Gaining").forEach { (code, lbl) ->
                        val sel = feeling == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                .clickable { feeling = code }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = sleepText,
                    onValueChange = { sleepText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Hours Slept Last Night (e.g. 7.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5.0, 6.5, 7.5, 8.5).forEach { hrs ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surfaceMuted)
                                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                .clickable {
                                    sleepText = hrs.toString()
                                    sleepQuality = HealthMath.sleepHoursToQuality(hrs)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("${hrs}h", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("Sleep Quality (1–5)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { q ->
                        val sel = sleepQuality == q
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) c.accent else c.surfaceMuted)
                                .clickable { sleepQuality = q }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$q", color = if (sel) c.ctaInk else c.ink, style = TabularNumberStyle)
                        }
                    }
                }

                Text("Today's Energy Level", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW" to "Low", "STEADY" to "Steady", "STRONG" to "Strong").forEach { (code, lbl) ->
                        val sel = energy == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                .clickable { energy = code }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = symptomsNote,
                    onValueChange = { symptomsNote = it },
                    label = { Text("Brief Note (Headache, appetite, dizziness...)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TibbPrimaryPillButton(
                onClick = {
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .format(Date())
                    onSave(
                        DailyDoctorCheckIn(
                            answeredDateKey = todayKey,
                            currentWeightKg = parsedKg?.takeIf { it > 2.0 },
                            weightFeeling = feeling,
                            weightDeltaKg = deltaKg,
                            sleepHours = sleepText.toDoubleOrNull()?.let { HealthMath.sanitizeSleepHours(it) },
                            sleepQuality = sleepQuality,
                            energyLevel = energy,
                            symptomsNote = symptomsNote.trim().ifEmpty { null },
                            answeredAtIso = nowIso,
                        )
                    )
                },
                modifier = Modifier.testTag("checkin_save_btn"),
            ) {
                Text(if (isUr) "محفوظ کریں" else "Save Check-In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
