package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.domain.model.ProjectTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateProjectFromTemplateUseCaseTest {
    
    private val useCase = CreateProjectFromTemplateUseCase()
    
    @Test
    fun `creates bedroom template with correct spaces`() {
        val project = useCase(ProjectTemplate.BEDROOM)
        
        assertEquals("Bedroom", project.name)
        assertEquals(5, project.spaces.size) // 4 walls + ceiling
        assertTrue(project.spaces.any { it.name == "Wall 1" })
        assertTrue(project.spaces.any { it.name == "Ceiling" })
    }
    
    @Test
    fun `creates garage template with slab`() {
        val project = useCase(ProjectTemplate.GARAGE)
        
        assertEquals("Garage", project.name)
        assertEquals(1, project.spaces.size)
        assertTrue(project.spaces.any { it.name == "Garage slab" })
    }
    
    @Test
    fun `creates blank project with no spaces`() {
        val project = useCase(ProjectTemplate.BLANK, "My Project")
        
        assertEquals("My Project", project.name)
        assertEquals(0, project.spaces.size)
    }
}
