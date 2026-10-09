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
 * Serializes the given notes into a portable JSON backup document.
 *
 * Shared by the manual export and the automatic backup so both write exactly
 * the same format. Notes with no title and no body are skipped so the file is
 * never padded with empty entries.
 */
fun buildBackupJson(notes: List<Note>): String {
    val array = JSONArray()
    notes.filter { it.isBackupWorthy() }.forEach { note ->
        val obj = JSONObject()
        obj.put("title", note.title)
        obj.put("content", note.content)
        obj.put("date", note.date)
        obj.put("colorIndex", note.colorIndex)
        obj.put("isPinned", note.isPinned)
        obj.put("inArchive", note.inArchive)
        obj.put("reminderTime", note.reminderTime ?: JSONObject.NULL)
        val tagArray = JSONArray()
        note.tags.forEach { tagArray.put(it) }
        obj.put("tags", tagArray)
        array.put(obj)
    }
    val root = JSONObject()
    root.put("app", "Elite Memo Pro")
    root.put("version", 1)
    root.put("notes", array)
    return root.toString(2)
}

/** Parses a JSON backup document produced by [buildBackupJson]. */
fun parseBackupJson(text: String): List<Note> {
    if (text.isBlank()) return emptyList()
    return runCatching {
        val root = JSONObject(text)
        val array = root.optJSONArray("notes")
        val result = mutableListOf<Note>()
        if (array != null) {
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
                        reminderTime = if (obj.isNull("reminderTime")) null else obj.optLong("reminderTime")
                    )
                )
            }
        }
        result.toList()
    }.getOrDefault(emptyList())
}
