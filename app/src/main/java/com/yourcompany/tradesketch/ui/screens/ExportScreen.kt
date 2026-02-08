package com.yourcompany.tradesketch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.tradesketch.ui.viewmodel.ExportViewModel

@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Export Estimate",
            style = MaterialTheme.typography.titleLarge
        )
        
        if (uiState.result != null) {
            // Summary Card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = uiState.summaryContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Export Actions
            Button(
                onClick = {
                    viewModel.copyToClipboard()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy to Clipboard")
            }
            
            Button(
                onClick = {
                    val intent = viewModel.createShareIntent()
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
            
            OutlinedButton(
                onClick = { /* TODO: CSV export with SAF */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export as CSV")
            }
            
            OutlinedButton(
                onClick = { /* TODO: PDF export */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export as PDF")
            }
            
            // Feedback
            uiState.lastAction?.let { action ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = action,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Card {
                Text(
                    text = "No takeoff calculated yet. Go to the Takeoff tab to calculate material quantities.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
