package com.deepanjanxyz.notepad.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileNamesTest {

    /** The backup file's own name is recognised as this app's backup. */
    @Test
    fun testTheBackupFileItselfIsOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.json"))
    }

    /** The internal `.partial` and `.previous` working files are recognised as this app's. */
    @Test
    fun testWorkingFilesAreOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.partial.json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.previous.json"))
    }

    /** The `(N)` copies the media store makes of the backup and its working files are recognised as this app's. */
    @Test
    fun testNumberedCopiesAreOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup (1).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup (18).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.partial (9).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.previous (3).json"))
    }

    /** Files that only resemble the backup's name, or are not JSON, are not recognised as this app's. */
    @Test
    fun testOtherFilesAreNotOurs() {
        assertFalse(BackupFileNames.isOurBackupName("notes.json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup copy.json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup (x).json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup (1).txt"))
    }

    /** Every numbered copy is removable once the real backup file is present. */
    @Test
    fun testEveryCopyGoesOnceTheRealBackupIsThere() {
        val found = listOf(
            "elite-memo-backup.json" to 100L,
            "elite-memo-backup (1).json" to 90L,
            "elite-memo-backup (2).json" to 95L
        )
        assertEquals(
            listOf("elite-memo-backup (2).json", "elite-memo-backup (1).json"),
            BackupFileNames.copiesToRemove(found)
        )
    }

    /** When the real backup is missing, the newest copy is kept back as the only recoverable one. */
    @Test
    fun testTheNewestCopyIsKeptWhenTheRealBackupIsNotVisible() {
        val found = listOf(
            "elite-memo-backup (1).json" to 90L,
            "elite-memo-backup (2).json" to 95L,
            "elite-memo-backup (3).json" to 80L
        )
        assertEquals(
            listOf("elite-memo-backup (1).json", "elite-memo-backup (3).json"),
            BackupFileNames.copiesToRemove(found)
        )
    }

    /** A single copy with no real backup present is never removed. */
    @Test
    fun testTheLastCopyIsNeverRemoved() {
        assertEquals(
            emptyList<String>(),
            BackupFileNames.copiesToRemove(listOf("elite-memo-backup (7).json" to 1L))
        )
    }

    /** Files that are not this app's backup names are never proposed for removal. */
    @Test
    fun testFilesThatAreNotOursAreNeverRemoved() {
        assertEquals(
            emptyList<String>(),
            BackupFileNames.copiesToRemove(listOf("holiday.json" to 5L, "notes.txt" to 6L))
        )
    }

    /** The partial and previous working names keep the `.json` suffix. */
    @Test
    fun testWorkingNamesKeepTheJsonSuffix() {
        assertTrue(BackupFileNames.partialName("elite-memo-backup.json").endsWith(".json"))
        assertTrue(BackupFileNames.previousName("elite-memo-backup.json").endsWith(".json"))
        assertFalse(BackupFileNames.isOurBackupName(BackupFileNames.partialName("notes.json")))
    }
}
