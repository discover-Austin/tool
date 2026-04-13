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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.TakeoffInputMode
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

private val ProjectTypeTitleShadow = Shadow(
    color = Color.Black.copy(alpha = 0.34f),
    offset = Offset(0f, 4f),
    blurRadius = 14f
)

private val ProjectTypeCardTitleShadow = Shadow(
    color = Color.Black.copy(alpha = 0.28f),
    offset = Offset(0f, 3f),
    blurRadius = 10f
)

private val ProjectTypeHeaderBrush = Brush.verticalGradient(
    listOf(
        ReferenceBlueprintNavy.copy(alpha = 0.94f),
        ReferenceBlueprintNavyDeep.copy(alpha = 0.82f)
    )
)

enum class ProjectTypePlateOption(
    val inputMode: TakeoffInputMode
) {
    BLUEPRINT(TakeoffInputMode.BLUEPRINT),
    MANUAL(TakeoffInputMode.MANUAL)
}

@Composable
fun ProjectTypePlateScreen(
    onSelectOption: (ProjectTypePlateOption) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReferenceIntroBackdrop(
        modifier = modifier,
        bandHeight = 166.dp
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
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                ProjectTypeHeader()
                Spacer(modifier = Modifier.height(18.dp))

                ProjectTypeModeCard(
                    title = "Start with Blueprint",
                    subtitle = "Sketch the layout first, then use those measurements in the estimate.",
                    accentLabel = "Best when you need to measure the job",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Architecture,
                            contentDescription = null,
                            tint = ReferenceBlueprintGold,
                            modifier = Modifier.size(46.dp)
                        )
                    },
                    emphasized = true,
                    onClick = { onSelectOption(ProjectTypePlateOption.BLUEPRINT) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProjectTypeModeCard(
                    title = "Manual Entry",
                    subtitle = "Skip the drawing and enter the measurements and pricing directly.",
                    accentLabel = "Best when you already know the numbers",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = ReferenceBlueprintNavyBright,
                            modifier = Modifier.size(42.dp)
                        )
                    },
                    emphasized = false,
                    onClick = { onSelectOption(ProjectTypePlateOption.MANUAL) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                ProjectTypeActions(
                    onBack = onBack
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            ReferenceIntroFooterBar()
        }
    }
}

@Composable
private fun ProjectTypeHeader() {
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
                .background(ProjectTypeHeaderBrush)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "How do you want to start?",
                style = MaterialTheme.typography.displaySmall.copy(shadow = ProjectTypeTitleShadow),
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Choose the setup that fits this job.",
                style = MaterialTheme.typography.titleSmall.copy(shadow = ProjectTypeTitleShadow),
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.94f)
            )
        }
    }
}

@Composable
private fun ProjectTypeModeCard(
    title: String,
    subtitle: String,
    accentLabel: String,
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
            .heightIn(min = 154.dp),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (emphasized) 1.35.dp else 1.15.dp,
            color = if (emphasized) {
                ReferenceBlueprintGoldBorder.copy(alpha = 0.72f)
            } else {
                ReferenceBlueprintBorder.copy(alpha = 0.44f)
            }
        ),
        shadowElevation = if (emphasized) 8.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    containerBrush
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
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
                    modifier = Modifier.size(84.dp),
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
                        shadow = if (emphasized) ProjectTypeCardTitleShadow else null
                    ),
                    fontWeight = FontWeight.Black,
                    color = if (emphasized) Color.White else ReferenceBlueprintNavyDeep
                )
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 3.dp)
                        .background(
                            if (emphasized) {
                                ReferenceBlueprintGold
                            } else {
                                ReferenceBlueprintNavyBright.copy(alpha = 0.82f)
                            }
                        )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = if (emphasized) ProjectTypeCardTitleShadow else null
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (emphasized) Color.White.copy(alpha = 0.95f) else ReferenceBlueprintInk.copy(alpha = 0.94f)
                )
                Text(
                    text = accentLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (emphasized) {
                        ReferenceBlueprintGold
                    } else {
                        ReferenceBlueprintMuted
                    }
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = title,
                tint = if (emphasized) Color.White else ReferenceBlueprintNavyDeep,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ProjectTypeActions(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        ProjectTypeActionButton(
            text = "Back",
            primary = false,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.34f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ReferenceBlueprintInk,
                    modifier = Modifier.size(17.dp)
                )
            }
        )
    }
}

@Composable
private fun ProjectTypeActionButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val brush = if (primary) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFD65C),
                ReferenceBlueprintGold,
                Color(0xFFE0AA21)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                ReferenceBlueprintPaperAlt
            )
        )
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (primary) 1.35.dp else 1.1.dp,
            color = if (primary) ReferenceBlueprintGoldBorder else ReferenceBlueprintBorder.copy(alpha = 0.72f)
        ),
        shadowElevation = if (primary) 5.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(horizontal = 12.dp, vertical = if (primary) 11.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    style = if (primary) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ReferenceBlueprintInk
                )
                trailingIcon?.invoke()
            }
        }
    }
}
