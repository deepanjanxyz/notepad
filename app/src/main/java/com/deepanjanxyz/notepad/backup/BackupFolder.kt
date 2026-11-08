package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import java.io.IOException

/**
 * The folder the user has handed to the app to keep its backup in.
 *
 * A backup file written by an earlier installation belongs to that installation.
 * After a reinstall this one cannot read it, write over it or delete it, and the
 * media store quietly saves every new backup beside it as "name (1)", "name (2)"
 * and so on - one more file on every run, for ever. The only way in is for the
 * user to grant the folder itself, once. This holds that grant and the file
 * operations it makes possible.
 *
 * `TooManyFunctions` is suppressed here rather than the object split. This object
 * is the whole of the backup folder - where it is, how it is named, and every read,
 * write and delete the app performs inside it - and its functions are short and
 * each of them is called from somewhere. Splitting them across files would move the
 * same code behind a second name without making any of it simpler.
 */
@Suppress("TooManyFunctions")
object BackupFolder {

    /** Name of the folder the backup is kept in, under Downloads. */
    const val FOLDER_NAME = "EliteMemoPro"

    private const val PREFERENCES = "backup_folder"
    private const val KEY_TREE_URI = "tree_uri"
    private const val KEY_ADOPT_PENDING = "adopt_pending"
    private const val AUTHORITY = "com.android.externalstorage.documents"
    private const val MIME_TYPE = "application/json"
    // The documented mode for replacing a document's contents: read-write, and
    // truncate first. "w" alone no longer truncates on recent releases.
    private const val WRITE_MODE = "rwt"

    /** One file in the granted folder. */
    data class Child(val uri: Uri, val name: String, val lastModified: Long)

    /** The granted folder, or null when the user has not granted one. */
    fun treeUri(context: Context): Uri? =
        preferences(context).getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun hasAccess(context: Context): Boolean = treeUri(context) != null

    /**
     * The folder the picker should open at, so the user only has to confirm it.
     * Null below Android 8, where the picker has no way to be pointed at it.
     */
    fun initialUri(folder: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) folderDocumentUri(folder) else null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun folderDocumentUri(folder: String): Uri =
        DocumentsContract.buildDocumentUri(
            AUTHORITY,
            "primary:${Environment.DIRECTORY_DOWNLOADS}/$folder"
        )

    /**
     * Keeps the folder the user picked, so it survives restarts and reboots.
     *
     * The folder is checked first, because the picker will let the user confirm
     * any folder at all and the write, the cleanup and the location shown in
     * Settings all assume the backup folder is the one that was chosen.
     *
     * The folder is also marked for adoption: whatever backup is already in it was
     * not written by this installation, and its notes are brought in on the first
     * write rather than overwritten unseen.
     */
    fun take(context: Context, uri: Uri): Boolean {
        if (!isBackupFolder(context, uri)) return false
        val taken = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.isSuccess
        if (taken) {
            preferences(context).edit()
                .putString(KEY_TREE_URI, uri.toString())
                .putBoolean(KEY_ADOPT_PENDING, true)
                .apply()
        }
        return taken
    }

    /**
     * True while a folder has been granted whose backup has not been brought in
     * yet. It stays true until [finishAdoption], so an import that could not be
     * done is tried again rather than silently skipped.
     */
    fun hasAdoption(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ADOPT_PENDING, false)

    /** Marks the adoption finished, so the next write is a plain overwrite. */
    fun finishAdoption(context: Context) {
        preferences(context).edit().putBoolean(KEY_ADOPT_PENDING, false).apply()
    }

    /**
     * True when the tree is the backup folder inside Downloads - and not merely a
     * folder with the same name somewhere else, which the picker would happily
     * hand over while the write, the cleanup and the location shown in Settings
     * all went on assuming the backup folder is this one.
     */
    private fun isBackupFolder(context: Context, tree: Uri): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isUnderDownloads(context, tree)
        } else {
            nameOf(context, treeDocumentUri(tree)) == FOLDER_NAME
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isUnderDownloads(context: Context, tree: Uri): Boolean {
        val path = runCatching {
            DocumentsContract.findDocumentPath(context.contentResolver, treeDocumentUri(tree))?.path
        }.getOrNull() ?: return false
        val expected = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"
        return path.any { it.endsWith(":$expected") }
    }

    /** Every file in the granted folder. */
    fun children(context: Context): List<Child> {
        val tree = treeUri(context) ?: return emptyList()
        val children = mutableListOf<Child>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree)
        )
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                if (id != null && name != null) {
                    children += Child(
                        uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                        name = name,
                        lastModified = cursor.getLong(2)
                    )
                }
            }
        }
        return children
    }

    /** True when the granted folder is still there and can be read. */
    fun isReachable(context: Context): Boolean {
        val tree = treeUri(context) ?: return false
        return runCatching {
            context.contentResolver.query(
                treeDocumentUri(tree),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            )?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    /** Forgets the granted folder, so the user is asked for it again. */
    fun release(context: Context) {
        preferences(context).edit().remove(KEY_TREE_URI).apply()
    }

    /** The file in the folder with this exact name, or null. */
    fun find(context: Context, name: String): Child? =
        children(context).firstOrNull { it.name == name }

    fun read(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull()

    /**
     * Replaces the contents of an existing file and reads them back.
     *
     * The bytes that were there are read first, so a write that fails or cannot
     * be verified puts them back instead of leaving the user without their last
     * good backup.
     */
    fun write(context: Context, uri: Uri, json: String) {
        // Read without swallowing errors: if the old contents cannot be read then
        // the write must not begin at all, because there would be nothing to put
        // back if it failed.
        val previous = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes().decodeToString() }
        val written = runCatching { writeTo(context, uri, json) }
        if (written.isSuccess && read(context, uri) == json) return
        if (previous != null) {
            runCatching { writeTo(context, uri, previous) }
        }
        throw written.exceptionOrNull()
            ?: IOException("The backup could not be verified after writing")
    }

    private fun writeTo(context: Context, uri: Uri, json: String) {
        val stream = context.contentResolver.openOutputStream(uri, WRITE_MODE)
            ?: throw IOException("Could not open the backup file for writing")
        stream.use { it.write(json.toByteArray()) }
    }

    /**
     * Creates the file under exactly this name, or returns null when the folder
     * will not give it that name - which is what keeps a second file from ever
     * being written beside the first. A provider answers a name that is already
     * taken with "name (1)" rather than an error, so the name that came back is
     * read and a numbered file is removed again.
     */
    fun create(context: Context, name: String): Uri? {
        val tree = treeUri(context) ?: return null
        val created = runCatching {
            DocumentsContract.createDocument(
                context.contentResolver,
                treeDocumentUri(tree),
                MIME_TYPE,
                name
            )
        }.getOrNull()
        val usable = created?.takeIf { nameOf(context, it) == name }
        if (created != null && usable == null) {
            delete(context, created)
        }
        return usable
    }

    private fun treeDocumentUri(tree: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** The display name of a single document, or null when it cannot be read. */
    private fun nameOf(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()

    fun delete(context: Context, uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            .getOrDefault(false)

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
