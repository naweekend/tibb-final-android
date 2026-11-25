package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.FrequentPlace
import com.example.data.model.GeoCoords
import com.example.data.model.SelectedLocationState
import com.example.engine.TriageEngine
import com.example.ui.components.PakistanLocationPickerDialog
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbSecondaryPillButton
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingStep3Medical(
    locale: AppLocale,
    sex: String,
    ageMonths: Int,
    chronic: SnapshotStateList<String>,
    isPregnantOrLactating: Boolean,
    onTogglePregnant: (Boolean) -> Unit,
    allergies: SnapshotStateList<String>,
    customAllergy: String,
    onCustomAllergyChange: (String) -> Unit,
    dailyMedicines: String,
    onDailyMedicinesChange: (String) -> Unit,
    priorLabNotes: String,
    onPriorLabNotesChange: (String) -> Unit,
    emergencyContact: String,
    onEmergencyContactChange: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR

    val chronicOptions = listOf(
        "HYPERTENSION" to "High BP (Hypertension)",
        "DIABETES" to "Diabetes (Sugar)",
        "ASTHMA" to "Asthma / Breathing",
        "HEART_DISEASE" to "Heart Disease",
        "KIDNEY" to "Kidney Condition",
        "THYROID" to "Thyroid",
    )

    Text(if (isUr) "پرانی بیماریاں (اگر کوئی ہوں)" else "Chronic Conditions", style = MaterialTheme.typography.labelLarge, color = c.ink)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chronicOptions.forEach { (code, lbl) ->
            val sel = chronic.contains(code)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (sel) c.accent.copy(alpha = 0.24f) else c.surfaceMuted)
                    .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(999.dp))
                    .clickable { if (sel) chronic.remove(code) else chronic.add(code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(lbl, style = MaterialTheme.typography.labelMedium, color = c.ink)
            }
        }
    }

    if (sex == "F" && ageMonths >= 12 * 12) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceMuted)
                .clickable { onTogglePregnant(!isPregnantOrLactating) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isPregnantOrLactating, onCheckedChange = onTogglePregnant)
            Text(
                text = if (isUr) "حاملہ یا دودھ پلانے والی ماں (اضافی زچگی حفاظتی سوالات فعال ہوں گے)"
                else "Currently Pregnant or Breastfeeding (enables WHO maternal danger checks)",
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
        }
    }

    Text(if (isUr) "الرجی" else "Allergies", style = MaterialTheme.typography.labelLarge, color = c.ink)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Penicillin", "Sulfa Drugs", "NSAIDs / Aspirin", "Peanuts", "Seafood").forEach { item ->
            val sel = allergies.contains(item)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (sel) c.dangerSoft else c.surfaceMuted)
                    .border(1.dp, if (sel) c.dangerRed else c.border, RoundedCornerShape(999.dp))
                    .clickable { if (sel) allergies.remove(item) else allergies.add(item) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(item, style = MaterialTheme.typography.labelMedium, color = c.ink)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = customAllergy,
            onValueChange = onCustomAllergyChange,
            label = { Text("Add custom allergy") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TibbSecondaryPillButton(
            onClick = {
                val clean = customAllergy.trim()
                if (clean.isNotEmpty() && !allergies.contains(clean)) {
                    allergies.add(clean)
                    onCustomAllergyChange("")
                }
            },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Allergy")
        }
    }

    OutlinedTextField(
        value = dailyMedicines,
        onValueChange = onDailyMedicinesChange,
        label = { Text(if (isUr) "روزانہ کی دوائیں (اختیاری)" else "Daily Medicines Note (Optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = priorLabNotes,
        onValueChange = onPriorLabNotesChange,
        label = { Text(if (isUr) "پچھلی لیب رپورٹس یا ہسپتال نوٹس" else "Prior Lab or Doctor Notes (Optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = emergencyContact,
        onValueChange = onEmergencyContactChange,
        label = { Text(if (isUr) "ہنگامی رابطہ نمبر (ICE)" else "Emergency Contact Phone (ICE)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun OnboardingStep4Places(
    locale: AppLocale,
    triageEngine: TriageEngine,
    places: SnapshotStateList<FrequentPlace>,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onRequestGps: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR

    var searchQuery by remember { mutableStateOf("") }
    var placeLabel by remember { mutableStateOf("Home") }
    var customCityMode by remember { mutableStateOf(false) }
    var customDistrict by remember { mutableStateOf("") }
    var customLandmark by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) onRequestGps()
    }

    Text(
        text = if (isUr) "روزمرہ مقامات (پاکستان بھر سے 1 تا 3 مقامات محفوظ کریں)"
        else "Pakistan Everyday Locations (Save 1–3 areas for instant clinic routing)",
        style = MaterialTheme.typography.titleMedium,
        color = c.ink,
    )

    TibbSecondaryPillButton(
        onClick = { showMapPicker = true },
        enabled = places.size < 3,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("onboard_open_map_btn"),
    ) {
        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isUr) "پاکستان آف لائن نقشے سے مقام منتخب کریں" else "Pick on Pakistan Offline Map (545+ Places)",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
    ) {
        TibbSecondaryPillButton(
            onClick = {
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) onRequestGps()
                else locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("onboard_gps_btn"),
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (gpsLocating) "Locating..." else if (userGpsCoords != null) "GPS Ready" else "Attach GPS",
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        TibbSecondaryPillButton(
            onClick = { customCityMode = !customCityMode },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("onboard_custom_city_toggle"),
        ) {
            Text(
                text = if (customCityMode) "Search Places" else "+ Custom Town",
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    OutlinedTextField(
        value = placeLabel,
        onValueChange = { placeLabel = it },
        label = { Text(if (isUr) "لیبل (مثلاً گھر، اسکول، دفتر)" else "Label (e.g. Home, School, Office)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("onboard_place_label_input"),
    )

    if (customCityMode) {
        OutlinedTextField(
            value = customDistrict,
            onValueChange = { customDistrict = it },
            label = { Text("City / District") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = customLandmark,
            onValueChange = { customLandmark = it },
            label = { Text("Area / Tehsil / Landmark") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TibbSecondaryPillButton(
            enabled = customDistrict.isNotBlank() && places.size < 3,
            onClick = {
                places.add(
                    FrequentPlace(
                        id = "plc_${UUID.randomUUID().toString().take(8)}",
                        label = placeLabel.ifBlank { "Home" },
                        district = customDistrict.trim(),
                        areaOrLandmark = customLandmark.trim(),
                        lat = userGpsCoords?.lat,
                        lng = userGpsCoords?.lng,
                    )
                )
                customDistrict = ""
                customLandmark = ""
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("onboard_save_custom_place_btn"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save Place")
        }
    } else {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (isUr) "پاکستان کا شہر، ضلع، قصبہ یا محلہ تلاش کریں..." else "Search city, district, town, sector or landmark...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboard_district_search"),
        )
        val filtered = remember(searchQuery) {
            triageEngine.searchPakistanLocations(searchQuery).take(10)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filtered.forEach { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceMuted)
                        .clickable(enabled = places.size < 3) {
                            places.add(
                                FrequentPlace(
                                    id = "plc_${UUID.randomUUID().toString().take(8)}",
                                    label = placeLabel.ifBlank { "Home" },
                                    district = loc.district,
                                    areaOrLandmark = if (isUr) loc.nameUr else loc.nameEn,
                                    province = loc.province.displayNameEn,
                                    lat = loc.lat,
                                    lng = loc.lng,
                                )
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("onboard_pk_loc_${loc.id}"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUr) loc.nameUr else loc.nameEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                        )
                        Text(
                            text = "${loc.district} · ${loc.province.shortCode} · ${if (isUr) loc.category.labelUr else loc.category.labelEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                        )
                    }
                    Icon(Icons.Rounded.Add, contentDescription = "Add ${loc.nameEn}", tint = c.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (places.isNotEmpty()) {
        Text("Saved Locations (${places.size}/3)", style = MaterialTheme.typography.labelLarge, color = c.accent)
        places.forEach { plc ->
            TibbGlassCard(padding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${plc.label} · ${plc.district}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        val sub = buildString {
                            if (plc.areaOrLandmark.isNotBlank()) append(plc.areaOrLandmark)
                            if (plc.province.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(plc.province)
                            }
                        }
                        if (sub.isNotBlank()) {
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                    IconButton(onClick = { places.remove(plc) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove ${plc.label}", tint = c.dangerRed)
                    }
                }
            }
        }
    }

    if (showMapPicker) {
        val initialSel = places.firstOrNull()?.let { p ->
            if (p.lat != null && p.lng != null) {
                SelectedLocationState(
                    name = p.areaOrLandmark.ifBlank { p.district },
                    district = p.district,
                    province = p.province,
                    lat = p.lat,
                    lng = p.lng,
                )
            } else null
        }
        PakistanLocationPickerDialog(
            locale = locale,
            triageEngine = triageEngine,
            initialSelection = initialSel,
            userGpsCoords = userGpsCoords,
            gpsLocating = gpsLocating,
            onRequestGps = onRequestGps,
            onConfirm = { picked ->
                if (places.size < 3) {
                    places.add(
                        FrequentPlace(
                            id = "plc_${UUID.randomUUID().toString().take(8)}",
                            label = placeLabel.ifBlank { "Home" },
                            district = picked.district,
                            areaOrLandmark = picked.name,
                            province = picked.province,
                            lat = picked.lat,
                            lng = picked.lng,
                        )
                    )
                }
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
        )
    }
}
