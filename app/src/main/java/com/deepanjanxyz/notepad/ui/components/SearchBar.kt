package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R

@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isGridLayout: Boolean,
    onToggleLayout: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_search_bar")
            .testTag("search_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            // Leading Icon: If query is active, show back arrow to clear search, otherwise drawer menu toggle
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.testTag("search_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Distinct Search Icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                modifier = Modifier
                    .padding(start = 2.dp, end = 2.dp)
                    .size(22.dp)
                    .testTag("search_icon")
            )

            // Real-time Search Input Field: Filters notes by title or content keywords
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("search_text_field")
            )

            // Clear search text button if active
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    },
                    modifier = Modifier.testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trailing Icon: Layout View Switcher button (Grid / Staggered Grid vs Single Column List)
            IconButton(
                onClick = onToggleLayout,
                modifier = Modifier.testTag("layout_toggle_button")
            ) {
                Icon(
                    imageVector = if (isGridLayout) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridLayout) "Switch to Single Column List" else "Switch to Grid View",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

