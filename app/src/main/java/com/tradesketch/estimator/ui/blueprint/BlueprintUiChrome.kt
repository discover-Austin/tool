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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Architecture
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
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
private fun blueprintHudContainerColor(alpha: Float = 0.98f): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
}

@Composable
private fun blueprintHudBorderColor(alpha: Float = 0.82f): Color {
    return MaterialTheme.colorScheme.outline.copy(alpha = alpha)
}

@Composable
internal fun BlueprintBottomBar(
    canDeleteSelection: Boolean,
    detachedWalls: Boolean,
    boxModeEnabled: Boolean,
    circleModeEnabled: Boolean,
    activePanel: OpeningPanelType?,
    paramsExpanded: Boolean,
    onToggleDetached: () -> Unit,
    onToggleBoxMode: () -> Unit,
    onToggleCircleMode: () -> Unit,
    onDeleteSelection: () -> Unit,
    onToggleDoors: () -> Unit,
    onToggleWindows: () -> Unit,
    onToggleStairUp: () -> Unit,
    onToggleStairDown: () -> Unit,
    onToggleParams: () -> Unit,
    showHelp: Boolean,
    onToggleHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compactBottomBar = LocalConfiguration.current.screenWidthDp < 420
    val actionButtonSize = if (compactBottomBar) 24.dp else 26.dp
    val actionIconSize = if (compactBottomBar) 11.dp else 12.dp
    val toggleButtonSize = if (compactBottomBar) 28.dp else 32.dp
    val toggleIconSize = if (compactBottomBar) 13.dp else 15.dp
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = blueprintHudContainerColor()),
        border = BorderStroke(1.15.dp, blueprintHudBorderColor(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactBottomBar) 50.dp else 60.dp)
                .padding(horizontal = if (compactBottomBar) 6.dp else 8.dp, vertical = if (compactBottomBar) 5.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactBottomBar) 2.dp else 5.dp),
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
            BarDivider(height = if (compactBottomBar) 18.dp else 22.dp)
            listOf(
                BlueprintIconToggleSpec(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = "Detached walls",
                    selected = detachedWalls,
                    onClick = onToggleDetached
                ),
                BlueprintIconToggleSpec(
                    icon = Icons.Filled.CropSquare,
                    contentDescription = "Box mode",
                    selected = boxModeEnabled,
                    onClick = onToggleBoxMode
                ),
                BlueprintIconToggleSpec(
                    icon = Icons.Filled.PanoramaFishEye,
                    contentDescription = "Circle mode",
                    selected = circleModeEnabled,
                    onClick = onToggleCircleMode
                )
            ).forEach { toggle ->
                SlimIconToggle(
                    icon = toggle.icon,
                    contentDescription = toggle.contentDescription,
                    selected = toggle.selected,
                    onClick = toggle.onClick,
                    buttonSize = toggleButtonSize,
                    iconSize = toggleIconSize,
                    minimumTouchTarget = false
                )
            }
            BarDivider(height = if (compactBottomBar) 18.dp else 22.dp)
            SlimIconToggle(
                icon = Icons.Filled.DoorFront,
                contentDescription = "Doors panel",
                selected = activePanel == OpeningPanelType.DOORS,
                onClick = onToggleDoors,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
            )
            SlimIconToggle(
                icon = Icons.Filled.Window,
                contentDescription = "Windows panel",
                selected = activePanel == OpeningPanelType.WINDOWS,
                onClick = onToggleWindows,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Stair up panel",
                selected = activePanel == OpeningPanelType.STAIR_UP,
                onClick = onToggleStairUp,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Stair down panel",
                selected = activePanel == OpeningPanelType.STAIR_DOWN,
                onClick = onToggleStairDown,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
            )
            SlimIconToggle(
                icon = Icons.Filled.Tune,
                contentDescription = "Params panel",
                selected = paramsExpanded,
                onClick = onToggleParams,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
            )
            BarDivider(height = if (compactBottomBar) 18.dp else 22.dp)
            SlimIconToggle(
                icon = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Rail help",
                selected = showHelp,
                onClick = onToggleHelp,
                buttonSize = toggleButtonSize,
                iconSize = toggleIconSize,
                minimumTouchTarget = false
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
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.99f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val tint = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.84f)
            } else {
                blueprintHudBorderColor(alpha = 0.92f)
            }
        ),
        shadowElevation = if (selected) 6.dp else 3.dp,
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
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
    }
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.1.dp, blueprintHudBorderColor(alpha = if (enabled) 0.9f else 0.55f)),
        shadowElevation = if (enabled) 5.dp else 1.dp,
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
    minWidth: Dp = 102.dp,
    minHeight: Dp = 48.dp,
    horizontalPadding: Dp = 10.dp,
    iconSize: Dp = 12.dp,
    labelFontSize: TextUnit = 11.sp,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onChangeScope(scope.next()) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.96f),
        border = BorderStroke(1.1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.76f)),
        shadowElevation = 6.dp,
        modifier = modifier.widthIn(min = minWidth).sizeIn(minHeight = minHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
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
                fontWeight = FontWeight.SemiBold
            )
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
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.78f))
    )
}

@Composable
internal fun ClearAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compactButton = LocalConfiguration.current.screenWidthDp < 420
    val buttonSize = if (compactButton) 34.dp else 38.dp
    val iconSize = if (compactButton) 14.dp else 16.dp
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.98f),
        border = BorderStroke(1.1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
        shadowElevation = 5.dp,
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
        shape = RoundedCornerShape(16.dp),
        color = blueprintHudContainerColor(alpha = 0.96f),
        border = BorderStroke(1.dp, blueprintHudBorderColor(alpha = 0.42f)),
        shadowElevation = 10.dp
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
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
                shadowElevation = 2.dp
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
        shape = RoundedCornerShape(14.dp),
        color = blueprintHudContainerColor(alpha = 0.95f),
        border = BorderStroke(1.dp, blueprintHudBorderColor(alpha = 0.4f)),
        shadowElevation = 8.dp,
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
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
        shape = RoundedCornerShape(14.dp),
        color = blueprintHudContainerColor(alpha = 0.95f),
        border = BorderStroke(1.dp, blueprintHudBorderColor(alpha = 0.4f)),
        shadowElevation = 8.dp,
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
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.width(214.dp).padding(8.dp),
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
    scope: TakeoffScope,
    activeTool: BlueprintDraftTool,
    params: BlueprintParams,
    takeoffSession: ProjectTakeoffSession,
    snapThresholdFeet: Double,
    useMetric: Boolean,
    onSelectDrawWallTool: () -> Unit,
    onSelectDrawArcTool: () -> Unit,
    onSelectDrawCircleTool: () -> Unit,
    onParamsChange: (BlueprintParams) -> Unit,
    onWallHeightChange: (Long) -> Unit,
    onDrywallSheetAreaChange: (Double) -> Unit,
    onDrywallWasteChange: (Double) -> Unit,
    onDrywallScrewsChange: (Int) -> Unit,
    onDrywallMudRateChange: (Double) -> Unit,
    onDrywallIncludeCeilingsChange: (Boolean) -> Unit,
    onConcreteDepthFeetChange: (Double) -> Unit,
    onConcreteWasteChange: (Double) -> Unit,
    onGravelDepthFeetChange: (Double) -> Unit,
    onGravelDensityChange: (Double) -> Unit,
    onGravelWasteChange: (Double) -> Unit,
    onPaintCoverageChange: (Double) -> Unit,
    onPaintCoatsChange: (Int) -> Unit,
    onPaintWasteChange: (Double) -> Unit,
    onSnapThresholdFeetChange: (Double) -> Unit,
    onScopeExpand: () -> Unit,
    onDetectRooms: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    var heightFt by remember(params.wallHeightMm) { mutableStateOf("%.2f".format(Millimeters(params.wallHeightMm).toFeet())) }
    var drywallSheetArea by remember(takeoffSession.drywall.sheetAreaSqFt) { mutableStateOf(takeoffSession.drywall.sheetAreaSqFt.toString()) }
    var drywallWaste by remember(takeoffSession.drywall.wastePercent) { mutableStateOf(takeoffSession.drywall.wastePercent.toString()) }
    var drywallScrews by remember(takeoffSession.drywall.screwsPerSheet) { mutableStateOf(takeoffSession.drywall.screwsPerSheet.toString()) }
    var drywallMudRate by remember(takeoffSession.drywall.mudGallonsPer100SqFt) { mutableStateOf(takeoffSession.drywall.mudGallonsPer100SqFt.toString()) }
    var concreteDepthInches by remember(takeoffSession.concrete.thicknessFeet) { mutableStateOf("%.2f".format(takeoffSession.concrete.thicknessFeet * 12.0)) }
    var concreteWaste by remember(takeoffSession.concrete.wastePercent) { mutableStateOf(takeoffSession.concrete.wastePercent.toString()) }
    var gravelDepthInches by remember(takeoffSession.gravel.depthFeet) { mutableStateOf("%.2f".format(takeoffSession.gravel.depthFeet * 12.0)) }
    var gravelDensity by remember(takeoffSession.gravel.densityTonsPerYard) { mutableStateOf("%.2f".format(takeoffSession.gravel.densityTonsPerYard)) }
    var gravelWaste by remember(takeoffSession.gravel.wastePercent) { mutableStateOf(takeoffSession.gravel.wastePercent.toString()) }
    var paintCoverage by remember(takeoffSession.paint.coverageSqFtPerGallon) { mutableStateOf(takeoffSession.paint.coverageSqFtPerGallon.toString()) }
    var paintCoats by remember(takeoffSession.paint.coats) { mutableStateOf(takeoffSession.paint.coats.toString()) }
    var paintWaste by remember(takeoffSession.paint.wastePercent) { mutableStateOf(takeoffSession.paint.wastePercent.toString()) }
    var selectedGravelType by remember(takeoffSession.gravel.densityTonsPerYard) {
        mutableStateOf(closestGravelMaterialPreset(takeoffSession.gravel.densityTonsPerYard).label)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)) {
        Card(
            modifier = Modifier
                .width(278.dp)
                .heightIn(max = 548.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
        ) {
            Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${scope.shortLabel()} parameters", style = MaterialTheme.typography.titleSmall)
                }
                val clampedSnapThresholdFeet = snapThresholdFeet.coerceIn(
                    MIN_SNAP_THRESHOLD_FEET,
                    MAX_SNAP_THRESHOLD_FEET
                )
                Text(
                    text = if (useMetric) {
                        "Snap sensitivity: ${"%.1f".format(clampedSnapThresholdFeet * 30.48)} cm"
                    } else {
                        "Snap sensitivity: ${"%.2f".format(clampedSnapThresholdFeet)} ft"
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
                Text("Shape tools", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = activeTool == BlueprintDraftTool.DRAW_WALL,
                        onClick = onSelectDrawWallTool,
                        label = { Text("Straight walls") }
                    )
                    FilterChip(
                        selected = activeTool == BlueprintDraftTool.DRAW_ARC,
                        onClick = onSelectDrawArcTool,
                        label = { Text("Curved wall") }
                    )
                    Text(
                        text = "Curve: tap start, end, then bend. Circle now lives on the build rail.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (scope) {
                    TakeoffScope.DRYWALL -> {
                        OutlinedTextField(
                            value = heightFt,
                            onValueChange = {
                                heightFt = it
                                it.toDoubleOrNull()?.let { value ->
                                    onWallHeightChange(Millimeters.fromFeet(value).value)
                                }
                            },
                            label = { Text("Wall height (ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallSheetArea,
                            onValueChange = {
                                drywallSheetArea = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallSheetAreaChange(value)
                                }
                            },
                            label = { Text("Sheet size (sq ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallWaste,
                            onValueChange = {
                                drywallWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallWasteChange(value)
                                    onParamsChange(params.copy(wasteFactorPercent = value.coerceAtLeast(0.0)))
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallScrews,
                            onValueChange = {
                                drywallScrews = it
                                it.toIntOrNull()?.let { value ->
                                    onDrywallScrewsChange(value)
                                }
                            },
                            label = { Text("Screws per sheet") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallMudRate,
                            onValueChange = {
                                drywallMudRate = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallMudRateChange(value)
                                }
                            },
                            label = { Text("Mud gallons / 100 sq ft") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = takeoffSession.drywall.includeCeilings,
                            onClick = { onDrywallIncludeCeilingsChange(!takeoffSession.drywall.includeCeilings) },
                            label = { Text(if (takeoffSession.drywall.includeCeilings) "Ceilings included" else "Ceilings excluded") }
                        )
                    }

                    TakeoffScope.CONCRETE -> {
                        OutlinedTextField(
                            value = concreteDepthInches,
                            onValueChange = {
                                concreteDepthInches = it
                                it.toDoubleOrNull()?.let { value ->
                                    val depthFeet = (value / 12.0).coerceAtLeast(0.0)
                                    onConcreteDepthFeetChange(depthFeet)
                                    onParamsChange(params.copy(concreteThicknessMm = Millimeters.fromFeet(depthFeet).value))
                                }
                            },
                            label = { Text("Depth (in)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = concreteWaste,
                            onValueChange = {
                                concreteWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onConcreteWasteChange(value)
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TakeoffScope.GRAVEL_MULCH -> {
                        OutlinedTextField(
                            value = gravelDepthInches,
                            onValueChange = {
                                gravelDepthInches = it
                                it.toDoubleOrNull()?.let { value ->
                                    val depthFeet = (value / 12.0).coerceAtLeast(0.0)
                                    onGravelDepthFeetChange(depthFeet)
                                    onParamsChange(params.copy(bedDepthMm = Millimeters.fromFeet(depthFeet).value))
                                }
                            },
                            label = { Text("Depth (in)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Material type", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            gravelMaterialPresets.forEach { preset ->
                                FilterChip(
                                    selected = selectedGravelType == preset.label,
                                    onClick = {
                                        selectedGravelType = preset.label
                                        gravelDensity = "%.2f".format(preset.densityTonsPerYard)
                                        onGravelDensityChange(preset.densityTonsPerYard)
                                    },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = gravelDensity,
                            onValueChange = {
                                gravelDensity = it
                                it.toDoubleOrNull()?.let { value ->
                                    onGravelDensityChange(value)
                                    selectedGravelType = closestGravelMaterialPreset(value).label
                                }
                            },
                            label = { Text("Tons per yard³") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gravelWaste,
                            onValueChange = {
                                gravelWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onGravelWasteChange(value)
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TakeoffScope.PAINT -> {
                        OutlinedTextField(
                            value = heightFt,
                            onValueChange = {
                                heightFt = it
                                it.toDoubleOrNull()?.let { value ->
                                    onWallHeightChange(Millimeters.fromFeet(value).value)
                                }
                            },
                            label = { Text("Wall height (ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintCoats,
                            onValueChange = {
                                paintCoats = it
                                it.toIntOrNull()?.let { value ->
                                    onPaintCoatsChange(value)
                                    onParamsChange(params.copy(paintCoats = value.coerceAtLeast(1)))
                                }
                            },
                            label = { Text("Coats") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintCoverage,
                            onValueChange = {
                                paintCoverage = it
                                it.toDoubleOrNull()?.let { value ->
                                    onPaintCoverageChange(value)
                                }
                            },
                            label = { Text("Coverage (sq ft / gal)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintWaste,
                            onValueChange = {
                                paintWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onPaintWasteChange(value)
                                    onParamsChange(params.copy(wasteFactorPercent = value.coerceAtLeast(0.0)))
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("Room tools", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = onDetectRooms,
                        label = { Text("Detect Rooms") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onScopeExpand,
                        label = { Text("Tag Rooms") }
                    )
                }
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
        OpeningType.STAIR_DOWN -> "3', 4ft, 1200mm"
        else -> "3', 3.5ft, 900mm"
    }
    val heightHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "9', 10ft, 3000mm"
        else -> "7', 7ft, 2100mm"
    }
    val sillHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "0', 1ft, 300mm"
        else -> "0', 3ft, 900mm"
    }
    val customPreset = OpeningPreset(
        name = when (type) {
            OpeningType.DOOR -> "Custom Door"
            OpeningType.WINDOW -> "Custom Window"
            OpeningType.STAIR_UP -> "Custom Stair Up"
            OpeningType.STAIR_DOWN -> "Custom Stair Down"
        },
        type = type,
        widthMm = DimensionParser.parseLengthToMillimeters(customWidthFeet)?.coerceAtLeast(1L) ?: selectedPreset.widthMm,
        heightMm = DimensionParser.parseLengthToMillimeters(customHeightFeet)?.coerceAtLeast(1L) ?: selectedPreset.heightMm,
        sillMm = DimensionParser.parseLengthToMillimeters(customSillFeet)?.coerceAtLeast(0L) ?: selectedPreset.sillMm
    )
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
        Column(
            modifier = Modifier
                .width(156.dp)
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    label = { Text("List", style = MaterialTheme.typography.labelSmall) }
                )
            }
            Text(
                "Select size. Item stays on pointer until deselected.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            AddonPresetCard(
                preset = customPreset,
                selected = false,
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
                onClick = { onSelectPreset(customPreset) }
            )
            if (showPresets) {
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
                        onClick = { onSelectPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun RailHelpPanel(
    expanded: Boolean,
    dualJoysticksEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .width(314.dp)
                .heightIn(max = 360.dp)
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
                title = "Control mode",
                detail = if (dualJoysticksEnabled) {
                    "Dual joysticks uses the left pad for pan and the right pad for cursor placement."
                } else {
                    "Touch mode uses the bottom quick tools for Select, Draw, Grab, and Cancel."
                }
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
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            ) {
                Text(
                    text = if (dualJoysticksEnabled) {
                        "Tip: Right stick moves the cursor and confirms at the active point. Left stick pans, and left-pad tap/press handles cancel, alt-select, and quick wall clear."
                    } else {
                        "Tip: Use the quick tools to switch Select, Draw, and Grab. Two-finger gestures still pan and zoom the canvas."
                    },
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
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
                    when (preset.type) {
                        OpeningType.DOOR -> "Door swing arc"
                        OpeningType.WINDOW -> "Window break"
                        OpeningType.STAIR_UP -> "Stair opening up"
                        OpeningType.STAIR_DOWN -> "Stair opening down"
                    },
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
    useMetric: Boolean,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 190.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
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
                    Text(
                        "Wall",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "L: ${formatLengthDisplay(mm = selectedWall.lengthMillimeters(), useMetric = useMetric)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "H: ${formatLengthDisplay(mm = selectedWall.height.value, useMetric = useMetric)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Delete from rail trash.",
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
    modifier: Modifier = Modifier
) {
    val compactHud = LocalConfiguration.current.screenWidthDp < 420
    val floorNumberLabel = selectedFloor.floorDisplayLabel()
    val compactFloorLabel = floorNumberLabel
        .removePrefix("Floor: ")
        .removePrefix("Floor ")
        .replace("Ground", "Gnd")
    Card(
        modifier = modifier.widthIn(max = if (compactHud) 104.dp else 164.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = blueprintHudContainerColor(alpha = 0.97f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compactHud) 5.dp else 10.dp,
                vertical = if (compactHud) 5.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compactHud) 2.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (compactHud) "TOTAL" else "PROJECT TOTAL",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactHud) 7.5.sp else 9.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = if (compactHud) compactFloorLabel else floorNumberLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactHud) 7.5.sp else 9.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = liveScopeQuantity.value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactHud) 8.sp else 10.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Text(
                text = if (useMetric) {
                    if (compactHud) {
                        "Lin ${formatLiveValue(linearFeet * 0.3048, 2)} m"
                    } else {
                        "Linear: ${formatLiveValue(linearFeet * 0.3048, 2)} m"
                    }
                } else {
                    if (compactHud) {
                        "Lin ${formatLiveValue(linearFeet, 1)} ft"
                    } else {
                        "Linear Feet: ${formatLiveValue(linearFeet, 1)} ft"
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactHud) 7.5.sp else 9.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = if (useMetric) {
                    if (compactHud) {
                        "Sq ${formatLiveValue(squareFeet * 0.09290304, 2)} m"
                    } else {
                        "Area: ${formatLiveValue(squareFeet * 0.09290304, 2)} sq m"
                    }
                } else {
                    if (compactHud) {
                        "Sq ${formatLiveValue(squareFeet, 1)} sf"
                    } else {
                        "Square Feet: ${formatLiveValue(squareFeet, 1)} sq ft"
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactHud) 7.5.sp else 9.sp),
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
    val cornerRadius = if (compact) 9.dp else 12.dp
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
        shape = RoundedCornerShape(cornerRadius + 2.dp),
        color = blueprintHudContainerColor(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.48f)),
        shadowElevation = 8.dp
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
        shape = RoundedCornerShape(12.dp),
        color = blueprintHudContainerColor(alpha = 0.92f + (emphasis * 0.06f)),
        border = BorderStroke(
            (1f + (0.35f * emphasis)).dp,
            accentColor.copy(alpha = 0.6f + (emphasis * 0.28f))
        ),
        shadowElevation = 7.dp
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
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val textColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = container,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.62f)
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

