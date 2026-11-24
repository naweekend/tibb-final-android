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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.TriageEvaluationResult
import com.example.data.model.TriageTier
import com.example.engine.AudioFeedbackManager
import com.example.engine.HelpTopic
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.QrBitmapImage
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.StabilizationTimers
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbEspressoCard
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

@Composable
fun TriageResultView(
    result: TriageEvaluationResult,
    patient: PatientProfile,
    locale: AppLocale,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
    onOpenDialer: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onResetNewCheckup: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    val isRoman = locale == AppLocale.ROMAN

    BackHandler { onResetNewCheckup() }

    var showFarTimers by remember { mutableStateOf(false) }
    var followUpInput by remember { mutableStateOf("") }
    var followUpAnswer by remember { mutableStateOf<String?>(null) }
    var followUpAmrBlocked by remember { mutableStateOf(false) }

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
    val (facilities, needsLoc) = remember(activeCoords, selectedDistrict, result) {
        if (activeCoords == null && selectedDistrict.isBlank() && result.recommendedFacilities.isNotEmpty()) {
            result.recommendedFacilities to false
        } else {
            triageEngine.sortFacilities(
                tier = result.tier,
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // RED Tier Banner & First Aid
        if (result.tier == TriageTier.RED) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.dangerSoft)
                    .border(2.dp, c.dangerRed, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Emergency, contentDescription = null, tint = c.dangerRed)
                            Text(
                                text = s.redBannerLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = c.dangerRed,
                            )
                        }
                        SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                    }
                    Text(
                        text = s.redHeadline,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                    )
                    Text(
                        text = s.redSubline,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("1122" to "Rescue 1122", "115" to "Edhi 115", "1020" to "Chhipa 1020", "1166" to "Health 1166").forEach { (num, lbl) ->
                            TibbPrimaryPillButton(
                                onClick = { onOpenDialer(num) },
                                containerColor = c.dangerRed,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("triage_call_$num"),
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(lbl)
                            }
                        }
                    }

                    // Triggered Danger Signs
                    if (result.redFlags.isNotEmpty()) {
                        Text(s.firstAidTriggersTitle, style = MaterialTheme.typography.labelLarge, color = c.dangerRed)
                        result.redFlags.forEach { rf ->
                            Text(
                                text = "• ${if (isUr) rf.label_ur else rf.label_en} (${rf.source})",
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                color = c.ink,
                            )
                        }
                    }

                    // Immediate Do Now & Do Not
                    result.emergencyFirstAid?.let { fa ->
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TibbSecondaryPillButton(
                            onClick = {
                                audioManager.speakOfflineGuidance(
                                    TrilingualStrings.URDU_EMERGENCY_REDIRECT,
                                    "ur-PK",
                                )
                            },
                            borderColor = c.dangerRed,
                            contentColor = c.dangerRed,
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.urduAudio)
                        }
                        TibbSecondaryPillButton(
                            onClick = { showFarTimers = !showFarTimers },
                            borderColor = c.amber,
                            contentColor = c.amber,
                            modifier = Modifier.testTag("triage_toggle_far_timers"),
                        ) {
                            Text(if (showFarTimers) "Hide Timers" else "Stabilization Timers")
                        }
                    }
                }
            }

            if (showFarTimers) {
                StabilizationTimers(locale = locale)
            }
        } else {
            // AMBER or GREEN Banner + WHO Home Care
            val isAmber = result.tier == TriageTier.AMBER
            val accentColor = if (isAmber) c.amber else c.safeGreen
            val softBg = if (isAmber) c.amberSoft else c.safeGreen.copy(alpha = 0.14f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(softBg)
                    .border(1.5.dp, accentColor, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isAmber) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                            )
                            Text(
                                text = if (isAmber) s.amberBannerLabel else s.greenBannerLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = accentColor,
                            )
                        }
                        SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                    }
                    if (result.amberReasons.isNotEmpty()) {
                        result.amberReasons.forEach { rsn ->
                            Text("• $rsn", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                    }

                    result.matchedHomeCare?.let { care ->
                        val title = when {
                            isUr -> care.title_ur
                            isRoman -> care.title_roman
                            else -> care.title_en
                        }
                        val steps = when {
                            isUr -> care.steps_ur
                            isRoman -> care.steps_roman
                            else -> care.steps_en
                        }
                        Text(s.homeCareStepsTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text(title, style = MaterialTheme.typography.titleMedium, color = accentColor)
                        steps.forEachIndexed { idx, st ->
                            Text("${idx + 1}. $st", style = MaterialTheme.typography.bodySmall, color = c.ink)
                        }
                        Text(
                            text = if (isUr) care.escalation_warning_ur else care.escalation_warning_en,
                            style = MaterialTheme.typography.labelMedium,
                            color = c.amber,
                        )
                        TibbSecondaryPillButton(
                            onClick = {
                                audioManager.speakOfflineGuidance(care.steps_ur.joinToString(" "), "ur-PK")
                            },
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.urduAudio)
                        }
                    }
                }
            }

            // DRAP-Guarded Follow-Up Q&A
            TibbGlassCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                            Text(s.verifiedExplanationTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                        }
                        SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = followUpInput,
                            onValueChange = { followUpInput = it },
                            placeholder = { Text(s.followUpPlaceholder) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("triage_followup_input"),
                        )
                        TibbPrimaryPillButton(
                            enabled = followUpInput.isNotBlank(),
                            onClick = {
                                val audit = triageEngine.verifyAgainstClinicalGroundTruth(
                                    rawText = followUpInput,
                                    ageMonths = patient.ageMonths,
                                    homeCare = result.matchedHomeCare,
                                    diseaseRef = result.matchedDiseaseRef,
                                    triageTier = result.tier,
                                )
                                followUpAmrBlocked = audit.amrBlocked
                                followUpAnswer = if (audit.amrBlocked) {
                                    if (isUr) "عالمی ادارہ صحت اور ڈریپ اصولوں کے تحت اینٹی بائیوٹک یا دوا کی خوراک تجویز نہیں کی جا سکتی۔"
                                    else "Blocked by DRAP Antimicrobial Stewardship Guard: Tibb never recommends antibiotics or milligram dosages. Discuss prescriptions with your doctor."
                                } else {
                                    val facts = if (isUr) result.matchedDiseaseRef.verified_facts_ur else result.matchedDiseaseRef.verified_facts_en
                                    facts.joinToString(" • ")
                                }
                            },
                            modifier = Modifier.testTag("triage_followup_send"),
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = s.followUpSubmitBtn, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (followUpAnswer != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (followUpAmrBlocked) c.dangerSoft else c.surfaceMuted)
                                .padding(12.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (followUpAmrBlocked) s.amrGuardTriggered else s.groundTruthVerified,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (followUpAmrBlocked) c.dangerRed else c.safeGreen,
                                )
                                Text(followUpAnswer.orEmpty(), style = MaterialTheme.typography.bodySmall, color = c.ink)
                            }
                        }
                    }
                }
            }
        }

        // Offline QR Referral Slip
        TibbGlassCard(padding = 16.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(36.dp))
                    Text(s.qrSlipTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                    SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
                }
                Text(s.qrSlipSubline, style = MaterialTheme.typography.bodySmall, color = c.muted)
                val qrPayload = "${result.referralSlip.referralToken}|${patient.name}|${result.tier}|${result.referralSlip.ageBracket}|${result.referralSlip.primaryFlag}"
                QrBitmapImage(content = qrPayload, sizeDp = 168)
                Text(
                    text = result.referralSlip.referralToken,
                    style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                    color = c.accent,
                )
                TibbSecondaryPillButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(result.referralSlip.referralToken))
                        onShowToast("Copied token: ${result.referralSlip.referralToken}")
                    },
                    modifier = Modifier.testTag("copy_referral_token_btn"),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Referral Token")
                }
            }
        }

        // Location-Explicit Verified Facilities
        TibbEspressoCard(padding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = Color(0xFF95CEA4))
                        Text(s.nearestFacilitiesTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFF2EFE4))
                    }
                    SectionHelpButton(topic = HelpTopic.NEAREST_HOSPITALS, locale = locale, onDarkSurface = true)
                }
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
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFF95CEA4), modifier = Modifier.size(14.dp))
                            Text(if (gpsLocating) s.locatingGps else s.useGpsBtn, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF2EFE4))
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
                            Text(selectedDistrict.ifBlank { s.allDistrictsOption }, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF2EFE4))
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
                    Text(s.locationRequiredBody, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB6C9BE))
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
                                Text(if (isUr) fac.name_ur else fac.name_en, style = MaterialTheme.typography.labelLarge, color = Color(0xFFF2EFE4))
                                Text(
                                    text = "${fac.tehsil}, ${fac.district}" + (fac.distanceKm?.let { " · $it km" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB6C9BE),
                                )
                            }
                            TibbSecondaryPillButton(
                                onClick = { onOpenDialer(fac.phone) },
                                borderColor = Color(0xFF95CEA4),
                                contentColor = Color(0xFF95CEA4),
                            ) {
                                Text(s.callBtn)
                            }
                        }
                    }
                }
            }
        }

        TibbSecondaryPillButton(
            onClick = onResetNewCheckup,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("triage_start_new_btn"),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.startNewCheckupBtn)
        }
        Spacer(Modifier.height(14.dp))
    }
}
