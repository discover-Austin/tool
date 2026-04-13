package com.tradesketch.estimator.ui.blueprint

import androidx.compose.ui.graphics.Color
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.takeoffScopeOrNull

internal enum class BlueprintRoomFillPattern {
    CONCRETE_HATCH,
    GRAVEL_PEBBLES
}

internal enum class BlueprintWallAccentPattern {
    DRYWALL_PARALLEL,
    CONCRETE_TICKS,
    GRAVEL_PEBBLES,
    PAINT_DASH
}

internal data class BlueprintWallDisplayStyle(
    val baseColor: Color,
    val accentColor: Color,
    val pattern: BlueprintWallAccentPattern?
)

internal data class BlueprintRoomFillStyle(
    val fillColor: Color,
    val outlineColor: Color,
    val patternColor: Color,
    val pattern: BlueprintRoomFillPattern
)

internal fun WallSegment.visualTradeScope(): TakeoffScope? = tags.takeoffScopeOrNull()

internal fun TakeoffScope.tradeWallBaseColor(): Color = when (this) {
    TakeoffScope.DRYWALL -> Color(0xFF12E7FF)
    TakeoffScope.CONCRETE -> Color(0xFFFF6B4A)
    TakeoffScope.GRAVEL_MULCH -> Color(0xFFFFF34D)
    TakeoffScope.PAINT -> Color(0xFF3DFF8A)
}

internal fun TakeoffScope.tradeWallAccentColor(): Color = when (this) {
    TakeoffScope.DRYWALL -> Color(0xFFD7FCFF)
    TakeoffScope.CONCRETE -> Color(0xFFFFDED5)
    TakeoffScope.GRAVEL_MULCH -> Color(0xFFFFFFBE)
    TakeoffScope.PAINT -> Color(0xFFD8FFE8)
}

internal fun resolveWallDisplayStyle(
    wall: WallSegment,
    activeScope: TakeoffScope
): BlueprintWallDisplayStyle {
    val wallScope = wall.visualTradeScope()
    val resolvedScope = wallScope ?: activeScope
    val isActive = wallScope == null || wallScope == activeScope
    return when (resolvedScope) {
        TakeoffScope.DRYWALL -> BlueprintWallDisplayStyle(
            baseColor = resolvedScope.tradeWallBaseColor().copy(alpha = if (isActive) 1f else 0.82f),
            accentColor = resolvedScope.tradeWallAccentColor().copy(alpha = if (isActive) 0.96f else 0.74f),
            pattern = BlueprintWallAccentPattern.DRYWALL_PARALLEL
        )
        TakeoffScope.CONCRETE -> BlueprintWallDisplayStyle(
            baseColor = resolvedScope.tradeWallBaseColor().copy(alpha = if (isActive) 1f else 0.82f),
            accentColor = resolvedScope.tradeWallAccentColor().copy(alpha = if (isActive) 0.94f else 0.72f),
            pattern = BlueprintWallAccentPattern.CONCRETE_TICKS
        )
        TakeoffScope.GRAVEL_MULCH -> BlueprintWallDisplayStyle(
            baseColor = resolvedScope.tradeWallBaseColor().copy(alpha = if (isActive) 1f else 0.84f),
            accentColor = resolvedScope.tradeWallAccentColor().copy(alpha = if (isActive) 0.96f else 0.74f),
            pattern = BlueprintWallAccentPattern.GRAVEL_PEBBLES
        )
        TakeoffScope.PAINT -> BlueprintWallDisplayStyle(
            baseColor = resolvedScope.tradeWallBaseColor().copy(alpha = if (isActive) 1f else 0.82f),
            accentColor = resolvedScope.tradeWallAccentColor().copy(alpha = if (isActive) 0.94f else 0.7f),
            pattern = BlueprintWallAccentPattern.PAINT_DASH
        )
    }
}

internal fun resolveWallDisplayColor(
    wall: WallSegment,
    activeScope: TakeoffScope
): Color = resolveWallDisplayStyle(wall, activeScope).baseColor

internal fun resolveVisibleRoomTradeScope(
    room: Room,
    wallsById: Map<String, WallSegment>,
    activeScope: TakeoffScope
): TakeoffScope? {
    return room.takeoffScopeOrNull(wallsById)
        ?: activeScope.takeIf { it.usesRoomSurfaceFill() }
}

internal fun TakeoffScope.usesRoomSurfaceFill(): Boolean = when (this) {
    TakeoffScope.CONCRETE,
    TakeoffScope.GRAVEL_MULCH -> true
    TakeoffScope.DRYWALL,
    TakeoffScope.PAINT -> false
}

internal fun TakeoffScope.roomFillStyle(active: Boolean): BlueprintRoomFillStyle? {
    return when (this) {
        TakeoffScope.CONCRETE -> BlueprintRoomFillStyle(
            fillColor = if (active) Color(0x42FF845C) else Color(0x22FF845C),
            outlineColor = if (active) Color(0xFFFFB49F) else Color(0xBAFFB49F),
            patternColor = if (active) Color(0x72FFE2D7) else Color(0x46FFE2D7),
            pattern = BlueprintRoomFillPattern.CONCRETE_HATCH
        )
        TakeoffScope.GRAVEL_MULCH -> BlueprintRoomFillStyle(
            fillColor = if (active) Color(0x3AFFCF3A) else Color(0x1FFFCF3A),
            outlineColor = if (active) Color(0xFFFFE69A) else Color(0xBAFFE69A),
            patternColor = if (active) Color(0x78FFF2BA) else Color(0x50FFF2BA),
            pattern = BlueprintRoomFillPattern.GRAVEL_PEBBLES
        )
        TakeoffScope.DRYWALL,
        TakeoffScope.PAINT -> null
    }
}
