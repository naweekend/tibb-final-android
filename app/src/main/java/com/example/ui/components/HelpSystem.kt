package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLocale
import com.example.engine.HelpEntry
import com.example.engine.HelpTopic
import com.example.engine.SimpleHelpCatalog
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

/**
 * Small, subtle transparent-ish (?) button for any section or card.
 * Keeps a generous interactive touch area while remaining visually light.
 */
@Composable
fun SectionHelpButton(
    topic: HelpTopic,
    locale: AppLocale,
    modifier: Modifier = Modifier,
    onDarkSurface: Boolean = false,
    onMistSurface: Boolean = false,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    var showPopup by remember { mutableStateOf(false) }
    var showFullGuide by remember { mutableStateOf(false) }

    val badgeBg = when {
        onDarkSurface -> Color.White.copy(alpha = 0.10f)
        onMistSurface -> c.mistInk.copy(alpha = 0.10f)
        else -> c.ink.copy(alpha = 0.06f)
    }
    val badgeBorder = when {
        onDarkSurface -> Color.White.copy(alpha = 0.24f)
        onMistSurface -> c.mistInk.copy(alpha = 0.24f)
        else -> c.ink.copy(alpha = 0.16f)
    }
    val badgeInk = when {
        onDarkSurface -> Color(0xFFE6EFE9).copy(alpha = 0.84f)
        onMistSurface -> c.mistInk.copy(alpha = 0.80f)
        else -> c.muted.copy(alpha = 0.90f)
    }

    val desc = when (locale) {
        AppLocale.UR -> "اس حصے کی سادہ وضاحت"
        AppLocale.ROMAN -> "Is hissay ki aasan madad"
        AppLocale.EN -> "Simple help for this section"
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { showPopup = true }
            .semantics { contentDescription = desc }
            .testTag("help_btn_${topic.name.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.dp, badgeBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = badgeInk,
            )
        }
    }

    if (showPopup) {
        val entry = remember(topic, locale) { SimpleHelpCatalog.getEntry(topic, locale) }
        SimpleHelpPopupDialog(
            entry = entry,
            locale = locale,
            onDismiss = { showPopup = false },
            onOpenAllFeatures = {
                showPopup = false
                showFullGuide = true
            },
        )
    }

    if (showFullGuide) {
        TibbFullHelpSheet(
            locale = locale,
            onDismiss = { showFullGuide = false },
        )
    }
}

@Composable
fun SimpleHelpPopupDialog(
    entry: HelpEntry,
    locale: AppLocale,
    onDismiss: () -> Unit,
    onOpenAllFeatures: () -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val whatTitle = when (locale) {
        AppLocale.UR -> "یہ کیا ہے؟"
        AppLocale.ROMAN -> "Yeh kya hai?"
        AppLocale.EN -> "What is this?"
    }
    val howTitle = when (locale) {
        AppLocale.UR -> "استعمال کیسے کریں؟"
        AppLocale.ROMAN -> "Kaise istemal karein?"
        AppLocale.EN -> "How to use it"
    }
    val tipTitle = when (locale) {
        AppLocale.UR -> "ضروری بات"
        AppLocale.ROMAN -> "Zaroori baat"
        AppLocale.EN -> "Good to know"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.ink,
        textContentColor = c.ink,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = entry.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent,
                    )
                }
                Text(
                    text = entry.title,
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                    color = c.ink,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(whatTitle, style = MaterialTheme.typography.labelLarge, color = c.accent)
                    Text(
                        text = entry.simpleSummary,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceMuted)
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = c.accent, modifier = Modifier.size(15.dp))
                            Text(howTitle, style = MaterialTheme.typography.labelMedium, color = c.ink)
                        }
                        Text(
                            text = entry.howToUse,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.amberSoft)
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.amber, modifier = Modifier.size(15.dp))
                            Text(tipTitle, style = MaterialTheme.typography.labelMedium, color = c.amber)
                        }
                        Text(
                            text = entry.safetyOrTip,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onOpenAllFeatures,
                modifier = Modifier.heightIn(min = 48.dp).testTag("help_popup_full_guide_btn"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when (locale) {
                        AppLocale.UR -> "تمام فیچرز کی رہنمائی"
                        AppLocale.ROMAN -> "Poori App Guide"
                        AppLocale.EN -> "All App Features"
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        confirmButton = {
            TibbPrimaryPillButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("help_popup_ok_btn"),
            ) {
                Text(
                    text = when (locale) {
                        AppLocale.UR -> "سمجھ آ گیا"
                        AppLocale.ROMAN -> "Samajh Aa Gaya"
                        AppLocale.EN -> "Got It"
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TibbFullHelpSheet(
    locale: AppLocale,
    onDismiss: () -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    var query by remember { mutableStateOf("") }
    val allEntries = remember(locale) { SimpleHelpCatalog.getAllEntries(locale) }
    val filtered = remember(allEntries, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allEntries
        else allEntries.filter {
            it.title.lowercase().contains(q) ||
                it.badge.lowercase().contains(q) ||
                it.simpleSummary.lowercase().contains(q) ||
                it.howToUse.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when (locale) {
                            AppLocale.UR -> "طب کی آسان مکمل رہنمائی (?)"
                            AppLocale.ROMAN -> "Tibb Ki Asaan Poori Guide (?)"
                            AppLocale.EN -> "Simple Guide to Every Feature (?)"
                        },
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.ink,
                    )
                    Text(
                        text = when (locale) {
                            AppLocale.UR -> "ہر بٹن اور مشکل لفظ کا آسان مطلب — کسی طبی علم کی ضرورت نہیں۔"
                            AppLocale.ROMAN -> "Har button aur mushkil lafz (jaise BHU, OCR, BMI) ka aasan matlab."
                            AppLocale.EN -> "Every button & medical word (BHU, OCR, BMI, DRAP) explained in plain words."
                        },
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Help Guide", tint = c.muted)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = c.muted) },
                placeholder = {
                    Text(
                        when (locale) {
                            AppLocale.UR -> "تلاش کریں (مثلاً BHU، OCR، بی پی، شوگر، الارم)..."
                            AppLocale.ROMAN -> "Search karein (jaise BHU, OCR, BP, Sugar, Alarm)..."
                            AppLocale.EN -> "Search (e.g. BHU, OCR, BP, Sugar, QR, Backup)..."
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_help_search_input"),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filtered.isEmpty()) {
                    TibbGlassCard(padding = 16.dp) {
                        Text(
                            text = when (locale) {
                                AppLocale.UR -> "کوئی نتیجہ نہیں ملا۔ کوئی اور لفظ لکھیں یا تلاش صاف کریں۔"
                                AppLocale.ROMAN -> "Koi nateeja nahi mila. Search saaf karein."
                                AppLocale.EN -> "No matching topic found. Clear the search box to see all ${allEntries.size} guides."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                } else {
                    filtered.forEach { item ->
                        TibbGlassCard(padding = 14.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.title,
                                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                                        color = c.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(c.accentSoft)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(item.badge, style = MaterialTheme.typography.labelSmall, color = c.accent)
                                    }
                                }
                                Text(
                                    text = item.simpleSummary,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                    color = c.ink,
                                )
                                Text(
                                    text = item.howToUse,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                    color = c.muted,
                                )
                                Text(
                                    text = item.safetyOrTip,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.labelSmall) else MaterialTheme.typography.labelSmall,
                                    color = c.amber,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
