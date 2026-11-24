package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.engine.TrilingualStrings
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.TibbBrandMark
import com.example.ui.components.TibbGlobe
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbMistCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

@Composable
fun LandingScreen(
    locale: AppLocale,
    darkMode: Boolean,
    hasProfiles: Boolean,
    onSetLocale: (AppLocale) -> Unit,
    onToggleDarkMode: () -> Unit,
    onStartWorkspace: () -> Unit,
    onOpenAuthVault: () -> Unit,
    onOpenLegal: () -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    TibbGradientScaffoldBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= 700.dp

            Column(
                modifier = Modifier.widthIn(max = 920.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TibbBrandMark()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surface)
                                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            AppLocale.entries.forEach { loc ->
                                val selected = loc == locale
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) c.ctaBg else c.surface)
                                        .clickable { onSetLocale(loc) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("landing_locale_${loc.code}"),
                                ) {
                                    Text(
                                        loc.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) c.ctaInk else c.ink,
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surface)
                                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                .testTag("landing_theme_toggle"),
                        ) {
                            Icon(
                                imageVector = if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle Light or Dark theme",
                                tint = c.ink,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Hero Section (Split on tablet, stacked on phone)
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HeroCopyColumn(
                            locale = locale,
                            hasProfiles = hasProfiles,
                            onStartWorkspace = onStartWorkspace,
                            onOpenAuthVault = onOpenAuthVault,
                            modifier = Modifier.weight(1.1f),
                        )
                        HeroVisualPreviewCard(
                            locale = locale,
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                } else {
                    HeroCopyColumn(
                        locale = locale,
                        hasProfiles = hasProfiles,
                        onStartWorkspace = onStartWorkspace,
                        onOpenAuthVault = onOpenAuthVault,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HeroVisualPreviewCard(
                        locale = locale,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 3 Clinical Pillars
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillarCard(
                        number = "01",
                        icon = Icons.Rounded.MedicalServices,
                        title = if (isUr) "ڈاکٹر جیسا محفوظ معائنہ" else "Doctor-Grade Offline Intake",
                        body = if (isUr) "ڈبلیو ایچ او اصولوں پر مبنی ایک وقت میں ایک سوال، اردو آواز اور فیملی ممبر تحفظ۔"
                        else "WHO IMCI & ETAT safety rules, one calm question at a time, Urdu audio & cross-person guard.",
                    )
                    PillarCard(
                        number = "02",
                        icon = Icons.Rounded.LocalHospital,
                        title = if (isUr) "آف لائن 1122 اور قریبی ہسپتال" else "Offline 1122 & District Routing",
                        body = if (isUr) "بغیر انٹرنیٹ پاکستان کے تصدیق شدہ ہسپتال، کیو آر ریفرل سلپ اور ہنگامی ابتدائی امداد۔"
                        else "Verified Pakistani hospitals & BHUs ranked by GPS or district, plus offline QR referral slips.",
                    )
                    PillarCard(
                        number = "03",
                        icon = Icons.Rounded.Alarm,
                        title = if (isUr) "روزانہ غذا، نبض اور دوا کے الارم" else "Daily Diet, Vitals & Urdu Alarms",
                        body = if (isUr) "جنوبی ایشیائی بی ایم آئی غذا، کیمرہ نبض، آف لائن لیب سکینر اور پی ڈی ایف رپورٹ۔"
                        else "Desi meal swaps, CameraX optical pulse, on-device ML Kit lab OCR & native PDF summaries.",
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.offlineEncryptedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                    TextButton(
                        onClick = onOpenLegal,
                        modifier = Modifier.testTag("landing_legal_btn"),
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
private fun HeroCopyColumn(
    locale: AppLocale,
    hasProfiles: Boolean,
    onStartWorkspace: () -> Unit,
    onOpenAuthVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OfflineStatusBadge(label = s.careMadeSimple)

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

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TibbPrimaryPillButton(
                onClick = onStartWorkspace,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("landing_primary_cta"),
            ) {
                Text(
                    text = if (hasProfiles) {
                        if (isUr) "ہیلتھ ہب کھولیں" else "Open Your Health Hub"
                    } else {
                        if (isUr) "مکمل معائنہ شروع کریں" else "Start Doctor Intake"
                    },
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        TibbSecondaryPillButton(
            onClick = onOpenAuthVault,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("landing_auth_cta"),
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isUr) "پِن لاک والٹ کھولیں / بنائیں" else "Open or Create PIN-Protected Vault")
        }
    }
}

@Composable
private fun HeroVisualPreviewCard(
    locale: AppLocale,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR

    TibbMistCard(modifier = modifier, padding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUr) "فیملی اور بی ایچ یو ہیلتھ ہب" else "PAKISTAN HOUSEHOLD & BHU HUB",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.mistInk.copy(alpha = 0.72f),
                    )
                    Text(
                        text = if (isUr) "مکمل آف لائن تحفظ" else "100% On-Device Clinical Core",
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                        color = c.mistInk,
                    )
                }
                TibbGlobe(size = 86.dp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PreviewMiniMetric(
                    icon = Icons.Rounded.DirectionsWalk,
                    label = if (isUr) "روزانہ ہدف" else "Motion Sensor",
                    value = "7,500",
                    modifier = Modifier.weight(1f),
                )
                PreviewMiniMetric(
                    icon = Icons.Rounded.WaterDrop,
                    label = if (isUr) "پانی و نمکیات" else "ORS & Water",
                    value = "2.5 L",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PreviewMiniMetric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.mistInk.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.mistInk, modifier = Modifier.size(18.dp))
        Text(value, style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.mistInk)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.mistInk.copy(alpha = 0.8f))
    }
}

@Composable
private fun PillarCard(
    number: String,
    icon: ImageVector,
    title: String,
    body: String,
) {
    val c = LocalTibbColors.current
    TibbGlassCard(padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(number, style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold), color = c.accent)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }
    }
}
