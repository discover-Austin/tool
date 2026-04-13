package com.tradesketch.estimator.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ReferencePlateMetrics(
    val width: Dp,
    val height: Dp
)

@Composable
fun ReferencePlateLayout(
    @DrawableRes plateRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable BoxScope.(ReferencePlateMetrics) -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = plateRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        content(ReferencePlateMetrics(width = maxWidth, height = maxHeight))
    }
}

fun Modifier.plateRect(
    metrics: ReferencePlateMetrics,
    leftFraction: Float,
    topFraction: Float,
    widthFraction: Float,
    heightFraction: Float
): Modifier {
    return this
        .offset(
            x = metrics.width * leftFraction,
            y = metrics.height * topFraction
        )
        .width(metrics.width * widthFraction)
        .height(metrics.height * heightFraction)
}

@Composable
fun BoxScope.ReferencePlateHotspot(
    metrics: ReferencePlateMetrics,
    leftFraction: Float,
    topFraction: Float,
    widthFraction: Float,
    heightFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .plateRect(
                metrics = metrics,
                leftFraction = leftFraction,
                topFraction = topFraction,
                widthFraction = widthFraction,
                heightFraction = heightFraction
            )
            .clip(RoundedCornerShape(cornerRadius))
            .semantics {
                contentDescription = label
            }
            .then(
                if (testTag != null) {
                    Modifier.testTag(testTag)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}
