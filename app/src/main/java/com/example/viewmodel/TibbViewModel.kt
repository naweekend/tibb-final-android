package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BundledDataLoader
import com.example.data.local.PreferencesRepository
import com.example.data.local.TibbDatabase
import com.example.data.local.UnlockedVaultSession
import com.example.data.local.VaultRepository
import com.example.data.model.AccountRecord
import com.example.data.model.AlarmFrequency
import com.example.data.model.AppLocale
import com.example.data.model.AuditLogEntry
import com.example.data.model.ChatThread
import com.example.data.model.DailyDoctorCheckIn
import com.example.data.model.DailyWellnessLog
import com.example.data.model.EpisodeInput
import com.example.data.model.EpisodeVitals
import com.example.data.model.GeoCoords
import com.example.data.model.MedicalGuidanceAssessment
import com.example.data.model.PatientProfile
import com.example.data.model.PersonalizedWellnessPlan
import com.example.data.model.ReminderType
import com.example.data.model.SavedPersonReport
import com.example.data.model.SavedReportKind
import com.example.data.model.StoredAlarm
import com.example.data.model.StoredEpisode
import com.example.data.model.StoredLabRecord
import com.example.data.model.StructuredAssessmentInput
import com.example.data.model.TriageEvaluationResult
import com.example.data.model.TriageTier
import com.example.data.model.UnitSystem
import com.example.data.model.WellnessGoal
import com.example.data.model.WorkspaceMode
import com.example.engine.AndroidSensorsManager
import com.example.engine.AudioFeedbackManager
import com.example.engine.HealthMath
import com.example.engine.LocalMedicalGuidanceEngine
import com.example.engine.RemediesAndPlans
import com.example.engine.TriageEngine
import com.example.worker.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class AppEntryStage {
    BOOTING,
    AUTH_LOCK,
    LANDING,
    WORKSPACE,
}

enum class MainNavTab {
    HOME,
    CHECKUP_CHAT,
    COACH,
    VITALS,
    ALARMS_LABS,
    RECORDS,
    QUEUE,
    ANDROID_KIT,
}

data class TibbUiState(
    val entryStage: AppEntryStage = AppEntryStage.BOOTING,
    val darkMode: Boolean = true,
    val locale: AppLocale = AppLocale.EN,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val workspaceMode: WorkspaceMode = WorkspaceMode.GHARANA,
    val session: UnlockedVaultSession? = null,
    val customAccounts: List<AccountRecord> = emptyList(),
    val authError: String? = null,
    val authBusy: Boolean = false,
    val activeTab: MainNavTab = MainNavTab.HOME,
    val checkupSubMode: Int = 0, // 0 = Offline Structured Symptom Checker, 1 = Offline Medical Library & Scope
    val alarmsSubMode: Int = 0, // 0 = Medicine Alarms, 1 = Lab OCR
    val showOnboardingModal: Boolean = false,
    val editingProfile: PatientProfile? = null,
    val showPinUpgradeSheet: Boolean = false,
    val showLegalModal: Boolean = false,
    val profiles: List<PatientProfile> = emptyList(),
    val activePersonId: String? = null,
    val episodes: List<StoredEpisode> = emptyList(),
    val alarms: List<StoredAlarm> = emptyList(),
    val labs: List<StoredLabRecord> = emptyList(),
    val wellnessLogs: List<DailyWellnessLog> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val savedReports: List<SavedPersonReport> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val activeEvaluation: TriageEvaluationResult? = null,
    val activeGuidanceAssessment: MedicalGuidanceAssessment? = null,
    val activeThreadId: String? = null,
    val userGpsCoords: GeoCoords? = null,
    val gpsLocating: Boolean = false,
    val gpsError: String? = null,
    val sensorStepsActive: Boolean = false,
    val stepSensitivityHigh: Boolean = false,
    val toastMessage: String? = null,
) {
    val activePerson: PatientProfile?
        get() = profiles.firstOrNull { it.id == activePersonId } ?: profiles.firstOrNull()
}

class TibbViewModel(application: Application) : AndroidViewModel(application) {

    val dataLoader = BundledDataLoader(application)
    val triageEngine = TriageEngine(dataLoader)
    val guidanceEngine = LocalMedicalGuidanceEngine(dataLoader, triageEngine)
    val audioManager = AudioFeedbackManager(application)
    private val prefsRepo = PreferencesRepository(application)
    private val db = TibbDatabase.getInstance(application)
    private val vaultRepo = VaultRepository(application, db.tibbDao(), dataLoader.moshi)

    private val _uiState = MutableStateFlow(TibbUiState())
    val uiState: StateFlow<TibbUiState> = _uiState.asStateFlow()

    private var unregisterStepSensor: (() -> Unit)? = null

    init {
        NotificationHelper.ensureChannel(application)
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            val customAccounts = vaultRepo.listCustomAccounts()
            _uiState.update {
                it.copy(
                    darkMode = prefs.darkMode,
                    locale = prefs.locale,
                    unitSystem = prefs.unitSystem,
                    workspaceMode = prefs.workspaceMode,
                    activePersonId = prefs.activePersonId,
                    customAccounts = customAccounts,
                )
            }
            if (customAccounts.isNotEmpty()) {
                _uiState.update { it.copy(entryStage = AppEntryStage.AUTH_LOCK) }
            } else {
                val defaultSession = vaultRepo.openOrCreateDefaultSession(prefs.workspaceMode)
                loadVaultDataIntoState(defaultSession)
                _uiState.update {
                    it.copy(
                        session = defaultSession,
                        entryStage = if (prefs.hasEnteredWorkspace && it.profiles.isNotEmpty()) {
                            AppEntryStage.WORKSPACE
                        } else {
                            AppEntryStage.LANDING
                        },
                    )
                }
            }
        }
    }

    private fun todayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

    private suspend fun loadVaultDataIntoState(session: UnlockedVaultSession) {
        val profiles = vaultRepo.loadProfiles(session)
        val episodes = vaultRepo.loadEpisodes(session)
        val alarms = vaultRepo.loadAlarms(session)
        val labs = vaultRepo.loadLabs(session)
        val wellness = vaultRepo.loadWellnessLogs(session)
        val chats = vaultRepo.loadChatThreads(session)
        val reports = vaultRepo.loadSavedReports(session)
        val audits = vaultRepo.getAuditLogs(session.accountId)

        val currentActiveId = _uiState.value.activePersonId
        val resolvedActiveId = profiles.firstOrNull { it.id == currentActiveId }?.id ?: profiles.firstOrNull()?.id
        val firstThreadForPerson = chats.firstOrNull { it.personId == resolvedActiveId }?.id

        _uiState.update {
            it.copy(
                session = session,
                workspaceMode = session.workspace,
                profiles = profiles,
                activePersonId = resolvedActiveId,
                episodes = episodes,
                alarms = alarms,
                labs = labs,
                wellnessLogs = wellness,
                chatThreads = chats,
                savedReports = reports,
                auditLogs = audits,
                activeThreadId = firstThreadForPerson,
            )
        }
    }

    fun setLocale(locale: AppLocale) {
        _uiState.update { it.copy(locale = locale) }
        viewModelScope.launch { prefsRepo.setLocale(locale) }
    }

    fun toggleDarkMode() {
        val next = !_uiState.value.darkMode
        _uiState.update { it.copy(darkMode = next) }
        viewModelScope.launch { prefsRepo.setDarkMode(next) }
    }

    fun setUnitSystem(unit: UnitSystem) {
        _uiState.update { it.copy(unitSystem = unit) }
        viewModelScope.launch { prefsRepo.setUnitSystem(unit) }
    }

    fun setWorkspaceMode(mode: WorkspaceMode) {
        _uiState.update { state ->
            val nextTab = if (mode == WorkspaceMode.GHARANA && state.activeTab == MainNavTab.QUEUE) {
                MainNavTab.HOME
            } else state.activeTab
            state.copy(workspaceMode = mode, activeTab = nextTab)
        }
        viewModelScope.launch { prefsRepo.setWorkspaceMode(mode) }
    }

    fun setActiveTab(tab: MainNavTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setCheckupSubMode(mode: Int) {
        _uiState.update { it.copy(activeTab = MainNavTab.CHECKUP_CHAT, checkupSubMode = mode) }
    }

    fun setAlarmsSubMode(mode: Int) {
        _uiState.update { it.copy(activeTab = MainNavTab.ALARMS_LABS, alarmsSubMode = mode) }
    }

    fun enterWorkspaceFromLanding(openOnboardingIfEmpty: Boolean = true) {
        viewModelScope.launch {
            prefsRepo.setHasEnteredWorkspace(true)
            val needsOnboarding = openOnboardingIfEmpty && _uiState.value.profiles.isEmpty()
            _uiState.update {
                it.copy(
                    entryStage = AppEntryStage.WORKSPACE,
                    showOnboardingModal = needsOnboarding,
                    editingProfile = null,
                )
            }
        }
    }

    fun openLandingOverview() {
        _uiState.update { it.copy(entryStage = AppEntryStage.LANDING) }
    }

    fun lockVaultToAuthScreen() {
        stopStepSensor()
        viewModelScope.launch {
            val custom = vaultRepo.listCustomAccounts()
            _uiState.update {
                it.copy(
                    customAccounts = custom,
                    entryStage = AppEntryStage.AUTH_LOCK,
                    authError = null,
                )
            }
        }
    }

    fun createPinAccount(displayName: String, pin: String, workspace: WorkspaceMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(authBusy = true, authError = null) }
            val result = vaultRepo.createPinAccount(displayName, pin, workspace)
            result.fold(
                onSuccess = { session ->
                    val custom = vaultRepo.listCustomAccounts()
                    loadVaultDataIntoState(session)
                    prefsRepo.setHasEnteredWorkspace(true)
                    _uiState.update {
                        it.copy(
                            authBusy = false,
                            customAccounts = custom,
                            entryStage = AppEntryStage.WORKSPACE,
                            showOnboardingModal = it.profiles.isEmpty(),
                        )
                    }
                    audioManager.playClinicalChime("success")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(authBusy = false, authError = err.message ?: "Unable to create vault") }
                },
            )
        }
    }

    fun unlockPinAccount(accountId: String, pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authBusy = true, authError = null) }
            val result = vaultRepo.unlockPinAccount(accountId, pin)
            result.fold(
                onSuccess = { session ->
                    loadVaultDataIntoState(session)
                    prefsRepo.setHasEnteredWorkspace(true)
                    _uiState.update {
                        it.copy(
                            authBusy = false,
                            entryStage = AppEntryStage.WORKSPACE,
                            showOnboardingModal = it.profiles.isEmpty(),
                        )
                    }
                    audioManager.playClinicalChime("success")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(authBusy = false, authError = err.message ?: "Incorrect PIN") }
                },
            )
        }
    }

    fun openDefaultQuickAccess() {
        viewModelScope.launch {
            val session = vaultRepo.openOrCreateDefaultSession(_uiState.value.workspaceMode)
            loadVaultDataIntoState(session)
            prefsRepo.setHasEnteredWorkspace(true)
            _uiState.update {
                it.copy(
                    entryStage = AppEntryStage.WORKSPACE,
                    showOnboardingModal = it.profiles.isEmpty(),
                )
            }
        }
    }

    fun setShowPinUpgradeSheet(show: Boolean) {
        _uiState.update { it.copy(showPinUpgradeSheet = show, authError = null) }
    }

    fun upgradeToPinVault(displayName: String, pin: String, workspace: WorkspaceMode) {
        val currentSession = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(authBusy = true, authError = null) }
            val res = vaultRepo.upgradeDefaultToPin(currentSession, displayName, pin, workspace)
            res.fold(
                onSuccess = { newSession ->
                    val custom = vaultRepo.listCustomAccounts()
                    loadVaultDataIntoState(newSession)
                    _uiState.update {
                        it.copy(
                            authBusy = false,
                            customAccounts = custom,
                            showPinUpgradeSheet = false,
                            toastMessage = "PIN vault enabled & data encrypted.",
                        )
                    }
                    audioManager.playClinicalChime("success")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(authBusy = false, authError = err.message) }
                },
            )
        }
    }

    fun setShowLegalModal(show: Boolean) {
        _uiState.update { it.copy(showLegalModal = show) }
    }

    fun openAddPersonOnboarding() {
        _uiState.update { it.copy(showOnboardingModal = true, editingProfile = null) }
    }

    fun openEditActivePerson() {
        val active = _uiState.value.activePerson ?: return
        _uiState.update { it.copy(showOnboardingModal = true, editingProfile = active) }
    }

    fun closeOnboardingModal() {
        _uiState.update { it.copy(showOnboardingModal = false, editingProfile = null) }
    }

    fun selectActivePerson(personId: String) {
        viewModelScope.launch { prefsRepo.setActivePersonId(personId) }
        _uiState.update { state ->
            val threadId = state.chatThreads.firstOrNull { it.personId == personId }?.id
            state.copy(
                activePersonId = personId,
                activeThreadId = threadId,
                activeGuidanceAssessment = null,
                activeEvaluation = null,
            )
        }
    }

    fun savePersonProfile(profile: PatientProfile) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            val normalized = profile.copy(
                accountId = session.accountId,
                createdAtIso = profile.createdAtIso.ifBlank { nowIso() },
                baselineWeightKg = profile.baselineWeightKg ?: profile.weightKg,
            )
            vaultRepo.saveProfile(session, normalized)
            val profiles = vaultRepo.loadProfiles(session)
            val audits = vaultRepo.getAuditLogs(session.accountId)
            prefsRepo.setActivePersonId(normalized.id)
            prefsRepo.setUnitSystem(normalized.preferredUnitSystem)
            _uiState.update {
                it.copy(
                    profiles = profiles,
                    activePersonId = normalized.id,
                    unitSystem = normalized.preferredUnitSystem,
                    showOnboardingModal = false,
                    editingProfile = null,
                    auditLogs = audits,
                    entryStage = AppEntryStage.WORKSPACE,
                )
            }
            audioManager.playClinicalChime("success")
        }
    }

    fun getOrCreateTodayWellness(person: PatientProfile): DailyWellnessLog {
        val key = todayDateKey()
        val id = "well_${person.id}_$key"
        return _uiState.value.wellnessLogs.firstOrNull { it.id == id }
            ?: run {
                val bmi = RemediesAndPlans.computeBmiAssessment(person.weightKg, person.heightCm, person.ageMonths)
                DailyWellnessLog(
                    id = id,
                    personId = person.id,
                    dateKey = key,
                    steps = 0,
                    stepGoal = bmi.recommendedSteps,
                    waterMl = 0,
                    waterGoalMl = bmi.recommendedWaterMl,
                    sleepHours = 7.0,
                    updatedAtIso = nowIso(),
                )
            }
    }

    fun updateWellnessLog(log: DailyWellnessLog) {
        val session = _uiState.value.session ?: return
        val updated = log.copy(updatedAtIso = nowIso())
        viewModelScope.launch {
            vaultRepo.saveWellnessLog(session, updated)
            val list = vaultRepo.loadWellnessLogs(session)
            _uiState.update { it.copy(wellnessLogs = list) }
        }
    }

    fun submitDailyCheckIn(checkIn: DailyDoctorCheckIn) {
        val person = _uiState.value.activePerson ?: return
        val currentLog = getOrCreateTodayWellness(person)
        val updatedLog = currentLog.copy(
            sleepHours = checkIn.sleepHours ?: currentLog.sleepHours,
            dailyCheckIn = checkIn,
            updatedAtIso = nowIso(),
        )
        updateWellnessLog(updatedLog)
        if (checkIn.currentWeightKg != null && checkIn.currentWeightKg > 2.0) {
            savePersonProfile(person.copy(weightKg = checkIn.currentWeightKg))
        }
    }

    fun generateWellnessPlan(goal: WellnessGoal, variationSeed: Int = 0) {
        val person = _uiState.value.activePerson ?: return
        val plan = RemediesAndPlans.buildPersonalizedPlanOffline(person, goal, variationSeed)
        val currentLog = getOrCreateTodayWellness(person)
        updateWellnessLog(
            currentLog.copy(
                plan = plan,
                stepGoal = plan.dailyStepsGoal,
                waterGoalMl = plan.dailyWaterGoalMl,
            )
        )
        if (person.preferredGoal != goal) {
            savePersonProfile(person.copy(preferredGoal = goal))
        }
    }

    fun updateActiveWellnessPlan(plan: PersonalizedWellnessPlan) {
        val person = _uiState.value.activePerson ?: return
        val currentLog = getOrCreateTodayWellness(person)
        updateWellnessLog(currentLog.copy(plan = plan))
    }

    fun toggleStepSensor(sensitive: Boolean? = null) {
        val nextSensitive = sensitive ?: _uiState.value.stepSensitivityHigh
        if (_uiState.value.sensorStepsActive && sensitive == null) {
            stopStepSensor()
        } else {
            stopStepSensor()
            unregisterStepSensor = AndroidSensorsManager.registerAccelerometerStepDetector(
                context = getApplication(),
                sensitive = nextSensitive,
                onStepDetected = {
                    val person = _uiState.value.activePerson ?: return@registerAccelerometerStepDetector
                    val log = getOrCreateTodayWellness(person)
                    updateWellnessLog(log.copy(steps = log.steps + 1))
                },
            )
            _uiState.update { it.copy(sensorStepsActive = true, stepSensitivityHigh = nextSensitive) }
        }
    }

    private fun stopStepSensor() {
        unregisterStepSensor?.invoke()
        unregisterStepSensor = null
        _uiState.update { it.copy(sensorStepsActive = false) }
    }

    fun completeTriageEpisode(input: EpisodeInput, result: TriageEvaluationResult) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            val topFacility = result.recommendedFacilities.firstOrNull()
            val ep = StoredEpisode(
                id = "ep_${UUID.randomUUID().toString().take(10)}",
                patientId = input.patient.id,
                patientName = input.patient.name,
                ageBracket = result.referralSlip.ageBracket,
                triageTier = result.tier,
                symptomCategory = result.symptomCategory,
                redFlags = result.redFlags.map { it.label_en },
                facilityId = topFacility?.id ?: "LOCATION_PENDING",
                facilityName = topFacility?.name_en ?: "Select District / GPS",
                referralToken = result.referralSlip.referralToken,
                vitals = input.vitals,
                createdAtIso = nowIso(),
            )
            vaultRepo.saveEpisode(session, ep)

            // Sync latest HR/BP/Sugar to today's wellness log
            val currentLog = getOrCreateTodayWellness(input.patient)
            val syncedLog = currentLog.copy(
                restingHr = input.vitals.heartRate ?: currentLog.restingHr,
                lastBpSystolic = input.vitals.bpSystolic ?: currentLog.lastBpSystolic,
                lastBpDiastolic = input.vitals.bpDiastolic ?: currentLog.lastBpDiastolic,
                lastSugarMgDl = input.vitals.bloodGlucoseMgDl ?: currentLog.lastSugarMgDl,
            )
            vaultRepo.saveWellnessLog(session, syncedLog)

            val episodes = vaultRepo.loadEpisodes(session)
            val wellness = vaultRepo.loadWellnessLogs(session)
            val audits = vaultRepo.getAuditLogs(session.accountId)
            _uiState.update {
                it.copy(
                    episodes = episodes,
                    wellnessLogs = wellness,
                    auditLogs = audits,
                    activeEvaluation = result,
                )
            }
            if (result.tier == TriageTier.RED) {
                audioManager.playClinicalChime("red_sos")
            } else {
                audioManager.playClinicalChime("success")
            }
        }
    }

    fun completeStructuredAssessment(
        input: StructuredAssessmentInput,
        assessment: MedicalGuidanceAssessment,
    ) {
        val (compatInput, compatResult) = guidanceEngine.toCompatibilityEvaluation(input, assessment)
        if (assessment.outcome == com.example.data.model.AssessmentOutcome.INVALID_OR_CONFLICTING) {
            _uiState.update { it.copy(activeGuidanceAssessment = assessment) }
            return
        }
        _uiState.update { it.copy(activeGuidanceAssessment = assessment) }
        completeTriageEpisode(compatInput, compatResult)
    }

    fun clearGuidanceAssessment() {
        _uiState.update { it.copy(activeGuidanceAssessment = null, activeEvaluation = null) }
    }

    fun clearActiveEvaluation() {
        _uiState.update { it.copy(activeEvaluation = null, activeGuidanceAssessment = null) }
    }

    fun verifyReferralEpisode(episodeId: String) {
        val session = _uiState.value.session ?: return
        val target = _uiState.value.episodes.firstOrNull { it.id == episodeId } ?: return
        viewModelScope.launch {
            vaultRepo.saveEpisode(session, target.copy(referralCompleted = true))
            val episodes = vaultRepo.loadEpisodes(session)
            _uiState.update { it.copy(episodes = episodes) }
            audioManager.playClinicalChime("success")
        }
    }

    fun logVitalsToActivePerson(vitals: EpisodeVitals, label: String) {
        val session = _uiState.value.session ?: return
        val person = _uiState.value.activePerson ?: return
        viewModelScope.launch {
            val currentLog = getOrCreateTodayWellness(person)
            val updated = currentLog.copy(
                restingHr = vitals.heartRate ?: currentLog.restingHr,
                lastBpSystolic = vitals.bpSystolic ?: currentLog.lastBpSystolic,
                lastBpDiastolic = vitals.bpDiastolic ?: currentLog.lastBpDiastolic,
                lastSugarMgDl = vitals.bloodGlucoseMgDl ?: currentLog.lastSugarMgDl,
            )
            vaultRepo.saveWellnessLog(session, updated)
            vaultRepo.savePersonReport(
                session,
                SavedPersonReport(
                    id = "rep_${UUID.randomUUID().toString().take(10)}",
                    personId = person.id,
                    kind = SavedReportKind.VITALS_LOG,
                    title = label,
                    notes = buildString {
                        vitals.bpSystolic?.let { append("BP $it/${vitals.bpDiastolic ?: "--"} mmHg ") }
                        vitals.bloodGlucoseMgDl?.let { append("Sugar $it mg/dL ") }
                        vitals.heartRate?.let { append("HR $it bpm ") }
                        vitals.respiratoryRate?.let { append("RR $it/min") }
                    }.trim(),
                    vitalsSnapshot = vitals,
                    createdAtIso = nowIso(),
                ),
            )
            _uiState.update {
                it.copy(
                    wellnessLogs = vaultRepo.loadWellnessLogs(session),
                    savedReports = vaultRepo.loadSavedReports(session),
                    toastMessage = "Vitals logged to ${person.name}.",
                )
            }
            audioManager.playClinicalChime("success")
        }
    }

    fun saveQuickPersonReport(kind: SavedReportKind, title: String, notes: String, imageUri: Uri? = null) {
        val session = _uiState.value.session ?: return
        val person = _uiState.value.activePerson ?: return
        viewModelScope.launch {
            val savedPath = imageUri?.let {
                AndroidSensorsManager.stripExifAndSaveToVault(getApplication(), session.accountId, it).getOrNull()
            }
            vaultRepo.savePersonReport(
                session,
                SavedPersonReport(
                    id = "rep_${UUID.randomUUID().toString().take(10)}",
                    personId = person.id,
                    kind = kind,
                    title = title.trim().ifEmpty { kind.name },
                    notes = notes.trim(),
                    imagePath = savedPath,
                    createdAtIso = nowIso(),
                ),
            )
            _uiState.update {
                it.copy(
                    savedReports = vaultRepo.loadSavedReports(session),
                    toastMessage = "Saved to ${person.name}'s vault.",
                )
            }
            audioManager.playClinicalChime("success")
        }
    }

    fun addAlarm(
        patientId: String,
        reminderType: ReminderType,
        label: String,
        time24h: String,
        secondTime24h: String?,
        frequency: AlarmFrequency,
        notes: String?,
    ) {
        val session = _uiState.value.session ?: return
        val patient = _uiState.value.profiles.firstOrNull { it.id == patientId } ?: _uiState.value.activePerson
        val alarm = StoredAlarm(
            id = "alm_${UUID.randomUUID().toString().take(10)}",
            patientId = patient?.id ?: patientId,
            patientName = patient?.name ?: "Family Member",
            reminderType = reminderType,
            medicineLabel = label.trim(),
            time24h = time24h,
            secondTime24h = if (frequency == AlarmFrequency.TWICE_DAILY) secondTime24h else null,
            frequency = frequency,
            notes = notes?.trim()?.ifEmpty { null },
            active = true,
        )
        viewModelScope.launch {
            vaultRepo.saveAlarm(session, alarm)
            NotificationHelper.scheduleAlarmWork(getApplication(), alarm)
            val alarms = vaultRepo.loadAlarms(session)
            _uiState.update { it.copy(alarms = alarms, toastMessage = "Alarm scheduled offline.") }
            audioManager.playClinicalChime("alarm")
        }
    }

    fun markAlarmTakenToday(alarmId: String) {
        val session = _uiState.value.session ?: return
        val existing = _uiState.value.alarms.firstOrNull { it.id == alarmId } ?: return
        val today = todayDateKey()
        if (existing.lastTakenDate == today) return
        viewModelScope.launch {
            vaultRepo.saveAlarm(
                session,
                existing.copy(lastTakenDate = today, takenCount = existing.takenCount + 1),
            )
            _uiState.update { it.copy(alarms = vaultRepo.loadAlarms(session)) }
            audioManager.playClinicalChime("success")
        }
    }

    fun saveLabRecord(record: StoredLabRecord) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            vaultRepo.saveLab(session, record)
            _uiState.update {
                it.copy(
                    labs = vaultRepo.loadLabs(session),
                    auditLogs = vaultRepo.getAuditLogs(session.accountId),
                    toastMessage = "Lab report saved encrypted.",
                )
            }
            audioManager.playClinicalChime("success")
        }
    }

    fun requestCurrentGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(gpsLocating = true, gpsError = null) }
            val coords = AndroidSensorsManager.fetchCurrentLocation(getApplication())
            _uiState.update {
                if (coords != null) {
                    it.copy(gpsLocating = false, userGpsCoords = coords, gpsError = null)
                } else {
                    it.copy(gpsLocating = false, gpsError = "GPS unavailable indoors. Choose a district or saved place.")
                }
            }
        }
    }

    fun exportWeeklyDigestPdf() {
        val state = _uiState.value
        val lines = AndroidSensorsManager.buildDigestLines(state.profiles, state.episodes, state.labs, state.alarms)
        AndroidSensorsManager.exportAndSharePdfReport(
            context = getApplication(),
            title = "Tibb Weekly Family Health Digest",
            lines = lines,
            fileNamePrefix = "tibb_weekly_digest",
        ).onFailure { err ->
            _uiState.update { it.copy(toastMessage = err.message ?: "PDF export failed") }
        }
    }

    fun exportActivePersonSummaryPdf() {
        val state = _uiState.value
        val person = state.activePerson ?: return
        val lines = AndroidSensorsManager.buildPersonSummaryLines(
            person = person,
            episodes = state.episodes.filter { it.patientId == person.id },
            labs = state.labs.filter { it.patientId == person.id },
            alarms = state.alarms.filter { it.patientId == person.id },
            reports = state.savedReports.filter { it.personId == person.id },
        )
        AndroidSensorsManager.exportAndSharePdfReport(
            context = getApplication(),
            title = "Tibb Medical Summary — ${person.name}",
            lines = lines,
            fileNamePrefix = "tibb_${person.name.lowercase().replace(" ", "_")}",
        ).onFailure { err ->
            _uiState.update { it.copy(toastMessage = err.message ?: "PDF export failed") }
        }
    }

    fun exportEncryptedBackup(passphrase: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            vaultRepo.exportEncryptedBackup(session, passphrase).fold(
                onSuccess = { json ->
                    AndroidSensorsManager.shareTextOrBackupFile(
                        context = getApplication(),
                        fileName = "tibb-backup-${session.accountId}.tibb-backup",
                        content = json,
                    )
                    _uiState.update {
                        it.copy(
                            auditLogs = vaultRepo.getAuditLogs(session.accountId),
                            toastMessage = "Encrypted backup prepared.",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(toastMessage = err.message ?: "Backup failed") }
                },
            )
        }
    }

    fun restoreEncryptedBackup(backupJson: String, passphrase: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            vaultRepo.restoreEncryptedBackup(session, backupJson, passphrase).fold(
                onSuccess = { count ->
                    loadVaultDataIntoState(session)
                    _uiState.update { it.copy(toastMessage = "Restored $count encrypted records.") }
                    audioManager.playClinicalChime("success")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(toastMessage = err.message ?: "Restore failed") }
                },
            )
        }
    }

    fun wipeActiveAccount() {
        val session = _uiState.value.session ?: return
        stopStepSensor()
        viewModelScope.launch {
            vaultRepo.wipeAccountData(session.accountId)
            val remaining = vaultRepo.listCustomAccounts()
            if (remaining.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        customAccounts = remaining,
                        session = null,
                        profiles = emptyList(),
                        episodes = emptyList(),
                        alarms = emptyList(),
                        labs = emptyList(),
                        wellnessLogs = emptyList(),
                        chatThreads = emptyList(),
                        savedReports = emptyList(),
                        entryStage = AppEntryStage.AUTH_LOCK,
                        toastMessage = "Account permanently wiped.",
                    )
                }
            } else {
                val fresh = vaultRepo.openOrCreateDefaultSession(WorkspaceMode.GHARANA)
                loadVaultDataIntoState(fresh)
                prefsRepo.setHasEnteredWorkspace(false)
                _uiState.update {
                    it.copy(
                        customAccounts = emptyList(),
                        entryStage = AppEntryStage.LANDING,
                        toastMessage = "All vault data wiped.",
                    )
                }
            }
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun openEmergencyDialer(number: String = "1122") {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_DIAL,
            Uri.parse("tel:$number"),
        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
        getApplication<Application>().startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        stopStepSensor()
        audioManager.shutdown()
    }
}
