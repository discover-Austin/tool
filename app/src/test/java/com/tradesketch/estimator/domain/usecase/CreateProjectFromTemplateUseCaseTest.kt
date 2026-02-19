package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.domain.model.ProjectTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateProjectFromTemplateUseCaseTest {
    
    private val useCase = CreateProjectFromTemplateUseCase()
    
    @Test
    fun `creates bedroom template with correct blueprint`() {
        val project = useCase(ProjectTemplate.BEDROOM)
        val blueprint = project.blueprintDocument
        
        assertEquals("Bedroom", project.name)
        assertEquals(4, blueprint.walls.size) // 4 walls
        assertEquals(1, blueprint.rooms.size) // 1 room (ceiling as room)
        assertEquals(3, blueprint.openings.size) // 1 door + 2 windows
        assertTrue(blueprint.rooms.any { it.name == "Bedroom" })
    }
    
    @Test
    fun `creates garage template with slab room`() {
        val project = useCase(ProjectTemplate.GARAGE)
        val blueprint = project.blueprintDocument
        
        assertEquals("Garage", project.name)
        assertEquals(0, blueprint.walls.size)
        assertEquals(1, blueprint.rooms.size)
        assertTrue(blueprint.rooms.any { it.name == "Garage slab" })
    }
    
    @Test
    fun `creates blank project with empty blueprint`() {
        val project = useCase(ProjectTemplate.BLANK, "My Project")
        val blueprint = project.blueprintDocument
        
        assertEquals("My Project", project.name)
        assertEquals(0, blueprint.walls.size)
        assertEquals(0, blueprint.rooms.size)
        assertEquals(0, blueprint.openings.size)
    }
}
