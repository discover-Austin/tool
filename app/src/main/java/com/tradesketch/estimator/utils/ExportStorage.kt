package com.tradesketch.estimator.utils

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ShareIntentSpec(
    val mimeType: String,
    val subject: String,
    val text: String,
    val chooserTitle: String,
    val flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
)

internal object ExportStorage {
    fun buildFileName(
        projectName: String,
        suffix: String,
        extension: String
    ): String {
        val cleanName = sanitizeProjectName(projectName)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val entropy = (System.nanoTime() and 0xFFFL).toString(16).padStart(3, '0')
        return "${cleanName}_${suffix}_${stamp}_$entropy.$extension"
    }

    fun createShareIntent(
        context: Context,
        shareFile: File,
        mimeType: String,
        subject: String,
        text: String,
        chooserTitle: String
    ): ExportResult<Intent> {
        if (!shareFile.exists() || shareFile.length() <= 0L) {
            return ExportResult.Failure(
                userMessage = "Could not prepare ${shareFile.name} because the shared file was empty."
            )
        }
        val spec = buildShareIntentSpec(
            mimeType = mimeType,
            subject = subject,
            text = text,
            chooserTitle = chooserTitle
        )
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = spec.mimeType
                putExtra(Intent.EXTRA_SUBJECT, spec.subject)
                putExtra(Intent.EXTRA_TEXT, spec.text)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, spec.subject, uri)
                addFlags(spec.flags)
            }
            Intent.createChooser(shareIntent, spec.chooserTitle).apply {
                addFlags(spec.flags)
            }
        }.fold(
            onSuccess = { intent ->
                ExportResult.Success(
                    value = intent,
                    userMessage = ExportDestination.SHARE_CACHE.saveSuccessMessage(shareFile.name)
                )
            },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not prepare ${shareFile.name} for sharing.",
                    cause = error
                )
            }
        )
    }

    internal fun buildShareIntentSpec(
        mimeType: String,
        subject: String,
        text: String,
        chooserTitle: String
    ): ShareIntentSpec {
        return ShareIntentSpec(
            mimeType = mimeType,
            subject = subject,
            text = text,
            chooserTitle = chooserTitle
        )
    }

    suspend fun writeBytesToDocument(
        context: Context,
        uri: Uri,
        bytes: ByteArray,
        fileName: String,
        exportLabel: String
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) {
            return@withContext ExportResult.Failure(
                userMessage = "Could not save $exportLabel because no data was generated."
            )
        }
        runCatching {
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open output stream for $fileName")
            output.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
            SavedExport(
                uri = uri,
                fileName = fileName,
                destination = ExportDestination.USER_SELECTED
            )
        }.fold(
            onSuccess = { saved ->
                ExportResult.Success(
                    value = saved,
                    userMessage = saved.destination.saveSuccessMessage(saved.fileName)
                )
            },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not save $exportLabel to the selected location.",
                    cause = error
                )
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveBytesToPublicDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) {
            return@withContext ExportResult.Failure(
                userMessage = "Could not save $fileName because the export was empty."
            )
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext ExportResult.Failure(
                userMessage = "Could not create a file in Downloads/TradeSketch."
            )
        try {
            val output = resolver.openOutputStream(uri)
                ?: error("Failed to open output stream for $fileName")
            output.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            val saved = SavedExport(
                uri = uri,
                fileName = fileName,
                destination = ExportDestination.PUBLIC_DOWNLOADS
            )
            ExportResult.Success(
                value = saved,
                userMessage = saved.destination.saveSuccessMessage(saved.fileName)
            )
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            ExportResult.Failure(
                userMessage = "Could not save $fileName to Downloads/TradeSketch.",
                cause = error
            )
        }
    }

    suspend fun saveBytesToAppDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) {
            return@withContext ExportResult.Failure(
                userMessage = "Could not save $fileName because the export was empty."
            )
        }
        runCatching {
            val downloadsDir = resolveAppScopedExportDir(context)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(bytes)
                output.flush()
            }
            if (file.length() <= 0L) {
                error("$fileName was written as an empty file")
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            SavedExport(
                uri = uri,
                fileName = fileName,
                destination = ExportDestination.APP_STORAGE
            )
        }.fold(
            onSuccess = { saved ->
                ExportResult.Success(
                    value = saved,
                    userMessage = saved.destination.saveSuccessMessage(saved.fileName)
                )
            },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not save $fileName to TradeSketch app storage.",
                    cause = error
                )
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveBitmapToPublicDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext ExportResult.Failure(
                userMessage = "Could not create a PNG in Downloads/TradeSketch."
            )
        try {
            val output = resolver.openOutputStream(uri)
                ?: error("Failed to open image output stream for $fileName")
            val wroteImage = output.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream).also { stream.flush() }
            }
            if (!wroteImage) {
                error("PNG compression failed for $fileName")
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            val saved = SavedExport(
                uri = uri,
                fileName = fileName,
                destination = ExportDestination.PUBLIC_DOWNLOADS
            )
            ExportResult.Success(
                value = saved,
                userMessage = saved.destination.saveSuccessMessage(saved.fileName)
            )
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            ExportResult.Failure(
                userMessage = "Could not save $fileName to Downloads/TradeSketch.",
                cause = error
            )
        }
    }

    suspend fun saveBitmapToAppDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadsDir = resolveAppScopedExportDir(context)
            val file = File(downloadsDir, fileName)
            val wroteImage = FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output).also { output.flush() }
            }
            if (!wroteImage || file.length() <= 0L) {
                error("PNG compression failed for $fileName")
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            SavedExport(
                uri = uri,
                fileName = fileName,
                destination = ExportDestination.APP_STORAGE
            )
        }.fold(
            onSuccess = { saved ->
                ExportResult.Success(
                    value = saved,
                    userMessage = saved.destination.saveSuccessMessage(saved.fileName)
                )
            },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not save $fileName to TradeSketch app storage.",
                    cause = error
                )
            }
        )
    }

    private fun sanitizeProjectName(projectName: String): String {
        return projectName
            .trim()
            .ifBlank { "project" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .take(40)
            .ifBlank { "project" }
    }

    private fun resolveAppScopedExportDir(context: Context): File {
        val externalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = externalDownloads ?: File(context.filesDir, "exports")
        val exportDir = File(baseDir, "TradeSketch")
        if (exportDir.exists() || exportDir.mkdirs()) {
            return exportDir
        }
        error("Could not create the TradeSketch export directory.")
    }
}
