package com.deepanjanxyz.notepad

/**
 * How the note list is ordered.
 */
enum class NoteSortOrder(val labelRes: Int) {
    NEWEST(R.string.sort_newest),
    OLDEST(R.string.sort_oldest),
    TITLE(R.string.sort_title);

    /** SQL fragment appended to the SELECT used by [DatabaseHelper]. */
    val orderByClause: String
        get() = when (this) {
            NEWEST -> "ORDER BY ${DatabaseHelper.COLUMN_ID} DESC"
            OLDEST -> "ORDER BY ${DatabaseHelper.COLUMN_ID} ASC"
            TITLE -> "ORDER BY ${DatabaseHelper.COLUMN_TITLE} COLLATE NOCASE ASC"
        }
}

/**
 * Which note fields the search query is matched against.
 */
enum class SearchScope(val labelRes: Int) {
    ALL(R.string.filter_all),
    TITLE(R.string.filter_title),
    CONTENT(R.string.filter_content);
}
