package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCodeScanner
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.StoredEpisode
import com.example.data.model.TriageTier
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton

@Composable
fun BhuQueueScreen(
    locale: AppLocale,
    accountId: String,
    episodes: List<StoredEpisode>,
    onVerifyReferralEpisode: (String) -> Unit,
    onBackHome: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    BackHandler { onBackHome() }

    var filterTier by remember { mutableStateOf("ALL") }
    var tokenQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    if (showQrScanner) {
        CameraMultimodalModal(
            locale = locale,
            accountId = accountId,
            qrOnlyMode = true,
            onCapturedPhoto = { _, _ -> showQrScanner = false },
            onQrScanned = { raw ->
                val token = raw.split("|").firstOrNull()?.trim().orEmpty()
                tokenQuery = token
                showQrScanner = false
                val hit = episodes.firstOrNull { it.referralToken.equals(token, ignoreCase = true) }
                if (hit != null) {
                    onVerifyReferralEpisode(hit.id)
                    onShowToast("QR verified for ${hit.patientName} (${hit.referralToken})")
                } else {
                    onShowToast("Scanned QR: $token")
                }
            },
            onDismiss = { showQrScanner = false },
        )
        return
    }

    val sortedFiltered = remember(episodes, filterTier) {
        val base = when (filterTier) {
            "RED" -> episodes.filter { it.triageTier == TriageTier.RED }
            "AMBER" -> episodes.filter { it.triageTier == TriageTier.AMBER }
            "GREEN" -> episodes.filter { it.triageTier == TriageTier.GREEN }
            else -> episodes
        }
        base.sortedBy {
            when (it.triageTier) {
                TriageTier.RED -> 0
                TriageTier.AMBER -> 1
                TriageTier.GREEN -> 2
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
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.queueTitle, style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text(s.queueSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "RED", "AMBER", "GREEN").forEach { key ->
                        val sel = filterTier == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else c.surfaceMuted)
                                .clickable { filterTier = key }
                                .padding(vertical = 8.dp)
                                .testTag("queue_filter_${key.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key, style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                        }
                    }
                }
            }
        }

        // QR Verify Card
        TibbGlassCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.queueVerifyTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tokenQuery,
                        onValueChange = { tokenQuery = it },
                        label = { Text("TIBB-RED-XXXX Token") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("queue_token_input"),
                    )
                    TibbSecondaryPillButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier.testTag("queue_scan_qr_btn"),
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan Referral QR", modifier = Modifier.size(18.dp))
                    }
                }
                TibbPrimaryPillButton(
                    enabled = tokenQuery.isNotBlank(),
                    onClick = {
                        val hit = episodes.firstOrNull { it.referralToken.equals(tokenQuery.trim(), ignoreCase = true) }
                        if (hit != null) {
                            onVerifyReferralEpisode(hit.id)
                            onShowToast("Marked referral completed for ${hit.patientName}")
                        } else {
                            onShowToast("No matching offline episode found for ${tokenQuery.trim()}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("queue_verify_btn"),
                ) {
                    Text(s.queueVerifyBtn)
                }
            }
        }

        Text("${s.queueEpisodesTitle} (${sortedFiltered.size})", style = MaterialTheme.typography.titleMedium, color = c.ink)
        if (sortedFiltered.isEmpty()) {
            Text(s.queueEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
        } else {
            sortedFiltered.forEach { ep ->
                val tierColor = when (ep.triageTier) {
                    TriageTier.RED -> c.dangerRed
                    TriageTier.AMBER -> c.amber
                    TriageTier.GREEN -> c.safeGreen
                }
                TibbGlassCard(padding = 14.dp, borderColor = tierColor.copy(alpha = 0.6f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${ep.patientName} (${ep.ageBracket})", style = MaterialTheme.typography.titleMedium, color = c.ink)
                            Text(ep.triageTier.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = tierColor)
                        }
                        Text("${ep.symptomCategory} · ${ep.referralToken} · ${ep.facilityName}", style = TabularNumberStyle, color = c.muted)
                        if (ep.referralCompleted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.safeGreen, modifier = Modifier.size(16.dp))
                                Text("Referral Completed", style = MaterialTheme.typography.labelSmall, color = c.safeGreen)
                            }
                        } else {
                            TibbSecondaryPillButton(
                                onClick = { onVerifyReferralEpisode(ep.id) },
                                modifier = Modifier.testTag("queue_complete_${ep.id}"),
                            ) {
                                Text(s.queueVerifyBtn)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
