package com.deepanjanxyz.notepad.domain.model

import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
) {
    companion object {
        fun parseList(raw: String): List<ChecklistItem> {
            if (raw.isBlank()) return emptyList()
            return raw.lines().filter { it.isNotBlank() }.map { line ->
                when {
                    line.startsWith("[x] ", ignoreCase = true) ->
                        ChecklistItem(text = line.substring(4), isChecked = true)
                    line.startsWith("[ ] ") ->
                        ChecklistItem(text = line.substring(4), isChecked = false)
                    else ->
                        ChecklistItem(text = line, isChecked = false)
                }
            }
        }

        fun serializeList(items: List<ChecklistItem>): String {
            return items.joinToString("\n") { item ->
                val prefix = if (item.isChecked) "[x] " else "[ ] "
                prefix + item.text
            }
        }
    }
}

fun parseChecklist(raw: String): List<ChecklistItem> = ChecklistItem.parseList(raw)
fun serializeChecklist(items: List<ChecklistItem>): String = ChecklistItem.serializeList(items)

