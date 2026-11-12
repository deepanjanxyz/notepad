package com.deepanjanxyz.notepad.data.local.dao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteIdBatchesTest {

    @Test
    fun aWholeLibrarySplitsIntoBatchesNoStatementCanOverrun() {
        val ids = (1L..7_000L).toList()

        val batches = ids.toStatementBatches()

        assertEquals(14, batches.size)
        assertTrue(batches.all { it.size <= MAX_IDS_PER_STATEMENT })
        assertEquals(ids, batches.flatten())
    }

    @Test
    fun aSelectionThatFitsStaysOneBatch() {
        val ids = listOf(4L, 8L, 15L)

        assertEquals(listOf(ids), ids.toStatementBatches())
    }

    @Test
    fun aRepeatedIdIsOnlySentOnce() {
        val ids = List(1_200) { 7L }

        val batches = ids.toStatementBatches()

        assertEquals(listOf(listOf(7L)), batches)
    }

    @Test
    fun anEmptySelectionSendsNoStatement() {
        assertEquals(emptyList<List<Long>>(), emptyList<Long>().toStatementBatches())
    }

    @Test
    fun theBatchSizeStaysUnderTheStatementCap() {
        assertTrue(MAX_IDS_PER_STATEMENT > 0)
        assertTrue(MAX_IDS_PER_STATEMENT < 999)
    }
}
