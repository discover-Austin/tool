package com.tradesketch.estimator.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R

val ReferenceBlueprintPaper = Color(0xFFF2F0E8)
val ReferenceBlueprintPaperAlt = Color(0xFFE7E4DA)
val ReferenceBlueprintBorder = Color(0xFF46596A)
val ReferenceBlueprintInk = Color(0xFF223544)
val ReferenceBlueprintMuted = Color(0xFF556675)
val ReferenceBlueprintNavy = Color(0xFF233A54)
val ReferenceBlueprintNavyDeep = Color(0xFF10283D)
val ReferenceBlueprintNavyBright = Color(0xFF2B5074)
val ReferenceBlueprintGold = Color(0xFFF2C43B)
val ReferenceBlueprintGoldBorder = Color(0xFF9A6F12)
val ReferenceBlueprintSteel = Color(0xFF1F4464)
val ReferenceBlueprintSteelBorder = Color(0xFF7C93A6)

@Composable
fun ReferenceWorkspaceBackdrop(
    modifier: Modifier = Modifier,
    showBrand: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF123452),
                        Color(0xFF0C253B),
                        Color(0xFF061826)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minorStep = 28.dp.toPx()
            val majorStep = minorStep * 4f
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = Color.White.copy(alpha = if (x % majorStep == 0f) 0.14f else 0.055f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = if (x % majorStep == 0f) 1.4f else 1f
                )
                x += minorStep
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = Color.White.copy(alpha = if (y % majorStep == 0f) 0.14f else 0.055f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = if (y % majorStep == 0f) 1.4f else 1f
                )
                y += minorStep
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.22f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF235D86).copy(alpha = 0.44f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (showBrand) {
            ReferenceIntroBrand(
                compact = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 18.dp)
            )
        }

        content()
    }
}

@Composable
fun ReferenceIntroBackdrop(
    modifier: Modifier = Modifier,
    bandHeight: androidx.compose.ui.unit.Dp = 178.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F1EA))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 34.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = Color(0xFF9FB1C0).copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = Color(0xFF9FB1C0).copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bandHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF21496A),
                            ReferenceBlueprintNavyBright,
                            ReferenceBlueprintNavy
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = bandHeight - 18.dp)
        ) {
            val dividerPath = Path().apply {
                moveTo(0f, size.height * 0.74f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(dividerPath, Color.White)
            drawLine(
                color = Color.Black.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.74f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 2.4f
            )
        }

        content()
    }
}

@Composable
fun ReferenceIntroBrand(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Trade")
                    withStyle(SpanStyle(color = Color(0xFF5EA1E8))) {
                        append("Sketch")
                    }
                },
                style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = "Plan. Estimate. Build.",
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f)
        )
    }
}

@Composable
fun ReferenceIntroFooterBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeDialog by rememberSaveable { mutableStateOf<IntroFooterDialog?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ReferenceBlueprintNavy,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReferenceIntroFooterItem(
                icon = Icons.Filled.Info,
                label = "About Us",
                onClick = { activeDialog = IntroFooterDialog.ABOUT }
            )
            ReferenceIntroFooterDivider()
            ReferenceIntroFooterItem(
                icon = Icons.Filled.VerifiedUser,
                label = "Privacy Policy",
                onClick = { activeDialog = IntroFooterDialog.PRIVACY }
            )
            ReferenceIntroFooterDivider()
            ReferenceIntroFooterItem(
                icon = Icons.Filled.Description,
                label = "Terms of Service",
                onClick = { activeDialog = IntroFooterDialog.TERMS }
            )
        }
    }

    when (activeDialog) {
        IntroFooterDialog.ABOUT -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text(stringResource(R.string.about_us_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.about_us_message,
                            stringResource(R.string.support_email)
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activeDialog = null
                            launchIntroSupportEmail(context)
                        }
                    ) {
                        Text(stringResource(R.string.send_feedback))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        IntroFooterDialog.PRIVACY -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text(stringResource(R.string.privacy_policy_title)) },
                text = { Text(stringResource(R.string.privacy_policy_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activeDialog = null
                            openIntroExternalUrl(
                                context = context,
                                url = context.getString(R.string.privacy_policy_url)
                            )
                        }
                    ) {
                        Text(stringResource(R.string.open_policy))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        IntroFooterDialog.TERMS -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text(stringResource(R.string.terms_of_service_title)) },
                text = { Text(stringResource(R.string.terms_of_service_message)) },
                confirmButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        null -> Unit
    }
}

@Composable
fun ReferenceWorksheetPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = appCardColors(containerColor = Color.Transparent),
        border = BorderStroke(width = 2.dp, color = ReferenceBlueprintSteelBorder.copy(alpha = 0.9f)),
        elevation = appCardElevation(raised = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D273D),
                            ReferenceBlueprintNavyDeep,
                            Color(0xFF071A2A)
                        )
                    )
                )
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8F6EE),
                                ReferenceBlueprintPaper,
                                Color(0xFFE8E3D5)
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 18.dp.toPx()
                    var y = 0f
                    while (y <= size.height) {
                        drawLine(
                            color = ReferenceBlueprintBorder.copy(alpha = 0.055f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                    drawRect(
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 1.4f)
                    )
                    drawRect(
                        color = ReferenceBlueprintBorder.copy(alpha = 0.32f),
                        style = Stroke(width = 1.1f)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ReferenceWorksheetTitleBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        border = BorderStroke(width = 1.2.dp, color = ReferenceBlueprintBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ReferenceBlueprintNavyBright,
                                ReferenceBlueprintNavy
                            )
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBack != null) {
                        ReferenceMiniBackButton(onClick = onBack)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                }
            }
            Text(
                text = subtitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EFE3).copy(alpha = 0.96f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = ReferenceBlueprintMuted
            )
        }
    }
}

@Composable
fun ReferenceSectionFrame(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFEFEBDD).copy(alpha = 0.98f),
        border = BorderStroke(width = 1.15.dp, color = ReferenceBlueprintBorder.copy(alpha = 0.9f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ReferenceBlueprintNavy,
                                ReferenceBlueprintSteel,
                                Color(0xFFEFEBDD)
                            )
                        )
                    )
                    .padding(start = 9.dp, end = 9.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 14.dp)
                            .background(ReferenceBlueprintGold)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                content = content
            )
        }
    }
}

@Composable
fun ReferenceMiniBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Back"
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(width = 1.1.dp, color = ReferenceBlueprintSteelBorder.copy(alpha = 0.92f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = label,
                tint = ReferenceBlueprintInk,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ReferenceBlueprintInk
            )
        }
    }
}

@Composable
fun ReferenceActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasize: Boolean = false
) {
    val containerBrush = if (emphasize) {
        Brush.verticalGradient(listOf(Color(0xFFFFD65C), ReferenceBlueprintGold, Color(0xFFD39A16)))
    } else {
        Brush.verticalGradient(listOf(ReferenceBlueprintNavyBright, ReferenceBlueprintSteel, ReferenceBlueprintNavyDeep))
    }
    val borderColor = if (emphasize) {
        ReferenceBlueprintGoldBorder
    } else {
        ReferenceBlueprintSteelBorder
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        color = Color.Transparent,
        border = BorderStroke(width = 1.45.dp, color = borderColor),
        shadowElevation = if (emphasize) 5.dp else 3.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(containerBrush)
                    .padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (emphasize) ReferenceBlueprintInk else Color.White,
                        maxLines = 2
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = text,
                        tint = if (emphasize) ReferenceBlueprintInk else Color.White,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = if (emphasize) 0.46f else 0.24f))
                )
            }
        }
    }
}

@Composable
fun ReferenceFooterNote(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFDFDDD4),
        border = BorderStroke(width = 1.dp, color = ReferenceBlueprintBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun BoxScope.ReferencePanelOverlay(
    metrics: ReferencePlateMetrics,
    leftFraction: Float,
    topFraction: Float,
    widthFraction: Float,
    heightFraction: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    ReferenceWorksheetPanel(
        modifier = Modifier.plateRect(
            metrics = metrics,
            leftFraction = leftFraction,
            topFraction = topFraction,
            widthFraction = widthFraction,
            heightFraction = heightFraction
        )
    ) {
        content()
    }
}

@Composable
private fun ReferenceIntroFooterItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun ReferenceIntroFooterDivider() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.24f))
    )
}

private enum class IntroFooterDialog {
    ABOUT,
    PRIVACY,
    TERMS
}

private fun openIntroExternalUrl(
    context: Context,
    url: String
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        val message = if (error is ActivityNotFoundException) {
            "No browser is available for this link."
        } else {
            "Could not open this link."
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

private fun launchIntroSupportEmail(context: Context) {
    val supportEmail = context.getString(R.string.support_email)
    val intent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:${Uri.encode(supportEmail)}")
    ).apply {
        putExtra(Intent.EXTRA_SUBJECT, "TradeSketch Support")
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        val message = if (error is ActivityNotFoundException) {
            context.getString(R.string.feedback_no_mail_app, supportEmail)
        } else {
            context.getString(R.string.feedback_open_failed, supportEmail)
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
