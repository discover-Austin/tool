package com.tradesketch.estimator

import com.tradesketch.estimator.domain.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceProjectSaveStateTest {

    @Test
    fun `saved header shows no pending changes when draft matches project name`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")

        assertFalse(hasPendingProjectHeaderChanges(project, "Kitchen Remodel"))
    }

    @Test
    fun `header reports pending changes when trimmed draft differs from saved name`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")

        assertTrue(hasPendingProjectHeaderChanges(project, "Garage Remodel"))
    }

    @Test
    fun `header ignores whitespace only edits when comparing against saved name`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")

        assertFalse(hasPendingProjectHeaderChanges(project, "  Kitchen Remodel  "))
    }

    @Test
    fun `saved badge stays hidden until user explicitly saves`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")
        val currentFingerprint = projectHeaderStateFingerprint(project, "Kitchen Remodel")

        assertFalse(
            shouldShowSavedProjectBadge(
                explicitSaveFingerprint = null,
                hasProjectChangedSinceExplicitSave = false,
                currentProjectFingerprint = currentFingerprint
            )
        )
    }

    @Test
    fun `saved badge shows only for an unchanged explicit save snapshot`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")
        val fingerprint = projectHeaderStateFingerprint(project, "Kitchen Remodel")

        assertTrue(
            shouldShowSavedProjectBadge(
                explicitSaveFingerprint = fingerprint,
                hasProjectChangedSinceExplicitSave = false,
                currentProjectFingerprint = fingerprint
            )
        )
    }

    @Test
    fun `saved badge stays hidden after a change until save is pressed again`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")
        val fingerprint = projectHeaderStateFingerprint(project, "Kitchen Remodel")

        assertFalse(
            shouldShowSavedProjectBadge(
                explicitSaveFingerprint = fingerprint,
                hasProjectChangedSinceExplicitSave = true,
                currentProjectFingerprint = fingerprint
            )
        )
    }

    @Test
    fun `project fingerprint ignores updated at noise`() {
        val project = Project(id = "project-1", name = "Kitchen Remodel")
        val olderFingerprint = projectHeaderStateFingerprint(project.copy(updatedAt = 1L), "Kitchen Remodel")
        val newerFingerprint = projectHeaderStateFingerprint(project.copy(updatedAt = 99L), "Kitchen Remodel")

        assertEquals(olderFingerprint, newerFingerprint)
    }
}
