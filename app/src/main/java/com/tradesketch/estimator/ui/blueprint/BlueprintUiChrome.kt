package com.tradesketch.estimator.ui.blueprint

import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel
import com.tradesketch.estimator.utils.DimensionParser
import java.util.UUID
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
private fun blueprintHudContainerColor(alpha: Float = 0.95f): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
}

@Composable
private fun blueprintHudBorderColor(alpha: Float = 0.96f): Color {
    return MaterialTheme.colorScheme.outline.copy(alpha = alpha)
}

@Composable
internal fun BlueprintBottomBar(
    compact: Boolean,
    canDeleteSelection: Boolean,
    detachedWalls: Boolean,
    boxModeEnabled: Boolean,
    measuredArcModeEnabled: Boolean,
    sketchCurveModeEnabled: Boolean,
    circleModeEnabled: Boolean,
    activePanel: OpeningPanelType?,
    paramsExpanded: Boolean,
    onToggleDetached: () -> Unit,
    onToggleBoxMode: () -> Unit,
    onToggleMeasuredArcMode: () -> Unit,
    onToggleSketchCurveMode: () -> Unit,
    onToggleCircleMode: () -> Unit,
    onDeleteSelection: () -> Unit,
    onToggleDoors: () -> Unit,
    onToggleWindows: () -> Unit,
    onToggleStairUp: () -> Unit,
    onToggleStairDown: () -> Unit,
    onToggleParams: () -> Unit,
    showHelp: Boolean,
    onToggleHelp: () -> Unit,
    detachedButtonModifier: Modifier = Modifier,
    boxButtonModifier: Modifier = Modifier,
    measuredArcButtonModifier: Modifier = Modifier,
    sketchCurveButtonModifier: Modifier = Modifier,
    circleButtonModifier: Modifier = Modifier,
    doorsButtonModifier: Modifier = Modifier,
    windowsButtonModifier: Modifier = Modifier,
    stairUpButtonModifier: Modifier = Modifier,
    stairDownButtonModifier: Modifier = Modifier,
    paramsButtonModifier: Modifier = Modifier,
    helpButtonModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val actionButtonSize = if (compact) 24.dp else 26.dp
    val actionIconSize = if (compact) 11.dp else 12.dp
    val toggleButtonSize = if (compact) 28.dp else 32.dp
    val toggleIconSize = if (compact) 13.dp else 15.dp
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = blueprintHudContainerColor()),
        border = BorderStroke(1.4.dp, blueprintHudBorderColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 50.dp else 60.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 5.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlimIconAction(
                icon = Icons.Filled.Delete,
                contentDescription = "Delete selected",
                enabled = canDeleteSelection,
                onClick = onDeleteSelection,
                buttonSize = actionButtonSize,
                iconSize = actionIconSize,
                minimumTouchTarget = false
            )
            BarDivider(height = if (compact) 18.dp else 22.dp)
            SlimIconToggle(
                icon = Icons.Filled.Workspaces,
                contentDescription = "Detached walls",
                selected = detachedWalls,
                onClick = onToggleDetached,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = detachedButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.CropSquare,
                contentDescription = "Box mode",
                selected = boxModeEnabled,
                onClick = onToggleBoxMode,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = boxButtonModifier
            )
            SlimIconToggle(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = "Measured arc mode",
                selected = measuredArcModeEnabled,
                onClick = onToggleMeasuredArcMode,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = measuredArcButtonModifier
            )
            SlimIconToggle(
                icon = Icons.AutoMirrored.Filled.CallSplit,
                contentDescription = "Sketch curve mode",
                selected = sketchCurveModeEnabled,
                onClick = onToggleSketchCurveMode,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = sketchCurveButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.PanoramaFishEye,
                contentDescription = "Circle mode",
                selected = circleModeEnabled,
                onClick = onToggleCircleMode,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = circleButtonModifier
            )
            BarDivider(height = if (compact) 18.dp else 22.dp)
            SlimIconToggle(
                icon = Icons.Filled.DoorFront,
                contentDescription = "Doors panel",
                selected = activePanel == OpeningPanelType.DOORS,
                onClick = onToggleDoors,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = doorsButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.Window,
                contentDescription = "Windows panel",
                selected = activePanel == OpeningPanelType.WINDOWS,
                onClick = onToggleWindows,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = windowsButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Stair up panel",
                selected = activePanel == OpeningPanelType.STAIR_UP,
                onClick = onToggleStairUp,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = stairUpButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Stair down panel",
                selected = activePanel == OpeningPanelType.STAIR_DOWN,
                onClick = onToggleStairDown,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = stairDownButtonModifier
            )
            SlimIconToggle(
                icon = Icons.Filled.Tune,
                contentDescription = "Params panel",
                selected = paramsExpanded,
                onClick = onToggleParams,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = paramsButtonModifier
            )
            BarDivider(height = if (compact) 18.dp else 22.dp)
            SlimIconToggle(
                icon = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Rail help",
                selected = showHelp,
                onClick = onToggleHelp,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false,
                modifier = helpButtonModifier
            )
        }
    }
}

@Composable
internal fun SlimIconToggle(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 32.dp,
    iconSize: Dp = 15.dp,
    minimumTouchTarget: Boolean = true,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val tint = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = container,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.2.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
            } else {
                blueprintHudBorderColor()
            }
        ),
        shadowElevation = if (selected) 8.dp else 4.dp,
        modifier = modifier
            .size(buttonSize)
            .then(
                if (minimumTouchTarget) {
                    Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
internal fun SlimIconAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 26.dp,
    iconSize: Dp = 12.dp,
    minimumTouchTarget: Boolean = true,
    modifier: Modifier = Modifier
) {
    val container = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    }
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        color = container,
        border = BorderStroke(1.3.dp, blueprintHudBorderColor(alpha = if (enabled) 0.96f else 0.5f)),
        shadowElevation = if (enabled) 6.dp else 1.dp,
        modifier = modifier
            .size(buttonSize)
            .then(
                if (minimumTouchTarget) {
                    Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
internal fun ScopeSelector(
    scope: TakeoffScope,
    onChangeScope: (TakeoffScope) -> Unit,
    minWidth: Dp = 62.dp,
    minHeight: Dp = 34.dp,
    horizontalPadding: Dp = 8.dp,
    iconSize: Dp = 11.dp,
    labelFontSize: TextUnit = 10.sp,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onChangeScope(scope.next()) },
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)),
        shadowElevation = 8.dp,
        modifier = modifier.widthIn(min = minWidth).sizeIn(minHeight = minHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = scope.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = scope.railLabel(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun TradeLayerLegend(
    activeScope: TakeoffScope,
    visibleScopes: List<TakeoffScope>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (visibleScopes.size <= 1) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(),
        border = BorderStroke(1.2.dp, blueprintHudBorderColor(alpha = 0.84f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 6.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Text(
                text = "Canvas Layers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            visibleScopes.forEach { scope ->
                val isActive = scope == activeScope
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = scope.wallColor().copy(alpha = if (isActive) 1f else 0.84f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f)
                            }
                        ),
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                    ) {}
                    Text(
                        text = scope.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (isActive) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BarDivider(
    height: Dp = 22.dp
) {
    Spacer(
        modifier = Modifier
            .height(height)
            .width(1.2.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.92f))
    )
}

@Composable
internal fun ClearAllButton(
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonSize = 34.dp
    val iconSize = if (compact) 14.dp else 15.dp
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.82f)),
        shadowElevation = 8.dp,
        modifier = modifier.size(buttonSize)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Clear all blueprint items",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
internal fun FloorArrowButtons(
    level: BlueprintFloorLevel,
    onLowerFloor: () -> Unit,
    onUpperFloor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val floorButtonSize = 22.dp
    val floorIconSize = 14.dp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = blueprintHudContainerColor(),
            border = BorderStroke(1.2.dp, blueprintHudBorderColor(alpha = 0.9f)),
            shadowElevation = 6.dp
        ) {
            Text(
                text = level.inlineFloorLabel(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        SlimIconAction(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Lower floor",
            enabled = true,
            onClick = onLowerFloor,
            buttonSize = floorButtonSize,
            iconSize = floorIconSize
        )
        SlimIconAction(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Upper floor",
            enabled = true,
            onClick = onUpperFloor,
            buttonSize = floorButtonSize,
            iconSize = floorIconSize
        )
    }
}

@Composable
internal fun FloorLevelSwitcher(
    level: BlueprintFloorLevel,
    onSelect: (BlueprintFloorLevel) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = if (compact) 8.dp else 10.dp
    val verticalPadding = if (compact) 6.dp else 8.dp
    val innerSpacing = if (compact) 4.dp else 6.dp
    val floorLabelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val floorButtonSize = if (compact) 24.dp else 30.dp
    val floorButtonIconSize = if (compact) 13.dp else 15.dp
    val groundButtonLabel = if (compact) "Ground" else "Go Ground"
    val groundPaddingHorizontal = if (compact) 6.dp else 8.dp
    val groundPaddingVertical = if (compact) 3.dp else 4.dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(),
        border = BorderStroke(1.4.dp, blueprintHudBorderColor()),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(innerSpacing)
        ) {
            Text(
                text = "Floor",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = level.floorDisplayLabel(),
                style = floorLabelStyle,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(innerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlimIconAction(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Lower floor",
                    enabled = true,
                    onClick = { onSelect(level - 1) },
                    buttonSize = floorButtonSize,
                    iconSize = floorButtonIconSize
                )
                SlimIconAction(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Upper floor",
                    enabled = true,
                    onClick = { onSelect(level + 1) },
                    buttonSize = floorButtonSize,
                    iconSize = floorButtonIconSize
                )
            }
            Surface(
                onClick = { onSelect(FLOOR_GROUND_LEVEL) },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = groundButtonLabel,
                    modifier = Modifier.padding(horizontal = groundPaddingHorizontal, vertical = groundPaddingVertical),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
internal fun GridScaleBadge(
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val badgeHeight = if (compact) 20.dp else 30.dp
    val chipSize = if (compact) 8.dp else 12.dp
    val horizontalPadding = if (compact) 5.dp else 8.dp
    val verticalPadding = if (compact) 2.dp else 4.dp
    val itemSpacing = if (compact) 3.dp else 6.dp
    val badgeLabel = if (compact) "Scale $label" else "1 block = $label"
    val textStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
    } else {
        MaterialTheme.typography.labelSmall
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(),
        border = BorderStroke(1.3.dp, blueprintHudBorderColor()),
        shadowElevation = 10.dp,
        modifier = modifier.height(badgeHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(chipSize)
                    .height(chipSize)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Text(
                text = badgeLabel,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun FloorCompactBadge(
    level: BlueprintFloorLevel,
    onLowerFloor: () -> Unit,
    onUpperFloor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(),
        border = BorderStroke(1.3.dp, blueprintHudBorderColor()),
        shadowElevation = 10.dp,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Floor: ${level.label()}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            SlimIconAction(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Lower floor",
                enabled = true,
                onClick = onLowerFloor,
                buttonSize = 14.dp,
                iconSize = 9.dp
            )
            SlimIconAction(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Upper floor",
                enabled = true,
                onClick = onUpperFloor,
                buttonSize = 14.dp,
                iconSize = 9.dp
            )
        }
    }
}

@Composable
internal fun GridScaleEditorPanel(
    expanded: Boolean,
    useMetric: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    feetValue: String,
    inchesValue: String,
    centimetersValue: String,
    onFeetValueChange: (String) -> Unit,
    onInchesValueChange: (String) -> Unit,
    onCentimetersValueChange: (String) -> Unit,
    onNudgeInches: (Int) -> Unit,
    onNudgeCentimeters: (Int) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    panelWidth: Dp = 214.dp,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.3.dp, blueprintHudBorderColor())
    ) {
        Column(
            modifier = Modifier.width(panelWidth).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Grid", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Step") },
                singleLine = true,
                placeholder = { Text(if (useMetric) "30cm" else "1'") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Assisted input",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (useMetric) {
                OutlinedTextField(
                    value = centimetersValue,
                    onValueChange = onCentimetersValueChange,
                    label = { Text("cm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onNudgeCentimeters(-1) }) { Text("-1cm") }
                    TextButton(onClick = { onNudgeCentimeters(1) }) { Text("+1cm") }
                    TextButton(onClick = { onNudgeCentimeters(5) }) { Text("+5cm") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = feetValue,
                        onValueChange = onFeetValueChange,
                        label = { Text("ft") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(92.dp)
                    )
                    OutlinedTextField(
                        value = inchesValue,
                        onValueChange = onInchesValueChange,
                        label = { Text("in") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(92.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onNudgeInches(-1) }) { Text("-1\"") }
                    TextButton(onClick = { onNudgeInches(1) }) { Text("+1\"") }
                    TextButton(onClick = { onNudgeInches(6) }) { Text("+6\"") }
                }
            }
            Text(
                text = if (useMetric) {
                    "Examples: 30cm, 300mm, 0.3m"
                } else {
                    "Examples: 1' 3\", 18in, 0.5ft"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Close") }
                TextButton(onClick = onApply) { Text("Set") }
            }
        }
    }
}

@Composable
internal fun ParamsPanel(
    expanded: Boolean,
    activeTool: BlueprintDraftTool,
    params: BlueprintParams,
    snapThresholdFeet: Double,
    useMetric: Boolean,
    onSelectDrawWallTool: () -> Unit,
    onWallHeightChange: (Long) -> Unit,
    onSnapThresholdFeetChange: (Double) -> Unit,
    panelWidth: Dp = 238.dp,
    panelMaxHeight: Dp = 420.dp,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    var heightFt by remember(params.wallHeightMm) {
        mutableStateOf(
            if (useMetric) "%.1f".format(Millimeters(params.wallHeightMm).value / 10.0)
            else "%.2f".format(Millimeters(params.wallHeightMm).toFeet())
        )
    }
    val heightLabel = if (useMetric) "Wall height (cm)" else "Wall height (ft)"
    Surface(modifier = modifier, shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.4.dp, blueprintHudBorderColor())) {
        Card(
            modifier = Modifier
                .width(panelWidth)
                .heightIn(max = panelMaxHeight),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Drawing Settings", style = MaterialTheme.typography.titleSmall)

                val clampedSnapThresholdFeet = snapThresholdFeet.coerceIn(
                    MIN_SNAP_THRESHOLD_FEET,
                    MAX_SNAP_THRESHOLD_FEET
                )
                Text(
                    text = if (useMetric) {
                        "Snap: ${"%.1f".format(clampedSnapThresholdFeet * 30.48)} cm"
                    } else {
                        "Snap: ${"%.2f".format(clampedSnapThresholdFeet)} ft"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = clampedSnapThresholdFeet.toFloat(),
                    onValueChange = { onSnapThresholdFeetChange(it.toDouble()) },
                    valueRange = MIN_SNAP_THRESHOLD_FEET.toFloat()..MAX_SNAP_THRESHOLD_FEET.toFloat()
                )
                Text(
                    text = "Lower snaps less. Higher snaps from farther away.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Draw mode", style = MaterialTheme.typography.labelLarge)
                FilterChip(
                    selected = activeTool == BlueprintDraftTool.DRAW_WALL,
                    onClick = onSelectDrawWallTool,
                    label = { Text("Straight walls") }
                )

                OutlinedTextField(
                    value = heightFt,
                    onValueChange = {
                        heightFt = it
                        it.toDoubleOrNull()?.let { value ->
                            val mm = if (useMetric) (value * 10.0).toLong() else Millimeters.fromFeet(value).value
                            onWallHeightChange(mm)
                        }
                    },
                    label = { Text(heightLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "New walls use this height. Existing walls keep their height.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun OpeningAddonsPanel(
    panelType: OpeningPanelType,
    selectedPreset: OpeningPreset,
    presets: List<OpeningPreset>,
    customWidthFeet: String,
    customHeightFeet: String,
    customSillFeet: String,
    showPresets: Boolean,
    onTogglePresets: () -> Unit,
    onSelectPreset: (OpeningPreset) -> Unit,
    onCustomWidthChange: (String) -> Unit,
    onCustomHeightChange: (String) -> Unit,
    onCustomSillChange: (String) -> Unit,
    panelWidth: Dp = 150.dp,
    panelMaxHeight: Dp = 318.dp,
    modifier: Modifier = Modifier
) {
    val type = when (panelType) {
        OpeningPanelType.DOORS -> OpeningType.DOOR
        OpeningPanelType.WINDOWS -> OpeningType.WINDOW
        OpeningPanelType.STAIR_UP -> OpeningType.STAIR_UP
        OpeningPanelType.STAIR_DOWN -> OpeningType.STAIR_DOWN
    }
    val panelTitle = when (panelType) {
        OpeningPanelType.DOORS -> "Doors"
        OpeningPanelType.WINDOWS -> "Windows"
        OpeningPanelType.STAIR_UP -> "Stairs Up"
        OpeningPanelType.STAIR_DOWN -> "Stairs Down"
    }
    val heightLabel = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "Run"
        else -> "Height"
    }
    val sillLabel = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "Rise"
        else -> "Sill"
    }
    val widthHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "3', 3.5ft, 1000mm"
        else -> "3', 3.5ft, 900mm"
    }
    val heightHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "10', 12ft, 3000mm"
        else -> "7', 7ft, 2100mm"
    }
    val sillHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "8.5', 9ft, 2600mm"
        else -> "0', 3ft, 900mm"
    }
    val customWidthMm = DimensionParser.parseLengthToMillimeters(customWidthFeet)?.coerceAtLeast(1L) ?: selectedPreset.widthMm
    val customHeightMm = DimensionParser.parseLengthToMillimeters(customHeightFeet)?.coerceAtLeast(1L) ?: selectedPreset.heightMm
    val customSillMm = DimensionParser.parseLengthToMillimeters(customSillFeet)?.coerceAtLeast(0L) ?: selectedPreset.sillMm
    val customPreset = OpeningPreset(
        name = when (type) {
            OpeningType.DOOR -> "Custom Door"
            OpeningType.WINDOW -> "Custom Window"
            OpeningType.STAIR_UP -> "Custom Stair Up"
            OpeningType.STAIR_DOWN -> "Custom Stair Down"
        },
        type = type,
        widthMm = customWidthMm,
        heightMm = customHeightMm,
        sillMm = customSillMm
    )
    val customSelected = selectedPreset == customPreset
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(alpha = 0.92f),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .width(panelWidth)
                .heightIn(max = panelMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(panelTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FilterChip(
                    selected = showPresets,
                    onClick = onTogglePresets,
                    label = {
                        Text(
                            if (showPresets) "Standard On" else "Standard Off",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
            ) {
                Text(
                    "Preset armed: ${selectedPreset.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text("How to place", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Text("1. Pick Standard or Custom.", style = MaterialTheme.typography.labelSmall)
                    Text("2. Move cursor to a wall.", style = MaterialTheme.typography.labelSmall)
                    Text("3. Tap when preview is not red.", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onSelectPreset(customPreset) }) {
                            Text("Use Custom", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedTextField(
                        value = customWidthFeet,
                        onValueChange = onCustomWidthChange,
                        label = { Text("W", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        placeholder = { Text(widthHint, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customHeightFeet,
                        onValueChange = onCustomHeightChange,
                        label = { Text(heightLabel.take(1), style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        placeholder = { Text(heightHint, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customSillFeet,
                        onValueChange = onCustomSillChange,
                        label = { Text(sillLabel.take(1), style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        placeholder = { Text(sillHint, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            AddonPresetCard(
                preset = customPreset,
                selected = customSelected,
                icon = {
                    Icon(
                        imageVector = when (type) {
                            OpeningType.DOOR -> Icons.Filled.DoorFront
                            OpeningType.WINDOW -> Icons.Filled.Window
                            OpeningType.STAIR_UP -> Icons.Filled.KeyboardArrowUp
                            OpeningType.STAIR_DOWN -> Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                },
                subtitle = "Custom dimensions",
                onClick = { onSelectPreset(customPreset) }
            )
            if (showPresets) {
                Text(
                    "Standard presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                presets.forEach { preset ->
                    AddonPresetCard(
                        preset = preset,
                        selected = selectedPreset == preset,
                        icon = {
                            Icon(
                                imageVector = when (preset.type) {
                                    OpeningType.DOOR -> Icons.Filled.DoorFront
                                    OpeningType.WINDOW -> Icons.Filled.Window
                                    OpeningType.STAIR_UP -> Icons.Filled.KeyboardArrowUp
                                    OpeningType.STAIR_DOWN -> Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null
                            )
                        },
                        subtitle = when (preset.type) {
                            OpeningType.DOOR -> "Door opening"
                            OpeningType.WINDOW -> "Window opening"
                            OpeningType.STAIR_UP -> "Stair opening up"
                            OpeningType.STAIR_DOWN -> "Stair opening down"
                        },
                        onClick = { onSelectPreset(preset) }
                    )
                }
            } else {
                Text(
                    "Enable Standard to view common presets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun RailHelpPanel(
    expanded: Boolean,
    panelWidth: Dp = 314.dp,
    panelMaxHeight: Dp = 360.dp,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.3.dp, blueprintHudBorderColor())
    ) {
        Column(
            modifier = Modifier
                .width(panelWidth)
                .heightIn(max = panelMaxHeight)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text("Rail Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Fast controls for drawing, snapping, and takeoff flow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RailHelpLine(title = "Delete", detail = "Removes selected wall/opening.")
            RailHelpLine(title = "Select", detail = "Tap walls/openings to edit or remove.")
            RailHelpLine(title = "Draw", detail = "Tap start and end points to create walls.")
            RailHelpLine(
                title = "Box",
                detail = "Tap once to start a box, move to size, use side dials to rotate/expand, then tap again to finish."
            )
            RailHelpLine(
                title = "Measured Arc",
                detail = "Use the curved-arrow rail icon, tap start and end, then pull the rise. The guide shows chord, rise, radius, sweep, and arc length."
            )
            RailHelpLine(
                title = "Sketch Curve",
                detail = "Use the split-curve rail icon when you want a freeform bend. The side dials fine-tune shift and bend after the end point is set."
            )
            RailHelpLine(
                title = "Control layout",
                detail = "Dual joysticks uses the left pad for pan and the right pad for cursor placement."
            )
            RailHelpLine(title = "Trade", detail = "Top-right trade chip cycles drywall, concrete, gravel, and paint.")
            RailHelpLine(title = "Chain", detail = "Continue from last clicked corner on each wall.")
            RailHelpLine(title = "Split", detail = "Detach next wall from the current chain.")
            RailHelpLine(title = "Circle", detail = "Use the rail circle icon for center-and-radius circles.")
            RailHelpLine(title = "Doors/Windows/Stairs", detail = "Open panel, size it, and drag onto walls.")
            RailHelpLine(title = "Floor", detail = "Step floors up/down: Ground, 2, 3... and Basement levels.")
            RailHelpLine(title = "Params", detail = "Opens takeoff and snap settings for the current trade.")
            RailHelpLine(title = "Undo/Redo + Zoom", detail = "Bottom-center cluster above the rail.")
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            ) {
                Text(
                    text = "Tip: Right stick moves the cursor and confirms at the active point. Left stick pans, and left-pad tap/press handles cancel, alt-select, and quick wall clear.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun RailHelpLine(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$title:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun AddonPresetCard(
    preset: OpeningPreset,
    selected: Boolean,
    icon: @Composable () -> Unit,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
            }
        ),
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.9.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.64f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
            }
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SelectionPanel(
    selectedWall: WallSegment?,
    selectedOpening: BlueprintOpening?,
    circleSelection: CircleSelectionInfo?,
    arcSelection: ArcSelectionInfo?,
    useMetric: Boolean,
    onCircleRadiusStep: (Int) -> Unit,
    onCircleDiameterStep: (Int) -> Unit,
    onDeselect: () -> Unit,
    maxWidth: Dp = 190.dp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = maxWidth),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.97f)
        ),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onDeselect,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }

            when {
                selectedWall != null -> {
                    val selectionTitle = when {
                        circleSelection != null -> "Circle"
                        arcSelection?.kind == CurveSelectionKind.MEASURED_ARC -> "Measured Arc"
                        arcSelection?.kind == CurveSelectionKind.SKETCH_CURVE -> "Sketch Curve"
                        else -> "Wall"
                    }
                    Text(
                        selectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (arcSelection != null) {
                        Text(
                            "${if (arcSelection.kind == CurveSelectionKind.MEASURED_ARC) "Arc" else "Curve"}: ${formatLengthDisplay(mm = arcSelection.arcLengthMm, useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${if (arcSelection.kind == CurveSelectionKind.MEASURED_ARC) "Chord" else "Span"}: ${formatLengthDisplay(mm = arcSelection.spanMm, useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Text(
                            "L: ${formatLengthDisplay(mm = selectedWall.lengthMillimeters(), useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        "H: ${formatLengthDisplay(mm = selectedWall.height.value, useMetric = useMetric)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (circleSelection != null) {
                        Text(
                            "R: ${formatLengthDisplay(mm = circleSelection.radiusMm, useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "D: ${formatLengthDisplay(mm = circleSelection.diameterMm, useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Segments: ${circleSelection.segmentCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = { onCircleRadiusStep(-1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("R-", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = { onCircleRadiusStep(1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("R+", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = { onCircleDiameterStep(-1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("D-", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = { onCircleDiameterStep(1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("D+", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (arcSelection != null) {
                        if (arcSelection.kind == CurveSelectionKind.MEASURED_ARC) {
                            Text(
                                "Rise: ${formatSignedLengthDisplay(mm = arcSelection.riseMm ?: 0L, useMetric = useMetric)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "Radius: ${formatLengthDisplay(mm = arcSelection.radiusMm ?: 0L, useMetric = useMetric)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Text(
                                "Shift: ${formatSignedLengthDisplay(mm = arcSelection.shiftMm ?: 0L, useMetric = useMetric)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "Bend: ${formatSignedLengthDisplay(mm = arcSelection.bendMm ?: 0L, useMetric = useMetric)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            "${if (arcSelection.kind == CurveSelectionKind.MEASURED_ARC) "Sweep" else "Turn"}: ${formatAngleLabel(arcSelection.turnDegrees)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Segments: ${arcSelection.segmentCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (circleSelection != null) {
                            "Resize with R/D controls. Delete from rail trash."
                        } else if (arcSelection != null) {
                            if (arcSelection.kind == CurveSelectionKind.MEASURED_ARC) {
                                "Measured arc values stay recoverable here. Delete from rail trash."
                            } else {
                                "Sketch-curve values stay recoverable here. Delete from rail trash."
                            }
                        } else {
                            "Delete from rail trash."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                selectedOpening != null -> {
                    Text(
                        selectedOpening.type.displayLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "W: ${formatLengthDisplay(mm = selectedOpening.widthMm, useMetric = useMetric)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "${if (selectedOpening.type.isStair()) "Run" else "H"}: ${formatLengthDisplay(mm = selectedOpening.heightMm, useMetric = useMetric)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (selectedOpening.type.isStair()) {
                        Text(
                            "Rise: ${formatLengthDisplay(mm = selectedOpening.sillMm, useMetric = useMetric)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        "Delete from rail trash.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun LiveOverlay(
    liveScopeQuantity: LiveScopeQuantity,
    squareFeet: Double,
    linearFeet: Double,
    selectedFloor: BlueprintFloorLevel,
    useMetric: Boolean,
    compact: Boolean,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val floorNumberLabel = selectedFloor.floorDisplayLabel()
    val compactFloorLabel = floorNumberLabel
        .removePrefix("Floor: ")
        .removePrefix("Floor ")
        .replace("Ground", "Gnd")
    Card(
        modifier = modifier.widthIn(max = maxWidth),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = blueprintHudContainerColor()),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 5.dp else 10.dp,
                vertical = if (compact) 5.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (compact) "TOTAL" else "PROJECT TOTAL",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.5.sp else 9.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = if (compact) compactFloorLabel else floorNumberLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.5.sp else 9.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = liveScopeQuantity.value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 8.sp else 10.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Text(
                text = if (useMetric) {
                    if (compact) {
                        "Lin ${formatLiveValue(linearFeet * 0.3048, 2)} m"
                    } else {
                        "Linear: ${formatLiveValue(linearFeet * 0.3048, 2)} m"
                    }
                } else {
                    if (compact) {
                        "Lin ${formatLiveValue(linearFeet, 1)} ft"
                    } else {
                        "Linear Feet: ${formatLiveValue(linearFeet, 1)} ft"
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.5.sp else 9.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = if (useMetric) {
                    if (compact) {
                        "Sq ${formatLiveValue(squareFeet * 0.09290304, 2)} m"
                    } else {
                        "Area: ${formatLiveValue(squareFeet * 0.09290304, 2)} sq m"
                    }
                } else {
                    if (compact) {
                        "Sq ${formatLiveValue(squareFeet, 1)} sf"
                    } else {
                        "Square Feet: ${formatLiveValue(squareFeet, 1)} sq ft"
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.5.sp else 9.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
internal fun CursorCoordinateOverlay(
    worldPoint: PointMm?,
    showRotate: Boolean,
    onRotate: () -> Unit,
    useMetric: Boolean,
    compact: Boolean = false,
    rotateButtonModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    if (worldPoint == null) return
    val xValue = formatSignedLengthDisplay(mm = worldPoint.x, useMetric = useMetric)
    val yValue = formatSignedLengthDisplay(mm = worldPoint.y, useMetric = useMetric)
    val cornerRadius = 4.dp
    val horizontalPadding = if (compact) 5.dp else 8.dp
    val verticalPadding = if (compact) 3.dp else 5.dp
    val rowSpacing = if (compact) 4.dp else 7.dp
    val textSize = if (compact) 9.sp else 10.sp
    val labelSize = if (compact) 8.5.sp else 10.sp
    val valueSlotWidth = when {
        compact && useMetric -> 42.dp
        compact -> 54.dp
        useMetric -> 56.dp
        else -> 72.dp
    }
    val rotateButtonSize = if (compact) 18.dp else 22.dp
    val rotateIconSize = if (compact) 8.dp else 10.dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = blueprintHudContainerColor(),
        border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.68f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRotate) {
                SlimIconAction(
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = "Rotate picked wall +5 degrees",
                    enabled = true,
                    onClick = onRotate,
                    buttonSize = rotateButtonSize,
                    iconSize = rotateIconSize,
                    modifier = rotateButtonModifier
                )
            }
            Text(
                text = "X",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Box(
                modifier = Modifier.width(valueSlotWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = xValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = textSize,
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = "|",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize),
                maxLines = 1
            )
            Text(
                text = "Y",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Box(
                modifier = Modifier.width(valueSlotWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = yValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = textSize,
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
internal fun ControlStateHud(
    stateLabel: String,
    modifier: Modifier = Modifier
) {
    val emphasis by animateFloatAsState(
        targetValue = when (stateLabel) {
            "Draw", "Box" -> 1f
            "Selected", "Picked Up" -> 0.92f
            else -> 0.72f
        },
        animationSpec = tween(durationMillis = 180),
        label = "control-state-hud-emphasis"
    )
    val (accentColor, textColor) = when (stateLabel) {
        "Draw" -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimaryContainer)
        "Box" -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimaryContainer)
        "Selected" -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiaryContainer)
        "Picked Up" -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Pair(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.onSurface)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = blueprintHudContainerColor(alpha = 0.95f + (emphasis * 0.05f)),
        border = BorderStroke(
            (1.2f + (0.4f * emphasis)).dp,
            accentColor.copy(alpha = 0.72f + (emphasis * 0.24f))
        ),
        shadowElevation = 9.dp
    ) {
        Text(
            text = stateLabel,
            color = textColor.copy(alpha = 0.94f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun ControlStateChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val container = if (active) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = container,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            width = if (active) 1.4.dp else 1.2.dp,
            color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.82f)
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

