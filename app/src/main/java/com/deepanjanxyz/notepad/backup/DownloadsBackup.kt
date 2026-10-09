package com.deepanjanxyz.notepad.backup

import android.content.ContentResolver
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
    // Suffixes of the internal working files a write goes through. They are not
    // backups in their own right, and are kept out of the Restore list whenever
    // the real backup is present.
    private const val PARTIAL_SUFFIX = ".partial.json"
    private const val PREVIOUS_SUFFIX = ".previous.json"

    /** True when writing needs the legacy WRITE_EXTERNAL_STORAGE runtime permission. */
    val needsLegacyPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Location of a backup file, phrased for display in Settings. */
    fun locationLabel(name: String = FILE_NAME): String = "Downloads/$FOLDER_NAME/$name"

    /** Where the backup currently is: its location and when it was last written. */
    data class Info(val location: String, val lastModified: Long)

    /** One backup file found in the folder. */
    data class Entry(val name: String, val lastModified: Long)

    /**
     * Writes [json] to a backup file, replacing any existing file of the same name.
     *
     * The new content is written to a temporary file and read back before the
     * previous backup is touched, so a failed or interrupted write can never
     * leave the user without their last good backup: the old file is only
     * replaced once the new one is confirmed on disk.
     */
    fun write(context: Context, json: String, name: String = FILE_NAME) {
        // Deliberate recovery policy, not an oversight: when there is nothing
        // worth storing - every note deleted, say - the file already on disk is
        // left exactly as it is. Replacing a backup that still holds yesterday's
        // notes with one that holds nothing would turn a recoverable mistake
        // into an unrecoverable one.
        if (json.isBlank()) return
        val tempName = partialName(name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, json, tempName, name)
        } else {
            writeViaFile(json, tempName, name)
        }
    }

    /**
     * Name of the temporary file a write goes through first.
     *
     * It deliberately keeps the `.json` suffix: if a write is interrupted between
     * the previous file being dropped and the temporary one being renamed, the
     * verified copy is still returned by [list] and can be restored by hand.
     */
    private fun partialName(name: String): String =
        if (name.endsWith(JSON_SUFFIX)) {
            name.removeSuffix(JSON_SUFFIX) + PARTIAL_SUFFIX
        } else {
            "$name.partial"
        }

    /**
     * Name the previous backup is parked under while the new one is moved into
     * place. Like the partial name, it keeps the `.json` suffix so an interrupted
     * swap still leaves a restorable file.
     */
    private fun previousName(name: String): String =
        if (name.endsWith(JSON_SUFFIX)) {
            name.removeSuffix(JSON_SUFFIX) + PREVIOUS_SUFFIX
        } else {
            "$name.previous"
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

    /**
     * Every backup file in the folder, newest first.
     *
     * The `.partial` / `.previous` files a write goes through are internal, not
     * backups in their own right. While the real backup is present they are left
     * out, so the folder and the Restore list show the user a single backup
     * rather than a backup of a backup. They appear only when the real backup is
     * missing, which is exactly when one of them is the copy worth recovering.
     */
    fun list(context: Context): List<Entry> {
        val entries = readEntries(context)
        val hasRealBackup = entries.any { it.name == FILE_NAME }
        return entries
            .filter { !(hasRealBackup && isInternal(it.name)) }
            .sortedByDescending { it.lastModified }
    }

    private fun readEntries(context: Context): List<Entry> {
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
            return entries
        }
        return legacyDir()
            .listFiles { file -> file.isFile && file.name.endsWith(JSON_SUFFIX) }
            ?.map { Entry(it.name, it.lastModified()) }
            .orEmpty()
    }

    /** True for the internal working files a write goes through. */
    private fun isInternal(name: String): Boolean =
        name.endsWith(PARTIAL_SUFFIX) || name.endsWith(PREVIOUS_SUFFIX)

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
    private fun writeViaMediaStore(
        context: Context,
        json: String,
        tempName: String,
        finalName: String
    ) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        // Clear the working files left behind by an interrupted earlier write.
        // The .previous copy is only worth keeping while the real backup is
        // missing, so once it is back the leftover is just clutter.
        resolver.delete(collection, selection(), selectionArgs(tempName))
        if (findFile(resolver, collection, finalName) != null) {
            resolver.delete(collection, selection(), selectionArgs(previousName(finalName)))
        }

        val uri = insertPendingFile(resolver, collection, tempName)
            ?: error("Could not create the backup file in Downloads")

        resolver.openOutputStream(uri)?.use { output ->
            output.write(json.toByteArray())
        } ?: error("Could not open the backup file for writing")

        val published = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(uri, published, null, null)

        // Read the new file back before the previous backup is touched: if the
        // bytes do not match, the old backup is left exactly as it was.
        val written = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        if (written != json) {
            resolver.delete(uri, null, null)
            error("The backup could not be verified after writing")
        }

        // The new copy is confirmed on disk, but the previous backup is still not
        // deleted: a rename can fail on some builds or under scoped-storage
        // restrictions, and deleting first would then leave nothing but the
        // .partial file. Instead the old file is parked under a third name, the
        // new one is promoted, and only once that promotion is confirmed is the
        // old copy removed. If the promotion fails, the old file is put back
        // exactly where it was.
        val parkedUri = findFile(resolver, collection, finalName)
        if (parkedUri != null && !rename(resolver, parkedUri, previousName(finalName))) {
            // The old backup could not be moved aside, so nothing is touched and
            // the verified new copy is kept under its temporary name.
            error("Could not move the previous backup aside")
        }

        if (!rename(resolver, uri, finalName)) {
            // Put the previous backup back under its real name; the verified new
            // copy stays on disk as the .partial file, so neither is lost.
            if (parkedUri != null) rename(resolver, parkedUri, finalName)
            error("Could not replace the previous backup")
        }

        resolver.delete(collection, selection(), selectionArgs(previousName(finalName)))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertPendingFile(
        resolver: ContentResolver,
        collection: Uri,
        name: String
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return resolver.insert(collection, values)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findFile(resolver: ContentResolver, collection: Uri, name: String): Uri? =
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection(),
            selectionArgs(name),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                ContentUris.withAppendedId(collection, cursor.getLong(0))
            } else {
                null
            }
        }

    /**
     * Renames a file and confirms the new name actually took effect. A silent
     * no-op update is exactly the case that would otherwise lose the backup, so
     * the result is read back rather than assumed.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun rename(resolver: ContentResolver, uri: Uri, name: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }
        resolver.update(uri, values, null, null)
        return resolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == name
        } ?: false
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

    /**
     * Writes through a temporary file and then renames it over the target.
     *
     * The rename replaces the destination in a single step on the same
     * filesystem, so the previous backup is never absent while the new one is
     * being put in place.
     */
    private fun writeViaFile(json: String, tempName: String, finalName: String) {
        val temp = legacyFile(tempName)
        temp.parentFile?.mkdirs()
        temp.writeText(json)
        if (temp.readText() != json) {
            temp.delete()
            error("The backup could not be verified after writing")
        }
        if (!temp.renameTo(legacyFile(finalName))) {
            temp.delete()
            error("Could not replace the previous backup")
        }
    }
}
