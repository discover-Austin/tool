package com.tradesketch.estimator

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = AndroidColor.TRANSPARENT)
        )
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
    WORKSPACE
}

@Composable
private fun TradeSketchRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val onboardingUiState by onboardingViewModel.uiState.collectAsState()

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

    var stage by rememberSaveable(settingsUiState.settings.firstRun) {
        mutableStateOf(
            if (settingsUiState.settings.firstRun) RootStage.WELCOME else RootStage.WORKSPACE
        )
    }
    var startupProjectId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsUiState.settings.firstRun) {
        if (!settingsUiState.settings.firstRun) {
            stage = RootStage.WORKSPACE
        }
    }

    LaunchedEffect(onboardingUiState.completedProjectId) {
        val completedProjectId = onboardingUiState.completedProjectId ?: return@LaunchedEffect
        startupProjectId = completedProjectId
        stage = RootStage.WORKSPACE
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

            RootStage.WORKSPACE -> {
                WorkspaceShell(
                    onRecordTap = settingsViewModel::recordTap,
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
                        Text("Dismiss")
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
                Text(if (step == 1) "Back" else "Back to Name")
            }
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(28.dp))
                Text("Finalizing project ritual...")
            }
        }
    }
}

@Composable
private fun WorkspaceShell(
    onRecordTap: (String) -> Unit,
    initialProjectId: String?,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = hiltViewModel()
) {
    val projectsUiState by projectsViewModel.uiState.collectAsState()
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

    Row(modifier = modifier.fillMaxSize()) {
        WorkspaceLeftRail(
            currentTab = selectedTab,
            collapsed = leftRailCollapsed,
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
        if (!leftRailCollapsed) {
            VerticalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val projectTabContent: (@Composable (String) -> Unit)? = when (selectedTab) {
                DetailTab.BLUEPRINT -> { projectId ->
                    BlueprintScreen(
                        projectId = projectId,
                        initialShowAddons = false,
                        initialShowParams = false,
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
                    label = { Text("Project Name", style = MaterialTheme.typography.labelSmall) },
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
                    title = { Text("Start New Project?") },
                    text = {
                        Text(
                            "Save current project and keep it, or continue without saving and delete it."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showNewProjectConfirm = false
                                launchNewProject(true)
                            }
                        ) {
                            Text("Save & New")
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    showNewProjectConfirm = false
                                    launchNewProject(false)
                                }
                            ) {
                                Text("Delete & New")
                            }
                            TextButton(onClick = { showNewProjectConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceLeftRail(
    currentTab: DetailTab,
    collapsed: Boolean,
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
        Column(
            modifier = modifier
                .width(24.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                onClick = onToggleCollapsed,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
            ) {
                Text(
                    text = ">>",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp)
                )
            }
        }
        return
    }
    NavigationRail(
        modifier = modifier.width(62.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        header = {
            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleCollapsed,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = "<<",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Architecture,
                    contentDescription = "TradeSketch",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                WorkspaceRailActionItem(
                    label = "New+",
                    icon = Icons.Filled.Add,
                    selected = false,
                    onClick = onCreateNewProject,
                    onLongPress = {
                        Toast.makeText(context, "New Project", Toast.LENGTH_SHORT).show()
                    }
                )
                WorkspaceRailActionItem(
                    label = "Saved",
                    icon = Icons.Filled.Description,
                    selected = showSavedProjects,
                    onClick = onToggleSavedProjects,
                    onLongPress = {
                        Toast.makeText(context, "Saved Projects", Toast.LENGTH_SHORT).show()
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

@Composable
private fun WorkspaceRailActionItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.pointerInput(label) {
            detectTapGestures(
                onLongPress = {
                    onLongPress()
                }
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = true
    )
}

@Composable
private fun WorkspaceRailItem(
    tab: DetailTab,
    currentTab: DetailTab,
    onSelectTab: (DetailTab) -> Unit,
    onLongPress: () -> Unit
) {
    NavigationRailItem(
        selected = currentTab == tab,
        onClick = { onSelectTab(tab) },
        modifier = Modifier.pointerInput(tab) {
            detectTapGestures(
                onLongPress = {
                    onLongPress()
                }
            )
        },
        icon = {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label
            )
        },
        label = {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = false
    )
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
                Text("Saved Projects", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                    Text("Close")
                }
            }
            if (projects.isEmpty()) {
                Text(
                    "No projects saved yet.",
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
                                    modifier = Modifier.height(30.dp).width(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete project ${project.name}"
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
            title = { Text("Delete project?") },
            text = { Text("Delete \"${pendingProject.name}\" permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(pendingProject.id)
                        pendingDeleteProjectId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProjectId = null }) {
                    Text("Cancel")
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
                        text = "No project selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a starter project to continue in the blueprint-first workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onCreateStarterProject) {
                        Text("Create Starter Project")
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
