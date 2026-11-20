package com.deepanjanxyz.notepad.data.local.dao

/**
 * How many ids one statement may carry.
 *
 * SQLite caps the number of values a single statement can bind, and the cap on
 * the platform is far below a whole-library selection, so bulk actions over
 * thousands of notes are split into several statements instead of one `IN` list.
 */
internal const val MAX_IDS_PER_STATEMENT = 500

/**
 * Splits [ids] into batches small enough to bind in one statement.
 *
 * Repeated ids collapse first, so a selection that names the same note twice
 * cannot inflate the number of statements, and the order of the selection is
 * kept so the batches stay predictable.
 */
internal fun List<Long>.toStatementBatches(): List<List<Long>> =
    distinct().chunked(MAX_IDS_PER_STATEMENT)
