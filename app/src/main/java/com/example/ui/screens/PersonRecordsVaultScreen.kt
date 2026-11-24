package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLocale
import com.example.data.model.PatientProfile
import com.example.data.model.SavedPersonReport
import com.example.data.model.SavedReportKind
import com.example.data.model.StoredAlarm
import com.example.data.model.StoredEpisode
import com.example.data.model.StoredLabRecord
import com.example.data.model.UnitSystem
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
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonRecordsVaultScreen(
    locale: AppLocale,
    unitSystem: UnitSystem,
    activePerson: PatientProfile?,
    reports: List<SavedPersonReport>,
    episodes: List<StoredEpisode>,
    labs: List<StoredLabRecord>,
    alarms: List<StoredAlarm>,
    onEditProfile: () -> Unit,
    onExportPersonPdf: () -> Unit,
    onSaveQuickReport: (SavedReportKind, String, String, Uri?) -> Unit,
    onBackHome: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    BackHandler { onBackHome() }

    if (activePerson == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.onboardingSubtitle, color = c.muted)
        }
        return
    }

    var kind by remember { mutableStateOf(SavedReportKind.REPORT_PHOTO) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedPhotoUri = uri }

    val scopedReports = remember(reports, activePerson.id) { reports.filter { it.personId == activePerson.id } }
    val scopedEpisodes = remember(episodes, activePerson.id) { episodes.filter { it.patientId == activePerson.id } }
    val scopedLabs = remember(labs, activePerson.id) { labs.filter { it.patientId == activePerson.id } }
    val scopedAlarms = remember(alarms, activePerson.id) { alarms.filter { it.patientId == activePerson.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header + Edit + Print Summary
        TibbGlassCard(padding = 16.dp, borderColor = c.accent.copy(alpha = 0.45f)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.recordsTitle,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    SectionHelpButton(topic = HelpTopic.SAVED_RECORDS, locale = locale)
                }
                Text(s.recordsSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Text(
                    text = "${activePerson.name} · ${activePerson.bloodGroup.label} · ${HealthMath.formatWeightDisplay(activePerson.weightKg, unitSystem)} · ${HealthMath.formatHeightDisplay(activePerson.heightCm, unitSystem)}",
                    style = TabularNumberStyle,
                    color = c.accent,
                )
                if (activePerson.chronicConditions.isNotEmpty() || activePerson.allergies.isNotEmpty()) {
                    Text(
                        text = "Chronic: ${activePerson.chronicConditions.joinToString().ifEmpty { "None" }} | Allergies: ${activePerson.allergies.joinToString().ifEmpty { "None" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TibbSecondaryPillButton(
                        onClick = onEditProfile,
                        modifier = Modifier.weight(1f).testTag("records_edit_profile_btn"),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.editProfileBtn)
                    }
                    TibbPrimaryPillButton(
                        onClick = onExportPersonPdf,
                        modifier = Modifier.weight(1f).testTag("records_export_pdf_btn"),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.printSummaryBtn)
                    }
                }
            }
        }

        // Quick Log / Photo Upload Composer
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add Report, Prescription, Meal or Activity Note", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                    SectionHelpButton(topic = HelpTopic.SAVED_RECORDS, locale = locale)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SavedReportKind.REPORT_PHOTO,
                        SavedReportKind.PRESCRIPTION,
                        SavedReportKind.MEAL_PHOTO,
                        SavedReportKind.VITALS_LOG,
                        SavedReportKind.EXERCISE_LOG,
                    ).forEach { k ->
                        val sel = kind == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                .clickable { kind = k }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        ) {
                            Text(k.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Dr. Prescription, Lunch Photo, CBC Slip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("records_title_input"),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Summary") },
                    modifier = Modifier.fillMaxWidth().testTag("records_notes_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TibbSecondaryPillButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f).testTag("records_pick_photo_btn"),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (pickedPhotoUri != null) "Photo Ready" else "Attach Photo")
                    }
                    TibbPrimaryPillButton(
                        enabled = title.isNotBlank() || notes.isNotBlank() || pickedPhotoUri != null,
                        onClick = {
                            onSaveQuickReport(kind, title, notes, pickedPhotoUri)
                            title = ""
                            notes = ""
                            pickedPhotoUri = null
                        },
                        modifier = Modifier.weight(1f).testTag("records_save_entry_btn"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save to Vault")
                    }
                }
            }
        }

        // Saved Reports & Photos Gallery
        Text("Saved Reports & Snapshots (${scopedReports.size})", style = MaterialTheme.typography.titleMedium, color = c.ink)
        if (scopedReports.isEmpty()) {
            Text("No photos or quick logs saved for ${activePerson.name} yet.", style = MaterialTheme.typography.bodySmall, color = c.muted)
        } else {
            scopedReports.forEach { rep ->
                TibbGlassCard(padding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${rep.title} · ${rep.kind.name.replace('_', ' ')}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text(rep.createdAtIso.take(10), style = TabularNumberStyle.copy(color = c.accent))
                        if (rep.notes.isNotBlank()) Text(rep.notes, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        rep.imagePath?.let { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = rep.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)),
                            )
                        }
                    }
                }
            }
        }

        // Scoped Triage Episodes & Labs & Alarms Summary
        Text("Triage History (${scopedEpisodes.size}) · Lab Scans (${scopedLabs.size}) · Alarms (${scopedAlarms.size})", style = MaterialTheme.typography.titleMedium, color = c.ink)
        scopedEpisodes.forEach { ep ->
            TibbGlassCard(padding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("[${ep.triageTier}] ${ep.symptomCategory} · ${ep.referralToken}", style = TabularNumberStyle, color = c.accent)
                    Text("${ep.facilityName} (${ep.createdAtIso.take(10)})", style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
