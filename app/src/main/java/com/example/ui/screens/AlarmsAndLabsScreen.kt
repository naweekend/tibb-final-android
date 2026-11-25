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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.TibbSegmentedTabs
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

    // Accordion section control (all collapsed by default)
    var openAlarmSection by remember { mutableStateOf<String?>(null) }
    var openLabSection by remember { mutableStateOf<String?>(null) }

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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Unified Sub-mode switcher
        TibbSegmentedTabs(
            options = listOf(
                0 to (if (isUr) "دوا کے الارم و PDF" else "Alarms & Weekly PDF"),
                1 to (if (isUr) "لیب رپورٹ سکینر" else "Lab Report OCR"),
            ),
            selected = alarmsSubMode,
            onSelect = onSetSubMode,
            tagPrefix = "alarms_subtab",
        )

        if (alarmsSubMode == 0) {
            val scopedAlarms = remember(alarms, selectedPatient) {
                if (selectedPatient == null) alarms else alarms.filter { it.patientId == selectedPatient.id }
            }

            // Primary Card: Add Medicine / Routine Alarm
            TibbGlassCard(padding = 16.dp, backgroundColor = c.surfaceElevated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Text(s.alarmsTitle, style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge, color = c.ink)
                                Text(s.alarmsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                        SectionHelpButton(topic = HelpTopic.ALARMS_MEDICINE, locale = locale)
                    }

                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Medicine / Reminder Label (e.g. Morning Tablet, Walk)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("alarm_label_input"),
                    )

                    // 12-hour picker
                    val compactPickerPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    Text("Time (12h AM/PM)", style = MaterialTheme.typography.labelLarge, color = c.ink)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TibbSecondaryPillButton(
                            onClick = { hour12 = if (hour12 <= 1) 12 else hour12 - 1 },
                            contentPadding = compactPickerPadding,
                            modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                        ) { Text("-") }
                        Text(hour12.toString().padStart(2, '0'), style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        TibbSecondaryPillButton(
                            onClick = { hour12 = if (hour12 >= 12) 1 else hour12 + 1 },
                            contentPadding = compactPickerPadding,
                            modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                        ) { Text("+") }
                        Text(":", style = TabularNumberStyle, color = c.ink)
                        TibbSecondaryPillButton(
                            onClick = { minute = (minute + 45) % 60 },
                            contentPadding = compactPickerPadding,
                            modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                        ) { Text("-") }
                        Text(minute.toString().padStart(2, '0'), style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        TibbSecondaryPillButton(
                            onClick = { minute = (minute + 15) % 60 },
                            contentPadding = compactPickerPadding,
                            modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                        ) { Text("+") }
                        TibbSecondaryPillButton(
                            onClick = { period = if (period == "AM") "PM" else "AM" },
                            contentPadding = compactPickerPadding,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 44.dp),
                        ) { Text(period) }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlarmFrequency.entries.forEach { freq ->
                            val sel = frequency == freq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) c.accentSoft else c.surfaceMuted)
                                    .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(999.dp))
                                    .clickable { frequency = freq }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(freq.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.accent else c.ink)
                            }
                        }
                    }

                    if (frequency == AlarmFrequency.TWICE_DAILY) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("2nd Time:", style = MaterialTheme.typography.labelMedium, color = c.muted)
                            TibbSecondaryPillButton(
                                onClick = { secondHour12 = if (secondHour12 <= 1) 12 else secondHour12 - 1 },
                                contentPadding = compactPickerPadding,
                                modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                            ) { Text("-") }
                            Text("${secondHour12.toString().padStart(2, '0')}:00 $secondPeriod", style = TabularNumberStyle, color = c.ink)
                            TibbSecondaryPillButton(
                                onClick = { secondHour12 = if (secondHour12 >= 12) 1 else secondHour12 + 1 },
                                contentPadding = compactPickerPadding,
                                modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                            ) { Text("+") }
                            TibbSecondaryPillButton(
                                onClick = { secondPeriod = if (secondPeriod == "AM") "PM" else "AM" },
                                contentPadding = compactPickerPadding,
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 44.dp),
                            ) { Text(secondPeriod) }
                        }
                    }

                    // Collapsed by default additional reminder options (Type + Note)
                    TibbExpandableSection(
                        title = if (isUr) "اضافی تفصیلات (قسم اور نوٹ)" else "Additional Options (Category & Note)",
                        summary = "${reminderType.name.replace('_', ' ')}${if (alarmNotes.isNotBlank()) " · $alarmNotes" else ""}",
                        icon = Icons.Rounded.Tune,
                        expanded = openAlarmSection == "options",
                        onExpandedChange = { openAlarmSection = if (it) "options" else null },
                        headerTestTag = "alarms_options_expand",
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReminderType.entries.forEach { rt ->
                                val sel = reminderType == rt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (sel) c.accentSoft else c.surfaceMuted)
                                        .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(999.dp))
                                        .clickable { reminderType = rt }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                ) {
                                    Text(rt.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.accent else c.ink)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = alarmNotes,
                            onValueChange = { alarmNotes = it },
                            label = { Text("Optional Note (e.g. After breakfast)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    val scheduleAlarmAction = {
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
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 380.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TibbPrimaryPillButton(
                                    enabled = alarmLabel.isNotBlank() && selectedPatient != null,
                                    onClick = { scheduleAlarmAction() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("alarm_schedule_btn"),
                                ) {
                                    Text(s.alarmsAddBtn, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                TibbSecondaryPillButton(
                                    onClick = { audioManager.speakOfflineGuidance(TrilingualStrings.URDU_MEDICINE_REMINDER, "ur-PK") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("alarm_test_urdu_voice"),
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.alarmsTestVoiceBtn, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            ) {
                                TibbSecondaryPillButton(
                                    onClick = { audioManager.speakOfflineGuidance(TrilingualStrings.URDU_MEDICINE_REMINDER, "ur-PK") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxHeight().heightIn(min = 48.dp).testTag("alarm_test_urdu_voice"),
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.alarmsTestVoiceBtn, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                TibbPrimaryPillButton(
                                    enabled = alarmLabel.isNotBlank() && selectedPatient != null,
                                    onClick = { scheduleAlarmAction() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("alarm_schedule_btn"),
                                ) {
                                    Text(s.alarmsAddBtn, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Accordion 1: Scheduled Alarms (collapsed by default)
            TibbExpandableSection(
                title = "${s.alarmsScheduledTitle} (${scopedAlarms.size})",
                summary = if (scopedAlarms.isEmpty()) s.alarmsEmptyText else "${scopedAlarms.count { it.lastTakenDate == todayKey }} / ${scopedAlarms.size} taken today",
                icon = Icons.Rounded.Alarm,
                expanded = openAlarmSection == "scheduled",
                onExpandedChange = { openAlarmSection = if (it) "scheduled" else null },
                headerTestTag = "alarms_scheduled_expand",
            ) {
                if (scopedAlarms.isEmpty()) {
                    Text(s.alarmsEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
                } else {
                    scopedAlarms.forEach { alm ->
                        val takenToday = alm.lastTakenDate == todayKey
                        TibbGlassCard(padding = 12.dp, backgroundColor = c.surfaceMuted) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${alm.medicineLabel} (${alm.reminderType.name.replace('_', ' ')})", style = MaterialTheme.typography.titleSmall, color = c.ink)
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
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("alarm_mark_taken_${alm.id}"),
                                    ) {
                                        Text(s.alarmsMarkTaken, textAlign = TextAlign.Center, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accordion 2: Weekly Digest PDF (collapsed by default)
            TibbExpandableSection(
                title = s.weeklyDigestTitle,
                summary = s.weeklyDigestSubtitle,
                icon = Icons.Rounded.PictureAsPdf,
                expanded = openAlarmSection == "pdf",
                onExpandedChange = { openAlarmSection = if (it) "pdf" else null },
                headerTestTag = "alarms_pdf_expand",
            ) {
                Text(s.weeklyDigestSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                TibbPrimaryPillButton(
                    onClick = onExportWeeklyPdf,
                    modifier = Modifier.fillMaxWidth().testTag("export_weekly_pdf_btn"),
                ) {
                    Text(s.exportPdfBtn)
                }
            }
        } else {
            val scopedLabs = remember(labs, selectedPatient) {
                if (selectedPatient == null) labs else labs.filter { it.patientId == selectedPatient.id }
            }
            val filledBiomarkerCount = listOf(hb, wbc, plt, fbs, hba1c, creat, chol, alt).count { it.isNotBlank() }

            // Primary Card: Lab Report OCR
            TibbGlassCard(padding = 16.dp, backgroundColor = c.surfaceElevated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Text(s.labsTitle, style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge, color = c.ink)
                                Text(s.labsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                        SectionHelpButton(topic = HelpTopic.LAB_OCR, locale = locale)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    ) {
                        TibbSecondaryPillButton(
                            enabled = !ocrBusy,
                            onClick = { labPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("lab_ocr_photo_btn"),
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (ocrBusy) "Scanning..." else "Photo OCR", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        TibbSecondaryPillButton(
                            enabled = !ocrBusy,
                            onClick = { labPdfPicker.launch(arrayOf("application/pdf")) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("lab_ocr_pdf_btn"),
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (ocrBusy) "Scanning..." else "PDF OCR", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { applyParsedBiomarkers(it, sourceKind) },
                        label = { Text("Extracted / Pasted Lab Text (Auto-parses values)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("lab_raw_text_input"),
                    )

                    // Accordion 1: Manual / Parsed Biomarkers (collapsed by default)
                    TibbExpandableSection(
                        title = if (isUr) "بائیومارکر ویلیوز (Hb، شوگر، وغیرہ)" else "Manual / Parsed Biomarker Values",
                        summary = if (filledBiomarkerCount > 0) "$filledBiomarkerCount value(s) detected" else "Hb, WBC, Platelets, Sugar, HbA1c, Creatinine, Cholesterol, ALT",
                        icon = Icons.Rounded.Science,
                        expanded = openLabSection == "biomarkers",
                        onExpandedChange = { openLabSection = if (it) "biomarkers" else null },
                        headerTestTag = "lab_biomarkers_expand",
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth >= 500.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = hb, onValueChange = { hb = it }, label = { Text("Hb (g/dL)") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = wbc, onValueChange = { wbc = it }, label = { Text("WBC") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = plt, onValueChange = { plt = it }, label = { Text("Platelets") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = fbs, onValueChange = { fbs = it }, label = { Text("Sugar mg/dL") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = hba1c, onValueChange = { hba1c = it }, label = { Text("HbA1c %") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = creat, onValueChange = { creat = it }, label = { Text("Creatinine") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = chol, onValueChange = { chol = it }, label = { Text("Cholesterol") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = alt, onValueChange = { alt = it }, label = { Text("ALT / SGPT") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    TibbPrimaryPillButton(
                        enabled = selectedPatient != null && (filledBiomarkerCount > 0 || extractedText.isNotBlank()),
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("lab_save_btn"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.labsSaveBtn)
                    }
                }
            }

            // Accordion 2: Saved Lab History (collapsed by default)
            TibbExpandableSection(
                title = "${s.labsHistoryTitle} (${scopedLabs.size})",
                summary = if (scopedLabs.isEmpty()) s.labsEmptyText else "Latest: ${scopedLabs.first().scannedAtIso.take(10)}",
                icon = Icons.Rounded.History,
                expanded = openLabSection == "history",
                onExpandedChange = { openLabSection = if (it) "history" else null },
                headerTestTag = "lab_history_expand",
            ) {
                if (scopedLabs.isEmpty()) {
                    Text(s.labsEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
                } else {
                    scopedLabs.forEach { rec ->
                        TibbGlassCard(padding = 12.dp, backgroundColor = c.surfaceMuted) {
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
        }
        Spacer(Modifier.height(14.dp))
    }
}
