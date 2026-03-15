package com.tradesketch.estimator

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.ui.screens.BlueprintScreen
import com.tradesketch.estimator.ui.screens.ExportScreen
import com.tradesketch.estimator.ui.screens.ProjectRitualScreen1_Name
import com.tradesketch.estimator.ui.screens.ProjectRitualScreen2_EstimateType
import com.tradesketch.estimator.ui.screens.SettingsScreen
import com.tradesketch.estimator.ui.screens.TakeoffScreen
import com.tradesketch.estimator.ui.screens.TakeoffScreenMode
import com.tradesketch.estimator.ui.screens.WelcomeScreenPro
import com.tradesketch.estimator.ui.theme.Midnight900
import com.tradesketch.estimator.ui.theme.Midnight950
import com.tradesketch.estimator.ui.theme.Slate800
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import com.tradesketch.estimator.ui.viewmodel.OnboardingViewModel
import com.tradesketch.estimator.ui.viewmodel.ProjectsEvent
import com.tradesketch.estimator.ui.viewmodel.ProjectsViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                                    Midnight950,
                                    Midnight900,
                                    Slate800.copy(alpha = 0.82f)
                                )
                            )
                        )
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TradeSketchRoot()
                    }
                }
            }
        }
    }
}

private enum class RootStage {
    WELCOME,
    RITUAL,
    TUTORIAL,
    WORKSPACE
}

private val WorkspaceRailShell = Color(0xFFF4F8FD)
private val WorkspaceRailSurface = Color(0xFFFFFFFF)
private val WorkspaceRailAccent = Color(0xFF2F6E9E)
private val WorkspaceRailAccentBright = Color(0xFF4F89BB)
private val WorkspaceRailAccentBorder = Color(0xFFC5D7E7)
private val WorkspaceRailTextPrimary = Color(0xFF243240)
private val WorkspaceRailTextOnAccent = Color(0xFFFFFFFF)

@Composable
private fun TradeSketchRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

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

    var forceTutorial by rememberSaveable { mutableStateOf(false) }
    var stage by rememberSaveable(
        settingsUiState.settings.firstRun,
        settingsUiState.settings.hasCompletedAppTutorial,
        forceTutorial
    ) {
        mutableStateOf(
            when {
                settingsUiState.settings.firstRun -> RootStage.WELCOME
                forceTutorial -> RootStage.TUTORIAL
                settingsUiState.settings.hasCompletedAppTutorial -> RootStage.WORKSPACE
                else -> RootStage.TUTORIAL
            }
        )
    }
    var startupProjectId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(
        settingsUiState.settings.firstRun,
        settingsUiState.settings.hasCompletedAppTutorial,
        forceTutorial
    ) {
        stage = when {
            settingsUiState.settings.firstRun -> {
                if (stage == RootStage.RITUAL) RootStage.RITUAL else RootStage.WELCOME
            }
            forceTutorial -> RootStage.TUTORIAL
            settingsUiState.settings.hasCompletedAppTutorial -> RootStage.WORKSPACE
            else -> RootStage.TUTORIAL
        }
    }

    LaunchedEffect(onboardingUiState.completedProjectId) {
        val completedProjectId = onboardingUiState.completedProjectId ?: return@LaunchedEffect
        startupProjectId = completedProjectId
        stage = if (settingsUiState.settings.hasCompletedAppTutorial) {
            RootStage.WORKSPACE
        } else {
            RootStage.TUTORIAL
        }
        forceTutorial = false
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
            RootStage.WELCOME -> {
                WelcomeScreenPro(
                    onBegin = { stage = RootStage.RITUAL },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )
            }

            RootStage.RITUAL -> {
                ProjectRitualFlow(
                    isSaving = onboardingUiState.isSaving,
                    error = onboardingUiState.error,
                    onDismissError = onboardingViewModel::clearError,
                    onBackToWelcome = { stage = RootStage.WELCOME },
                    onComplete = { name, trade ->
                        onboardingViewModel.completeRitual(projectName = name, trade = trade)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )
            }

            RootStage.TUTORIAL -> {
                WorkspaceShell(
                    tutorialMode = true,
                    onExitTutorialMode = {
                        settingsViewModel.setAppTutorialCompleted(true)
                        forceTutorial = false
                        stage = RootStage.WORKSPACE
                    },
                    onRecordTap = settingsViewModel::recordTap,
                    onOpenTutorial = {
                        forceTutorial = true
                        stage = RootStage.TUTORIAL
                    },
                    initialProjectId = startupProjectId,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )
            }

            RootStage.WORKSPACE -> {
                WorkspaceShell(
                    onRecordTap = settingsViewModel::recordTap,
                    onOpenTutorial = {
                        forceTutorial = true
                        stage = RootStage.TUTORIAL
                    },
                    initialProjectId = startupProjectId,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
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
    onComplete: (String, PrimaryTrade) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(1) }
    var projectName by rememberSaveable { mutableStateOf("") }
    var selectedTrade by rememberSaveable { mutableStateOf<PrimaryTrade?>(null) }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(260)
                    ) + fadeIn(animationSpec = tween(200)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(220)
                        ) + fadeOut(animationSpec = tween(180))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(260)
                    ) + fadeIn(animationSpec = tween(200)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(220)
                        ) + fadeOut(animationSpec = tween(180))
                }
            },
            label = "project_ritual_transition"
        ) { currentStep ->
            when (currentStep) {
                1 -> {
                    ProjectRitualScreen1_Name(
                        projectName = projectName,
                        onProjectNameChange = { projectName = it },
                        onContinue = { step = 2 },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    ProjectRitualScreen2_EstimateType(
                        selectedTrade = selectedTrade,
                        onSelectTrade = { selectedTrade = it },
                        onComplete = {
                            val trade = selectedTrade ?: return@ProjectRitualScreen2_EstimateType
                            onComplete(projectName.trim(), trade)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

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

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (step == 1) onBackToWelcome() else step = 1
                },
                enabled = !isSaving
            ) {
                Text(
                    if (step == 1) {
                        stringResource(R.string.back)
                    } else {
                        stringResource(R.string.back_to_name)
                    }
                )
            }
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(28.dp))
                Text(stringResource(R.string.finalizing_project_ritual))
            }
        }
    }
}

@Composable
private fun WorkspaceShell(
    tutorialMode: Boolean = false,
    onExitTutorialMode: (Boolean) -> Unit = {},
    onRecordTap: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    initialProjectId: String?,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = hiltViewModel()
) {
    val projectsUiState by projectsViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.BLUEPRINT) }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(initialProjectId) }
    var showSavedProjects by rememberSaveable { mutableStateOf(false) }
    var showNewProjectConfirm by rememberSaveable { mutableStateOf(false) }
    var leftRailCollapsed by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val tutorialSteps = remember {
        listOf(
            WorkspaceTourStep(
                title = "Workspace Rail",
                message = "Use the left rail to control projects and move through the full estimating flow.",
                controls = listOf(
                    "New+ starts a fresh project.",
                    "Saved opens your project list so you can switch or delete jobs.",
                    "Blueprint, Materials, Export, and Settings/About are your main workflow tabs.",
                    "Use the top arrow to collapse or expand the rail."
                ),
                tip = "If you are learning, stay in Blueprint first, then move right through Materials and Export.",
                targetTab = DetailTab.BLUEPRINT
            ),
            WorkspaceTourStep(
                title = "Canvas Navigation",
                message = "Start by learning how to frame the area before placing any walls.",
                controls = listOf(
                    "Touch: pinch to zoom and pan with two fingers.",
                    "Joystick: right stick moves the cursor and left stick pans the canvas.",
                    "Use the Project Name field at the top to rename the active job.",
                    "Zoom in until corners are easy to hit before you start drawing."
                ),
                tip = "A clean view makes every later tap easier.",
                targetTab = DetailTab.BLUEPRINT,
                drillTitle = "Warm-Up",
                drill = listOf(
                    "Pan until one outside corner is centered.",
                    "Pinch in and out once so you know your working zoom."
                )
            ),
            WorkspaceTourStep(
                title = "Draw Walls",
                message = "Wall drawing is a simple start-point and finish-point rhythm.",
                controls = listOf(
                    "Switch to Draw before placing linework.",
                    "Tap once to start a wall, move to the next corner, then tap again to finish it.",
                    "Chain continues from the last placed corner for fast runs.",
                    "Split lets the next segment break away instead of continuing the chain."
                ),
                tip = "Place one clean segment at a time until the shape feels natural.",
                targetTab = DetailTab.BLUEPRINT,
                drillTitle = "Touch Drill",
                drill = listOf(
                    "Tap Draw.",
                    "Tap a corner to start a wall.",
                    "Tap the next corner to place the segment."
                )
            ),
            WorkspaceTourStep(
                title = "Box Rooms",
                message = "Box mode is the fastest way to block in rectangular spaces.",
                controls = listOf(
                    "Tap Box, then tap the first corner of the room.",
                    "Move diagonally to size the room and tap again to finish the rectangle.",
                    "Use the side dials before the final tap when you need exact angle or length.",
                    "Undo or Cancel is faster than fighting a bad box."
                ),
                tip = "Rough in simple rooms with Box first, then refine edges later.",
                targetTab = DetailTab.BLUEPRINT
            ),
            WorkspaceTourStep(
                title = "Select, Edit, Delete",
                message = "Use Select mode whenever you need to inspect, adjust, or remove existing geometry.",
                controls = listOf(
                    "Tap a wall or opening to select it.",
                    "Trash deletes only the current selection.",
                    "Use Cancel or a right-tap to clear an unfinished action or accidental pick.",
                    "Zoom closer before editing short walls or tight opening layouts."
                ),
                tip = "Select before Delete so the target is always obvious.",
                targetTab = DetailTab.BLUEPRINT
            ),
            WorkspaceTourStep(
                title = "Openings and Snap",
                message = "Place doors, windows, and stairs after the wall layout is stable.",
                controls = listOf(
                    "Door, Window, Stair Up, and Stair Down open preset placement panels.",
                    "Place the preview onto an existing wall segment to create the opening.",
                    "Params controls snap behavior and other blueprint tuning.",
                    "If placement feels sticky or too loose, adjust snap and try again."
                ),
                tip = "Walls first, openings second keeps the canvas easier to edit.",
                targetTab = DetailTab.BLUEPRINT
            ),
            WorkspaceTourStep(
                title = "Touch and Joystick",
                message = "Both input styles drive the same tools, so use whichever keeps you accurate.",
                controls = listOf(
                    "Touch is best for quick zooming, panning, and direct taps.",
                    "Joystick is best when you want a steady cursor without covering the canvas.",
                    "Left-tap is the primary action at the cursor; right-tap is cancel or reset.",
                    "Swap freely between touch and joystick while the same tool stays active."
                ),
                tip = "Many users frame the view with touch, then use joystick for precise wall placement.",
                targetTab = DetailTab.BLUEPRINT,
                drillTitle = "Quick-Start Drill",
                drill = listOf(
                    "Move the cursor to a corner.",
                    "Left-tap start wall.",
                    "Move the cursor to the next corner.",
                    "Left-tap place wall.",
                    "Right-tap reset."
                )
            ),
            WorkspaceTourStep(
                title = "Floors and Params",
                message = "Use the blueprint side controls to manage level context and tuning.",
                controls = listOf(
                    "Floor controls move between Ground, upper levels, and basements.",
                    "Params opens snap and trade parameter controls.",
                    "The ? button reopens the rail help when you want a reminder.",
                    "Undo/Redo and Zoom stay available on the upper control rail."
                ),
                tip = "Check floor and snap before drawing if something feels off.",
                targetTab = DetailTab.BLUEPRINT
            ),
            WorkspaceTourStep(
                title = "Materials",
                message = "Review computed quantities and tune assumptions before pricing or export.",
                controls = listOf(
                    "Validate generated line items against field reality.",
                    "Adjust waste factors, coverage rates, and unit assumptions as needed.",
                    "Use manual overrides only when site conditions differ from the drawing."
                ),
                tip = "Keep overrides minimal so revisions remain traceable to blueprint geometry.",
                targetTab = DetailTab.MATERIALS
            ),
            WorkspaceTourStep(
                title = "Export",
                message = "Create files and shareable outputs for clients, purchasing, or internal review.",
                controls = listOf(
                    "Choose the output type (PDF, CSV, JSON, text, or blueprint PNG).",
                    "For blueprint PNG, use Grid On or Grid Off before saving.",
                    "Use consistent naming so field teams can match files to project versions."
                ),
                tip = "Export after major geometry edits so the client packet always matches the latest drawing.",
                targetTab = DetailTab.EXPORT
            ),
            WorkspaceTourStep(
                title = "Settings/About",
                message = "Configure defaults once to make every new project faster and more consistent.",
                controls = listOf(
                    "Set snap defaults, joystick behavior, and touch preferences.",
                    "Review business profile and estimating defaults.",
                    "Use Replay Tutorial any time you want a guided refresher."
                ),
                tip = "Team-wide default values reduce estimate drift across different operators.",
                targetTab = DetailTab.SETTINGS_ABOUT
            )
        )
    }
    var tutorialStepIndex by rememberSaveable(tutorialMode) { mutableStateOf(0) }
    val activeProject = remember(selectedProjectId, projectsUiState.projects) {
        projectsUiState.projects.firstOrNull { it.id == selectedProjectId }
    }
    var projectNameDraft by rememberSaveable(selectedProjectId) {
        mutableStateOf(activeProject?.name.orEmpty())
    }

    LaunchedEffect(Unit) {
        projectsViewModel.events.collect { event ->
            if (event is ProjectsEvent.NavigateToProject) {
                selectedProjectId = event.projectId
                selectedTab = DetailTab.BLUEPRINT
                showSavedProjects = false
            }
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
            }
            selectedProjectId == null || projects.none { it.id == selectedProjectId } -> {
                selectedProjectId = projects.first().id
            }
        }
    }

    LaunchedEffect(activeProject?.name) {
        val latestName = activeProject?.name ?: ""
        if (latestName != projectNameDraft) {
            projectNameDraft = latestName
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

    LaunchedEffect(tutorialMode) {
        if (tutorialMode) {
            tutorialStepIndex = 0
        }
    }

    LaunchedEffect(tutorialMode, projectsUiState.projects.size) {
        if (tutorialMode && projectsUiState.projects.isEmpty()) {
            projectsViewModel.createEasyStartProject()
        }
    }

    LaunchedEffect(tutorialMode, tutorialStepIndex) {
        if (!tutorialMode) return@LaunchedEffect
        val target = tutorialSteps.getOrNull(tutorialStepIndex)?.targetTab ?: return@LaunchedEffect
        selectedTab = normalizeTab(target)
    }

    val navigateToTab: (DetailTab) -> Unit = { rawTab ->
        val tab = normalizeTab(rawTab)
        if (selectedTab != tab) {
            onRecordTap("workspace_tab_${selectedTab.route}_to_${tab.route}")
        }
        selectedTab = tab
    }
    val toggleLeftRail: () -> Unit = {
        val collapsing = !leftRailCollapsed
        leftRailCollapsed = collapsing
        if (collapsing) {
            showSavedProjects = false
        }
    }
    val launchNewProject: (Boolean) -> Unit = { keepCurrent ->
        if (!keepCurrent) {
            activeProject?.id?.let { projectId ->
                projectsViewModel.deleteProject(projectId)
                if (selectedProjectId == projectId) {
                    selectedProjectId = null
                }
            }
        }
        projectsViewModel.createEasyStartProject()
        showSavedProjects = false
    }

    val configuration = LocalConfiguration.current
    val railCompact = configuration.screenWidthDp < 600
    val railExpandedForLargeWindow = configuration.screenWidthDp >= 840
    val collapsedRailWidth = if (railCompact) 32.dp else 36.dp
    val expandedRailWidth = if (railExpandedForLargeWindow) 84.dp else 72.dp
    val leftBlueprintOverlayInset = 0.dp
    val dockRailForBlueprint = selectedTab == DetailTab.BLUEPRINT
    val blueprintRailTopPadding = if (configuration.screenHeightDp < 760) 118.dp else 132.dp
    val blueprintCollapsedRailTopPadding = if (configuration.screenHeightDp < 760) 56.dp else 68.dp
    val blueprintRailBottomPadding = if (configuration.screenHeightDp < 760) 156.dp else 172.dp
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val projectTabContent: (@Composable (String) -> Unit)? = when (selectedTab) {
                DetailTab.BLUEPRINT -> { projectId ->
                    BlueprintScreen(
                        projectId = projectId,
                        initialShowAddons = false,
                        initialShowParams = false,
                        leftEdgeDialInset = leftBlueprintOverlayInset,
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.MATERIALS -> { projectId ->
                    TakeoffScreen(
                        projectId = projectId,
                        screenMode = TakeoffScreenMode.MATERIALS,
                        onOpenModel = { navigateToTab(DetailTab.EXPORT) },
                        onOpenBlueprint = { navigateToTab(DetailTab.BLUEPRINT) },
                        onOpenMaterials = { navigateToTab(DetailTab.MATERIALS) },
                        onOpenExport = { navigateToTab(DetailTab.EXPORT) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.REVIEW,
                DetailTab.EXPORT -> { projectId ->
                    ExportScreen(
                        projectId = projectId,
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.QUANTITIES,
                DetailTab.ADDONS,
                DetailTab.SETTINGS_ABOUT -> null
            }

            if (projectTabContent == null) {
                SettingsScreen(
                    onReplayTutorial = {
                        onRecordTap("settings_replay_tutorial")
                        onOpenTutorial()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProjectScopedTab(
                    selectedProjectId = selectedProjectId,
                    onCreateStarterProject = {
                        projectsViewModel.createEasyStartProject()
                    },
                    content = projectTabContent
                )
            }

            if (selectedTab == DetailTab.BLUEPRINT && activeProject != null) {
                OutlinedTextField(
                    value = projectNameDraft,
                    onValueChange = { projectNameDraft = it },
                    label = {
                        Text(
                            stringResource(R.string.project_name_label),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            projectsViewModel.renameProject(activeProject.id, projectNameDraft)
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .widthIn(min = 156.dp, max = 232.dp)
                        .height(52.dp)
                )
            }

            SavedProjectsPanel(
                expanded = showSavedProjects,
                projects = projectsUiState.projects,
                selectedProjectId = selectedProjectId,
                onSelectProject = { projectId ->
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
                    .padding(start = 10.dp, top = 8.dp)
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
            if (tutorialMode) {
                WorkspaceTourOverlay(
                    step = tutorialSteps[tutorialStepIndex],
                    stepIndex = tutorialStepIndex,
                    totalSteps = tutorialSteps.size,
                    onSkip = { onExitTutorialMode(false) },
                    onBack = {
                        tutorialStepIndex = (tutorialStepIndex - 1).coerceAtLeast(0)
                    },
                    onNext = {
                        if (tutorialStepIndex >= tutorialSteps.lastIndex) {
                            onExitTutorialMode(true)
                        } else {
                            tutorialStepIndex += 1
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .padding(
                    start = 4.dp,
                    top = if (dockRailForBlueprint) {
                        if (leftRailCollapsed) blueprintCollapsedRailTopPadding else blueprintRailTopPadding
                    } else {
                        0.dp
                    },
                    bottom = if (dockRailForBlueprint && !leftRailCollapsed) blueprintRailBottomPadding else 0.dp
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
                ),
            verticalAlignment = Alignment.Top
        ) {
            WorkspaceLeftRail(
                currentTab = selectedTab,
                collapsed = leftRailCollapsed,
                compact = railCompact,
                blueprintDocked = dockRailForBlueprint,
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
                onToggleCollapsed = toggleLeftRail,
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
}

private data class WorkspaceTourStep(
    val title: String,
    val message: String,
    val controls: List<String>,
    val tip: String,
    val targetTab: DetailTab,
    val drillTitle: String? = null,
    val drill: List<String> = emptyList()
)

@Composable
private fun WorkspaceTourOverlay(
    step: WorkspaceTourStep,
    stepIndex: Int,
    totalSteps: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compactHeightWindow = LocalConfiguration.current.screenHeightDp < 700
    val maxCardHeight = if (compactHeightWindow) 320.dp else 520.dp
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .heightIn(max = maxCardHeight),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.interactive_tour),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip))
                }
            }
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(
                    R.string.tour_step_progress,
                    stepIndex + 1,
                    totalSteps,
                    step.title
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = step.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                step.controls.forEach { control ->
                    Text(
                        text = "• $control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (step.drill.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = step.drillTitle ?: "Quick Drill",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        step.drill.forEachIndexed { index, instruction ->
                            Text(
                                text = "${index + 1}. $instruction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.tour_tip, step.tip),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = stepIndex > 0
                ) {
                    Text(stringResource(R.string.back))
                }
                Button(onClick = onNext) {
                    Text(
                        if (stepIndex == totalSteps - 1) {
                            stringResource(R.string.finish_tour)
                        } else {
                            stringResource(R.string.next)
                        }
                    )
                }
            }
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
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val primaryTabs = DetailTab.entries.filterNot { tab ->
        tab == DetailTab.QUANTITIES ||
            tab == DetailTab.ADDONS ||
            tab == DetailTab.REVIEW ||
            tab == DetailTab.SETTINGS_ABOUT
    }
    val blueprintTab = primaryTabs.firstOrNull { it == DetailTab.BLUEPRINT } ?: DetailTab.BLUEPRINT
    val secondaryTabs = primaryTabs.filterNot { it == DetailTab.BLUEPRINT }
    if (collapsed) {
        val collapsedArrowTopPadding = when {
            blueprintDocked && compact -> 12.dp
            blueprintDocked -> 18.dp
            else -> 0.dp
        }
        Box(
            modifier = modifier
                .width(collapsedRailWidth)
                .padding(
                    top = if (blueprintDocked) collapsedArrowTopPadding else 8.dp,
                    bottom = 8.dp
                ),
            contentAlignment = if (blueprintDocked) Alignment.TopStart else Alignment.CenterStart
        ) {
            Surface(
                onClick = onToggleCollapsed,
                shape = MaterialTheme.shapes.small,
                color = WorkspaceRailSurface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, WorkspaceRailAccentBorder.copy(alpha = 0.92f)),
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .padding(start = 2.dp)
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
    NavigationRail(
        modifier = modifier
            .width(expandedRailWidth)
            .padding(vertical = 8.dp),
        containerColor = Color.Transparent
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight(),
            shape = MaterialTheme.shapes.large,
            color = WorkspaceRailShell.copy(alpha = 0.98f),
            border = BorderStroke(
                width = 1.dp,
                color = WorkspaceRailAccentBorder.copy(alpha = 0.82f)
            ),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 5.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceRailBrand()
                Surface(
                    onClick = onToggleCollapsed,
                    shape = MaterialTheme.shapes.small,
                    color = WorkspaceRailSurface.copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, WorkspaceRailAccentBorder.copy(alpha = 0.92f)),
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.collapse_navigation_rail),
                        tint = WorkspaceRailAccentBright,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                WorkspaceRailActionItem(
                    label = stringResource(R.string.rail_new_plus),
                    icon = Icons.Filled.Add,
                    selected = false,
                    onClick = onCreateNewProject,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.new_project_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                WorkspaceRailActionItem(
                    label = stringResource(R.string.rail_saved),
                    icon = Icons.Filled.Description,
                    selected = showSavedProjects,
                    onClick = onToggleSavedProjects,
                    onLongPress = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.saved_projects_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                WorkspaceRailItem(
                    tab = blueprintTab,
                    currentTab = currentTab,
                    onSelectTab = onSelectTab,
                    onLongPress = {
                        Toast.makeText(context, blueprintTab.label, Toast.LENGTH_SHORT).show()
                    }
                )
                secondaryTabs.forEach { tab ->
                    WorkspaceRailItem(
                        tab = tab,
                        currentTab = currentTab,
                        onSelectTab = onSelectTab,
                        onLongPress = {
                            Toast.makeText(context, tab.label, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                WorkspaceRailItem(
                    tab = DetailTab.SETTINGS_ABOUT,
                    currentTab = currentTab,
                    onSelectTab = onSelectTab,
                    onLongPress = {
                        Toast.makeText(context, DetailTab.SETTINGS_ABOUT.label, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceRailActionItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val containerColor = if (selected) WorkspaceRailAccentBright else WorkspaceRailSurface
    val contentColor = if (selected) WorkspaceRailTextOnAccent else WorkspaceRailTextPrimary
    val borderColor = if (selected) Color(0xFFD8E7F5) else WorkspaceRailAccentBorder
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .pointerInput(label) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    }
                )
            },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = contentColor,
            selectedTextColor = contentColor,
            indicatorColor = Color.Transparent,
            unselectedIconColor = contentColor,
            unselectedTextColor = contentColor
        )
    )
}

@Composable
private fun WorkspaceRailItem(
    tab: DetailTab,
    currentTab: DetailTab,
    onSelectTab: (DetailTab) -> Unit,
    onLongPress: () -> Unit
) {
    val isSelected = currentTab == tab
    val containerColor = if (isSelected) WorkspaceRailAccentBright else WorkspaceRailSurface
    val contentColor = if (isSelected) WorkspaceRailTextOnAccent else WorkspaceRailTextPrimary
    val borderColor = if (isSelected) Color(0xFFD8E7F5) else WorkspaceRailAccentBorder
    NavigationRailItem(
        selected = isSelected,
        onClick = { onSelectTab(tab) },
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = tab.label
            }
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .pointerInput(tab) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    }
                )
            },
        icon = {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = contentColor
            )
        },
        label = {
            Text(
                text = tab.railLabel(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = contentColor,
            selectedTextColor = contentColor,
            indicatorColor = Color.Transparent,
            unselectedIconColor = contentColor,
            unselectedTextColor = contentColor
        )
    )
}

@Composable
private fun WorkspaceRailBrand() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = WorkspaceRailSurface,
        border = BorderStroke(
            width = 1.dp,
            color = WorkspaceRailAccentBorder.copy(alpha = 0.88f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = WorkspaceRailAccent,
                border = BorderStroke(1.dp, WorkspaceRailAccentBright)
            ) {
                Icon(
                    imageVector = Icons.Filled.Architecture,
                    contentDescription = stringResource(R.string.app_name),
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
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
                    projects.take(24).forEach { project ->
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

private enum class DetailTab(
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
    SETTINGS_ABOUT("tab_settings_about", "Settings/About", Icons.Filled.Settings)
}
