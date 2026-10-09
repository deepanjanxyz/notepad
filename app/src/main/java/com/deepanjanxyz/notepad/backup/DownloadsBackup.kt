package com.deepanjanxyz.notepad.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Reads and writes the app's backup file in the public Downloads folder.
 *
 * The public Downloads folder is used instead of app-private storage on purpose:
 * the file stays there after the app's data is cleared (or the app is
 * uninstalled), so the notes can be restored afterwards, and the user can open
 * the folder and see the file. A single file is kept by default and replaced on
 * every write, so backups never pile up.
 *
 * On Android 10+ the file is written through MediaStore, which needs no runtime
 * permission for files the app itself owns. On Android 9 and below it is written
 * to the public Downloads directory directly, which requires
 * WRITE_EXTERNAL_STORAGE at run time.
 */
object DownloadsBackup {

    /** Name of the single, always-overwritten backup file. */
    const val FILE_NAME = "elite-memo-backup.json"

    private const val MIME_TYPE = "application/json"
    private const val FOLDER_NAME = "EliteMemoPro"
    // Built from the framework constant, so it cannot be a compile-time const.
    private val RELATIVE_DIR = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"
    private const val JSON_SUFFIX = ".json"

    /** True when writing needs the legacy WRITE_EXTERNAL_STORAGE runtime permission. */
    val needsLegacyPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Location of a backup file, phrased for display in Settings. */
    fun locationLabel(name: String = FILE_NAME): String = "Downloads/$FOLDER_NAME/$name"

    /** Where the backup currently is: its location and when it was last written. */
    data class Info(val location: String, val lastModified: Long)

    /** One backup file found in the folder. */
    data class Entry(val name: String, val lastModified: Long)

    /** Writes [json] to a backup file, replacing any existing file of the same name. */
    fun write(context: Context, json: String, name: String = FILE_NAME) {
        // Never create an empty backup file: with nothing to store, leave the
        // file already on disk untouched.
        if (json.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, json, name)
        } else {
            legacyFile(name).apply {
                parentFile?.mkdirs()
                writeText(json)
            }
        }
    }

    /** Reads a backup file, or null when it does not exist. */
    fun read(context: Context, name: String = FILE_NAME): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            readViaMediaStore(context, name)
        } else {
            legacyFile(name).takeIf { it.exists() }?.readText()
        }

    /** Describes the default backup file, or null when it does not exist. */
    fun info(context: Context): Info? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
                selection(),
                selectionArgs(FILE_NAME),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // MediaStore stores DATE_MODIFIED in seconds.
                    return Info(locationLabel(), cursor.getLong(0) * 1000L)
                }
            }
            return null
        }
        val file = legacyFile(FILE_NAME)
        return if (file.exists()) Info(locationLabel(), file.lastModified()) else null
    }

    /** Every backup file in the folder, newest first. */
    fun list(context: Context): List<Entry> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val entries = mutableListOf<Entry>()
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED),
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf("$RELATIVE_DIR/"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    if (!name.endsWith(JSON_SUFFIX)) continue
                    entries += Entry(name, cursor.getLong(1) * 1000L)
                }
            }
            return entries.sortedByDescending { it.lastModified }
        }
        return legacyDir()
            .listFiles { file -> file.isFile && file.name.endsWith(JSON_SUFFIX) }
            ?.map { Entry(it.name, it.lastModified()) }
            ?.sortedByDescending { it.lastModified }
            .orEmpty()
    }

    /** Removes a backup file. Returns true when a file was actually removed. */
    fun delete(context: Context, name: String = FILE_NAME): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                selection(),
                selectionArgs(name)
            ) > 0
        }
        return legacyFile(name).delete()
    }

    // --- Android 10+ (MediaStore) -------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(context: Context, json: String, name: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        // Replace any previous copy so the same name never accumulates files.
        resolver.delete(collection, selection(), selectionArgs(name))

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: error("Could not create the backup file in Downloads")

        resolver.openOutputStream(uri)?.use { output ->
            output.write(json.toByteArray())
        } ?: error("Could not open the backup file for writing")

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readViaMediaStore(context: Context, name: String): String? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection(),
            selectionArgs(name),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val uri: Uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
                return resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun selection(): String =
        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun selectionArgs(name: String): Array<String> = arrayOf(name, "$RELATIVE_DIR/")

    // --- Android 9 and below (direct file access) --------------------------------

    @Suppress("DEPRECATION")
    private fun legacyDir(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FOLDER_NAME
        )

    private fun legacyFile(name: String): File = File(legacyDir(), name)
}
