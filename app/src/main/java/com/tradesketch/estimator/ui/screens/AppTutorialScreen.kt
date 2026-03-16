package com.tradesketch.estimator.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppTutorialScreen(
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember { tutorialSteps() }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val progress = (stepIndex + 1f) / steps.size.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cardMaxHeight = (maxHeight - 8.dp).coerceAtLeast(320.dp)
            val compactFooter = maxWidth < 420.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .heightIn(max = cardMaxHeight),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TutorialTag(label = "GET STARTED")
                            Text(
                                text = "Interactive Tour",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Step ${stepIndex + 1} of ${steps.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Learn the basics once, then start your first project with confidence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(
                                    initialOffsetX = { it / 4 },
                                    animationSpec = tween(240)
                                ) + fadeIn(animationSpec = tween(180)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 4 },
                                        animationSpec = tween(200)
                                    ) + fadeOut(animationSpec = tween(160))
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 4 },
                                    animationSpec = tween(240)
                                ) + fadeIn(animationSpec = tween(180)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { it / 4 },
                                        animationSpec = tween(200)
                                    ) + fadeOut(animationSpec = tween(160))
                            }
                        },
                        label = "app_tutorial_steps"
                    ) { index ->
                        val page = steps[index]
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = page.title,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = page.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                page.controls.forEachIndexed { controlIndex, control ->
                                    TutorialControlCard(
                                        step = controlIndex + 1,
                                        text = control
                                    )
                                }
                            }
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Field Tip",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = page.tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (compactFooter) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "You can replay this any time in Settings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stepIndex > 0) {
                                    OutlinedButton(onClick = { stepIndex -= 1 }) {
                                        Text("Back")
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (stepIndex == steps.lastIndex) onFinish() else stepIndex += 1
                                    }
                                ) {
                                    Text(if (stepIndex == steps.lastIndex) "Start Estimating" else "Next")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "You can replay this any time in Settings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stepIndex > 0) {
                                    OutlinedButton(onClick = { stepIndex -= 1 }) {
                                        Text("Back")
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (stepIndex == steps.lastIndex) onFinish() else stepIndex += 1
                                    }
                                ) {
                                    Text(if (stepIndex == steps.lastIndex) "Start Estimating" else "Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialTag(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TutorialControlCard(
    step: Int,
    text: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class TutorialStep(
    val title: String,
    val summary: String,
    val controls: List<String>,
    val tip: String,
    val icon: ImageVector
)

private fun tutorialSteps(): List<TutorialStep> = listOf(
    TutorialStep(
        title = "Navigation",
        summary = "The left rail keeps project controls and workspace tabs in one place.",
        controls = listOf(
            "Use New Project to start a fresh estimate.",
            "Use Projects to switch jobs or remove old ones.",
            "Tap Blueprint, Materials, Export, or Settings to change workspace.",
            "Use the arrow at the top of the rail to collapse or expand it."
        ),
        tip = "Start in Blueprint, then continue to Materials and Export when the drawing is ready.",
        icon = Icons.Filled.Description
    ),
    TutorialStep(
        title = "Blueprint Basics",
        summary = "All takeoff quantities come from what you draw here.",
        controls = listOf(
            "Select mode lets you tap walls/openings to inspect or edit.",
            "Draw mode lets you tap start and end points to create walls.",
            "Pinch to zoom and use two-finger drag to pan the drawing.",
            "Use the top project-name field to rename the active job."
        ),
        tip = "Zoom in before placing openings to avoid accidental misalignment.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Blueprint Controls",
        summary = "The bottom rail and side dials give you fast editing control.",
        controls = listOf(
            "Trash removes the selected wall or opening.",
            "Box mode draws a room rectangle: tap once, size it, tap again to finish.",
            "Door/Window/Stair tools open opening presets for wall placement.",
            "Angle and length side dials rotate and resize active lines, boxes, or picked-up walls."
        ),
        tip = "If an action gets stuck, use Cancel in touch tools to reset the current mode.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Joystick Controls",
        summary = "Use the right stick for the cursor and the left stick to pan the drawing.",
        controls = listOf(
            "Right stick moves the cursor. Small movement gives precision; full movement crosses the canvas quickly.",
            "Left stick pans the blueprint without changing zoom.",
            "Right-stick tap is your primary tap at cursor (start walls, place endpoints/openings, confirm).",
            "Left-stick tap and stick-press are alternate actions (cancel active draw/pick-up, quick-select, or clear nearby wall).",
            "Practice: right-stick to a corner, right-tap start, move, right-tap place, left-tap reset."
        ),
        tip = "Fine-tune in Settings: lower joystick sensitivity and raise dead zone for tighter control.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Materials",
        summary = "Validate quantities and tune estimating assumptions.",
        controls = listOf(
            "Review generated quantity rows from the blueprint geometry.",
            "Adjust waste factors, coverage rates, and trade-specific values.",
            "Use manual overrides for field conditions not shown in plan."
        ),
        tip = "Keep overrides minimal so totals stay traceable to the drawing.",
        icon = Icons.Filled.Assessment
    ),
    TutorialStep(
        title = "Export",
        summary = "Create outputs for clients, purchase lists, and field reference.",
        controls = listOf(
            "Choose PDF, CSV, JSON, text, or blueprint PNG output.",
            "For blueprint PNG, choose Grid On or Grid Off before saving.",
            "Use clear file names so teams can match exports to revision stages."
        ),
        tip = "Export after major geometry edits to keep documents synchronized.",
        icon = Icons.Filled.Flag
    ),
    TutorialStep(
        title = "Settings",
        summary = "Set app defaults so each new estimate starts with your standards.",
        controls = listOf(
            "Configure snap behavior, joystick settings, and touch preferences.",
            "Set unit, pricing, and business profile defaults.",
            "Use Replay Tutorial any time from Settings."
        ),
        tip = "Shared defaults improve consistency across teammates and devices.",
        icon = Icons.Filled.Settings
    )
)
