package com.tradesketch.estimator.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Quick Tutorial",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Step ${stepIndex + 1} of ${steps.size}",
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
                    modifier = Modifier.fillMaxWidth()
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = page.title,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = page.summary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            page.controls.forEach { control ->
                                Text(
                                    text = "• $control",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "Tip: ${page.tip}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "You can replay this any time in Settings/About.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stepIndex > 0) {
                            OutlinedButton(
                                onClick = { stepIndex -= 1 }
                            ) {
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

private data class TutorialStep(
    val title: String,
    val summary: String,
    val controls: List<String>,
    val tip: String,
    val icon: ImageVector
)

private fun tutorialSteps(): List<TutorialStep> = listOf(
    TutorialStep(
        title = "Workspace Rail",
        summary = "The left rail controls project flow and global navigation.",
        controls = listOf(
            "Use New+ to create a fresh project.",
            "Use Saved to switch between jobs or remove old ones.",
            "Tap Blueprint, Materials, Export, or Settings/About to change workspace.",
            "Use the arrow at top of rail to collapse or expand it."
        ),
        tip = "When learning, stay in Blueprint first, then move to Materials and Export.",
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
        title = "Dual Joystick Mastery",
        summary = "Use right stick for cursor and left stick for camera pan, then tap from either side for action flow.",
        controls = listOf(
            "Right stick moves the cursor. Small movement gives precision; full movement crosses the canvas quickly.",
            "Left stick pans the blueprint without changing zoom.",
            "Left-stick tap is your primary tap at cursor (start walls, place endpoints/openings, confirm).",
            "Right-stick tap is alternate action (cancel active draw/pick-up, quick-select or clear nearby wall).",
            "Practice: right-stick to a corner, left-tap start, move, left-tap place, right-tap reset."
        ),
        tip = "Fine-tune in Settings/About: lower joystick sensitivity and raise deadzone for tighter control.",
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
            "Use Replay Tutorial any time from Settings/About."
        ),
        tip = "Shared defaults improve consistency across teammates and devices.",
        icon = Icons.Filled.Settings
    )
)
