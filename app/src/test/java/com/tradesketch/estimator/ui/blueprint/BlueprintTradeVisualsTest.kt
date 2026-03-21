package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlueprintTradeVisualsTest {

    @Test
    fun `only concrete and gravel scopes produce room fills`() {
        assertNotNull(TakeoffScope.CONCRETE.roomFillStyle(active = true))
        assertNotNull(TakeoffScope.GRAVEL_MULCH.roomFillStyle(active = true))
        assertNull(TakeoffScope.DRYWALL.roomFillStyle(active = true))
        assertNull(TakeoffScope.PAINT.roomFillStyle(active = true))
    }

    @Test
    fun `inactive wall colors stay trade-specific but get toned down`() {
        val wall = WallSegment(
            id = "conc-wall",
            start = PointMm(0, 0),
            end = PointMm(2_000, 0),
            tags = setOf(TakeoffScope.CONCRETE.wallScopeTag())
        )

        val active = resolveWallDisplayColor(
            wall = wall,
            activeScope = TakeoffScope.CONCRETE
        )
        val inactive = resolveWallDisplayColor(
            wall = wall,
            activeScope = TakeoffScope.DRYWALL
        )

        assertTrue(abs(active.red - inactive.red) < 0.001f)
        assertTrue(abs(active.green - inactive.green) < 0.001f)
        assertTrue(abs(active.blue - inactive.blue) < 0.001f)
        assertTrue(inactive.alpha < active.alpha)
    }

    @Test
    fun `room scope falls back to linked wall scope before active scope`() {
        val concreteWall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(3_000, 0),
            tags = setOf(TakeoffScope.CONCRETE.wallScopeTag())
        )
        val room = Room(
            id = "room-1",
            wallSegmentIds = listOf(concreteWall.id)
        )

        assertEquals(
            TakeoffScope.CONCRETE,
            resolveVisibleRoomTradeScope(
                room = room,
                wallsById = mapOf(concreteWall.id to concreteWall),
                activeScope = TakeoffScope.DRYWALL
            )
        )
    }

    @Test
    fun `unscoped rooms only inherit active scope for filled-surface trades`() {
        val room = Room(id = "room-2")

        assertEquals(
            TakeoffScope.CONCRETE,
            resolveVisibleRoomTradeScope(
                room = room,
                wallsById = emptyMap(),
                activeScope = TakeoffScope.CONCRETE
            )
        )
        assertEquals(
            TakeoffScope.GRAVEL_MULCH,
            resolveVisibleRoomTradeScope(
                room = room,
                wallsById = emptyMap(),
                activeScope = TakeoffScope.GRAVEL_MULCH
            )
        )
        assertNull(
            resolveVisibleRoomTradeScope(
                room = room,
                wallsById = emptyMap(),
                activeScope = TakeoffScope.DRYWALL
            )
        )
        assertNull(
            resolveVisibleRoomTradeScope(
                room = room,
                wallsById = emptyMap(),
                activeScope = TakeoffScope.PAINT
            )
        )
    }
}
