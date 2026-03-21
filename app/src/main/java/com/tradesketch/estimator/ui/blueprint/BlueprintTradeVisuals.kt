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

internal data class BlueprintRoomFillStyle(
    val fillColor: Color,
    val outlineColor: Color,
    val patternColor: Color,
    val pattern: BlueprintRoomFillPattern
)

internal fun WallSegment.visualTradeScope(): TakeoffScope? = tags.takeoffScopeOrNull()

internal fun resolveWallDisplayColor(
    wall: WallSegment,
    activeScope: TakeoffScope
): Color {
    val wallScope = wall.visualTradeScope()
    val base = (wallScope ?: activeScope).wallColor()
    return if (wallScope == null || wallScope == activeScope) {
        base
    } else {
        base.copy(alpha = 0.7f)
    }
}

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
