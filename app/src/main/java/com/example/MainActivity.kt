package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLocale
import com.example.ui.components.LegalComplianceDialog
import com.example.ui.components.PersonSwitcherStrip
import com.example.ui.components.PinUpgradeDialog
import com.example.ui.components.SettingsDrawerSheet
import com.example.ui.components.TibbBottomBar
import com.example.ui.components.TibbFullHelpSheet
import com.example.ui.components.TibbNavigationRail
import com.example.ui.components.TibbTopBar
import com.example.ui.screens.AlarmsAndLabsScreen
import com.example.ui.screens.AndroidKitScreen
import com.example.ui.screens.AuthLockScreen
import com.example.ui.screens.BhuQueueScreen
import com.example.ui.screens.CheckupAndChatScreen
import com.example.ui.screens.HomeHubDashboardScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.PersonOnboardingDialog
import com.example.ui.screens.PersonRecordsVaultScreen
import com.example.ui.screens.VitalsEstimatorScreen
import com.example.ui.screens.WellnessCoachScreen
import com.example.ui.theme.TibbGradientScaffoldBackground
import com.example.ui.theme.TibbTheme
import com.example.viewmodel.AppEntryStage
import com.example.viewmodel.MainNavTab
import com.example.viewmodel.TibbViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val viewModel: TibbViewModel = viewModel()
            TibbAppRoot(
                viewModel = viewModel,
                isWideScreen = widthSizeClass != WindowWidthSizeClass.Compact,
            )
        }
    }
}

@Composable
private fun TibbAppRoot(
    viewModel: TibbViewModel,
    isWideScreen: Boolean,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPinProtected = state.session?.let { sess ->
        state.customAccounts.any { it.accountId == sess.accountId }
    } == true
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showFullHelpSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    TibbTheme(
        darkTheme = state.darkMode,
        isUrduRtl = state.locale == AppLocale.UR,
    ) {
        TibbGradientScaffoldBackground {
            when (state.entryStage) {
                AppEntryStage.BOOTING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                AppEntryStage.AUTH_LOCK -> {
                    AuthLockScreen(
                        locale = state.locale,
                        accounts = state.customAccounts,
                        authBusy = state.authBusy,
                        authError = state.authError,
                        onSetLocale = viewModel::setLocale,
                        onCreateAccount = viewModel::createPinAccount,
                        onUnlockAccount = viewModel::unlockPinAccount,
                        onQuickGuestVault = viewModel::openDefaultQuickAccess,
                        onOpenHelp = { showFullHelpSheet = true },
                        onOpenLegal = { viewModel.setShowLegalModal(true) },
                    )
                }

                AppEntryStage.LANDING -> {
                    LandingScreen(
                        locale = state.locale,
                        darkMode = state.darkMode,
                        hasProfiles = state.profiles.isNotEmpty(),
                        onSetLocale = viewModel::setLocale,
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onStartWorkspace = { viewModel.enterWorkspaceFromLanding(openOnboardingIfEmpty = true) },
                        onOpenAuthVault = { viewModel.lockVaultToAuthScreen() },
                        onOpenHelp = { showFullHelpSheet = true },
                        onOpenLegal = { viewModel.setShowLegalModal(true) },
                    )
                }

                AppEntryStage.WORKSPACE -> {
                    BackHandler(enabled = state.activeTab != MainNavTab.HOME) {
                        viewModel.setActiveTab(MainNavTab.HOME)
                    }

                    val activePerson = state.activePerson
                    val todayLog = remember(activePerson, state.wellnessLogs) {
                        activePerson?.let { viewModel.getOrCreateTodayWellness(it) }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TibbTopBar(
                                locale = state.locale,
                                isPinProtected = isPinProtected,
                                onCallSos = { viewModel.openEmergencyDialer("1122") },
                                onOpenHelp = { showFullHelpSheet = true },
                                onOpenSettings = { showSettingsDrawer = true },
                                onOpenRecords = { viewModel.setActiveTab(MainNavTab.RECORDS) },
                                onLockOrPin = {
                                    if (isPinProtected) viewModel.lockVaultToAuthScreen()
                                    else viewModel.setShowPinUpgradeSheet(true)
                                },
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen) {
                                TibbBottomBar(
                                    activeTab = state.activeTab,
                                    locale = state.locale,
                                    workspaceMode = state.workspaceMode,
                                    onSelectTab = viewModel::setActiveTab,
                                )
                            }
                        },
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                                .imePadding(),
                        ) {
                            if (isWideScreen) {
                                TibbNavigationRail(
                                    activeTab = state.activeTab,
                                    locale = state.locale,
                                    workspaceMode = state.workspaceMode,
                                    onSelectTab = viewModel::setActiveTab,
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
                                ) {
                                    PersonSwitcherStrip(
                                        profiles = state.profiles,
                                        activePerson = activePerson,
                                        locale = state.locale,
                                        onSelectPerson = viewModel::selectActivePerson,
                                        onAddPerson = viewModel::openAddPersonOnboarding,
                                        onOpenRecords = { viewModel.setActiveTab(MainNavTab.RECORDS) },
                                    )

                                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                        when (state.activeTab) {
                                            MainNavTab.HOME -> HomeHubDashboardScreen(
                                                locale = state.locale,
                                                unitSystem = state.unitSystem,
                                                profiles = state.profiles,
                                                activePerson = activePerson,
                                                todayLog = todayLog,
                                                alarms = state.alarms,
                                                triageEngine = viewModel.triageEngine,
                                                userGpsCoords = state.userGpsCoords,
                                                gpsLocating = state.gpsLocating,
                                                sensorStepsActive = state.sensorStepsActive,
                                                stepSensitivityHigh = state.stepSensitivityHigh,
                                                onSelectPerson = viewModel::selectActivePerson,
                                                onAddPerson = viewModel::openAddPersonOnboarding,
                                                onEditPerson = viewModel::openEditActivePerson,
                                                onUpdateWellness = viewModel::updateWellnessLog,
                                                onSubmitCheckIn = viewModel::submitDailyCheckIn,
                                                onToggleStepSensor = viewModel::toggleStepSensor,
                                                onMarkAlarmTaken = viewModel::markAlarmTakenToday,
                                                onRequestGps = viewModel::requestCurrentGpsLocation,
                                                onOpenDialer = viewModel::openEmergencyDialer,
                                                onNavigateTab = viewModel::setActiveTab,
                                                onStartStructuredCheckup = { viewModel.setCheckupSubMode(0) },
                                            )

                                            MainNavTab.CHECKUP_CHAT -> CheckupAndChatScreen(
                                                locale = state.locale,
                                                activePerson = activePerson,
                                                checkupSubMode = state.checkupSubMode,
                                                activeGuidanceAssessment = state.activeGuidanceAssessment,
                                                guidanceEngine = viewModel.guidanceEngine,
                                                triageEngine = viewModel.triageEngine,
                                                audioManager = viewModel.audioManager,
                                                userGpsCoords = state.userGpsCoords,
                                                gpsLocating = state.gpsLocating,
                                                onSetSubMode = viewModel::setCheckupSubMode,
                                                onCompleteStructuredAssessment = viewModel::completeStructuredAssessment,
                                                onClearGuidanceAssessment = viewModel::clearGuidanceAssessment,
                                                onRequestGps = viewModel::requestCurrentGpsLocation,
                                                onOpenDialer = viewModel::openEmergencyDialer,
                                                onShowToast = viewModel::showToast,
                                                onOpenWellnessCoach = { viewModel.setActiveTab(MainNavTab.COACH) },
                                            )

                                            MainNavTab.COACH -> WellnessCoachScreen(
                                                locale = state.locale,
                                                activePerson = activePerson,
                                                todayLog = todayLog,
                                                onGeneratePlan = viewModel::generateWellnessPlan,
                                                onUpdatePlan = viewModel::updateActiveWellnessPlan,
                                                onUpdateWellness = viewModel::updateWellnessLog,
                                            )

                                            MainNavTab.VITALS -> VitalsEstimatorScreen(
                                                locale = state.locale,
                                                activePerson = activePerson,
                                                todayLog = todayLog,
                                                triageEngine = viewModel.triageEngine,
                                                audioManager = viewModel.audioManager,
                                                onLogVitals = viewModel::logVitalsToActivePerson,
                                            )

                                            MainNavTab.ALARMS_LABS -> AlarmsAndLabsScreen(
                                                locale = state.locale,
                                                alarmsSubMode = state.alarmsSubMode,
                                                profiles = state.profiles,
                                                activePerson = activePerson,
                                                alarms = state.alarms,
                                                labs = state.labs,
                                                audioManager = viewModel.audioManager,
                                                onSetSubMode = viewModel::setAlarmsSubMode,
                                                onAddAlarm = viewModel::addAlarm,
                                                onMarkAlarmTaken = viewModel::markAlarmTakenToday,
                                                onExportWeeklyPdf = viewModel::exportWeeklyDigestPdf,
                                                onSaveLabRecord = viewModel::saveLabRecord,
                                                onShowToast = viewModel::showToast,
                                            )

                                            MainNavTab.RECORDS -> PersonRecordsVaultScreen(
                                                locale = state.locale,
                                                unitSystem = state.unitSystem,
                                                activePerson = activePerson,
                                                reports = state.savedReports,
                                                episodes = state.episodes,
                                                labs = state.labs,
                                                alarms = state.alarms,
                                                onEditProfile = viewModel::openEditActivePerson,
                                                onExportPersonPdf = viewModel::exportActivePersonSummaryPdf,
                                                onSaveQuickReport = viewModel::saveQuickPersonReport,
                                                onBackHome = { viewModel.setActiveTab(MainNavTab.HOME) },
                                            )

                                            MainNavTab.QUEUE -> BhuQueueScreen(
                                                locale = state.locale,
                                                accountId = state.session?.accountId ?: "acc_local_family_default",
                                                episodes = state.episodes,
                                                onVerifyReferralEpisode = viewModel::verifyReferralEpisode,
                                                onBackHome = { viewModel.setActiveTab(MainNavTab.HOME) },
                                                onShowToast = viewModel::showToast,
                                            )

                                            MainNavTab.ANDROID_KIT -> AndroidKitScreen(
                                                locale = state.locale,
                                                auditLogs = state.auditLogs,
                                                onExportBackup = viewModel::exportEncryptedBackup,
                                                onRestoreBackup = viewModel::restoreEncryptedBackup,
                                                onWipeAccount = viewModel::wipeActiveAccount,
                                                onBackHome = { viewModel.setActiveTab(MainNavTab.HOME) },
                                                onShowToast = viewModel::showToast,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.showOnboardingModal) {
                        PersonOnboardingDialog(
                            locale = state.locale,
                            workspaceMode = state.workspaceMode,
                            initialUnitSystem = state.unitSystem,
                            editingProfile = state.editingProfile,
                            canCancel = state.profiles.isNotEmpty() || state.editingProfile != null,
                            triageEngine = viewModel.triageEngine,
                            userGpsCoords = state.userGpsCoords,
                            gpsLocating = state.gpsLocating,
                            onRequestGps = viewModel::requestCurrentGpsLocation,
                            onSaveProfile = viewModel::savePersonProfile,
                            onDismiss = viewModel::closeOnboardingModal,
                        )
                    }
                }
            }

            if (showSettingsDrawer) {
                SettingsDrawerSheet(
                    locale = state.locale,
                    darkMode = state.darkMode,
                    unitSystem = state.unitSystem,
                    workspaceMode = state.workspaceMode,
                    accountName = state.session?.displayName ?: "Local Vault",
                    isPinProtected = isPinProtected,
                    onDismiss = { showSettingsDrawer = false },
                    onSetLocale = viewModel::setLocale,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onSetUnitSystem = viewModel::setUnitSystem,
                    onSetWorkspaceMode = viewModel::setWorkspaceMode,
                    onNavigateTab = viewModel::setActiveTab,
                    onOpenPinUpgrade = { viewModel.setShowPinUpgradeSheet(true) },
                    onLockVault = viewModel::lockVaultToAuthScreen,
                    onOpenLanding = viewModel::openLandingOverview,
                    onOpenHelp = { showFullHelpSheet = true },
                    onOpenLegal = { viewModel.setShowLegalModal(true) },
                )
            }

            if (state.showPinUpgradeSheet) {
                PinUpgradeDialog(
                    locale = state.locale,
                    workspaceMode = state.workspaceMode,
                    authBusy = state.authBusy,
                    authError = state.authError,
                    onDismiss = { viewModel.setShowPinUpgradeSheet(false) },
                    onSubmit = viewModel::upgradeToPinVault,
                )
            }

            if (state.showLegalModal) {
                LegalComplianceDialog(
                    locale = state.locale,
                    onDismiss = { viewModel.setShowLegalModal(false) },
                )
            }

            if (showFullHelpSheet) {
                TibbFullHelpSheet(
                    locale = state.locale,
                    onDismiss = { showFullHelpSheet = false },
                )
            }
        }
    }
}
