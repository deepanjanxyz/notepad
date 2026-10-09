package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * A note is worth backing up only if it actually carries something: either a
 * title or body text. A drawing is stored inside [Note.content], so a note that
 * holds only a drawing still counts. This keeps empty notes out of the backup.
 */
fun Note.isBackupWorthy(): Boolean = title.isNotBlank() || content.isNotBlank()

/**
 * Version of the backup format written by this build.
 *
 * 1 - the original format (title, content, date, colour, flags, tags, reminder).
 * 2 - adds the createdAt/updatedAt timestamps, so a restore keeps the original
 *     ordering as well as the original creation date.
 *
 * The parser accepts every version up to this one, so a backup taken with an
 * older build still restores.
 */
private const val BACKUP_VERSION = 2

private const val APP_NAME = "Elite Memo Pro"

/**
 * Outcome of reading a backup document.
 *
 * A malformed file is reported as a [Failure] with a reason that can be shown to
 * the user, rather than silently becoming an empty list: "nothing was restored"
 * and "this file is not a backup" are very different things to someone trying to
 * get their notes back.
 */
sealed interface BackupParseResult {
    data class Success(val notes: List<Note>) : BackupParseResult
    data class Failure(val reason: String) : BackupParseResult
}

/**
 * Serializes the given notes into a portable JSON backup document.
 *
 * Shared by the manual export and the automatic backup so both write exactly
 * the same format. Notes with no title and no body are skipped so the file is
 * never padded with empty entries, and an empty document is returned when there
 * is nothing to store at all, which the writer treats as "write nothing".
 */
fun buildBackupJson(notes: List<Note>): String {
    val worthBackingUp = notes.filter { it.isBackupWorthy() }
    // With nothing to store, return an empty document so callers can tell the
    // difference and avoid writing an empty file.
    if (worthBackingUp.isEmpty()) return ""
    val array = JSONArray()
    worthBackingUp.forEach { note ->
        val obj = JSONObject()
        obj.put("title", note.title)
        obj.put("content", note.content)
        obj.put("date", note.date)
        obj.put("colorIndex", note.colorIndex)
        obj.put("isPinned", note.isPinned)
        obj.put("inArchive", note.inArchive)
        obj.put("reminderTime", note.reminderTime ?: JSONObject.NULL)
        obj.put("createdAt", note.createdAt)
        obj.put("updatedAt", note.updatedAt)
        val tagArray = JSONArray()
        note.tags.forEach { tagArray.put(it) }
        obj.put("tags", tagArray)
        array.put(obj)
    }
    val root = JSONObject()
    root.put("app", APP_NAME)
    root.put("version", BACKUP_VERSION)
    root.put("notes", array)
    return root.toString(2)
}

/**
 * Parses a JSON backup document produced by [buildBackupJson].
 *
 * The document is validated before anything is read out of it: it has to be
 * valid JSON, carry a version this build understands, and contain a `notes`
 * array. Anything else is reported as a [BackupParseResult.Failure] explaining
 * what is wrong.
 */
fun parseBackupJson(text: String): BackupParseResult {
    if (text.isBlank()) return BackupParseResult.Failure("That file is empty")

    val root = runCatching { JSONObject(text) }.getOrElse {
        return BackupParseResult.Failure("That file is not a JSON backup")
    }

    // A missing version means this is not one of our documents at all; a version
    // above ours means it was written by a newer build whose fields we cannot be
    // sure we understand.
    val version = root.optInt("version", 0)
    if (version <= 0) {
        return BackupParseResult.Failure("That file is not an Elite Memo Pro backup")
    }
    if (version > BACKUP_VERSION) {
        return BackupParseResult.Failure(
            "That backup was written by a newer version of the app"
        )
    }

    val array = root.optJSONArray("notes")
        ?: return BackupParseResult.Failure("That backup has no notes section")

    val result = mutableListOf<Note>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val tagArray = obj.optJSONArray("tags")
        val tags = if (tagArray == null) {
            emptyList()
        } else {
            (0 until tagArray.length())
                .mapNotNull { index -> tagArray.optString(index).takeIf { it.isNotBlank() } }
        }
        result.add(
            Note(
                title = obj.optString("title"),
                content = obj.optString("content"),
                date = obj.optString("date"),
                colorIndex = obj.optInt("colorIndex"),
                isPinned = obj.optBoolean("isPinned"),
                inArchive = obj.optBoolean("inArchive"),
                tags = tags,
                reminderTime = if (obj.isNull("reminderTime")) null else obj.optLong("reminderTime"),
                createdAt = obj.optLong("createdAt"),
                updatedAt = obj.optLong("updatedAt")
            )
        )
    }
    if (result.isEmpty()) {
        return BackupParseResult.Failure("That backup contains no notes")
    }
    return BackupParseResult.Success(result)
}
