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

internal data class ShareIntentSpec(
    val mimeType: String,
    val subject: String,
    val text: String,
    val chooserTitle: String,
    val flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
)

object ExportStorage {
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
    ): Intent {
        val spec = buildShareIntentSpec(
            mimeType = mimeType,
            subject = subject,
            text = text,
            chooserTitle = chooserTitle
        )
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
            // Some OEM share targets require clip data grants in addition to EXTRA_STREAM.
            clipData = ClipData.newRawUri(spec.subject, uri)
            addFlags(spec.flags)
        }
        return Intent.createChooser(
            shareIntent,
            spec.chooserTitle
        ).apply {
            addFlags(spec.flags)
        }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBytesToPublicDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val wroteBytes = resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
                true
            } ?: false
            if (!wroteBytes) throw IllegalStateException("Failed to open output stream for byte export")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun saveBytesToAppDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String
    ): Uri? {
        return try {
            val downloadsDir = resolveAppScopedExportDir(context)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(bytes)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBitmapToPublicDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val wroteImage = resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: false
            if (!wroteImage) throw IllegalStateException("Failed to open output stream for image export")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun saveBitmapToAppDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {
        return try {
            val downloadsDir = resolveAppScopedExportDir(context)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
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
        return File(baseDir, "TradeSketch").apply { mkdirs() }
    }
}
