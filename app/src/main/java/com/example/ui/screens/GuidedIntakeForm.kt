package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLocale
import com.example.data.model.BodyRegion
import com.example.data.model.EmergencyCategory
import com.example.data.model.EpisodeInput
import com.example.data.model.EpisodeVitals
import com.example.data.model.GeoCoords
import com.example.data.model.HydrationStatus
import com.example.data.model.MeasurementMethod
import com.example.data.model.PatientProfile
import com.example.data.model.PrimaryComplaint
import com.example.data.model.TemperatureBand
import com.example.data.model.TriageAnswers
import com.example.data.model.TriageEvaluationResult
import com.example.engine.AudioFeedbackManager
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.TibbGlassCard
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.example.ui.theme.urduTextStyle

private data class ScreeningQuestion(
    val key: String,
    val promptEn: String,
    val promptUr: String,
    val promptRoman: String,
    val maternalOnly: Boolean = false,
)

private val QUESTION_BANK = listOf(
    ScreeningQuestion(
        key = "crushingChestPain",
        promptEn = "Is there crushing chest pain, pressure, tightness, or pain spreading to arm/jaw?",
        promptUr = "کیا سینے میں شدید دباؤ، جکڑن یا بازو اور جبڑے تک پھیلنے والا درد ہے؟",
        promptRoman = "Kya seenay mein tez dard, jakran ya baazu/jabray tak dard ja raha hai?",
    ),
    ScreeningQuestion(
        key = "strokeFastSigns",
        promptEn = "Any sudden face drooping, weakness on one side, or slurred speech?",
        promptUr = "کیا اچانک چہرہ ٹیڑھا ہوا، ایک طرف کمزوری یا زبان لڑکھڑا رہی ہے؟",
        promptRoman = "Kya achanak chehra terha, ek taraf kamzori ya zuban larkhara rahi hai?",
    ),
    ScreeningQuestion(
        key = "uncontrolledBleeding",
        promptEn = "Is there active heavy bleeding that will not stop after gentle firm pressure?",
        promptUr = "کیا جسم کے کسی حصے سے شدید خون بہہ رہا ہے جو دبانے سے بھی نہیں رک رہا؟",
        promptRoman = "Kya tez khoon beh raha hai jo dabanay se bhi nahi ruk raha?",
    ),
    ScreeningQuestion(
        key = "convulsions",
        promptEn = "Has the patient had convulsions, fits, or involuntary body shaking?",
        promptUr = "کیا مریض کو جھٹکے یا مرگی کا دورہ پڑا ہے؟",
        promptRoman = "Kya mareez ko jhatkay ya daura para hai?",
    ),
    ScreeningQuestion(
        key = "lethargicOrUnconscious",
        promptEn = "Is the patient unusually drowsy, unresponsive, or difficult to wake up?",
        promptUr = "کیا مریض غیر معمولی غنودگی میں ہے یا جگانے پر بھی ہوش میں نہیں آ رہا؟",
        promptRoman = "Kya mareez behosh hai ya jaganay par bhi nahi uth raha?",
    ),
    ScreeningQuestion(
        key = "unableToDrink",
        promptEn = "Is the patient completely unable to drink water or breastfeed?",
        promptUr = "کیا مریض پانی پینے یا بچہ دودھ پینے سے بالکل قاصر ہے؟",
        promptRoman = "Kya mareez paani ya doodh bilkul nahi pee pa raha?",
    ),
    ScreeningQuestion(
        key = "vomitsEverything",
        promptEn = "Does the patient vomit up everything swallowed?",
        promptUr = "کیا مریض جو کچھ کھاتا یا پیتا ہے فوراً قے کر دیتا ہے؟",
        promptRoman = "Kya mareez jo khata ya peeta hai foran qai kar deta hai?",
    ),
    ScreeningQuestion(
        key = "chestIndrawingOrStridor",
        promptEn = "Is there chest indrawing (ribs pulling in), noisy harsh breathing, or severe shortness of breath?",
        promptUr = "کیا سانس لیتے وقت پسلیاں اندر دھنس رہی ہیں یا سانس لینے میں شدید دشواری ہے؟",
        promptRoman = "Kya saans letay waqt pasliyan andar ja rahi hain ya saans phool raha hai?",
    ),
    ScreeningQuestion(
        key = "stiffNeckOrPetechialRash",
        promptEn = "Is there a stiff neck with high fever or dark purple spots that do not fade when pressed?",
        promptUr = "کیا تیز بخار کے ساتھ گردن میں اکڑاؤ یا جلد پر گہرے جامنی دھبے ہیں؟",
        promptRoman = "Kya tez bukhar ke sath gardan akad gayi hai ya jild par neelay dhabbay hain?",
    ),
    ScreeningQuestion(
        key = "pregnancyDangerSigns",
        promptEn = "Any pregnancy/postpartum warning: vaginal bleeding, severe headache, or blurred vision?",
        promptUr = "حمل یا زچگی کے دوران کوئی خطرناک علامت: خون آنا، شدید سر درد یا نظر دھندلانا؟",
        promptRoman = "Hamal ke dauran khoon aana, tez sar dard ya nazar dhundlana?",
        maternalOnly = true,
    ),
    ScreeningQuestion(
        key = "bloodInStoolOrVomit",
        promptEn = "Has there been blood in stool or vomit?",
        promptUr = "کیا قے یا پاخانے میں خون آیا ہے؟",
        promptRoman = "Kya qai ya pakhane mein khoon aaya hai?",
    ),
    ScreeningQuestion(
        key = "persistentCoughOver14Days",
        promptEn = "Has cough lasted longer than 14 days?",
        promptUr = "کیا کھانسی کو 14 دن سے زیادہ ہو چکے ہیں؟",
        promptRoman = "Kya khansi 14 din se zyada arsay se hai?",
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidedIntakeForm(
    patient: PatientProfile,
    locale: AppLocale,
    triageEngine: TriageEngine,
    audioManager: AudioFeedbackManager,
    userCoords: GeoCoords?,
    onCompleteEpisode: (EpisodeInput, TriageEvaluationResult) -> Unit,
) {
    val c = LocalTibbColors.current
    val s = TrilingualStrings.get(locale)
    val isUr = locale == AppLocale.UR

    var stage by remember { mutableIntStateOf(1) } // 1..4
    var durationHours by remember { mutableIntStateOf(18) }
    var tempBand by remember { mutableStateOf(TemperatureBand.MILD_FEVER) }
    var hydration by remember { mutableStateOf(HydrationStatus.DRINKING_WELL) }
    var complaint by remember { mutableStateOf(PrimaryComplaint.FEVER) }
    var bodyRegion by remember { mutableStateOf(BodyRegion.GENERAL_FEVER) }

    val questions = remember(patient.isPregnantOrLactating) {
        QUESTION_BANK.filter { !it.maternalOnly || patient.isPregnantOrLactating }
    }
    var qIndex by remember { mutableIntStateOf(0) }
    val answersMap = remember { mutableStateMapOf<String, Boolean>() }

    var rrText by remember { mutableStateOf("") }
    var hrText by remember { mutableStateOf("") }
    var bpSysText by remember { mutableStateOf("") }
    var bpDiaText by remember { mutableStateOf("") }
    var sugarText by remember { mutableStateOf("") }

    BackHandler(enabled = stage > 1) {
        stage--
    }

    fun buildAndSubmit(emergencyOverride: EmergencyCategory? = null) {
        val ans = TriageAnswers(
            unableToDrink = answersMap["unableToDrink"] == true,
            vomitsEverything = answersMap["vomitsEverything"] == true,
            convulsions = answersMap["convulsions"] == true,
            lethargicOrUnconscious = answersMap["lethargicOrUnconscious"] == true,
            chestIndrawingOrStridor = answersMap["chestIndrawingOrStridor"] == true,
            crushingChestPain = answersMap["crushingChestPain"] == true,
            strokeFastSigns = answersMap["strokeFastSigns"] == true,
            severeShortnessOfBreath = answersMap["chestIndrawingOrStridor"] == true,
            uncontrolledBleeding = answersMap["uncontrolledBleeding"] == true,
            stiffNeckOrPetechialRash = answersMap["stiffNeckOrPetechialRash"] == true,
            pregnancyDangerSigns = answersMap["pregnancyDangerSigns"] == true,
            bloodInStoolOrVomit = answersMap["bloodInStoolOrVomit"] == true,
            persistentCoughOver14Days = answersMap["persistentCoughOver14Days"] == true || durationHours > 336,
            burnOrDeepWound = complaint == PrimaryComplaint.MINOR_BURN || complaint == PrimaryComplaint.MINOR_WOUND,
            heatExhaustionSigns = complaint == PrimaryComplaint.HEAT_EXHAUSTION,
            primaryComplaint = complaint,
        )
        val plc = patient.frequentPlaces.firstOrNull()
        val coords = userCoords ?: if (plc?.lat != null && plc.lng != null) GeoCoords(plc.lat, plc.lng) else null
        val vitals = EpisodeVitals(
            respiratoryRate = rrText.toIntOrNull(),
            heartRate = hrText.toIntOrNull(),
            bpSystolic = bpSysText.toIntOrNull(),
            bpDiastolic = bpDiaText.toIntOrNull(),
            bloodGlucoseMgDl = sugarText.toIntOrNull(),
            measurementMethod = if (rrText.isNotBlank() || hrText.isNotBlank() || bpSysText.isNotBlank()) MeasurementMethod.MANUAL else MeasurementMethod.NONE,
        )
        val input = EpisodeInput(
            patient = patient,
            bodyRegion = bodyRegion,
            durationHours = durationHours,
            temperatureBand = tempBand,
            hydrationStatus = hydration,
            vitals = vitals,
            answers = ans,
            district = plc?.district,
            userCoords = coords,
            emergencyOverrideCategory = emergencyOverride,
        )
        val res = triageEngine.evaluateCareEpisode(input)
        onCompleteEpisode(input, res)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LinearProgressIndicator(
            progress = { stage / 4f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
            color = c.accent,
            trackColor = c.surfaceMuted,
        )

        when (stage) {
            1 -> {
                // Direct RED Emergency Quick Chips
                TibbGlassCard(padding = 14.dp, borderColor = c.dangerRed.copy(alpha = 0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Emergency, contentDescription = null, tint = c.dangerRed, modifier = Modifier.size(18.dp))
                            Text("Immediate RED Emergency Bypass", style = MaterialTheme.typography.titleMedium, color = c.dangerRed)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                EmergencyCategory.CHEST_PAIN to "Chest Pain",
                                EmergencyCategory.STROKE_FAST to "Stroke / Paralysis",
                                EmergencyCategory.CHOKING to "Choking",
                                EmergencyCategory.UNCONTROLLED_BLEEDING to "Heavy Bleeding",
                                EmergencyCategory.SEIZURE_CONVULSIONS to "Seizure / Fits",
                                EmergencyCategory.POISONING_OVERDOSE to "Poisoning",
                            ).forEach { (cat, lbl) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(c.dangerSoft)
                                        .border(1.dp, c.dangerRed, RoundedCornerShape(999.dp))
                                        .clickable { buildAndSubmit(cat) }
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                        .testTag("guided_red_bypass_${cat.name.lowercase()}"),
                                ) {
                                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = c.dangerRed)
                                }
                            }
                        }
                    }
                }

                TibbGlassCard(padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. Primary Complaint", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryComplaint.entries.forEach { pc ->
                                val sel = complaint == pc
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { complaint = pc }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(pc.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = if (sel) c.ctaInk else c.ink)
                                }
                            }
                        }

                        Text("Duration", style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(12 to "< 24h", 36 to "1–2d", 96 to "3–7d", 360 to "> 14d").forEach { (hrs, lbl) ->
                                val sel = durationHours == hrs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { durationHours = hrs }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Text("Fever / Temperature Band", style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                TemperatureBand.NORMAL to "Normal",
                                TemperatureBand.MILD_FEVER to "Mild Fever",
                                TemperatureBand.HIGH_FEVER to "High Fever",
                            ).forEach { (tb, lbl) ->
                                val sel = tempBand == tb
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { tempBand = tb }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Text("Drinking / Hydration", style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                HydrationStatus.DRINKING_WELL to "Drinking Well",
                                HydrationStatus.REDUCED_FLUIDS to "Reduced",
                                HydrationStatus.UNABLE_TO_DRINK to "Unable",
                            ).forEach { (hs, lbl) ->
                                val sel = hydration == hs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { hydration = hs }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(lbl, color = if (sel) c.ctaInk else c.ink, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        TibbPrimaryPillButton(
                            onClick = { stage = 2 },
                            modifier = Modifier.fillMaxWidth().testTag("guided_stage1_next"),
                        ) {
                            Text("Continue to Body Map")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            2 -> {
                TibbGlassCard(padding = 16.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("2. Tap Body Area Where Symptoms Are Strongest", style = MaterialTheme.typography.titleMedium, color = c.ink)

                        // Interactive Canvas Anatomical Body Silhouette
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(c.surfaceMuted)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val normY = offset.y / size.height.toFloat()
                                        val normX = offset.x / size.width.toFloat()
                                        bodyRegion = when {
                                            normY < 0.28f -> BodyRegion.HEAD_NECK
                                            normY < 0.50f && normX in 0.30f..0.70f -> BodyRegion.CHEST
                                            normY < 0.72f && normX in 0.30f..0.70f -> BodyRegion.ABDOMEN
                                            else -> BodyRegion.SKIN_LIMBS
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                                val cx = size.width / 2f
                                val h = size.height
                                fun fillFor(reg: BodyRegion): Color =
                                    if (bodyRegion == reg) c.accent else c.muted.copy(alpha = 0.38f)

                                // Head & Neck
                                drawCircle(
                                    color = fillFor(BodyRegion.HEAD_NECK),
                                    radius = h * 0.11f,
                                    center = Offset(cx, h * 0.14f),
                                )
                                // Chest
                                drawRoundRect(
                                    color = fillFor(BodyRegion.CHEST),
                                    topLeft = Offset(cx - h * 0.16f, h * 0.28f),
                                    size = Size(h * 0.32f, h * 0.21f),
                                    cornerRadius = CornerRadius(14f, 14f),
                                )
                                // Abdomen
                                drawRoundRect(
                                    color = fillFor(BodyRegion.ABDOMEN),
                                    topLeft = Offset(cx - h * 0.15f, h * 0.51f),
                                    size = Size(h * 0.30f, h * 0.18f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                )
                                // Arms & Legs (Skin/Limbs)
                                drawRoundRect(
                                    color = fillFor(BodyRegion.SKIN_LIMBS),
                                    topLeft = Offset(cx - h * 0.27f, h * 0.29f),
                                    size = Size(h * 0.08f, h * 0.34f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                )
                                drawRoundRect(
                                    color = fillFor(BodyRegion.SKIN_LIMBS),
                                    topLeft = Offset(cx + h * 0.19f, h * 0.29f),
                                    size = Size(h * 0.08f, h * 0.34f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                )
                                drawRoundRect(
                                    color = fillFor(BodyRegion.SKIN_LIMBS),
                                    topLeft = Offset(cx - h * 0.14f, h * 0.71f),
                                    size = Size(h * 0.11f, h * 0.26f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                )
                                drawRoundRect(
                                    color = fillFor(BodyRegion.SKIN_LIMBS),
                                    topLeft = Offset(cx + h * 0.03f, h * 0.71f),
                                    size = Size(h * 0.11f, h * 0.26f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                )
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BodyRegion.entries.forEach { reg ->
                                val sel = bodyRegion == reg
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (sel) c.ctaBg else c.surfaceMuted)
                                        .clickable { bodyRegion = reg }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("body_region_${reg.name.lowercase()}"),
                                ) {
                                    Text(reg.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = if (sel) c.ctaInk else c.ink)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            TibbSecondaryPillButton(onClick = { stage = 1 }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                            }
                            TibbPrimaryPillButton(
                                onClick = {
                                    qIndex = 0
                                    stage = 3
                                },
                                modifier = Modifier.weight(2f).testTag("guided_stage2_next"),
                            ) {
                                Text("Start Screening")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }

            3 -> {
                val q = questions[qIndex.coerceIn(0, questions.lastIndex)]
                val prompt = when (locale) {
                    AppLocale.UR -> q.promptUr
                    AppLocale.ROMAN -> q.promptRoman
                    AppLocale.EN -> q.promptEn
                }
                TibbGlassCard(padding = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${s.oneQuestionLabel} · ${qIndex + 1}/${questions.size}", style = TabularNumberStyle, color = c.accent)
                            TibbSecondaryPillButton(
                                onClick = { audioManager.speakOfflineGuidance(q.promptUr, "ur-PK") },
                                modifier = Modifier.testTag("guided_q_audio_btn"),
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.urduAudio, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Text(
                            text = prompt,
                            style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                            color = c.ink,
                        )

                        fun answerQuestion(valBool: Boolean?) {
                            if (valBool != null) answersMap[q.key] = valBool
                            if (valBool == true && qIndex <= 8) {
                                buildAndSubmit()
                                return
                            }
                            if (qIndex < questions.lastIndex) qIndex++ else stage = 4
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TibbPrimaryPillButton(
                                onClick = { answerQuestion(true) },
                                containerColor = c.dangerRed,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f).height(50.dp).testTag("guided_answer_yes"),
                            ) { Text(s.yes) }

                            TibbPrimaryPillButton(
                                onClick = { answerQuestion(false) },
                                modifier = Modifier.weight(1f).height(50.dp).testTag("guided_answer_no"),
                            ) { Text(s.no) }

                            TibbSecondaryPillButton(
                                onClick = { answerQuestion(null) },
                                modifier = Modifier.weight(1f).height(50.dp).testTag("guided_answer_unsure"),
                            ) { Text(s.unsure) }
                        }
                    }
                }
            }

            4 -> {
                TibbGlassCard(padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("4. Optional Vitals Before Final Triage", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rrText,
                                onValueChange = { rrText = it.filter(Char::isDigit) },
                                label = { Text("Breaths/min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = hrText,
                                onValueChange = { hrText = it.filter(Char::isDigit) },
                                label = { Text("Pulse BPM") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bpSysText,
                                onValueChange = { bpSysText = it.filter(Char::isDigit) },
                                label = { Text("Upper BP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = bpDiaText,
                                onValueChange = { bpDiaText = it.filter(Char::isDigit) },
                                label = { Text("Lower BP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = sugarText,
                                onValueChange = { sugarText = it.filter(Char::isDigit) },
                                label = { Text("Sugar") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            TibbSecondaryPillButton(onClick = { stage = 3 }, modifier = Modifier.weight(1f)) {
                                Text("Back")
                            }
                            TibbPrimaryPillButton(
                                onClick = { buildAndSubmit() },
                                modifier = Modifier.weight(2f).height(50.dp).testTag("guided_evaluate_btn"),
                            ) {
                                Text("Evaluate & Show Care Plan")
                            }
                        }
                    }
                }
            }
        }
    }
}
