package com.yourcompany.tradesketch.ui.screens

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // CSV export launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(viewModel.getCSVContent().toByteArray())
                    }
                    viewModel.setLastAction("CSV exported successfully")
                } catch (e: Exception) {
                    viewModel.setError("Failed to export CSV: ${e.message}")
                }
            }
        }
    }
    
    // PDF export launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val pdfDocument = createPdfDocument(
                            viewModel.uiState.value.project?.name ?: "Project",
                            viewModel.uiState.value.takeoffType,
                            viewModel.uiState.value.textContent
                        )
                        pdfDocument.writeTo(outputStream)
                        pdfDocument.close()
                    }
                    viewModel.setLastAction("PDF exported successfully")
                } catch (e: Exception) {
                    viewModel.setError("Failed to export PDF: ${e.message}")
                }
            }
        }
    }
    
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
                onClick = { 
                    csvLauncher.launch(viewModel.getCSVFileName())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export as CSV")
            }
            
            OutlinedButton(
                onClick = { 
                    pdfLauncher.launch(viewModel.getPdfFileName())
                },
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

/**
 * Creates a PDF document with the estimate details
 */
private fun createPdfDocument(projectName: String, takeoffType: String, content: String): PdfDocument {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val page = document.startPage(pageInfo)
    
    val canvas = page.canvas
    val paint = Paint()
    
    // Title
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 20f
    canvas.drawText("TradeSketch Estimator", 40f, 50f, paint)
    
    // Project info
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 14f
    var yPos = 80f
    
    canvas.drawText("Project: $projectName", 40f, yPos, paint)
    yPos += 20f
    canvas.drawText("Takeoff Type: $takeoffType", 40f, yPos, paint)
    yPos += 40f
    
    // Content
    paint.textSize = 12f
    val lines = content.split("\n")
    for (line in lines) {
        if (yPos > 800f) break // Prevent overflow for now
        canvas.drawText(line, 40f, yPos, paint)
        yPos += 15f
    }
    
    document.finishPage(page)
    return document
}
