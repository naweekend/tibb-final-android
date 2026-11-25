package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.UnitSystem
import com.example.data.model.WorkspaceMode
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import com.example.viewmodel.MainNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawerSheet(
    locale: AppLocale,
    darkMode: Boolean,
    unitSystem: UnitSystem,
    workspaceMode: WorkspaceMode,
    accountName: String,
    isPinProtected: Boolean,
    onDismiss: () -> Unit,
    onSetLocale: (AppLocale) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSetUnitSystem: (UnitSystem) -> Unit,
    onSetWorkspaceMode: (WorkspaceMode) -> Unit,
    onNavigateTab: (MainNavTab) -> Unit,
    onOpenPinUpgrade: () -> Unit,
    onLockVault: () -> Unit,
    onOpenLanding: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLegal: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceElevated,
        contentColor = c.ink,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TibbBrandMark()
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Menu", tint = c.muted)
                }
            }

            HorizontalDivider(color = c.border)

            // Language Switcher
            Text(
                text = if (isUr) "زبان منتخب کریں" else "Language / زبان / Roman",
                style = MaterialTheme.typography.labelLarge,
                color = c.muted,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            ) {
                AppLocale.entries.forEach { loc ->
                    val selected = loc == locale
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) c.ctaBg else c.surfaceMuted)
                            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                            .clickable { onSetLocale(loc) }
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                            .testTag("drawer_locale_${loc.code}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = loc.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) c.ctaInk else c.ink,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Theme & Unit System
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            ) {
                TibbSecondaryPillButton(
                    onClick = onToggleDarkMode,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("drawer_theme_toggle"),
                ) {
                    Icon(
                        imageVector = if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (darkMode) "Ivory Light" else "Charcoal Dark",
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TibbSecondaryPillButton(
                    onClick = {
                        onSetUnitSystem(if (unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("drawer_unit_toggle"),
                ) {
                    Text(
                        text = if (unitSystem == UnitSystem.METRIC) "cm / kg" else "ft / lbs",
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Workspace Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.workspaceRoleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.muted,
                    modifier = Modifier.weight(1f),
                )
                SectionHelpButton(topic = HelpTopic.PIN_SECURITY, locale = locale)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            ) {
                WorkspaceMode.entries.forEach { mode ->
                    val selected = workspaceMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) c.accentSoft else c.surfaceMuted)
                            .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
                            .clickable { onSetWorkspaceMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .testTag("drawer_workspace_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (mode == WorkspaceMode.GHARANA) s.gharanaMode else s.bhuMode,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Collapsed by default Vault Tools, Guide & Legal
            TibbExpandableSection(
                title = if (isUr) "والٹ ریکارڈ، رہنمائی اور معلومات" else "Vault Tools, Guide & Legal",
                summary = if (isUr) "ریکارڈز، رہنمائی (?)، بیک اپ اور رازداری" else "Saved Records, Simple Help, Backup & Legal",
                icon = Icons.Rounded.Apps,
                initiallyExpanded = false,
                headerTestTag = "drawer_tools_expand",
            ) {
                DrawerNavRow(
                    icon = Icons.Rounded.FolderShared,
                    title = s.navRecords,
                    subtitle = s.recordsSubtitle,
                    testTag = "drawer_open_records",
                    onClick = {
                        onDismiss()
                        onNavigateTab(MainNavTab.RECORDS)
                    },
                )
                if (workspaceMode == WorkspaceMode.BHU) {
                    DrawerNavRow(
                        icon = Icons.AutoMirrored.Rounded.FactCheck,
                        title = s.navQueue,
                        subtitle = s.queueSubtitle,
                        testTag = "drawer_open_queue",
                        onClick = {
                            onDismiss()
                            onNavigateTab(MainNavTab.QUEUE)
                        },
                    )
                }
                DrawerNavRow(
                    icon = Icons.Rounded.Security,
                    title = s.navKit,
                    subtitle = s.kitSubtitle,
                    testTag = "drawer_open_kit",
                    onClick = {
                        onDismiss()
                        onNavigateTab(MainNavTab.ANDROID_KIT)
                    },
                )
                DrawerNavRow(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = when (locale) {
                        AppLocale.UR -> "آسان مکمل رہنمائی (?)"
                        AppLocale.ROMAN -> "Asaan Poori App Guide (?)"
                        AppLocale.EN -> "Simple Help Guide (?)"
                    },
                    subtitle = when (locale) {
                        AppLocale.UR -> "BHU، OCR، بی پی، شوگر اور ہر بٹن کی سادہ وضاحت"
                        AppLocale.ROMAN -> "BHU, OCR, BP, Sugar aur har feature ka aasan matlab"
                        AppLocale.EN -> "Plain-language explanation of BHU, OCR, BP & every feature"
                    },
                    testTag = "drawer_open_help",
                    onClick = {
                        onDismiss()
                        onOpenHelp()
                    },
                )
                DrawerNavRow(
                    icon = Icons.Rounded.Info,
                    title = if (isUr) "تعارف / ہوم پیج" else "App Overview & Pillars",
                    subtitle = s.tagline,
                    testTag = "drawer_open_landing",
                    onClick = {
                        onDismiss()
                        onOpenLanding()
                    },
                )
                DrawerNavRow(
                    icon = Icons.Rounded.Policy,
                    title = if (isUr) "طبی احتیاط اور رازداری (Legal & Privacy)" else "Clinical Disclaimer & Privacy",
                    subtitle = s.offlineEncryptedLabel,
                    testTag = "drawer_open_legal",
                    onClick = {
                        onDismiss()
                        onOpenLegal()
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (!isPinProtected) {
                    TibbPrimaryPillButton(
                        onClick = {
                            onDismiss()
                            onOpenPinUpgrade()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("drawer_set_pin_btn"),
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.setPinBtn, textAlign = TextAlign.Center, maxLines = 2)
                    }
                } else {
                    TibbPrimaryPillButton(
                        onClick = {
                            onDismiss()
                            onLockVault()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("drawer_lock_vault_btn"),
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.lockVault, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2)
        }
    }
}

@Composable
fun PinUpgradeDialog(
    locale: AppLocale,
    workspaceMode: WorkspaceMode,
    authBusy: Boolean,
    authError: String?,
    onDismiss: () -> Unit,
    onSubmit: (displayName: String, pin: String, workspace: WorkspaceMode) -> Unit,
) {
    val s = TrilingualStrings.get(locale)
    var name by remember { mutableStateOf("Household Vault") }
    var pin by remember { mutableStateOf("") }
    var selectedWorkspace by remember { mutableStateOf(workspaceMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.createVaultTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.authFeature1, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.accountNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text(s.pinLabelCreate) },
                    placeholder = { Text(s.pinPlaceholder) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_upgrade_input"),
                )
                if (!authError.isNullOrBlank()) {
                    Text(authError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TibbPrimaryPillButton(
                enabled = !authBusy && pin.length in 4..8 && name.isNotBlank(),
                onClick = { onSubmit(name, pin, selectedWorkspace) },
                modifier = Modifier.testTag("pin_upgrade_submit"),
            ) {
                Text(s.createBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun LegalComplianceDialog(
    locale: AppLocale,
    onDismiss: () -> Unit,
) {
    val isUr = locale == AppLocale.UR
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isUr) "طبی انتباہ اور رازداری کا اصول" else "Medical Disclaimer & Offline Privacy",
                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isUr) {
                        "• طب صرف معلوماتی طبی رہنمائی اور ڈبلیو ایچ او حفاظتی اصولوں پر مبنی ہے۔ یہ ڈاکٹر کے معائنے یا تشخیص کا متبادل نہیں۔\n" +
                            "• ہنگامی علامات (سینے میں درد، سانس کی بندش، دورے، بے ہوشی) میں فوری 1122 پر کال کریں۔\n" +
                            "• ڈریپ (DRAP) اصولوں کے تحت اینٹی بائیوٹک یا کسی دوا کی خوراک تجویز نہیں کی جاتی۔\n" +
                            "• آپ کا تمام ریکارڈ PBKDF2 + AES-GCM-256 کے ساتھ صرف آپ کے فون پر محفوظ رہتا ہے اور انٹرنیٹ کے بغیر کام کرتا ہے۔"
                    } else {
                        "• Tibb provides WHO IMCI/ETAT supportive guidance only and is NOT a medical diagnosis or prescription tool.\n" +
                            "• For danger signs (chest pain, stroke signs, severe breathing distress, seizures, heavy bleeding), call Rescue 1122 / 115 immediately.\n" +
                            "• Under DRAP antimicrobial stewardship rules, antibiotics and milligram dosages are never recommended.\n" +
                            "• All patient profiles, vitals, chats, labs, and photos (with EXIF stripped locally) remain 100% offline inside your AES-GCM-256 encrypted Room vault."
                    },
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TibbPrimaryPillButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("legal_dialog_ok"),
            ) {
                Text(if (isUr) "سمجھ گیا" else "Understood")
            }
        },
    )
}
