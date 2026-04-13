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
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation

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
                colors = appCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = appCardBorder(accented = true),
                elevation = appCardElevation(raised = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp, max = cardMaxHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
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
                                    text = "Short version: this shows where things are and what to tap first.",
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
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "You can replay this any time in Settings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (compactFooter) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (stepIndex > 0) {
                                        OutlinedButton(onClick = { stepIndex -= 1 }) {
                                            Text("Back")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stepIndex > 0) {
                                    OutlinedButton(onClick = { stepIndex -= 1 }) {
                                        Text("Back")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.widthIn(min = 88.dp))
                                }
                                Spacer(modifier = Modifier.weight(1f))
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
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
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
        summary = "Use the left side of the app to move between the main screens.",
        controls = listOf(
            "Tap New Project to start a new job.",
            "Tap Projects to open an old job or delete one.",
            "Tap Blueprint, Materials, Export, or Settings to switch screens.",
            "Tap the arrow to hide or show the left menu."
        ),
        tip = "Most jobs start in Blueprint.",
        icon = Icons.Filled.Description
    ),
    TutorialStep(
        title = "Blueprint Basics",
        summary = "Draw the job here first. The rest of the app uses this drawing.",
        controls = listOf(
            "Select lets you tap a wall or opening to inspect it.",
            "Draw lets you place new walls.",
            "Pinch to zoom. Use two fingers to move the drawing.",
            "Rename the job at the top when you want."
        ),
        tip = "Zoom in before you place doors or windows.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Blueprint Controls",
        summary = "The bottom bar holds the main drawing tools.",
        controls = listOf(
            "Trash deletes the thing you selected.",
            "Box draws a rectangle fast.",
            "Door, Window, and Stair open placement tools.",
            "The side dials fine-tune angle and size after you start a shape."
        ),
        tip = "If the app feels stuck, use Cancel to clear the current action.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Joystick Controls",
        summary = "Right stick aims. Left stick moves the drawing.",
        controls = listOf(
            "Move the right stick a little for tiny moves and a lot for big moves.",
            "Use right-stick tap to do the main action at the cursor.",
            "Use the left stick to pan without changing zoom.",
            "Use left tap for quick extra actions like cancel or reset."
        ),
        tip = "If it feels too fast, lower joystick sensitivity in Settings.",
        icon = Icons.Filled.Architecture
    ),
    TutorialStep(
        title = "Materials",
        summary = "Check the numbers here and adjust how the estimate is calculated.",
        controls = listOf(
            "Review the quantities made from your drawing.",
            "Adjust waste, coverage, and other estimate settings.",
            "Add manual overrides only when the drawing does not show something important."
        ),
        tip = "Use as few manual overrides as you can.",
        icon = Icons.Filled.Assessment
    ),
    TutorialStep(
        title = "Export",
        summary = "Save or share the estimate in the format you need.",
        controls = listOf(
            "You can export PDF, CSV, JSON, text, or a blueprint image.",
            "For blueprint images, choose whether the grid is on or off.",
            "Use clear file names so you can tell versions apart later."
        ),
        tip = "Export again after big drawing changes.",
        icon = Icons.Filled.Flag
    ),
    TutorialStep(
        title = "Settings",
        summary = "Set your default behavior here so new jobs start the way you like.",
        controls = listOf(
            "Change snap, joystick, and touch behavior.",
            "Set your units, pricing defaults, and business info.",
            "Use Replay Tutorial any time if you forget something."
        ),
        tip = "Good defaults save time on every new job.",
        icon = Icons.Filled.Settings
    )
)
