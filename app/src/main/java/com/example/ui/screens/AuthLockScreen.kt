package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountRecord
import com.example.data.model.AppLocale
import com.example.data.model.WorkspaceMode
import androidx.compose.foundation.shape.CircleShape
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.TibbBrandMark
import com.example.ui.components.TibbGlobe
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

@Composable
fun AuthLockScreen(
    locale: AppLocale,
    accounts: List<AccountRecord>,
    authBusy: Boolean,
    authError: String?,
    onSetLocale: (AppLocale) -> Unit,
    onCreateAccount: (displayName: String, pin: String, workspace: WorkspaceMode) -> Unit,
    onUnlockAccount: (accountId: String, pin: String) -> Unit,
    onQuickGuestVault: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLegal: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    var modeTab by remember(accounts.size) { mutableIntStateOf(if (accounts.isNotEmpty()) 0 else 1) }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.accountId.orEmpty()) }
    var accountDropdownOpen by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var selectedWorkspace by remember { mutableStateOf(WorkspaceMode.GHARANA) }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.accountId == selectedAccountId }) {
            selectedAccountId = accounts.first().accountId
        }
    }

    TibbGradientScaffoldBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TibbBrandMark()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfflineStatusBadge(label = s.offlineEncryptedLabel)
                        IconButton(
                            onClick = onOpenHelp,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(c.surface.copy(alpha = 0.72f))
                                .border(1.dp, c.accent.copy(alpha = 0.42f), CircleShape)
                                .testTag("auth_help_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = "Open Simple Help Guide",
                                tint = c.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Locale Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLocale.entries.forEach { loc ->
                        val selected = loc == locale
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) c.ctaBg else c.surface)
                                .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                .clickable { onSetLocale(loc) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("auth_locale_${loc.code}"),
                        ) {
                            Text(
                                text = loc.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) c.ctaInk else c.ink,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = s.authHeadline,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.headlineMedium) else MaterialTheme.typography.headlineMedium,
                            color = c.ink,
                        )
                        Text(
                            text = s.authSubline,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                    TibbGlobe(size = 128.dp)
                }

                TibbGlassCard(padding = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (modeTab == 0) s.unlockVaultTitle else s.createVaultTitle,
                                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                                color = c.ink,
                                modifier = Modifier.weight(1f),
                            )
                            SectionHelpButton(topic = HelpTopic.AUTH_VAULT, locale = locale)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surfaceMuted)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            listOf(
                                0 to s.existingAccountsBtn,
                                1 to s.newAccountBtn,
                            ).forEach { (idx, label) ->
                                val active = modeTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) c.ctaBg else c.surfaceMuted)
                                        .clickable {
                                            modeTab = idx
                                            pin = ""
                                        }
                                        .padding(vertical = 10.dp)
                                        .testTag(if (idx == 0) "auth_tab_existing" else "auth_tab_new"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) c.ctaInk else c.ink,
                                    )
                                }
                            }
                        }

                        if (modeTab == 0) {
                            if (accounts.isEmpty()) {
                                Text(
                                    text = if (isUr) "ابھی کوئی پِن اکاؤنٹ موجود نہیں، نیا اکاؤنٹ بنائیں۔" else "No PIN vaults created yet. Switch to New Account or open quick local vault.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.muted,
                                )
                            } else {
                                val activeAcc = accounts.firstOrNull { it.accountId == selectedAccountId } ?: accounts.first()
                                Text(s.selectAccountLabel, style = MaterialTheme.typography.labelMedium, color = c.muted)
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(c.surfaceMuted)
                                            .border(1.dp, c.border, RoundedCornerShape(14.dp))
                                            .clickable { accountDropdownOpen = true }
                                            .padding(horizontal = 14.dp, vertical = 14.dp)
                                            .testTag("auth_account_selector"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${activeAcc.displayName} (${activeAcc.workspace.name})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = c.ink,
                                        )
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Account", tint = c.muted)
                                    }
                                    DropdownMenu(
                                        expanded = accountDropdownOpen,
                                        onDismissRequest = { accountDropdownOpen = false },
                                    ) {
                                        accounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = { Text("${acc.displayName} (${acc.workspace.name})") },
                                                onClick = {
                                                    selectedAccountId = acc.accountId
                                                    accountDropdownOpen = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.workspaceRoleLabel, style = MaterialTheme.typography.labelMedium, color = c.muted)
                                SectionHelpButton(topic = HelpTopic.PIN_SECURITY, locale = locale)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WorkspaceMode.entries.forEach { mode ->
                                    val selected = selectedWorkspace == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (selected) c.accent.copy(alpha = 0.2f) else c.surfaceMuted)
                                            .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
                                            .clickable { selectedWorkspace = mode }
                                            .padding(vertical = 12.dp)
                                            .testTag("auth_workspace_${mode.name.lowercase()}"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (mode == WorkspaceMode.GHARANA) s.gharanaMode else s.bhuMode,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = c.ink,
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(s.accountNameLabel) },
                                placeholder = {
                                    Text(
                                        if (selectedWorkspace == WorkspaceMode.GHARANA) s.accountNamePlaceholderGharana
                                        else s.accountNamePlaceholderBhu
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_account_name_input"),
                            )
                        }

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                            label = { Text(if (modeTab == 0) s.pinLabelUnlock else s.pinLabelCreate) },
                            placeholder = { Text(s.pinPlaceholder) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_pin_input"),
                        )

                        if (!authError.isNullOrBlank()) {
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.dangerRed,
                            )
                        }

                        TibbPrimaryPillButton(
                            enabled = !authBusy && pin.length in 4..8 && (modeTab == 1 || accounts.isNotEmpty()),
                            onClick = {
                                if (modeTab == 0 && accounts.isNotEmpty()) {
                                    onUnlockAccount(selectedAccountId.ifEmpty { accounts.first().accountId }, pin)
                                } else {
                                    onCreateAccount(displayName, pin, selectedWorkspace)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_btn"),
                        ) {
                            Icon(
                                imageVector = if (modeTab == 0) Icons.Rounded.LockOpen else Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (modeTab == 0) s.unlockBtn else s.createBtn)
                        }

                        TibbSecondaryPillButton(
                            onClick = onQuickGuestVault,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_quick_local_btn"),
                        ) {
                            Text(if (isUr) "بغیر پِن فوری مقامی والٹ کھولیں" else "Continue with Quick Local Vault (No PIN)")
                        }
                    }
                }

                // Security guarantees
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                        Text(s.authFeature1, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                        Text(s.authFeature2, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }

                TextButton(
                    onClick = onOpenLegal,
                    modifier = Modifier.testTag("auth_legal_btn"),
                ) {
                    Text(
                        text = if (isUr) "طبی احتیاط اور رازداری کا معاہدہ" else "Medical Disclaimer & Local Encryption Info",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                    )
                }
            }
        }
    }
}
