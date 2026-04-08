package com.tradesketch.estimator.ui.blueprint

import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintFloorLabelTest {

    @Test
    fun inlineFloorLabel_usesSimpleNumericLabeling() {
        assertEquals("fl:1", FLOOR_GROUND_LEVEL.inlineFloorLabel())
        assertEquals("fl:2", 1.inlineFloorLabel())
        assertEquals("fl:3", 2.inlineFloorLabel())
        assertEquals("fl:-1", (-1).inlineFloorLabel())
        assertEquals("fl:-2", (-2).inlineFloorLabel())
    }
}
