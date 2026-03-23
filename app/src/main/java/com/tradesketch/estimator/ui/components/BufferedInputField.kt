package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            text = value
        },
        label = { Text(label) },
        placeholder = { Text(hint) },
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
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = appOutlinedTextFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val wasFocused = isFocused
                isFocused = focusState.isFocused
                if (wasFocused && !focusState.isFocused && text != lastCommittedValue) {
                    latestOnValueChange(text)
                    lastCommittedValue = text
                }
            }
    )
}
