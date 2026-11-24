package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLocale
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.EpisodeInput
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.TriageEvaluationResult
import com.example.data.model.TriageTier
import com.example.engine.AudioFeedbackManager
import com.example.engine.HelpTopic
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.components.CyclicSighingPacer
import com.example.ui.components.OfflineStatusBadge
import com.example.ui.components.SectionHelpButton
import com.example.ui.components.SpeechDictationButton
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckupAndChatScreen(
    locale: AppLocale,
    accountId: String,
    activePerson: PatientProfile?,
    checkupSubMode: Int, // 0 = Live Chat, 1 = Guided Form
    chatThreads: List<ChatThread>,
    activeThreadId: String?,
    activeEvaluation: TriageEvaluationResult?,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    userGpsCoords: GeoCoords?,
    gpsLocating: Boolean,
    onSetSubMode: (Int) -> Unit,
    onSelectThread: (String) -> Unit,
    onCreateNewThread: () -> Unit,
    onDetectCrossPerson: (String) -> PatientProfile?,
    onSwitchPersonAndSend: (PatientProfile, String, Uri?, String?, String?) -> Unit,
    onSendMessage: (String, Uri?, String?, String?) -> Unit,
    onCompleteEpisode: (EpisodeInput, TriageEvaluationResult) -> Unit,
    onClearEvaluation: () -> Unit,
    onRequestGps: () -> Unit,
    onOpenDialer: (String) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    if (activePerson == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.onboardingSubtitle, color = c.muted)
        }
        return
    }

    var draft by remember { mutableStateOf("") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingMediaKind by remember { mutableStateOf<String?>(null) }
    var showCameraModal by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingPhotoUri = uri
            pendingPhotoPath = null
            pendingMediaKind = "IMAGE"
        }
    }

    val crossPersonMention = remember(draft, activePerson.id) {
        if (draft.length >= 3) onDetectCrossPerson(draft) else null
    }

    val personThreads = remember(chatThreads, activePerson.id) {
        chatThreads.filter { it.personId == activePerson.id }
    }
    val currentThread = remember(personThreads, activeThreadId) {
        personThreads.firstOrNull { it.id == activeThreadId } ?: personThreads.firstOrNull()
    }

    BackHandler(enabled = activeEvaluation != null || checkupSubMode == 1) {
        if (activeEvaluation != null) onClearEvaluation()
        else onSetSubMode(0)
    }

    if (showCameraModal) {
        CameraMultimodalModal(
            locale = locale,
            accountId = accountId,
            qrOnlyMode = false,
            onCapturedPhoto = { path, kind ->
                pendingPhotoPath = path
                pendingPhotoUri = null
                pendingMediaKind = kind
                showCameraModal = false
            },
            onQrScanned = { showCameraModal = false },
            onDismiss = { showCameraModal = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sub-Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(0 to s.agentModeBtn, 1 to s.structuredCheckupBtn).forEach { (idx, lbl) ->
                val sel = checkupSubMode == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) c.ctaBg else Color.Transparent)
                        .clickable { onSetSubMode(idx) }
                        .padding(vertical = 10.dp)
                        .testTag(if (idx == 0) "checkup_mode_chat" else "checkup_mode_guided"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelLarge, color = if (sel) c.ctaInk else c.ink)
                }
            }
        }

        if (checkupSubMode == 1) {
            if (activeEvaluation != null) {
                TriageResultView(
                    result = activeEvaluation,
                    patient = activePerson,
                    locale = locale,
                    triageEngine = triageEngine,
                    audioManager = audioManager,
                    userGpsCoords = userGpsCoords,
                    gpsLocating = gpsLocating,
                    onRequestGps = onRequestGps,
                    onOpenDialer = onOpenDialer,
                    onShowToast = onShowToast,
                    onResetNewCheckup = onClearEvaluation,
                )
            } else {
                GuidedIntakeForm(
                    patient = activePerson,
                    locale = locale,
                    triageEngine = triageEngine,
                    audioManager = audioManager,
                    userCoords = userGpsCoords,
                    onCompleteEpisode = onCompleteEpisode,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OfflineStatusBadge(
                    label = s.activeEngineOffline,
                    modifier = Modifier.weight(1f, fill = false),
                )
                SectionHelpButton(topic = HelpTopic.CHECKUP_CHAT_LIVE, locale = locale)
            }

            // Thread Pills + New Chat
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TibbSecondaryPillButton(
                    onClick = onCreateNewThread,
                    modifier = Modifier.testTag("chat_new_thread_btn"),
                ) {
                    Icon(Icons.Rounded.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.newChatBtn, style = MaterialTheme.typography.labelSmall)
                }
                personThreads.forEach { thr ->
                    val sel = thr.id == currentThread?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (sel) c.accent.copy(alpha = 0.24f) else c.surface)
                            .border(1.dp, if (sel) c.accent else c.border, RoundedCornerShape(999.dp))
                            .clickable { onSelectThread(thr.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(thr.title, style = MaterialTheme.typography.labelSmall, color = c.ink, maxLines = 1)
                    }
                }
            }

            // Cross-Person Mention Guard
            if (crossPersonMention != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.amberSoft)
                        .border(1.dp, c.amber, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mentioned ${crossPersonMention.name} (${crossPersonMention.relationship.labelEn})",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.ink,
                            )
                            Text(
                                text = "Keep records separate by switching before saving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.muted,
                            )
                        }
                        TibbPrimaryPillButton(
                            onClick = {
                                val txt = draft.trim()
                                if (txt.isNotEmpty() || pendingPhotoUri != null || pendingPhotoPath != null) {
                                    onSwitchPersonAndSend(
                                        crossPersonMention,
                                        txt.ifBlank { "Visual checkup photo" },
                                        pendingPhotoUri,
                                        pendingPhotoPath,
                                        pendingMediaKind,
                                    )
                                    draft = ""
                                    pendingPhotoUri = null
                                    pendingPhotoPath = null
                                    pendingMediaKind = null
                                }
                            },
                            modifier = Modifier.testTag("cross_person_switch_send"),
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Switch & Save")
                        }
                    }
                }
            }

            // Messages or Empty Welcome
            val msgs = currentThread?.messages.orEmpty()
            if (msgs.isEmpty()) {
                TibbGlassCard(padding = 18.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = s.agentPromptTitle,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.titleMedium) else MaterialTheme.typography.titleMedium,
                            color = c.ink,
                        )
                        Text(
                            text = s.heroSubline,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                        Text(s.agentQuickChipsLabel, style = MaterialTheme.typography.labelMedium, color = c.accent)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "High fever for 3 days" to "3 دن سے تیز بخار",
                                "Child has mild cough" to "بچے کو ہلکی کھانسی ہے",
                                "Feeling anxious and heart racing" to "گھبراہٹ محسوس ہو رہی ہے",
                                "Chest pain and sweating" to "سینے میں شدید درد اور پسینہ",
                            ).forEach { (en, ur) ->
                                val chipText = if (isUr) ur else en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(c.surfaceMuted)
                                        .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                        .clickable { onSendMessage(chipText, null, null, null) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(chipText, style = MaterialTheme.typography.labelSmall, color = c.ink)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    msgs.forEach { msg ->
                        ChatMessageCard(
                            msg = msg,
                            locale = locale,
                            audioManager = audioManager,
                            onQuickReply = { reply -> onSendMessage(reply, null, null, null) },
                            onOpenDialer = onOpenDialer,
                        )
                    }
                }
            }

            // Pending attachment indicator
            if (pendingPhotoPath != null || pendingPhotoUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.accentSoft)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Attachment ready (EXIF GPS will be stripped locally)", style = MaterialTheme.typography.labelSmall, color = c.ink)
                    Text(
                        "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dangerRed,
                        modifier = Modifier.clickable {
                            pendingPhotoUri = null
                            pendingPhotoPath = null
                            pendingMediaKind = null
                        },
                    )
                }
            }

            // Composer Bar
            TibbGlassCard(padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(s.agentPromptPlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("chat_message_input"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(c.surfaceMuted)
                                    .testTag("chat_photo_picker_btn"),
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = s.uploadReportBtn, tint = c.ink)
                            }
                            IconButton(
                                onClick = { showCameraModal = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(c.surfaceMuted)
                                    .testTag("chat_live_camera_btn"),
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = s.liveCameraBtn, tint = c.accent)
                            }
                            SpeechDictationButton(
                                locale = locale,
                                compact = true,
                                onResultText = { spoken ->
                                    draft = if (draft.isBlank()) spoken else "$draft $spoken"
                                },
                                onError = onShowToast,
                            )
                        }

                        TibbPrimaryPillButton(
                            enabled = draft.isNotBlank() || pendingPhotoUri != null || pendingPhotoPath != null,
                            onClick = {
                                val textToSend = draft.trim().ifBlank { "Visual checkup photo" }
                                onSendMessage(textToSend, pendingPhotoUri, pendingPhotoPath, pendingMediaKind)
                                draft = ""
                                pendingPhotoUri = null
                                pendingPhotoPath = null
                                pendingMediaKind = null
                            },
                            modifier = Modifier.testTag("chat_send_btn"),
                        ) {
                            Text(s.agentSendBtn)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageCard(
    msg: ChatMessage,
    locale: AppLocale,
    audioManager: AudioFeedbackManager,
    onQuickReply: (String) -> Unit,
    onOpenDialer: (String) -> Unit,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val isUser = msg.role == "user"
    val isRed = msg.triageTier == TriageTier.RED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isRed -> c.dangerSoft
                    isUser -> c.accent.copy(alpha = 0.16f)
                    else -> c.surface
                }
            )
            .border(1.dp, if (isRed) c.dangerRed else c.border, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (msg.mediaPath != null) {
            AsyncImage(
                model = File(msg.mediaPath),
                contentDescription = "Attached report or symptom photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }

        val displayBody = if (isUr && !msg.text_ur.isNullOrBlank()) msg.text_ur else msg.text
        Text(
            text = displayBody,
            style = if (isUr && !isUser) urduTextStyle(MaterialTheme.typography.bodyMedium) else MaterialTheme.typography.bodyMedium,
            color = c.ink,
        )

        if (!isUser && msg.firstAid != null) {
            val fa = msg.firstAid
            val steps = if (isUr) fa.do_now_ur else fa.do_now_en
            steps.forEachIndexed { idx, step ->
                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = c.ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TibbPrimaryPillButton(
                    onClick = { onOpenDialer("1122") },
                    containerColor = c.dangerRed,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call 1122")
                }
                TibbSecondaryPillButton(
                    onClick = { onOpenDialer("115") },
                    borderColor = c.dangerRed,
                    contentColor = c.dangerRed,
                ) { Text("Edhi 115") }
            }
        }

        if (!isUser && msg.stayHomeGuidance != null) {
            val g = msg.stayHomeGuidance
            Text(
                text = if (isUr) g.title_ur else g.title_en,
                style = MaterialTheme.typography.labelLarge,
                color = if (isRed) c.dangerRed else c.accent,
            )
        }

        if (!isUser && msg.suggestCalmPacer) {
            CyclicSighingPacer(locale = locale, audioManager = audioManager)
        }

        if (!isUser) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TibbSecondaryPillButton(
                    onClick = {
                        audioManager.speakOfflineGuidance(msg.text_ur ?: msg.text, if (msg.text_ur != null) "ur-PK" else "en-US")
                    },
                ) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(TrilingualStrings.get(locale).urduAudio, style = MaterialTheme.typography.labelSmall)
                }
                msg.engineUsed?.let { eng ->
                    Text(eng, style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }

        if (msg.quickReplies.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                msg.quickReplies.forEach { qr ->
                    val lbl = if (isUr) qr.label_ur else qr.label_en
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.surfaceMuted)
                            .border(1.dp, c.border, RoundedCornerShape(999.dp))
                            .clickable { onQuickReply(lbl) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(lbl, style = MaterialTheme.typography.labelSmall, color = c.ink)
                    }
                }
            }
        }
    }
}
