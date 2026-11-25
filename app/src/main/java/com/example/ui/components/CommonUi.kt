package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLocale
import com.example.data.model.PatientProfile
import com.example.data.model.TriageTier
import com.example.data.model.WorkspaceMode
import com.example.engine.AndroidSensorsManager
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.NotoNastaliqUrduFamily
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import com.example.viewmodel.MainNavTab

@Composable
fun TibbBrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.accent.copy(alpha = 0.16f))
                .border(1.dp, c.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MedicalServices,
                contentDescription = "Tibb Emblem",
                tint = c.accent,
                modifier = Modifier.size(if (compact) 17.dp else 20.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "tibb",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = c.ink,
            )
            Text(
                text = "طب",
                fontFamily = NotoNastaliqUrduFamily,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                color = c.accent,
            )
        }
    }
}

@Composable
fun OfflineStatusBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.safeGreen.copy(alpha = 0.12f))
            .border(1.dp, c.safeGreen.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(c.safeGreen),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun triageTierAccent(tier: TriageTier): Color {
    val c = LocalTibbColors.current
    return when (tier) {
        TriageTier.RED -> c.dangerRed
        TriageTier.AMBER -> c.urgentYellow
        TriageTier.GREEN -> c.safeGreen
        TriageTier.BLUE -> c.wellnessBlue
    }
}

@Composable
fun triageTierSwatch(tier: TriageTier): Color {
    val c = LocalTibbColors.current
    return when (tier) {
        TriageTier.RED -> c.dangerRed
        TriageTier.AMBER -> c.urgentYellowSwatch
        TriageTier.GREEN -> c.safeGreen
        TriageTier.BLUE -> c.wellnessBlue
    }
}

@Composable
fun triageTierSoftBg(tier: TriageTier): Color {
    val c = LocalTibbColors.current
    return when (tier) {
        TriageTier.RED -> c.dangerSoft
        TriageTier.AMBER -> c.urgentYellowSoft
        TriageTier.GREEN -> c.safeGreenSoft
        TriageTier.BLUE -> c.wellnessBlueSoft
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UrgencyClassificationsPillStrip(
    locale: AppLocale,
    activeTier: TriageTier? = null,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TriageTier.entries.forEach { tier ->
            val accent = triageTierAccent(tier)
            val swatch = triageTierSwatch(tier)
            val softBg = triageTierSoftBg(tier)
            val isActive = activeTier == tier
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(softBg)
                    .border(if (isActive) 1.5.dp else 1.dp, accent.copy(alpha = if (isActive) 0.95f else 0.45f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .testTag("urgency_pill_${tier.displayCode.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(swatch),
                )
                Text(
                    text = tier.shortLabel(locale),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold),
                    color = accent,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
fun OfflineUrgencyClassificationsCard(
    locale: AppLocale,
    activeTier: TriageTier? = null,
    onTierClick: ((TriageTier) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    TibbGlassCard(
        modifier = modifier.testTag("offline_urgency_classifications_card"),
        padding = 14.dp,
        backgroundColor = c.surfaceElevated,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = s.offlineTriageClassificationsTitle,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleSmall) else MaterialTheme.typography.titleSmall,
                        color = c.ink,
                    )
                    Text(
                        text = when (locale) {
                            AppLocale.EN -> "Colour-coded clinical urgency levels — 100% offline rule-based."
                            AppLocale.UR -> "چار رنگوں پر مبنی ہنگامی درجہ بندی — 100% آف لائن اصولی نظام۔"
                            AppLocale.ROMAN -> "4 rangon ki clinical darja-bandi — 100% offline usooli nizam."
                        },
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
                SectionHelpButton(topic = HelpTopic.TRIAGE_RESULT, locale = locale)
            }

            TriageTier.entries.forEach { tier ->
                val accent = triageTierAccent(tier)
                val swatch = triageTierSwatch(tier)
                val softBg = triageTierSoftBg(tier)
                val isActive = activeTier == tier
                val icon = when (tier) {
                    TriageTier.RED -> Icons.Rounded.Emergency
                    TriageTier.AMBER -> Icons.Rounded.LocalHospital
                    TriageTier.GREEN -> Icons.Rounded.MedicalServices
                    TriageTier.BLUE -> Icons.Rounded.Spa
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(softBg)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) accent else accent.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .then(
                            if (onTierClick != null) Modifier.clickable { onTierClick(tier) }
                            else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("urgency_tier_${tier.displayCode.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatch.copy(alpha = 0.22f))
                            .border(1.5.dp, swatch, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tier.shortLabel(locale),
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = tier.shortLabel(locale),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                            )
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = if (isUr) "موجودہ" else "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        Text(
                            text = tier.subtitle(locale),
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TibbTopBar(
    locale: AppLocale,
    isPinProtected: Boolean,
    onCallSos: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecords: () -> Unit,
    onLockOrPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val compactBar = maxWidth < 380.dp
        val iconBtnSize = 40.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TibbBrandMark(
                compact = true,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactBar) 4.dp else 6.dp),
            ) {
                // Calm, clearly separated Emergency 1122 button (distinct from utility icons)
                Row(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.dangerSoft)
                        .border(1.dp, c.dangerRed.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onCallSos)
                        .padding(horizontal = if (compactBar) 10.dp else 12.dp, vertical = 6.dp)
                        .testTag("topbar_sos_1122"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Emergency,
                        contentDescription = null,
                        tint = c.dangerRed,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (compactBar) "1122" else "SOS 1122",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.dangerRed,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(20.dp),
                    thickness = 1.dp,
                    color = c.border,
                )

                IconButton(
                    onClick = onOpenHelp,
                    modifier = Modifier
                        .size(iconBtnSize)
                        .clip(CircleShape)
                        .testTag("topbar_help_btn"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = when (locale) {
                            AppLocale.UR -> "مکمل رہنمائی"
                            AppLocale.ROMAN -> "Poori App Help"
                            AppLocale.EN -> "Simple Help Guide"
                        },
                        tint = c.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = onLockOrPin,
                    modifier = Modifier
                        .size(iconBtnSize)
                        .clip(CircleShape)
                        .testTag("topbar_lock_btn"),
                ) {
                    Icon(
                        imageVector = if (isPinProtected) Icons.Rounded.Lock else Icons.Rounded.Security,
                        contentDescription = if (isPinProtected) s.lockVault else s.setPinBtn,
                        tint = if (isPinProtected) c.accent else c.muted,
                        modifier = Modifier.size(19.dp),
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(iconBtnSize)
                        .clip(CircleShape)
                        .background(c.surfaceMuted)
                        .testTag("topbar_menu_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Open Settings & Drawer",
                        tint = c.ink,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

private data class NavTabSpec(
    val tab: MainNavTab,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun primaryNavItems(locale: AppLocale, workspaceMode: WorkspaceMode): List<NavTabSpec> {
    val s = TrilingualStrings.get(locale)
    return buildList {
        add(NavTabSpec(MainNavTab.HOME, s.navHome, Icons.Rounded.Home))
        add(NavTabSpec(MainNavTab.CHECKUP_CHAT, s.navCheckup, Icons.Rounded.MedicalServices))
        add(NavTabSpec(MainNavTab.COACH, s.navCoach, Icons.Rounded.Restaurant))
        add(NavTabSpec(MainNavTab.VITALS, s.navVitals, Icons.Rounded.MonitorHeart))
        add(NavTabSpec(MainNavTab.ALARMS_LABS, s.navAlarmsReports, Icons.Rounded.Alarm))
        if (workspaceMode == WorkspaceMode.BHU) {
            add(NavTabSpec(MainNavTab.QUEUE, s.navQueue, Icons.AutoMirrored.Rounded.FactCheck))
        }
    }
}

@Composable
fun TibbBottomBar(
    activeTab: MainNavTab,
    locale: AppLocale,
    workspaceMode: WorkspaceMode,
    onSelectTab: (MainNavTab) -> Unit,
) {
    val c = LocalTibbColors.current
    val items = primaryNavItems(locale, workspaceMode)
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = c.border)
        NavigationBar(
            containerColor = c.surface.copy(alpha = 0.97f),
            contentColor = c.ink,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            items.forEach { item ->
                val selected = activeTab == item.tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(item.tab) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.accent,
                        selectedTextColor = c.ink,
                        indicatorColor = c.accentSoft,
                        unselectedIconColor = c.muted,
                        unselectedTextColor = c.muted,
                    ),
                    modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}"),
                )
            }
        }
    }
}

@Composable
fun TibbNavigationRail(
    activeTab: MainNavTab,
    locale: AppLocale,
    workspaceMode: WorkspaceMode,
    onSelectTab: (MainNavTab) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val items = primaryNavItems(locale, workspaceMode) + listOf(
        NavTabSpec(MainNavTab.RECORDS, s.navRecords, Icons.Rounded.FolderShared),
        NavTabSpec(MainNavTab.ANDROID_KIT, s.navKit, Icons.Rounded.Security),
    )
    NavigationRail(
        containerColor = c.surface,
        contentColor = c.ink,
        windowInsets = WindowInsets(0),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            items.forEach { item ->
                val selected = activeTab == item.tab
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectTab(item.tab) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = {
                        Text(
                            item.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = c.accent,
                        selectedTextColor = c.ink,
                        indicatorColor = c.accentSoft,
                        unselectedIconColor = c.muted,
                        unselectedTextColor = c.muted,
                    ),
                    modifier = Modifier.testTag("rail_tab_${item.tab.name.lowercase()}"),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun PersonSwitcherStrip(
    profiles: List<PatientProfile>,
    activePerson: PatientProfile?,
    locale: AppLocale,
    onSelectPerson: (String) -> Unit,
    onAddPerson: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(999.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("person_switcher_dropdown"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.People, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text(
                    text = activePerson?.let {
                        val rel = if (isUr) it.relationship.labelUr else it.relationship.labelEn
                        "${it.name} · $rel"
                    } ?: s.activePersonLabel,
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.labelLarge) else MaterialTheme.typography.labelLarge,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Switch Member", tint = c.muted)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                profiles.forEach { person ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("${person.name} (${if (isUr) person.relationship.labelUr else person.relationship.labelEn})")
                                if (person.id == activePerson?.id) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.safeGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectPerson(person.id)
                        },
                    )
                }
                HorizontalDivider(color = c.border)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                            Text(s.addPersonBtn, color = c.accent)
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddPerson()
                    },
                )
            }
        }

        IconButton(
            onClick = onAddPerson,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.border, CircleShape)
                .testTag("add_member_chip_btn"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = s.addPersonBtn, tint = c.ink, modifier = Modifier.size(18.dp))
        }

        IconButton(
            onClick = onOpenRecords,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.border, CircleShape)
                .testTag("saved_reports_chip_btn"),
        ) {
            Icon(Icons.Rounded.FolderShared, contentDescription = s.navRecords, tint = c.accent, modifier = Modifier.size(18.dp))
        }

        SectionHelpButton(topic = HelpTopic.PERSON_SWITCHER, locale = locale)
    }
}

@Composable
fun QrBitmapImage(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180,
) {
    val bmp = remember(content) { AndroidSensorsManager.generateQrBitmap(content, 420) }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "Offline Referral QR Code for $content",
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp),
    )
}

@Composable
fun SpeechDictationButton(
    locale: AppLocale,
    onResultText: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    var listening by remember { mutableStateOf(false) }
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    fun startNativeRecognition() {
        val rec = recognizer
        if (rec == null) {
            onError("On-device speech recognizer unavailable on this device.")
            return
        }
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                onError("Voice capture ended (code $error). You can type or tap quick chips.")
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) onResultText(spoken)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (locale == AppLocale.UR) "ur-PK" else "en-PK",
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        runCatching { rec.startListening(intent) }.onFailure {
            listening = false
            onError("Unable to start voice recognizer.")
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startNativeRecognition()
        else onError("Microphone permission denied.")
    }

    val onClickAction = {
        if (listening) {
            recognizer?.stopListening()
            listening = false
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) startNativeRecognition()
            else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (compact) {
        IconButton(
            onClick = onClickAction,
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (listening) c.dangerSoft else c.surfaceMuted)
                .border(1.dp, if (listening) c.dangerRed else c.border, CircleShape)
                .testTag("voice_dictate_icon_btn"),
        ) {
            Icon(
                imageVector = if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = if (listening) s.recordingStop else s.voiceInput,
                tint = if (listening) c.dangerRed else c.accent,
            )
        }
    } else {
        TibbSecondaryPillButton(
            onClick = onClickAction,
            borderColor = if (listening) c.dangerRed else c.border,
            contentColor = if (listening) c.dangerRed else c.ink,
            modifier = modifier.testTag("voice_dictate_btn"),
        ) {
            Icon(
                imageVector = if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(if (listening) s.recordingStop else s.voiceInput)
        }
    }
}
