package com.deepanjanxyz.notepad.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileNamesTest {

    @Test
    fun testTheBackupFileItselfIsOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.json"))
    }

    @Test
    fun testWorkingFilesAreOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.partial.json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.previous.json"))
    }

    @Test
    fun testNumberedCopiesAreOurs() {
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup (1).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup (18).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.partial (9).json"))
        assertTrue(BackupFileNames.isOurBackupName("elite-memo-backup.previous (3).json"))
    }

    @Test
    fun testWorkingFilesAreRecognised() {
        assertTrue(BackupFileNames.isWorkingName("elite-memo-backup.partial.json"))
        assertTrue(BackupFileNames.isWorkingName("elite-memo-backup.previous (2).json"))
        assertFalse(BackupFileNames.isWorkingName("elite-memo-backup.json"))
        assertFalse(BackupFileNames.isWorkingName("elite-memo-backup (1).json"))
    }

    @Test
    fun testOtherFilesAreNotOurs() {
        assertFalse(BackupFileNames.isOurBackupName("notes.json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup copy.json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup (x).json"))
        assertFalse(BackupFileNames.isOurBackupName("elite-memo-backup (1).txt"))
    }

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

    @Test
    fun testTheLastCopyIsNeverRemoved() {
        assertEquals(
            emptyList<String>(),
            BackupFileNames.copiesToRemove(listOf("elite-memo-backup (7).json" to 1L))
        )
    }

    @Test
    fun testACopyNewerThanTheRealBackupIsKept() {
        val found = listOf(
            "elite-memo-backup.json" to 100L,
            "elite-memo-backup (1).json" to 120L,
            "elite-memo-backup (2).json" to 90L
        )
        assertEquals(
            listOf("elite-memo-backup (2).json"),
            BackupFileNames.copiesToRemove(found)
        )
    }

    @Test
    fun testACopyWrittenInTheSameSecondAsTheRealBackupIsKept() {
        val found = listOf(
            "elite-memo-backup.json" to 100L,
            "elite-memo-backup (1).json" to 100L
        )
        assertEquals(emptyList<String>(), BackupFileNames.copiesToRemove(found))
    }

    @Test
    fun testFilesThatAreNotOursAreNeverRemoved() {
        assertEquals(
            emptyList<String>(),
            BackupFileNames.copiesToRemove(listOf("holiday.json" to 5L, "notes.txt" to 6L))
        )
    }

    @Test
    fun testWorkingNamesKeepTheJsonSuffix() {
        assertTrue(BackupFileNames.partialName("elite-memo-backup.json").endsWith(".json"))
        assertTrue(BackupFileNames.previousName("elite-memo-backup.json").endsWith(".json"))
        assertFalse(BackupFileNames.isOurBackupName(BackupFileNames.partialName("notes.json")))
    }
}
