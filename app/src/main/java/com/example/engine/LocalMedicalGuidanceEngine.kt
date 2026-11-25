package com.example.engine

import com.example.data.local.BundledDataLoader
import com.example.data.model.AssessmentOutcome
import com.example.data.model.BodyRegion
import com.example.data.model.ConditionSeverity
import com.example.data.model.EmergencyCategory
import com.example.data.model.EmergencyRule
import com.example.data.model.EpisodeInput
import com.example.data.model.FollowUpOption
import com.example.data.model.FollowUpQuestion
import com.example.data.model.HydrationStatus
import com.example.data.model.MatchedConditionResult
import com.example.data.model.MedicalConditionEntry
import com.example.data.model.MedicalGuidanceAssessment
import com.example.data.model.PatientProfile
import com.example.data.model.PrimaryComplaint
import com.example.data.model.ReferralSlip
import com.example.data.model.StructuredAssessmentInput
import com.example.data.model.SymptomCategoryNode
import com.example.data.model.SymptomItem
import com.example.data.model.TemperatureBand
import com.example.data.model.TriageAnswers
import com.example.data.model.TriageEvaluationResult
import com.example.data.model.TriageTier
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LocalMedicalGuidanceEngine(
    private val data: BundledDataLoader,
    private val triageEngine: TriageEngine = TriageEngine(data),
) {
    private val secureRandom = SecureRandom()

    private val symptomById: Map<String, SymptomItem> by lazy {
        data.symptomTaxonomy.categories
            .flatMap { it.symptoms }
            .associateBy { it.id }
    }

    private val optionById: Map<String, FollowUpOption> by lazy {
        data.symptomTaxonomy.followUpQuestions
            .flatMap { it.options }
            .associateBy { it.id }
    }

    fun getKnowledgeMetadata() = data.knowledgeMetadata

    fun getAllConditions(): List<MedicalConditionEntry> = data.medicalConditions

    fun searchConditions(query: String, categoryId: String? = null): List<MedicalConditionEntry> {
        val clean = query.trim().lowercase()
        return data.medicalConditions.filter { cond ->
            val catOk = categoryId.isNullOrBlank() || cond.categoryId == categoryId
            if (!catOk) return@filter false
            if (clean.isEmpty()) return@filter true
            cond.nameEn.contains(clean, ignoreCase = true) ||
                cond.nameUr.contains(clean, ignoreCase = true) ||
                cond.categoryLabelEn.contains(clean, ignoreCase = true) ||
                cond.categoryLabelUr.contains(clean, ignoreCase = true) ||
                cond.subcategoryEn.contains(clean, ignoreCase = true) ||
                cond.subcategoryUr.contains(clean, ignoreCase = true) ||
                cond.shortDescriptionEn.contains(clean, ignoreCase = true) ||
                cond.shortDescriptionUr.contains(clean, ignoreCase = true) ||
                cond.commonSymptomsEn.any { it.contains(clean, ignoreCase = true) } ||
                cond.commonSymptomsUr.any { it.contains(clean, ignoreCase = true) } ||
                cond.associatedSymptomsEn.any { it.contains(clean, ignoreCase = true) } ||
                cond.associatedSymptomsUr.any { it.contains(clean, ignoreCase = true) }
        }
    }

    fun getStarterSymptomsForCondition(cond: MedicalConditionEntry): Set<String> {
        val required = cond.matchingCriteria.requiredAnySymptomIds.take(1)
        val topSupported = cond.matchingCriteria.supportingSymptomWeights.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filterNot { symptomById[it]?.isRedFlagSymptom == true }
            .take(2)
        return (required + topSupported).toSet()
    }

    fun visibleCategories(patient: PatientProfile): List<SymptomCategoryNode> {
        return data.symptomTaxonomy.categories.filter { cat ->
            val minOk = cat.minAgeMonths == null || patient.ageMonths >= cat.minAgeMonths
            val maxOk = cat.maxAgeMonths == null || patient.ageMonths <= cat.maxAgeMonths
            val pregOk = !cat.pregnantOnly || patient.isPregnantOrLactating
            minOk && maxOk && pregOk
        }
    }

    fun visibleSymptoms(category: SymptomCategoryNode, patient: PatientProfile): List<SymptomItem> {
        return category.symptoms.filter { sym ->
            val minOk = sym.minAgeMonths == null || patient.ageMonths >= sym.minAgeMonths
            val maxOk = sym.maxAgeMonths == null || patient.ageMonths <= sym.maxAgeMonths
            val pregOk = !sym.pregnantOnly || patient.isPregnantOrLactating
            minOk && maxOk && pregOk
        }
    }

    fun visibleFollowUpQuestions(
        categoryId: String,
        selectedSymptomIds: Set<String>,
        patient: PatientProfile,
    ): List<FollowUpQuestion> {
        return data.symptomTaxonomy.followUpQuestions.filter { q ->
            val catOk = q.showForCategories.isEmpty() || categoryId in q.showForCategories
            val symOk = q.showForSymptoms.isEmpty() || selectedSymptomIds.any { it in q.showForSymptoms }
            val minOk = q.minAgeMonths == null || patient.ageMonths >= q.minAgeMonths
            val maxOk = q.maxAgeMonths == null || patient.ageMonths <= q.maxAgeMonths
            val pregOk = !q.pregnantOnly || patient.isPregnantOrLactating
            catOk && symOk && minOk && maxOk && pregOk
        }
    }

    fun checkEmergencyTriggers(input: StructuredAssessmentInput): List<EmergencyRule> {
        val allOptions = input.selectedOptionIdsByQuestion.values.flatten().toSet()
        val matched = mutableListOf<EmergencyRule>()

        // 1. Direct emergency bypass selection
        if (input.directEmergencyCategory != null) {
            val matchingRule = data.emergencyRules.firstOrNull { it.category == input.directEmergencyCategory }
            if (matchingRule != null) {
                matched.add(matchingRule)
            } else {
                val fa = triageEngine.getEmergencyFirstAid(input.directEmergencyCategory)
                matched.add(
                    EmergencyRule(
                        id = "rule_direct_${input.directEmergencyCategory.name.lowercase()}",
                        priority = 1,
                        category = input.directEmergencyCategory,
                        titleEn = fa.title_en,
                        titleUr = fa.title_ur,
                        summaryEn = fa.do_now_en.firstOrNull() ?: fa.title_en,
                        summaryUr = fa.do_now_ur.firstOrNull() ?: fa.title_ur,
                        sourceCitation = fa.sourceCitation,
                        lastReviewedDate = data.knowledgeMetadata.lastReviewedDate,
                    )
                )
            }
        }

        // 2. Critical vitals checks (always evaluated first)
        val vitals = input.vitals
        val rrThreshold = triageEngine.getFastBreathingThreshold(input.patient.ageMonths)
        if (vitals.respiratoryRate != null && vitals.respiratoryRate >= rrThreshold) {
            data.emergencyRules.firstOrNull { it.category == EmergencyCategory.BREATHING_DISTRESS }
                ?.let { rule ->
                    matched.add(
                        rule.copy(
                            summaryEn = "Fast breathing detected (${vitals.respiratoryRate}/min >= $rrThreshold/min threshold for age).",
                            summaryUr = "تیز سانس کی علامت (${vitals.respiratoryRate} فی منٹ)۔",
                        )
                    )
                }
        }
        if ((vitals.bpSystolic != null && vitals.bpSystolic >= 180) ||
            (vitals.bpDiastolic != null && vitals.bpDiastolic >= 120)
        ) {
            data.emergencyRules.firstOrNull { it.category == EmergencyCategory.STROKE_FAST }?.let {
                matched.add(
                    it.copy(
                        summaryEn = "Critically elevated blood pressure (${vitals.bpSystolic ?: "--"}/${vitals.bpDiastolic ?: "--"} mmHg).",
                        summaryUr = "انتہائی خطرناک ہائی بلڈ پریشر (${vitals.bpSystolic ?: "--"}/${vitals.bpDiastolic ?: "--"})۔",
                    )
                )
            }
        }
        if (vitals.bpSystolic != null && vitals.bpSystolic < 80) {
            data.emergencyRules.firstOrNull { it.category == EmergencyCategory.UNCONSCIOUS_FAINTING }?.let {
                matched.add(
                    it.copy(
                        summaryEn = "Critically low systolic blood pressure (${vitals.bpSystolic} mmHg) — shock warning.",
                        summaryUr = "انتہائی کم بلڈ پریشر (${vitals.bpSystolic}) — فوری ہسپتال جائیں۔",
                    )
                )
            }
        }
        if (vitals.bloodGlucoseMgDl != null && (vitals.bloodGlucoseMgDl < 54 || vitals.bloodGlucoseMgDl >= 400)) {
            val cat = if (vitals.bloodGlucoseMgDl < 54) {
                EmergencyCategory.UNCONSCIOUS_FAINTING
            } else {
                EmergencyCategory.SEVERE_DEHYDRATION
            }
            data.emergencyRules.firstOrNull { it.category == cat }?.let {
                matched.add(
                    it.copy(
                        summaryEn = "Critical blood sugar reading (${vitals.bloodGlucoseMgDl} mg/dL).",
                        summaryUr = "شوگر کی انتہائی خطرناک سطح (${vitals.bloodGlucoseMgDl} mg/dL)۔",
                    )
                )
            }
        }

        // 3. Infant < 2 months with any fever indicator
        val hasFeverInput = input.selectedSymptomIds.any {
            it == "sym_mild_fever" || it == "sym_high_fever" || it == "sym_infant_under2m_fever"
        } || "opt_assoc_fever" in allOptions
        if (input.patient.ageMonths < 2 && hasFeverInput) {
            data.emergencyRules.firstOrNull { it.id == "rule_pediatric_infant_fever" }?.let { matched.add(it) }
        }

        // 4. Rule-table scan sorted by priority
        for (rule in data.emergencyRules) {
            val symTriggered = rule.triggerSymptomIds.any { it in input.selectedSymptomIds }
            val optTriggered = rule.triggerOptionIds.any { it in allOptions }
            if (!symTriggered && !optTriggered) continue

            if (rule.requiresPregnancy) {
                val eligibleFemale = !input.patient.sex.equals("M", ignoreCase = true) && input.patient.ageMonths >= 120
                val isPregContext = input.patient.isPregnantOrLactating ||
                    input.selectedSymptomIds.any { it.startsWith("sym_pregnancy_") }
                if (!eligibleFemale || !isPregContext) continue
            }

            if (rule.maxAgeMonthsTrigger != null && rule.id == "rule_pediatric_infant_fever") {
                val explicitChildTrigger = "sym_infant_under2m_fever" in input.selectedSymptomIds ||
                    "sym_child_danger_signs" in input.selectedSymptomIds ||
                    "opt_rf_stiff_neck_rash" in allOptions
                val ageOk = input.patient.ageMonths < rule.maxAgeMonthsTrigger || explicitChildTrigger
                if (!ageOk) continue
            }

            matched.add(rule)
        }

        // 5. Fallback for any symptom/option marked as red flag not yet covered
        input.selectedSymptomIds.mapNotNull { symptomById[it] }
            .filter { it.isRedFlagSymptom && it.linkedEmergencyCategory != null }
            .forEach { sym ->
                val cat = sym.linkedEmergencyCategory!!
                if (matched.none { it.category == cat }) {
                    val fa = triageEngine.getEmergencyFirstAid(cat)
                    matched.add(
                        EmergencyRule(
                            id = "rule_sym_${sym.id}",
                            priority = 50,
                            category = cat,
                            titleEn = sym.labelEn,
                            titleUr = sym.labelUr,
                            summaryEn = fa.do_now_en.firstOrNull() ?: sym.labelEn,
                            summaryUr = fa.do_now_ur.firstOrNull() ?: sym.labelUr,
                            triggerSymptomIds = listOf(sym.id),
                            sourceCitation = fa.sourceCitation,
                            lastReviewedDate = data.knowledgeMetadata.lastReviewedDate,
                        )
                    )
                }
            }

        allOptions.mapNotNull { optionById[it] }
            .filter { it.isEmergencyTrigger && it.linkedEmergencyCategory != null }
            .forEach { opt ->
                val cat = opt.linkedEmergencyCategory!!
                if (matched.none { it.category == cat }) {
                    val fa = triageEngine.getEmergencyFirstAid(cat)
                    matched.add(
                        EmergencyRule(
                            id = "rule_opt_${opt.id}",
                            priority = 55,
                            category = cat,
                            titleEn = opt.labelEn,
                            titleUr = opt.labelUr,
                            summaryEn = fa.do_now_en.firstOrNull() ?: opt.labelEn,
                            summaryUr = fa.do_now_ur.firstOrNull() ?: opt.labelUr,
                            triggerOptionIds = listOf(opt.id),
                            sourceCitation = fa.sourceCitation,
                            lastReviewedDate = data.knowledgeMetadata.lastReviewedDate,
                        )
                    )
                }
            }

        return matched.distinctBy { it.category }.sortedBy { it.priority }
    }

    fun detectConflicts(input: StructuredAssessmentInput): List<Pair<String, String>> {
        val errors = mutableListOf<Pair<String, String>>()
        val allOptions = input.selectedOptionIdsByQuestion.values.flatten().toSet()

        if (input.selectedSymptomIds.isEmpty() && input.directEmergencyCategory == null) {
            errors.add(
                "Please select at least one symptom to evaluate." to
                    "براہِ کرم جانچ کے لیے کم از کم ایک علامت منتخب کریں۔"
            )
            return errors
        }

        // Check required visible follow-up questions
        val requiredQuestions = visibleFollowUpQuestions(
            categoryId = input.categoryId,
            selectedSymptomIds = input.selectedSymptomIds,
            patient = input.patient,
        ).filter { it.required }

        for (q in requiredQuestions) {
            if (input.selectedOptionIdsByQuestion[q.id].isNullOrEmpty()) {
                errors.add(
                    "Please answer required follow-up: ${q.promptEn}" to
                        "براہِ کرم لازمی سوال کا جواب دیں: ${q.promptUr}"
                )
            }
        }

        // Contradictory symptom combinations
        if ("sym_watery_diarrhea" in input.selectedSymptomIds && "sym_constipation" in input.selectedSymptomIds) {
            errors.add(
                "Conflicting selection: Loose watery diarrhea and hard constipation cannot be assessed simultaneously as one acute pattern." to
                    "متضاد انتخاب: اسہال (پتلے دست) اور قبض ایک ساتھ منتخب نہیں کیے جا سکتے۔"
            )
        }
        if ("sym_mild_fever" in input.selectedSymptomIds && "sym_high_fever" in input.selectedSymptomIds) {
            errors.add(
                "Conflicting fever range: Please select either low-grade fever (< 101°F) or high fever (≥ 101°F), not both." to
                    "بخار کا متضاد انتخاب: ہلکا بخار یا تیز بخار میں سے کوئی ایک منتخب کریں۔"
            )
        }
        if ("sym_prolonged_cough_14d" in input.selectedSymptomIds &&
            ("opt_dur_under24h" in allOptions || "opt_dur_1_2d" in allOptions)
        ) {
            errors.add(
                "Duration mismatch: 'Cough lasting more than 14 days' conflicts with selecting a duration under 2 days." to
                    "مدت کا تضاد: 14 دن سے زائد کھانسی اور 1 تا 2 دن کی مدت آپس میں میل نہیں کھاتے۔"
            )
        }

        // Demographic consistency checks
        val hasPregnancyOnlySelection = input.categoryId == "cat_pregnancy" ||
            input.selectedSymptomIds.any { it == "sym_pregnancy_mild_nausea" }
        if (hasPregnancyOnlySelection && (!input.patient.isPregnantOrLactating || input.patient.sex.equals("M", ignoreCase = true) || input.patient.ageMonths < 120)) {
            errors.add(
                "Profile mismatch: Pregnancy care options do not match the selected patient profile (${input.patient.name}). Switch profile or update pregnancy status first." to
                    "پروفائل کا تضاد: حمل کی نگہداشت کے سوالات منتخب فرد کی عمر یا جنس سے مطابقت نہیں رکھتے۔"
            )
        }
        if (input.selectedSymptomIds.any { it.startsWith("sym_elderly_") } && input.patient.ageMonths < 720) {
            errors.add(
                "Age mismatch: Older-adult (60+) screening symptoms were selected for a profile under 60 years." to
                    "عمر کا تضاد: 60 سال سے زائد بزرگوں کی علامات کم عمر پروفائل کے لیے منتخب کی گئی ہیں۔"
            )
        }

        return errors
    }

    fun evaluateGuidance(input: StructuredAssessmentInput): MedicalGuidanceAssessment {
        val patient = input.patient
        val isPediatric = patient.ageMonths <= 60
        val allOptions = input.selectedOptionIdsByQuestion.values.flatten().toSet()

        // STEP 1: EMERGENCY TRIAGE ALWAYS RUNS FIRST (Overrides mild symptoms & suppresses home care)
        val triggeredEmergencies = checkEmergencyTriggers(input)
        if (triggeredEmergencies.isNotEmpty()) {
            val topRule = triggeredEmergencies.first()
            val firstAid = triageEngine.getEmergencyFirstAid(topRule.category)
            val (facilities, locationRequired) = triageEngine.sortFacilities(
                tier = TriageTier.RED,
                isPediatric = isPediatric,
                userCoords = input.userCoords,
                districtFilter = input.district,
            )
            return MedicalGuidanceAssessment(
                outcome = AssessmentOutcome.EMERGENCY_OVERRIDE,
                triageTier = TriageTier.RED,
                probabilisticDisclaimerEn = "EMERGENCY WARNING: Immediate emergency assessment required. Routine home remedies are intentionally withheld.",
                probabilisticDisclaimerUr = "فوری ہنگامی انتباہ: خطرناک علامت کی بنا پر گھریلو مشورے روک دیے گئے ہیں، فوری 1122 یا ایمرجنسی ہسپتال سے رجوع کریں۔",
                triggeredEmergencyRules = triggeredEmergencies,
                emergencyFirstAid = firstAid,
                matchedConditions = emptyList(),
                recommendedFacilities = facilities,
                locationRequired = locationRequired,
                referralSlip = buildReferralSlip(
                    patient = patient,
                    tier = TriageTier.RED,
                    primaryFlag = topRule.id.uppercase(),
                    facilityId = facilities.firstOrNull()?.id ?: "LOCATION_PENDING",
                ),
                knowledgeMetadata = data.knowledgeMetadata,
            )
        }

        // STEP 2: INPUT VALIDATION & CONFLICT DETECTION
        val conflicts = detectConflicts(input)
        if (conflicts.isNotEmpty()) {
            val (facilities, locationRequired) = triageEngine.sortFacilities(
                tier = TriageTier.AMBER,
                isPediatric = isPediatric,
                userCoords = input.userCoords,
                districtFilter = input.district,
            )
            return MedicalGuidanceAssessment(
                outcome = AssessmentOutcome.INVALID_OR_CONFLICTING,
                triageTier = TriageTier.AMBER,
                probabilisticDisclaimerEn = "Assessment paused due to incomplete or conflicting symptom inputs. Please review and update your selections.",
                probabilisticDisclaimerUr = "نامکمل یا متضاد معلومات کی وجہ سے جانچ روک دی گئی ہے۔ براہِ کرم اپنی علامات درست کریں۔",
                validationErrorsEn = conflicts.map { it.first },
                validationErrorsUr = conflicts.map { it.second },
                recommendedFacilities = facilities,
                locationRequired = locationRequired,
                knowledgeMetadata = data.knowledgeMetadata,
            )
        }

        // STEP 3: CONDITION MATCHING & SCORING
        val maxUrgencyBoost = allOptions.mapNotNull { optionById[it]?.urgencyBoost }.maxOrNull() ?: 0
        val hasHighFever = "sym_high_fever" in input.selectedSymptomIds
        val matched = mutableListOf<MatchedConditionResult>()

        for (cond in data.medicalConditions) {
            val criteria = cond.matchingCriteria
            if (criteria.minAgeMonths != null && patient.ageMonths < criteria.minAgeMonths) continue
            if (criteria.maxAgeMonths != null && patient.ageMonths > criteria.maxAgeMonths) continue
            if (criteria.pregnantOnly && !patient.isPregnantOrLactating) continue
            if (criteria.excludedSymptomIds.any { it in input.selectedSymptomIds }) continue
            if (criteria.excludedOptionIds.any { it in allOptions }) continue
            if (criteria.requiredAnySymptomIds.isNotEmpty() &&
                criteria.requiredAnySymptomIds.none { it in input.selectedSymptomIds }
            ) continue

            var score = 0
            val matchedSyms = mutableListOf<SymptomItem>()
            for (symId in input.selectedSymptomIds) {
                val w = criteria.supportingSymptomWeights[symId] ?: 0
                if (w > 0) {
                    score += w
                    symptomById[symId]?.let { matchedSyms.add(it) }
                }
            }
            for (optId in allOptions) {
                score += criteria.supportingOptionWeights[optId] ?: 0
            }

            if (score < criteria.minScore) continue

            // Age-safe home care sanitization (NEVER recommend honey under 12 months)
            val safeHomeCareEn = if (patient.ageMonths < 12) {
                cond.safeHomeCareEn.filterNot { it.contains("honey", ignoreCase = true) }
            } else cond.safeHomeCareEn
            val safeHomeCareUr = if (patient.ageMonths < 12) {
                cond.safeHomeCareUr.filterNot { it.contains("شہد") }
            } else cond.safeHomeCareUr
            val sanitizedCondition = cond.copy(
                safeHomeCareEn = safeHomeCareEn,
                safeHomeCareUr = safeHomeCareUr,
            )

            // Determine adjusted severity & population precautions
            var adjustedSeverity = cond.possibleSeverity
            if ((maxUrgencyBoost >= 2 || hasHighFever) && adjustedSeverity == ConditionSeverity.MILD) {
                adjustedSeverity = ConditionSeverity.MODERATE
            }
            if (maxUrgencyBoost >= 2 && "opt_sev_severe" in allOptions && adjustedSeverity == ConditionSeverity.MODERATE) {
                adjustedSeverity = ConditionSeverity.URGENT
            }
            if (patient.isPregnantOrLactating && criteria.pregnancyEscalatesUrgency && adjustedSeverity != ConditionSeverity.URGENT) {
                adjustedSeverity = ConditionSeverity.URGENT
            }
            if (patient.ageMonths >= 720 && criteria.olderAdultEscalatesUrgency && adjustedSeverity == ConditionSeverity.MILD) {
                adjustedSeverity = ConditionSeverity.MODERATE
            }
            if (patient.ageMonths < 2 && criteria.infantUnder2mAlwaysUrgent) {
                adjustedSeverity = ConditionSeverity.URGENT
            }

            val alertPair: Pair<String?, String?> = when {
                patient.ageMonths < 12 ->
                    cond.specialPrecautions.childrenEn to cond.specialPrecautions.childrenUr
                patient.isPregnantOrLactating ->
                    cond.specialPrecautions.pregnancyEn to cond.specialPrecautions.pregnancyUr
                patient.ageMonths >= 720 ->
                    cond.specialPrecautions.olderAdultsEn to cond.specialPrecautions.olderAdultsUr
                patient.ageMonths <= 216 ->
                    cond.specialPrecautions.childrenEn to cond.specialPrecautions.childrenUr
                else -> null to null
            }

            matched.add(
                MatchedConditionResult(
                    condition = sanitizedCondition,
                    score = score,
                    matchedSymptomLabelsEn = matchedSyms.map { it.labelEn }.ifEmpty {
                        input.selectedSymptomIds.mapNotNull { symptomById[it]?.labelEn }
                    },
                    matchedSymptomLabelsUr = matchedSyms.map { it.labelUr }.ifEmpty {
                        input.selectedSymptomIds.mapNotNull { symptomById[it]?.labelUr }
                    },
                    probabilisticHeaderEn = "",
                    probabilisticHeaderUr = "",
                    adjustedSeverity = adjustedSeverity,
                    ageOrPregnancyAlertEn = alertPair.first,
                    ageOrPregnancyAlertUr = alertPair.second,
                )
            )
        }

        val ranked = matched
            .sortedWith(
                compareByDescending<MatchedConditionResult> { it.score }
                    .thenByDescending { it.adjustedSeverity.ordinal }
            )
            .take(3)
            .mapIndexed { idx, item ->
                val enPhrase = if (idx == 0) {
                    "These symptoms may be associated with ${item.condition.nameEn}. Other conditions may cause similar symptoms."
                } else {
                    "One possible explanation is ${item.condition.nameEn}. Only a clinician can confirm the true cause."
                }
                val urPhrase = if (idx == 0) {
                    "یہ علامات ممکنہ طور پر «${item.condition.nameUr}» سے منسلک ہو سکتی ہیں۔ دیگر بیماریاں بھی ملتی جلتی علامات پیدا کر سکتی ہیں۔"
                } else {
                    "ایک اور ممکنہ وجہ «${item.condition.nameUr}» ہو سکتی ہے۔ حتمی تصدیق صرف ڈاکٹر ہی کر سکتا ہے۔"
                }
                item.copy(probabilisticHeaderEn = enPhrase, probabilisticHeaderUr = urPhrase)
            }

        // STEP 4: EXPLICIT HANDLING OF UNKNOWN OR INSUFFICIENT COMBINATIONS
        if (ranked.isEmpty()) {
            val (facilities, locationRequired) = triageEngine.sortFacilities(
                tier = TriageTier.AMBER,
                isPediatric = isPediatric,
                userCoords = input.userCoords,
                districtFilter = input.district,
            )
            return MedicalGuidanceAssessment(
                outcome = AssessmentOutcome.INSUFFICIENT_OR_UNKNOWN,
                triageTier = TriageTier.AMBER,
                probabilisticDisclaimerEn = "Informational guidance only — NOT a medical diagnosis. This offline rule engine cannot match your specific combination reliably.",
                probabilisticDisclaimerUr = "یہ صرف معلوماتی رہنمائی ہے، حتمی تشخیص نہیں۔ آپ کی منتخب علامات کا مجموعہ اس آف لائن نظام کے کسی ایک واضح مرض سے قابلِ اعتماد طور پر میل نہیں کھاتا۔",
                uncertaintyExplanationEn = "Your entered symptom combination does not provide enough specific matches in our curated offline library (${data.medicalConditions.size} conditions). Because forcing a guess is unsafe, please consult a qualified doctor or BHU for proper physical examination—especially if symptoms worsen, fever persists beyond 48 hours, or any emergency sign appears.",
                uncertaintyExplanationUr = "آپ کی درج کردہ علامات ہمارے محفوظ آف لائن طبی مجموعے (${data.medicalConditions.size} امراض) میں کسی ایک واضح کیفیت سے مطابقت نہیں رکھتیں۔ حفاظت کے پیشِ نظر نظام کوئی اندازہ نہیں لگاتا؛ براہِ کرم مستند ڈاکٹر یا مرکزِ صحت (BHU) سے معائنہ کروائیں۔",
                recommendedFacilities = facilities,
                locationRequired = locationRequired,
                referralSlip = buildReferralSlip(
                    patient = patient,
                    tier = TriageTier.AMBER,
                    primaryFlag = "UNMATCHED_CLINICIAN_REVIEW",
                    facilityId = facilities.firstOrNull()?.id ?: "LOCATION_PENDING",
                ),
                knowledgeMetadata = data.knowledgeMetadata,
            )
        }

        // STEP 5: DERIVE OVERALL TRIAGE TIER (NEVER FORCE HOME CARE FOR URGENT CONDITIONS)
        val requiresClinicToday = ranked.any {
            it.adjustedSeverity == ConditionSeverity.URGENT ||
                it.condition.noHomeCareReasonEn != null ||
                it.adjustedSeverity == ConditionSeverity.MODERATE
        } || maxUrgencyBoost >= 1 || hasHighFever

        val tier = if (requiresClinicToday) TriageTier.AMBER else TriageTier.GREEN
        val (facilities, locationRequired) = triageEngine.sortFacilities(
            tier = tier,
            isPediatric = isPediatric,
            userCoords = input.userCoords,
            districtFilter = input.district,
        )

        return MedicalGuidanceAssessment(
            outcome = AssessmentOutcome.CONDITION_MATCHED,
            triageTier = tier,
            probabilisticDisclaimerEn = "Educational & rule-based triage support only — NOT a confirmed medical diagnosis. Other conditions may cause similar symptoms.",
            probabilisticDisclaimerUr = "یہ صرف تعلیمی اور اصولی طبی رہنمائی ہے، حتمی تشخیص ہرگز نہیں۔ دیگر امراض میں بھی ملتی جلتی علامات ہو سکتی ہیں۔",
            matchedConditions = ranked,
            recommendedFacilities = facilities,
            locationRequired = locationRequired,
            referralSlip = buildReferralSlip(
                patient = patient,
                tier = tier,
                primaryFlag = ranked.first().condition.id.uppercase(),
                facilityId = facilities.firstOrNull()?.id ?: "LOCATION_PENDING",
            ),
            knowledgeMetadata = data.knowledgeMetadata,
        )
    }

    fun toCompatibilityEvaluation(
        input: StructuredAssessmentInput,
        assessment: MedicalGuidanceAssessment,
    ): Pair<EpisodeInput, TriageEvaluationResult> {
        val allOptions = input.selectedOptionIdsByQuestion.values.flatten().toSet()
        val durationHours = when {
            "opt_dur_over7d" in allOptions -> 360
            "opt_dur_3_7d" in allOptions -> 96
            "opt_dur_1_2d" in allOptions -> 36
            else -> 12
        }
        val complaint = when (input.categoryId) {
            "cat_respiratory" -> PrimaryComplaint.COUGH_COLD
            "cat_digestive" -> PrimaryComplaint.DIARRHEA
            "cat_chronic" -> if ("sym_heat_exhaustion" in input.selectedSymptomIds) PrimaryComplaint.HEAT_EXHAUSTION else PrimaryComplaint.CHRONIC_FLARE
            "cat_injuries" -> if ("sym_minor_burn" in input.selectedSymptomIds || "sym_severe_burn" in input.selectedSymptomIds) PrimaryComplaint.MINOR_BURN else PrimaryComplaint.MINOR_WOUND
            else -> PrimaryComplaint.FEVER
        }
        val bodyRegion = when (input.categoryId) {
            "cat_eye_ear", "cat_neuro" -> BodyRegion.HEAD_NECK
            "cat_cardio", "cat_respiratory" -> BodyRegion.CHEST
            "cat_digestive", "cat_urinary", "cat_pregnancy" -> BodyRegion.ABDOMEN
            "cat_skin", "cat_msk", "cat_injuries" -> BodyRegion.SKIN_LIMBS
            else -> BodyRegion.GENERAL_FEVER
        }
        val epInput = EpisodeInput(
            patient = input.patient,
            bodyRegion = bodyRegion,
            durationHours = durationHours,
            temperatureBand = if ("sym_high_fever" in input.selectedSymptomIds) TemperatureBand.HIGH_FEVER else TemperatureBand.MILD_FEVER,
            hydrationStatus = if ("opt_rf_unable_drink" in allOptions || "sym_severe_dehydration" in input.selectedSymptomIds) {
                HydrationStatus.UNABLE_TO_DRINK
            } else {
                HydrationStatus.DRINKING_WELL
            },
            vitals = input.vitals,
            answers = TriageAnswers(primaryComplaint = complaint),
            district = input.district,
            userCoords = input.userCoords,
            emergencyOverrideCategory = assessment.triggeredEmergencyRules.firstOrNull()?.category,
        )
        val slip = assessment.referralSlip ?: buildReferralSlip(
            patient = input.patient,
            tier = assessment.triageTier,
            primaryFlag = input.categoryId.uppercase(),
            facilityId = assessment.recommendedFacilities.firstOrNull()?.id ?: "LOCATION_PENDING",
        )
        val result = TriageEvaluationResult(
            tier = assessment.triageTier,
            redFlags = assessment.triggeredEmergencyRules.map {
                com.example.data.model.RedFlagReason(it.id, it.titleEn, it.titleUr, it.sourceCitation)
            },
            amberReasons = assessment.matchedConditions.map { it.condition.nameEn },
            fastBreathingThreshold = triageEngine.getFastBreathingThreshold(input.patient.ageMonths),
            isFastBreathing = input.vitals.respiratoryRate != null &&
                input.vitals.respiratoryRate >= triageEngine.getFastBreathingThreshold(input.patient.ageMonths),
            matchedHomeCare = null,
            emergencyFirstAid = assessment.emergencyFirstAid,
            matchedDiseaseRef = data.diseaseReference.first(),
            recommendedFacilities = assessment.recommendedFacilities,
            locationRequired = assessment.locationRequired,
            referralSlip = slip,
            symptomCategory = input.categoryId.uppercase(),
        )
        return epInput to result
    }

    private fun buildReferralSlip(
        patient: PatientProfile,
        tier: TriageTier,
        primaryFlag: String,
        facilityId: String,
    ): ReferralSlip {
        val bytes = ByteArray(4).also { secureRandom.nextBytes(it) }
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
        return ReferralSlip(
            referralToken = "TIBB-$tier-$hex",
            ageBracket = triageEngine.getAgeBracket(patient.ageMonths),
            tier = tier,
            primaryFlag = primaryFlag,
            facilityId = facilityId,
            createdAtIso = nowIso,
        )
    }
}
