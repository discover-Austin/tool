package com.tradesketch.estimator

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.ui.blueprint.calculateDockedRailOverlayInset
import com.tradesketch.estimator.ui.blueprint.calculateWorkspaceBlueprintChromeLayout
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

private val WorkspaceRailShell = Color(0xFFE3EBF2)
private val WorkspaceRailSurface = Color(0xFFFBFDFE)
private val WorkspaceRailAccent = Color(0xFF1F628D)
private val WorkspaceRailAccentBright = Color(0xFF2E7CAC)
private val WorkspaceRailAccentBorder = Color(0xFF869BAC)
private val WorkspaceRailTextPrimary = Color(0xFF162532)
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
                    onOpenTutorial = {
                        forceTutorial = true
                        stage = RootStage.TUTORIAL
                    },
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
                Text(stringResource(R.string.back))
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
    var showSaveProjectConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingProjectDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var leftRailCollapsed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeProject = remember(selectedProjectId, projectsUiState.projects) {
        projectsUiState.projects.firstOrNull { it.id == selectedProjectId }
    }
    var projectNameDraft by rememberSaveable(selectedProjectId) {
        mutableStateOf(activeProject?.name.orEmpty())
    }

    LaunchedEffect(Unit) {
        projectsViewModel.events.collect { event ->
            if (event is ProjectsEvent.NavigateToProject) {
                val projectIdToDelete = pendingProjectDeletionId
                    ?.takeIf { pendingId -> pendingId != event.projectId }
                pendingProjectDeletionId = null
                selectedProjectId = event.projectId
                selectedTab = DetailTab.BLUEPRINT
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

    LaunchedEffect(tutorialMode, projectsUiState.projects.size) {
        if (tutorialMode && projectsUiState.projects.isEmpty()) {
            projectsViewModel.createEasyStartProject()
        }
    }

    LaunchedEffect(tutorialMode) {
        if (tutorialMode) {
            selectedTab = DetailTab.BLUEPRINT
            showSavedProjects = false
        }
    }

    val navigateToTab: (DetailTab) -> Unit = { rawTab ->
        if (!tutorialMode) {
            val tab = normalizeTab(rawTab)
            if (selectedTab != tab) {
                onRecordTap("workspace_tab_${selectedTab.route}_to_${tab.route}")
            }
            selectedTab = tab
        }
    }
    val toggleLeftRail: () -> Unit = {
        val collapsing = !leftRailCollapsed
        leftRailCollapsed = collapsing
        if (collapsing) {
            showSavedProjects = false
        }
    }
    val persistProjectNameDraft: (Boolean) -> Boolean = persistProjectName@{ showFeedback ->
        val project = activeProject ?: return@persistProjectName false
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
            return@persistProjectName false
        }
        if (project.name != normalizedName) {
            projectsViewModel.recordTap("workspace_project_save")
            projectsViewModel.renameProject(project.id, normalizedName)
        }
        focusManager.clearFocus()
        if (showFeedback) {
            Toast.makeText(
                context,
                context.getString(R.string.project_saved),
                Toast.LENGTH_SHORT
            ).show()
        }
        true
    }
    val launchNewProject: (Boolean) -> Unit = { keepCurrent ->
        persistProjectNameDraft(false)
        pendingProjectDeletionId = if (keepCurrent) null else activeProject?.id
        projectsViewModel.createEasyStartProject()
        showSavedProjects = false
    }
    val leftBlueprintOverlayInset = 0.dp
    val saveActiveProject: () -> Unit = {
        persistProjectNameDraft(true)
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
        val savedProjectsPanelStartPadding = calculateDockedRailOverlayInset(activeRailWidth)
        val leftBlueprintDockedOverlayInset = if (dockRailForBlueprint) {
            savedProjectsPanelStartPadding
        } else {
            0.dp
        }
        val nonBlueprintContentStartPadding = if (dockRailForBlueprint) {
            0.dp
        } else {
            if (leftRailCollapsed) collapsedRailWidth + 8.dp else expandedRailWidth + 12.dp
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
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                        tutorialMode = tutorialMode,
                        onExitTutorialMode = { onExitTutorialMode(true) },
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
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailTab.REVIEW,
                DetailTab.EXPORT -> { projectId ->
                    ExportScreen(
                        projectId = projectId,
                        onBack = { navigateToTab(DetailTab.MATERIALS) },
                        onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
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
                        onOpenTutorial()
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
                        Surface(
                            onClick = requestSaveActiveProject,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            border = BorderStroke(1.15.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.86f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(blueprintHeaderButtonSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = stringResource(R.string.save_project),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(if (compactBlueprintHud) 15.dp else 17.dp)
                                )
                            }
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
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val blueprintDockedTopOffset = if (blueprintDocked) 22.dp else 0.dp
    val railShellOuterPadding = 8.dp
    val railContentTopPadding = 10.dp + blueprintDockedTopOffset
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
                .fillMaxWidth(),
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
    SETTINGS_ABOUT("tab_settings_about", "Settings", Icons.Filled.Settings)
}
