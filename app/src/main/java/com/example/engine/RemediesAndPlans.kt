package com.example.engine

import com.example.data.model.AppLocale
import com.example.data.model.DietMealSlot
import com.example.data.model.EmergencyCategory
import com.example.data.model.EmergencyFirstAidProtocol
import com.example.data.model.HomeRemedyItem
import com.example.data.model.MealOptionItem
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.WellnessGoal
import com.example.data.model.WorkoutExerciseSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object RemediesAndPlans {

    val HOME_REMEDIES_PAKISTAN: List<HomeRemedyItem> = listOf(
        HomeRemedyItem(
            id = "rem_ors_hydration",
            category = "STOMACH_HYDRATION",
            title_en = "Homemade WHO ORS (Namak-Cheeni Solution)",
            title_ur = "گھر پر مستند او آر ایس (نمک چینی محلول)",
            ingredients_en = "1 liter clean boiled & cooled water, 6 level teaspoons sugar, 1/2 level teaspoon salt (or 1 WHO ORS packet).",
            ingredients_ur = "1 لیٹر ابلا اور ٹھنڈا صاف پانی، 6 چپٹے چمچ چینی، آدھا چپٹا چمچ نمک (یا 1 او آر ایس پیکٹ)۔",
            steps_en = listOf(
                "Wash hands thoroughly and dissolve sugar and salt completely in cooled boiled water.",
                "Sip slowly every 5–10 minutes after loose stools, fever sweating, or heat exposure.",
                "Discard unused homemade ORS after 24 hours and prepare a fresh batch.",
            ),
            steps_ur = listOf(
                "ہاتھ دھو کر ابلے اور ٹھنڈے پانی میں چینی اور نمک اچھی طرح حل کریں۔",
                "اسہال، پسینے یا گرمی کے بعد ہر 5 سے 10 منٹ میں تھوڑا تھوڑا پلائیں۔",
                "تیار شدہ او آر ایس 24 گھنٹے بعد ضائع کر دیں اور نیا بنائیں۔",
            ),
            caution_en = "Avoid extra salt if kidney failure or severe heart failure is present; seek clinic immediately if unable to drink, sunken eyes, or vomiting everything.",
            caution_ur = "گردوں یا دل کے شدید مرض میں احتیاط کریں؛ قے نہ رکے یا مریض پی نہ سکے تو فوری ہسپتال جائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_haldi_throat",
            category = "FEVER_THROAT",
            title_en = "Warm Turmeric Milk & Saltwater Gargle",
            title_ur = "گرم ہلدی دودھ اور نیم گرم نمکین غرارے",
            ingredients_en = "1/2 tsp turmeric in 1 cup warm milk; 1/2 tsp salt in 1 glass warm water for gargles.",
            ingredients_ur = "ایک کپ نیم گرم دودھ میں آدھا چمچ ہلدی؛ غراروں کے لیے نیم گرم پانی میں آدھا چمچ نمک۔",
            steps_en = listOf(
                "Gargle warm salt water for 30 seconds, 3 times daily, to reduce throat irritation and swelling.",
                "Drink warm turmeric milk before rest to soothe mild dryness and promote comfort.",
            ),
            steps_ur = listOf(
                "گلے کی سوزش اور خراش کم کرنے کے لیے دن میں 3 بار نیم گرم نمکین پانی سے غرارے کریں۔",
                "سونے سے پہلے گرم ہلدی دودھ پئیں تاکہ گلے کو سکون ملے۔",
            ),
            caution_en = "Seek clinic evaluation if high fever lasts > 48 hours, tonsils have white spots, or breathing feels fast/labored.",
            caution_ur = "اگر تیز بخار 48 گھنٹے سے زائد رہے یا سانس لینے میں دشواری ہو تو فوری معائنہ کروائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_honey_ginger_cough",
            category = "FEVER_THROAT",
            title_en = "Warm Ginger-Honey Soother for Mild Dry Cough (Age > 1 Year)",
            title_ur = "خشک کھانسی کے لیے ادرک اور شہد کا نیم گرم قہوہ (1 سال سے زائد)",
            ingredients_en = "1 tsp pure honey, 3 slices fresh crushed ginger, 1 cup lukewarm water or tulsi/mint tea.",
            ingredients_ur = "1 چمچ خالص شہد، تازہ ادرک کے 3 ٹکڑے، 1 کپ نیم گرم پانی یا پودینہ قہوہ۔",
            steps_en = listOf(
                "Simmer fresh ginger in water for 5 minutes, let it cool until lukewarm (not boiling).",
                "Stir in 1 teaspoon honey and sip slowly before sleep to calm nocturnal cough reflex.",
            ),
            steps_ur = listOf(
                "ادرک کو پانی میں 5 منٹ ہلکی آنچ پر پکائیں اور نیم گرم ہونے دیں۔",
                "اس میں 1 چائے کا چمچ شہد ملا کر سونے سے پہلے آہستہ آہستہ پئیں۔",
            ),
            caution_en = "STRICT SAFETY RULE: NEVER give honey to infants under 12 months (risk of infant botulism). Cough lasting > 14 days needs TB/clinic screening.",
            caution_ur = "سخت احتیاط: 1 سال سے کم عمر بچے کو شہد ہرگز نہ دیں۔ 14 دن سے زیادہ کھانسی پر ٹی بی/کلینک ٹیسٹ ضروری ہے۔",
        ),
        HomeRemedyItem(
            id = "rem_steam_sinus",
            category = "FEVER_THROAT",
            title_en = "Safe Steam Inhalation & Warm Saline Drops",
            title_ur = "بند ناک کھولنے کے لیے بھاپ اور نمکین قطرے",
            ingredients_en = "Bowl of hot water (not on stove), clean towel; or sterile nasal saline spray/drops.",
            ingredients_ur = "گرم پانی کا پیالہ (چولہے سے اترا ہوا)، صاف تولیہ یا ناک کے نمکین قطرے۔",
            steps_en = listOf(
                "Sit comfortably away from heat source, drape towel loosely, and breathe gentle warm steam for 5–8 minutes.",
                "For young children, use only lukewarm bathroom humidity and 1–2 sterile saline nose drops — never open boiling bowls.",
            ),
            steps_ur = listOf(
                "آرام سے بیٹھ کر محفوظ فاصلے سے 5 سے 8 منٹ ہلکی بھاپ لیں (بچوں کو ابلتے پانی کے قریب نہ لائیں)۔",
                "چھوٹے بچوں کے لیے صرف ناک میں نمکین قطرے اور باتھ روم کی ہلکی بھاپ استعمال کریں۔",
            ),
            caution_en = "Do not use scalding bowls near children (burn hazard). Seek 1122 immediately for chest indrawing, blue lips, or wheezing distress.",
            caution_ur = "ابلتی پتیلی بچوں کے قریب ہرگز نہ رکھیں۔ پسلیوں کے دھنسنے یا سانس کی تکلیف پر فوراً 1122 پر کال کریں۔",
        ),
        HomeRemedyItem(
            id = "rem_ginger_mint",
            category = "STOMACH_HYDRATION",
            title_en = "Ginger-Mint Kahwa for Nausea, Gas & Indigestion",
            title_ur = "متلی، گیس اور بدہضمی کے لیے ادرک پودینہ قہوہ",
            ingredients_en = "3 thin slices fresh ginger, 6 fresh mint leaves, 1/4 tsp crushed saunf (fennel), 1 cup hot water.",
            ingredients_ur = "تازہ ادرک کے 3 پتلے ٹکڑے، پودینے کے 6 پتے، چوتھائی چمچ سونف، 1 کپ گرم پانی۔",
            steps_en = listOf(
                "Steep ginger, mint, and fennel in covered hot water for 5–7 minutes, then strain.",
                "Sip lukewarm slowly rather than chugging fizzy or chilled drinks; keep meals bland (khichdi/banana).",
            ),
            steps_ur = listOf(
                "ادرک، پودینہ اور سونف گرم پانی میں 5 سے 7 منٹ ڈھانپ کر رکھیں پھر چھان لیں۔",
                "سوڈا یا ٹھنڈے مشروبات کے بجائے نیم گرم گھونٹ آہستہ پئیں اور ہلکی غذا (کھچڑی یا کیلا) لیں۔",
            ),
            caution_en = "Seek urgent hospital care for severe right-lower belly pain, blood in vomit/stool, black tarry stools, or persistent vomiting.",
            caution_ur = "پیٹ میں شدید درد، قے یا پاخانے میں خون آنے پر فوری ایمرجنسی ہسپتال جائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_isabgol_fiber",
            category = "STOMACH_HYDRATION",
            title_en = "Psyllium Husk (Isabgol) for Mild Constipation & Gut Comfort",
            title_ur = "قبض اور معدے کی تیزابیت کے لیے اسپغول کا محفوظ استعمال",
            ingredients_en = "1 level teaspoon plain Isabgol husk, 1 full glass lukewarm water or unsweetened thin yogurt/lassi.",
            ingredients_ur = "1 چمچ سادہ اسپغول کی بھوسی، 1 پورا گلاس نیم گرم پانی یا پتلا دہی۔",
            steps_en = listOf(
                "Stir 1 teaspoon Isabgol briskly into a full glass of water and drink immediately.",
                "Follow with another half glass of clean water; stay hydrated throughout the day.",
            ),
            steps_ur = listOf(
                "1 چمچ اسپغول ایک پورے گلاس پانی میں گھول کر فوراً پی لیں۔",
                "اس کے فوراً بعد آدھا گلاس مزید سادہ پانی پئیں تاکہ آنتوں میں خشکی نہ ہو۔",
            ),
            caution_en = "NEVER swallow dry Isabgol or take without adequate water (choking/bowel blockage hazard). Avoid if sudden severe abdominal pain or inability to pass gas occurs.",
            caution_ur = "اسپغول خشک ہرگز نہ نگلیں اور کم پانی کے ساتھ نہ لیں۔ شدید پیٹ درد میں استعمال نہ کریں۔",
        ),
        HomeRemedyItem(
            id = "rem_heat_cooling",
            category = "HEAT_SKIN",
            title_en = "Heat Exhaustion Shade & Cool Wet-Cloth Cooling",
            title_ur = "لو لگنے پر فوری ٹھنڈک اور سایہ دار آرام",
            ingredients_en = "Cool clean water, damp cotton towels, shaded ventilated room or fan.",
            ingredients_ur = "صاف ٹھنڈا پانی، گیلے سوتی کپڑے اور سایہ دار ہوا دار کمرہ یا پنکھا۔",
            steps_en = listOf(
                "Move immediately to shade or fan airflow, lie down with legs slightly elevated, and loosen outer clothing.",
                "Place cool damp cloths on neck, armpits, groin, and forehead while sipping cool water or ORS.",
            ),
            steps_ur = listOf(
                "مریض کو فوراً سایہ اور ہوا دار جگہ لٹا کر ٹانگیں ہلکی اونچی کریں اور تنگ کپڑے ڈھیلے کریں۔",
                "گردن، بغلوں اور ماتھے پر ٹھنڈی گیلی پٹیاں رکھیں اور تھوڑا تھوڑا او آر ایس دیں۔",
            ),
            caution_en = "If confused, fainting, seizing, vomiting, or hot dry skin with no sweat (heat stroke), call 1122 immediately.",
            caution_ur = "اگر مریض بہکی باتیں کرے یا بے ہوش ہو جائے تو فوراً 1122 پر کال کریں۔",
        ),
        HomeRemedyItem(
            id = "rem_burn_cool_water",
            category = "HEAT_SKIN",
            title_en = "First Aid for Minor Kitchen Burns (Cool Running Water Only)",
            title_ur = "معمولی جلنے کی فوری ابتدائی امداد (صرف ٹھنڈا بہتا پانی)",
            ingredients_en = "Cool (not ice) running tap water for 20 minutes, clean non-fluffy cling film or sterile gauze.",
            ingredients_ur = "نلکے کا ٹھنڈا بہتا پانی (برف نہیں) 20 منٹ تک، صاف کلنگ فلم یا جراثیم سے پاک پٹی۔",
            steps_en = listOf(
                "Hold the burned skin gently under cool running tap water for a full 20 minutes to stop deep tissue heat.",
                "Gently remove rings/watches before swelling starts, then cover loosely with clean non-stick wrap or sterile dressing.",
            ),
            steps_ur = listOf(
                "جلی ہوئی جگہ کو فوری طور پر 20 منٹ تک ٹھنڈے بہتے پانی کے نیچے رکھیں۔",
                "سوجن سے پہلے انگوٹھی یا گھڑی اتار دیں اور صاف غیر چپکنے والی پٹی یا کلنگ فلم ہلکی سی لپیٹ دیں۔",
            ),
            caution_en = "MYTH ALERT: NEVER apply toothpaste, butter, ice, egg, ink, or turmeric powder on burns (causes severe infection). Rush to hospital for face/hand/deep/large burns.",
            caution_ur = "خبردار: جلے ہوئے زخم پر ٹوتھ پیسٹ، مکھن، برف یا ہلدی ہرگز نہ لگائیں۔ گہرے یا چہرے/ہاتھ کے جلنے پر فوری ہسپتال جائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_minor_cut",
            category = "HEAT_SKIN",
            title_en = "Clean Soap-and-Water First Aid for Minor Cuts & Scrapes",
            title_ur = "معمولی زخم اور خراش کی صفائی اور پٹی",
            ingredients_en = "Clean running water, mild soap, clean folded gauze pad or cloth.",
            ingredients_ur = "صاف بہتا پانی، سادہ صابن اور صاف سوتی پٹی یا گوز۔",
            steps_en = listOf(
                "Apply gentle direct pressure with clean cloth for 5–10 minutes until minor bleeding stops.",
                "Rinse wound thoroughly under clean running water (wash skin around it with mild soap) and cover with clean dry gauze.",
            ),
            steps_ur = listOf(
                "صاف کپڑے سے 5 سے 10 منٹ ہلکا دباؤ ڈال کر خون روکیں۔",
                "زخم کو صاف بہتے پانی سے اچھی طرح دھوئیں اور صاف خشک پٹی باندھ دیں۔",
            ),
            caution_en = "Do not pack soil, surma, tobacco, or ash into cuts (tetanus danger). Visit clinic for deep/gaping cuts, animal bites, or tetanus booster check.",
            caution_ur = "زخم میں مٹی، سرمہ یا راکھ ہرگز نہ لگائیں۔ گہرے زخم یا کتے کے کاٹنے پر فوری ہسپتال اور تشنج کا ٹیکہ لگوائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_sprain_rice",
            category = "PAIN_RECOVERY",
            title_en = "Ankle / Wrist Sprain Rest & Wrapped Cold Compress",
            title_ur = "موچ اور ہلکی سوجن کے لیے آرام اور برف کی ٹکور",
            ingredients_en = "Ice cubes wrapped in a damp towel (never bare ice on skin), pillow for elevation.",
            ingredients_ur = "گیلے کپڑے میں لپٹی ہوئی برف (براہِ راست جلد پر نہیں)، سہارے کے لیے تکیہ۔",
            steps_en = listOf(
                "Rest the injured joint and elevate it on a pillow above chest level when sitting or lying down.",
                "Apply towel-wrapped ice pack for 15 minutes every 2–3 hours during the first 24–48 hours to limit swelling.",
            ),
            steps_ur = listOf(
                "متاثرہ جوڑ کو آرام دیں اور بیٹھتے یا لیٹتے وقت تکیے پر ہلکا اونچا رکھیں۔",
                "پہلے 24 سے 48 گھنٹے ہر دو تین گھنٹے بعد کپڑے میں لپٹی برف سے 15 منٹ ٹکور کریں۔",
            ),
            caution_en = "Seek X-ray/hospital care if unable to bear weight for 4 steps, visible bone deformity, numbness, or severe throbbing pain.",
            caution_ur = "اگر پاؤں زمین پر نہ رکھا جا سکے، ہڈی ٹیڑھی لگے یا سن پن ہو تو فوری ہسپتال جائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_tension_headache",
            category = "PAIN_RECOVERY",
            title_en = "Hydration, Screen Rest & Warm Neck Compress for Tension Headache",
            title_ur = "پٹھوں کے کھنچاؤ والے سر درد کے لیے پانی اور گردن کی ٹکور",
            ingredients_en = "1–2 glasses clean water, warm damp towel on back of neck, quiet dim room.",
            ingredients_ur = "1 سے 2 گلاس صاف پانی، گردن کے پیچھے نیم گرم گیلا تولیہ، پُرسکون کمرہ۔",
            steps_en = listOf(
                "Drink 1–2 glasses of water (mild dehydration often triggers afternoon headaches) and step away from bright screens for 15 minutes.",
                "Place a warm damp towel behind the neck/shoulders to relax tight muscles and rest quietly.",
            ),
            steps_ur = listOf(
                "1 سے 2 گلاس صاف پانی پئیں اور اسکرین سے 15 منٹ کے لیے دور رہیں۔",
                "گردن اور کندھوں کے پیچھے نیم گرم تولیہ رکھ کر پٹھوں کو ڈھیلا چھوڑیں۔",
            ),
            caution_en = "EMERGENCY: Sudden 'worst-ever' thunderclap headache, stiff neck, fever, blurred vision, slurred speech, or BP >= 180/120 requires immediate 1122/hospital care.",
            caution_ur = "فوری خطرہ: اچانک شدید ترین سر درد، گردن میں اکڑاؤ، بخار، دھندلی نظر یا بی پی 180/120 پر فوری ہسپتال جائیں۔",
        ),
        HomeRemedyItem(
            id = "rem_sleep_calm",
            category = "STRESS_SLEEP",
            title_en = "Cyclic Sighing & Caffeine-Free Wind-Down for Deeper Sleep",
            title_ur = "پُرسکون نیند اور ذہنی سکون کے لیے سانس کی مشق",
            ingredients_en = "5 minutes quiet breathing, caffeine-free mint/chamomile tea, dim evening light.",
            ingredients_ur = "5 منٹ پُرسکون سانس کی مشق، پودینہ یا بابونہ قہوہ اور مدھم روشنی۔",
            steps_en = listOf(
                "Practice 6 cycles of double-inhale through the nose followed by a slow 6-second mouth exhale.",
                "Stop strong chai/tea, coffee, and heavy fried food at least 4–5 hours before bedtime; keep room cool and dark.",
            ),
            steps_ur = listOf(
                "ناک سے دو بار گہرا سانس لے کر منہ سے 6 سیکنڈ میں آہستہ باہر نکالنے کے 6 چکر کریں۔",
                "سونے سے 5 گھنٹے پہلے چائے، کافی اور بھاری تلی ہوئی خوراک سے پرہیز کریں۔",
            ),
            caution_en = "Chest tightness, sweating, jaw/arm pain, or sudden shortness of breath must NEVER be dismissed as stress — call 1122.",
            caution_ur = "سینے میں جکڑن یا بازو میں درد کو کبھی محض ذہنی دباؤ نہ سمجھیں، فوراً 1122 پر کال کریں۔",
        ),
    )

    data class BmiAssessment(
        val bmi: Double?,
        val category: String, // UNDERWEIGHT, OPTIMAL, OVERWEIGHT, OBESE, UNAVAILABLE
        val labelEn: String,
        val labelUr: String,
        val idealMinKg: Double?,
        val idealMaxKg: Double?,
        val recommendedDeltaKg: Double,
        val recommendedGoal: WellnessGoal,
        val recommendedWaterMl: Int,
        val recommendedSteps: Int,
        val adviceEn: String,
        val adviceUr: String,
    )

    fun computeBmiAssessment(weightKg: Double?, heightCm: Double?, ageMonths: Int = 300): BmiAssessment {
        val safeW = weightKg?.takeIf { it > 2.0 }
        val safeH = heightCm?.takeIf { it > 40.0 }
        if (safeW == null || safeH == null || ageMonths < 18 * 12) {
            val water = safeW?.let { (it * 32.0).toInt().coerceIn(1500, 3500) } ?: 2200
            val steps = if (ageMonths >= 60 * 12) 4500 else 6500
            return BmiAssessment(
                bmi = null,
                category = "UNAVAILABLE",
                labelEn = if (ageMonths < 18 * 12) "Pediatric Growth Tracking" else "Enter Height & Weight for Body Mass Advice",
                labelUr = if (ageMonths < 18 * 12) "بچوں کی نشوونما کا ریکارڈ" else "قد اور وزن درج کریں تاکہ درست طبی مشورہ ملے",
                idealMinKg = null,
                idealMaxKg = null,
                recommendedDeltaKg = 0.0,
                recommendedGoal = if (ageMonths >= 60 * 12) WellnessGoal.SENIOR_MOBILITY else WellnessGoal.BALANCED,
                recommendedWaterMl = water,
                recommendedSteps = steps,
                adviceEn = if (ageMonths < 18 * 12) {
                    "Pediatric weight is tracked by age-specific clinician curves rather than adult BMI cutoffs."
                } else {
                    "Provide your height and weight so Tibb can calculate your healthy weight range, water intake, and step goals."
                },
                adviceUr = if (ageMonths < 18 * 12) {
                    "بچوں کے لیے قد اور وزن عمر کے حساب سے ڈاکٹر کے چارٹ کے مطابق دیکھا جاتا ہے۔"
                } else {
                    "اپنا قد اور وزن درج کریں تاکہ طب آپ کے لیے پانی، قدم اور صحت مند وزن کا ہدف تجویز کر سکے۔"
                },
            )
        }
        val hM = safeH / 100.0
        val bmi = round((safeW / (hM * hM)) * 10.0) / 10.0
        val idealMinKg = round(18.5 * hM * hM * 10.0) / 10.0
        val idealMaxKg = round(22.9 * hM * hM * 10.0) / 10.0
        val isSenior = ageMonths >= 60 * 12
        val baseWater = (safeW * 33.0).toInt().coerceIn(2000, 3600)

        return when {
            bmi < 18.5 -> {
                val gain = max(0.5, round((idealMinKg - safeW) * 10.0) / 10.0)
                BmiAssessment(
                    bmi = bmi,
                    category = "UNDERWEIGHT",
                    labelEn = "Underweight · Healthy Weight Gain Needed",
                    labelUr = "کم وزن · صحت مند وزن بڑھانے کی ضرورت",
                    idealMinKg = idealMinKg,
                    idealMaxKg = idealMaxKg,
                    recommendedDeltaKg = gain,
                    recommendedGoal = WellnessGoal.MUSCLE_GAIN,
                    recommendedWaterMl = baseWater,
                    recommendedSteps = if (isSenior) 4500 else 6000,
                    adviceEn = "Your BMI is $bmi. To reach your healthy range ($idealMinKg–$idealMaxKg kg), aim to gain ~$gain kg gradually through protein-rich home meals & strength work.",
                    adviceUr = "آپ کا بی ایم آئی $bmi ہے۔ صحت مند وزن ($idealMinKg تا $idealMaxKg کلو) کے لیے تقریباً $gain کلو وزن پروٹین اور متوازن غذا سے بتدریج بڑھائیں۔",
                )
            }
            bmi <= 22.9 -> BmiAssessment(
                bmi = bmi,
                category = "OPTIMAL",
                labelEn = "Optimal Healthy Range",
                labelUr = "مثالی اور صحت مند وزن",
                idealMinKg = idealMinKg,
                idealMaxKg = idealMaxKg,
                recommendedDeltaKg = 0.0,
                recommendedGoal = if (isSenior) WellnessGoal.SENIOR_MOBILITY else WellnessGoal.BALANCED,
                recommendedWaterMl = baseWater,
                recommendedSteps = if (isSenior) 5000 else 7500,
                adviceEn = "Your BMI is $bmi, right inside the South Asian healthy range ($idealMinKg–$idealMaxKg kg). Focus on steady hydration, daily movement, and balanced meals.",
                adviceUr = "آپ کا بی ایم آئی $bmi ہے جو بالکل صحت مند حد ($idealMinKg تا $idealMaxKg کلو) میں ہے۔ روزانہ پانی، چہل قدمی اور متوازن غذا برقرار رکھیں۔",
            )
            bmi < 25.0 -> {
                val lose = max(0.5, round((safeW - idealMaxKg) * 10.0) / 10.0)
                BmiAssessment(
                    bmi = bmi,
                    category = "OVERWEIGHT",
                    labelEn = "Mildly Above Optimal · Gradual Fat Loss Advised",
                    labelUr = "وزن ہلکا زیادہ · بتدریج کمی کا مشورہ",
                    idealMinKg = idealMinKg,
                    idealMaxKg = idealMaxKg,
                    recommendedDeltaKg = -lose,
                    recommendedGoal = WellnessGoal.WEIGHT_LOSS,
                    recommendedWaterMl = min(3600, baseWater + 250),
                    recommendedSteps = if (isSenior) 5500 else 8500,
                    adviceEn = "Your BMI is $bmi. Shedding ~$lose kg gently toward $idealMaxKg kg reduces metabolic & blood-pressure risk. Pair 8,500 daily steps with portion control.",
                    adviceUr = "آپ کا بی ایم آئی $bmi ہے۔ شوگر اور بی پی سے بچاؤ کے لیے تقریباً $lose کلو وزن بتدریج کم کر کے $idealMaxKg کلو تک لانا بہتر ہے۔",
                )
            }
            else -> {
                val lose = max(1.0, round((safeW - idealMaxKg) * 10.0) / 10.0)
                BmiAssessment(
                    bmi = bmi,
                    category = "OBESE",
                    labelEn = "Elevated Body Mass · Structured Weight Loss Recommended",
                    labelUr = "زیادہ وزن · باقاعدہ وزن کم کرنے کی ضرورت",
                    idealMinKg = idealMinKg,
                    idealMaxKg = idealMaxKg,
                    recommendedDeltaKg = -lose,
                    recommendedGoal = WellnessGoal.WEIGHT_LOSS,
                    recommendedWaterMl = min(3800, baseWater + 350),
                    recommendedSteps = if (isSenior) 6000 else 9500,
                    adviceEn = "Your BMI is $bmi. Gradual reduction of ~$lose kg toward $idealMaxKg kg with daily brisk walking, higher hydration, and low-sugar Desi meals is strongly advised.",
                    adviceUr = "آپ کا بی ایم آئی $bmi ہے۔ صحت مند حد ($idealMaxKg کلو) تک آنے کے لیے تقریباً $lose کلو وزن روزانہ تیز واک اور کم چکنائی والی خوراک سے بتدریج کم کریں۔",
                )
            }
        }
    }

    private fun makeSlot(slot: String, options: List<MealOptionItem>, variationSeed: Int): DietMealSlot {
        val safeIdx = ((variationSeed % options.size) + options.size) % options.size
        val primary = options[safeIdx]
        return DietMealSlot(
            slot = slot,
            title_en = primary.title_en,
            title_ur = primary.title_ur,
            portions_en = primary.portions_en,
            portions_ur = primary.portions_ur,
            caloriesApprox = primary.caloriesApprox,
            selectedOptionIndex = safeIdx,
            alternatives = options,
        )
    }

    fun buildPersonalizedPlanOffline(
        person: PatientProfile,
        goal: WellnessGoal,
        variationSeed: Int = 0,
    ): PersonalizedWellnessPlan {
        val hasDiabetes = person.chronicConditions.any { it.contains("DIABETES", ignoreCase = true) } || goal == WellnessGoal.DIABETES_CONTROL
        val hasBp = person.chronicConditions.contains("HYPERTENSION") || goal == WellnessGoal.BP_FRIENDLY
        val isWeightLoss = goal == WellnessGoal.WEIGHT_LOSS
        val isMuscleGain = goal == WellnessGoal.MUSCLE_GAIN || goal == WellnessGoal.ENERGY_STRENGTH
        val isSenior = person.ageMonths >= 60 * 12 || goal == WellnessGoal.SENIOR_MOBILITY

        val bmiCheck = computeBmiAssessment(person.weightKg, person.heightCm, person.ageMonths)
        val dailyWaterGoalMl = bmiCheck.recommendedWaterMl
        val dailyStepsGoal = when {
            isSenior -> 4800
            isWeightLoss -> max(8500, bmiCheck.recommendedSteps)
            isMuscleGain -> 7500
            else -> bmiCheck.recommendedSteps
        }
        val isVeg = person.dietaryPreferences.any { it.contains("veg", ignoreCase = true) }

        val weightLossSlots = listOf(
            makeSlot(
                "Breakfast",
                listOf(
                    MealOptionItem(
                        "Oats / Barley Daliya in Skim Milk + 1 Boiled Egg White Omelette",
                        "جو کا دلیہ + 1 انڈے کی سفیدی کا آملیٹ (بغیر مکھن)",
                        "High protein & soluble fiber keeps fullness high for fewer calories.",
                        "کم کیلوریز اور بھرپور فائبر جو دوپہر تک پیٹ بھرا رکھتا ہے۔",
                        290,
                    ),
                    MealOptionItem(
                        "2 Thin Besan Chillas + Mint Chutney + Unsweetened Dahi",
                        "2 پتلے بیسن کے چلے + پودینے کی چٹنی + بغیر چینی دہی",
                        "Low-GI chickpea protein keeps morning cravings steady.",
                        "بیسن کی پروٹین بھوک کنٹرول رکھتی ہے۔",
                        300,
                    ),
                    MealOptionItem(
                        if (isVeg) "1 Whole-Wheat Bran Toast + Low-Fat Paneer Scramble + Green Tea"
                        else "2 Boiled Eggs (1 yolk) + 1 Small Bran Roti + Cucumber Slices",
                        if (isVeg) "1 چوکر بریڈ + کم چکنائی پنیر بھرجی + سبز چائے"
                        else "2 ابلے انڈے + 1 چھوٹی چکی روٹی + کھیرے کے قتلے",
                        "Clean lean breakfast under 310 kcal.",
                        "310 کیلوریز سے کم ہلکا اور غذائیت بھرا ناشتہ۔",
                        285,
                    ),
                ),
                variationSeed,
            ),
            makeSlot(
                "Mid-Day Snack",
                listOf(
                    MealOptionItem("Cucumber & Guava Bowl + Sugar-Free Mint Kahwa", "کھیرا اور امرود + بغیر چینی پودینہ قہوہ", "Hydrating crunch under 110 kcal.", "کم کیلوریز اور وٹامن سی سے بھرپور۔", 110),
                    MealOptionItem("Roasted Makhana / Foxnuts (25g) + 5 Soaked Almonds", "بھنے مکھانے (25 گرام) + 5 بھیگے بادام", "Satisfies crisp snack cravings without fried oils.", "تلی ہوئی چیزوں کا بہترین متبادل۔", 135),
                    MealOptionItem("Unsweetened Zeera Chaach / Thin Lassi + Chia Seeds (1 tsp)", "بغیر نمک زیرہ لسی + 1 چمچ تخمِ ملنگا", "Cooling probiotic drink promoting satiety.", "معدے کے لیے ٹھنڈا اور ہلکا مشروب۔", 105),
                ),
                variationSeed,
            ),
            makeSlot(
                "Lunch",
                listOf(
                    MealOptionItem(
                        if (isVeg) "Steamed Moong Daal + Big Kachumber Salad + 1 Thin Whole-Wheat Roti"
                        else "120g Grilled Chicken Breast or Tikka + Mixed Salad + 1 Thin Roti",
                        if (isVeg) "مونگ کی دال + بڑا کچومر سلاد + 1 پتلی چپاتی"
                        else "120 گرام گرلڈ چکن تکہ + کچومر سلاد + 1 پتلی روٹی",
                        "Eat salad first, keep cooking oil to 1 tsp.",
                        "پہلے سلاد کھائیں اور صرف 1 چمچ تیل استعمال کریں۔",
                        410,
                    ),
                    MealOptionItem("Kala Chana / Chickpea Chaat Bowl with Onion, Tomato, Lemon & 1 Roti", "کالے چنے کی چاٹ + 1 پتلی روٹی", "Slow-digesting legume fiber & steady afternoon energy.", "فائبر اور پروٹین سے بھرپور دوپہر کا کھانا۔", 395),
                    MealOptionItem(
                        if (isVeg) "Palak Paneer (light oil) + 1 Small Bowl Brown Rice + Raita"
                        else "140g Baked/Grilled Rahu Fish + Sautéed Greens + 1 Thin Roti",
                        if (isVeg) "پالک پنیر (کم تیل) + چھوٹا پیالہ براؤن چاول + رائتہ"
                        else "140 گرام گرلڈ مچھلی + ہلکی تلی سبزی + 1 پتلی روٹی",
                        "Omega-rich lean protein keeps fat loss sustainable.",
                        "ہلکی چکنائی کے ساتھ بہترین پروٹین۔",
                        420,
                    ),
                ),
                variationSeed,
            ),
            makeSlot(
                "Dinner",
                listOf(
                    MealOptionItem("Clear Daal / Chicken Vegetable Soup + Sautéed Lauki or Cabbage", "سبزی اور دال کا سوپ + کم تیل میں لوکی یا بند گوبھی", "Light, early dinner at least 2.5 hours before sleep.", "سونے سے ڈھائی گھنٹے پہلے ہلکا اور زود ہضم کھانا۔", 280),
                    MealOptionItem(
                        if (isVeg) "Steamed Tori / Tinda Sabzi + 1 Cup Dahi Raita (No Roti)"
                        else "100g Boiled/Air-Fried Chicken Strips + Steamed Vegetables + Raita",
                        if (isVeg) "توری یا ٹینڈے کی ہلکی سبزی + 1 کپ دہی رائتہ"
                        else "100 گرام ہلکا گرلڈ چکن + ابلی سبزیاں + رائتہ",
                        "Low-carb evening meal avoids overnight glucose storage.",
                        "رات کا ہلکا اور کم کاربوہائیڈریٹ کھانا۔",
                        295,
                    ),
                    MealOptionItem("Light Khichdi (1/2 cup) with Extra Vegetables + Mint Yogurt", "سبزی والی ہلکی مونگ کھچڑی (آدھا پیالہ) + پودینہ دہی", "Gentle on digestion before bedtime.", "ہلکی اور آسانی سے ہضم ہونے والی غذا۔", 310),
                ),
                variationSeed,
            ),
        )

        val muscleGainSlots = listOf(
            makeSlot(
                "Breakfast",
                listOf(
                    MealOptionItem(
                        if (isVeg) "2 Besan Chillas + Thick Home Dahi + 1 Banana + Handful Almonds"
                        else "2 Whole Eggs + 1 Whole-Wheat Paratha (1 tsp ghee) + Unsweetened Lassi",
                        if (isVeg) "2 بیسن روٹی + گاڑھا دہی + 1 کیلا + بادام"
                        else "2 انڈے + 1 ہلکے گھی کی روٹی + دودھ یا لسی",
                        "~24g natural protein & slow-digesting complex carbs.",
                        "عضلات کی مضبوطی کے لیے قدرتی پروٹین اور توانائی۔",
                        480,
                    ),
                    MealOptionItem("Milk Oats Porridge + Peanut Butter (1 tbsp) + 3 Dates + 2 Boiled Eggs", "دودھ میں جو کا دلیہ + 1 چمچ پینٹ بٹر + 3 کھجور + 2 انڈے", "Calorie & protein boost for lean muscle recovery.", "طاقت اور پٹھوں کی نشوونما کے لیے بھرپور ناشتہ۔", 520),
                    MealOptionItem(
                        if (isVeg) "Moong Daal Cheela Stuffed with Paneer + Warm Milk Glass"
                        else "Chicken & Cheese Egg Omelette + 2 Whole-Wheat Toasts",
                        if (isVeg) "پنیر بھری مونگ دال روٹی + 1 گلاس دودھ"
                        else "چکن اور انڈے کا آملیٹ + 2 گندم ٹوسٹ",
                        "Balanced amino acids from start of day.",
                        "دن کے آغاز سے ہی بہترین پروٹین۔",
                        495,
                    ),
                ),
                variationSeed,
            ),
            makeSlot(
                "Mid-Day Snack",
                listOf(
                    MealOptionItem("Roasted Kala Chana (50g) + 8 Almonds/Walnuts + 2 Dates", "بھنے کالے چنے (50 گرام) + 8 بادام/اخروٹ + 2 کھجور", "Calorie-dense clean desi protein & iron snack.", "پٹھوں اور طاقت کے لیے بہترین دیسی خوراک۔", 260),
                    MealOptionItem("Banana + Thick Yogurt/Milk Shake with Crushed Sattu (2 tbsp)", "کیلا اور ستو (2 چمچ) دودھ/دہی شیک (بغیر چینی)", "Traditional Pakistani energy & glycogen shake.", "توانائی اور طاقت کے لیے روایتی ستو شیک۔", 290),
                    MealOptionItem("Boiled Sweet Potato (Shakarkandi) + Handful Roasted Peanuts", "ابلی شکر قندی + مٹھی بھر بھنی مونگ پھلی", "Clean carbs + healthy fats pre-workout fuel.", "ورزش سے پہلے بہترین توانائی کا ذریعہ۔", 275),
                ),
                variationSeed,
            ),
            makeSlot(
                "Lunch",
                listOf(
                    MealOptionItem(
                        if (isVeg) "Rajma / Chana Masala + Basmati Rice + Dahi Raita + Salad"
                        else "160g Chicken / Fish Karahi (light oil) + 2 Whole-Wheat Rotis + Dahi",
                        if (isVeg) "لوبیا یا سفید چنے + چاول + دہی رائتہ + سلاد"
                        else "160 گرام چکن یا مچھلی + 2 چکی آٹے کی روٹیاں + دہی",
                        "Rich post-workout protein & glycogen replenishment.",
                        "ورزش کے بعد پٹھوں کی مرمت کے لیے مکمل کھانا۔",
                        620,
                    ),
                    MealOptionItem(
                        if (isVeg) "2 Bowls Thick Arhar/Chana Daal + Paneer Cubes + 2 Rotis"
                        else "150g Beef/Mutton Yakhni Pulao (lean cuts) + Kachumber + Raita",
                        if (isVeg) "گاڑھی چنا دال + پنیر + 2 روٹیاں"
                        else "150 گرام کم چکنائی یخنی پلاؤ + کچومر سلاد + رائتہ",
                        "Hearty whole-food lunch supporting muscle hypertrophy.",
                        "صحت مند وزن اور طاقت بڑھانے والا دوپہر کا کھانا۔",
                        640,
                    ),
                    MealOptionItem(
                        if (isVeg) "Palak Paneer + Masoor Daal + 2 Rotis + Salad"
                        else "Chicken Tikka Boti + Daal Mash (light oil) + 2 Tandoori Rotis",
                        if (isVeg) "پالک پنیر + مسور دال + 2 روٹیاں"
                        else "چکن تکہ بوٹی + دال ماش + 2 تندوری روٹیاں",
                        "High bioavailable protein + complex iron/zinc.",
                        "پروٹین اور آئرن سے بھرپور خوراک۔",
                        610,
                    ),
                ),
                variationSeed,
            ),
            makeSlot(
                "Dinner",
                listOf(
                    MealOptionItem(
                        if (isVeg) "Paneer / Tofu Bhurji + 1 Roti + Warm Haldi Milk before bed"
                        else "Minced Lean Keema with Peas + 1 Roti + Warm Milk",
                        if (isVeg) "پنیر بھرجی + 1 روٹی + سونے سے پہلے نیم گرم دودھ"
                        else "مٹر قیمہ (کم تیل) + 1 روٹی + نیم گرم دودھ",
                        "Slow-release casein & protein recovery overnight.",
                        "رات بھر جسم کی بحالی اور طاقت کے لیے۔",
                        520,
                    ),
                    MealOptionItem(
                        if (isVeg) "Mixed Lentil & Vegetable Khichdi with 1 tsp Desi Ghee + Dahi"
                        else "140g Grilled Fish / Chicken + Mashed Potatoes & Steamed Beans",
                        if (isVeg) "مکس دال سبزی کھچڑی (1 چمچ دیسی گھی) + دہی"
                        else "140 گرام گرلڈ مچھلی/چکن + آلو و پھلیاں",
                        "Clean carbs and protein without feeling overly stuffed.",
                        "پٹھوں کی مرمت کے لیے متوازن رات کا کھانا۔",
                        495,
                    ),
                    MealOptionItem(
                        if (isVeg) "Chana & Vegetable Curry + 1.5 Whole-Wheat Rotis + Milk"
                        else "Shami Kabab (2 lean) + 1 Whole-Wheat Roti + Vegetable Soup",
                        if (isVeg) "سفید چنے و سبزی + ڈیڑھ روٹی + دودھ"
                        else "2 شامی کباب (کم تیل) + 1 روٹی + سبزی سوپ",
                        "Easy-to-digest protein before rest.",
                        "رات کے آرام سے پہلے زود ہضم پروٹین۔",
                        485,
                    ),
                ),
                variationSeed,
            ),
        )

        val balancedSlots = listOf(
            makeSlot(
                "Breakfast",
                listOf(
                    MealOptionItem(
                        if (hasDiabetes) "1 Small Whole-Wheat Bran Roti + 1 Boiled Egg + Unsweetened Dahi"
                        else "Whole-Wheat Besan Chilla or Porridge (Daliya) + Boiled Egg",
                        if (hasDiabetes) "1 چھوٹی چکی آٹے کی روٹی + 1 ابلا انڈہ + بغیر چینی دہی"
                        else "گندم کا دلیہ یا بیسن روٹی + 1 ابلا انڈہ",
                        "High fiber, steady morning energy without sugar spike.",
                        "فائبر سے بھرپور ناشتہ جو شوگر کو متوازن رکھتا ہے۔",
                        340,
                    ),
                    MealOptionItem(
                        if (isVeg) "Oats & Flaxseed Daliya in Low-Fat Milk + 4 Almonds"
                        else "Vegetable Omelette (tomato, onion, spinach) + 1 Whole-Wheat Roti",
                        if (isVeg) "جو اور السی کا دلیہ + 4 بادام"
                        else "سبزی والا آملیٹ + 1 چکی آٹے کی روٹی",
                        if (hasBp) "Cooked with pinch-free herbs instead of extra salt." else "Balanced vitamins and steady satiety.",
                        "متوازن وٹامنز اور صحت مند آغاز۔",
                        350,
                    ),
                    MealOptionItem("1 Bowl Unsweetened Dahi + 2 Tbsp Boiled Oats/Daliya + Guava/Apple", "1 پیالہ دہی + 2 چمچ جو کا دلیہ + امرود یا سیب", "Light gut-friendly probiotic breakfast.", "معدے اور ہاضمے کے لیے ہلکا ناشتہ۔", 315),
                ),
                variationSeed,
            ),
            makeSlot(
                "Mid-Day Snack",
                listOf(
                    MealOptionItem("Guava or Apple Slices + 6 Unsalted Almonds + Water", "امرود یا سیب + 6 بغیر نمک بادام + صاف پانی", if (hasBp) "Potassium-rich fruit, strictly zero added chaat salt." else "Balanced micronutrients & satiety.", "بغیر نمک تازہ پھل اور بادام۔", 160),
                    MealOptionItem("Roasted Chickpeas (30g) + Mint/Lemon Cucumber Salad", "بھنے چنے (30 گرام) + کھیرے اور پودینے کا سلاد", "Low-GI crunch ideal for sugar and BP stability.", "شوگر اور بی پی کے لیے بہترین ہلکی غذا۔", 155),
                    MealOptionItem("4 Walnuts + Sugar-Free Green Tea / Cardamom Kahwa", "4 اخروٹ + بغیر چینی سبز چائے / الائچی قہوہ", "Heart-healthy omega-3 fats & antioxidants.", "دل اور دماغ کے لیے مفید صحت مند چکنائی۔", 145),
                ),
                variationSeed,
            ),
            makeSlot(
                "Lunch",
                listOf(
                    MealOptionItem(
                        if (isVeg) "Masoor / Moong Daal + Vegetable Sabzi + Cucumber Salad + 1 Whole-Wheat Roti"
                        else "Masoor / Moong Daal + Grilled Chicken or Fish + Salad + 1 Roti",
                        if (isVeg) "مسور یا مونگ دال + سبزی + کچومر سلاد + 1 روٹی"
                        else "مسور یا مونگ دال + گرلڈ چکن/مچھلی + کچومر سلاد + 1 روٹی",
                        if (hasBp) "Cooked with minimal salt & 1 tsp mustard/olive oil; fresh lemon instead of pickle." else "Half plate salad/sabzi, quarter protein, quarter whole grain.",
                        "آدھی پلیٹ سبزی و سلاد، ایک چوتھائی پروٹین، 1 چپاتی۔",
                        480,
                    ),
                    MealOptionItem(
                        if (isVeg) "Aloo Gobhi / Matar Paneer (light oil) + Daal + 1 Multigrain Roti"
                        else "Chicken Shorba / Karahi (skimmed oil) + Bhindi Sabzi + 1 Roti",
                        if (isVeg) "مٹر پنیر / گوبھی (کم تیل) + دال + 1 ملٹی گرین روٹی"
                        else "کم تیل چکن شوربہ + بھنڈی سبزی + 1 روٹی",
                        "Portion-balanced everyday Pakistani household lunch.",
                        "گھر کے عام کھانے کا متوازن اور صحت مند انداز۔",
                        470,
                    ),
                    MealOptionItem(
                        if (isVeg) "Red Kidney Beans (Lobia) Curry + 1 Small Bowl Brown Rice + Raita"
                        else "Baked/Pan-Seared Fish + Spinach/Saag + 1 Whole-Wheat Roti",
                        if (isVeg) "لال لوبیا سالن + چھوٹا پیالہ براؤن چاول + رائتہ"
                        else "گرلڈ مچھلی + پالک ساگ + 1 روٹی",
                        "Slow-release carbohydrates & iron support.",
                        "آئرن اور متوازن توانائی سے بھرپور کھانا۔",
                        465,
                    ),
                ),
                variationSeed,
            ),
            makeSlot(
                "Dinner",
                listOf(
                    MealOptionItem("Steamed or Lightly Stir-Fried Bhindi / Lauki Sabzi + Bowl of Raita", "کم تیل میں پکی لوکی یا بھنڈی کی سبزی + رائتہ", "Eat at least 2 hours before sleep to support digestion & overnight glucose.", "سونے سے کم از کم 2 گھنٹے پہلے ہلکا کھانا کھائیں۔", 360),
                    MealOptionItem(
                        if (isVeg) "Moong Daal Soup + Sautéed Cabbage & Carrot + 1 Small Roti"
                        else "100g Shredded Chicken & Vegetable Yakhni + 1 Small Roti",
                        if (isVeg) "مونگ دال سوپ + بند گوبھی گاجر + 1 چھوٹی روٹی"
                        else "100 گرام چکن و سبزی یخنی + 1 چھوٹی روٹی",
                        "Warm, soothing, light-sodium dinner.",
                        "ہلکا، کم نمک اور زود ہضم رات کا کھانا۔",
                        345,
                    ),
                    MealOptionItem("Vegetable Khichdi (3/4 bowl) + Fresh Mint Yogurt", "سبزی کھچڑی + پودینے والا دہی", "Easy evening recovery meal.", "پُرسکون نیند کے لیے ہلکی غذا۔", 355),
                ),
                variationSeed,
            ),
        )

        val dietMeals = when {
            isWeightLoss -> weightLossSlots
            isMuscleGain -> muscleGainSlots
            else -> balancedSlots
        }

        val workouts = when {
            isSenior -> listOf(
                WorkoutExerciseSlot("Gentle Courtyard / Hallway Walk", "صحن یا کمرے میں ہلکی چہل قدمی", "15–20 mins", "Gentle", "Wear supportive flat shoes; stop if dizzy or short of breath."),
                WorkoutExerciseSlot("Seated Ankle Circles & Shoulder Rolls", "کرسی پر بیٹھ کر ٹخنوں اور کندھوں کی ورزش", "10 reps x 2 sets", "Gentle", "Improves circulation and lowers stiffness safely."),
                WorkoutExerciseSlot("Supported Chair Sit-to-Stand", "کرسی سے سہارا لے کر اٹھنے بیٹھنے کی مشق", "8 slow reps", "Gentle", "Exhale while standing up cleanly."),
            )
            isWeightLoss -> listOf(
                WorkoutExerciseSlot("Brisk Fat-Burn Walk / Marching Intervals", "تیز چہل قدمی (وقفوں کے ساتھ)", "30–35 mins", "Brisk", "Alternate 3 mins brisk pace with 1 min steady walk to burn fat safely."),
                WorkoutExerciseSlot("Bodyweight Squats + Step-Ups + Plank Hold", "اسکواٹس، سیڑھی چڑھنا اور پلینک ہولڈ", "12 reps x 3 sets", "Moderate", "Preserves lean muscle while targeting overall calorie deficit."),
                WorkoutExerciseSlot("Post-Dinner 15-Minute Stroll + Breath Wind-Down", "رات کے کھانے کے بعد 15 منٹ چہل قدمی", "15 mins", "Gentle", "Supports overnight metabolic rate and digestion."),
            )
            isMuscleGain -> listOf(
                WorkoutExerciseSlot("Push-Ups (Incline or Standard) & Pike Shoulder Press", "پش اپس اور کندھوں کی مضبوطی کی مشق", "10–15 reps x 3 sets", "Brisk", "Lower slowly for 3 seconds on each rep to stimulate muscle growth."),
                WorkoutExerciseSlot("Bulgarian / Split Lunges & Deep Bodyweight Squats", "لنجز اور گہرے اسکواٹس", "12 reps x 3 sets", "Brisk", "Keep knees aligned over toes and rest 60 seconds between sets."),
                WorkoutExerciseSlot("Glute Bridges + Core Plank + Brisk Evening Walk", "گلوٹ برجز، کور پلینک اور شام کی چہل قدمی", "20 mins total", "Moderate", "Pair immediately with a protein-rich meal."),
            )
            else -> listOf(
                WorkoutExerciseSlot("Brisk Post-Meal Walk (20 mins after lunch or dinner)", "کھانے کے بعد تیز چہل قدمی", "20–30 mins", "Moderate", "Lowers post-meal blood sugar & blood pressure naturally."),
                WorkoutExerciseSlot("Wall Push-Ups & Bodyweight Squats", "دیوار کے سہارے پش اپس اور ہلکی بیٹھک", "12 reps x 2 sets", "Moderate", "Keep back straight and breathe smoothly."),
                WorkoutExerciseSlot("Cyclic Sighing + Neck/Back Mobility Stretch", "سانس کی مشق اور کمر و گردن کی کھنچاؤ والی ورزش", "5 mins", "Gentle", "Relaxes nervous system after activity."),
            )
        }

        val avoidEn = buildList {
            if (hasBp) add("Limit pickles (achaar), papad, processed snacks, and extra table salt.")
            if (hasDiabetes) add("Avoid sugary chai, packaged juices, bakery mithai, and refined white naan.")
            if (isWeightLoss) add("Skip deep-fried samosas/pakoras, sugary cold drinks, and late-night desserts.")
            if (person.allergies.isNotEmpty()) add("Strictly avoid allergens recorded on profile: ${person.allergies.joinToString(", ")}.")
            add("Stay hydrated and avoid vigorous workouts if experiencing fever, chest pain, or severe fatigue.")
        }
        val avoidUr = buildList {
            if (hasBp) add("اچار، پاپڑ، بازاری نمکین اور اضافی نمک سے پرہیز کریں۔")
            if (hasDiabetes) add("میٹھی چائے، ڈبے والے جوس، مٹھائی اور میدے کے نان سے بچیں۔")
            if (isWeightLoss) add("سموسے پکوڑے، کولڈ ڈرنکس اور رات گئے مٹھائی سے مکمل پرہیز کریں۔")
            if (person.allergies.isNotEmpty()) add("ریکارڈ شدہ الرجی سے مکمل بچیں: ${person.allergies.joinToString("، ")}۔")
            add("بخار، سینے کے درد یا شدید تھکاوٹ میں ورزش ہرگز نہ کریں۔")
        }

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        return PersonalizedWellnessPlan(
            id = "plan_${UUID.randomUUID().toString().take(8)}",
            personId = person.id,
            goal = goal,
            generatedAtIso = nowIso,
            dailyWaterGoalMl = dailyWaterGoalMl,
            dailyStepsGoal = dailyStepsGoal,
            dietMeals = dietMeals,
            workoutRoutine = workouts,
            avoidNotes_en = avoidEn,
            avoidNotes_ur = avoidUr,
            engineUsed = "OFFLINE_NUTRITION_SAFETY_CORE",
        )
    }
}
