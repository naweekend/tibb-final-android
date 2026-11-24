package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StabilizationFocus(val labelEn: String, val labelUr: String) {
    BURN("Burn 20m Cooling", "جلنے پر 20 منٹ ٹھنڈک"),
    SEIZURE("Seizure Stopwatch", "دورہ / جھٹکے ٹائمر"),
    BLEEDING("Bleeding & Tourniquet", "خون روکنے کا ٹائمر"),
    HYPO("Low Sugar 15-15", "کم شوگر 15-15 اصول"),
    EN_ROUTE("En-Route Checklist", "ہسپتال روانگی چیک لسٹ"),
}

private fun formatMmSs(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val mm = s / 60
    val ss = s % 60
    return "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StabilizationTimers(
    locale: AppLocale,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    var focus by remember { mutableStateOf(StabilizationFocus.BURN) }

    // Burn 20m countdown
    var burnRemainingSec by remember { mutableIntStateOf(20 * 60) }
    var burnRunning by remember { mutableStateOf(false) }
    LaunchedEffect(burnRunning) {
        while (burnRunning && burnRemainingSec > 0) {
            delay(1000L)
            burnRemainingSec--
        }
        if (burnRemainingSec == 0) burnRunning = false
    }

    // Seizure stopwatch (5m status epilepticus alert)
    var seizureSec by remember { mutableIntStateOf(0) }
    var seizureRunning by remember { mutableStateOf(false) }
    LaunchedEffect(seizureRunning) {
        while (seizureRunning) {
            delay(1000L)
            seizureSec++
        }
    }

    // Bleeding pressure timer + tourniquet log
    var bleedSec by remember { mutableIntStateOf(0) }
    var bleedRunning by remember { mutableStateOf(false) }
    var tourniquetTime by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bleedRunning) {
        while (bleedRunning) {
            delay(1000L)
            bleedSec++
        }
    }

    // Hypo 15-15 timer
    var hypoSec by remember { mutableIntStateOf(15 * 60) }
    var hypoRunning by remember { mutableStateOf(false) }
    LaunchedEffect(hypoRunning) {
        while (hypoRunning && hypoSec > 0) {
            delay(1000L)
            hypoSec--
        }
        if (hypoSec == 0) hypoRunning = false
    }

    val checklistItems = remember(isUr) {
        if (isUr) listOf(
            "مریض کا شناختی کارڈ اور پرانی دوائیں/پرچہ ساتھ رکھیں",
            "اگر سانس کھلا ہے تو کروٹ پر (Recovery Position) لٹائیں",
            "بے ہوشی میں منہ میں پانی یا دوا ہرگز نہ ڈالیں",
            "ایمبولینس یا 1122 کو قریبی نمایاں نشانی بتا دیں",
        ) else listOf(
            "Keep patient CNIC, prior medicines/strips, and QR referral slip ready",
            "Place drowsy/unconscious breathing patient on their side (Recovery Position)",
            "NEVER put water, milk, honey, or tablets into an unconscious mouth",
            "Share exact nearest chowk/landmark with Rescue 1122 dispatcher",
        )
    }
    val checkedStates = remember { mutableStateListOf(false, false, false, false) }

    TibbGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = c.amber.copy(alpha = 0.5f),
        padding = 18.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = c.amber)
                Column {
                    Text(
                        text = s.farHospitalTitle,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                    Text(
                        text = s.farHospitalSubtitle,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StabilizationFocus.entries.forEach { tab ->
                    val selected = focus == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) c.ctaBg else c.surfaceMuted)
                            .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                            .clickable { focus = tab }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("stabilization_tab_${tab.name.lowercase()}"),
                    ) {
                        Text(
                            text = if (isUr) tab.labelUr else tab.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) c.ctaInk else c.ink,
                        )
                    }
                }
            }

            when (focus) {
                StabilizationFocus.BURN -> {
                    TimerPanel(
                        valueText = formatMmSs(burnRemainingSec),
                        caption = if (isUr) {
                            "جلی ہوئی جگہ پر 20 منٹ تک نلکے کا ٹھنڈا بہتا پانی ڈالیں (برف، ٹوتھ پیسٹ یا مکھن ہرگز نہ لگائیں)۔"
                        } else {
                            "Hold under cool running tap water for full 20 minutes. Never apply ice, toothpaste, butter, or egg."
                        },
                        running = burnRunning,
                        onToggle = { burnRunning = !burnRunning },
                        onReset = {
                            burnRunning = false
                            burnRemainingSec = 20 * 60
                        },
                        tagPrefix = "burn",
                        isAlert = burnRemainingSec == 0,
                        isUr = isUr,
                    )
                }

                StabilizationFocus.SEIZURE -> {
                    val fiveMinWarning = seizureSec >= 300
                    if (fiveMinWarning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.dangerSoft)
                                .border(1.dp, c.dangerRed, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.Emergency, contentDescription = null, tint = c.dangerRed)
                                Text(
                                    text = if (isUr) "5 منٹ سے زائد دورہ (Status Epilepticus) — فوری 1122 ایمرجنسی!" else "STATUS EPILEPTICUS (> 5:00) — Call Rescue 1122 immediately!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.dangerRed,
                                )
                            }
                        }
                    }
                    TimerPanel(
                        valueText = formatMmSs(seizureSec),
                        caption = if (isUr) {
                            "مریض کو کروٹ پر لٹائیں، سر کے نیچے نرم کپڑا رکھیں، منہ میں چمچ یا انگلی ہرگز نہ ڈالیں۔"
                        } else {
                            "Time the fit. Turn patient onto their side, cushion head, NEVER put anything inside the mouth."
                        },
                        running = seizureRunning,
                        onToggle = { seizureRunning = !seizureRunning },
                        onReset = {
                            seizureRunning = false
                            seizureSec = 0
                        },
                        tagPrefix = "seizure",
                        isAlert = fiveMinWarning,
                        isUr = isUr,
                    )
                }

                StabilizationFocus.BLEEDING -> {
                    TimerPanel(
                        valueText = formatMmSs(bleedSec),
                        caption = if (isUr) {
                            "صاف کپڑے سے زخم پر مسلسل سیدھا دباؤ برقرار رکھیں۔ اگر ٹورنیکے باندھیں تو وقت لازمی نوٹ کریں۔"
                        } else {
                            "Maintain firm continuous direct pressure with clean folded cloth. Log exact tourniquet time if applied."
                        },
                        running = bleedRunning,
                        onToggle = { bleedRunning = !bleedRunning },
                        onReset = {
                            bleedRunning = false
                            bleedSec = 0
                            tourniquetTime = null
                        },
                        tagPrefix = "bleed",
                        isAlert = false,
                        isUr = isUr,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TibbSecondaryPillButton(
                            onClick = {
                                val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                                tourniquetTime = sdf.format(Date())
                            },
                            modifier = Modifier.testTag("log_tourniquet_time_button"),
                        ) {
                            Text(if (isUr) "ٹورنیکے وقت محفوظ کریں" else "Log Tourniquet Time")
                        }
                        if (tourniquetTime != null) {
                            Text(
                                text = "Tourniquet @ $tourniquetTime",
                                style = TabularNumberStyle.copy(fontWeight = FontWeight.SemiBold),
                                color = c.dangerRed,
                            )
                        }
                    }
                }

                StabilizationFocus.HYPO -> {
                    TimerPanel(
                        valueText = formatMmSs(hypoSec),
                        caption = if (isUr) {
                            "ہوش میں ہو تو 15 گرام میٹھا (3 چمچ چینی پانی یا آدھا گلاس جوس) دے کر 15 منٹ بعد دوبارہ شوگر چیک کریں۔"
                        } else {
                            "Conscious only: give 15g fast sugar (3 tsp sugar in water or half glass juice), wait 15 min, recheck."
                        },
                        running = hypoRunning,
                        onToggle = { hypoRunning = !hypoRunning },
                        onReset = {
                            hypoRunning = false
                            hypoSec = 15 * 60
                        },
                        tagPrefix = "hypo",
                        isAlert = hypoSec == 0,
                        isUr = isUr,
                    )
                }

                StabilizationFocus.EN_ROUTE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        checklistItems.forEachIndexed { idx, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.surfaceMuted)
                                    .clickable { checkedStates[idx] = !checkedStates[idx] }
                                    .padding(12.dp)
                                    .testTag("en_route_check_$idx"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = if (checkedStates[idx]) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = line,
                                    tint = if (checkedStates[idx]) c.safeGreen else c.muted,
                                )
                                Text(
                                    text = line,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                                    color = c.ink,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerPanel(
    valueText: String,
    caption: String,
    running: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    tagPrefix: String,
    isAlert: Boolean,
    isUr: Boolean,
) {
    val c = LocalTibbColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceMuted)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = valueText,
            style = TabularNumberStyle.copy(
                fontSize = MaterialTheme.typography.displayLarge.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = if (isAlert) c.dangerRed else c.ink,
        )
        Text(
            text = caption,
            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
            color = c.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TibbPrimaryPillButton(
                onClick = onToggle,
                modifier = Modifier.testTag("${tagPrefix}_timer_toggle"),
            ) {
                Icon(
                    imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(if (running) (if (isUr) "روکیں" else "Pause") else (if (isUr) "شروع کریں" else "Start"))
            }
            TibbSecondaryPillButton(
                onClick = onReset,
                modifier = Modifier.testTag("${tagPrefix}_timer_reset"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(if (isUr) "ری سیٹ" else "Reset")
            }
        }
    }
}
