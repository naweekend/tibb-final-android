package com.example

import com.example.data.local.BundledDataLoader
import com.example.data.model.AppLocale
import com.example.data.model.AssessmentOutcome
import com.example.data.model.EmergencyCategory
import com.example.data.model.EpisodeVitals
import com.example.data.model.FollowUpDimension
import com.example.data.model.GeoCoords
import com.example.data.model.LocationCategory
import com.example.data.model.PakistanProvince
import com.example.data.model.PatientProfile
import com.example.data.model.StructuredAssessmentInput
import com.example.data.model.TriageTier
import com.example.engine.LocalMedicalGuidanceEngine
import com.example.engine.TriageEngine
import com.example.engine.TrilingualStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OfflineMedicalGuidanceTest {

    private lateinit var dataLoader: BundledDataLoader
    private lateinit var triageEngine: TriageEngine
    private lateinit var engine: LocalMedicalGuidanceEngine

    private val adultMale = PatientProfile(
        id = "p_adult_m",
        name = "Hamza",
        ageMonths = 420,
        sex = "M",
    )

    private val adultPregnantFemale = PatientProfile(
        id = "p_preg",
        name = "Ayesha",
        ageMonths = 336,
        sex = "F",
        isPregnantOrLactating = true,
    )

    private val infant6Months = PatientProfile(
        id = "p_infant_6m",
        name = "Baby Sara",
        ageMonths = 6,
        sex = "F",
    )

    private val newborn1Month = PatientProfile(
        id = "p_newborn_1m",
        name = "Baby Bilal",
        ageMonths = 1,
        sex = "M",
    )

    private val olderAdult70 = PatientProfile(
        id = "p_elderly",
        name = "Dada Tariq",
        ageMonths = 840,
        sex = "M",
    )

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
        engine = LocalMedicalGuidanceEngine(dataLoader, triageEngine)
    }

    @Test
    fun `offline medical knowledge base meets completeness and source requirements`() {
        val meta = engine.getKnowledgeMetadata()
        assertTrue(meta.version.isNotBlank())
        assertTrue(meta.lastReviewedDate.isNotBlank())
        assertTrue(meta.authoritativeSources.size >= 4)
        assertTrue(meta.coveredDomainsEn.size >= 8)
        assertTrue(meta.limitationsEn.size >= 4)
        assertTrue(meta.medicalDisclaimerEn.contains("never replaces", ignoreCase = true))

        assertEquals(16, dataLoader.symptomTaxonomy.categories.size)
        assertEquals(16, dataLoader.emergencyRules.size)
        assertEquals(16, dataLoader.emergencyFirstAid.size)
        assertTrue("Expected at least 50 conditions", engine.getAllConditions().size >= 50)
        assertEquals(63, engine.getAllConditions().size)

        val coveredDimensions = dataLoader.symptomTaxonomy.followUpQuestions.map { it.dimension }.toSet()
        assertEquals(FollowUpDimension.entries.toSet(), coveredDimensions)

        engine.getAllConditions().forEach { cond ->
            assertTrue("Empty sources for ${cond.id}", cond.sources.isNotEmpty())
            assertTrue("Empty lastReviewedDate for ${cond.id}", cond.lastReviewedDate.isNotBlank())
            assertTrue("Empty whenToConsultDoctorEn for ${cond.id}", cond.whenToConsultDoctorEn.isNotBlank())
            assertTrue("Empty thingsToAvoidEn for ${cond.id}", cond.thingsToAvoidEn.isNotEmpty())
            assertTrue(
                "Condition ${cond.id} must provide safeHomeCareEn or explicit noHomeCareReasonEn",
                cond.safeHomeCareEn.isNotEmpty() || !cond.noHomeCareReasonEn.isNullOrBlank(),
            )
        }
    }

    @Test
    fun `emergency triage runs first and overrides mild symptoms across all 16 emergency categories`() {
        EmergencyCategory.entries.forEach { cat ->
            val patient = if (cat == EmergencyCategory.MATERNAL_EMERGENCY) adultPregnantFemale else adultMale
            val res = engine.evaluateGuidance(
                StructuredAssessmentInput(
                    patient = patient,
                    categoryId = "cat_respiratory",
                    selectedSymptomIds = setOf("sym_mild_hives", "sym_runny_blocked_nose"),
                    selectedOptionIdsByQuestion = mapOf(
                        "q_severity" to setOf("opt_sev_mild"),
                        "q_duration" to setOf("opt_dur_1_2d"),
                    ),
                    directEmergencyCategory = cat,
                )
            )
            assertEquals("Expected EMERGENCY_OVERRIDE for $cat", AssessmentOutcome.EMERGENCY_OVERRIDE, res.outcome)
            assertEquals(TriageTier.RED, res.triageTier)
            assertTrue("Home care conditions must be suppressed for $cat", res.matchedConditions.isEmpty())
            assertNotNull("Emergency first aid required for $cat", res.emergencyFirstAid)
            assertTrue(res.triggeredEmergencyRules.any { it.category == cat })
        }

        // Combined mild + red-flag symptom selection -> EMERGENCY_OVERRIDE
        val anaphylaxisMixed = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_allergy",
                selectedSymptomIds = setOf("sym_mild_hives", "sym_anaphylaxis_swelling"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.EMERGENCY_OVERRIDE, anaphylaxisMixed.outcome)
        assertEquals(TriageTier.RED, anaphylaxisMixed.triageTier)
        assertTrue(anaphylaxisMixed.matchedConditions.isEmpty())
        assertEquals(EmergencyCategory.ANAPHYLAXIS, anaphylaxisMixed.triggeredEmergencyRules.first().category)
    }

    @Test
    fun `critical vitals and newborn under 2 months fever trigger immediate emergency override`() {
        val newbornFever = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = newborn1Month,
                categoryId = "cat_fever",
                selectedSymptomIds = setOf("sym_mild_fever"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.EMERGENCY_OVERRIDE, newbornFever.outcome)
        assertEquals(EmergencyCategory.PEDIATRIC_DANGER, newbornFever.triggeredEmergencyRules.first().category)

        val fastBreathing = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = infant6Months,
                categoryId = "cat_respiratory",
                selectedSymptomIds = setOf("sym_cough_dry_wet"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_1_2d"),
                ),
                vitals = EpisodeVitals(respiratoryRate = 54), // >= 50/min IMCI threshold for 2-11m
            )
        )
        assertEquals(AssessmentOutcome.EMERGENCY_OVERRIDE, fastBreathing.outcome)
        assertEquals(EmergencyCategory.BREATHING_DISTRESS, fastBreathing.triggeredEmergencyRules.first().category)

        val hypertensiveStrokeRisk = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_neuro",
                selectedSymptomIds = setOf("sym_band_headache"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
                vitals = EpisodeVitals(bpSystolic = 190, bpDiastolic = 122),
            )
        )
        assertEquals(AssessmentOutcome.EMERGENCY_OVERRIDE, hypertensiveStrokeRisk.outcome)
        assertEquals(TriageTier.RED, hypertensiveStrokeRisk.triageTier)
    }

    @Test
    fun `conditional follow-up questions and categories adapt by age pregnancy and domain`() {
        val maleCats = engine.visibleCategories(adultMale).map { it.id }
        assertFalse(maleCats.contains("cat_pregnancy"))
        assertFalse(maleCats.contains("cat_elderly"))
        assertFalse(maleCats.contains("cat_pediatric"))

        val pregCats = engine.visibleCategories(adultPregnantFemale).map { it.id }
        assertTrue(pregCats.contains("cat_pregnancy"))

        val elderlyCats = engine.visibleCategories(olderAdult70).map { it.id }
        assertTrue(elderlyCats.contains("cat_elderly"))

        val neuroQuestions = engine.visibleFollowUpQuestions("cat_neuro", setOf("sym_band_headache"), adultMale).map { it.id }
        assertTrue(neuroQuestions.contains("q_location_head"))
        assertFalse(neuroQuestions.contains("q_location_chest_abdomen"))

        val giQuestions = engine.visibleFollowUpQuestions("cat_digestive", setOf("sym_watery_diarrhea"), adultMale).map { it.id }
        assertTrue(giQuestions.contains("q_location_chest_abdomen"))
        assertFalse(giQuestions.contains("q_location_head"))
    }

    @Test
    fun `conflicting or incomplete inputs yield INVALID_OR_CONFLICTING without forced results`() {
        val missingRequired = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_respiratory",
                selectedSymptomIds = setOf("sym_cough_dry_wet"),
                selectedOptionIdsByQuestion = emptyMap(),
            )
        )
        assertEquals(AssessmentOutcome.INVALID_OR_CONFLICTING, missingRequired.outcome)
        assertTrue(missingRequired.validationErrorsEn.size >= 2)
        assertTrue(missingRequired.matchedConditions.isEmpty())

        val diarrheaAndConstipation = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_digestive",
                selectedSymptomIds = setOf("sym_watery_diarrhea", "sym_constipation"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_1_2d"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.INVALID_OR_CONFLICTING, diarrheaAndConstipation.outcome)
        assertTrue(diarrheaAndConstipation.validationErrorsEn.any { it.contains("Conflicting", ignoreCase = true) })

        val cough14dDurationConflict = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_infectious",
                selectedSymptomIds = setOf("sym_prolonged_cough_14d"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.INVALID_OR_CONFLICTING, cough14dDurationConflict.outcome)

        val malePregnancyConflict = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_pregnancy",
                selectedSymptomIds = setOf("sym_pregnancy_mild_nausea"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_1_2d"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.INVALID_OR_CONFLICTING, malePregnancyConflict.outcome)
    }

    @Test
    fun `unknown or insufficient combinations gracefully return INSUFFICIENT_OR_UNKNOWN`() {
        val isolatedFatigue = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_fever",
                selectedSymptomIds = setOf("sym_extreme_fatigue"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_1_2d"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.INSUFFICIENT_OR_UNKNOWN, isolatedFatigue.outcome)
        assertEquals(TriageTier.AMBER, isolatedFatigue.triageTier)
        assertTrue(isolatedFatigue.matchedConditions.isEmpty())
        assertFalse(isolatedFatigue.uncertaintyExplanationEn.isNullOrBlank())

        val isolatedVomiting = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_digestive",
                selectedSymptomIds = setOf("sym_nausea_vomiting"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.INSUFFICIENT_OR_UNKNOWN, isolatedVomiting.outcome)
        assertTrue(isolatedVomiting.matchedConditions.isEmpty())
    }

    @Test
    fun `matched guidance uses non-diagnostic probabilistic phrasing strips infant honey and flags unsafe home care`() {
        // 1. Infant 6 months cough -> honey advice MUST be stripped
        val infantCold = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = infant6Months,
                categoryId = "cat_respiratory",
                selectedSymptomIds = setOf("sym_cough_dry_wet", "sym_runny_blocked_nose"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_mild"),
                    "q_duration" to setOf("opt_dur_1_2d"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.CONDITION_MATCHED, infantCold.outcome)
        val topCold = infantCold.matchedConditions.first()
        assertEquals("cond_viral_uri", topCold.condition.id)
        assertTrue(topCold.probabilisticHeaderEn.contains("may be associated with", ignoreCase = true))
        assertFalse(topCold.probabilisticHeaderEn.contains("diagnosis confirmed", ignoreCase = true))
        assertTrue(topCold.condition.safeHomeCareEn.none { it.contains("honey", ignoreCase = true) })
        assertNotNull(topCold.ageOrPregnancyAlertEn)

        // 2. Dengue suspect -> requires clinic review (AMBER) + warns against ibuprofen/aspirin
        val dengueSuspect = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_infectious",
                selectedSymptomIds = setOf("sym_retro_orbital_joint_pain", "sym_high_fever"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_moderate"),
                    "q_duration" to setOf("opt_dur_3_7d"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.CONDITION_MATCHED, dengueSuspect.outcome)
        assertEquals(TriageTier.AMBER, dengueSuspect.triageTier)
        val dengue = dengueSuspect.matchedConditions.first { it.condition.id == "cond_dengue_suspect" }
        assertTrue(dengue.condition.thingsToAvoidEn.any { it.contains("Ibuprofen", ignoreCase = true) || it.contains("Aspirin", ignoreCase = true) })

        // 3. Animal bite -> home remedies explicitly inappropriate (`noHomeCareReasonEn` present)
        val animalBite = engine.evaluateGuidance(
            StructuredAssessmentInput(
                patient = adultMale,
                categoryId = "cat_injuries",
                selectedSymptomIds = setOf("sym_animal_bite"),
                selectedOptionIdsByQuestion = mapOf(
                    "q_severity" to setOf("opt_sev_moderate"),
                    "q_duration" to setOf("opt_dur_under24h"),
                ),
            )
        )
        assertEquals(AssessmentOutcome.CONDITION_MATCHED, animalBite.outcome)
        assertEquals(TriageTier.AMBER, animalBite.triageTier)
        val biteMatch = animalBite.matchedConditions.first { it.condition.id == "cond_animal_bite_rabies_risk" }
        assertFalse(biteMatch.condition.noHomeCareReasonEn.isNullOrBlank())
        assertTrue(biteMatch.condition.appropriateFirstAidEn.isNotEmpty())
        assertTrue(biteMatch.condition.safeHomeCareEn.isEmpty())

        // 4. DRAP antimicrobial compliance check across all 36 condition safeHomeCare entries
        engine.getAllConditions().forEach { cond ->
            cond.safeHomeCareEn.forEach { step ->
                val audit = triageEngine.verifyAgainstClinicalGroundTruth(
                    rawText = step,
                    ageMonths = 300,
                    homeCare = null,
                    diseaseRef = dataLoader.diseaseReference.first(),
                    triageTier = TriageTier.GREEN,
                )
                assertFalse("DRAP antibiotic leak in ${cond.id}: $step", audit.amrBlocked)
            }
        }
    }

    @Test
    fun `searchConditions finds expected conditions offline`() {
        val asthma = engine.searchConditions("asthma")
        assertTrue(asthma.any { it.id == "cond_asthma_flare" })

        val cardio = engine.searchConditions("", "cat_cardio")
        assertTrue(cardio.isNotEmpty())
        assertTrue(cardio.all { it.categoryId == "cat_cardio" })
    }

    @Test
    fun `four colour-coded urgency classifications cover RED YELLOW GREEN BLUE across locales`() {
        assertEquals(listOf("RED", "YELLOW", "GREEN", "BLUE"), TriageTier.entries.map { it.displayCode })

        assertEquals("RED (Emergency)", TriageTier.RED.shortLabel(AppLocale.EN))
        assertEquals("YELLOW (Urgent)", TriageTier.AMBER.shortLabel(AppLocale.EN))
        assertEquals("GREEN (Home Care)", TriageTier.GREEN.shortLabel(AppLocale.EN))
        assertEquals("BLUE (Wellness)", TriageTier.BLUE.shortLabel(AppLocale.EN))

        assertTrue(TriageTier.RED.subtitle(AppLocale.EN).contains("1122", ignoreCase = true))
        assertTrue(TriageTier.AMBER.subtitle(AppLocale.EN).contains("BHU", ignoreCase = true))
        assertTrue(TriageTier.GREEN.subtitle(AppLocale.EN).contains("ORS", ignoreCase = true))
        assertTrue(TriageTier.BLUE.subtitle(AppLocale.EN).contains("nutrition", ignoreCase = true))

        AppLocale.entries.forEach { locale ->
            val s = TrilingualStrings.get(locale)
            assertTrue(s.redBannerLabel.contains("RED"))
            assertTrue(s.amberBannerLabel.contains("YELLOW"))
            assertTrue(s.greenBannerLabel.contains("GREEN"))
            assertTrue(s.blueWellnessLabel.contains("BLUE"))
            TriageTier.entries.forEach { tier ->
                assertTrue(tier.shortLabel(locale).isNotBlank())
                assertTrue(tier.subtitle(locale).isNotBlank())
            }
        }
    }

    @Test
    fun `every one of the 63 conditions matches cleanly through engine evaluation`() {
        val validCatIds = dataLoader.symptomTaxonomy.categories.map { it.id }.toSet()
        val allSymsById = dataLoader.symptomTaxonomy.categories.flatMap { it.symptoms }.associateBy { it.id }

        engine.getAllConditions().forEach { cond ->
            assertTrue("Missing subcategoryEn on ${cond.id}", cond.subcategoryEn.isNotBlank())
            assertTrue("Missing subcategoryUr on ${cond.id}", cond.subcategoryUr.isNotBlank())
            assertTrue("Unknown categoryId on ${cond.id}", cond.categoryId in validCatIds)

            val crit = cond.matchingCriteria
            val patient = when {
                crit.pregnantOnly -> adultPregnantFemale
                crit.maxAgeMonths != null && crit.maxAgeMonths < 180 -> infant6Months
                crit.minAgeMonths != null && crit.minAgeMonths >= 600 -> olderAdult70
                else -> adultMale
            }

            // Pick non-red-flag required + supporting symptoms up to minScore
            val pickedSymptoms = mutableSetOf<String>()
            crit.requiredAnySymptomIds.firstOrNull { allSymsById[it]?.isRedFlagSymptom != true }?.let {
                pickedSymptoms.add(it)
            }
            var accumScore = pickedSymptoms.sumOf { crit.supportingSymptomWeights[it] ?: 0 }
            crit.supportingSymptomWeights.entries
                .sortedByDescending { it.value }
                .forEach { (symId, w) ->
                    if (accumScore < crit.minScore &&
                        symId !in pickedSymptoms &&
                        symId !in crit.excludedSymptomIds &&
                        allSymsById[symId]?.isRedFlagSymptom != true &&
                        !(symId == "sym_high_fever" && "sym_mild_fever" in pickedSymptoms) &&
                        !(symId == "sym_mild_fever" && "sym_high_fever" in pickedSymptoms)
                    ) {
                        pickedSymptoms.add(symId)
                        accumScore += w
                    }
                }

            val durOpt = if ("sym_prolonged_cough_14d" in pickedSymptoms) "opt_dur_over7d" else "opt_dur_3_7d"
            val sevOpt = if ("opt_sev_moderate" !in crit.excludedOptionIds) "opt_sev_moderate" else "opt_sev_mild"
            val extraOptions = mutableMapOf(
                "q_severity" to setOf(sevOpt),
                "q_duration" to setOf(durOpt),
            )
            // Add top non-emergency supportingOptionWeights if condition score relies on follow-up detail (e.g. RLQ abdominal pain)
            val allOptionsById = dataLoader.symptomTaxonomy.followUpQuestions
                .flatMap { q -> q.options.map { opt -> opt.id to (q.id to opt) } }
                .toMap()
            crit.supportingOptionWeights.entries
                .sortedByDescending { it.value }
                .forEach { (optId, _) ->
                    val pair = allOptionsById[optId]
                    if (pair != null &&
                        !pair.second.isEmergencyTrigger &&
                        optId !in crit.excludedOptionIds &&
                        pair.first != "q_severity" &&
                        pair.first != "q_duration"
                    ) {
                        val existing = extraOptions[pair.first]?.toMutableSet() ?: mutableSetOf()
                        existing.add(optId)
                        extraOptions[pair.first] = existing
                    }
                }

            val res = engine.evaluateGuidance(
                StructuredAssessmentInput(
                    patient = patient,
                    categoryId = cond.categoryId,
                    selectedSymptomIds = pickedSymptoms,
                    selectedOptionIdsByQuestion = extraOptions,
                )
            )
            assertEquals("Condition ${cond.id} should evaluate to CONDITION_MATCHED (was ${res.outcome})", AssessmentOutcome.CONDITION_MATCHED, res.outcome)
            assertTrue("Starter helper non-empty for ${cond.id}", engine.getStarterSymptomsForCondition(cond).isNotEmpty())
        }
    }

    @Test
    fun `pakistan locations dataset has at least 400 unique places across all provinces categories and routes nearest hospitals`() {
        val locations = triageEngine.getPakistanLocations()
        assertTrue("Expected at least 400 Pakistan locations, found ${locations.size}", locations.size >= 400)
        assertEquals(locations.size, locations.map { it.id }.distinct().size)

        // All 7 provinces/territories & 7 categories represented
        assertEquals(PakistanProvince.entries.toSet(), locations.map { it.province }.toSet())
        assertEquals(LocationCategory.entries.toSet(), locations.map { it.category }.toSet())

        // Geographic bounds inside Pakistan
        locations.forEach { loc ->
            assertTrue("Lat out of bounds for ${loc.id}: ${loc.lat}", loc.lat in 23.4..37.2)
            assertTrue("Lng out of bounds for ${loc.id}: ${loc.lng}", loc.lng in 60.8..77.9)
            assertTrue("Empty nameEn for ${loc.id}", loc.nameEn.isNotBlank())
            assertTrue("Empty nameUr for ${loc.id}", loc.nameUr.isNotBlank())
            assertTrue("Empty district for ${loc.id}", loc.district.isNotBlank())
        }

        // Province & category filtering + text search across Pakistan
        val skardu = triageEngine.searchPakistanLocations("Skardu", province = PakistanProvince.GILGIT_BALTISTAN)
        assertTrue(skardu.any { it.nameEn.contains("Skardu", ignoreCase = true) })

        val gwadar = triageEngine.searchPakistanLocations("Gwadar", province = PakistanProvince.BALOCHISTAN)
        assertTrue(gwadar.isNotEmpty())

        val muzaffarabad = triageEngine.searchPakistanLocations("Muzaffarabad", province = PakistanProvince.AZAD_KASHMIR)
        assertTrue(muzaffarabad.isNotEmpty())

        val landmarks = triageEngine.searchPakistanLocations("", category = LocationCategory.LANDMARK)
        assertTrue(landmarks.size >= 15)

        // Reverse nearest-location lookup from Lahore coords
        val nearestToLahore = triageEngine.findNearestPakistanLocation(31.5204, 74.3587)
        assertNotNull(nearestToLahore)
        assertEquals("Lahore", nearestToLahore!!.district)

        // Facility routing works cleanly for remote towns / custom pins across all 7 provinces
        listOf(
            "Skardu",
            "Gwadar",
            "Muzaffarabad",
            "Mingora",
            "Islamabad",
            "Sukkur",
            "Bahawalpur",
        ).forEach { city ->
            val loc = triageEngine.searchPakistanLocations(city).first()
            val (facs, locRequired) = triageEngine.sortFacilities(
                tier = TriageTier.AMBER,
                isPediatric = false,
                userCoords = GeoCoords(loc.lat, loc.lng),
                districtFilter = loc.district,
            )
            assertFalse("Location should not be flagged missing for $city", locRequired)
            assertTrue("Facilities should resolve for $city", facs.isNotEmpty())
            assertNotNull("Distance should compute for $city", facs.first().distanceKm)
        }
    }
}
