package com.tradesketch.estimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.tradesketch.estimator.ui.screens.*
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradeSketchTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TradeSketchRoot()
                }
            }
        }
    }
}

@Composable
private fun TradeSketchRoot() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Projects.route,
        modifier = modifier
    ) {
        composable(Route.Projects.route) {
            ProjectsScreen(
                onNavigateToProject = { projectId ->
                    navController.navigate("${Route.ProjectDetail.route}/$projectId")
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(
            route = "${Route.ProjectDetail.route}/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ProjectDetailWithTabs(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Route.Settings.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("App Settings") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                SettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProjectDetailWithTabs(
    projectId: String,
    onNavigateBack: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = DetailTab.entries.find { it.route == currentRoute } ?: DetailTab.MODEL
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Project Workspace")
                        Text(
                            text = "Step ${selectedTab.step} of 4 - ${selectedTab.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                DetailTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DetailTab.MODEL.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(DetailTab.MODEL.route) {
                ProjectDetailScreen(
                    projectId = projectId,
                    onOpenBlueprint = {
                        navController.navigate(DetailTab.BLUEPRINT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenTakeoff = {
                        navController.navigate(DetailTab.TAKEOFF.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenExport = {
                        navController.navigate(DetailTab.EXPORT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(DetailTab.BLUEPRINT.route) {
                BlueprintScreen(
                    projectId = projectId,
                    onOpenModel = {
                        navController.navigate(DetailTab.MODEL.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenTakeoff = {
                        navController.navigate(DetailTab.TAKEOFF.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenExport = {
                        navController.navigate(DetailTab.EXPORT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(DetailTab.TAKEOFF.route) {
                TakeoffScreen(
                    projectId = projectId,
                    onOpenModel = {
                        navController.navigate(DetailTab.MODEL.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBlueprint = {
                        navController.navigate(DetailTab.BLUEPRINT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenExport = {
                        navController.navigate(DetailTab.EXPORT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(DetailTab.EXPORT.route) {
                ExportScreen(
                    projectId = projectId,
                    onOpenModel = {
                        navController.navigate(DetailTab.MODEL.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBlueprint = {
                        navController.navigate(DetailTab.BLUEPRINT.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenTakeoff = {
                        navController.navigate(DetailTab.TAKEOFF.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private sealed class Route(val route: String) {
    data object Projects : Route("projects")
    data object ProjectDetail : Route("project_detail")
    data object Settings : Route("settings")
}

private enum class DetailTab(
    val route: String,
    val label: String,
    val step: Int,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    MODEL("tab_model", "Model", 1, "Define spaces and project structure", Icons.Filled.Architecture),
    BLUEPRINT("tab_blueprint", "Blueprint", 2, "Build layout in dedicated 3D studio", Icons.Filled.AutoFixHigh),
    TAKEOFF("tab_takeoff", "Takeoff", 3, "Tune quantities by trade", Icons.Filled.Assessment),
    EXPORT("tab_export", "Export", 4, "Share professional outputs", Icons.Filled.Description)
}
