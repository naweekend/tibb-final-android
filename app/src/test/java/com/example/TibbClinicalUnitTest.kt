package com.example

import com.example.data.local.BundledDataLoader
import com.example.data.local.VaultCrypto
import com.example.data.model.AppLocale
import com.example.data.model.BodyRegion
import com.example.data.model.EpisodeInput
import com.example.data.model.EpisodeVitals
import com.example.data.model.GeoCoords
import com.example.data.model.HydrationStatus
import com.example.data.model.PatientProfile
import com.example.data.model.PrimaryComplaint
import com.example.data.model.StayHomeVerdict
import com.example.data.model.SugarMode
import com.example.data.model.TemperatureBand
import com.example.data.model.TriageAnswers
import com.example.data.model.TriageTier
import com.example.data.model.UnitSystem
import com.example.data.model.WellnessGoal
import com.example.engine.HealthMath
import com.example.engine.HelpTopic
import com.example.engine.RemediesAndPlans
import com.example.engine.SimpleHelpCatalog
import com.example.engine.TriageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class TibbClinicalUnitTest {

    private lateinit var dataLoader: BundledDataLoader
    private lateinit var triageEngine: TriageEngine

    @Before
    fun setup() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).first { it.exists() }
        dataLoader = BundledDataLoader(
            assetOpener = { path -> File(assetsRoot, path).readText() }
        )
        triageEngine = TriageEngine(dataLoader)
    }

    @Test
    fun `bundled datasets parse completely with facilities protocols and DRAP blocklist`() {
        assertTrue(dataLoader.facilities.isNotEmpty())
        assertTrue(dataLoader.whoHomeCare.isNotEmpty())
        assertTrue(dataLoader.emergencyFirstAid.isNotEmpty())
        assertTrue(dataLoader.diseaseReference.isNotEmpty())
        assertTrue(dataLoader.drapBlocklist.antibiotics_generic.isNotEmpty())
        assertTrue(dataLoader.drapBlocklist.pakistan_brand_names.isNotEmpty())
    }

    @Test
    fun `pin format validator accepts 4-8 digits and rejects invalid inputs`() {
        assertTrue(VaultCrypto.validatePinFormat("1234"))
        assertTrue(VaultCrypto.validatePinFormat("48269012"))
        assertFalse(VaultCrypto.validatePinFormat("123"))
        assertFalse(VaultCrypto.validatePinFormat("123456789"))
        assertFalse(VaultCrypto.validatePinFormat("12ab"))
    }

    @Test
    fun `health math conversions sleep duration and readiness score behave accurately`() {
        assertEquals(70.0, HealthMath.lbsToKg(HealthMath.kgToLbs(70.0)), 0.2)
        val (ft, inch) = HealthMath.cmToFtIn(170.0)
        assertEquals(5, ft)
        assertEquals(7, inch)
        assertEquals("--", HealthMath.formatWeightDisplay(null, UnitSystem.METRIC))

        // Midnight rollover sleep: 23:00 -> 06:30 = 7.5 hours
        assertEquals(7.5, HealthMath.computeSleepDurationHours("23:00", "06:30"), 0.01)

        val breakdown = HealthMath.computeDailyReadinessScore(
            waterMl = 2250,
            waterGoalMl = 2250,
            steps = 7000,
            stepGoal = 7000,
            sleepHours = 7.5,
            activeAlarmsCount = 2,
            dueAlarmsCount = 0,
        )
        assertEquals(100, breakdown.score)
        assertEquals(100, breakdown.adherencePct)
    }

    @Test
    fun `optical ppg analyzer detects synthetic 72 bpm wave and rejects no-finger noise`() {
        val goodFrames = (0 until 400).map { idx ->
            val t = idx / 25.0
            val pulse = 4.5 * sin(2.0 * PI * 1.2 * t)
            HealthMath.PpgSampleFrame(
                tSec = t,
                r = 195.0 + pulse,
                g = 62.0 + pulse * 0.45,
                b = 35.0 + pulse * 0.15,
            )
        }
        val okResult = HealthMath.analyzePpgSamples(goodFrames)
        assertTrue("Expected Ok PPG result, got $okResult", okResult is HealthMath.PpgAnalysisOutput.Ok)
        val bpm = (okResult as HealthMath.PpgAnalysisOutput.Ok).bpm
        assertTrue("Expected BPM near 72, got $bpm", bpm in 68..76)

        val noFingerFrames = (0 until 400).map { idx ->
            HealthMath.PpgSampleFrame(idx / 25.0, r = 90.0, g = 92.0, b = 110.0)
        }
        val rejected = HealthMath.analyzePpgSamples(noFingerFrames)
        assertTrue(rejected is HealthMath.PpgAnalysisOutput.Rejected)
    }

    @Test
    fun `biomarker regex extractor parses pakistani lab report snippet`() {
        val report = """
            CBC & Metabolic Panel
            Haemoglobin: 11.4 g/dL
            WBC Count: 8,400 /uL
            Platelet Count: 215,000 /uL
            Fasting Blood Sugar: 132 mg/dL
            HbA1c: 6.8 %
            Serum Creatinine: 1.05 mg/dL
            Total Cholesterol: 194 mg/dL
            SGPT (ALT): 38 U/L
        """.trimIndent()
        val parsed = HealthMath.extractBiomarkers(report)
        assertEquals("11.4", parsed.hb)
        assertEquals("8400", parsed.wbc)
        assertEquals("215000", parsed.plt)
        assertEquals("132", parsed.fbs)
        assertEquals("6.8", parsed.hba1c)
        assertEquals("1.05", parsed.creatinine)
        assertEquals("194", parsed.cholesterol)
        assertEquals("38", parsed.altSgpt)
    }

    @Test
    fun `triage engine detects RED chest pain suppresses home care and enforces DRAP blocklist`() {
        val adult = PatientProfile(id = "p1", name = "Tariq", ageMonths = 540, sex = "M")
        val redResult = triageEngine.evaluateCareEpisode(
            EpisodeInput(
                patient = adult,
                bodyRegion = BodyRegion.CHEST,
                durationHours = 2,
                temperatureBand = TemperatureBand.NORMAL,
                hydrationStatus = HydrationStatus.DRINKING_WELL,
                vitals = EpisodeVitals(bpSystolic = 150, bpDiastolic = 95),
                answers = TriageAnswers(crushingChestPain = true, primaryComplaint = PrimaryComplaint.CHRONIC_FLARE),
                userCoords = GeoCoords(31.5204, 74.3587),
            )
        )
        assertEquals(TriageTier.RED, redResult.tier)
        assertNull(redResult.matchedHomeCare)
        assertNotNull(redResult.emergencyFirstAid)
        assertTrue(redResult.recommendedFacilities.isNotEmpty())
        assertFalse(redResult.locationRequired)

        // Infant under 12m cough must strip honey guidance
        val infant = PatientProfile(id = "p_inf", name = "Baby Zain", ageMonths = 8, sex = "M")
        val infantGreen = triageEngine.evaluateCareEpisode(
            EpisodeInput(
                patient = infant,
                bodyRegion = BodyRegion.HEAD_NECK,
                durationHours = 12,
                temperatureBand = TemperatureBand.NORMAL,
                hydrationStatus = HydrationStatus.DRINKING_WELL,
                vitals = EpisodeVitals(respiratoryRate = 34),
                answers = TriageAnswers(primaryComplaint = PrimaryComplaint.COUGH_COLD),
                district = "Lahore",
            )
        )
        assertEquals(TriageTier.GREEN, infantGreen.tier)
        val stepsEn = infantGreen.matchedHomeCare?.steps_en.orEmpty()
        assertTrue(stepsEn.none { it.contains("honey", ignoreCase = true) })

        // DRAP antimicrobial & dosage guardrails
        val unsafeAdvice = triageEngine.verifyAgainstClinicalGroundTruth(
            rawText = "Take Augmentin 625 mg twice a day and honey for cough.",
            ageMonths = 8,
            homeCare = infantGreen.matchedHomeCare,
            diseaseRef = infantGreen.matchedDiseaseRef,
            triageTier = TriageTier.GREEN,
        )
        assertFalse(unsafeAdvice.passed)
        assertTrue(unsafeAdvice.amrBlocked)
        assertTrue(unsafeAdvice.violations.any { it.contains("INFANT_HONEY") })
    }

    @Test
    fun `stay home vs 1122 guidance distinguishes conscious 15-15 hypoglycemia vs hypertensive organ crisis`() {
        val mildHypo = triageEngine.evaluateStayHomeVsHospital(
            bpSystolic = 115,
            bpDiastolic = 75,
            bloodGlucoseMgDl = 64,
            isConsciousAbleToSwallow = true,
        )
        assertEquals(StayHomeVerdict.STAY_HOME, mildHypo.verdict)

        val crisisOrgan = triageEngine.evaluateStayHomeVsHospital(
            bpSystolic = 192,
            bpDiastolic = 122,
            bloodGlucoseMgDl = 110,
            hasOrganWarningSigns = true,
        )
        assertEquals(StayHomeVerdict.EMERGENCY_1122, crisisOrgan.verdict)

        val sugarInterp = triageEngine.interpretBloodSugar(135, SugarMode.FASTING)
        assertEquals("HIGH", sugarInterp.status)
    }

    @Test
    fun `remedies and plans computes south asian bmi and diabetes safe wellness plan`() {
        val bmi = RemediesAndPlans.computeBmiAssessment(weightKg = 84.0, heightCm = 172.0, ageMonths = 420)
        assertEquals("OBESE", bmi.category)
        assertEquals(WellnessGoal.WEIGHT_LOSS, bmi.recommendedGoal)

        val profile = PatientProfile(
            id = "p_fatima",
            name = "Fatima",
            ageMonths = 480,
            sex = "F",
            weightKg = 74.0,
            heightCm = 162.0,
            chronicConditions = listOf("DIABETES_TYPE_2"),
            dietaryPreferences = listOf("vegetarian"),
        )
        val plan = RemediesAndPlans.buildPersonalizedPlanOffline(profile, WellnessGoal.DIABETES_CONTROL)
        assertEquals(4, plan.dietMeals.size)
        assertTrue(plan.workoutRoutine.isNotEmpty())
        assertTrue(plan.avoidNotes_en.any { it.contains("sugary", ignoreCase = true) || it.contains("sweet", ignoreCase = true) })
    }

    @Test
    fun `simple help catalog covers every feature topic in english urdu and roman with plain language`() {
        val requiredTopics = HelpTopic.entries.filter { it != HelpTopic.FULL_GUIDE }
        AppLocale.entries.forEach { loc ->
            val entries = SimpleHelpCatalog.getAllEntries(loc)
            assertEquals("Expected all 26 topics for $loc", requiredTopics.size, entries.size)
            requiredTopics.forEach { topic ->
                val entry = SimpleHelpCatalog.getEntry(topic, loc)
                assertEquals(topic, entry.topic)
                assertTrue("Empty badge for $topic in $loc", entry.badge.isNotBlank())
                assertTrue("Empty title for $topic in $loc", entry.title.isNotBlank())
                assertTrue("Empty summary for $topic in $loc", entry.simpleSummary.isNotBlank())
                assertTrue("Empty howToUse for $topic in $loc", entry.howToUse.isNotBlank())
                assertTrue("Empty safetyOrTip for $topic in $loc", entry.safetyOrTip.isNotBlank())
            }
            val bhuHelp = SimpleHelpCatalog.getEntry(HelpTopic.AUTH_VAULT, loc)
            assertTrue("BHU must be explained plainly for $loc", bhuHelp.simpleSummary.contains("Basic Health Unit", ignoreCase = true) || bhuHelp.simpleSummary.contains("بنیادی مرکزِ صحت"))
            val ocrHelp = SimpleHelpCatalog.getEntry(HelpTopic.LAB_OCR, loc)
            assertTrue("OCR must be explained plainly for $loc", ocrHelp.simpleSummary.contains("OCR"))
        }
    }
}
