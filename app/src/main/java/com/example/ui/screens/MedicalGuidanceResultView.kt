package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.AssessmentOutcome
import com.example.data.model.ConditionSeverity
import com.example.data.model.GeoCoords
import com.example.data.model.MatchedConditionResult
import com.example.data.model.MedicalGuidanceAssessment
import com.example.data.model.PatientProfile
import com.example.data.model.TriageTier
import com.example.engine.AudioFeedbackManager
import com.example.engine.HelpTopic
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineUrgencyClassificationsCard
import com.example.ui.components.QrBitmapImage
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.StabilizationTimers
import com.example.ui.components.UrgencyClassificationsPillStrip
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalGuidanceResultView(
    assessment: MedicalGuidanceAssessment,
    patient: PatientProfile,
    locale: AppLocale,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
    onOpenDialer: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onChangeSelections: () -> Unit,
    onStartNewAssessment: () -> Unit,
    onOpenWellnessCoach: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    BackHandler { onChangeSelections() }

    var expandedPanel by remember { mutableStateOf<String?>(null) }

    var selectedDistrict by remember(patient.id) {
        mutableStateOf(patient.frequentPlaces.firstOrNull()?.district.orEmpty())
    }
    var activeCoords by remember(patient.id, userGpsCoords) {
        val plc = patient.frequentPlaces.firstOrNull()
        mutableStateOf(
            userGpsCoords ?: if (plc?.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
        )
    }
    var districtOpen by remember { mutableStateOf(false) }
    val districts = remember { triageEngine.getAvailableDistricts() }
    val (facilities, needsLoc) = remember(activeCoords, selectedDistrict, assessment) {
        if (activeCoords == null && selectedDistrict.isBlank() && assessment.recommendedFacilities.isNotEmpty()) {
            assessment.recommendedFacilities to false
        } else {
            triageEngine.sortFacilities(
                tier = assessment.triageTier,
                isPediatric = patient.ageMonths <= 60,
                userCoords = activeCoords,
                districtFilter = selectedDistrict,
            )
        }
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) onRequestGps()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (assessment.outcome) {
            AssessmentOutcome.EMERGENCY_OVERRIDE -> {
                EmergencyOverrideCard(
                    assessment = assessment,
                    locale = locale,
                    audioManager = audioManager,
                    onOpenDialer = onOpenDialer,
                )

                TibbExpandableSection(
                    title = if (isUr) "اسٹیبلائزیشن ٹائمرز (ہنگامی امداد)" else "Stabilization Timers",
                    summary = if (isUr) "جلنے، دورے، خون روکنے اور ہسپتال روانگی چیک لسٹ" else "Burn 20m, seizure timer, bleeding & en-route checklist",
                    icon = Icons.Rounded.Timer,
                    accentColor = c.urgentYellow,
                    expanded = expandedPanel == "timers",
                    onExpandedChange = { open -> expandedPanel = if (open) "timers" else null },
                    headerTestTag = "triage_toggle_far_timers",
                ) {
                    StabilizationTimers(locale = locale)
                }
            }

            AssessmentOutcome.INVALID_OR_CONFLICTING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.urgentYellowSoft)
                        .border(1.5.dp, c.urgentYellow, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                ) {
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
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.urgentYellow)
                                Text(
                                    text = if (isUr) "معلومات میں تضاد یا کمی" else "Input Review Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = c.urgentYellow,
                                )
                            }
                            SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                        }
                        Text(
                            text = if (isUr) assessment.probabilisticDisclaimerUr else assessment.probabilisticDisclaimerEn,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                        val errs = if (isUr) assessment.validationErrorsUr else assessment.validationErrorsEn
                        errs.forEach { err ->
                            Text(
                                text = "• $err",
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodyMedium) else MaterialTheme.typography.bodyMedium,
                                color = c.ink,
                            )
                        }
                        TibbPrimaryPillButton(
                            onClick = onChangeSelections,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("validation_fix_btn"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isUr) "انتخابات درست کریں" else "Edit My Selections")
                        }
                    }
                }
            }

            AssessmentOutcome.INSUFFICIENT_OR_UNKNOWN -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.urgentYellowSoft)
                        .border(1.5.dp, c.urgentYellow, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                ) {
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
                                Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = c.urgentYellow)
                                Text(
                                    text = if (isUr) "واضح مطابقت موجود نہیں — BHU / RHC ڈاکٹر سے معائنہ کروائیں" else "YELLOW (Urgent) · Same-Day BHU / RHC Visit Advised",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = c.urgentYellow,
                                )
                            }
                            SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                        }
                        Text(
                            text = if (isUr) assessment.probabilisticDisclaimerUr else assessment.probabilisticDisclaimerEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.ink,
                        )
                        Text(
                            text = (if (isUr) assessment.uncertaintyExplanationUr else assessment.uncertaintyExplanationEn).orEmpty(),
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodyMedium) else MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                        )
                        TibbPrimaryPillButton(
                            onClick = onChangeSelections,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isUr) "مزید علامات شامل کریں یا تبدیل کریں" else "Refine Symptom Selections")
                        }
                    }
                }
            }

            AssessmentOutcome.CONDITION_MATCHED -> {
                val isYellowUrgent = assessment.triageTier == TriageTier.AMBER
                val accent = if (isYellowUrgent) c.urgentYellow else c.safeGreen
                val softBg = if (isYellowUrgent) c.urgentYellowSoft else c.safeGreenSoft

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(softBg)
                        .border(1.5.dp, accent, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                        .testTag("triage_result_banner_${assessment.triageTier.displayCode.lowercase()}"),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                Icon(
                                    imageVector = if (isYellowUrgent) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                )
                                Text(
                                    text = if (isYellowUrgent) s.amberBannerLabel else s.greenBannerLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                )
                            }
                            SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                        }
                        Text(
                            text = assessment.triageTier.subtitle(locale),
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.labelMedium) else MaterialTheme.typography.labelMedium,
                            color = c.ink,
                        )
                        Text(
                            text = if (isUr) assessment.probabilisticDisclaimerUr else assessment.probabilisticDisclaimerEn,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                }

                assessment.matchedConditions.forEachIndexed { idx, matched ->
                    MatchedConditionGuidanceCard(
                        matched = matched,
                        rank = idx + 1,
                        locale = locale,
                        audioManager = audioManager,
                    )
                }
            }
        }

        // BLUE (Wellness) Preventive Care Card (shown when not in acute RED emergency)
        if (assessment.triageTier != TriageTier.RED) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.wellnessBlueSoft)
                    .border(1.dp, c.wellnessBlue.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
                    .testTag("triage_blue_wellness_card"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Spa, contentDescription = null, tint = c.wellnessBlue, modifier = Modifier.size(18.dp))
                        Text(
                            text = s.blueWellnessLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = c.wellnessBlue,
                        )
                    }
                    Text(
                        text = TriageTier.BLUE.subtitle(locale),
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                    TibbSecondaryPillButton(
                        onClick = onOpenWellnessCoach,
                        borderColor = c.wellnessBlue,
                        contentColor = c.wellnessBlue,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("triage_open_blue_wellness_btn"),
                    ) {
                        Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (locale) {
                                AppLocale.EN -> "Open Preventive Diet, Hydration & Sleep"
                                AppLocale.UR -> "حفاظتی غذا، پانی اور نیند پلان کھولیں"
                                AppLocale.ROMAN -> "Hifazati Ghaza, Paani aur Neend Kholein"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        // Offline Clinical Triage 4-Colour Classifications Legend (Collapsed by default)
        TibbExpandableSection(
            title = s.offlineTriageClassificationsTitle,
            summary = "RED · YELLOW · GREEN · BLUE",
            icon = Icons.Rounded.Info,
            accentColor = c.wellnessBlue,
            expanded = expandedPanel == "tiers",
            onExpandedChange = { open -> expandedPanel = if (open) "tiers" else null },
            headerTestTag = "guidance_expand_tiers",
        ) {
            OfflineUrgencyClassificationsCard(
                locale = locale,
                activeTier = assessment.triageTier,
            )
        }

        // Offline Medical Sources, Coverage & Limitations (Collapsed by default)
        TibbExpandableSection(
            title = if (isUr) "طبی ذرائع، دائرہ کار اور حدود" else "Medical Sources, Scope & Limitations",
            summary = if (isUr) {
                "آخری جائزہ: ${assessment.knowledgeMetadata.lastReviewedDate}"
            } else {
                "Offline v${assessment.knowledgeMetadata.version} · Reviewed ${assessment.knowledgeMetadata.lastReviewedDate}"
            },
            icon = Icons.Rounded.Info,
            expanded = expandedPanel == "scope",
            onExpandedChange = { open -> expandedPanel = if (open) "scope" else null },
            headerTestTag = "guidance_expand_scope",
        ) {
            Text(
                text = if (isUr) assessment.knowledgeMetadata.medicalDisclaimerUr else assessment.knowledgeMetadata.medicalDisclaimerEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.urgentYellow,
            )
            Text(
                text = if (isUr) "مستند طبی ذرائع:" else "Authoritative Sources:",
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
            )
            assessment.knowledgeMetadata.authoritativeSources.forEach { src ->
                Text("• $src", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Text(
                text = if (isUr) "اس آف لائن نظام کی حدود:" else "System Limitations:",
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
            )
            val limits = if (isUr) assessment.knowledgeMetadata.limitationsUr else assessment.knowledgeMetadata.limitationsEn
            limits.forEach { lim ->
                Text("• $lim", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // Offline QR Referral Slip (only when present)
        assessment.referralSlip?.let { slip ->
            TibbExpandableSection(
                title = s.qrSlipTitle,
                summary = slip.referralToken,
                icon = Icons.Rounded.QrCode2,
                expanded = expandedPanel == "qr",
                onExpandedChange = { open -> expandedPanel = if (open) "qr" else null },
                headerTestTag = "triage_expand_qr",
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(s.qrSlipSubline, style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center)
                    val qrPayload = "${slip.referralToken}|${patient.name}|${assessment.triageTier}|${slip.ageBracket}|${slip.primaryFlag}"
                    QrBitmapImage(content = qrPayload, sizeDp = 168)
                    Text(
                        text = slip.referralToken,
                        style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                        color = c.accent,
                    )
                    TibbSecondaryPillButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(slip.referralToken))
                            onShowToast("Copied token: ${slip.referralToken}")
                        },
                        modifier = Modifier.testTag("copy_referral_token_btn"),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isUr) "ریفرل کوڈ کاپی کریں" else "Copy Referral Token")
                    }
                }
            }
        }

        // Nearest Public Hospitals & BHUs
        TibbExpandableSection(
            title = s.nearestFacilitiesTitle,
            summary = selectedDistrict.ifBlank { if (isUr) "قریبی ہسپتال تلاش کریں" else "Tap to locate nearest hospitals & BHUs" },
            icon = Icons.Rounded.LocalHospital,
            accentColor = Color(0xFF8FD0A0),
            containerColor = c.espressoBottom,
            borderColor = Color.White.copy(alpha = 0.12f),
            titleColor = Color(0xFFF3F0E6),
            summaryColor = Color(0xFFB1C4B9),
            expanded = expandedPanel == "hospitals",
            onExpandedChange = { open -> expandedPanel = if (open) "hospitals" else null },
            headerTestTag = "triage_expand_hospitals",
            trailingHeaderContent = {
                SectionHelpButton(topic = HelpTopic.NEAREST_HOSPITALS, locale = locale, onDarkSurface = true)
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFF8FD0A0), modifier = Modifier.size(14.dp))
                        Text(if (gpsLocating) s.locatingGps else s.useGpsBtn, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3F0E6))
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { districtOpen = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(selectedDistrict.ifBlank { s.allDistrictsOption }, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3F0E6))
                    }
                    DropdownMenu(expanded = districtOpen, onDismissRequest = { districtOpen = false }) {
                        districts.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = {
                                    selectedDistrict = d
                                    activeCoords = null
                                    districtOpen = false
                                },
                            )
                        }
                    }
                }
            }

            if (needsLoc) {
                Text(s.locationRequiredBody, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB1C4B9))
            } else {
                facilities.take(4).forEach { fac ->
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
                            Text(if (isUr) fac.name_ur else fac.name_en, style = MaterialTheme.typography.labelLarge, color = Color(0xFFF3F0E6))
                            Text(
                                text = "${fac.tehsil}, ${fac.district}" + (fac.distanceKm?.let { " · $it km" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB1C4B9),
                            )
                        }
                        TibbSecondaryPillButton(
                            onClick = { onOpenDialer(fac.phone) },
                            borderColor = Color(0xFF8FD0A0),
                            contentColor = Color(0xFF8FD0A0),
                        ) {
                            Text(s.callBtn)
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TibbSecondaryPillButton(
                onClick = onChangeSelections,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("guidance_change_selections_btn"),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (isUr) "تبدیل کریں" else "Edit Inputs", maxLines = 1)
            }
            TibbPrimaryPillButton(
                onClick = onStartNewAssessment,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("triage_start_new_btn"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(s.startNewCheckupBtn, maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmergencyOverrideCard(
    assessment: MedicalGuidanceAssessment,
    locale: AppLocale,
    audioManager: AudioFeedbackManager,
    onOpenDialer: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.dangerSoft)
            .border(2.dp, c.dangerRed, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Icon(Icons.Rounded.Emergency, contentDescription = null, tint = c.dangerRed)
                    Text(s.redBannerLabel, style = MaterialTheme.typography.labelLarge, color = c.dangerRed)
                }
                SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
            }
            Text(
                text = s.redHeadline,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                color = c.ink,
            )
            Text(
                text = if (isUr) assessment.probabilisticDisclaimerUr else assessment.probabilisticDisclaimerEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.dangerRed,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("1122" to "Rescue 1122", "115" to "Edhi 115", "1020" to "Chhipa 1020", "1166" to "Health 1166").forEach { (num, lbl) ->
                    TibbPrimaryPillButton(
                        onClick = { onOpenDialer(num) },
                        containerColor = c.dangerRed,
                        contentColor = Color.White,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("triage_call_$num"),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(lbl, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (assessment.triggeredEmergencyRules.isNotEmpty()) {
                Text(s.firstAidTriggersTitle, style = MaterialTheme.typography.labelLarge, color = c.dangerRed)
                assessment.triggeredEmergencyRules.forEach { rule ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surface.copy(alpha = 0.72f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "• ${if (isUr) rule.titleUr else rule.titleEn}",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.dangerRed,
                        )
                        Text(
                            text = if (isUr) rule.summaryUr else rule.summaryEn,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                        Text(
                            text = "${rule.sourceCitation} · Reviewed ${rule.lastReviewedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                        )
                    }
                }
            }

            assessment.emergencyFirstAid?.let { fa ->
                Text(s.firstAidDoNowTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                val doNow = if (isUr) fa.do_now_ur else fa.do_now_en
                doNow.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
                Text(s.firstAidDoNotTitle, style = MaterialTheme.typography.titleMedium, color = c.dangerRed)
                val doNot = if (isUr) fa.do_not_ur else fa.do_not_en
                doNot.forEach { step ->
                    Text("• $step", style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }

            TibbSecondaryPillButton(
                onClick = {
                    audioManager.speakOfflineGuidance(TrilingualStrings.URDU_EMERGENCY_REDIRECT, "ur-PK")
                },
                borderColor = c.dangerRed,
                contentColor = c.dangerRed,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.urduAudio)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchedConditionGuidanceCard(
    matched: MatchedConditionResult,
    rank: Int,
    locale: AppLocale,
    audioManager: AudioFeedbackManager,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    val cond = matched.condition

    val sevColor = when (matched.adjustedSeverity) {
        ConditionSeverity.EMERGENCY -> c.dangerRed
        ConditionSeverity.URGENT, ConditionSeverity.MODERATE -> c.urgentYellow
        ConditionSeverity.MILD -> c.safeGreen
    }
    val sevLabel = when (matched.adjustedSeverity) {
        ConditionSeverity.EMERGENCY -> if (isUr) "RED · ہنگامی" else "RED · Emergency"
        ConditionSeverity.URGENT -> if (isUr) "YELLOW · فوری BHU/RHC" else "YELLOW · Urgent Visit"
        ConditionSeverity.MODERATE -> if (isUr) "YELLOW · ڈاکٹر معائنہ" else "YELLOW · Doctor Visit"
        ConditionSeverity.MILD -> if (isUr) "GREEN · گھریلو نگہداشت" else "GREEN · Home Care"
    }

    TibbGlassCard(padding = 16.dp, backgroundColor = c.surfaceElevated) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isUr) "ممکنہ مطابقت #$rank" else "Possible Match #$rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(sevColor.copy(alpha = 0.16f))
                        .border(1.dp, sevColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(sevLabel, style = MaterialTheme.typography.labelSmall, color = sevColor)
                }
            }

            Text(
                text = if (isUr) matched.probabilisticHeaderUr else matched.probabilisticHeaderEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                color = c.ink,
            )

            Text(
                text = if (isUr) cond.shortDescriptionUr else cond.shortDescriptionEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.muted,
            )

            val matchedLabels = if (isUr) matched.matchedSymptomLabelsUr else matched.matchedSymptomLabelsEn
            if (matchedLabels.isNotEmpty()) {
                Text(
                    text = if (isUr) "مطابقت کی وجہ:" else "Why this matched:",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.ink,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    matchedLabels.forEach { lbl ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.accentSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = c.ink)
                        }
                    }
                }
            }

            val popAlert = if (isUr) matched.ageOrPregnancyAlertUr else matched.ageOrPregnancyAlertEn
            if (!popAlert.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.urgentYellowSoft)
                        .border(1.dp, c.urgentYellow.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = (if (isUr) "خصوصی احتیاط: " else "Population Precaution: ") + popAlert,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                }
            }

            // 1. Immediate First Aid
            val firstAidList = if (isUr) cond.appropriateFirstAidUr else cond.appropriateFirstAidEn
            if (firstAidList.isNotEmpty()) {
                SectionGuidanceBlock(
                    title = if (isUr) "فوری ابتدائی امداد (First Aid)" else "Appropriate First Aid",
                    icon = Icons.Rounded.MedicalServices,
                    accentColor = c.accent,
                    items = firstAidList,
                    numbered = true,
                    isUr = isUr,
                )
            }

            // 2. Safe Home Care OR Explicit Reason Home Remedies Are Unsafe
            val noHomeReason = if (isUr) cond.noHomeCareReasonUr else cond.noHomeCareReasonEn
            if (!noHomeReason.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.dangerSoft)
                        .border(1.dp, c.dangerRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Block, contentDescription = null, tint = c.dangerRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isUr) "گھریلو علاج محفوظ نہیں" else "Home Remedies Not Appropriate",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.dangerRed,
                            )
                        }
                        Text(
                            text = noHomeReason,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                    }
                }
            } else {
                val homeList = if (isUr) cond.safeHomeCareUr else cond.safeHomeCareEn
                if (homeList.isNotEmpty()) {
                    SectionGuidanceBlock(
                        title = if (isUr) "محفوظ گھریلو دیکھ بھال" else "Safe Home Care",
                        icon = Icons.Rounded.Healing,
                        accentColor = c.safeGreen,
                        items = homeList,
                        numbered = true,
                        isUr = isUr,
                    )
                }
            }

            // 3. Things to Avoid
            val avoidList = if (isUr) cond.thingsToAvoidUr else cond.thingsToAvoidEn
            if (avoidList.isNotEmpty()) {
                SectionGuidanceBlock(
                    title = if (isUr) "کن چیزوں سے پرہیز کریں" else "Things to Avoid",
                    icon = Icons.Rounded.Block,
                    accentColor = c.dangerRed,
                    items = avoidList,
                    numbered = false,
                    isUr = isUr,
                )
            }

            // 4. Professional Care Thresholds
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surfaceMuted)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (isUr) "ڈاکٹر یا ہسپتال کب جائیں:" else "When to Seek Medical Care:",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.ink,
                )
                Text(
                    text = "• " + (if (isUr) "ڈاکٹر/کلینک: ${cond.whenToConsultDoctorUr}" else "Doctor / Clinic: ${cond.whenToConsultDoctorEn}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.ink,
                )
                Text(
                    text = "• " + (if (isUr) "فوری کلینک (YELLOW): ${cond.whenUrgentCareUr}" else "Urgent BHU / RHC (YELLOW): ${cond.whenUrgentCareEn}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.urgentYellow,
                )
                Text(
                    text = "• " + (if (isUr) "ہنگامی 1122: ${cond.whenEmergencyServicesUr}" else "Emergency (1122): ${cond.whenEmergencyServicesEn}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dangerRed,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TibbSecondaryPillButton(
                    onClick = {
                        val speechText = buildString {
                            append(cond.nameUr).append("۔ ")
                            append(cond.appropriateFirstAidUr.joinToString("۔ "))
                            append(" ").append(cond.safeHomeCareUr.joinToString("۔ "))
                            append(" ").append(cond.whenToConsultDoctorUr)
                        }
                        audioManager.speakOfflineGuidance(speechText, "ur-PK")
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.urduAudio, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = "${cond.sources.firstOrNull() ?: "WHO"} · ${cond.lastReviewedDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun SectionGuidanceBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    items: List<String>,
    numbered: Boolean,
    isUr: Boolean,
) {
    val c = LocalTibbColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceMuted)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = accentColor)
        }
        items.forEachIndexed { idx, line ->
            val prefix = if (numbered) "${idx + 1}. " else "• "
            Text(
                text = "$prefix$line",
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.ink,
            )
        }
    }
}
