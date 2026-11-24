package com.example.engine

import com.example.data.local.BundledDataLoader
import com.example.data.model.DiseaseReferenceCard
import com.example.data.model.EmergencyCategory
import com.example.data.model.EmergencyFirstAidProtocol
import com.example.data.model.EpisodeInput
import com.example.data.model.FacilityRecord
import com.example.data.model.GeoCoords
import com.example.data.model.GroundingAuditResult
import com.example.data.model.HomeCareProtocol
import com.example.data.model.PrimaryComplaint
import com.example.data.model.RedFlagReason
import com.example.data.model.ReferralSlip
import com.example.data.model.StayHomeGuidance
import com.example.data.model.StayHomeVerdict
import com.example.data.model.SugarMode
import com.example.data.model.TemperatureBand
import com.example.data.model.HydrationStatus
import com.example.data.model.TriageEvaluationResult
import com.example.data.model.TriageTier
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

class TriageEngine(private val data: BundledDataLoader) {

    data class FacilityAreaAnchor(
        val id: String,
        val label: String,
        val district: String,
        val tehsil: String,
        val lat: Double,
        val lng: Double,
    )

    data class EmergencyDetection(
        val isEmergency: Boolean,
        val category: EmergencyCategory? = null,
        val reasonEn: String? = null,
        val reasonUr: String? = null,
    )

    data class BpInterpretation(
        val status: String, // NONE, LOW, NORMAL, ELEVATED, HIGH_1, HIGH_2, CRISIS
        val labelEn: String,
        val labelUr: String,
        val guidanceEn: String,
        val guidanceUr: String,
    )

    data class SugarInterpretation(
        val status: String, // NONE, LOW, NORMAL, BORDERLINE, HIGH, CRITICAL
        val labelEn: String,
        val labelUr: String,
        val guidanceEn: String,
        val guidanceUr: String,
    )

    private val bidiRegex = Regex("[\\u202A-\\u202E\\u2066-\\u2069\\u200E\\u200F\\u061C]")
    private val invisibleRegex = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val secureRandom = SecureRandom()

    fun sanitizePlainText(raw: String): String {
        return raw.replace(bidiRegex, "")
            .replace(invisibleRegex, "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }

    fun getFastBreathingThreshold(ageMonths: Int): Int = when {
        ageMonths < 2 -> 60
        ageMonths < 12 -> 50
        ageMonths <= 60 -> 40
        else -> 30
    }

    fun getAgeBracket(ageMonths: Int): String = when {
        ageMonths < 2 -> "0-2m"
        ageMonths < 12 -> "2-11m"
        ageMonths <= 60 -> "1-5y"
        ageMonths <= 216 -> "6-18y"
        ageMonths <= 720 -> "19-60y"
        else -> "60y+"
    }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return round(r * c * 10.0) / 10.0
    }

    fun getEmergencyFirstAid(category: EmergencyCategory): EmergencyFirstAidProtocol {
        return data.emergencyFirstAid.firstOrNull { it.id == category }
            ?: data.emergencyFirstAid.first()
    }

    fun detectEmergencyFromText(rawText: String): EmergencyDetection {
        val t = sanitizePlainText(rawText).lowercase()
        val opts = setOf(RegexOption.IGNORE_CASE)
        return when {
            Regex("(chest pain|crushing chest|heart attack|seenay mein dard|seene mein dard|dil ka dard|سینے میں.*درد|دل کا دورہ)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.CHEST_PAIN, "Suspected acute cardiac chest pain", "سینے میں شدید درد / ممکنہ دل کی ہنگامی حالت")
            Regex("(face droop|slurred speech|stroke|paralysis|falij|laqwa|زبان لڑکھڑا|فالج|لقوہ|چہرہ ٹیڑھا)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.STROKE_FAST, "Suspected acute stroke (FAST warning signs)", "ممکنہ فالج کی ہنگامی علامات")
            Regex("(heavy bleeding|uncontrolled bleeding|won't stop bleeding|tez khoon|khoon nahi ruk|شدید خون|خون بہنا)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.UNCONTROLLED_BLEEDING, "Severe uncontrolled bleeding", "شدید اور نہ رکنے والا خون بہنا")
            Regex("(seizure|convulsion|fitting|fits|daura|mirgi|jhatkay|دورے|جھٹکے|مرگی)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.SEIZURE_CONVULSIONS, "Active seizure or convulsions", "جھٹکے یا مرگی کا دورہ")
            Regex("(choking|can't swallow anything|gala band|saans phans|گلا بند|سانس پھنس)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.CHOKING, "Airway choking emergency", "سانس کی نالی میں رکاوٹ (دم گھٹنا)")
            Regex("(acid burn|electric shock|severe burn|deep burn|large burn|tezab|current lag|شدید جلنا|تیزاب|کرنٹ)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.SEVERE_BURN, "Severe electrical, chemical, or deep burn", "شدید جلنا، بجلی کا جھٹکا یا تیزاب")
            Regex("(poison|overdose|pesticide|bleach|kerosene|swallowed medicine|zehr|گولیاں کھا|زہر|تیل پی)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.POISONING_OVERDOSE, "Suspected poisoning or chemical/drug overdose", "ممکنہ زہر خورانی یا دوا کی زیادتی")
            Regex("(heatstroke|heat stroke|hot dry skin|loo lag|لُو لگ|گرمی سے بے ہوش)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.HEATSTROKE, "Suspected heatstroke emergency", "شدید لو لگنے (ہیٹ اسٹروک) کی ہنگامی حالت")
            Regex("(unconscious|not waking|fainted|collapsed|behosh|بے ہوش|ہوش نہیں)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.UNCONSCIOUS_FAINTING, "Unconscious or unresponsive patient", "مریض بے ہوش یا غیر جوابدہ حالت میں")
            Regex("(can't breathe|cannot breathe|struggling to breathe|gasping|blue lips|saans nahi|saans phool|سانس نہیں آ|سانس بند|تیز سانس)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.BREATHING_DISTRESS, "Severe breathing difficulty / respiratory distress", "سانس لینے میں شدید دشواری")
            Regex("(pregnant.*bleed|pregnancy.*bleed|labor pain|حمل.*خون)", opts).containsMatchIn(t) ->
                EmergencyDetection(true, EmergencyCategory.MATERNAL_EMERGENCY, "Pregnancy or postpartum emergency warning", "دورانِ حمل یا زچگی ہنگامی علامت")
            else -> EmergencyDetection(false)
        }
    }

    fun interpretBloodPressure(systolic: Int?, diastolic: Int?): BpInterpretation {
        if (systolic == null || diastolic == null) {
            return BpInterpretation(
                "NONE",
                "Enter both Upper & Lower BP numbers",
                "اوپر اور نیچے دونوں بلڈ پریشر نمبر درج کریں",
                "Example: Upper 120, Lower 80 mmHg.",
                "مثال: اوپر 120 اور نیچے 80 درج کریں۔",
            )
        }
        return when {
            systolic >= 180 || diastolic >= 120 -> BpInterpretation(
                "CRISIS",
                "Hypertensive Crisis (Emergency)",
                "انتہائی خطرناک ہائی بلڈ پریشر (فوری ایمرجنسی)",
                "Seek emergency hospital care immediately, especially with headache, chest pain, or blurred vision.",
                "فوری ایمرجنسی ہسپتال جائیں، خاص طور پر اگر سر درد، سینے میں درد یا نظر دھندلی ہو۔",
            )
            systolic < 90 || diastolic < 60 -> BpInterpretation(
                "LOW",
                "Low Blood Pressure (Hypotension)",
                "کم بلڈ پریشر (لو بی پی)",
                "Lie down with legs slightly raised; seek urgent care if dizzy, fainting, or cold/clammy.",
                "لیٹ جائیں اور ٹانگیں ہلکی اونچی رکھیں؛ چکر یا بے ہوشی میں فوری ہسپتال جائیں۔",
            )
            systolic >= 140 || diastolic >= 90 -> BpInterpretation(
                "HIGH_2",
                "High Blood Pressure (Stage 2)",
                "ہائی بلڈ پریشر (دوسرا درجہ)",
                "Visit a doctor today for proper medical evaluation. Do not adjust prescriptions on your own.",
                "آج ہی ڈاکٹر سے معائنہ کروائیں۔ خود سے دوا تبدیل نہ کریں۔",
            )
            systolic >= 130 || diastolic >= 80 -> BpInterpretation(
                "HIGH_1",
                "High Blood Pressure (Stage 1)",
                "ہائی بلڈ پریشر (پہلا درجہ)",
                "Recheck calmly after 5 minutes of rest and share log with your clinic doctor.",
                "5 منٹ آرام کے بعد دوبارہ چیک کریں اور اپنے ڈاکٹر کو ریکارڈ دکھائیں۔",
            )
            systolic >= 120 && diastolic < 80 -> BpInterpretation(
                "ELEVATED",
                "Slightly Elevated BP",
                "ہلکا سا بڑھا ہوا بلڈ پریشر",
                "Upper reading is mildly elevated while lower reading is normal. Monitor over the week.",
                "اوپر والا نمبر ہلکا سا زیادہ ہے جبکہ نیچے والا نارمل ہے۔ ہفتے بھر نگرانی کریں۔",
            )
            else -> BpInterpretation(
                "NORMAL",
                "Normal Blood Pressure",
                "نارمل بلڈ پریشر",
                "Both upper (systolic) and lower (diastolic) readings are within healthy resting range.",
                "اوپر اور نیچے دونوں نمبر صحت مند اور نارمل حد میں ہیں۔",
            )
        }
    }

    fun interpretBloodSugar(mgDl: Int?, mode: SugarMode = SugarMode.RANDOM): SugarInterpretation {
        if (mgDl == null) {
            return SugarInterpretation(
                "NONE",
                "Enter glucometer reading",
                "شوگر کی ریڈنگ درج کریں",
                "Select Fasting (8h no food) or Random.",
                "نہار منہ یا کھانے کے بعد کی قسم منتخب کریں۔",
            )
        }
        if (mgDl < 70) {
            return SugarInterpretation(
                "LOW",
                if (mgDl < 54) "Severe Low Sugar (Emergency)" else "Low Blood Sugar (Hypoglycemia)",
                "کم شوگر (فوری توجہ درکار)",
                if (mgDl < 54) "Emergency low sugar. If conscious, give quick sugar (glucose water/juice) immediately and seek urgent medical care."
                else "If alert, take 15g fast sugar (3 tsp sugar in water or half glass juice) and recheck in 15 minutes.",
                "شوگر کم ہے۔ اگر مریض ہوش میں ہو تو فوراً میٹھا پانی یا جوس دیں اور 15 منٹ بعد دوبارہ چیک کریں۔",
            )
        }
        if (mgDl >= 300) {
            return SugarInterpretation(
                "CRITICAL",
                "Very High Blood Sugar (Urgent)",
                "انتہائی زیادہ شوگر (فوری ڈاکٹر سے رجوع کریں)",
                "Visit an emergency clinic urgently, especially if vomiting, abdominal pain, or fast breathing occurs.",
                "فوری ہسپتال یا کلینک جائیں، خاص طور پر اگر قے، پیٹ درد یا تیز سانس ہو۔",
            )
        }
        return if (mode == SugarMode.FASTING) {
            when {
                mgDl <= 99 -> SugarInterpretation("NORMAL", "Normal Fasting Sugar", "نہار منہ شوگر نارمل ہے", "Within normal fasting range (70–99 mg/dL).", "نہار منہ شوگر نارمل حد (70 تا 99) میں ہے۔")
                mgDl <= 125 -> SugarInterpretation("BORDERLINE", "Borderline Fasting (Prediabetes Range)", "بارڈر لائن شوگر", "Between 100–125 mg/dL fasting. Discuss repeat lab test with your doctor.", "نہار منہ 100 تا 125 بارڈر لائن ہے۔ ڈاکٹر سے لیب ٹیسٹ کا مشورہ کریں۔")
                else -> SugarInterpretation("HIGH", "High Fasting Sugar", "نہار منہ شوگر زیادہ ہے", "Fasting reading >= 126 mg/dL. Schedule a clinic review.", "نہار منہ شوگر 126 یا اس سے زائد ہے، ڈاکٹر سے معائنہ کروائیں۔")
            }
        } else {
            when {
                mgDl <= 140 -> SugarInterpretation("NORMAL", "Normal Random Blood Sugar", "کھانے کے بعد شوگر نارمل ہے", "Within healthy post-meal range.", "شوگر نارمل حد میں ہے۔")
                mgDl < 200 -> SugarInterpretation("BORDERLINE", "Elevated Random Sugar", "شوگر ہلکی بڑھی ہوئی ہے", "Consider checking an empty-stomach (fasting) sugar reading tomorrow morning.", "کل صبح نہار منہ دوبارہ شوگر چیک کریں۔")
                else -> SugarInterpretation("HIGH", "High Blood Sugar", "شوگر زیادہ ہے", "Reading >= 200 mg/dL. Consult your physician for evaluation.", "شوگر 200 سے زیادہ ہے، معالج سے مشورہ کریں۔")
            }
        }
    }

    fun evaluateStayHomeVsHospital(
        bpSystolic: Int?,
        bpDiastolic: Int?,
        bloodGlucoseMgDl: Int?,
        hasOrganWarningSigns: Boolean = false,
        hasShockSigns: Boolean = false,
        isConsciousAbleToSwallow: Boolean = true,
    ): StayHomeGuidance {
        if (bloodGlucoseMgDl != null && bloodGlucoseMgDl < 70) {
            if (!isConsciousAbleToSwallow || bloodGlucoseMgDl < 54) {
                return StayHomeGuidance(
                    verdict = StayHomeVerdict.EMERGENCY_1122,
                    title_en = "Emergency Hypoglycemia — Call 1122 / Hospital Immediately",
                    title_ur = "خطرناک حد تک کم شوگر — فوری ایمرجنسی ہسپتال جائیں",
                    explanation_en = "Severe low sugar (<54 mg/dL) or altered consciousness cannot be treated safely by mouth.",
                    explanation_ur = "شدید کم شوگر یا غنودگی/بے ہوشی میں منہ کے ذریعے کچھ پلانا خطرناک ہو سکتا ہے۔",
                    actionSteps_en = listOf(
                        "Call Rescue 1122 immediately and place person in recovery position on their side if drowsy.",
                        "NEVER force liquids or food into the mouth of someone unconscious or unable to swallow safely.",
                    ),
                    actionSteps_ur = listOf(
                        "فوری 1122 پر کال کریں اور اگر مریض نیم بے ہوش ہو تو کروٹ پر لٹائیں۔",
                        "بے ہوش شخص کے منہ میں ہرگز پانی، شہد یا جوس نہ ڈالیں۔",
                    ),
                    disclaimer_en = "Hospital IV glucose or emergency glucagon may be needed immediately.",
                    disclaimer_ur = "فوری طبی امداد اور ڈرپ کی ضرورت ہو سکتی ہے۔",
                )
            }
            return StayHomeGuidance(
                verdict = StayHomeVerdict.STAY_HOME,
                title_en = "Treat Safely at Home Using the 15-15 Rule (Conscious Only)",
                title_ur = "گھر پر 15-15 اصول سے فوراً شوگر متوازن کریں (صرف ہوش میں)",
                explanation_en = "Sugar is 54–69 mg/dL and the person is alert and able to swallow.",
                explanation_ur = "شوگر 70 سے کم ہے لیکن مریض مکمل ہوش میں ہے اور پی سکتا ہے۔",
                actionSteps_en = listOf(
                    "Take 15g fast sugar now: 3 teaspoons sugar dissolved in water, OR half glass juice/regular soda, OR 1 tablespoon honey (age > 1y).",
                    "Wait 15 minutes and recheck blood sugar with your glucometer.",
                    "If still < 70 mg/dL repeat 15g sugar; once >= 70 mg/dL eat a small meal/roti to prevent another drop.",
                ),
                actionSteps_ur = listOf(
                    "فوراً 15 گرام تیز میٹھا لیں: 3 چمچ چینی پانی میں گھول کر یا آدھا گلاس جوس پئیں۔",
                    "15 منٹ انتظار کر کے دوبارہ شوگر چیک کریں۔",
                    "جب شوگر 70 سے اوپر آ جائے تو ہلکی روٹی یا بسکٹ کھائیں تاکہ دوبارہ کم نہ ہو۔",
                ),
                disclaimer_en = "If confusion develops or sugar fails to rise after 2 rounds, go to emergency immediately.",
                disclaimer_ur = "اگر غنودگی ہو یا دو بار میں بھی شوگر نہ بڑھے تو فوری ہسپتال جائیں۔",
            )
        }

        if ((bpSystolic != null && bpSystolic >= 180) || (bpDiastolic != null && bpDiastolic >= 120)) {
            if (hasOrganWarningSigns) {
                return StayHomeGuidance(
                    verdict = StayHomeVerdict.EMERGENCY_1122,
                    title_en = "Immediate Hospital / 1122 Required (Hypertensive Emergency)",
                    title_ur = "فوری ایمرجنسی ہسپتال جائیں (انتہائی ہائی بی پی مع خطرناک علامات)",
                    explanation_en = "BP >= 180/120 mmHg combined with chest pain, breathlessness, numbness, vision loss, or severe headache indicates end-organ risk.",
                    explanation_ur = "بی پی 180/120 سے زائد ہونے کے ساتھ سینے کا درد، کمزوری یا شدید سر درد فوری ہسپتال کا تقاضا کرتا ہے۔",
                    actionSteps_en = listOf(
                        "Call Rescue 1122 or reach the nearest Emergency Hospital right now.",
                        "Sit calmly upright; do NOT walk, drive yourself, or swallow unprescribed extra pills.",
                    ),
                    actionSteps_ur = listOf(
                        "فوری 1122 پر کال کریں یا قریبی ایمرجنسی ہسپتال پہنچیں۔",
                        "پرسکون بیٹھیں؛ خود گاڑی نہ چلائیں اور بغیر مشورے اضافی گولی نہ کھائیں۔",
                    ),
                    disclaimer_en = "Sudden organ warning signs cannot be stabilized safely at home.",
                    disclaimer_ur = "خطرناک علامات میں گھر پر انتظار کرنا نقصان دہ ہو سکتا ہے۔",
                )
            }
            return StayHomeGuidance(
                verdict = StayHomeVerdict.RECHECK_AND_CLINIC,
                title_en = "Rest 5 Min, Recheck Quietly, & Contact Clinic Today",
                title_ur = "5 منٹ آرام کر کے دوبارہ چیک کریں اور آج ہی ڈاکٹر کو دکھائیں",
                explanation_en = "BP is >= 180/120 mmHg without chest pain or stroke signs. Sit calmly for 5 minutes and recheck.",
                explanation_ur = "بی پی 180/120 سے زیادہ ہے مگر ابھی سینے کا درد یا فالج جیسی علامت نہیں ہے۔ پرسکون ہو کر دوبارہ چیک کریں۔",
                actionSteps_en = listOf(
                    "Sit quietly for 5 minutes (use the Cyclic Sighing breathing pacer if anxious) with cuff at heart level and recheck.",
                    "If still >= 180/120 mmHg, go promptly to a clinic/hospital today even without pain.",
                    "If chest pain, vision blur, numbness, or shortness of breath appears, call 1122 immediately.",
                ),
                actionSteps_ur = listOf(
                    "5 منٹ پرسکون بیٹھیں (گھبراہٹ ہو تو سانس کی مشق کریں) اور دوبارہ بی پی چیک کریں۔",
                    "اگر دوبارہ بھی 180/120 یا زیادہ ہو تو آج ہی فوراً ڈاکٹر یا ہسپتال جائیں۔",
                    "اگر سینے میں درد، کمزوری یا سانس کی تکلیف ہو تو فوراً 1122 پر کال کریں۔",
                ),
                disclaimer_en = "Do not self-medicate extra blood pressure pills without physician direction.",
                disclaimer_ur = "ڈاکٹر کے مشورے کے بغیر خود سے اضافی دوا نہ کھائیں۔",
            )
        }

        if ((bpSystolic != null && bpSystolic < 90) || (bpDiastolic != null && bpDiastolic < 60)) {
            if (hasShockSigns) {
                return StayHomeGuidance(
                    verdict = StayHomeVerdict.EMERGENCY_1122,
                    title_en = "Immediate Emergency Care (Low BP with Shock Signs)",
                    title_ur = "فوری ایمرجنسی ہسپتال جائیں (کم بلڈ پریشر اور شدید کمزوری)",
                    explanation_en = "Blood pressure is below 90/60 mmHg accompanied by shock warning signs (confusion, cold/clammy skin, rapid shallow breathing, fainting).",
                    explanation_ur = "بلڈ پریشر 90/60 سے کم ہے اور ساتھ بے ہوشی، ٹھنڈا پسینہ یا تیز سانس کی علامات ہیں۔",
                    actionSteps_en = listOf(
                        "Call 1122 / 115 immediately and lay the person flat with legs gently elevated.",
                        "Keep warm and monitor consciousness until medical help arrives.",
                    ),
                    actionSteps_ur = listOf(
                        "فوری 1122 پر کال کریں اور مریض کو سیدھا لٹا کر ٹانگیں ہلکی اونچی رکھیں۔",
                        "جسم کو مناسب گرم رکھیں اور ہوش پر نظر رکھیں۔",
                    ),
                    disclaimer_en = "Emergency signs require urgent hospital stabilization.",
                    disclaimer_ur = "خطرناک علامات میں فوری ہسپتال جانا ضروری ہے۔",
                )
            }
            return StayHomeGuidance(
                verdict = StayHomeVerdict.STAY_HOME,
                title_en = "Rest & Hydrate Safely at Home; Seek Clinic if Dizziness Persists",
                title_ur = "گھر پر آرام اور پانی لیں؛ چکر برقرار رہیں تو ڈاکٹر کو دکھائیں",
                explanation_en = "Blood pressure is < 90/60 mmHg without severe shock signs. Mild dizziness can often settle with safe posture and fluids.",
                explanation_ur = "بلڈ پریشر 90/60 سے کم ہے مگر خطرناک علامات نہیں ہیں۔ احتیاطی اقدامات اور پانی سے بہتری آ سکتی ہے۔",
                actionSteps_en = listOf(
                    "Drink water or ORS gradually; sit or lie down and stand up slowly in stages.",
                    "Tense leg and thigh muscles gently before standing to support circulation.",
                    "Check with your doctor before adding extra dietary salt.",
                ),
                actionSteps_ur = listOf(
                    "آہستہ آہستہ صاف پانی یا او آر ایس پئیں اور بیٹھنے یا اٹھنے میں جلدی نہ کریں۔",
                    "کھڑے ہونے سے پہلے ٹانگوں اور رانوں کے پٹھوں کو ہلکا سا دبائیں۔",
                    "خوراک میں اضافی نمک بڑھانے سے پہلے اپنے ڈاکٹر سے مشورہ ضرور کریں۔",
                ),
                disclaimer_en = "Informational supportive guidance only — seek urgent care if fainting or chest pain occurs.",
                disclaimer_ur = "صرف معلوماتی رہنمائی؛ بے ہوشی یا سینے کے درد میں فوری ہسپتال جائیں۔",
            )
        }

        return StayHomeGuidance(
            verdict = StayHomeVerdict.STAY_HOME,
            title_en = "Safe to Stay Home & Monitor Calmly",
            title_ur = "گھر پر پُرسکون نگرانی محفوظ ہے",
            explanation_en = "Your entered vitals do not cross emergency hospital thresholds.",
            explanation_ur = "درج کی گئی ریڈنگز میں کوئی فوری ایمرجنسی خطرہ ظاہر نہیں ہوا۔",
            actionSteps_en = listOf(
                "Maintain clean hydration, calm rest, and log follow-up readings in this person’s profile.",
                "Seek clinic review if new symptoms, high fever > 48h, or discomfort develops.",
            ),
            actionSteps_ur = listOf(
                "صاف پانی پیتے رہیں، آرام کریں اور اس فرد کے ریکارڈ میں اگلی ریڈنگ محفوظ کریں۔",
                "نئی علامات یا 48 گھنٹے سے زائد بخار پر کلینک سے معائنہ کروائیں۔",
            ),
            disclaimer_en = "Supportive triage guidance only — not a clinical diagnosis.",
            disclaimer_ur = "یہ صرف معلوماتی طبی رہنمائی ہے، باقاعدہ تشخیص نہیں۔",
        )
    }

    fun getAvailableDistricts(): List<String> =
        data.facilities.map { it.district }.distinct().sorted()

    fun getFacilityAreaAnchors(): List<FacilityAreaAnchor> {
        val seen = HashSet<String>()
        val anchors = mutableListOf<FacilityAreaAnchor>()
        for (fac in data.facilities) {
            val key = "${fac.district}:${fac.tehsil}"
            if (seen.add(key)) {
                anchors.add(
                    FacilityAreaAnchor(
                        id = fac.id,
                        label = "${fac.tehsil}, ${fac.district}",
                        district = fac.district,
                        tehsil = fac.tehsil,
                        lat = fac.lat,
                        lng = fac.lng,
                    )
                )
            }
        }
        return anchors.sortedWith(compareBy<FacilityAreaAnchor> { it.district }.thenBy { it.tehsil })
    }

    fun sortFacilities(
        tier: TriageTier,
        isPediatric: Boolean,
        userCoords: GeoCoords? = null,
        districtFilter: String? = null,
    ): Pair<List<FacilityRecord>, Boolean> {
        val cleanQuery = districtFilter?.trim()?.lowercase().orEmpty()
        if (userCoords == null && cleanQuery.isEmpty()) {
            return emptyList<FacilityRecord>() to true
        }
        val all = data.facilities
        val exact = if (cleanQuery.isNotEmpty()) all.filter { it.district.equals(cleanQuery, ignoreCase = true) } else emptyList()
        val partial = if (cleanQuery.isNotEmpty() && exact.isEmpty()) {
            all.filter {
                it.district.contains(cleanQuery, ignoreCase = true) ||
                    it.tehsil.contains(cleanQuery, ignoreCase = true) ||
                    it.name_en.contains(cleanQuery, ignoreCase = true)
            }
        } else emptyList()
        val pool = when {
            exact.isNotEmpty() -> exact
            partial.isNotEmpty() -> partial
            else -> all
        }
        val mapped = pool.map { f ->
            f.copy(distanceKm = userCoords?.let { haversineKm(it.lat, it.lng, f.lat, f.lng) })
        }
        val sorted = mapped.sortedWith { a, b ->
            if (a.distanceKm != null && b.distanceKm != null) {
                if (tier == TriageTier.RED && abs(a.distanceKm - b.distanceKm) < 2.5) {
                    val aUrgent = (if (a.emergency24x7) 0 else 10) + (if (isPediatric && a.pediatricEmergency) -3 else 0)
                    val bUrgent = (if (b.emergency24x7) 0 else 10) + (if (isPediatric && b.pediatricEmergency) -3 else 0)
                    if (aUrgent != bUrgent) return@sortedWith aUrgent - bUrgent
                }
                return@sortedWith a.distanceKm.compareTo(b.distanceKm)
            }
            if (tier == TriageTier.RED) {
                val aUrgent = (if (a.emergency24x7) 0 else 100) +
                    (if (isPediatric && a.pediatricEmergency) -20 else 0) +
                    (if (a.tier == "TERTIARY") -10 else 0)
                val bUrgent = (if (b.emergency24x7) 0 else 100) +
                    (if (isPediatric && b.pediatricEmergency) -20 else 0) +
                    (if (b.tier == "TERTIARY") -10 else 0)
                if (aUrgent != bUrgent) return@sortedWith aUrgent - bUrgent
            }
            a.name_en.compareTo(b.name_en)
        }.take(6)
        return sorted to false
    }

    fun evaluateCareEpisode(input: EpisodeInput): TriageEvaluationResult {
        val redFlags = mutableListOf<RedFlagReason>()
        val patient = input.patient
        val vitals = input.vitals
        val answers = input.answers
        val isPediatric = patient.ageMonths <= 60
        val threshold = getFastBreathingThreshold(patient.ageMonths)
        val isFastBreathing = vitals.respiratoryRate != null && vitals.respiratoryRate >= threshold

        var primaryCategory = input.emergencyOverrideCategory
            ?: if (isPediatric) EmergencyCategory.PEDIATRIC_DANGER else EmergencyCategory.BREATHING_DISTRESS

        if (answers.crushingChestPain) {
            primaryCategory = EmergencyCategory.CHEST_PAIN
            redFlags.add(RedFlagReason("ETAT_CHEST_PAIN", "Crushing chest pain, tightness, or jaw/arm radiation", "سینے میں شدید دباؤ یا جکڑن والا درد", "WHO_ETAT"))
        }
        if (answers.strokeFastSigns) {
            primaryCategory = EmergencyCategory.STROKE_FAST
            redFlags.add(RedFlagReason("ETAT_STROKE_FAST", "Sudden face droop, unilateral weakness, or slurred speech", "چہرے کا ٹیڑھا ہونا، ایک طرف کی کمزوری یا زبان لڑکھڑانا", "WHO_ETAT"))
        }
        if (answers.uncontrolledBleeding) {
            primaryCategory = EmergencyCategory.UNCONTROLLED_BLEEDING
            redFlags.add(RedFlagReason("ETAT_BLEEDING", "Uncontrolled active bleeding", "شدید اور نہ رکنے والا خون بہنا", "WHO_ETAT"))
        }
        if (answers.convulsions) {
            primaryCategory = EmergencyCategory.SEIZURE_CONVULSIONS
            redFlags.add(RedFlagReason("IMCI_CONVULSIONS", "Convulsions or fits during this illness", "دورے یا جھٹکے پڑنا", "WHO_IMCI"))
        }
        if (answers.lethargicOrUnconscious) {
            primaryCategory = EmergencyCategory.UNCONSCIOUS_FAINTING
            redFlags.add(RedFlagReason("IMCI_UNCONSCIOUS", "Unusually drowsy, lethargic, or unconscious", "غیر معمولی غنودگی یا بے ہوشی", "WHO_IMCI"))
        }
        if (answers.unableToDrink || input.hydrationStatus == HydrationStatus.UNABLE_TO_DRINK) {
            redFlags.add(RedFlagReason("IMCI_UNABLE_DRINK", "Unable to drink or breastfeed", "پانی یا دودھ پینے سے قاصر", "WHO_IMCI"))
        }
        if (answers.vomitsEverything) {
            redFlags.add(RedFlagReason("IMCI_VOMITS_ALL", "Vomits everything swallowed", "جو کھائے پیے فوراً قے کر دینا", "WHO_IMCI"))
        }
        if (answers.chestIndrawingOrStridor || answers.severeShortnessOfBreath || isFastBreathing) {
            if (!answers.crushingChestPain && !answers.strokeFastSigns) {
                primaryCategory = EmergencyCategory.BREATHING_DISTRESS
            }
            redFlags.add(RedFlagReason("WHO_BREATHING_DANGER", "Severe breathing difficulty, chest indrawing, or fast breathing (>= $threshold/min)", "سانس میں شدید دشواری یا تیز سانس", "WHO_IMCI"))
        }
        if (answers.stiffNeckOrPetechialRash) {
            redFlags.add(RedFlagReason("ETAT_MENINGITIS_DENGUE", "Stiff neck or non-blanching petechial rash", "گردن کا اکڑاؤ یا جلد پر نہ مٹنے والے دھبے", "WHO_ETAT"))
        }
        if (patient.isPregnantOrLactating && answers.pregnancyDangerSigns) {
            primaryCategory = EmergencyCategory.MATERNAL_EMERGENCY
            redFlags.add(RedFlagReason("MATERNAL_DANGER", "Pregnancy/postpartum danger signs (severe headache, vision changes, bleeding)", "حمل یا زچگی کے دوران خطرے کی علامات", "MATERNAL"))
        }
        if (vitals.bpSystolic != null && (vitals.bpSystolic >= 180 || vitals.bpSystolic < 80)) {
            redFlags.add(RedFlagReason("VITALS_CRITICAL_BP", "Critical blood pressure reading (${vitals.bpSystolic}/${vitals.bpDiastolic ?: "--"} mmHg)", "بلڈ پریشر کی خطرناک سطح (${vitals.bpSystolic})", "VITALS"))
        }
        if (vitals.bloodGlucoseMgDl != null && (vitals.bloodGlucoseMgDl < 54 || vitals.bloodGlucoseMgDl >= 400)) {
            redFlags.add(RedFlagReason("VITALS_CRITICAL_GLUCOSE", "Critical blood glucose reading (${vitals.bloodGlucoseMgDl} mg/dL)", "شوگر کی خطرناک سطح (${vitals.bloodGlucoseMgDl} mg/dL)", "VITALS"))
        }
        if (input.emergencyOverrideCategory != null && redFlags.isEmpty()) {
            val fa = getEmergencyFirstAid(input.emergencyOverrideCategory)
            redFlags.add(RedFlagReason("DIRECT_${input.emergencyOverrideCategory}", fa.title_en, fa.title_ur, "WHO_ETAT"))
        }

        val (homeCareId, refId, symptomCategory) = when (answers.primaryComplaint) {
            PrimaryComplaint.COUGH_COLD -> Triple("mild_cough_cold", "respiratory_imci", "RESPIRATORY")
            PrimaryComplaint.DIARRHEA -> Triple("mild_diarrhea_ors", "diarrhea_dehydration", "GI_HYDRATION")
            PrimaryComplaint.MINOR_WOUND -> Triple("minor_abrasion", "wound_burn_first_aid", "FIRST_AID")
            PrimaryComplaint.MINOR_BURN -> Triple("minor_burn_first_aid", "wound_burn_first_aid", "FIRST_AID")
            PrimaryComplaint.HEAT_EXHAUSTION -> Triple("heat_exhaustion_first_aid", "wound_burn_first_aid", "HEAT")
            PrimaryComplaint.CHRONIC_FLARE -> Triple("mild_fever", "fever_dengue_typhoid", "CHRONIC")
            PrimaryComplaint.FEVER -> Triple("mild_fever", "fever_dengue_typhoid", "FEVER")
        }

        val rawCare = data.whoHomeCare.firstOrNull { it.id == homeCareId } ?: data.whoHomeCare.first()
        val ageSafeCare = if (patient.ageMonths < 12) {
            rawCare.copy(
                steps_en = rawCare.steps_en.filterNot { it.contains("honey", ignoreCase = true) },
                steps_ur = rawCare.steps_ur.filterNot { it.contains("شہد") },
                steps_roman = rawCare.steps_roman.filterNot { it.contains("shehad", ignoreCase = true) },
            )
        } else rawCare
        val diseaseRef = data.diseaseReference.firstOrNull { it.id == refId } ?: data.diseaseReference.first()

        val amberReasons = mutableListOf<String>()
        val tier = if (redFlags.isNotEmpty()) {
            TriageTier.RED
        } else {
            if (input.durationHours > 48) amberReasons.add("Symptoms have persisted beyond 48 hours and need clinician review.")
            if (input.temperatureBand == TemperatureBand.HIGH_FEVER) amberReasons.add("High fever requires physical examination to rule out bacterial or vector-borne infection.")
            if (input.hydrationStatus == HydrationStatus.REDUCED_FLUIDS || answers.moderateDehydration) amberReasons.add("Reduced oral intake or signs of moderate dehydration.")
            if (answers.bloodInStoolOrVomit) amberReasons.add("Blood in stool or vomit requires same-day clinic assessment.")
            if (answers.burnOrDeepWound) amberReasons.add("Deep wound or significant burn needs sterile clinic dressing and tetanus review.")
            if (answers.persistentCoughOver14Days) amberReasons.add("Cough lasting over 14 days requires TB/respiratory screening at a BHU/THQ.")
            if (patient.ageMonths < 2 && input.temperatureBand != TemperatureBand.NORMAL) amberReasons.add("Any fever in an infant under 2 months requires urgent physician assessment.")
            if (patient.chronicConditions.isNotEmpty() && answers.primaryComplaint == PrimaryComplaint.CHRONIC_FLARE) {
                amberReasons.add("Chronic illness flare-up requires clinician adjustment—do not alter prescriptions at home.")
            }
            if (amberReasons.isNotEmpty()) TriageTier.AMBER else TriageTier.GREEN
        }

        val (facilities, locationRequired) = sortFacilities(tier, isPediatric, input.userCoords, input.district)
        val emergencyFirstAid = if (tier == TriageTier.RED) getEmergencyFirstAid(primaryCategory) else null
        val primaryFlag = redFlags.firstOrNull()?.code
            ?: if (amberReasons.isNotEmpty()) "AMBER_$symptomCategory" else "GREEN_${ageSafeCare.id.uppercase()}"

        val bytes = ByteArray(4).also { secureRandom.nextBytes(it) }
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        return TriageEvaluationResult(
            tier = tier,
            redFlags = redFlags,
            amberReasons = amberReasons,
            fastBreathingThreshold = threshold,
            isFastBreathing = isFastBreathing,
            matchedHomeCare = if (tier == TriageTier.RED) null else ageSafeCare,
            emergencyFirstAid = emergencyFirstAid,
            matchedDiseaseRef = diseaseRef,
            recommendedFacilities = facilities,
            locationRequired = locationRequired,
            referralSlip = ReferralSlip(
                referralToken = "TIBB-$tier-$hex",
                ageBracket = getAgeBracket(patient.ageMonths),
                tier = tier,
                primaryFlag = primaryFlag,
                facilityId = facilities.firstOrNull()?.id ?: "LOCATION_PENDING",
                createdAtIso = nowIso,
            ),
            symptomCategory = symptomCategory,
        )
    }

    fun verifyAgainstClinicalGroundTruth(
        rawText: String,
        ageMonths: Int,
        homeCare: HomeCareProtocol?,
        diseaseRef: DiseaseReferenceCard,
        triageTier: TriageTier,
    ): GroundingAuditResult {
        val sanitized = sanitizePlainText(rawText)
        val lower = sanitized.lowercase()
        val violations = mutableListOf<String>()
        var amrBlocked = false

        if (sanitized.length < 12) violations.add("EMPTY_OR_SHORT_OUTPUT")
        if (triageTier == TriageTier.RED) violations.add("RED_TIER_LLM_SUPPRESSED")

        val blockedTerms = data.drapBlocklist.antibiotics_generic +
            data.drapBlocklist.pakistan_brand_names +
            data.drapBlocklist.restricted_rx_classes

        for (term in blockedTerms) {
            val wordRegex = Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE)
            if (wordRegex.containsMatchIn(lower)) {
                violations.add("DRAP_RX_BLOCKED:$term")
                amrBlocked = true
                break
            }
        }
        for (phrase in data.drapBlocklist.diagnostic_phrases) {
            if (lower.contains(phrase.lowercase())) {
                violations.add("DIAGNOSTIC_OR_DOSAGE_CLAIM:$phrase")
                break
            }
        }
        if (Regex("\\b\\d+\\s*(mg|ml|mcg|iu|tablets?|capsules?|drops?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
            violations.add("UNGROUNDED_MEDICATION_DOSAGE")
            amrBlocked = true
        }
        if (ageMonths < 12 && Regex("\\b(honey|shehad|شہد)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
            violations.add("INFANT_HONEY_CONTRAINDICATION")
        }

        val allowedNums = buildSet {
            homeCare?.allowedNumericClaims?.let { addAll(it) }
            addAll(diseaseRef.allowedNumericClaims)
            addAll(listOf("1122", "115", "1020", "1166"))
        }
        Regex("\\b\\d+(?:\\.\\d+)?\\b").findAll(sanitized).forEach { match ->
            if (!allowedNums.contains(match.value)) {
                violations.add("UNGROUNDED_NUMBER:${match.value}")
                return@forEach
            }
        }

        return GroundingAuditResult(
            passed = violations.isEmpty(),
            sanitizedText = if (violations.isEmpty()) sanitized else "",
            violations = violations,
            amrBlocked = amrBlocked,
        )
    }
}
