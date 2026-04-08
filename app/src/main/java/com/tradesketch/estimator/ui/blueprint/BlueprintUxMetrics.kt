package com.tradesketch.estimator.ui.blueprint

internal enum class BlueprintControlMode(val metricSuffix: String) {
    JOYSTICK("joystick")
}

internal enum class BlueprintMetricAction {
    SCREEN_OPENED,
    WALL_PLACED,
    BOX_PLACED,
    OPENING_PLACED,
    CLEAR_ALL
}

internal fun blueprintControlMode(): BlueprintControlMode {
    return BlueprintControlMode.JOYSTICK
}

internal fun blueprintMetricKey(
    action: BlueprintMetricAction,
    controlMode: BlueprintControlMode
): String {
    val actionPrefix = when (action) {
        BlueprintMetricAction.SCREEN_OPENED -> "blueprint_screen_opened"
        BlueprintMetricAction.WALL_PLACED -> "blueprint_wall_placed"
        BlueprintMetricAction.BOX_PLACED -> "blueprint_box_placed"
        BlueprintMetricAction.OPENING_PLACED -> "blueprint_opening_placed"
        BlueprintMetricAction.CLEAR_ALL -> "blueprint_clear_all"
    }
    return "${actionPrefix}_${controlMode.metricSuffix}"
}

internal fun blueprintScreenOpenedMetricKey(
    lastTrackedModeSuffix: String?,
    controlMode: BlueprintControlMode
): String? {
    return if (lastTrackedModeSuffix == controlMode.metricSuffix) {
        null
    } else {
        blueprintMetricKey(BlueprintMetricAction.SCREEN_OPENED, controlMode)
    }
}
