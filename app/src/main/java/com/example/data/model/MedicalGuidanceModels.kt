package com.example.data.model

import com.squareup.moshi.JsonClass

enum class FollowUpDimension {
    SEVERITY,
    DURATION,
    LOCATION,
    FREQUENCY,
    ASSOCIATED_SYMPTOMS,
    CIRCUMSTANCES,
    WARNING_SIGNS,
}

enum class ConditionSeverity {
    MILD,
    MODERATE,
    URGENT,
    EMERGENCY,
}

enum class AssessmentOutcome {
    EMERGENCY_OVERRIDE,
    CONDITION_MATCHED,
    INSUFFICIENT_OR_UNKNOWN,
    INVALID_OR_CONFLICTING,
}

@JsonClass(generateAdapter = true)
data class KnowledgeMetadata(
    val version: String,
    val lastReviewedDate: String,
    val titleEn: String,
    val titleUr: String,
    val authoritativeSources: List<String>,
    val coveredDomainsEn: List<String>,
    val coveredDomainsUr: List<String>,
    val limitationsEn: List<String>,
    val limitationsUr: List<String>,
    val medicalDisclaimerEn: String,
    val medicalDisclaimerUr: String,
)

@JsonClass(generateAdapter = true)
data class SymptomCategoryNode(
    val id: String,
    val titleEn: String,
    val titleUr: String,
    val titleRoman: String,
    val subtitleEn: String,
    val subtitleUr: String,
    val iconKey: String,
    val minAgeMonths: Int? = null,
    val maxAgeMonths: Int? = null,
    val pregnantOnly: Boolean = false,
    val symptoms: List<SymptomItem>,
)

@JsonClass(generateAdapter = true)
data class SymptomItem(
    val id: String,
    val labelEn: String,
    val labelUr: String,
    val labelRoman: String,
    val hintEn: String = "",
    val hintUr: String = "",
    val isRedFlagSymptom: Boolean = false,
    val linkedEmergencyCategory: EmergencyCategory? = null,
    val minAgeMonths: Int? = null,
    val maxAgeMonths: Int? = null,
    val pregnantOnly: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class FollowUpQuestion(
    val id: String,
    val dimension: FollowUpDimension,
    val promptEn: String,
    val promptUr: String,
    val promptRoman: String,
    val multiSelect: Boolean = false,
    val required: Boolean = false,
    val showForCategories: List<String> = emptyList(),
    val showForSymptoms: List<String> = emptyList(),
    val minAgeMonths: Int? = null,
    val maxAgeMonths: Int? = null,
    val pregnantOnly: Boolean = false,
    val options: List<FollowUpOption>,
)

@JsonClass(generateAdapter = true)
data class FollowUpOption(
    val id: String,
    val labelEn: String,
    val labelUr: String,
    val labelRoman: String,
    val isEmergencyTrigger: Boolean = false,
    val linkedEmergencyCategory: EmergencyCategory? = null,
    val urgencyBoost: Int = 0, // 0 normal, 1 amber/doctor, 2 urgent
)

@JsonClass(generateAdapter = true)
data class SymptomTaxonomy(
    val categories: List<SymptomCategoryNode>,
    val followUpQuestions: List<FollowUpQuestion>,
)

@JsonClass(generateAdapter = true)
data class EmergencyRule(
    val id: String,
    val priority: Int,
    val category: EmergencyCategory,
    val titleEn: String,
    val titleUr: String,
    val summaryEn: String,
    val summaryUr: String,
    val triggerSymptomIds: List<String> = emptyList(),
    val triggerOptionIds: List<String> = emptyList(),
    val requiresPregnancy: Boolean = false,
    val maxAgeMonthsTrigger: Int? = null,
    val sourceCitation: String,
    val lastReviewedDate: String,
)

@JsonClass(generateAdapter = true)
data class SpecialPrecautions(
    val childrenEn: String,
    val childrenUr: String,
    val olderAdultsEn: String,
    val olderAdultsUr: String,
    val pregnancyEn: String,
    val pregnancyUr: String,
)

@JsonClass(generateAdapter = true)
data class ConditionMatchingCriteria(
    val categoryIds: List<String> = emptyList(),
    val requiredAnySymptomIds: List<String> = emptyList(),
    val supportingSymptomWeights: Map<String, Int> = emptyMap(),
    val supportingOptionWeights: Map<String, Int> = emptyMap(),
    val excludedSymptomIds: List<String> = emptyList(),
    val excludedOptionIds: List<String> = emptyList(),
    val minScore: Int = 3,
    val minAgeMonths: Int? = null,
    val maxAgeMonths: Int? = null,
    val pregnantOnly: Boolean = false,
    val infantUnder2mAlwaysUrgent: Boolean = false,
    val pregnancyEscalatesUrgency: Boolean = false,
    val olderAdultEscalatesUrgency: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class MedicalConditionEntry(
    val id: String,
    val nameEn: String,
    val nameUr: String,
    val categoryId: String,
    val categoryLabelEn: String,
    val categoryLabelUr: String,
    val subcategoryEn: String = "",
    val subcategoryUr: String = "",
    val shortDescriptionEn: String,
    val shortDescriptionUr: String,
    val commonSymptomsEn: List<String>,
    val commonSymptomsUr: List<String>,
    val associatedSymptomsEn: List<String>,
    val associatedSymptomsUr: List<String>,
    val riskFactorsEn: List<String>,
    val riskFactorsUr: List<String>,
    val possibleSeverity: ConditionSeverity,
    val emergencyWarningSignsEn: List<String>,
    val emergencyWarningSignsUr: List<String>,
    val appropriateFirstAidEn: List<String>,
    val appropriateFirstAidUr: List<String>,
    val safeHomeCareEn: List<String>,
    val safeHomeCareUr: List<String>,
    val noHomeCareReasonEn: String? = null,
    val noHomeCareReasonUr: String? = null,
    val thingsToAvoidEn: List<String>,
    val thingsToAvoidUr: List<String>,
    val whenToConsultDoctorEn: String,
    val whenToConsultDoctorUr: String,
    val whenUrgentCareEn: String,
    val whenUrgentCareUr: String,
    val whenEmergencyServicesEn: String,
    val whenEmergencyServicesUr: String,
    val specialPrecautions: SpecialPrecautions,
    val sources: List<String>,
    val lastReviewedDate: String,
    val matchingCriteria: ConditionMatchingCriteria,
)

data class StructuredAssessmentInput(
    val patient: PatientProfile,
    val categoryId: String,
    val selectedSymptomIds: Set<String>,
    val selectedOptionIdsByQuestion: Map<String, Set<String>>,
    val vitals: EpisodeVitals = EpisodeVitals(),
    val district: String? = null,
    val userCoords: GeoCoords? = null,
    val directEmergencyCategory: EmergencyCategory? = null,
)

data class MatchedConditionResult(
    val condition: MedicalConditionEntry,
    val score: Int,
    val matchedSymptomLabelsEn: List<String>,
    val matchedSymptomLabelsUr: List<String>,
    val probabilisticHeaderEn: String,
    val probabilisticHeaderUr: String,
    val adjustedSeverity: ConditionSeverity,
    val ageOrPregnancyAlertEn: String? = null,
    val ageOrPregnancyAlertUr: String? = null,
)

data class MedicalGuidanceAssessment(
    val outcome: AssessmentOutcome,
    val triageTier: TriageTier,
    val probabilisticDisclaimerEn: String,
    val probabilisticDisclaimerUr: String,
    val triggeredEmergencyRules: List<EmergencyRule> = emptyList(),
    val emergencyFirstAid: EmergencyFirstAidProtocol? = null,
    val matchedConditions: List<MatchedConditionResult> = emptyList(),
    val uncertaintyExplanationEn: String? = null,
    val uncertaintyExplanationUr: String? = null,
    val validationErrorsEn: List<String> = emptyList(),
    val validationErrorsUr: List<String> = emptyList(),
    val recommendedFacilities: List<FacilityRecord> = emptyList(),
    val locationRequired: Boolean = true,
    val referralSlip: ReferralSlip? = null,
    val knowledgeMetadata: KnowledgeMetadata,
)
