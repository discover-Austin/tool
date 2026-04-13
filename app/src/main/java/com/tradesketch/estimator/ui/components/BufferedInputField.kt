package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val INPUT_EXTERNAL_SYNC_DELAY_MS = 320L

@Composable
fun BufferedInputField(
    label: String,
    initialValue: String,
    hint: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    var text by rememberSaveable(label) { mutableStateOf(initialValue) }
    var isFocused by rememberSaveable(label) { mutableStateOf(false) }
    var lastCommittedValue by rememberSaveable(label) { mutableStateOf(initialValue) }
    var lastObservedExternalValue by rememberSaveable(label) { mutableStateOf(initialValue) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(initialValue) {
        lastObservedExternalValue = initialValue
        if (!isFocused && initialValue != text) {
            text = initialValue
            lastCommittedValue = initialValue
        }
    }
    LaunchedEffect(isFocused, lastObservedExternalValue, text) {
        if (isFocused || text == lastObservedExternalValue) return@LaunchedEffect
        delay(INPUT_EXTERNAL_SYNC_DELAY_MS)
        if (isFocused || text == lastObservedExternalValue) return@LaunchedEffect
        text = lastObservedExternalValue
        lastCommittedValue = lastObservedExternalValue
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val wasFocused = isFocused
                isFocused = focusState.isFocused
                if (wasFocused && !focusState.isFocused && text != lastCommittedValue) {
                    latestOnValueChange(text)
                    lastCommittedValue = text
                }
            },
        color = ReferenceBlueprintPaperAlt.copy(alpha = 0.9f),
        border = BorderStroke(width = 1.1.dp, color = ReferenceBlueprintBorder.copy(alpha = 0.86f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ReferenceBlueprintInk
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted
                )
            }
            Surface(
                modifier = Modifier.width(116.dp),
                color = Color(0xFFFFFEF8),
                border = BorderStroke(
                    width = if (isFocused) 1.25.dp else 1.dp,
                    color = if (isFocused) {
                        ReferenceBlueprintGoldBorder
                    } else {
                        ReferenceBlueprintBorder.copy(alpha = 0.78f)
                    }
                ),
                shadowElevation = if (isFocused) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isBlank()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = ReferenceBlueprintMuted.copy(alpha = 0.62f)
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { value -> text = value },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ReferenceBlueprintInk
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (text != lastCommittedValue) {
                                    latestOnValueChange(text)
                                    lastCommittedValue = text
                                }
                            }
                        ),
                        cursorBrush = SolidColor(ReferenceBlueprintNavy),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
