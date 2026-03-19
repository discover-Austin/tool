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
    var leftRailCollapsed by rememberSaveable { mutableStateOf(false) }
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
    val compactBlueprintHud = configuration.screenWidthDp < 420
    val collapsedRailWidth = if (railCompact) 22.dp else 24.dp
    val expandedRailWidth = if (railExpandedForLargeWindow) 64.dp else 60.dp
    val leftBlueprintOverlayInset = 0.dp
    val dockRailForBlueprint = selectedTab == DetailTab.BLUEPRINT
    val blueprintRailTopPadding = if (configuration.screenHeightDp < 760) 118.dp else 132.dp
    val blueprintCollapsedRailTopPadding = if (configuration.screenHeightDp < 760) 56.dp else 68.dp
    val blueprintRailBottomPadding = if (configuration.screenHeightDp < 760) 156.dp else 172.dp
    val nonBlueprintContentStartPadding = if (dockRailForBlueprint) {
        0.dp
    } else {
        if (leftRailCollapsed) collapsedRailWidth + 8.dp else expandedRailWidth + 12.dp
    }
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
                        tutorialMode = tutorialMode,
                        onExitTutorialMode = { onExitTutorialMode(true) },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = nonBlueprintContentStartPadding)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = nonBlueprintContentStartPadding)
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

            if (selectedTab == DetailTab.BLUEPRINT && activeProject != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .widthIn(
                            min = if (compactBlueprintHud) 132.dp else 156.dp,
                            max = if (compactBlueprintHud) 176.dp else 232.dp
                        )
                        .height(58.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
                    shadowElevation = 10.dp
                ) {
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
                    .padding(
                        start = if (dockRailForBlueprint) 10.dp else nonBlueprintContentStartPadding + 6.dp,
                        top = 8.dp
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
        }

        if (!tutorialMode) {
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
    val primaryTabs = listOf(
        DetailTab.BLUEPRINT,
        DetailTab.MATERIALS,
        DetailTab.EXPORT
    )
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
                    .size(36.dp)
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
    Box(
        modifier = modifier
            .width(expandedRailWidth)
            .padding(vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
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
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                            .weight(1f)
                            .height(36.dp)
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
                            .weight(1f)
                            .height(36.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    WorkspaceRailButton(
                        label = stringResource(R.string.rail_settings_short),
                        icon = Icons.Filled.Settings,
                        selected = currentTab == DetailTab.SETTINGS_ABOUT,
                        iconOnly = true,
                        onClick = onOpenSettings,
                        onLongPress = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.rail_settings_short),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
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
                            .height(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceRailButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
    onLongPress: (() -> Unit)? = null
) {
    val containerColor = if (selected) WorkspaceRailAccentBright else WorkspaceRailSurface
    val contentColor = if (selected) WorkspaceRailTextOnAccent else WorkspaceRailTextPrimary
    val borderColor = if (selected) Color(0xFFD8E7F5) else WorkspaceRailAccentBorder
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .clip(MaterialTheme.shapes.medium)
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
            if (iconOnly) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
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
    SETTINGS_ABOUT("tab_settings_about", "Settings", Icons.Filled.Settings)
}
