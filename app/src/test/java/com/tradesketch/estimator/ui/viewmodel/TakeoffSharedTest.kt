package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Space
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TakeoffSharedTest {
    @Test
    fun `paintable spaces prefers explicit paint tags when present`() {
        val project = Project(
            id = "p1",
            name = "Tagged Paint",
            spaces = listOf(
                Space(
                    id = "wall-1",
                    name = "Wall 1",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(10.0),
                        height = Millimeters.fromFeet(8.0)
                    ),
                    tags = setOf("drywall")
                ),
                Space(
                    id = "room-1",
                    name = "Room 1",
                    geometry = Geometry.Rect(
                        length = Millimeters.fromFeet(10.0),
                        width = Millimeters.fromFeet(10.0)
                    ),
                    tags = setOf("paint")
                )
            )
        )

        val paintable = project.paintableSpaces()
        assertEquals(1, paintable.size)
        assertTrue(paintable.single().id == "room-1")
    }

    @Test
    fun `paintable spaces falls back to wall and room geometry`() {
        val project = Project(
            id = "p2",
            name = "Fallback Paint",
            spaces = listOf(
                Space(
                    id = "wall-1",
                    name = "Wall 1",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(10.0),
                        height = Millimeters.fromFeet(8.0)
                    )
                ),
                Space(
                    id = "room-1",
                    name = "Room 1",
                    geometry = Geometry.Rect(
                        length = Millimeters.fromFeet(12.0),
                        width = Millimeters.fromFeet(10.0)
                    )
                ),
                Space(
                    id = "slab-1",
                    name = "Slab 1",
                    geometry = Geometry.Slab(
                        length = Millimeters.fromFeet(10.0),
                        width = Millimeters.fromFeet(10.0),
                        thickness = Millimeters.fromFeet(0.33)
                    )
                )
            )
        )

        val paintable = project.paintableSpaces()
        assertEquals(2, paintable.size)
        assertTrue(paintable.any { it.id == "wall-1" })
        assertTrue(paintable.any { it.id == "room-1" })
    }
}
