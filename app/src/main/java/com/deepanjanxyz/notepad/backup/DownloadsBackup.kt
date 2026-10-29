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
import java.io.IOException

private const val MILLIS_PER_SECOND = 1000L


/**
 * Reads and writes the app's backup file in the public Downloads folder.
 *
 * The public Downloads folder is used instead of app-private storage on purpose:
 * the file stays there after the app's data is cleared (or the app is
 * uninstalled), so the notes can be restored afterwards, and the user can open
 * the folder and see the file.
 *
 * One file is kept, and every write replaces its contents. On Android 10 and
 * above that file is written through the media store, which needs no runtime
 * permission for files the app itself owns. On Android 9 and below it is written
 * to the public Downloads directory directly, which requires
 * WRITE_EXTERNAL_STORAGE at run time.
 *
 * A file written by an earlier installation is not owned by this one, so the
 * media store will not let the app replace it and saves each new backup beside
 * it under a numbered name instead. Once the user hands the folder over (see
 * [BackupFolder]) the folder is used, which can read the old file, replace it
 * and clear the copies away.
 *
 * `TooManyFunctions` is suppressed here rather than the object split, for the same
 * reason as in [BackupFolder]: these functions are the whole of writing, finding,
 * reading and removing the backup file, they are short, and each is called from
 * somewhere. Moving them apart would be a rearrangement, not a fix.
 */
@Suppress("TooManyFunctions")
object DownloadsBackup {

    /** Name of the single, always-replaced backup file. */
    const val FILE_NAME: String = BackupFileNames.FILE_NAME

    private const val MIME_TYPE = "application/json"
    // Not a backup, and not a .json, so the entry used to bring the folder into
    // being never shows up in the backup lists, even for the moment it exists.
    private const val FOLDER_MARKER = ".elite-memo-folder"
    private const val FOLDER_NAME = BackupFolder.FOLDER_NAME
    // Built from the framework constant, so it cannot be a compile-time const.
    private val RELATIVE_DIR = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"

    /** True when writing needs the legacy WRITE_EXTERNAL_STORAGE runtime permission. */
    val needsLegacyPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** True when the user has handed the backup folder to the app. */
    fun hasFolderAccess(context: Context): Boolean = BackupFolder.hasAccess(context)

    /**
     * True when the folder has not been granted and copies this app cannot touch
     * are already sitting in it - the state a reinstall leaves behind, where every
     * write would add another file. Settings offers the grant then.
     */
    fun needsFolderAccess(context: Context): Boolean =
        !BackupFolder.hasAccess(context) && readEntries(context).any {
            it.name != FILE_NAME && BackupFileNames.isOurBackupName(it.name)
        }

    /**
     * Makes sure the folder the backup is kept in exists.
     *
     * The folder comes into being with the first file put in it, so an install
     * that has never backed up has nowhere for the file to go - and nowhere for
     * the folder picker to open at either. Calling this when automatic backup is
     * switched on puts the folder there at that point, and calling it before a
     * write puts it back if the user has removed it since.
     */
    fun ensureFolder(context: Context) {
        if (BackupFolder.hasAccess(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ensureMediaStoreFolder(context)
        } else {
            legacyDir().mkdirs()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureMediaStoreFolder(context: Context) {
        if (readEntries(context).isNotEmpty()) return
        // The media store makes the folder as a side effect of a file being put
        // in it, and has no way to make one on its own, so an entry is placed and
        // taken away again.
        val resolver = context.contentResolver
        val uri = insertPendingFile(resolver, MediaStore.Downloads.EXTERNAL_CONTENT_URI, FOLDER_MARKER)
            ?: return
        runCatching { resolver.delete(uri, null, null) }
    }

    /** Location of a backup file, phrased for display in Settings. */
    fun locationLabel(name: String = FILE_NAME): String = "Downloads/$FOLDER_NAME/$name"

    /** Where the backup is, when it was last written, and the file it is in. */
    data class Info(val location: String, val lastModified: Long, val name: String = FILE_NAME)

    /** One backup file found in the folder. */
    data class Entry(val name: String, val lastModified: Long)

    /**
     * Writes [json] to the backup file, replacing the contents of the file that
     * is already there.
     *
     * The new content is written over the existing file rather than saved under a
     * new name, and it is read back before the write is called a success, so a
     * failed or interrupted write cannot leave the user without their last good
     * backup.
     */
    fun write(context: Context, json: String, name: String = FILE_NAME) {
        // The folder is put back if it has been removed, so a write always has
        // somewhere to go.
        ensureFolder(context)
        // Deliberate recovery policy, not an oversight: when there is nothing
        // worth storing - every note deleted, say - the file already on disk is
        // left exactly as it is. Replacing a backup that still holds yesterday's
        // notes with one that holds nothing would turn a recoverable mistake
        // into an unrecoverable one.
        if (json.isBlank()) return
        // Clear out copies left by earlier versions before writing, so the write
        // cannot collide with one of them.
        cleanUpCopies(context)
        if (BackupFolder.hasAccess(context)) {
            writeViaFolder(context, json, name)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, json, name)
        } else {
            writeViaFile(json, name)
        }
    }

    /**
     * Writes through the folder the user granted: the file that is already there
     * is reused, and one is created only when the folder holds none at all.
     */
    private fun writeViaFolder(context: Context, json: String, name: String) {
        if (!BackupFolder.isReachable(context)) {
            // The folder the user granted has been removed or renamed. Forget the
            // grant so Settings asks for it again, and fail loudly rather than
            // write nothing and leave it looking as though a backup was made.
            BackupFolder.release(context)
            error("The backup folder could not be reached; allow access to it again")
        }
        val existing = BackupFolder.find(context, name) ?: newestFolderCopy(context, name)
        if (existing != null) {
            BackupFolder.write(context, existing.uri, json)
            return
        }
        val created = BackupFolder.create(context, name)
            ?: error("Could not create the backup file in Downloads")
        try {
            BackupFolder.write(context, created, json)
        } catch (e: IOException) {
            // Never leave a half-written file behind for the next write to collide with.
            BackupFolder.delete(context, created)
            throw e
        }
    }

    /** The newest copy of the backup file in the granted folder, or null. */
    private fun newestFolderCopy(context: Context, name: String): BackupFolder.Child? =
        BackupFolder.children(context)
            .filter { it.name != name && BackupFileNames.isOurBackupName(it.name) }
            .maxByOrNull { it.lastModified }

    /**
     * Removes the numbered copies of this app's backup files.
     *
     * Copies are only removed once the real backup is present. If it is missing,
     * one of those copies may be the only backup left, so it stays where the
     * Restore list can still offer it.
     *
     * A copy written by an earlier installation is not this app's to delete, and
     * the media store offers no way to ask: the consent requests it has cover
     * images, video and audio, and a Downloads file is none of those. Clearing
     * those copies is what the folder grant is for.
     */
    fun cleanUpCopies(context: Context) {
        if (BackupFolder.hasAccess(context)) {
            cleanUpFolderCopies(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cleanUpMediaStoreCopies(context)
        } else {
            cleanUpLegacyCopies()
        }
    }

    /**
     * Removes the copies in the granted folder. Nothing has to be asked for here:
     * the folder was handed over, so every file in it is the app's to delete.
     */
    private fun cleanUpFolderCopies(context: Context) {
        val children = BackupFolder.children(context)
        val surplus = BackupFileNames.copiesToRemove(children.map { it.name to it.lastModified }).toSet()
        children
            .filter { it.name in surplus }
            .forEach { BackupFolder.delete(context, it.uri) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cleanUpMediaStoreCopies(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val found = readEntries(context).map { it.name to it.lastModified }
        BackupFileNames.copiesToRemove(found).forEach { name ->
            val uri = findFile(resolver, collection, name) ?: return@forEach
            // A copy this app may not delete is left where it is; the folder
            // grant is what clears it.
            runCatching { resolver.delete(uri, null, null) }
        }
    }

    private fun cleanUpLegacyCopies() {
        val files = legacyDir().listFiles().orEmpty()
        if (files.any { it.name == FILE_NAME }) {
            files
                .filter { it.name != FILE_NAME && BackupFileNames.isOurBackupName(it.name) }
                .forEach { it.delete() }
        }
    }

    /**
     * Reads the newest backup, or the file under [name] when one is given.
     *
     * The default is the file [info] reports rather than [FILE_NAME], so a restore
     * never reads an older backup than the one Settings says was written last.
     */
    fun read(context: Context, name: String? = null): String? {
        val target = name ?: newestName(context) ?: FILE_NAME
        return if (BackupFolder.hasAccess(context)) {
            BackupFolder.find(context, target)?.let { BackupFolder.read(context, it.uri) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            readViaMediaStore(context, target)
        } else {
            legacyFile(target).takeIf { it.exists() }?.readText()
        }
    }

    /** The file the newest backup is in, or null when there is none. */
    private fun newestName(context: Context): String? = info(context)?.name

    /** Describes the newest backup this app can see, or null when there is none. */
    fun info(context: Context): Info? {
        val found = list(context)
            .filter { it.name == FILE_NAME || BackupFileNames.isOurBackupName(it.name) }
            .maxWithOrNull(compareBy<Entry> { it.lastModified }.thenBy { it.name == FILE_NAME })
        return found?.let { Info(locationLabel(it.name), it.lastModified, it.name) }
    }

    /**
     * Every backup file in the folder, newest first.
     *
     * The working files and the numbered copies a write goes through are internal,
     * not backups in their own right. While the real backup is present they are
     * left out, so the folder and the Restore list show the user a single backup
     * rather than a backup of a backup. They appear only when the real backup is
     * missing, which is exactly when one of them is the copy worth recovering.
     */
    fun list(context: Context): List<Entry> {
        val entries = if (BackupFolder.hasAccess(context)) {
            BackupFolder.children(context).map { Entry(it.name, it.lastModified) }
        } else {
            readEntries(context)
        }
        val real = entries.firstOrNull { it.name == FILE_NAME }
        val newestCopy = entries
            .filter { it.name != FILE_NAME && BackupFileNames.isOurBackupName(it.name) }
            .maxByOrNull { it.lastModified }
        // A copy newer than the real file is the backup that was written last, so it
        // is listed; older copies stay hidden behind the real one.
        val keepNewestCopy = real != null && newestCopy != null &&
            newestCopy.lastModified > real.lastModified
        return entries
            .filter {
                it.name == FILE_NAME ||
                    !BackupFileNames.isOurBackupName(it.name) ||
                    real == null ||
                    (keepNewestCopy && it.name == newestCopy?.name)
            }
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
                    val name = cursor.getString(0)
                    if (name != null && name.endsWith(BackupFileNames.JSON_SUFFIX)) {
                        entries += Entry(name, cursor.getLong(1) * MILLIS_PER_SECOND)
                    }
                }
            }
            return entries
        }
        return legacyDir()
            .listFiles { file -> file.isFile && file.name.endsWith(BackupFileNames.JSON_SUFFIX) }
            ?.map { Entry(it.name, it.lastModified()) }
            .orEmpty()
    }

    /** Removes the newest backup, or the file under [name]. True when one was removed. */
    fun delete(context: Context, name: String? = null): Boolean {
        val target = name ?: newestName(context) ?: FILE_NAME
        if (BackupFolder.hasAccess(context)) {
            val file = BackupFolder.find(context, target) ?: return false
            return BackupFolder.delete(context, file.uri)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                selection(),
                selectionArgs(target)
            ) > 0
        }
        return legacyFile(target).delete()
    }

    // --- Android 10+ (MediaStore) -------------------------------------------------

    /**
     * Writes the backup through the media store.
     *
     * The file that is already there is found and its contents are replaced. The
     * media store never overwrites a file by name: adding one under a name that
     * exists quietly saves it as `name (1)`, `name (2)` and so on, and gives up
     * after about thirty of them with "Failed to build unique file". So nothing is
     * ever inserted or renamed while a backup exists - that is what keeps the
     * folder to a single file.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(context: Context, json: String, name: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val exact = findFile(resolver, collection, name)
        if (exact != null && replace(resolver, exact, json)) return
        // The exact name belongs to an older installation; replace the copy we own.
        val copy = newestCopyUri(context, resolver, collection, name)
        if (copy != null && replace(resolver, copy, json)) return
        insertNewFile(resolver, collection, name, json)
    }

    /** Replaces the contents; false only when the file is not this app's to replace. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun replace(resolver: ContentResolver, uri: Uri, json: String): Boolean {
        val written = runCatching { overwriteInPlace(resolver, uri, json) }
        val failure = written.exceptionOrNull() ?: return true
        if (failure is IllegalStateException && failure.cause is SecurityException) return false
        throw failure
    }

    /** The newest copy of the backup file the media store shows this app. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun newestCopyUri(
        context: Context,
        resolver: ContentResolver,
        collection: Uri,
        name: String
    ): Uri? {
        val copy = readEntries(context)
            .filter { it.name != name && BackupFileNames.isOurBackupName(it.name) }
            .maxByOrNull { it.lastModified }
            ?: return null
        return findFile(resolver, collection, copy.name)
    }

    /**
     * Replaces the contents of a file that is already in the folder.
     *
     * The stream is opened in "rwt" mode on purpose. From Android 10 "w" no longer
     * truncates, so a shorter backup written over a longer one would leave the
     * tail of the old file in place and produce invalid JSON; "rwt" is the mode
     * that truncates. The bytes that were there are kept in memory first, so a
     * write that fails or cannot be verified is undone.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun overwriteInPlace(resolver: ContentResolver, uri: Uri, json: String) {
        val previous = runCatching { readBack(resolver, uri) }.getOrNull()

        try {
            writeTo(resolver, uri, json)
        } catch (e: SecurityException) {
            // The file belongs to an earlier installation, and the media store can
            // neither replace it nor ask the user for it - a Downloads file is not
            // an image, video or audio item. Handing the folder over is the way
            // through, so say that rather than write a second file beside it.
            throw IllegalStateException(
                "The backup file belongs to an earlier installation; allow access to the backup folder",
                e
            )
        } catch (e: IOException) {
            // A write that fails part-way leaves the file truncated, so the bytes
            // that were there go back before the failure is passed on.
            if (previous != null) {
                runCatching { writeTo(resolver, uri, previous) }
            }
            throw e
        }

        if (readBack(resolver, uri) == json) return

        if (previous != null) {
            runCatching { writeTo(resolver, uri, previous) }
        }
        error("The backup could not be verified after writing")
    }

    /** Creates the file the first time a backup is written. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertNewFile(resolver: ContentResolver, collection: Uri, name: String, json: String) {
        val created = insertPendingFile(resolver, collection, name)
            ?: error("Could not create the backup file in Downloads")
        // The media store numbers the name when an older file holds it; keep that copy.
        val actual = displayName(resolver, created)
        if (actual != null && BackupFileNames.isOurBackupName(actual)) {
            publishOrDiscard(resolver, created, json)
            return
        }
        runCatching { resolver.delete(created, null, null) }
        error("The backup file could not be created in Downloads")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishOrDiscard(resolver: ContentResolver, uri: Uri, json: String) {
        val written = runCatching { publishNewFile(resolver, uri, json) }
        if (written.isFailure) {
            // Never leave a half-written file behind for the next write to collide with.
            runCatching { resolver.delete(uri, null, null) }
            throw written.exceptionOrNull() ?: IllegalStateException("The backup could not be written")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishNewFile(resolver: ContentResolver, uri: Uri, json: String) {
        writeTo(resolver, uri, json)
        if (readBack(resolver, uri) != json) {
            error("The backup could not be verified after writing")
        }
        val published = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(uri, published, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeTo(resolver: ContentResolver, uri: Uri, json: String) {
        // The documented mode for replacing a file's contents: read-write, and
        // truncate first. "w" alone no longer truncates on recent releases.
        val stream = resolver.openOutputStream(uri, "rwt")
            ?: error("Could not open the backup file for writing")
        stream.use { it.write(json.toByteArray()) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readBack(resolver: ContentResolver, uri: Uri): String? =
        resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

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
     * being put in place. This path is only used below Android 10, where a
     * filesystem rename replaces the file outright.
     */
    private fun writeViaFile(json: String, finalName: String) {
        val temp = legacyFile(BackupFileNames.partialName(finalName))
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
