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

internal fun resolveWallDisplayStyle(
    wall: WallSegment,
    activeScope: TakeoffScope
): BlueprintWallDisplayStyle {
    val wallScope = wall.visualTradeScope()
    val resolvedScope = wallScope ?: activeScope
    val isActive = wallScope == null || wallScope == activeScope
    return when (resolvedScope) {
        TakeoffScope.DRYWALL -> BlueprintWallDisplayStyle(
            baseColor = (if (isActive) Color(0xFF9FDEFF) else Color(0xFF9FDEFF)).copy(alpha = if (isActive) 1f else 0.7f),
            accentColor = Color(0xFFDDF6FF).copy(alpha = if (isActive) 0.82f else 0.54f),
            pattern = BlueprintWallAccentPattern.DRYWALL_PARALLEL
        )
        TakeoffScope.CONCRETE -> BlueprintWallDisplayStyle(
            baseColor = (if (isActive) Color(0xFFFF9A82) else Color(0xFFFF9A82)).copy(alpha = if (isActive) 1f else 0.72f),
            accentColor = Color(0xFFFFDFC8).copy(alpha = if (isActive) 0.8f else 0.52f),
            pattern = BlueprintWallAccentPattern.CONCRETE_TICKS
        )
        TakeoffScope.GRAVEL_MULCH -> BlueprintWallDisplayStyle(
            baseColor = (if (isActive) Color(0xFFFFD54D) else Color(0xFFFFD54D)).copy(alpha = if (isActive) 1f else 0.72f),
            accentColor = Color(0xFFFFF0B3).copy(alpha = if (isActive) 0.86f else 0.56f),
            pattern = BlueprintWallAccentPattern.GRAVEL_PEBBLES
        )
        TakeoffScope.PAINT -> BlueprintWallDisplayStyle(
            baseColor = (if (isActive) Color(0xFF7FF0B0) else Color(0xFF7FF0B0)).copy(alpha = if (isActive) 1f else 0.72f),
            accentColor = Color(0xFFD9FFE7).copy(alpha = if (isActive) 0.8f else 0.5f),
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
            fillColor = if (active) Color(0x38FF8D78) else Color(0x20FF8D78),
            outlineColor = if (active) Color(0xFFFFB29A) else Color(0xB3FFB29A),
            patternColor = if (active) Color(0x66FFE0D8) else Color(0x40FFD5CB),
            pattern = BlueprintRoomFillPattern.CONCRETE_HATCH
        )
        TakeoffScope.GRAVEL_MULCH -> BlueprintRoomFillStyle(
            fillColor = if (active) Color(0x33FFD56B) else Color(0x1CFFD56B),
            outlineColor = if (active) Color(0xFFFFE59C) else Color(0xB3FFE59C),
            patternColor = if (active) Color(0x73FFF2BE) else Color(0x4DFFF2BE),
            pattern = BlueprintRoomFillPattern.GRAVEL_PEBBLES
        )
        TakeoffScope.DRYWALL,
        TakeoffScope.PAINT -> null
    }
}
