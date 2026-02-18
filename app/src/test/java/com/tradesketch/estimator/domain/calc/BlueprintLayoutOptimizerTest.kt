package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintLayoutOptimizerTest {
    @Test
    fun `optimize separates overlapping defaults`() {
        val spaces = listOf(
            room(id = "a"),
            room(id = "b"),
            room(id = "c")
        )

        val optimized = BlueprintLayoutOptimizer.optimize(spaces, gridFeet = 1.0)

        assertEquals(0, overlapPairCount(optimized))
        assertTrue(optimized.all { it.transform.yFeet == 0.0 })
    }

    @Test
    fun `optimize keeps manual placement stable when defaults exist`() {
        val manual = room(
            id = "manual",
            transform = SpaceTransform(
                xFeet = 20.0,
                yFeet = 0.0,
                zFeet = 0.0,
                yawDegrees = 0.0
            )
        )
        val spaces = listOf(
            manual,
            room(id = "default-1"),
            room(id = "default-2")
        )

        val optimized = BlueprintLayoutOptimizer.optimize(spaces, gridFeet = 1.0)
        val manualAfter = optimized.first { it.id == "manual" }

        assertEquals(20.0, manualAfter.transform.xFeet)
        assertEquals(0.0, manualAfter.transform.zFeet)
        assertEquals(0, overlapPairCount(optimized))
    }

    @Test
    fun `optimize snaps axes and yaw while grounding elevations`() {
        val skewed = room(
            id = "skewed",
            transform = SpaceTransform(
                xFeet = 3.36,
                yFeet = 2.2,
                zFeet = 4.74,
                yawDegrees = 13.0
            )
        )

        val optimized = BlueprintLayoutOptimizer.optimize(listOf(skewed), gridFeet = 1.0)
        val result = optimized.single().transform

        assertEquals(3.0, result.xFeet)
        assertEquals(0.0, result.yFeet)
        assertEquals(5.0, result.zFeet)
        assertEquals(15.0, result.yawDegrees)
    }
}

private fun room(
    id: String,
    transform: SpaceTransform = SpaceTransform()
): Space {
    return Space(
        id = id,
        name = "Room $id",
        geometry = Geometry.Rect(
            length = Millimeters.fromFeet(12.0),
            width = Millimeters.fromFeet(10.0)
        ),
        transform = transform
    )
}

private data class Footprint(
    val minX: Double,
    val maxX: Double,
    val minZ: Double,
    val maxZ: Double
)

private fun overlapPairCount(spaces: List<Space>): Int {
    if (spaces.size < 2) return 0
    var count = 0
    for (i in 0 until spaces.lastIndex) {
        val a = footprint(spaces[i])
        for (j in (i + 1) until spaces.size) {
            val b = footprint(spaces[j])
            if (a.maxX >= b.minX && b.maxX >= a.minX && a.maxZ >= b.minZ && b.maxZ >= a.minZ) {
                count += 1
            }
        }
    }
    return count
}

private fun footprint(space: Space): Footprint {
    val (width, depth) = when (val geometry = space.geometry) {
        is Geometry.Rect -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Slab -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Wall -> geometry.length.toFeet() to 0.75
        is Geometry.Circle -> {
            val diameter = geometry.radius.toFeet() * 2.0
            diameter to diameter
        }
        is Geometry.LShape -> {
            val width = max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet())
            val depth = max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet())
            width to depth
        }
    }
    val halfW = width / 2.0
    val halfD = depth / 2.0
    return Footprint(
        minX = space.transform.xFeet - halfW,
        maxX = space.transform.xFeet + halfW,
        minZ = space.transform.zFeet - halfD,
        maxZ = space.transform.zFeet + halfD
    )
}
