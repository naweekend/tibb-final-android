package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.StoredEpisode
import com.example.data.model.TriageTier
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineUrgencyClassificationsCard
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.triageTierAccent
import com.example.ui.components.triageTierSoftBg
import com.example.ui.components.triageTierSwatch
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
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
            "YELLOW" -> episodes.filter { it.triageTier == TriageTier.AMBER }
            "GREEN" -> episodes.filter { it.triageTier == TriageTier.GREEN }
            "BLUE" -> episodes.filter { it.triageTier == TriageTier.BLUE }
            else -> episodes
        }
        base.sortedBy {
            when (it.triageTier) {
                TriageTier.RED -> 0
                TriageTier.AMBER -> 1
                TriageTier.GREEN -> 2
                TriageTier.BLUE -> 3
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
        // Header + Tier Filters
        TibbGlassCard(padding = 16.dp, backgroundColor = c.surfaceElevated) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.queueTitle, style = MaterialTheme.typography.headlineSmall, color = c.ink, modifier = Modifier.weight(1f))
                    SectionHelpButton(topic = HelpTopic.BHU_QUEUE, locale = locale)
                }
                Text(s.queueSubtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                ) {
                    listOf("ALL", "RED", "YELLOW", "GREEN", "BLUE").forEach { key ->
                        val sel = filterTier == key
                        val keyTier = when (key) {
                            "RED" -> TriageTier.RED
                            "YELLOW" -> TriageTier.AMBER
                            "GREEN" -> TriageTier.GREEN
                            "BLUE" -> TriageTier.BLUE
                            else -> null
                        }
                        val swatch = keyTier?.let { triageTierSwatch(it) }
                        val accent = keyTier?.let { triageTierAccent(it) } ?: c.ctaBg
                        val softBg = keyTier?.let { triageTierSoftBg(it) } ?: c.surfaceMuted
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) c.ctaBg else softBg)
                                .border(1.dp, if (sel) c.ctaBg else accent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                                .clickable { filterTier = key }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("queue_filter_${key.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                if (swatch != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(swatch),
                                    )
                                }
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) c.ctaInk else accent,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        TibbExpandableSection(
            title = s.offlineTriageClassificationsTitle,
            summary = "RED · YELLOW · GREEN · BLUE",
            icon = Icons.Rounded.Info,
            accentColor = c.wellnessBlue,
            initiallyExpanded = false,
            headerTestTag = "queue_tiers_expand",
        ) {
            OfflineUrgencyClassificationsCard(
                locale = locale,
                activeTier = when (filterTier) {
                    "RED" -> TriageTier.RED
                    "YELLOW" -> TriageTier.AMBER
                    "GREEN" -> TriageTier.GREEN
                    "BLUE" -> TriageTier.BLUE
                    else -> null
                },
                onTierClick = { tier ->
                    val code = tier.displayCode
                    filterTier = if (filterTier == code) "ALL" else code
                },
            )
        }

        // Collapsed by default QR / Token Verification
        TibbExpandableSection(
            title = s.queueVerifyTitle,
            summary = "Scan referral slip QR or enter TIBB-XXX token",
            icon = Icons.Rounded.QrCodeScanner,
            initiallyExpanded = false,
            headerTestTag = "queue_verify_expand",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = tokenQuery,
                    onValueChange = { tokenQuery = it },
                    label = { Text("TIBB-RED-XXXX Token") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("queue_token_input"),
                )
                TibbSecondaryPillButton(
                    onClick = { showQrScanner = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.heightIn(min = 52.dp).testTag("queue_scan_qr_btn"),
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan Referral QR", modifier = Modifier.size(20.dp))
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("queue_verify_btn"),
            ) {
                Text(s.queueVerifyBtn)
            }
        }

        Text("${s.queueEpisodesTitle} (${sortedFiltered.size})", style = MaterialTheme.typography.titleMedium, color = c.ink)
        if (sortedFiltered.isEmpty()) {
            TibbGlassCard(padding = 16.dp) {
                Text(s.queueEmptyText, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        } else {
            sortedFiltered.forEach { ep ->
                val tierColor = triageTierAccent(ep.triageTier)
                val tierSoftBg = triageTierSoftBg(ep.triageTier)
                TibbGlassCard(padding = 14.dp, borderColor = tierColor.copy(alpha = 0.55f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${ep.patientName} (${ep.ageBracket})",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(tierSoftBg)
                                    .border(1.dp, tierColor.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = ep.triageTier.shortLabel(locale),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tierColor,
                                )
                            }
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
                                modifier = Modifier.heightIn(min = 44.dp).testTag("queue_complete_${ep.id}"),
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
