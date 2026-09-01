package com.deepanjanxyz.notepad.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownPreview(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val lines = content.lines()
        var inCodeBlock = false
        val codeBlockBuffer = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    CodeBlockView(code = codeBlockBuffer.toString().trimEnd())
                    codeBlockBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(line).append("\n")
                continue
            }

            when {
                trimmed == "---" || trimmed == "***" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                trimmed.startsWith("> ") -> {
                    BlockquoteView(text = trimmed.removePrefix("> "))
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    ChecklistItemView(text = trimmed.substring(6), checked = false)
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("* [x] ") ||
                    trimmed.startsWith("- [X] ") || trimmed.startsWith("* [X] ") -> {
                    ChecklistItemView(text = trimmed.substring(6), checked = true)
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    BulletItemView(text = trimmed.substring(2))
                }
                trimmed.matches("^\\d+\\.\\s.*".toRegex()) -> {
                    val dotIndex = trimmed.indexOf('.')
                    val number = trimmed.substring(0, dotIndex + 1)
                    val text = trimmed.substring(dotIndex + 1).trim()
                    NumberedItemView(number = number, text = text)
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        if (inCodeBlock && codeBlockBuffer.isNotEmpty()) {
            CodeBlockView(code = codeBlockBuffer.toString().trimEnd())
        }
    }
}

@Composable
private fun BlockquoteView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.small
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BulletItemView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NumberedItemView(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChecklistItemView(text: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (checked) "☑ " else "☐ ",
            style = MaterialTheme.typography.titleMedium,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CodeBlockView(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp)
        )
    }
}
