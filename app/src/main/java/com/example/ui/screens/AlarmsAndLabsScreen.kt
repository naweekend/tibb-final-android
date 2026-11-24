package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AlarmFrequency
import com.example.data.model.AppLocale
import com.example.data.model.PatientProfile
import com.example.data.model.ReminderType
import com.example.data.model.StoredAlarm
import com.example.data.model.StoredLabRecord
import com.example.engine.AndroidSensorsManager
import com.example.engine.AudioFeedbackManager
import com.example.engine.HealthMath
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.components.SectionHelpButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmsAndLabsScreen(
    locale: AppLocale,
    alarmsSubMode: Int, // 0 = Medicine Alarms & PDF, 1 = Lab OCR
    profiles: List<PatientProfile>,
    activePerson: PatientProfile?,
    alarms: List<StoredAlarm>,
    labs: List<StoredLabRecord>,
    audioManager: AudioFeedbackManager,
    onSetSubMode: (Int) -> Unit,
    onAddAlarm: (String, ReminderType, String, String, String?, AlarmFrequency, String?) -> Unit,
    onMarkAlarmTaken: (String) -> Unit,
    onExportWeeklyPdf: () -> Unit,
    onSaveLabRecord: (StoredLabRecord) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val selectedPatient = activePerson ?: profiles.firstOrNull()

    // Alarm state
    var reminderType by remember { mutableStateOf(ReminderType.MEDICINE) }
    var alarmLabel by remember { mutableStateOf("") }
    var alarmNotes by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(AlarmFrequency.DAILY) }
    var hour12 by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var period by remember { mutableStateOf("AM") }
    var secondHour12 by remember { mutableIntStateOf(8) }
    var secondPeriod by remember { mutableStateOf("PM") }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) onShowToast("Notification permission denied; alarms still saved in vault.")
    }

    // Lab OCR state
    var ocrBusy by remember { mutableStateOf(false) }
    var sourceKind by remember { mutableStateOf("MANUAL") }
    var extractedText by remember { mutableStateOf("") }
    var hb by remember { mutableStateOf("") }
    var wbc by remember { mutableStateOf("") }
    var plt by remember { mutableStateOf("") }
    var fbs by remember { mutableStateOf("") }
    var hba1c by remember { mutableStateOf("") }
    var creat by remember { mutableStateOf("") }
    var chol by remember { mutableStateOf("") }
    var alt by remember { mutableStateOf("") }

    fun applyParsedBiomarkers(rawText: String, kind: String) {
        extractedText = rawText
        sourceKind = kind
        val parsed = HealthMath.extractBiomarkers(rawText)
        parsed.hb?.let { hb = it }
        parsed.wbc?.let { wbc = it }
        parsed.plt?.let { plt = it }
        parsed.fbs?.let { fbs = it }
        parsed.hba1c?.let { hba1c = it }
        parsed.creatinine?.let { creat = it }
        parsed.cholesterol?.let { chol = it }
        parsed.altSgpt?.let { alt = it }
    }

    val labPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ocrBusy = true
            scope.launch {
                AndroidSensorsManager.runOfflineLabOcr(context, uri, isPdf = false).fold(
                    onSuccess = { txt ->
                        applyParsedBiomarkers(txt, "PHOTO_OCR")
                        onShowToast("Offline ML Kit OCR completed.")
                    },
                    onFailure = { err -> onShowToast("OCR error: ${err.message}") },
                )
                ocrBusy = false
            }
        }
    }

    val labPdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ocrBusy = true
            scope.launch {
                AndroidSensorsManager.runOfflineLabOcr(context, uri, isPdf = true).fold(
                    onSuccess = { txt ->
                        applyParsedBiomarkers(txt, "PDF_OCR")
                        onShowToast("Offline PDF OCR completed.")
                    },
                    onFailure = { err -> onShowToast("PDF OCR error: ${err.message}") },
                )
                ocrBusy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sub-mode switcher
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
                0 to (if (isUr) "دوا کے الارم و PDF" else "Alarms & Weekly PDF"),
                1 to (if (isUr) "لیب رپورٹ سکینر" else "Lab Report OCR"),
            ).forEach { (idx, lbl) ->
                val sel = alarmsSubMode == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) c.ctaBg else Color.Transparent)
                        .clickable { onSetSubMode(idx) }
                        .padding(vertical = 10.dp)
                        .testTag("alarms_subtab_$idx"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelLarge, color = if (sel) c.ctaInk else c.ink)
                }
            }
        }

        if (alarmsSubMode == 0) {
            // Alarms & Weekly PDF
            TibbGlassCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = c.accent)
                            Column {
                                Text(s.alarmsTitle, style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium, color = c.ink)
                                Text(s.alarmsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                        SectionHelpButton(topic = HelpTopic.ALARMS_MEDICINE, locale = locale)
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReminderType.entries.forEach { rt ->
                            val sel = reminderType == rt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.ctaBg else c.surfaceMuted)
                                    .clickable { reminderType = rt }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(rt.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Medicine / Reminder Label (e.g. Morning Tablet, Walk)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("alarm_label_input"),
                    )
                    OutlinedTextField(
                        value = alarmNotes,
                        onValueChange = { alarmNotes = it },
                        label = { Text("Optional Note (e.g. After breakfast)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 12-hour picker
                    Text("Time (12h AM/PM)", style = MaterialTheme.typography.labelLarge, color = c.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TibbSecondaryPillButton(onClick = { hour12 = if (hour12 <= 1) 12 else hour12 - 1 }) { Text("-") }
                        Text(hour12.toString().padStart(2, '0'), style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        TibbSecondaryPillButton(onClick = { hour12 = if (hour12 >= 12) 1 else hour12 + 1 }) { Text("+") }
                        Text(":", style = TabularNumberStyle, color = c.ink)
                        TibbSecondaryPillButton(onClick = { minute = (minute + 45) % 60 }) { Text("-") }
                        Text(minute.toString().padStart(2, '0'), style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        TibbSecondaryPillButton(onClick = { minute = (minute + 15) % 60 }) { Text("+") }
                        TibbSecondaryPillButton(onClick = { period = if (period == "AM") "PM" else "AM" }) { Text(period) }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlarmFrequency.entries.forEach { freq ->
                            val sel = frequency == freq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.ctaBg else c.surfaceMuted)
                                    .clickable { frequency = freq }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(freq.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                            }
                        }
                    }

                    if (frequency == AlarmFrequency.TWICE_DAILY) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("2nd Time:", style = MaterialTheme.typography.labelMedium, color = c.muted)
                            TibbSecondaryPillButton(onClick = { secondHour12 = if (secondHour12 <= 1) 12 else secondHour12 - 1 }) { Text("-") }
                            Text("${secondHour12.toString().padStart(2, '0')}:00 $secondPeriod", style = TabularNumberStyle, color = c.ink)
                            TibbSecondaryPillButton(onClick = { secondHour12 = if (secondHour12 >= 12) 1 else secondHour12 + 1 }) { Text("+") }
                            TibbSecondaryPillButton(onClick = { secondPeriod = if (secondPeriod == "AM") "PM" else "AM" }) { Text(secondPeriod) }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TibbSecondaryPillButton(
                            onClick = { audioManager.speakOfflineGuidance(TrilingualStrings.URDU_MEDICINE_REMINDER, "ur-PK") },
                            modifier = Modifier.testTag("alarm_test_urdu_voice"),
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.alarmsTestVoiceBtn, style = MaterialTheme.typography.labelSmall)
                        }
                        TibbPrimaryPillButton(
                            enabled = alarmLabel.isNotBlank() && selectedPatient != null,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                val time24 = HealthMath.to24hTime(hour12, minute, period)
                                val time24Second = if (frequency == AlarmFrequency.TWICE_DAILY) {
                                    HealthMath.to24hTime(secondHour12, 0, secondPeriod)
                                } else null
                                selectedPatient?.let { pat ->
                                    onAddAlarm(pat.id, reminderType, alarmLabel, time24, time24Second, frequency, alarmNotes)
                                    alarmLabel = ""
                                    alarmNotes = ""
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("alarm_schedule_btn"),
                        ) {
                            Text(s.alarmsAddBtn)
                        }
                    }
                }
            }

            // Scheduled Alarms
            Text(s.alarmsScheduledTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
            val scopedAlarms = remember(alarms, selectedPatient) {
                if (selectedPatient == null) alarms else alarms.filter { it.patientId == selectedPatient.id }
            }
            if (scopedAlarms.isEmpty()) {
                Text(s.alarmsEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
            } else {
                scopedAlarms.forEach { alm ->
                    val takenToday = alm.lastTakenDate == todayKey
                    TibbGlassCard(padding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${alm.medicineLabel} (${alm.reminderType.name.replace('_', ' ')})", style = MaterialTheme.typography.titleMedium, color = c.ink)
                                val timeStr = HealthMath.formatTime12h(alm.time24h) + (alm.secondTime24h?.let { " & ${HealthMath.formatTime12h(it)}" } ?: "")
                                Text("$timeStr · ${alm.frequency.name.replace('_', ' ')} · Taken ${alm.takenCount}x", style = TabularNumberStyle, color = c.muted)
                            }
                            if (takenToday) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.safeGreen, modifier = Modifier.size(18.dp))
                                    Text(s.alarmsTakenToday, style = MaterialTheme.typography.labelSmall, color = c.safeGreen)
                                }
                            } else {
                                TibbPrimaryPillButton(
                                    onClick = { onMarkAlarmTaken(alm.id) },
                                    modifier = Modifier.testTag("alarm_mark_taken_${alm.id}"),
                                ) {
                                    Text(s.alarmsMarkTaken)
                                }
                            }
                        }
                    }
                }
            }

            // Weekly Digest PDF Card
            TibbGlassCard(padding = 16.dp, borderColor = c.accent.copy(alpha = 0.45f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = c.accent)
                            Text(s.weeklyDigestTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                        }
                        SectionHelpButton(topic = HelpTopic.ALARMS_MEDICINE, locale = locale)
                    }
                    Text(s.weeklyDigestSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    TibbPrimaryPillButton(
                        onClick = onExportWeeklyPdf,
                        modifier = Modifier.fillMaxWidth().testTag("export_weekly_pdf_btn"),
                    ) {
                        Text(s.exportPdfBtn)
                    }
                }
            }
        } else {
            // Lab Report OCR
            TibbGlassCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = c.accent)
                            Column {
                                Text(s.labsTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                                Text(s.labsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                        SectionHelpButton(topic = HelpTopic.LAB_OCR, locale = locale)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TibbSecondaryPillButton(
                            enabled = !ocrBusy,
                            onClick = { labPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f).testTag("lab_ocr_photo_btn"),
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (ocrBusy) "Scanning..." else "Photo OCR", style = MaterialTheme.typography.labelSmall)
                        }
                        TibbSecondaryPillButton(
                            enabled = !ocrBusy,
                            onClick = { labPdfPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.weight(1f).testTag("lab_ocr_pdf_btn"),
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (ocrBusy) "Scanning..." else "PDF OCR", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { applyParsedBiomarkers(it, sourceKind) },
                        label = { Text("Extracted / Pasted Lab Text (Auto-parses below)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("lab_raw_text_input"),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hb, onValueChange = { hb = it }, label = { Text("Hb (g/dL)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = wbc, onValueChange = { wbc = it }, label = { Text("WBC") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = plt, onValueChange = { plt = it }, label = { Text("Platelets") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fbs, onValueChange = { fbs = it }, label = { Text("Sugar mg/dL") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hba1c, onValueChange = { hba1c = it }, label = { Text("HbA1c %") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = creat, onValueChange = { creat = it }, label = { Text("Creatinine") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = chol, onValueChange = { chol = it }, label = { Text("Cholesterol") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = alt, onValueChange = { alt = it }, label = { Text("ALT / SGPT") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    TibbPrimaryPillButton(
                        enabled = selectedPatient != null && (hb.isNotBlank() || wbc.isNotBlank() || plt.isNotBlank() || fbs.isNotBlank() || hba1c.isNotBlank() || creat.isNotBlank() || chol.isNotBlank() || alt.isNotBlank() || extractedText.isNotBlank()),
                        onClick = {
                            selectedPatient?.let { pat ->
                                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    .format(Date())
                                onSaveLabRecord(
                                    StoredLabRecord(
                                        id = "lab_${UUID.randomUUID().toString().take(10)}",
                                        patientId = pat.id,
                                        scannedAtIso = nowIso,
                                        hemoglobin = hb.ifBlank { null },
                                        wbc = wbc.ifBlank { null },
                                        platelets = plt.ifBlank { null },
                                        fastingGlucose = fbs.ifBlank { null },
                                        hba1c = hba1c.ifBlank { null },
                                        creatinine = creat.ifBlank { null },
                                        cholesterol = chol.ifBlank { null },
                                        altSgpt = alt.ifBlank { null },
                                        sourceFileType = sourceKind,
                                        excerpt = extractedText.take(240),
                                    )
                                )
                                extractedText = ""
                                hb = ""; wbc = ""; plt = ""; fbs = ""; hba1c = ""; creat = ""; chol = ""; alt = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("lab_save_btn"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.labsSaveBtn)
                    }
                }
            }

            Text(s.labsHistoryTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
            val scopedLabs = remember(labs, selectedPatient) {
                if (selectedPatient == null) labs else labs.filter { it.patientId == selectedPatient.id }
            }
            if (scopedLabs.isEmpty()) {
                Text(s.labsEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
            } else {
                scopedLabs.forEach { rec ->
                    TibbGlassCard(padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${rec.scannedAtIso.take(10)} · ${rec.sourceFileType}", style = MaterialTheme.typography.labelLarge, color = c.accent)
                            Text(
                                "Hb: ${rec.hemoglobin ?: "--"} | WBC: ${rec.wbc ?: "--"} | PLT: ${rec.platelets ?: "--"} | Sugar: ${rec.fastingGlucose ?: "--"} | HbA1c: ${rec.hba1c ?: "--"}",
                                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                color = c.ink,
                            )
                            Text(
                                "Creat: ${rec.creatinine ?: "--"} | Chol: ${rec.cholesterol ?: "--"} | ALT: ${rec.altSgpt ?: "--"}",
                                style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                color = c.muted,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
