package com.yourcompany.tradesketch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import com.yourcompany.tradesketch.ui.theme.TradeSketchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradeSketchTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TradeSketchApp()
                }
            }
        }
    }
}

@Composable
private fun TradeSketchApp() {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "TradeSketch Estimator") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_manage),
                            contentDescription = "Open settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = { ProjectFab(navController) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun ProjectFab(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    if (route == Route.Projects.route) {
        FloatingActionButton(onClick = {}) {
            Icon(
                painter = painterResource(android.R.drawable.ic_input_add),
                contentDescription = "Create new project"
            )
        }
    }
}

@Composable
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
            ProjectListScreen(
                onOpenProject = { navController.navigate(Route.ProjectDetail.route) },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(Route.ProjectDetail.route) {
            ProjectDetailScreen()
        }
    }
}

@Composable
private fun ProjectListScreen(
    onOpenProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = listOf(
        "Bedroom template",
        "Garage template",
        "Driveway slab",
        "Mulch bed"
    )
    val recentProjects = listOf(
        "Hillcrest Garage",
        "Maple Street Bedroom"
    )
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Recent projects",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (recentProjects.isEmpty()) {
            item {
                Text(
                    text = "No projects yet. Start with a template or create a blank project.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(recentProjects) { project ->
                ProjectRow(
                    title = project,
                    subtitle = "Updated just now",
                    onOpen = onOpenProject
                )
            }
        }
        item {
            Text(
                text = "Start with a template",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(templates) { template ->
            TemplateCard(
                title = template,
                description = "Pre-fill common dimensions to get a takeoff faster."
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Or create a blank project",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenProject,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(text = "Create blank project")
            }
        }
    }
}

@Composable
private fun TemplateCard(title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {}) {
                Text(text = "Use")
            }
        }
    }
}

@Composable
private fun ProjectRow(
    title: String,
    subtitle: String,
    onOpen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onOpen) {
                Text(text = "Open")
            }
        }
    }
}

@Composable
private fun ProjectDetailScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route ?: DetailRoute.Model.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                DetailRoute.items.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(text = destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DetailRoute.Model.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(DetailRoute.Model.route) {
                DetailPlaceholder(
                    title = "Model spaces",
                    description = "Add rooms, walls, slabs, and yard beds."
                )
            }
            composable(DetailRoute.Takeoff.route) {
                DetailPlaceholder(
                    title = "Takeoff",
                    description = "Choose a preset and compute material quantities."
                )
            }
            composable(DetailRoute.Export.route) {
                DetailPlaceholder(
                    title = "Export",
                    description = "Create CSV or PDF estimates to share."
                )
            }
        }
    }
}

@Composable
private fun DetailPlaceholder(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
    }
}

private sealed class Route(val route: String) {
    data object Projects : Route("projects")
    data object ProjectDetail : Route("project_detail")
}

private sealed class DetailRoute(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Model : DetailRoute("detail_model", "Model", Icons.Filled.Architecture)
    data object Takeoff : DetailRoute("detail_takeoff", "Takeoff", Icons.Filled.Assessment)
    data object Export : DetailRoute("detail_export", "Export", Icons.Filled.Description)

    companion object {
        val items = listOf(Model, Takeoff, Export)
    }
}
