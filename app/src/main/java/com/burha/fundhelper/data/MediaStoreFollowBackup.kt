package com.burha.fundhelper.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MediaStoreFollowBackup(
    private val context: Context,
) : FollowBackup {

    override suspend fun writeCodes(codes: List<String>) = withContext(Dispatchers.IO) {
        val body = FollowBackupCodec.encode(codes)
        val bytes = body.toByteArray(Charsets.UTF_8)
        val existing = findReadableUri()
        if (existing != null && writeToUri(existing, bytes)) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = insertDownload() ?: throw IOException("MediaStore insert failed")
            if (!writeToUri(uri, bytes)) throw IOException("MediaStore write failed")
            return@withContext
        }
        val file = legacyFile()
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    override suspend fun readCodes(): List<String> = withContext(Dispatchers.IO) {
        val body = readBody() ?: return@withContext emptyList()
        FollowBackupCodec.decode(body)
    }

    private fun readBody(): String? {
        findReadableUri()?.let { uri ->
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { return it.readText() }
        }
        val file = legacyFile()
        if (file.isFile) return file.readText()
        return null
    }

    private fun findReadableUri(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(DISPLAY_NAME)
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun insertDownload(): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, DISPLAY_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        return context.contentResolver.insert(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        )
    }

    private fun writeToUri(uri: Uri, bytes: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun legacyFile(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DISPLAY_NAME)

    companion object {
        const val DISPLAY_NAME = "com.burha.fundhelper-follows.json"
        private const val MIME_TYPE = "application/json"
    }
}
