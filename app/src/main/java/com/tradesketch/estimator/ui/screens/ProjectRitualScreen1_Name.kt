package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R
import com.tradesketch.estimator.ui.components.CenteredLabelTrailingIcon
import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation
import com.tradesketch.estimator.ui.components.appOutlinedTextFieldColors

@Composable
fun ProjectRitualScreen1_Name(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validName = projectName.trim().isNotEmpty()
    val paperColor = Color(0xFFF3F0E8)
    val paperBorder = Color(0xFF9AAAB5)
    val headerStart = Color(0xFF173B53)
    val headerEnd = Color(0xFF0C2535)
    val accent = Color(0xFF2B7396)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(paperColor)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val topOffset = when {
            maxHeight > 840.dp -> 88.dp
            maxHeight > 700.dp -> 52.dp
            else -> 12.dp
        }
        val headerHeight = if (maxHeight > 700.dp) 220.dp else 170.dp
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(Brush.verticalGradient(listOf(headerStart, headerEnd)))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topOffset),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    colors = appCardColors(containerColor = paperColor),
                    border = BorderStroke(width = 1.dp, color = paperBorder),
                    elevation = appCardElevation(raised = true)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(headerStart, headerEnd)))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.Transparent,
                                border = BorderStroke(width = 1.dp, color = Color.White.copy(alpha = 0.36f))
                            ) {
                                Text(
                                    text = stringResource(R.string.project_setup_step_1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = headerStart,
                                border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DriveFileRenameOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.name_this_project),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Set the working project name first so exports, summaries, and saved files all identify the job correctly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.enter_project_name_to_continue),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5F6F)
                            )
                            OutlinedTextField(
                                value = projectName,
                                onValueChange = onProjectNameChange,
                                label = { Text(stringResource(R.string.project_name_lower)) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = appOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            PrimaryActionButton(
                                onClick = onContinue,
                                enabled = validName,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CenteredLabelTrailingIcon(
                                    label = stringResource(R.string.continue_button),
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
