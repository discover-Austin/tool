package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.ui.components.ReferenceBlueprintBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintGold
import com.tradesketch.estimator.ui.components.ReferenceBlueprintGoldBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintInk
import com.tradesketch.estimator.ui.components.ReferenceBlueprintMuted
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavy
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavyDeep
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavyBright
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaper
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaperAlt
import com.tradesketch.estimator.ui.components.ReferenceIntroBackdrop
import com.tradesketch.estimator.ui.components.ReferenceIntroBrand
import com.tradesketch.estimator.ui.components.ReferenceIntroFooterBar

enum class WelcomeHeroMode {
    COLD_START,
    RETURNING_HOME
}

private val IntroTitleShadow = Shadow(
    color = Color.Black.copy(alpha = 0.34f),
    offset = Offset(0f, 4f),
    blurRadius = 14f
)

private val IntroCardTitleShadow = Shadow(
    color = Color.Black.copy(alpha = 0.28f),
    offset = Offset(0f, 3f),
    blurRadius = 10f
)

private val IntroHeaderBrush = Brush.verticalGradient(
    listOf(
        ReferenceBlueprintNavy.copy(alpha = 0.94f),
        ReferenceBlueprintNavyDeep.copy(alpha = 0.82f)
    )
)

@Composable
fun WelcomeScreenPro(
    heroMode: WelcomeHeroMode = WelcomeHeroMode.COLD_START,
    onBegin: () -> Unit,
    savedProjects: List<Project>,
    onOpenSavedProject: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAllSavedProjects by rememberSaveable(savedProjects.size) { mutableStateOf(false) }
    val visibleSavedProjects = if (showAllSavedProjects) savedProjects else savedProjects.take(3)

    ReferenceIntroBackdrop(
        modifier = modifier,
        bandHeight = 236.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top +
                            WindowInsetsSides.Bottom +
                            WindowInsetsSides.Start +
                            WindowInsetsSides.End
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            ReferenceIntroBrand(compact = true)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                WelcomeHeroHeader(
                    mode = heroMode,
                    savedProjectCount = savedProjects.size
                )
                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WelcomeActionCard(
                        title = "New Project",
                        subtitle = if (savedProjects.isEmpty()) {
                            "Start from a blueprint or enter the numbers by hand."
                        } else {
                            "Start a new job without changing the ones you've already saved."
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Construction,
                                contentDescription = null,
                                tint = ReferenceBlueprintGold,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        emphasized = true,
                        onClick = onBegin
                    )

                    WelcomeSavedProjectsPanel(
                        savedProjects = visibleSavedProjects,
                        totalSavedProjectCount = savedProjects.size,
                        showAllSavedProjects = showAllSavedProjects,
                        onToggleShowAll = { showAllSavedProjects = !showAllSavedProjects },
                        onOpenSavedProject = onOpenSavedProject
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                WelcomeFeatureRail(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReferenceIntroFooterBar()
        }
    }
}

@Composable
private fun WelcomeSavedProjectsPanel(
    savedProjects: List<Project>,
    totalSavedProjectCount: Int,
    showAllSavedProjects: Boolean,
    onToggleShowAll: () -> Unit,
    onOpenSavedProject: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.34f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (totalSavedProjectCount > 0) "Recent Projects" else stringResource(R.string.saved_projects),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ReferenceBlueprintNavyDeep
                    )
                    Text(
                        text = if (totalSavedProjectCount > 0) {
                            "Open a saved job and keep working."
                        } else {
                            "Saved projects will show up here."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                }
                if (totalSavedProjectCount > 3) {
                    TextButton(onClick = onToggleShowAll) {
                        Text(if (showAllSavedProjects) "Show less" else "Show all")
                    }
                }
            }
            if (savedProjects.isEmpty()) {
                WelcomeSavedProjectsEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 232.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedProjects.forEach { project ->
                        WelcomeSavedProjectRow(
                            project = project,
                            onClick = { onOpenSavedProject(project.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSavedProjectsEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReferenceBlueprintPaperAlt.copy(alpha = 0.78f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.96f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderCopy,
                        contentDescription = null,
                        tint = ReferenceBlueprintNavy,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(R.string.no_saved_projects),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ReferenceBlueprintInk
                )
                Text(
                    text = "Start a project above and it will be ready to reopen here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted
                )
            }
        }
    }
}

@Composable
private fun WelcomeSavedProjectRow(
    project: Project,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = ReferenceBlueprintPaperAlt.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.96f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderCopy,
                        contentDescription = null,
                        tint = ReferenceBlueprintNavy,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ReferenceBlueprintInk
                )
                Text(
                    text = stringResource(R.string.welcome_saved_project_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = project.name,
                tint = ReferenceBlueprintNavyDeep,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WelcomeHeroHeader(
    mode: WelcomeHeroMode,
    savedProjectCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shape = MaterialTheme.shapes.large,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IntroHeaderBrush)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = if (savedProjectCount > 0) {
                        "$savedProjectCount saved project" + if (savedProjectCount == 1) "" else "s"
                    } else {
                        "No saved projects yet"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.94f)
                )
            }
            Text(
                text = welcomeHeroHeadline(mode),
                style = MaterialTheme.typography.displaySmall.copy(shadow = IntroTitleShadow),
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = welcomeHeroSubtitle(mode, savedProjectCount),
                style = MaterialTheme.typography.titleMedium.copy(shadow = IntroTitleShadow),
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.94f)
            )
        }
    }
}

internal fun welcomeHeroHeadline(mode: WelcomeHeroMode): String {
    return when (mode) {
        WelcomeHeroMode.COLD_START -> "Start a new estimate."
        WelcomeHeroMode.RETURNING_HOME -> "Back to your projects."
    }
}

internal fun welcomeHeroSubtitle(
    mode: WelcomeHeroMode,
    savedProjectCount: Int
): String {
    return when {
        savedProjectCount > 0 -> "Open a saved project below or start a new one."
        mode == WelcomeHeroMode.RETURNING_HOME -> "Start a new project here any time."
        else -> "Start from a blueprint or enter your numbers manually. Everything stays on this device."
    }
}

@Composable
private fun WelcomeActionCard(
    title: String,
    subtitle: String?,
    icon: @Composable () -> Unit,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val containerBrush = if (emphasized) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF2B5F90),
                ReferenceBlueprintNavyBright,
                ReferenceBlueprintNavy
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                Color(0xFFF8F6EE),
                ReferenceBlueprintPaper
            )
        )
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (emphasized) 164.dp else 152.dp),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (emphasized) 1.35.dp else 1.15.dp,
            color = if (emphasized) {
                ReferenceBlueprintGoldBorder.copy(alpha = 0.75f)
            } else {
                ReferenceBlueprintBorder.copy(alpha = 0.48f)
            }
        ),
        shadowElevation = if (emphasized) 8.dp else 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = if (emphasized) 24.dp else 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (emphasized) Color.White.copy(alpha = 0.98f) else ReferenceBlueprintPaperAlt,
                    shape = CircleShape,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier.size(if (emphasized) 86.dp else 78.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            shadow = if (emphasized) IntroCardTitleShadow else null
                        ),
                        fontWeight = FontWeight.Black,
                        color = if (emphasized) Color.White else ReferenceBlueprintNavyDeep
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 3.dp)
                            .background(
                                if (emphasized) {
                                    ReferenceBlueprintGold
                                } else {
                                    ReferenceBlueprintNavyBright.copy(alpha = 0.82f)
                                }
                            )
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                shadow = if (emphasized) IntroCardTitleShadow else null
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = if (emphasized) {
                                Color.White.copy(alpha = 0.94f)
                            } else {
                                ReferenceBlueprintInk.copy(alpha = 0.94f)
                            }
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = title,
                    tint = if (emphasized) Color.White else ReferenceBlueprintNavyDeep,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRail(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.32f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WelcomeFeatureCell(
                icon = Icons.Filled.Architecture,
                label = "Blueprints",
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureDivider()
            WelcomeFeatureCell(
                icon = Icons.Filled.Inventory2,
                label = "Materials",
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureDivider()
            WelcomeFeatureCell(
                icon = Icons.Filled.Calculate,
                label = "Estimates",
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureDivider()
            WelcomeFeatureCell(
                icon = Icons.Filled.Description,
                label = "Reports",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WelcomeFeatureCell(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Surface(
            color = ReferenceBlueprintPaperAlt,
            border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.24f)),
            shadowElevation = 1.dp,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ReferenceBlueprintNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ReferenceBlueprintMuted
        )
    }
}

@Composable
private fun WelcomeFeatureDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 52.dp)
            .background(ReferenceBlueprintBorder.copy(alpha = 0.16f))
    )
}
