package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val topOffset = when {
            maxHeight > 840.dp -> 88.dp
            maxHeight > 700.dp -> 52.dp
            else -> 12.dp
        }
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
                colors = appCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = appCardBorder(accented = true),
                elevation = appCardElevation(raised = true)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.project_setup_step_1),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.name_this_project),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.enter_project_name_to_continue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = onProjectNameChange,
                        label = { Text(stringResource(R.string.project_name_lower)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
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
