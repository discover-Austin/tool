package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R
import com.tradesketch.estimator.ui.components.CenteredLabelTrailingIcon
import com.tradesketch.estimator.ui.components.PrimaryActionButton

@Composable
fun WelcomeScreenPro(
    onBegin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        val compactHeight = maxHeight < 700.dp
        val wideLandscapeLayout = compactHeight && maxWidth > 720.dp
        val landscapePanelWidth = ((maxWidth - 58.dp) / 2).coerceAtMost(340.dp)
        val rootModifier = if (compactHeight && !wideLandscapeLayout) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(rootModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (compactHeight) Arrangement.Top else Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (wideLandscapeLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WelcomeHeroPanel(
                            compact = true,
                            modifier = Modifier.width(landscapePanelWidth)
                        )
                        WelcomeFeatureColumn(
                            onBegin = onBegin,
                            compact = true,
                            modifier = Modifier
                                .width(landscapePanelWidth)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        WelcomeHeroPanel(
                            compact = compactHeight,
                            modifier = Modifier.fillMaxWidth()
                        )
                        WelcomeFeatureColumn(
                            onBegin = onBegin,
                            compact = compactHeight,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeroPanel(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val heroPadding = if (compact) 18.dp else 24.dp
    val heroSpacing = if (compact) 10.dp else 14.dp
    val heroIconSize = if (compact) 24.dp else 30.dp
    val titleStyle = if (compact) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineLarge
    }
    val bodyStyle = if (compact) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(heroPadding),
            verticalArrangement = Arrangement.spacedBy(heroSpacing)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.26f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Architecture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(heroIconSize)
                )
            }
            Text(
                text = stringResource(R.string.welcome_title),
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Start a project, sketch the job, and export a clear estimate from one workspace.",
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun WelcomeFeatureColumn(
    onBegin: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)
    ) {
        WelcomeFeatureRow(
            icon = Icons.Filled.Architecture,
            title = "Draw the job clearly",
            summary = "Blueprint geometry stays at the center so every downstream number has context."
        )
        WelcomeFeatureRow(
            icon = Icons.Filled.Assessment,
            title = "Turn scope into takeoff fast",
            summary = "Quantities, materials, and pricing stay in sync with your active project."
        )
        WelcomeFeatureRow(
            icon = Icons.Filled.Description,
            title = "Present work professionally",
            summary = "Export client-ready estimates, shopping lists, and blueprint sheets from the same workspace."
        )
        Spacer(modifier = Modifier.height(2.dp))
        PrimaryActionButton(
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth()
        ) {
            CenteredLabelTrailingIcon(
                label = stringResource(R.string.begin),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    summary: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
