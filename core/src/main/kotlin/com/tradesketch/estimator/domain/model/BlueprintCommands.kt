package com.tradesketch.estimator.domain.model

/**
 * Command abstraction for blueprint editing.
 * Editors apply commands and can deterministically undo/redo each one.
 */
sealed interface BlueprintCommand {
    val label: String

    fun apply(document: BlueprintDocument): BlueprintDocument

    fun undo(document: BlueprintDocument): BlueprintDocument
}

/**
 * Generic command wrapper for a known before/after blueprint transition.
 */
data class BlueprintDocumentCommand(
    override val label: String,
    private val before: BlueprintDocument,
    private val after: BlueprintDocument
) : BlueprintCommand {
    override fun apply(document: BlueprintDocument): BlueprintDocument = after

    override fun undo(document: BlueprintDocument): BlueprintDocument = before
}
