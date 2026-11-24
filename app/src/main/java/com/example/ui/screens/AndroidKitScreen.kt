package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.AuditLogEntry
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.components.SectionHelpButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton

private enum class RuntimeProfileMode(val label: String, val ramMb: Int, val detail: String) {
    IDLE("Core Idle (Room + WHO Engine)", 128, "AES-GCM encrypted database + deterministic WHO IMCI/ETAT rules loaded."),
    OCR_ACTIVE("ML Kit Lab OCR Active", 196, "On-device text recognition & PdfRenderer active; <20% RAM ceiling."),
    WHISPER_ACTIVE("Voice & TTS Pipeline Active", 393, "Native Android speech & offline Urdu AudioTrack/TTS active."),
    MULTIMODAL_PEAK("CameraX PPG + Vision Peak", 908, "CameraX YUV analyzer + optical pulse + UI buffers safely below 1024 MB cap."),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AndroidKitScreen(
    locale: AppLocale,
    auditLogs: List<AuditLogEntry>,
    onExportBackup: (String) -> Unit,
    onRestoreBackup: (String, String) -> Unit,
    onWipeAccount: () -> Unit,
    onBackHome: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)

    BackHandler { onBackHome() }

    var runtimeMode by remember { mutableStateOf(RuntimeProfileMode.IDLE) }
    var passphrase by remember { mutableStateOf("") }
    var loadedBackupJson by remember { mutableStateOf<String?>(null) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.fold(
                onSuccess = { text ->
                    if (!text.isNullOrBlank()) {
                        loadedBackupJson = text
                        onShowToast("Backup file loaded. Enter passphrase & tap Restore.")
                    }
                },
                onFailure = { err -> onShowToast("Unable to read file: ${err.message}") },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = c.accent)
                        Text(s.kitTitle, style = MaterialTheme.typography.titleLarge, color = c.ink)
                    }
                    SectionHelpButton(topic = HelpTopic.ANDROID_KIT, locale = locale)
                }
                Text(s.kitSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        // On-Device Memory Guard (<1024 MB)
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = c.accent)
                        Text(s.kitMemoryTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${runtimeMode.ramMb} / 1024 MB", style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.safeGreen)
                        SectionHelpButton(topic = HelpTopic.ANDROID_KIT, locale = locale)
                    }
                }

                LinearProgressIndicator(
                    progress = { (runtimeMode.ramMb / 1024f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                    color = if (runtimeMode.ramMb > 850) c.amber else c.safeGreen,
                    trackColor = c.surfaceMuted,
                )
                Text(runtimeMode.detail, style = MaterialTheme.typography.bodySmall, color = c.muted)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RuntimeProfileMode.entries.forEach { m ->
                        val sel = runtimeMode == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                .clickable { runtimeMode = m }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        ) {
                            Text(m.label, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                        }
                    }
                }
            }
        }

        // Encrypted .tibb-backup Export & Restore
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.kitBackupTitle, style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                    SectionHelpButton(topic = HelpTopic.ANDROID_KIT, locale = locale)
                }
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Backup Encryption Passphrase (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("kit_backup_passphrase"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TibbPrimaryPillButton(
                        enabled = passphrase.length >= 6,
                        onClick = { onExportBackup(passphrase) },
                        modifier = Modifier.weight(1f).testTag("kit_export_backup_btn"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.kitExportBtn, style = MaterialTheme.typography.labelSmall)
                    }
                    TibbSecondaryPillButton(
                        onClick = { backupPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f).testTag("kit_load_backup_btn"),
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.kitLoadBtn, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (loadedBackupJson != null) {
                    TibbPrimaryPillButton(
                        enabled = passphrase.length >= 6,
                        onClick = {
                            loadedBackupJson?.let { onRestoreBackup(it, passphrase) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("kit_restore_backup_btn"),
                    ) {
                        Text(s.kitRestoreBtn)
                    }
                }

                TibbPrimaryPillButton(
                    onClick = { showWipeConfirm = true },
                    containerColor = c.dangerRed,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth().testTag("kit_wipe_data_btn"),
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.kitWipeBtn)
                }
            }
        }

        // Security Audit Log
        Text("Local Vault Audit Trail (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, color = c.ink)
        auditLogs.take(15).forEach { entry ->
            TibbGlassCard(padding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.action, style = MaterialTheme.typography.labelLarge, color = c.accent)
                        Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = c.ink)
                    }
                    Text(entry.timestampIso.take(10), style = TabularNumberStyle, color = c.muted)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Permanently Wipe Local Vault?") },
            text = {
                Text("This permanently deletes all profiles, vitals, chats, lab reports, and alarms for this vault from your device. This action cannot be undone.")
            },
            confirmButton = {
                TibbPrimaryPillButton(
                    onClick = {
                        showWipeConfirm = false
                        onWipeAccount()
                    },
                    containerColor = c.dangerRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("kit_confirm_wipe_btn"),
                ) {
                    Text("Wipe Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
