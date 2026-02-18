package com.tradesketch.estimator

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.screens.BlueprintScreen
import com.tradesketch.estimator.ui.screens.ExportScreen
import com.tradesketch.estimator.ui.screens.ProjectDetailScreen
import com.tradesketch.estimator.ui.screens.ProjectsScreen
import com.tradesketch.estimator.ui.screens.SettingsScreen
import com.tradesketch.estimator.ui.screens.TakeoffScreen
import com.tradesketch.estimator.ui.theme.Midnight900
import com.tradesketch.estimator.ui.theme.Midnight950
import com.tradesketch.estimator.ui.theme.Slate800
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
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
            // Keep transparent nav bar behavior consistent across gesture and 3-button navigation.
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

@Composable
private fun TradeSketchRoot() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    AppNavHost(
        navController = navController,
        appSettings = settingsUiState.settings,
        onRecordTap = settingsViewModel::recordTap,
        onUpdatePrimaryTrade = settingsViewModel::updatePrimaryTrade,
        onCompleteWelcome = {
            settingsViewModel.completeWelcomeOnboarding()
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppNavHost(
    navController: NavHostController,
    appSettings: Settings,
    onRecordTap: (String) -> Unit,
    onUpdatePrimaryTrade: (PrimaryTrade) -> Unit,
    onCompleteWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (appSettings.firstRun) {
            Route.Welcome.route
        } else {
            Route.Workspace.route
        },
        modifier = modifier
    ) {
        composable(Route.Welcome.route) {
            WelcomeOnboardingScreen(
                onBeginWorkspace = {
                    onRecordTap("welcome_begin_workspace")
                    onCompleteWelcome()
                    navController.navigate(Route.Workspace.route) {
                        popUpTo(Route.Welcome.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(Route.Workspace.route) {
            WorkspaceShell(
                appSettings = appSettings,
                onRecordTap = onRecordTap,
                onUpdatePrimaryTrade = onUpdatePrimaryTrade,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}

@Composable
private fun WelcomeOnboardingScreen(
    onBeginWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = showDetail,
            transitionSpec = {
                if (targetState) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(340)
                    ) + fadeIn(animationSpec = tween(260)) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(220))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(260)
                    ) + fadeIn(animationSpec = tween(220)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(240)
                        ) + fadeOut(animationSpec = tween(200))
                }
            },
            label = "welcome_transition"
        ) { detailVisible ->
            if (!detailVisible) {
                WelcomeHeroCard(
                    onContinue = { showDetail = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                WelcomeDetailCard(
                    onBack = { showDetail = false },
                    onBeginWorkspace = onBeginWorkspace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeroCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Architecture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Text(
                text = "TradeSketch",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Precision blueprint estimating for drywall, concrete, paint, and bed work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Begin")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun WelcomeDetailCard(
    onBack: () -> Unit,
    onBeginWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Build It The Right Way",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Name the project, choose what you are estimating, then let the blueprint drive everything.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "What you can do immediately:",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "• Precise wall drafting with chain, angle, and snap controls",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Room/slab/bed labeling and color-coded blueprint layers",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Door/window placement and estimate-ready quantities",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecondaryActionButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onBeginWorkspace,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enter Workspace")
                }
            }
        }
    }
}

@Composable
private fun WorkspaceShell(
    appSettings: Settings,
    onRecordTap: (String) -> Unit,
    onUpdatePrimaryTrade: (PrimaryTrade) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.PROJECTS) }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val navigateToTab: (DetailTab) -> Unit = { tab ->
        if (selectedTab != tab) {
            onRecordTap("workspace_tab_${selectedTab.route}_to_${tab.route}")
        }
        selectedTab = tab
    }

    Row(modifier = modifier.fillMaxSize()) {
        WorkspaceLeftRail(
            currentTab = selectedTab,
            onSelectTab = navigateToTab,
            modifier = Modifier.fillMaxHeight()
        )
        VerticalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (selectedTab) {
                DetailTab.PROJECTS -> {
                    if (selectedProjectId == null) {
                        ProjectsScreen(
                            onNavigateToProject = { projectId ->
                                selectedProjectId = projectId
                                navigateToTab(DetailTab.BLUEPRINT)
                            },
                            onNavigateToSettings = { navigateToTab(DetailTab.SETTINGS_ABOUT) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Project Ritual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SecondaryActionButton(
                                    onClick = { selectedProjectId = null }
                                ) {
                                    Text("Switch Project")
                                }
                            }
                            ProjectSetupScreen(
                                projectId = selectedProjectId!!,
                                selectedTrade = appSettings.primaryTrade,
                                onSelectTrade = { trade ->
                                    onRecordTap("workspace_select_estimate_${trade.name.lowercase()}")
                                    if (trade != appSettings.primaryTrade) {
                                        onUpdatePrimaryTrade(trade)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                DetailTab.BLUEPRINT -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        BlueprintScreen(
                            projectId = projectId,
                            onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.MATERIALS -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        TakeoffScreen(
                            projectId = projectId,
                            onOpenModel = { navigateToTab(DetailTab.REVIEW) },
                            onOpenBlueprint = { navigateToTab(DetailTab.BLUEPRINT) },
                            onOpenExport = { navigateToTab(DetailTab.EXPORT) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.QUANTITIES -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        TakeoffScreen(
                            projectId = projectId,
                            onOpenModel = { navigateToTab(DetailTab.REVIEW) },
                            onOpenBlueprint = { navigateToTab(DetailTab.BLUEPRINT) },
                            onOpenExport = { navigateToTab(DetailTab.EXPORT) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.ADDONS -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        BlueprintScreen(
                            projectId = projectId,
                            onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.REVIEW -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        ProjectDetailScreen(
                            projectId = projectId,
                            onOpenBlueprint = { navigateToTab(DetailTab.BLUEPRINT) },
                            onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.EXPORT -> {
                    ProjectScopedTab(
                        selectedProjectId = selectedProjectId,
                        onOpenProjects = { navigateToTab(DetailTab.PROJECTS) }
                    ) { projectId ->
                        ExportScreen(
                            projectId = projectId,
                            onOpenTakeoff = { navigateToTab(DetailTab.MATERIALS) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                DetailTab.SETTINGS_ABOUT -> {
                    SettingsScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun WorkspaceLeftRail(
    currentTab: DetailTab,
    onSelectTab: (DetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Architecture,
                        contentDescription = "TradeSketch",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    ) {
        DetailTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = { onSelectTab(tab) },
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
    }
}

@Composable
private fun ProjectScopedTab(
    selectedProjectId: String?,
    onOpenProjects: () -> Unit,
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
                        text = "Select a project first",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Use the Projects tab to create or open a project before estimating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SecondaryActionButton(onClick = onOpenProjects) {
                        Text("Open Projects")
                    }
                }
            }
        }
    } else {
        content(selectedProjectId)
    }
}

@Composable
private fun ProjectSetupScreen(
    projectId: String,
    selectedTrade: PrimaryTrade,
    onSelectTrade: (PrimaryTrade) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var projectNameDraft by rememberSaveable(projectId) { mutableStateOf("") }
    val options = rememberEstimateOptions()

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }
    LaunchedEffect(uiState.project?.name) {
        projectNameDraft = uiState.project?.name.orEmpty()
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Project",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            OutlinedTextField(
                value = projectNameDraft,
                onValueChange = { projectNameDraft = it },
                label = { Text("Project Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SecondaryActionButton(
                onClick = {
                    val normalized = projectNameDraft.trim()
                    if (normalized.isNotBlank()) {
                        viewModel.updateProjectName(normalized)
                    }
                },
                enabled = projectNameDraft.trim().isNotBlank() &&
                    projectNameDraft.trim() != uiState.project?.name,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Name")
            }
        }
        item {
            Text(
                text = "What Are We Estimating?",
                style = MaterialTheme.typography.titleSmall
            )
        }
        items(options, key = { it.trade.name }) { option ->
            val isSelected = option.trade == selectedTrade
            Card(
                onClick = {
                    onSelectTrade(option.trade)
                    viewModel.applyProjectEstimateProfile(option.trade)
                },
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                } else {
                    null
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

private data class EstimateOption(
    val trade: PrimaryTrade,
    val title: String,
    val description: String
)

private fun rememberEstimateOptions(): List<EstimateOption> {
    return listOf(
        EstimateOption(
            trade = PrimaryTrade.DRYWALL,
            title = "Drywall",
            description = "Walls and rooms with sheet, screw, and mud quantities."
        ),
        EstimateOption(
            trade = PrimaryTrade.CONCRETE,
            title = "Concrete",
            description = "Footprint and thickness-driven slab calculations."
        ),
        EstimateOption(
            trade = PrimaryTrade.PAINT,
            title = "Paint",
            description = "Coverage-based takeoff with coats and waste settings."
        ),
        EstimateOption(
            trade = PrimaryTrade.GRAVEL_MULCH,
            title = "Gravel / Mulch",
            description = "Ground coverage with depth and density assumptions."
        ),
        EstimateOption(
            trade = PrimaryTrade.MULTI,
            title = "Multi-Trade",
            description = "Flexible workspace for mixed estimate projects."
        )
    )
}

private sealed class Route(val route: String) {
    data object Welcome : Route("welcome")
    data object Workspace : Route("workspace")
}

private enum class DetailTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    PROJECTS("tab_projects", "Projects", Icons.Filled.FolderOpen),
    BLUEPRINT("tab_blueprint", "Blueprint", Icons.Filled.AutoFixHigh),
    MATERIALS("tab_materials", "Materials", Icons.Filled.Assessment),
    QUANTITIES("tab_quantities", "Qty", Icons.Filled.Straighten),
    ADDONS("tab_addons", "Add-ons", Icons.Filled.Add),
    REVIEW("tab_review", "Review", Icons.Filled.Description),
    EXPORT("tab_export", "Export", Icons.Filled.Share),
    SETTINGS_ABOUT("tab_settings", "Settings", Icons.Filled.Tune)
}
