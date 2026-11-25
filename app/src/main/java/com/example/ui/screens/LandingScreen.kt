package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.engine.HelpTopic
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.TibbBrandMark
import com.example.ui.components.TibbGlobe
import com.example.ui.components.UrgencyClassificationsPillStrip
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbExpandableSection
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.urduTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(
    locale: AppLocale,
    darkMode: Boolean,
    hasProfiles: Boolean,
    onSetLocale: (AppLocale) -> Unit,
    onToggleDarkMode: () -> Unit,
    onStartWorkspace: () -> Unit,
    onOpenAuthVault: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLegal: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    TibbGradientScaffoldBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= 680.dp
            val singleRowHeader = maxWidth >= 460.dp

            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 1. Clean Top Header (Brand + Language + Help & Theme)
                if (singleRowHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TibbBrandMark(modifier = Modifier.weight(1f, fill = false))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LandingLocalePillBar(locale = locale, onSetLocale = onSetLocale)
                            LandingHeaderActions(
                                darkMode = darkMode,
                                onOpenHelp = onOpenHelp,
                                onToggleDarkMode = onToggleDarkMode,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TibbBrandMark(modifier = Modifier.weight(1f, fill = false))
                            LandingHeaderActions(
                                darkMode = darkMode,
                                onOpenHelp = onOpenHelp,
                                onToggleDarkMode = onToggleDarkMode,
                            )
                        }
                        LandingLocalePillBar(
                            locale = locale,
                            onSetLocale = onSetLocale,
                            fillWidth = true,
                        )
                    }
                }

                // 2. Focused Value Proposition Hero
                TibbGlassCard(
                    padding = 24.dp,
                    borderColor = c.accentPastel.copy(alpha = 0.45f),
                    backgroundColor = c.surfaceElevated,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OfflineStatusBadge(label = s.careMadeSimple)
                            TibbGlobe(size = if (isWide) 76.dp else 56.dp)
                        }

                        Text(
                            text = s.tagline,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.displayLarge) else MaterialTheme.typography.displayLarge,
                            color = c.ink,
                        )

                        Text(
                            text = s.authSubline,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodyLarge) else MaterialTheme.typography.bodyLarge,
                            color = c.muted,
                        )

                        Spacer(Modifier.height(2.dp))

                        // 3. Single Dominant Primary Action
                        TibbPrimaryPillButton(
                            onClick = onStartWorkspace,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("landing_primary_cta"),
                        ) {
                            Text(
                                text = if (hasProfiles) {
                                    if (isUr) "ہیلتھ ہب کھولیں" else "Open Health Hub"
                                } else {
                                    if (isUr) "صحت کا معائنہ شروع کریں" else "Start Health Checkup"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }

                        // 4. Quiet Secondary Action (PIN Vault)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(
                                onClick = onOpenAuthVault,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("landing_auth_cta"),
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isUr) "پِن لاک فیملی والٹ کھولیں" else "PIN-Protected Family Vault",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.accent,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // 5. Progressive Disclosure: Features collapsed by default
                TibbExpandableSection(
                    title = if (isUr) "طب آپ کی کیا مدد کرتا ہے؟" else "What Tibb Helps You Do",
                    summary = if (isUr) "آف لائن معائنہ · 1122 ہسپتال · دوا و غذا" else "Offline Checkup · 1122 Hospitals · Diet & Alarms",
                    icon = Icons.Rounded.MedicalServices,
                    initiallyExpanded = false,
                    headerTestTag = "landing_features_expand_btn",
                    trailingHeaderContent = {
                        SectionHelpButton(topic = HelpTopic.LANDING_OVERVIEW, locale = locale)
                    },
                ) {
                    PillarRow(
                        number = "01",
                        icon = Icons.Rounded.MedicalServices,
                        title = if (isUr) "ڈاکٹر جیسا محفوظ معائنہ" else "Doctor-Grade Offline Checkup",
                        body = if (isUr) "ڈبلیو ایچ او اصولوں پر ایک وقت میں ایک سوال، اردو آواز اور فیملی تحفظ۔"
                        else "WHO IMCI & ETAT safety screening, one calm question at a time with Urdu audio.",
                    )
                    PillarRow(
                        number = "02",
                        icon = Icons.Rounded.LocalHospital,
                        title = if (isUr) "آف لائن 1122 اور قریبی ہسپتال" else "Offline 1122 & Nearest Clinics",
                        body = if (isUr) "بغیر انٹرنیٹ پاکستان کے تصدیق شدہ ہسپتال، کیو آر سلپ اور ہنگامی ہدایات۔"
                        else "Verified Pakistani hospitals & BHUs by district or GPS, plus offline QR slips.",
                    )
                    PillarRow(
                        number = "03",
                        icon = Icons.Rounded.Alarm,
                        title = if (isUr) "غذا، نبض اور دوا کے الارم" else "Diet Plan, Vitals & Urdu Alarms",
                        body = if (isUr) "دیسی غذا، کیمرہ نبض، لیب رپورٹ سکینر اور پی ڈی ایف خلاصہ۔"
                        else "Desi meal swaps, optical pulse estimator, offline lab OCR & PDF reports.",
                    )
                    Text(
                        text = s.offlineTriageClassificationsTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink,
                    )
                    UrgencyClassificationsPillStrip(locale = locale)
                }

                // 6. Quiet Trust Footer
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = s.offlineEncryptedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    TextButton(
                        onClick = onOpenLegal,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("landing_legal_btn"),
                    ) {
                        Text(
                            text = if (isUr) "طبی اصول اور رازداری" else "Disclaimer & Privacy",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingLocalePillBar(
    locale: AppLocale,
    onSetLocale: (AppLocale) -> Unit,
    fillWidth: Boolean = false,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(999.dp))
            .background(c.lilacGrey.copy(alpha = 0.65f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppLocale.entries.forEach { loc ->
            val selected = loc == locale
            Box(
                modifier = (if (fillWidth) Modifier.weight(1f) else Modifier)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) c.ctaBg else Color.Transparent)
                    .clickable { onSetLocale(loc) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("landing_locale_${loc.code}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = loc.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) c.ctaInk else c.ink.copy(alpha = 0.72f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun LandingHeaderActions(
    darkMode: Boolean,
    onOpenHelp: () -> Unit,
    onToggleDarkMode: () -> Unit,
) {
    val c = LocalTibbColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onOpenHelp,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.surfaceElevated)
                .border(0.75.dp, c.border, CircleShape)
                .testTag("landing_help_btn"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "Open Simple Help Guide",
                tint = c.accent,
                modifier = Modifier.size(19.dp),
            )
        }

        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.surfaceElevated)
                .border(0.75.dp, c.border, CircleShape)
                .testTag("landing_theme_toggle"),
        ) {
            Icon(
                imageVector = if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Toggle Light or Dark theme",
                tint = c.ink,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun PillarRow(
    number: String,
    icon: ImageVector,
    title: String,
    body: String,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceMuted.copy(alpha = 0.6f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(number, style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.accent)
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f, fill = false))
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}
