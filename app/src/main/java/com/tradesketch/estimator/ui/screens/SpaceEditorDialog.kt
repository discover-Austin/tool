package com.tradesketch.estimator.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Opening
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.volumeCuFt
import com.tradesketch.estimator.utils.Formatters
import com.tradesketch.estimator.utils.Validators
import java.util.UUID
import java.util.Locale

@Composable
internal fun SpaceEditorDialog(
    initialSpace: Space?,
    onDismiss: () -> Unit,
    onSave: (Space) -> Unit
) {
    val isEditing = initialSpace != null
    var validationError by remember(initialSpace?.id) { mutableStateOf<String?>(null) }
    var name by remember(initialSpace?.id) { mutableStateOf(initialSpace?.name.orEmpty()) }
    var geometryType by remember(initialSpace?.id) {
        mutableStateOf(
            initialSpace?.geometry?.toEditorType() ?: SpaceGeometryType.WALL
        )
    }
    var lengthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.lengthForEditor())
    }
    var widthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.widthForEditor())
    }
    var heightFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.heightForEditor())
    }
    var thicknessFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.thicknessForEditor())
    }
    var radiusFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.radiusForEditor())
    }
    var rectALengthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.rectALengthForEditor())
    }
    var rectAWidthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.rectAWidthForEditor())
    }
    var rectBLengthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.rectBLengthForEditor())
    }
    var rectBWidthFt by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.geometry.rectBWidthForEditor())
    }
    var showOpeningsEditor by remember(initialSpace?.id) {
        mutableStateOf(initialSpace?.openings?.isNotEmpty() == true)
    }
    var showLivePreview by remember(initialSpace?.id) { mutableStateOf(false) }
    var currentStep by remember(initialSpace?.id) { mutableStateOf(SpaceEditorStep.BASICS) }

    val openingDrafts = remember(initialSpace?.id) {
        mutableStateListOf<OpeningDraft>().apply {
            initialSpace?.openings?.forEach { opening ->
                add(
                    OpeningDraft(
                        widthFt = opening.width.toEditorFeetString(),
                        heightFt = opening.height.toEditorFeetString(),
                        count = opening.count.toString()
                    )
                )
            }
        }
    }

    val previewGeometry = buildPreviewGeometry(
        geometryType = geometryType,
        lengthFt = lengthFt,
        widthFt = widthFt,
        heightFt = heightFt,
        thicknessFt = thicknessFt,
        radiusFt = radiusFt,
        rectALengthFt = rectALengthFt,
        rectAWidthFt = rectAWidthFt,
        rectBLengthFt = rectBLengthFt,
        rectBWidthFt = rectBWidthFt
    )
    val previewOpeningsArea = openingDrafts.sumOf { draft ->
        val width = Validators.parsePositiveDouble(draft.widthFt) ?: return@sumOf 0.0
        val height = Validators.parsePositiveDouble(draft.heightFt) ?: return@sumOf 0.0
        val count = Validators.parsePositiveInt(draft.count) ?: return@sumOf 0.0
        width * height * count
    }
    val configuredOpeningRows = openingDrafts.count { draft ->
        draft.widthFt.isNotBlank() || draft.heightFt.isNotBlank() || draft.count.isNotBlank()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Space" else "Add Space") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep == SpaceEditorStep.BASICS) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Space Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Geometry",
                    style = MaterialTheme.typography.titleSmall
                )
                GeometryTypePicker(
                    selected = geometryType,
                    onSelected = { geometryType = it }
                )
                }

                if (currentStep == SpaceEditorStep.GEOMETRY) {
                when (geometryType) {
                    SpaceGeometryType.WALL -> {
                        DimensionField("Length (ft)", lengthFt) { lengthFt = it }
                        DimensionField("Height (ft)", heightFt) { heightFt = it }
                    }
                    SpaceGeometryType.ROOM -> {
                        DimensionField("Length (ft)", lengthFt) { lengthFt = it }
                        DimensionField("Width (ft)", widthFt) { widthFt = it }
                    }
                    SpaceGeometryType.SLAB -> {
                        DimensionField("Length (ft)", lengthFt) { lengthFt = it }
                        DimensionField("Width (ft)", widthFt) { widthFt = it }
                        DimensionField("Thickness (ft)", thicknessFt) { thicknessFt = it }
                    }
                    SpaceGeometryType.CIRCLE -> {
                        DimensionField("Radius (ft)", radiusFt) { radiusFt = it }
                    }
                    SpaceGeometryType.L_SHAPE -> {
                        Text(
                            text = "Rectangle A",
                            style = MaterialTheme.typography.titleSmall
                        )
                        DimensionField("A Length (ft)", rectALengthFt) { rectALengthFt = it }
                        DimensionField("A Width (ft)", rectAWidthFt) { rectAWidthFt = it }
                        Text(
                            text = "Rectangle B",
                            style = MaterialTheme.typography.titleSmall
                        )
                        DimensionField("B Length (ft)", rectBLengthFt) { rectBLengthFt = it }
                        DimensionField("B Width (ft)", rectBWidthFt) { rectBWidthFt = it }
                    }
                }
                }

                if (currentStep == SpaceEditorStep.OPENINGS && geometryType.supportsOpenings) {
                    Text(
                        text = "Openings (optional)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (configuredOpeningRows == 0) {
                            "No openings added."
                        } else {
                            "$configuredOpeningRows opening entry(ies) configured."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showOpeningsEditor = !showOpeningsEditor },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showOpeningsEditor) {
                                "Hide Opening Editor"
                            } else {
                                "Show Opening Editor"
                            }
                        )
                    }
                    if (showOpeningsEditor) {
                        if (openingDrafts.isNotEmpty()) {
                            openingDrafts.forEachIndexed { index, draft ->
                                OpeningEditorRow(
                                    openingNumber = index + 1,
                                    draft = draft,
                                    onWidthChange = { openingDrafts[index] = draft.copy(widthFt = it) },
                                    onHeightChange = { openingDrafts[index] = draft.copy(heightFt = it) },
                                    onCountChange = { openingDrafts[index] = draft.copy(count = it) },
                                    onDelete = { openingDrafts.removeAt(index) }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { openingDrafts.add(OpeningDraft()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Opening")
                        }
                    }
                }
                if (currentStep == SpaceEditorStep.OPENINGS && !geometryType.supportsOpenings) {
                    Text(
                        text = "${geometryType.label} does not use openings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (currentStep == SpaceEditorStep.REVIEW) {
                previewGeometry?.let { geometry ->
                    val areaSqFt = geometry.areaSqFt()
                    val volumeCuFt = geometry.volumeCuFt()
                    TextButton(
                        onClick = { showLivePreview = !showLivePreview },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showLivePreview) {
                                "Hide Live Preview"
                            } else {
                                "Show Live Preview"
                            }
                        )
                    }
                    if (showLivePreview) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Live Preview",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Area: ${Formatters.formatArea(areaSqFt)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (previewOpeningsArea > 0.0) {
                                    Text(
                                        text = "Openings: ${Formatters.formatQuantity(previewOpeningsArea)} sq ft",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Net area: ${Formatters.formatQuantity((areaSqFt - previewOpeningsArea).coerceAtLeast(0.0))} sq ft",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (volumeCuFt > 0.0) {
                                    Text(
                                        text = "Volume: ${Formatters.formatQuantity(volumeCuFt)} cu ft",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                if (previewGeometry == null) {
                    Text(
                        text = "Complete geometry fields to review the live preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }

                validationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = null
                    if (currentStep != SpaceEditorStep.REVIEW) {
                        currentStep = currentStep.next()
                        return@Button
                    }
                    if (!Validators.isValidSpaceName(name)) {
                        validationError = "Enter a space name (1-50 chars)."
                        return@Button
                    }

                    fun parseDimension(value: String, fieldName: String): Millimeters? {
                        val parsed = Validators.parsePositiveDouble(value)
                        if (parsed == null) {
                            validationError = "$fieldName must be a positive number."
                            return null
                        }
                        return Millimeters.fromFeet(parsed)
                    }

                    val geometry = when (geometryType) {
                        SpaceGeometryType.WALL -> {
                            val length = parseDimension(lengthFt, "Length") ?: return@Button
                            val height = parseDimension(heightFt, "Height") ?: return@Button
                            Geometry.Wall(length = length, height = height)
                        }
                        SpaceGeometryType.ROOM -> {
                            val length = parseDimension(lengthFt, "Length") ?: return@Button
                            val width = parseDimension(widthFt, "Width") ?: return@Button
                            Geometry.Rect(length = length, width = width)
                        }
                        SpaceGeometryType.SLAB -> {
                            val length = parseDimension(lengthFt, "Length") ?: return@Button
                            val width = parseDimension(widthFt, "Width") ?: return@Button
                            val thickness = parseDimension(thicknessFt, "Thickness") ?: return@Button
                            Geometry.Slab(length = length, width = width, thickness = thickness)
                        }
                        SpaceGeometryType.CIRCLE -> {
                            val radius = parseDimension(radiusFt, "Radius") ?: return@Button
                            Geometry.Circle(radius = radius)
                        }
                        SpaceGeometryType.L_SHAPE -> {
                            val aLength = parseDimension(rectALengthFt, "A Length") ?: return@Button
                            val aWidth = parseDimension(rectAWidthFt, "A Width") ?: return@Button
                            val bLength = parseDimension(rectBLengthFt, "B Length") ?: return@Button
                            val bWidth = parseDimension(rectBWidthFt, "B Width") ?: return@Button
                            Geometry.LShape(
                                rectA = Geometry.Rect(aLength, aWidth),
                                rectB = Geometry.Rect(bLength, bWidth)
                            )
                        }
                    }

                    val openings = mutableListOf<Opening>()
                    if (geometryType.supportsOpenings) {
                        openingDrafts.forEachIndexed { index, draft ->
                            val hasInput = draft.widthFt.isNotBlank() ||
                                draft.heightFt.isNotBlank() ||
                                draft.count.isNotBlank()
                            if (!hasInput) return@forEachIndexed
                            val width = Validators.parsePositiveDouble(draft.widthFt)
                            val height = Validators.parsePositiveDouble(draft.heightFt)
                            val count = Validators.parsePositiveInt(draft.count)
                            if (width == null || height == null || count == null) {
                                validationError = "Opening ${index + 1} has invalid values."
                                return@Button
                            }
                            openings += Opening(
                                width = Millimeters.fromFeet(width),
                                height = Millimeters.fromFeet(height),
                                count = count
                            )
                        }
                    }

                    onSave(
                        Space(
                            id = initialSpace?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            geometry = geometry,
                            openings = openings,
                            transform = initialSpace?.transform ?: SpaceTransform()
                        )
                    )
                },
                enabled = if (currentStep == SpaceEditorStep.REVIEW) name.isNotBlank() else true
            ) {
                Text(
                    if (currentStep == SpaceEditorStep.REVIEW) {
                        if (isEditing) "Save Changes" else "Add Space"
                    } else {
                        "Continue"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (currentStep == SpaceEditorStep.BASICS) {
                        onDismiss()
                    } else {
                        validationError = null
                        currentStep = currentStep.previous()
                    }
                }
            ) {
                Text(if (currentStep == SpaceEditorStep.BASICS) "Cancel" else "Back")
            }
        }
    )
}

@Composable
private fun GeometryTypePicker(
    selected: SpaceGeometryType,
    onSelected: (SpaceGeometryType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GeometryChip(
                type = SpaceGeometryType.WALL,
                selected = selected,
                onSelected = onSelected
            )
            GeometryChip(
                type = SpaceGeometryType.ROOM,
                selected = selected,
                onSelected = onSelected
            )
            GeometryChip(
                type = SpaceGeometryType.SLAB,
                selected = selected,
                onSelected = onSelected
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GeometryChip(
                type = SpaceGeometryType.CIRCLE,
                selected = selected,
                onSelected = onSelected
            )
            GeometryChip(
                type = SpaceGeometryType.L_SHAPE,
                selected = selected,
                onSelected = onSelected
            )
        }
    }
}

@Composable
private fun GeometryChip(
    type: SpaceGeometryType,
    selected: SpaceGeometryType,
    onSelected: (SpaceGeometryType) -> Unit
) {
    FilterChip(
        selected = selected == type,
        onClick = { onSelected(type) },
        label = { Text(type.label) }
    )
}

@Composable
private fun OpeningEditorRow(
    openingNumber: Int,
    draft: OpeningDraft,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opening $openingNumber",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete opening"
                    )
                }
            }
            DimensionField(
                label = "Width (ft)",
                value = draft.widthFt,
                onValueChange = onWidthChange
            )
            DimensionField(
                label = "Height (ft)",
                value = draft.heightFt,
                onValueChange = onHeightChange
            )
            OutlinedTextField(
                value = draft.count,
                onValueChange = onCountChange,
                label = { Text("Count") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DimensionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}


private enum class SpaceGeometryType(
    val label: String,
    val supportsOpenings: Boolean
) {
    WALL("Wall", true),
    ROOM("Room", true),
    SLAB("Slab", false),
    CIRCLE("Circle", false),
    L_SHAPE("L-Shape", false)
}

private fun Geometry.toEditorType(): SpaceGeometryType {
    return when (this) {
        is Geometry.Wall -> SpaceGeometryType.WALL
        is Geometry.Rect -> SpaceGeometryType.ROOM
        is Geometry.Slab -> SpaceGeometryType.SLAB
        is Geometry.Circle -> SpaceGeometryType.CIRCLE
        is Geometry.LShape -> SpaceGeometryType.L_SHAPE
    }
}

private fun buildPreviewGeometry(
    geometryType: SpaceGeometryType,
    lengthFt: String,
    widthFt: String,
    heightFt: String,
    thicknessFt: String,
    radiusFt: String,
    rectALengthFt: String,
    rectAWidthFt: String,
    rectBLengthFt: String,
    rectBWidthFt: String
): Geometry? {
    fun parseFeetOrNull(raw: String): Millimeters? {
        val parsed = Validators.parsePositiveDouble(raw) ?: return null
        return Millimeters.fromFeet(parsed)
    }

    return when (geometryType) {
        SpaceGeometryType.WALL -> {
            val length = parseFeetOrNull(lengthFt) ?: return null
            val height = parseFeetOrNull(heightFt) ?: return null
            Geometry.Wall(length = length, height = height)
        }
        SpaceGeometryType.ROOM -> {
            val length = parseFeetOrNull(lengthFt) ?: return null
            val width = parseFeetOrNull(widthFt) ?: return null
            Geometry.Rect(length = length, width = width)
        }
        SpaceGeometryType.SLAB -> {
            val length = parseFeetOrNull(lengthFt) ?: return null
            val width = parseFeetOrNull(widthFt) ?: return null
            val thickness = parseFeetOrNull(thicknessFt) ?: return null
            Geometry.Slab(length = length, width = width, thickness = thickness)
        }
        SpaceGeometryType.CIRCLE -> {
            val radius = parseFeetOrNull(radiusFt) ?: return null
            Geometry.Circle(radius = radius)
        }
        SpaceGeometryType.L_SHAPE -> {
            val aLength = parseFeetOrNull(rectALengthFt) ?: return null
            val aWidth = parseFeetOrNull(rectAWidthFt) ?: return null
            val bLength = parseFeetOrNull(rectBLengthFt) ?: return null
            val bWidth = parseFeetOrNull(rectBWidthFt) ?: return null
            Geometry.LShape(
                rectA = Geometry.Rect(aLength, aWidth),
                rectB = Geometry.Rect(bLength, bWidth)
            )
        }
    }
}

private fun Geometry?.lengthForEditor(): String {
    return when (this) {
        is Geometry.Wall -> length.toEditorFeetString()
        is Geometry.Rect -> length.toEditorFeetString()
        is Geometry.Slab -> length.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.widthForEditor(): String {
    return when (this) {
        is Geometry.Rect -> width.toEditorFeetString()
        is Geometry.Slab -> width.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.heightForEditor(): String {
    return when (this) {
        is Geometry.Wall -> height.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.thicknessForEditor(): String {
    return when (this) {
        is Geometry.Slab -> thickness.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.radiusForEditor(): String {
    return when (this) {
        is Geometry.Circle -> radius.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.rectALengthForEditor(): String {
    return when (this) {
        is Geometry.LShape -> rectA.length.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.rectAWidthForEditor(): String {
    return when (this) {
        is Geometry.LShape -> rectA.width.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.rectBLengthForEditor(): String {
    return when (this) {
        is Geometry.LShape -> rectB.length.toEditorFeetString()
        else -> ""
    }
}

private fun Geometry?.rectBWidthForEditor(): String {
    return when (this) {
        is Geometry.LShape -> rectB.width.toEditorFeetString()
        else -> ""
    }
}

private fun Millimeters.toEditorFeetString(): String {
    val feet = toFeet()
    val fixed = String.format(Locale.US, "%.2f", feet)
    return fixed.trimEnd('0').trimEnd('.')
}

private enum class SpaceEditorStep(val index: Int, val label: String) {
    BASICS(0, "Basics"),
    GEOMETRY(1, "Geometry"),
    OPENINGS(2, "Openings"),
    REVIEW(3, "Review");

    fun next(): SpaceEditorStep {
        return entries.getOrNull(index + 1) ?: this
    }

    fun previous(): SpaceEditorStep {
        return entries.getOrNull(index - 1) ?: this
    }
}


