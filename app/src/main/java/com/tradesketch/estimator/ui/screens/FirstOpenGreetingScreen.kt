package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.ui.components.ReferenceBlueprintBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintGold
import com.tradesketch.estimator.ui.components.ReferenceBlueprintInk
import com.tradesketch.estimator.ui.components.ReferenceBlueprintMuted
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavy
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavyBright
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaper
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaperAlt
import com.tradesketch.estimator.ui.components.ReferenceIntroBackdrop
import com.tradesketch.estimator.ui.components.ReferenceIntroBrand
import com.tradesketch.estimator.ui.components.ReferenceIntroFooterBar

@Composable
fun FirstOpenGreetingScreen(
    reducedMotionEnabled: Boolean,
    onTakeMeThere: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sectionSpacing = if (reducedMotionEnabled) 16.dp else 18.dp

    ReferenceIntroBackdrop(
        modifier = modifier,
        bandHeight = 188.dp
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
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            ReferenceIntroBrand(compact = true)
            Spacer(modifier = Modifier.height(26.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp),
                    shape = MaterialTheme.shapes.large,
                    color = ReferenceBlueprintPaper,
                    border = BorderStroke(1.2.dp, ReferenceBlueprintBorder.copy(alpha = 0.8f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White,
                                        Color(0xFFF2EFE4),
                                        Color(0xFFE9E3D3)
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                    ) {
                        Surface(
                            color = ReferenceBlueprintNavy,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "First time here?",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Start your first estimate.",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = ReferenceBlueprintInk
                            )
                            Text(
                                text = "Draw a layout or enter the numbers by hand. TradeSketch keeps materials, pricing, and exports in one place.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ReferenceBlueprintInk.copy(alpha = 0.9f)
                            )
                        }

                        FirstOpenBenefitsCard(modifier = Modifier.fillMaxWidth())

                        Surface(
                            color = Color.White.copy(alpha = 0.78f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = "No account, no subscription. Your projects stay on this device unless you export them.",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ReferenceBlueprintMuted
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            onClick = onTakeMeThere,
                            color = ReferenceBlueprintNavyBright,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, ReferenceBlueprintGold.copy(alpha = 0.72f)),
                            shadowElevation = 3.dp
                        ) {
                            Text(
                                text = "Set Up First Project",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReferenceIntroFooterBar()
        }
    }
}

@Composable
private fun FirstOpenBenefitsCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ReferenceBlueprintPaperAlt.copy(alpha = 0.86f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "What you can do right away",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ReferenceBlueprintMuted
            )
            FirstOpenBenefitRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Architecture,
                        contentDescription = null,
                        tint = ReferenceBlueprintGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = "Choose your start",
                description = "Sketch the layout or enter the job by hand."
            )
            FirstOpenBenefitRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = ReferenceBlueprintGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = "Review materials and totals",
                description = "Pricing and quantities stay together while you work."
            )
            FirstOpenBenefitRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = ReferenceBlueprintGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = "Send it out when ready",
                description = "Export PDF, PNG, CSV, or JSON from the same project."
            )
        }
    }
}

@Composable
private fun FirstOpenBenefitRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = ReferenceBlueprintNavy,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ReferenceBlueprintInk
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ReferenceBlueprintMuted
            )
        }
    }
}
