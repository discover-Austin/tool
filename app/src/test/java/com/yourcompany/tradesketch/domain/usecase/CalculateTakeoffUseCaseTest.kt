package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.domain.calc.TakeoffCalculator
import com.yourcompany.tradesketch.domain.model.Geometry
import com.yourcompany.tradesketch.domain.model.Millimeters
import com.yourcompany.tradesketch.domain.model.Space
import kotlin.test.Test
import kotlin.test.assertTrue

class CalculateTakeoffUseCaseTest {
    
    private val useCase = CalculateTakeoffUseCase(TakeoffCalculator)
    
    @Test
    fun `calculateDrywall returns result`() {
        val wall = Space(
            id = "1",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(10.0),
                height = Millimeters.fromFeet(8.0)
            )
        )
        
        val result = useCase.calculateDrywall(
            walls = listOf(wall),
            sheetAreaSqFt = 32.0,
            wastePercent = 10.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5
        )
        
        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.any { it.name == "Drywall sheets" })
    }
    
    @Test
    fun `calculateConcrete returns result`() {
        val slab = Space(
            id = "1",
            name = "Slab",
            geometry = Geometry.Slab(
                length = Millimeters.fromFeet(10.0),
                width = Millimeters.fromFeet(10.0),
                thickness = Millimeters.fromFeet(0.33)
            )
        )
        
        val result = useCase.calculateConcrete(
            slabSpaces = listOf(slab),
            thicknessFeet = 0.33,
            wastePercent = 5.0
        )
        
        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.any { it.unit == "cubic yards" })
    }
}
