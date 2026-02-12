package com.tradesketch.estimator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

@Composable
fun AnimatedEntry(
    visible: Boolean = true,
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    var render by remember(visible) { mutableStateOf(false) }
    LaunchedEffect(visible, delayMs) {
        if (visible) {
            if (delayMs > 0) delay(delayMs.toLong())
            render = true
        } else {
            render = false
        }
    }
    AnimatedVisibility(
        visible = render,
        enter = fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 280,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = androidx.compose.animation.core.tween(280, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 160)
        ) + slideOutVertically(
            targetOffsetY = { it / 10 },
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 160)
        )
    ) {
        content()
    }
}

class AppHaptics(private val haptic: HapticFeedback) {
    fun tap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { AppHaptics(haptic) }
}
