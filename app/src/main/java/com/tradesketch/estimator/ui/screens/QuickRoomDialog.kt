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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tradesketch.estimator.utils.Validators
import java.util.UUID
import java.util.Locale

@Composable
internal fun QuickRoomDialog(
    dialogKey: Int,
    onDismiss: () -> Unit,
    onSave: (List<Space>, Boolean) -> Unit
) {
    var roomName by remember(dialogKey) { mutableStateOf("Room") }
    var lengthFt by remember(dialogKey) { mutableStateOf("12") }
    var widthFt by remember(dialogKey) { mutableStateOf("10") }
    var wallHeightFt by remember(dialogKey) { mutableStateOf("8") }

    var editWalls by remember(dialogKey) { mutableStateOf(false) }
    var wall1Ft by remember(dialogKey) { mutableStateOf("12") }
    var wall2Ft by remember(dialogKey) { mutableStateOf("10") }
    var wall3Ft by remember(dialogKey) { mutableStateOf("12") }
    var wall4Ft by remember(dialogKey) { mutableStateOf("10") }

    var doorCount by remember(dialogKey) { mutableStateOf("1") }
    var doorWidthFt by remember(dialogKey) { mutableStateOf("3") }
    var doorHeightFt by remember(dialogKey) { mutableStateOf("7") }
    var editDoors by remember(dialogKey) { mutableStateOf(false) }
    val customDoors = remember(dialogKey) { mutableStateListOf(OpeningDraft()) }
    var includeExtendedClosetDoor by remember(dialogKey) { mutableStateOf(false) }
    var closetDoorWidthFt by remember(dialogKey) { mutableStateOf("5") }
    var closetDoorHeightFt by remember(dialogKey) { mutableStateOf("7") }

    var windowCount by remember(dialogKey) { mutableStateOf("2") }
    var windowWidthFt by remember(dialogKey) { mutableStateOf("3") }
    var windowHeightFt by remember(dialogKey) { mutableStateOf("4") }
    var editWindows by remember(dialogKey) { mutableStateOf(false) }
    val customWindows = remember(dialogKey) { mutableStateListOf(OpeningDraft()) }

    var includeCeiling by remember(dialogKey) { mutableStateOf(true) }
    var continueToNextRoom by remember(dialogKey) { mutableStateOf(false) }
    var validationError by remember(dialogKey) { mutableStateOf<String?>(null) }
    val presets = remember {
        listOf(
            RoomPreset(label = "Small", lengthFt = "10", widthFt = "10", heightFt = "8", doors = "1", windows = "1"),
            RoomPreset(label = "Standard", lengthFt = "12", widthFt = "10", heightFt = "8", doors = "1", windows = "2"),
            RoomPreset(label = "Large", lengthFt = "16", widthFt = "14", heightFt = "9", doors = "2", windows = "3")
        )
    }

    val previewLength = Validators.parsePositiveDouble(lengthFt)
    val previewWidth = Validators.parsePositiveDouble(widthFt)
    val previewHeight = Validators.parsePositiveDouble(wallHeightFt)
    val previewWallLengths = if (editWalls) {
        listOf(wall1Ft, wall2Ft, wall3Ft, wall4Ft).map { Validators.parsePositiveDouble(it) }
    } else {
        listOf(previewLength, previewWidth, previewLength, previewWidth)
    }
    val previewHasAllWallLengths = previewWallLengths.none { it == null }
    val previewWallArea = if (previewHeight != null && previewHasAllWallLengths) {
        previewWallLengths.filterNotNull().sum() * previewHeight
    } else {
        null
    }
    val previewCeilingArea = if (includeCeiling && previewLength != null && previewWidth != null) {
        previewLength * previewWidth
    } else {
        null
    }
    val previewDoorCount = doorCount.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val previewWindowCount = windowCount.toIntOrNull()?.coerceAtLeast(0) ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Room Wizard") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Live Room Summary",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (previewLength != null && previewWidth != null && previewHeight != null) {
                                "${"%.1f".format(Locale.US, previewLength)}' x ${"%.1f".format(Locale.US, previewWidth)}' x ${"%.1f".format(Locale.US, previewHeight)}'"
                            } else {
                                "Enter length, width, and height"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "$previewDoorCount door(s), $previewWindowCount window(s), " +
                                if (includeCeiling) "ceiling included" else "no ceiling",
                            style = MaterialTheme.typography.bodySmall
                        )
                        previewWallArea?.let {
                            Text(
                                text = "Estimated wall area: ${"%.1f".format(Locale.US, it)} sq ft",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        previewCeilingArea?.let {
                            Text(
                                text = "Estimated ceiling area: ${"%.1f".format(Locale.US, it)} sq ft",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                lengthFt = preset.lengthFt
                                widthFt = preset.widthFt
                                wallHeightFt = preset.heightFt
                                wall1Ft = preset.lengthFt
                                wall2Ft = preset.widthFt
                                wall3Ft = preset.lengthFt
                                wall4Ft = preset.widthFt
                                doorCount = preset.doors
                                windowCount = preset.windows
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preset.label)
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "1. Room Shell",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "This is a ___ x ___ room",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                label = "Length (ft)",
                                value = lengthFt,
                                onValueChange = { lengthFt = it },
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                label = "Width (ft)",
                                value = widthFt,
                                onValueChange = { widthFt = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberField(
                            label = "Wall Height (ft)",
                            value = wallHeightFt,
                            onValueChange = { wallHeightFt = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edit walls?")
                            Switch(
                                checked = editWalls,
                                onCheckedChange = { editWalls = it }
                            )
                        }
                        if (editWalls) {
                            Spacer(modifier = Modifier.height(6.dp))
                            NumberField("Wall 1 Length (ft)", wall1Ft, { wall1Ft = it }, Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            NumberField("Wall 2 Length (ft)", wall2Ft, { wall2Ft = it }, Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            NumberField("Wall 3 Length (ft)", wall3Ft, { wall3Ft = it }, Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            NumberField("Wall 4 Length (ft)", wall4Ft, { wall4Ft = it }, Modifier.fillMaxWidth())
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "2. Doors",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "With __ door(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberField("Door Count", doorCount, { doorCount = it }, Modifier.fillMaxWidth(), isInt = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                label = "Door Width (ft)",
                                value = doorWidthFt,
                                onValueChange = { doorWidthFt = it },
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                label = "Door Height (ft)",
                                value = doorHeightFt,
                                onValueChange = { doorHeightFt = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edit doors?")
                            Switch(
                                checked = editDoors,
                                onCheckedChange = { editDoors = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Optional extended closet door")
                            Switch(
                                checked = includeExtendedClosetDoor,
                                onCheckedChange = { includeExtendedClosetDoor = it }
                            )
                        }
                        if (includeExtendedClosetDoor) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField(
                                    label = "Closet Door Width (ft)",
                                    value = closetDoorWidthFt,
                                    onValueChange = { closetDoorWidthFt = it },
                                    modifier = Modifier.weight(1f)
                                )
                                NumberField(
                                    label = "Closet Door Height (ft)",
                                    value = closetDoorHeightFt,
                                    onValueChange = { closetDoorHeightFt = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (editDoors) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Additional custom-sized doors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            customDoors.forEachIndexed { index, draft ->
                                Spacer(modifier = Modifier.height(6.dp))
                                OpeningDraftRow(
                                    title = "Door ${index + 1}",
                                    draft = draft,
                                    onChange = { customDoors[index] = it },
                                    onDelete = { if (customDoors.size > 1) customDoors.removeAt(index) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { customDoors.add(OpeningDraft()) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Custom Door")
                            }
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "3. Windows",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "And __ window(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberField("Window Count", windowCount, { windowCount = it }, Modifier.fillMaxWidth(), isInt = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                label = "Window Width (ft)",
                                value = windowWidthFt,
                                onValueChange = { windowWidthFt = it },
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                label = "Window Height (ft)",
                                value = windowHeightFt,
                                onValueChange = { windowHeightFt = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edit windows? (multi-sized)")
                            Switch(
                                checked = editWindows,
                                onCheckedChange = { editWindows = it }
                            )
                        }
                        if (editWindows) {
                            customWindows.forEachIndexed { index, draft ->
                                Spacer(modifier = Modifier.height(6.dp))
                                OpeningDraftRow(
                                    title = "Window ${index + 1}",
                                    draft = draft,
                                    onChange = { customWindows[index] = it },
                                    onDelete = { if (customWindows.size > 1) customWindows.removeAt(index) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { customWindows.add(OpeningDraft()) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Custom Window")
                            }
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "4. Finish",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Optional ceiling?")
                            Switch(
                                checked = includeCeiling,
                                onCheckedChange = { includeCeiling = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continue to next room after save?")
                            Switch(
                                checked = continueToNextRoom,
                                onCheckedChange = { continueToNextRoom = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Turn on 'Continue' to chain multiple rooms quickly.",
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

                    val normalizedName = roomName.trim().ifBlank { "Room" }
                    val length = Validators.parsePositiveDouble(lengthFt)
                    val width = Validators.parsePositiveDouble(widthFt)
                    val height = Validators.parsePositiveDouble(wallHeightFt)
                    if (length == null || width == null || height == null) {
                        validationError = "Room length, width, and wall height must be positive numbers."
                        return@Button
                    }

                    val wallLengths = if (editWalls) {
                        listOf(wall1Ft, wall2Ft, wall3Ft, wall4Ft).mapIndexed { index, raw ->
                            Validators.parsePositiveDouble(raw) ?: run {
                                validationError = "Wall ${index + 1} length must be a positive number."
                                return@Button
                            }
                        }
                    } else {
                        listOf(length, width, length, width)
                    }

                    val openingsByWall = MutableList(4) { mutableListOf<Opening>() }

                    val parsedDoorCount = doorCount.toIntOrNull()?.coerceAtLeast(0) ?: run {
                        validationError = "Door count must be 0 or a positive integer."
                        return@Button
                    }
                    val parsedDoorWidth = Validators.parsePositiveDouble(doorWidthFt) ?: run {
                        validationError = "Door width must be a positive number."
                        return@Button
                    }
                    val parsedDoorHeight = Validators.parsePositiveDouble(doorHeightFt) ?: run {
                        validationError = "Door height must be a positive number."
                        return@Button
                    }
                    if (parsedDoorCount > 0) {
                        openingsByWall[0].add(
                            Opening(
                                width = Millimeters.fromFeet(parsedDoorWidth),
                                height = Millimeters.fromFeet(parsedDoorHeight),
                                count = parsedDoorCount
                            )
                        )
                    }

                    if (includeExtendedClosetDoor) {
                        val closetWidth = Validators.parsePositiveDouble(closetDoorWidthFt) ?: run {
                            validationError = "Closet door width must be positive."
                            return@Button
                        }
                        val closetHeight = Validators.parsePositiveDouble(closetDoorHeightFt) ?: run {
                            validationError = "Closet door height must be positive."
                            return@Button
                        }
                        openingsByWall[0].add(
                            Opening(
                                width = Millimeters.fromFeet(closetWidth),
                                height = Millimeters.fromFeet(closetHeight),
                                count = 1
                            )
                        )
                    }

                    if (editDoors) {
                        customDoors.forEachIndexed { index, draft ->
                            if (draft.widthFt.isBlank() && draft.heightFt.isBlank() && draft.count.isBlank()) {
                                return@forEachIndexed
                            }
                            val w = Validators.parsePositiveDouble(draft.widthFt) ?: run {
                                validationError = "Custom door ${index + 1} width is invalid."
                                return@Button
                            }
                            val h = Validators.parsePositiveDouble(draft.heightFt) ?: run {
                                validationError = "Custom door ${index + 1} height is invalid."
                                return@Button
                            }
                            val c = Validators.parsePositiveInt(draft.count) ?: run {
                                validationError = "Custom door ${index + 1} count is invalid."
                                return@Button
                            }
                            openingsByWall[0].add(
                                Opening(
                                    width = Millimeters.fromFeet(w),
                                    height = Millimeters.fromFeet(h),
                                    count = c
                                )
                            )
                        }
                    }

                    val parsedWindowCount = windowCount.toIntOrNull()?.coerceAtLeast(0) ?: run {
                        validationError = "Window count must be 0 or a positive integer."
                        return@Button
                    }
                    val parsedWindowWidth = Validators.parsePositiveDouble(windowWidthFt) ?: run {
                        validationError = "Window width must be a positive number."
                        return@Button
                    }
                    val parsedWindowHeight = Validators.parsePositiveDouble(windowHeightFt) ?: run {
                        validationError = "Window height must be a positive number."
                        return@Button
                    }

                    val windowOpenings = mutableListOf<Opening>()
                    if (parsedWindowCount > 0) {
                        windowOpenings += Opening(
                            width = Millimeters.fromFeet(parsedWindowWidth),
                            height = Millimeters.fromFeet(parsedWindowHeight),
                            count = parsedWindowCount
                        )
                    }

                    if (editWindows) {
                        customWindows.forEachIndexed { index, draft ->
                            if (draft.widthFt.isBlank() && draft.heightFt.isBlank() && draft.count.isBlank()) {
                                return@forEachIndexed
                            }
                            val w = Validators.parsePositiveDouble(draft.widthFt) ?: run {
                                validationError = "Custom window ${index + 1} width is invalid."
                                return@Button
                            }
                            val h = Validators.parsePositiveDouble(draft.heightFt) ?: run {
                                validationError = "Custom window ${index + 1} height is invalid."
                                return@Button
                            }
                            val c = Validators.parsePositiveInt(draft.count) ?: run {
                                validationError = "Custom window ${index + 1} count is invalid."
                                return@Button
                            }
                            windowOpenings += Opening(
                                width = Millimeters.fromFeet(w),
                                height = Millimeters.fromFeet(h),
                                count = c
                            )
                        }
                    }

                    distributeWindowsAcrossWalls(windowOpenings).forEachIndexed { wallIndex, wallOpenings ->
                        openingsByWall[wallIndex].addAll(wallOpenings)
                    }

                    val spaces = mutableListOf<Space>()
                    wallLengths.forEachIndexed { index, wallLength ->
                        spaces += Space(
                            id = UUID.randomUUID().toString(),
                            name = "$normalizedName Wall ${index + 1}",
                            geometry = Geometry.Wall(
                                length = Millimeters.fromFeet(wallLength),
                                height = Millimeters.fromFeet(height)
                            ),
                            openings = mergeOpenings(openingsByWall[index]),
                            transform = SpaceTransform()
                        )
                    }

                    if (includeCeiling) {
                        spaces += Space(
                            id = UUID.randomUUID().toString(),
                            name = "$normalizedName Ceiling",
                            geometry = Geometry.Rect(
                                length = Millimeters.fromFeet(length),
                                width = Millimeters.fromFeet(width)
                            ),
                            transform = SpaceTransform()
                        )
                    }

                    onSave(spaces, continueToNextRoom)
                },
                enabled = roomName.isNotBlank()
            ) {
                Text("Create Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OpeningDraftRow(
    title: String,
    draft: OpeningDraft,
    onChange: (OpeningDraft) -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "Width (ft)",
                    value = draft.widthFt,
                    onValueChange = { onChange(draft.copy(widthFt = it)) },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "Height (ft)",
                    value = draft.heightFt,
                    onValueChange = { onChange(draft.copy(heightFt = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            NumberField(
                label = "Count",
                value = draft.count,
                onValueChange = { onChange(draft.copy(count = it)) },
                modifier = Modifier.fillMaxWidth(),
                isInt = true
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    isInt: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = modifier
    )
}


private fun distributeWindowsAcrossWalls(windowOpenings: List<Opening>): List<List<Opening>> {
    val walls = MutableList(4) { mutableListOf<Opening>() }
    var wallCursor = 1
    windowOpenings.forEach { opening ->
        val count = opening.count.coerceAtLeast(1)
        repeat(count) {
            walls[wallCursor].add(opening.copy(count = 1))
            wallCursor++
            if (wallCursor > 3) wallCursor = 1
        }
    }
    return walls.map { mergeOpenings(it) }
}

private fun mergeOpenings(openings: List<Opening>): List<Opening> {
    return openings
        .groupBy { opening -> opening.width.value to opening.height.value }
        .map { (size, grouped) ->
            Opening(
                width = Millimeters(size.first),
                height = Millimeters(size.second),
                count = grouped.sumOf { it.count }
            )
        }
}

private data class RoomPreset(
    val label: String,
    val lengthFt: String,
    val widthFt: String,
    val heightFt: String,
    val doors: String,
    val windows: String
)
