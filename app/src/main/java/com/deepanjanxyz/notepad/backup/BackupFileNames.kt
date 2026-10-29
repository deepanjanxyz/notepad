package com.deepanjanxyz.notepad.backup

/**
 * The names the backup uses, and how to recognise the copies Android's media
 * store makes when a name is already taken.
 *
 * When a file is added to Downloads under a name that is already there, the media
 * store does not fail and does not overwrite: it quietly saves the new file under
 * a numbered name instead - `elite-memo-backup (1).json`, `(2)`, and so on - until
 * it gives up after about thirty tries and throws "Failed to build unique file".
 * Recognising those names is what lets the app clear them up, and it lives here,
 * apart from the storage code, so it can be tested on its own.
 */
object BackupFileNames {

    /** The single file a backup is always written to. */
    const val FILE_NAME = "elite-memo-backup.json"

    const val JSON_SUFFIX = ".json"
    private const val PARTIAL_SUFFIX = ".partial.json"
    private const val PREVIOUS_SUFFIX = ".previous.json"

    /**
     * Name of the temporary file a legacy write goes through first.
     *
     * It deliberately keeps the `.json` suffix: if a write is interrupted between
     * the previous file being dropped and the temporary one being renamed, the
     * verified copy is still listed and can be restored by hand.
     */
    fun partialName(name: String): String =
        if (name.endsWith(JSON_SUFFIX)) {
            name.removeSuffix(JSON_SUFFIX) + PARTIAL_SUFFIX
        } else {
            "$name.partial"
        }

    /**
     * Name the previous backup is parked under while a new one is moved into place
     * on the legacy path. Like the partial name, it keeps the `.json` suffix so an
     * interrupted swap still leaves a restorable file.
     */
    fun previousName(name: String): String =
        if (name.endsWith(JSON_SUFFIX)) {
            name.removeSuffix(JSON_SUFFIX) + PREVIOUS_SUFFIX
        } else {
            "$name.previous"
        }

    /** True when [name] is this app's backup file, a working file, or a copy of one. */
    fun isOurBackupName(name: String): Boolean =
        listOf(FILE_NAME, partialName(FILE_NAME), previousName(FILE_NAME))
            .any { isCopyOf(it, name) }

    /**
     * The copies among [found] that can safely be removed, newest first.
     *
     * While the real backup is there, every copy is surplus. When it is not - the
     * state a reinstall leaves behind, the old file still on disk but no longer
     * this app's to see - the newest copy is kept back, because it may be the only
     * backup the app can still reach and losing it would be worse than one extra
     * file left behind.
     */
    fun copiesToRemove(found: List<Pair<String, Long>>): List<String> {
        val copies = found
            .filter { it.first != FILE_NAME && isOurBackupName(it.first) }
            .sortedByDescending { it.second }
        val real = found.firstOrNull { it.first == FILE_NAME }?.second
        // A copy at least as new as the real file is the backup written last. The
        // comparison is not strict: the media store reports whole seconds, so a copy
        // written in the same second as the real file is the newer of the two.
        val keepNewest = copies.isNotEmpty() && (real == null || copies.first().second >= real)
        return (if (keepNewest) copies.drop(1) else copies).map { it.first }
    }

    /** True when [name] is [candidate] itself, or the `(N)` copy the media store made of it. */
    fun isCopyOf(candidate: String, name: String): Boolean {
        if (name == candidate) return true
        val base = candidate.removeSuffix(JSON_SUFFIX)
        val prefix = "$base ("
        val suffix = ")$JSON_SUFFIX"
        val number = if (name.startsWith(prefix) && name.endsWith(suffix)) {
            name.substring(prefix.length, name.length - suffix.length)
        } else {
            ""
        }
        return number.isNotEmpty() && number.all { it.isDigit() }
    }
}
