package com.tradesketch.estimator.utils

import android.net.Uri
import android.os.Build

internal enum class ExportDestination(
    val destinationLabel: String
) {
    PUBLIC_DOWNLOADS("Downloads/TradeSketch"),
    APP_STORAGE("TradeSketch app storage"),
    USER_SELECTED("the selected location"),
    SHARE_CACHE("TradeSketch share cache")
}

internal data class SavedExport(
    val uri: Uri,
    val fileName: String,
    val destination: ExportDestination
) {
    val destinationLabel: String
        get() = destination.destinationLabel
}

internal sealed interface ExportResult<out T> {
    data class Success<T>(
        val value: T,
        val userMessage: String? = null
    ) : ExportResult<T>

    data class Failure(
        val userMessage: String,
        val cause: Throwable? = null
    ) : ExportResult<Nothing>
}

internal fun defaultDeviceSaveHint(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "On Android 10 and newer, Save to device writes to Downloads/TradeSketch."
    } else {
        "On Android 9 and lower, Save to device writes to TradeSketch app storage. Use Save As to choose a public folder."
    }
}

internal fun ExportDestination.saveSuccessMessage(fileName: String): String {
    return when (this) {
        ExportDestination.PUBLIC_DOWNLOADS -> "Saved $fileName to Downloads/TradeSketch."
        ExportDestination.APP_STORAGE -> "Saved $fileName to TradeSketch app storage. Use Save As to place it in a public folder."
        ExportDestination.USER_SELECTED -> "Saved $fileName to the selected location."
        ExportDestination.SHARE_CACHE -> "Prepared $fileName for sharing."
    }
}
