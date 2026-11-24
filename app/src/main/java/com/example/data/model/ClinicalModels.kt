package com.example.data.model

import com.squareup.moshi.JsonClass

enum class AppLocale(val code: String, val label: String) {
    EN("en", "EN"),
    UR("ur", "اردو"),
    ROMAN("roman", "Roman"),
}

enum class UnitSystem { METRIC, IMPERIAL }

enum class WorkspaceMode { GHARANA, BHU }

enum class TriageTier { RED, AMBER, GREEN }

enum class BodyRegion { HEAD_NECK, CHEST, ABDOMEN, SKIN_LIMBS, GENERAL_FEVER }

enum class TemperatureBand { NORMAL, MILD_FEVER, HIGH_FEVER }

enum class HydrationStatus { DRINKING_WELL, REDUCED_FLUIDS, UNABLE_TO_DRINK }

enum class MeasurementMethod { TAP_TIMER, CAMERA_PPG, MANUAL, NONE }

enum class SugarMode { FASTING, RANDOM }

enum class PrimaryComplaint {
    FEVER,
    COUGH_COLD,
    DIARRHEA,
    MINOR_WOUND,
    MINOR_BURN,
    HEAT_EXHAUSTION,
    CHRONIC_FLARE,
}

enum class EmergencyCategory {
    CHEST_PAIN,
    STROKE_FAST,
    BREATHING_DISTRESS,
    UNCONTROLLED_BLEEDING,
    SEIZURE_CONVULSIONS,
    SEVERE_BURN,
    POISONING_OVERDOSE,
    HEATSTROKE,
    CHOKING,
    UNCONSCIOUS_FAINTING,
    MATERNAL_EMERGENCY,
    PEDIATRIC_DANGER,
}

enum class RelationshipType(val labelEn: String, val labelUr: String) {
    Self("Self", "خود"),
    Mother("Mother", "والدہ"),
    Father("Father", "والد"),
    Child("Child", "بچہ"),
    Spouse("Spouse", "شریکِ حیات"),
    Other("Other", "دیگر"),
}

enum class BloodGroup(val label: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-"),
    Unknown("Unknown"),
}

enum class WellnessGoal(val titleEn: String, val titleUr: String) {
    BALANCED("Balanced Daily Health", "متوازن روزمرہ صحت"),
    WEIGHT_LOSS("Healthy Weight Loss", "صحت مند وزن میں کمی"),
    MUSCLE_GAIN("Muscle & Lean Weight Gain", "پٹھوں کی مضبوطی اور وزن بڑھانا"),
    DIABETES_CONTROL("Blood Sugar Control", "شوگر کنٹرول"),
    BP_FRIENDLY("BP & Heart Friendly", "بلڈ پریشر اور دل کی حفاظت"),
    ENERGY_STRENGTH("Energy & Strength", "توانائی اور طاقت"),
    SENIOR_MOBILITY("Senior Gentle Mobility", "بزرگوں کی ہلکی ورزش"),
}

@JsonClass(generateAdapter = true)
data class FrequentPlace(
    val id: String,
    val label: String,
    val district: String,
    val areaOrLandmark: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

@JsonClass(generateAdapter = true)
data class PatientProfile(
    val id: String,
    val accountId: String = "acc_local_family_default",
    val name: String,
    val ageMonths: Int = 300,
    val sex: String = "O", // M, F, O
    val isPregnantOrLactating: Boolean = false,
    val chronicConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val workspace: WorkspaceMode = WorkspaceMode.GHARANA,
    val relationship: RelationshipType = RelationshipType.Self,
    val bloodGroup: BloodGroup = BloodGroup.Unknown,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val baselineWeightKg: Double? = null,
    val preferredUnitSystem: UnitSystem = UnitSystem.METRIC,
    val preferredGoal: WellnessGoal = WellnessGoal.BALANCED,
    val dietaryPreferences: List<String> = emptyList(),
    val priorLabNotes: String = "",
    val frequentPlaces: List<FrequentPlace> = emptyList(),
    val onboardingCompleted: Boolean = true,
    val dailyMedicinesNote: String = "",
    val emergencyContact: String = "",
    val createdAtIso: String = "",
)

@JsonClass(generateAdapter = true)
data class EpisodeVitals(
    val respiratoryRate: Int? = null,
    val heartRate: Int? = null,
    val measurementMethod: MeasurementMethod = MeasurementMethod.NONE,
    val signalQualityIndex: Int? = null,
    val bpSystolic: Int? = null,
    val bpDiastolic: Int? = null,
    val bloodGlucoseMgDl: Int? = null,
    val sugarMode: SugarMode? = null,
    val temperatureF: Double? = null,
)

@JsonClass(generateAdapter = true)
data class TriageAnswers(
    val unableToDrink: Boolean = false,
    val vomitsEverything: Boolean = false,
    val convulsions: Boolean = false,
    val lethargicOrUnconscious: Boolean = false,
    val chestIndrawingOrStridor: Boolean = false,
    val crushingChestPain: Boolean = false,
    val strokeFastSigns: Boolean = false,
    val severeShortnessOfBreath: Boolean = false,
    val uncontrolledBleeding: Boolean = false,
    val stiffNeckOrPetechialRash: Boolean = false,
    val pregnancyDangerSigns: Boolean = false,
    val moderateDehydration: Boolean = false,
    val bloodInStoolOrVomit: Boolean = false,
    val burnOrDeepWound: Boolean = false,
    val heatExhaustionSigns: Boolean = false,
    val persistentCoughOver14Days: Boolean = false,
    val primaryComplaint: PrimaryComplaint = PrimaryComplaint.FEVER,
)

data class GeoCoords(val lat: Double, val lng: Double)

data class EpisodeInput(
    val patient: PatientProfile,
    val bodyRegion: BodyRegion,
    val durationHours: Int,
    val temperatureBand: TemperatureBand,
    val hydrationStatus: HydrationStatus,
    val vitals: EpisodeVitals,
    val answers: TriageAnswers,
    val district: String? = null,
    val userCoords: GeoCoords? = null,
    val emergencyOverrideCategory: EmergencyCategory? = null,
)

@JsonClass(generateAdapter = true)
data class FacilityRecord(
    val id: String,
    val name_en: String,
    val name_ur: String,
    val tier: String, // TERTIARY, DHQ_THQ, RHC_BHU
    val district: String,
    val tehsil: String,
    val emergency24x7: Boolean,
    val pediatricEmergency: Boolean,
    val phone: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HomeCareProtocol(
    val id: String,
    val title_en: String,
    val title_ur: String,
    val title_roman: String,
    val steps_en: List<String>,
    val steps_ur: List<String>,
    val steps_roman: List<String>,
    val contraindications_en: List<String>,
    val contraindications_ur: List<String>,
    val allowedNumericClaims: List<String>,
    val escalation_warning_en: String,
    val escalation_warning_ur: String,
    val sourceCitation: String,
)

@JsonClass(generateAdapter = true)
data class EmergencyFirstAidProtocol(
    val id: EmergencyCategory,
    val title_en: String,
    val title_ur: String,
    val do_now_en: List<String>,
    val do_now_ur: List<String>,
    val do_not_en: List<String>,
    val do_not_ur: List<String>,
    val sourceCitation: String,
)

@JsonClass(generateAdapter = true)
data class DiseaseReferenceCard(
    val id: String,
    val category: String,
    val title_en: String,
    val title_ur: String,
    val verified_facts_en: List<String>,
    val verified_facts_ur: List<String>,
    val allowedNumericClaims: List<String>,
    val linkedHomeCareId: String,
)

@JsonClass(generateAdapter = true)
data class DrapBlocklist(
    val antibiotics_generic: List<String>,
    val pakistan_brand_names: List<String>,
    val restricted_rx_classes: List<String>,
    val diagnostic_phrases: List<String>,
)

data class RedFlagReason(
    val code: String,
    val label_en: String,
    val label_ur: String,
    val source: String, // WHO_IMCI, WHO_ETAT, MATERNAL, VITALS
)

data class ReferralSlip(
    val referralToken: String,
    val ageBracket: String,
    val tier: TriageTier,
    val primaryFlag: String,
    val facilityId: String,
    val createdAtIso: String,
)

data class TriageEvaluationResult(
    val tier: TriageTier,
    val redFlags: List<RedFlagReason>,
    val amberReasons: List<String>,
    val fastBreathingThreshold: Int,
    val isFastBreathing: Boolean,
    val matchedHomeCare: HomeCareProtocol?,
    val emergencyFirstAid: EmergencyFirstAidProtocol?,
    val matchedDiseaseRef: DiseaseReferenceCard,
    val recommendedFacilities: List<FacilityRecord>,
    val locationRequired: Boolean,
    val referralSlip: ReferralSlip,
    val symptomCategory: String,
)

data class GroundingAuditResult(
    val passed: Boolean,
    val sanitizedText: String,
    val violations: List<String>,
    val amrBlocked: Boolean,
)

enum class StayHomeVerdict { STAY_HOME, RECHECK_AND_CLINIC, EMERGENCY_1122 }

@JsonClass(generateAdapter = true)
data class StayHomeGuidance(
    val verdict: StayHomeVerdict,
    val title_en: String,
    val title_ur: String,
    val explanation_en: String,
    val explanation_ur: String,
    val actionSteps_en: List<String>,
    val actionSteps_ur: List<String>,
    val disclaimer_en: String,
    val disclaimer_ur: String,
)

@JsonClass(generateAdapter = true)
data class QuickReplyOption(
    val label_en: String,
    val label_ur: String,
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String,
    val threadId: String,
    val personId: String,
    val role: String, // "user" | "agent" | "system"
    val text: String,
    val text_ur: String? = null,
    val timestampIso: String,
    val mediaPath: String? = null,
    val mediaKind: String? = null, // IMAGE, CAMERA_FRAME, VIDEO_BURST, VOICE
    val triageTier: TriageTier? = null,
    val emergencyCategory: EmergencyCategory? = null,
    val firstAid: EmergencyFirstAidProtocol? = null,
    val stayHomeGuidance: StayHomeGuidance? = null,
    val matchedHomeCare: HomeCareProtocol? = null,
    val suggestCalmPacer: Boolean = false,
    val suggestFarHospitalMode: Boolean = false,
    val quickReplies: List<QuickReplyOption> = emptyList(),
    val engineUsed: String? = null,
    val amrBlocked: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ChatThread(
    val id: String,
    val personId: String,
    val title: String,
    val createdAtIso: String,
    val updatedAtIso: String,
    val latestTier: TriageTier = TriageTier.GREEN,
    val messages: List<ChatMessage> = emptyList(),
)

enum class SavedReportKind {
    REPORT_PHOTO,
    PRESCRIPTION,
    MEAL_PHOTO,
    VITALS_LOG,
    EXERCISE_LOG,
    REFERRAL_SLIP,
}

@JsonClass(generateAdapter = true)
data class SavedPersonReport(
    val id: String,
    val personId: String,
    val kind: SavedReportKind,
    val title: String,
    val notes: String,
    val imagePath: String? = null,
    val vitalsSnapshot: EpisodeVitals? = null,
    val referralToken: String? = null,
    val triageTier: TriageTier? = null,
    val createdAtIso: String,
)

@JsonClass(generateAdapter = true)
data class MealOptionItem(
    val title_en: String,
    val title_ur: String,
    val portions_en: String,
    val portions_ur: String,
    val caloriesApprox: Int,
)

@JsonClass(generateAdapter = true)
data class DietMealSlot(
    val slot: String, // Breakfast, Mid-Day Snack, Lunch, Dinner
    val title_en: String,
    val title_ur: String,
    val portions_en: String,
    val portions_ur: String,
    val caloriesApprox: Int,
    val selectedOptionIndex: Int = 0,
    val alternatives: List<MealOptionItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WorkoutExerciseSlot(
    val name_en: String,
    val name_ur: String,
    val durationOrReps: String,
    val intensity: String, // Gentle, Moderate, Brisk
    val tip_en: String,
)

@JsonClass(generateAdapter = true)
data class PersonalizedWellnessPlan(
    val id: String,
    val personId: String,
    val goal: WellnessGoal,
    val generatedAtIso: String,
    val dailyWaterGoalMl: Int,
    val dailyStepsGoal: Int,
    val dietMeals: List<DietMealSlot>,
    val workoutRoutine: List<WorkoutExerciseSlot>,
    val avoidNotes_en: List<String>,
    val avoidNotes_ur: List<String>,
    val engineUsed: String,
)

@JsonClass(generateAdapter = true)
data class DailyDoctorCheckIn(
    val answeredDateKey: String,
    val currentWeightKg: Double? = null,
    val weightFeeling: String? = null, // LOSING, SAME, GAINING
    val weightDeltaKg: Double? = null,
    val sleepHours: Double? = null,
    val bedTime24: String? = null,
    val wakeTime24: String? = null,
    val sleepQuality: Int? = null,
    val energyLevel: String? = null, // LOW, STEADY, STRONG
    val symptomsNote: String? = null,
    val answeredAtIso: String,
)

@JsonClass(generateAdapter = true)
data class DailyWellnessLog(
    val id: String,
    val personId: String,
    val dateKey: String,
    val steps: Int = 0,
    val stepGoal: Int = 7000,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2250,
    val sleepHours: Double = 7.0,
    val restingHr: Int? = null,
    val hrvMs: Int? = null,
    val lastSugarMgDl: Int? = null,
    val lastBpSystolic: Int? = null,
    val lastBpDiastolic: Int? = null,
    val mealsChecked: List<String> = emptyList(),
    val workoutDone: Boolean = false,
    val dailyCheckIn: DailyDoctorCheckIn? = null,
    val plan: PersonalizedWellnessPlan? = null,
    val updatedAtIso: String,
)

data class HomeRemedyItem(
    val id: String,
    val category: String, // FEVER_THROAT, STOMACH_HYDRATION, STRESS_SLEEP, HEAT_SKIN, PAIN_RECOVERY
    val title_en: String,
    val title_ur: String,
    val ingredients_en: String,
    val ingredients_ur: String,
    val steps_en: List<String>,
    val steps_ur: List<String>,
    val caution_en: String,
    val caution_ur: String,
)

@JsonClass(generateAdapter = true)
data class StoredEpisode(
    val id: String,
    val patientId: String,
    val patientName: String,
    val ageBracket: String,
    val triageTier: TriageTier,
    val symptomCategory: String,
    val redFlags: List<String>,
    val facilityId: String,
    val facilityName: String,
    val referralToken: String,
    val referralCompleted: Boolean = false,
    val vitals: EpisodeVitals,
    val createdAtIso: String,
)

enum class ReminderType { MEDICINE, EXERCISE, VITALS, REPORT_FOLLOWUP, DOCTOR_CHECKUP }
enum class AlarmFrequency { DAILY, TWICE_DAILY, WEEKLY, ONE_TIME }

@JsonClass(generateAdapter = true)
data class StoredAlarm(
    val id: String,
    val patientId: String,
    val patientName: String,
    val reminderType: ReminderType = ReminderType.MEDICINE,
    val medicineLabel: String,
    val time24h: String,
    val secondTime24h: String? = null,
    val frequency: AlarmFrequency = AlarmFrequency.DAILY,
    val startDate: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val lastTakenDate: String? = null,
    val takenCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class StoredLabRecord(
    val id: String,
    val patientId: String,
    val scannedAtIso: String,
    val hemoglobin: String? = null,
    val wbc: String? = null,
    val platelets: String? = null,
    val fastingGlucose: String? = null,
    val hba1c: String? = null,
    val creatinine: String? = null,
    val cholesterol: String? = null,
    val altSgpt: String? = null,
    val sourceFileType: String = "MANUAL",
    val excerpt: String = "",
)

@JsonClass(generateAdapter = true)
data class AccountRecord(
    val accountId: String,
    val displayName: String,
    val workspace: WorkspaceMode,
    val pinSaltBase64: String,
    val pinVerifierBase64: String,
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long = 0L,
    val createdAtIso: String,
)

@JsonClass(generateAdapter = true)
data class AuditLogEntry(
    val id: String,
    val accountId: String,
    val action: String,
    val detail: String,
    val timestampIso: String,
)

@JsonClass(generateAdapter = true)
data class EncryptedEnvelope(
    val ivBase64: String,
    val cipherBase64: String,
)

@JsonClass(generateAdapter = true)
data class BackupPayloadItem(
    val kind: String,
    val id: String,
    val updatedAtIso: String,
    val payloadJson: String,
)

@JsonClass(generateAdapter = true)
data class EncryptedBackupFile(
    val format: String = "TIBB_ENCRYPTED_BACKUP_V1",
    val accountDisplayName: String,
    val workspace: WorkspaceMode,
    val exportedAtIso: String,
    val saltBase64: String,
    val ivBase64: String,
    val cipherBase64: String,
)
