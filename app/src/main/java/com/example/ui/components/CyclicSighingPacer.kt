package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.engine.AudioFeedbackManager
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import kotlinx.coroutines.delay

private enum class SighPhase(val durationMs: Long, val labelEn: String, val labelUr: String, val targetScale: Float) {
    IDLE(0L, "Ready — Double Inhale, Long Exhale", "تیار — دو بار سانس اندر، لمبا سانس باہر", 0.72f),
    INHALE_1(2200L, "1. Deep Inhale through Nose", "1. ناک سے گہرا سانس اندر لیں", 1.0f),
    INHALE_2(1300L, "2. Quick Top-Up Sip In", "2. تھوڑا سا مزید سانس اوپر کھینچیں", 1.14f),
    EXHALE(6000L, "3. Long Slow Exhale through Mouth", "3. منہ سے آہستہ لمبا سانس باہر نکالیں", 0.66f),
}

@Composable
fun CyclicSighingPacer(
    locale: AppLocale,
    audioManager: AudioFeedbackManager,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    var running by remember { mutableStateOf(false) }
    var audioEnabled by remember { mutableStateOf(true) }
    var phase by remember { mutableStateOf(SighPhase.IDLE) }
    var cyclesCompleted by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        if (!running) {
            phase = SighPhase.IDLE
            return@LaunchedEffect
        }
        while (running) {
            phase = SighPhase.INHALE_1
            if (audioEnabled) {
                audioManager.speakOfflineGuidance(
                    if (isUr) "ناک سے گہرا سانس اندر لیں" else "Breathe in deeply through your nose",
                    if (isUr) "ur-PK" else "en-US",
                )
            }
            delay(SighPhase.INHALE_1.durationMs)
            if (!running) break

            phase = SighPhase.INHALE_2
            if (audioEnabled) {
                audioManager.speakOfflineGuidance(
                    if (isUr) "تھوڑا سا مزید سانس لیں" else "Take one more quick sip in",
                    if (isUr) "ur-PK" else "en-US",
                )
            }
            delay(SighPhase.INHALE_2.durationMs)
            if (!running) break

            phase = SighPhase.EXHALE
            if (audioEnabled) {
                audioManager.speakOfflineGuidance(
                    if (isUr) "اب منہ سے آہستہ لمبا سانس باہر نکالیں" else "Now exhale slowly through your mouth",
                    if (isUr) "ur-PK" else "en-US",
                )
            }
            delay(SighPhase.EXHALE.durationMs)
            if (!running) break
            cyclesCompleted++
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = phase.targetScale,
        animationSpec = tween(durationMillis = phase.durationMs.toInt().coerceAtLeast(350)),
        label = "sigh_circle_scale",
    )

    TibbGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = c.accent.copy(alpha = 0.35f),
        padding = 18.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = c.accent,
                    )
                    Text(
                        text = s.calmPacerTitle,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                }
                TibbSecondaryPillButton(
                    onClick = { audioEnabled = !audioEnabled },
                    modifier = Modifier.testTag("sigh_pacer_audio_toggle"),
                ) {
                    Icon(
                        imageVector = if (audioEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = if (audioEnabled) "Mute audio cues" else "Enable audio cues",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(if (audioEnabled) "Audio" else "Muted", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                text = s.calmPacerSubtitle,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                color = c.muted,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .size(148.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = if (running) 0.26f else 0.14f))
                        .border(2.dp, c.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$cyclesCompleted",
                            style = TabularNumberStyle.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = c.ink,
                        )
                        Text(
                            text = if (isUr) "چکر" else "cycles",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                        )
                    }
                }
            }

            Text(
                text = if (isUr) phase.labelUr else phase.labelEn,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                color = c.accent,
                textAlign = TextAlign.Center,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TibbPrimaryPillButton(
                    onClick = { running = !running },
                    modifier = Modifier.testTag("sigh_pacer_start_stop"),
                ) {
                    Icon(
                        imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (running) (if (isUr) "روکیں" else "Stop")
                        else (if (isUr) "مشق شروع کریں" else "Start Pacer")
                    )
                }

                if (cyclesCompleted > 0) {
                    TibbSecondaryPillButton(
                        onClick = {
                            running = false
                            cyclesCompleted = 0
                        },
                        modifier = Modifier.testTag("sigh_pacer_reset"),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset cycles", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
