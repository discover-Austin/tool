package com.tradesketch.estimator

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.ui.blueprint.calculateDockedRailOverlayInset
import com.tradesketch.estimator.ui.blueprint.calculateWorkspaceBlueprintChromeLayout
import com.tradesketch.estimator.ui.screens.BlueprintScreen
import com.tradesketch.estimator.ui.screens.FirstOpenGreetingScreen
import com.tradesketch.estimator.ui.screens.ProjectTypePlateOption
import com.tradesketch.estimator.ui.screens.ProjectTypePlateScreen
import com.tradesketch.estimator.ui.screens.ReferenceExportScreen
import com.tradesketch.estimator.ui.screens.SettingsScreen
import com.tradesketch.estimator.ui.screens.TakeoffScreen
import com.tradesketch.estimator.ui.screens.TakeoffScreenMode
import com.tradesketch.estimator.ui.screens.WelcomeHeroMode
import com.tradesketch.estimator.ui.screens.WelcomeScreenPro
import com.tradesketch.estimator.ui.tutorial.BlueprintGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.ExportGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialBlipOverlay
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialProgress
import com.tradesketch.estimator.ui.tutorial.MaterialsGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.WorkspaceRailGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.WorkspaceRailGuidedTutorialTarget
import com.tradesketch.estimator.ui.tutorial.workspaceGuidedTutorialSteps
import com.tradesketch.estimator.ui.components.WorkspaceHeaderBackButton
import com.tradesketch.estimator.ui.theme.Midnight900
import com.tradesketch.estimator.ui.theme.Midnight950
import com.tradesketch.estimator.ui.theme.Slate800
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import com.tradesketch.estimator.ui.viewmodel.OnboardingViewModel
import com.tradesketch.estimator.ui.viewmodel.ProjectsEvent
import com.tradesketch.estimator.ui.viewmodel.ProjectsViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel
import com.tradesketch.estimator.utils.resolveUniqueProjectName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPreferredOrientation()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            TradeSketchTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFDCE6F1),
                                    Midnight900,
                                    Slate800.copy(alpha = 0.96f)
                                )
                            )
                        )
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TradeSketchRoot(
                            debugStartStage = if (BuildConfig.DEBUG) {
                                intent?.getStringExtra("debug_stage")
                            } else {
                                null
                            },
                            debugStartTab = if (BuildConfig.DEBUG) {
                                intent?.getStringExtra("debug_tab")
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyPreferredOrientation()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        applyPreferredOrientation()
    }

    private fun applyPreferredOrientation() {
        val preferredOrientation = if (shouldAllowRotationForCurrentWindow()) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (requestedOrientation != preferredOrientation) {
            requestedOrientation = preferredOrientation
        }
    }

    private fun shouldAllowRotationForCurrentWindow(): Boolean {
        val config = resources.configuration
        return config.smallestScreenWidthDp >= 600 || isInMultiWindowMode
    }
}

internal enum class RootStage {
    GREETING,
    WELCOME,
    RITUAL,
    TUTORIAL,
    WORKSPACE
}

private val WorkspaceRailShell = Color(0xFFE3EBF2)
private val WorkspaceRailSurface = Color(0xFFFBFDFE)
private val WorkspaceRailAccent = Color(0xFF1F628D)
private val WorkspaceRailAccentBright = Color(0xFF2E7CAC)
private val WorkspaceRailAccentBorder = Color(0xFF869BAC)
private val WorkspaceRailTextPrimary = Color(0xFF162532)
private val WorkspaceRailTextOnAccent = Color(0xFFFFFFFF)

internal fun resolveRootStage(
    currentStage: RootStage,
    debugStageOverride: RootStage?,
    debugWorkspaceOverride: Boolean,
    firstRun: Boolean,
    firstOpenGreetingDismissed: Boolean,
    forceTutorial: Boolean,
    forceWorkspace: Boolean
): RootStage {
    return when {
        debugStageOverride != null -> debugStageOverride
        debugWorkspaceOverride -> RootStage.WORKSPACE
        forceTutorial -> RootStage.TUTORIAL
        forceWorkspace -> RootStage.WORKSPACE
        firstRun && !firstOpenGreetingDismissed -> {
            if (currentStage == RootStage.RITUAL) RootStage.RITUAL else RootStage.GREETING
        }
        currentStage == RootStage.RITUAL -> RootStage.RITUAL
        currentStage == RootStage.TUTORIAL -> RootStage.TUTORIAL
        currentStage == RootStage.WORKSPACE -> RootStage.WORKSPACE
        else -> RootStage.WELCOME
    }
}

@Composable
private fun TradeSketchRoot(
    debugStartStage: String? = null,
    debugStartTab: String? = null
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val projectsViewModel: ProjectsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val projectsUiState by projectsViewModel.uiState.collectAsStateWithLifecycle()
    val debugStageOverride = remember(debugStartStage) {
        debugStartStage
            ?.uppercase()
            ?.let { raw ->
                RootStage.entries.firstOrNull { it.name == raw }
            }
    }
    val debugTabOverride = remember(debugStartTab) {
        debugStartTab
            ?.uppercase()
            ?.let { raw ->
                DetailTab.entries.firstOrNull { it.name == raw }
            }
    }
    val debugWorkspaceOverride = debugStageOverride == RootStage.WORKSPACE || debugTabOverride != null

    if (settingsUiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var forceTutorial by remember { mutableStateOf(false) }
    var forceWorkspace by remember { mutableStateOf(false) }
    var firstOpenGreetingDismissed by remember { mutableStateOf(false) }
    var welcomeHeroMode by remember { mutableStateOf(WelcomeHeroMode.COLD_START) }
    var stage by remember(
        settingsUiState.settings.firstRun,
        settingsUiState.settings.hasCompletedAppTutorial,
        firstOpenGreetingDismissed,
        forceTutorial,
        forceWorkspace,
        debugWorkspaceOverride
    ) {
        mutableStateOf(
            resolveRootStage(
                currentStage = RootStage.WELCOME,
                debugStageOverride = debugStageOverride,
                debugWorkspaceOverride = debugWorkspaceOverride,
                firstRun = settingsUiState.settings.firstRun,
                firstOpenGreetingDismissed = firstOpenGreetingDismissed,
                forceTutorial = forceTutorial,
                forceWorkspace = forceWorkspace
            )
        )
    }
    var startupProjectId by remember { mutableStateOf<String?>(null) }
    var startupSelectedTab by remember { mutableStateOf<DetailTab?>(null) }

    LaunchedEffect(
        debugStageOverride,
        settingsUiState.settings.firstRun,
        settingsUiState.settings.hasCompletedAppTutorial,
        firstOpenGreetingDismissed,
        forceTutorial,
        forceWorkspace,
        debugWorkspaceOverride
    ) {
        stage = resolveRootStage(
            currentStage = stage,
            debugStageOverride = debugStageOverride,
            debugWorkspaceOverride = debugWorkspaceOverride,
            firstRun = settingsUiState.settings.firstRun,
            firstOpenGreetingDismissed = firstOpenGreetingDismissed,
            forceTutorial = forceTutorial,
            forceWorkspace = forceWorkspace
        )
    }

    LaunchedEffect(onboardingUiState.completedProjectId) {
        val completedProjectId = onboardingUiState.completedProjectId ?: return@LaunchedEffect
        startupProjectId = completedProjectId
        val tutorialCompleted = settingsUiState.settings.hasCompletedAppTutorial
        forceTutorial = !tutorialCompleted
        forceWorkspace = tutorialCompleted
        stage = if (tutorialCompleted) RootStage.WORKSPACE else RootStage.TUTORIAL
        onboardingViewModel.clearCompletion()
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(320)
                ) + fadeIn(animationSpec = tween(220)) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(280)
                ) + fadeIn(animationSpec = tween(220)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { it / 3 },
                        animationSpec = tween(240)
                    ) + fadeOut(animationSpec = tween(180))
            }
        },
        label = "root_flow_transition"
    ) { currentStage ->
        when (currentStage) {
            RootStage.GREETING -> {
                FirstOpenGreetingScreen(
                    reducedMotionEnabled = settingsUiState.settings.reducedMotionEnabled,
                    onTakeMeThere = {
                        firstOpenGreetingDismissed = true
                        settingsViewModel.completeFirstOpenGreeting()
                        forceWorkspace = false
                        forceTutorial = false
                        startupProjectId = null
                        startupSelectedTab = null
                        stage = RootStage.RITUAL
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            RootStage.WELCOME -> {
                WelcomeScreenPro(
                    heroMode = welcomeHeroMode,
                    onBegin = {
                        firstOpenGreetingDismissed = true
                        forceWorkspace = false
                        forceTutorial = false
                        startupProjectId = null
                        startupSelectedTab = null
                        stage = RootStage.RITUAL
                    },
                    savedProjects = projectsUiState.projects,
                    onOpenSavedProject = { projectId ->
                        firstOpenGreetingDismissed = true
                        val shouldResumeTutorial = !settingsUiState.settings.hasCompletedAppTutorial
                        val selectedProject = projectsUiState.projects.firstOrNull { it.id == projectId }
                        forceWorkspace = !shouldResumeTutorial
                        forceTutorial = shouldResumeTutorial
                        startupProjectId = projectId
                        startupSelectedTab = preferredStartupTabForProject(selectedProject)
                        stage = if (shouldResumeTutorial) {
                            RootStage.TUTORIAL
                        } else {
                            RootStage.WORKSPACE
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            RootStage.RITUAL -> {
                ProjectRitualFlow(
                    isSaving = onboardingUiState.isSaving,
                    error = onboardingUiState.error,
                    onDismissError = onboardingViewModel::clearError,
                    onBackToWelcome = {
                        firstOpenGreetingDismissed = true
                        forceWorkspace = false
                        forceTutorial = false
                        startupProjectId = null
                        startupSelectedTab = null
                        welcomeHeroMode = WelcomeHeroMode.RETURNING_HOME
                        stage = RootStage.WELCOME
                    },
                    onComplete = { option ->
                        startupSelectedTab = preferredStartupTabForOption(option)
                        onboardingViewModel.completeQuickStart(inputMode = option.inputMode)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            RootStage.TUTORIAL -> {
                WorkspaceShell(
                    guidedTutorialEnabled = true,
                    onDismissGuidedTutorial = {
                        settingsViewModel.setAppTutorialCompleted(true)
                        forceTutorial = false
                        stage = RootStage.WORKSPACE
                    },
                    onRecordTap = settingsViewModel::recordTap,
                    onReturnHome = {
                        firstOpenGreetingDismissed = true
                        forceWorkspace = false
                        forceTutorial = false
                        startupProjectId = null
                        startupSelectedTab = null
                        welcomeHeroMode = WelcomeHeroMode.RETURNING_HOME
                        stage = RootStage.WELCOME
                    },
                    onOpenTutorial = { projectId, selectedTab ->
                        startupProjectId = projectId
                        startupSelectedTab = selectedTab
                        forceTutorial = true
                        stage = RootStage.TUTORIAL
                    },
                    initialSelectedTab = debugTabOverride ?: startupSelectedTab,
                    initialShowSavedProjects = false,
                    initialProjectId = startupProjectId,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top +
                                    WindowInsetsSides.Start +
                                    WindowInsetsSides.End
                            )
                        )
                )
            }

            RootStage.WORKSPACE -> {
                WorkspaceShell(
                    onRecordTap = settingsViewModel::recordTap,
                    onReturnHome = {
                        firstOpenGreetingDismissed = true
                        forceWorkspace = false
                        forceTutorial = false
                        startupProjectId = null
                        startupSelectedTab = null
                        welcomeHeroMode = WelcomeHeroMode.RETURNING_HOME
                        stage = RootStage.WELCOME
                    },
                    onOpenTutorial = { projectId, selectedTab ->
                        startupProjectId = projectId
                        startupSelectedTab = selectedTab
                        forceTutorial = true
                        stage = RootStage.TUTORIAL
                    },
                    initialSelectedTab = debugTabOverride ?: startupSelectedTab,
                    initialShowSavedProjects = false,
                    initialProjectId = startupProjectId,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top +
                                    WindowInsetsSides.Start +
                                    WindowInsetsSides.End
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ProjectRitualFlow(
    isSaving: Boolean,
    error: String?,
    onDismissError: () -> Unit,
    onBackToWelcome: () -> Unit,
    onComplete: (ProjectTypePlateOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ProjectTypePlateScreen(
            onSelectOption = onComplete,
            onBack = onBackToWelcome,
            modifier = Modifier.fillMaxSize()
        )

        if (error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = onDismissError) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        }
        if (isSaving) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.height(28.dp))
                Text(stringResource(R.string.finalizing_project_ritual))
            }
        }
    }
}

private fun preferredStartupTabForOption(option: ProjectTypePlateOption): DetailTab {
    return if (option.inputMode == TakeoffInputMode.MANUAL) {
        DetailTab.MATERIALS
    } else {
        DetailTab.BLUEPRINT
    }
}

private fun preferredStartupTabForProject(project: Project?): DetailTab {
    return if (project?.takeoffSession?.inputMode == TakeoffInputMode.MANUAL) {
        DetailTab.MATERIALS
    } else {
        DetailTab.BLUEPRINT
    }
}

internal fun nextRailAutoCollapseArmedAfterManualToggle(wasCollapsed: Boolean): Boolean {
    return wasCollapsed
}

internal fun shouldAutoCollapseRail(
    railCollapsed: Boolean,
    railAutoCollapseArmed: Boolean,
    tutorialMode: Boolean,
    guidedTutorialActive: Boolean
): Boolean {
    return !railCollapsed && railAutoCollapseArmed && !tutorialMode && !guidedTutorialActive
}

@Composable
private fun WorkspaceShell(
    tutorialMode: Boolean = false,
    onExitTutorialMode: (Boolean) -> Unit = {},
    guidedTutorialEnabled: Boolean = false,
    onDismissGuidedTutorial: () -> Unit = {},
    onRecordTap: (String) -> Unit,
    onReturnHome: () -> Unit,
    onOpenTutorial: (String?, DetailTab?) -> Unit,
    initialSelectedTab: DetailTab? = null,
    initialShowSavedProjects: Boolean = false,
    initialProjectId: String?,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = hiltViewModel()
) {
    val projectsUiState by projectsViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable(initialSelectedTab) {
        mutableStateOf(initialSelectedTab ?: DetailTab.BLUEPRINT)
    }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(initialProjectId) }
    var showSavedProjects by rememberSaveable { mutableStateOf(false) }
    var showNewProjectConfirm by rememberSaveable { mutableStateOf(false) }
    var showSaveProjectConfirm by rememberSaveable { mutableStateOf(false) }
    var saveFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingProjectDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var leftRailCollapsed by rememberSaveable { mutableStateOf(false) }
    var railAutoCollapseArmed by rememberSaveable { mutableStateOf(false) }
    var guidedTutorialRailBounds by remember { mutableStateOf<Rect?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeProject = remember(selectedProjectId, projectsUiState.projects) {
        projectsUiState.projects.firstOrNull { it.id == selectedProjectId }
    }
    var projectNameDraft by rememberSaveable(selectedProjectId) {
        mutableStateOf(activeProject?.name.orEmpty())
    }
    var lastExplicitSaveFingerprint by rememberSaveable(selectedProjectId) {
        mutableStateOf<String?>(null)
    }
    var projectChangedSinceExplicitSave by rememberSaveable(selectedProjectId) {
        mutableStateOf(false)
    }
    val currentProjectFingerprint = remember(activeProject, projectNameDraft) {
        projectHeaderStateFingerprint(activeProject, projectNameDraft)
    }
    val showSavedProjectBadge = remember(
        lastExplicitSaveFingerprint,
        projectChangedSinceExplicitSave,
        currentProjectFingerprint
    ) {
        shouldShowSavedProjectBadge(
            explicitSaveFingerprint = lastExplicitSaveFingerprint,
            hasProjectChangedSinceExplicitSave = projectChangedSinceExplicitSave,
            currentProjectFingerprint = currentProjectFingerprint
        )
    }
    val guidedTutorialInputMode = activeProject?.takeoffSession?.inputMode ?: when {
        initialSelectedTab == DetailTab.MATERIALS -> TakeoffInputMode.MANUAL
        else -> TakeoffInputMode.BLUEPRINT
    }
    val guidedTutorialSteps = remember(guidedTutorialInputMode) {
        workspaceGuidedTutorialSteps(guidedTutorialInputMode)
    }
    var guidedTutorialStepIndex by rememberSaveable(
        guidedTutorialEnabled,
        initialProjectId,
        guidedTutorialInputMode.name
    ) {
        mutableIntStateOf(0)
    }
    val guidedTutorialStep = if (guidedTutorialEnabled && activeProject != null) {
        guidedTutorialSteps.getOrNull(guidedTutorialStepIndex)
    } else {
        null
    }
    val guidedTutorialProgress = guidedTutorialStep?.let {
        GuidedTutorialProgress(
            stepNumber = guidedTutorialStepIndex + 1,
            totalSteps = guidedTutorialSteps.size
        )
    }

    LaunchedEffect(Unit) {
        projectsViewModel.events.collect { event ->
            if (event is ProjectsEvent.NavigateToProject) {
                val projectIdToDelete = pendingProjectDeletionId
                    ?.takeIf { pendingId -> pendingId != event.projectId }
                pendingProjectDeletionId = null
                selectedProjectId = event.projectId
                selectedTab = preferredStartupTabForProject(
                    projectsUiState.projects.firstOrNull { it.id == event.projectId }
                )
                showSavedProjects = false
                projectIdToDelete?.let(projectsViewModel::deleteProject)
            }
        }
    }

    LaunchedEffect(projectsUiState.error) {
        if (projectsUiState.error != null) {
            pendingProjectDeletionId = null
        }
    }

    LaunchedEffect(initialProjectId, projectsUiState.projects) {
        val projects = projectsUiState.projects
        if (projects.isEmpty()) {
            selectedProjectId = null
            return@LaunchedEffect
        }
        when {
            initialProjectId != null && projects.any { it.id == initialProjectId } -> {
                selectedProjectId = initialProjectId
                if (initialSelectedTab == null) {
                    selectedTab = preferredStartupTabForProject(
                        projects.firstOrNull { it.id == initialProjectId }
                    )
                }
            }
            selectedProjectId == null || projects.none { it.id == selectedProjectId } -> {
                val firstProject = projects.first()
                selectedProjectId = firstProject.id
                if (initialSelectedTab == null) {
                    selectedTab = preferredStartupTabForProject(firstProject)
                }
            }
        }
    }

    LaunchedEffect(initialShowSavedProjects) {
        if (initialShowSavedProjects) {
            showSavedProjects = true
        }
    }

    LaunchedEffect(activeProject?.name) {
        val latestName = activeProject?.name ?: ""
        if (latestName != projectNameDraft) {
            projectNameDraft = latestName
        }
    }
    LaunchedEffect(currentProjectFingerprint, lastExplicitSaveFingerprint) {
        if (
            lastExplicitSaveFingerprint != null &&
            currentProjectFingerprint != null &&
            currentProjectFingerprint != lastExplicitSaveFingerprint
        ) {
            projectChangedSinceExplicitSave = true
        }
    }

    LaunchedEffect(saveFeedbackMessage) {
        if (saveFeedbackMessage != null) {
            delay(2200)
            saveFeedbackMessage = null
        }
    }

    val normalizeTab: (DetailTab) -> DetailTab = { tab ->
        when (tab) {
            DetailTab.QUANTITIES -> DetailTab.MATERIALS
            DetailTab.ADDONS -> DetailTab.BLUEPRINT
            DetailTab.REVIEW -> DetailTab.EXPORT
            else -> tab
        }
    }

    val normalizedSelectedTab = normalizeTab(selectedTab)
    if (selectedTab != normalizedSelectedTab) {
        selectedTab = normalizedSelectedTab
    }

    LaunchedEffect(tutorialMode, guidedTutorialEnabled, projectsUiState.projects.size) {
        if ((tutorialMode || guidedTutorialEnabled) && projectsUiState.projects.isEmpty()) {
            projectsViewModel.createEasyStartProject()
        }
    }

    LaunchedEffect(tutorialMode) {
        if (tutorialMode) {
            selectedTab = DetailTab.BLUEPRINT
            showSavedProjects = false
            railAutoCollapseArmed = false
        }
    }
    LaunchedEffect(guidedTutorialEnabled, guidedTutorialInputMode) {
        if (guidedTutorialEnabled) {
            guidedTutorialStepIndex = 0
            showSavedProjects = false
            leftRailCollapsed = false
            railAutoCollapseArmed = false
        }
    }
    LaunchedEffect(guidedTutorialStep?.tab) {
        guidedTutorialStep?.tab?.let { tutorialTab ->
            selectedTab = tutorialTab
            showSavedProjects = false
        }
    }

    val navigateToTab: (DetailTab) -> Unit = { rawTab ->
        if (!tutorialMode) {
            val tab = normalizeTab(rawTab)
            if (guidedTutorialStep == null || tab == guidedTutorialStep.tab) {
                if (selectedTab != tab) {
                    onRecordTap("workspace_tab_${selectedTab.route}_to_${tab.route}")
                }
                selectedTab = tab
            }
        }
    }
    val onGuidedTutorialBack: (() -> Unit)? =
        if (guidedTutorialStep != null && guidedTutorialStepIndex > 0) {
            {
                guidedTutorialStepIndex = (guidedTutorialStepIndex - 1).coerceAtLeast(0)
            }
        } else {
            null
        }
    val onGuidedTutorialNext: (() -> Unit)? =
        if (guidedTutorialStep != null) {
            {
                if (guidedTutorialStepIndex >= guidedTutorialSteps.lastIndex) {
                    onDismissGuidedTutorial()
                } else {
                    guidedTutorialStepIndex += 1
                }
            }
        } else {
            null
        }
    val onGuidedTutorialSkip: (() -> Unit)? =
        if (guidedTutorialStep != null) {
            onDismissGuidedTutorial
        } else {
            null
        }
    val workspaceRailGuidedTutorialStep = guidedTutorialStep as? WorkspaceRailGuidedTutorialStep
    val workspaceRailTutorialBounds: List<Rect> = when (workspaceRailGuidedTutorialStep?.target) {
        WorkspaceRailGuidedTutorialTarget.LEFT_RAIL -> listOfNotNull(guidedTutorialRailBounds)
        null -> emptyList()
    }
    val toggleLeftRail: () -> Unit = {
        val wasCollapsed = leftRailCollapsed
        val collapsing = !wasCollapsed
        leftRailCollapsed = collapsing
        railAutoCollapseArmed = nextRailAutoCollapseArmedAfterManualToggle(wasCollapsed)
        if (collapsing) {
            showSavedProjects = false
        }
    }
    val persistProjectNameDraft: (Boolean) -> String? = persistProjectName@{ showFeedback ->
        val project = activeProject ?: return@persistProjectName null
        val normalizedName = projectNameDraft.trim()
        if (normalizedName.isEmpty()) {
            if (showFeedback) {
                Toast.makeText(
                    context,
                    context.getString(R.string.project_name_required),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                projectNameDraft = project.name
                focusManager.clearFocus()
            }
            return@persistProjectName null
        }
        val resolvedName = resolveUniqueProjectName(
            requestedName = normalizedName,
            existingProjects = projectsUiState.projects,
            excludingProjectId = project.id
        )
        projectNameDraft = resolvedName
        if (project.name != resolvedName) {
            projectsViewModel.recordTap("workspace_project_save")
            projectsViewModel.renameProject(project.id, resolvedName)
        }
        focusManager.clearFocus()
        if (showFeedback) {
            saveFeedbackMessage = if (resolvedName != normalizedName) {
                context.getString(R.string.project_saved_as, resolvedName)
            } else {
                context.getString(R.string.project_saved)
            }
        }
        projectHeaderStateFingerprint(
            activeProject = project.copy(name = resolvedName),
            projectNameDraft = resolvedName
        )
    }
    val launchNewProject: (Boolean) -> Unit = { keepCurrent ->
        persistProjectNameDraft(false)
        pendingProjectDeletionId = if (keepCurrent) null else activeProject?.id
        projectsViewModel.createEasyStartProject()
        showSavedProjects = false
    }
    val leftBlueprintOverlayInset = 0.dp
    val saveActiveProject: () -> Unit = {
        persistProjectNameDraft(true)?.let { savedFingerprint ->
            lastExplicitSaveFingerprint = savedFingerprint
            projectChangedSinceExplicitSave = false
        }
    }
    val requestSaveActiveProject: () -> Unit = {
        val project = activeProject
        if (project == null) {
            saveActiveProject()
        } else {
            val normalizedName = projectNameDraft.trim()
            if (normalizedName.isBlank() || normalizedName == project.name) {
                saveActiveProject()
            } else {
                showSaveProjectConfirm = true
            }
        }
    }
    BoxWithConstraints(
        modifier = if (selectedTab == DetailTab.BLUEPRINT) {
            modifier.fillMaxSize()
        } else {
            modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F4464),
                            Color(0xFF12314A),
                            Color(0xFF0C2234)
                        )
                    )
                )
        }
    ) {
        val density = LocalDensity.current
        val workspaceSafeWidth = maxWidth
        val bottomSafeInset = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
        val chromeLayout = calculateWorkspaceBlueprintChromeLayout(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
            bottomInset = bottomSafeInset
        )
        val railCompact = chromeLayout.railCompact
        val compactBlueprintHud = chromeLayout.compactBlueprintHud
        val collapsedRailWidth = chromeLayout.collapsedRailWidth
        val expandedRailWidth = chromeLayout.expandedRailWidth
        val blueprintHeaderFieldHeight = chromeLayout.headerFieldHeight
        val blueprintHeaderLanePadding = chromeLayout.headerLanePadding
        val blueprintHeaderButtonSize = chromeLayout.headerButtonSize
        val blueprintHeaderSpacing = chromeLayout.headerSpacing
        val blueprintHeaderMinWidth = chromeLayout.headerMinWidth
        val blueprintHeaderMaxWidthCap = chromeLayout.headerMaxWidthCap
        val blueprintHeaderMaxWidth = (
            workspaceSafeWidth -
                (blueprintHeaderLanePadding * 2) -
                blueprintHeaderButtonSize -
                blueprintHeaderSpacing
            ).coerceIn(blueprintHeaderMinWidth, blueprintHeaderMaxWidthCap)
        val blueprintHeaderRowWidth =
            blueprintHeaderMaxWidth + blueprintHeaderButtonSize + blueprintHeaderSpacing
        val blueprintHeaderSideClearance = 18.dp
        val blueprintCenterReservedWidth = if (selectedTab == DetailTab.BLUEPRINT && activeProject != null && !tutorialMode) {
            blueprintHeaderRowWidth + (blueprintHeaderSideClearance * 2)
        } else {
            0.dp
        }
        val dockRailForBlueprint = selectedTab == DetailTab.BLUEPRINT
        val workspaceRailTopPadding = chromeLayout.workspaceRailTopPadding
        val workspaceCollapsedRailTopPadding = chromeLayout.workspaceCollapsedRailTopPadding
        val workspaceRailBottomPadding = chromeLayout.workspaceRailBottomPadding
        val activeRailWidth = if (leftRailCollapsed) collapsedRailWidth else expandedRailWidth
        val railAnchorTopPadding = if (leftRailCollapsed) {
            workspaceCollapsedRailTopPadding
        } else {
            workspaceRailTopPadding
        }
        val measuredRailRightInset = with(density) {
            (guidedTutorialRailBounds?.right ?: 0f).toDp()
        }
        val estimatedRailRightInset = 4.dp + expandedRailWidth
        val guidedTutorialSafeStartInset = if (!leftRailCollapsed && guidedTutorialStepIndex > 0) {
            maxOf(measuredRailRightInset, estimatedRailRightInset) + 12.dp
        } else {
            0.dp
        }
        val guidedTutorialActive = guidedTutorialStep != null
        val railAutoCollapseEnabled = shouldAutoCollapseRail(
            railCollapsed = leftRailCollapsed,
            railAutoCollapseArmed = railAutoCollapseArmed,
            tutorialMode = tutorialMode,
            guidedTutorialActive = guidedTutorialActive
        )
        val handleWorkspaceActiveUseStarted: () -> Unit = {
            if (
                shouldAutoCollapseRail(
                    railCollapsed = leftRailCollapsed,
                    railAutoCollapseArmed = railAutoCollapseArmed,
                    tutorialMode = tutorialMode,
                    guidedTutorialActive = guidedTutorialActive
                )
            ) {
                leftRailCollapsed = true
                railAutoCollapseArmed = false
                showSavedProjects = false
            }
        }
        val savedProjectsPanelStartPadding = calculateDockedRailOverlayInset(activeRailWidth)
        val leftBlueprintDockedOverlayInset = if (dockRailForBlueprint) {
            savedProjectsPanelStartPadding
        } else {
            0.dp
        }
        val nonBlueprintContentStartPadding = if (dockRailForBlueprint) {
            0.dp
        } else {
            if (leftRailCollapsed) {
                collapsedRailWidth + 6.dp
            } else {
                expandedRailWidth + 8.dp
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val projectTabContent: (@Composable (String) -> Unit)? = when (selectedTab) {
                DetailTab.BLUEPRINT -> { projectId ->
                    BlueprintScreen(
                        projectId = projectId,
                        initialShowAddons = false,
                        initialShowParams = false,
                        topCenterReservedWidth = blueprintCenterReservedWidth,
                        leftEdgeDialInset = leftBlueprintOverlayInset,
                        leftDockedOverlayInset = leftBlueprintDockedOverlayInset,
                        guidedTutorialSafeStartInset = guidedTutorialSafeStartInset,
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                        railAutoCollapseEnabled = railAutoCollapseEnabled,
                        onWorkspaceActiveUseStarted = handleWorkspaceActiveUseStarted,
                        tutorialMode = tutorialMode,
                        onExitTutorialMode = { onExitTutorialMode(true) },
                        guidedTutorialStep = guidedTutorialStep as? BlueprintGuidedTutorialStep,
                        guidedTutorialProgress = guidedTutorialProgress,
                        onGuidedTutorialBack = onGuidedTutorialBack,
                        onGuidedTutorialNext = onGuidedTutorialNext,
                        onGuidedTutorialSkip = onGuidedTutorialSkip,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.MATERIALS -> { projectId ->
                    TakeoffScreen(
                        projectId = projectId,
                        screenMode = TakeoffScreenMode.MATERIALS,
                        onBack = { navigateToTab(DetailTab.BLUEPRINT) },
                        onOpenModel = { navigateToTab(DetailTab.EXPORT) },
                        onOpenBlueprint = { navigateToTab(DetailTab.BLUEPRINT) },
                        onOpenMaterials = { navigateToTab(DetailTab.MATERIALS) },
                        onOpenExport = { navigateToTab(DetailTab.EXPORT) },
                        guidedTutorialStep = guidedTutorialStep as? MaterialsGuidedTutorialStep,
                        guidedTutorialProgress = guidedTutorialProgress,
                        onGuidedTutorialBack = onGuidedTutorialBack,
                        onGuidedTutorialNext = onGuidedTutorialNext,
                        onGuidedTutorialSkip = onGuidedTutorialSkip,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.REVIEW,
                DetailTab.EXPORT -> { projectId ->
                    ReferenceExportScreen(
                        projectId = projectId,
                        onBack = { navigateToTab(DetailTab.MATERIALS) },
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                        guidedTutorialStep = guidedTutorialStep as? ExportGuidedTutorialStep,
                        guidedTutorialProgress = guidedTutorialProgress,
                        onGuidedTutorialBack = onGuidedTutorialBack,
                        onGuidedTutorialNext = onGuidedTutorialNext,
                        onGuidedTutorialSkip = onGuidedTutorialSkip,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.QUANTITIES,
                DetailTab.ADDONS,
                DetailTab.SETTINGS_ABOUT -> null
            }

            val workspaceContentBottomInset = if (selectedTab == DetailTab.BLUEPRINT) {
                Modifier
            } else {
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            }

            if (projectTabContent == null) {
                SettingsScreen(
                    onReplayTutorial = {
                        onRecordTap("settings_replay_tutorial")
                        onOpenTutorial(selectedProjectId, selectedTab)
                    },
                    onBack = { navigateToTab(DetailTab.BLUEPRINT) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = nonBlueprintContentStartPadding)
                        .then(workspaceContentBottomInset)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = nonBlueprintContentStartPadding)
                        .then(workspaceContentBottomInset)
                ) {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onCreateStarterProject = {
                            projectsViewModel.createEasyStartProject()
                        },
                        content = projectTabContent
                    )
                }
            }

            if (selectedTab == DetailTab.BLUEPRINT && activeProject != null && !tutorialMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(
                            start = blueprintHeaderLanePadding,
                            end = blueprintHeaderLanePadding,
                            top = 8.dp
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(blueprintHeaderSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .widthIn(
                                        min = blueprintHeaderMinWidth,
                                        max = blueprintHeaderMaxWidth
                                    )
                                    .height(blueprintHeaderFieldHeight),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                border = BorderStroke(1.15.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.86f)),
                                shadowElevation = 8.dp
                            ) {
                                OutlinedTextField(
                                    value = projectNameDraft,
                                    onValueChange = { projectNameDraft = it },
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.project_name_label),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { requestSaveActiveProject() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            WorkspaceProjectSaveButton(
                                onClick = requestSaveActiveProject,
                                showSavedBadge = showSavedProjectBadge,
                                compactBlueprintHud = compactBlueprintHud,
                                modifier = Modifier.size(blueprintHeaderButtonSize)
                            )
                        }

                        saveFeedbackMessage?.let { message ->
                            WorkspaceSaveFeedbackBlip(
                                message = message,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                    }
                }
            }

            SavedProjectsPanel(
                expanded = showSavedProjects,
                projects = projectsUiState.projects,
                selectedProjectId = selectedProjectId,
                onSelectProject = { projectId ->
                    persistProjectNameDraft(false)
                    pendingProjectDeletionId = null
                    selectedProjectId = projectId
                    selectedTab = DetailTab.BLUEPRINT
                    showSavedProjects = false
                },
                onDeleteProject = { projectId ->
                    if (selectedProjectId == projectId) {
                        selectedProjectId = projectsUiState.projects.firstOrNull { it.id != projectId }?.id
                        selectedTab = DetailTab.BLUEPRINT
                    }
                    projectsViewModel.deleteProject(projectId)
                },
                onDismiss = { showSavedProjects = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = savedProjectsPanelStartPadding,
                        top = railAnchorTopPadding
                    )
            )

            if (showNewProjectConfirm) {
                AlertDialog(
                    onDismissRequest = { showNewProjectConfirm = false },
                    title = { Text(stringResource(R.string.start_new_project_title)) },
                    text = {
                        Text(stringResource(R.string.start_new_project_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showNewProjectConfirm = false
                                launchNewProject(true)
                            }
                        ) {
                            Text(stringResource(R.string.save_and_new))
                        }
                    },
                    dismissButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    showNewProjectConfirm = false
                                    launchNewProject(false)
                                }
                            ) {
                                Text(stringResource(R.string.delete_and_new))
                            }
                            TextButton(onClick = { showNewProjectConfirm = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                )
            }

            if (showSaveProjectConfirm) {
                val pendingName = projectNameDraft.trim().ifBlank {
                    activeProject?.name.orEmpty()
                }
                AlertDialog(
                    onDismissRequest = { showSaveProjectConfirm = false },
                    title = { Text(stringResource(R.string.save_project)) },
                    text = {
                        Text("Save this project name as \"$pendingName\"?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSaveProjectConfirm = false
                                saveActiveProject()
                            }
                        ) {
                            Text(stringResource(R.string.save_project))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveProjectConfirm = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            projectsUiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = projectsViewModel::clearError) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                }
            }

        }

        if (!tutorialMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .padding(
                        start = 4.dp,
                        top = railAnchorTopPadding,
                        bottom = if (!leftRailCollapsed) workspaceRailBottomPadding else 0.dp
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                verticalAlignment = Alignment.Top
            ) {
                WorkspaceLeftRail(
                    currentTab = selectedTab,
                    collapsed = leftRailCollapsed,
                    compact = railCompact,
                    blueprintDocked = true,
                    collapsedRailWidth = collapsedRailWidth,
                    expandedRailWidth = expandedRailWidth,
                    onSelectTab = navigateToTab,
                    showSavedProjects = showSavedProjects,
                    onCreateNewProject = {
                        if (activeProject != null) {
                            showNewProjectConfirm = true
                            showSavedProjects = false
                        } else {
                            launchNewProject(true)
                        }
                    },
                    onToggleSavedProjects = {
                        showSavedProjects = !showSavedProjects
                    },
                    onOpenSettings = {
                        navigateToTab(DetailTab.SETTINGS_ABOUT)
                        showSavedProjects = false
                    },
                    onGoHome = {
                        showSavedProjects = false
                        onReturnHome()
                    },
                    onToggleCollapsed = toggleLeftRail,
                    boundsModifier = Modifier.onGloballyPositioned {
                        guidedTutorialRailBounds = Rect(it.positionInRoot(), it.size.toSize())
                    },
                    modifier = Modifier.fillMaxHeight()
                )
                if (!leftRailCollapsed && !dockRailForBlueprint) {
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = WorkspaceRailAccentBorder.copy(alpha = 0.55f)
                    )
                }
            }
        }

        if (
            workspaceRailGuidedTutorialStep != null &&
            guidedTutorialProgress != null &&
            onGuidedTutorialNext != null &&
            onGuidedTutorialSkip != null
        ) {
            GuidedTutorialBlipOverlay(
                title = workspaceRailGuidedTutorialStep.title,
                message = workspaceRailGuidedTutorialStep.message,
                supporting = workspaceRailGuidedTutorialStep.supporting,
                progress = guidedTutorialProgress,
                targetBounds = workspaceRailTutorialBounds,
                primaryActionLabel = workspaceRailGuidedTutorialStep.primaryActionLabel,
                minimumTopClearance = 16.dp,
                onBack = onGuidedTutorialBack,
                onNext = onGuidedTutorialNext,
                onSkip = onGuidedTutorialSkip,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WorkspaceLeftRail(
    currentTab: DetailTab,
    collapsed: Boolean,
    compact: Boolean,
    blueprintDocked: Boolean,
    collapsedRailWidth: androidx.compose.ui.unit.Dp,
    expandedRailWidth: androidx.compose.ui.unit.Dp,
    onSelectTab: (DetailTab) -> Unit,
    showSavedProjects: Boolean,
    onCreateNewProject: () -> Unit,
    onToggleSavedProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onToggleCollapsed: () -> Unit,
    boundsModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val blueprintDockedTopOffset = if (blueprintDocked) 22.dp else 0.dp
    val railShellOuterPadding = 8.dp
    val railContentTopPadding = 6.dp + blueprintDockedTopOffset
    val collapsedToggleTopPadding = railShellOuterPadding + railContentTopPadding
    val primaryTabs = listOf(
        DetailTab.BLUEPRINT,
        DetailTab.MATERIALS,
        DetailTab.EXPORT
    )
    if (collapsed) {
        Box(
            modifier = modifier
                .width(collapsedRailWidth)
                .padding(
                    top = collapsedToggleTopPadding,
                    bottom = 8.dp
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                onClick = onToggleCollapsed,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = WorkspaceRailSurface,
                border = BorderStroke(1.15.dp, WorkspaceRailAccentBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.expand_navigation_rail),
                    tint = WorkspaceRailAccentBright,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
        return
    }
    Box(
        modifier = modifier
            .width(expandedRailWidth)
            .padding(vertical = railShellOuterPadding)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(boundsModifier),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = WorkspaceRailShell,
            border = BorderStroke(
                width = 1.2.dp,
                color = WorkspaceRailAccentBorder
            ),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 5.dp,
                        end = 5.dp,
                        top = railContentTopPadding,
                        bottom = 10.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    WorkspaceRailButton(
                        label = stringResource(R.string.collapse_navigation_rail),
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        selected = false,
                        iconOnly = true,
                        onClick = onToggleCollapsed,
                        modifier = Modifier.size(36.dp)
                    )
                }
                WorkspaceRailButton(
                    label = stringResource(R.string.rail_home),
                    icon = Icons.Filled.Home,
                    selected = false,
                    iconOnly = true,
                    onClick = onGoHome,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.home_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )
                Spacer(
                    modifier = Modifier.height(if (compact) 12.dp else 18.dp)
                )
                WorkspaceRailButton(
                    label = stringResource(R.string.rail_new_plus),
                    icon = Icons.Filled.Add,
                    selected = false,
                    iconOnly = true,
                    onClick = onCreateNewProject,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.new_project_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )
                WorkspaceRailButton(
                    label = stringResource(R.string.rail_saved),
                    icon = Icons.Filled.Description,
                    selected = showSavedProjects,
                    iconOnly = true,
                    onClick = onToggleSavedProjects,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.saved_projects_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )
                primaryTabs.forEach { tab ->
                    WorkspaceRailButton(
                        label = tab.railLabel(),
                        icon = tab.icon,
                        selected = currentTab == tab,
                        iconOnly = true,
                        onClick = { onSelectTab(tab) },
                        onLongPress = {
                            Toast.makeText(context, tab.label, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(if (compact) 20.dp else 28.dp)
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WorkspaceRailAccentBorder.copy(alpha = 0.68f))
                        
                )
                WorkspaceRailButton(
                    label = stringResource(R.string.rail_settings_short),
                    icon = Icons.Filled.Settings,
                    selected = currentTab == DetailTab.SETTINGS_ABOUT,
                    iconOnly = true,
                    accented = true,
                    onClick = onOpenSettings,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.rail_settings_short),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(42.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkspaceProjectSaveButton(
    onClick: () -> Unit,
    showSavedBadge: Boolean,
    compactBlueprintHud: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.15.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.86f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = stringResource(R.string.save_project),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(if (compactBlueprintHud) 15.dp else 17.dp)
            )

            if (showSavedBadge) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(if (compactBlueprintHud) 14.dp else 15.dp),
                    shape = CircleShape,
                    color = Color(0xFF2FA35A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.92f)),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (compactBlueprintHud) 8.dp else 9.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSaveFeedbackBlip(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = WorkspaceRailShell.copy(alpha = 0.98f),
        border = BorderStroke(1.15.dp, WorkspaceRailAccentBorder),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = stringResource(R.string.save_project),
                tint = WorkspaceRailAccent
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = WorkspaceRailTextPrimary
            )
        }
    }
}

internal fun hasPendingProjectHeaderChanges(
    activeProject: Project?,
    projectNameDraft: String
): Boolean {
    val project = activeProject ?: return false
    return projectNameDraft.trim() != project.name
}

internal fun projectHeaderStateFingerprint(
    activeProject: Project?,
    projectNameDraft: String
): String? {
    val project = activeProject ?: return null
    return project.copy(
        name = projectNameDraft.trim(),
        updatedAt = 0L
    ).toString()
}

internal fun shouldShowSavedProjectBadge(
    explicitSaveFingerprint: String?,
    hasProjectChangedSinceExplicitSave: Boolean,
    currentProjectFingerprint: String?
): Boolean {
    return explicitSaveFingerprint != null &&
        !hasProjectChangedSinceExplicitSave &&
        currentProjectFingerprint == explicitSaveFingerprint
}

@Composable
private fun WorkspaceRailButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accented: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
    onLongPress: (() -> Unit)? = null
) {
    val buttonShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val containerColor = when {
        accented && selected -> Color(0xFF234C6A)
        accented -> Color(0xFF1A3A52)
        selected -> WorkspaceRailAccent
        else -> WorkspaceRailSurface
    }
    val contentColor = when {
        accented -> Color.White
        selected -> WorkspaceRailTextOnAccent
        else -> WorkspaceRailTextPrimary
    }
    val borderColor = when {
        accented && selected -> Color(0xFFCADDEA)
        accented -> Color(0xFF9AB6C8)
        selected -> Color(0xFFBFD4E5)
        else -> WorkspaceRailAccentBorder
    }
    Surface(
        onClick = onClick,
        shape = buttonShape,
        color = containerColor,
        border = BorderStroke(
            width = if (accented) 1.3.dp else 1.1.dp,
            color = borderColor
        ),
        shadowElevation = if (selected || accented) 3.dp else 0.5.dp,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .clip(buttonShape)
            .pointerInput(label) {
                detectTapGestures(
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .then(modifier)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .fillMaxHeight(0.54f)
                        .width(3.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(
                            if (accented) {
                                Color.White.copy(alpha = 0.94f)
                            } else {
                                Color(0xFFE5F1FA)
                            }
                        )
                )
            }
            if (iconOnly) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(if (accented) 17.dp else 18.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTab.railLabel(): String = when (this) {
    DetailTab.BLUEPRINT -> stringResource(R.string.rail_plan_short)
    DetailTab.MATERIALS -> stringResource(R.string.rail_materials_short)
    DetailTab.SETTINGS_ABOUT -> stringResource(R.string.rail_settings_short)
    else -> label
}

@Composable
private fun SavedProjectsPanel(
    expanded: Boolean,
    projects: List<Project>,
    selectedProjectId: String?,
    onSelectProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    var pendingDeleteProjectId by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = modifier.widthIn(max = 280.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.saved_projects), style = MaterialTheme.typography.titleSmall)
                Button(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                    Text(stringResource(R.string.close))
                }
            }
            if (projects.isEmpty()) {
                Text(
                    stringResource(R.string.no_saved_projects),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .height(260.dp)
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    projects.forEach { project ->
                        Surface(
                            onClick = { onSelectProject(project.id) },
                            shape = MaterialTheme.shapes.small,
                            color = if (project.id == selectedProjectId) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project.name,
                                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                IconButton(
                                    onClick = { pendingDeleteProjectId = project.id },
                                    modifier = Modifier.height(48.dp).width(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(
                                            R.string.delete_project_content_description,
                                            project.name
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val pendingProject = projects.firstOrNull { it.id == pendingDeleteProjectId }
    if (pendingProject != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteProjectId = null },
            title = { Text(stringResource(R.string.delete_project_title)) },
            text = { Text(stringResource(R.string.delete_project_message, pendingProject.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(pendingProject.id)
                        pendingDeleteProjectId = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProjectId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProjectScopedTab(
    selectedProjectId: String?,
    onCreateStarterProject: () -> Unit,
    content: @Composable (String) -> Unit
) {
    if (selectedProjectId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_project_selected_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.no_project_selected_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onCreateStarterProject) {
                        Text(stringResource(R.string.create_starter_project))
                    }
                }
            }
        }
    } else {
        content(selectedProjectId)
    }
}

internal enum class DetailTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    BLUEPRINT("tab_blueprint", "Blueprint", Icons.Filled.Architecture),
    MATERIALS("tab_materials", "Materials", Icons.Filled.Assessment),
    QUANTITIES("tab_quantities", "Quantities", Icons.Filled.Straighten),
    ADDONS("tab_addons", "Add-ons", Icons.Filled.Add),
    REVIEW("tab_review", "Review", Icons.Filled.Description),
    EXPORT("tab_export", "Export", Icons.Filled.Flag),
    SETTINGS_ABOUT("tab_settings_about", "Settings", Icons.Filled.Settings)
}
