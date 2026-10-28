package com.tasteindia.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tasteindia.domain.FilterState
import com.tasteindia.domain.SortOrder
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Search recipes") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search text")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
fun FilterBar(
    filters: FilterState,
    categories: List<String>,
    actions: RecipesActions,
    onOpenIngredientPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = filters.category != null,
                onClick = { menuOpen = true },
                label = { Text(filters.category ?: "Category") },
                leadingIcon = if (filters.category != null) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose category") },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("All categories") },
                    onClick = { actions.onCategorySelected(null); menuOpen = false },
                )
                if (categories.isEmpty()) {
                    DropdownMenuItem(text = { Text("Categories unavailable") }, onClick = { menuOpen = false }, enabled = false)
                }
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = { actions.onCategorySelected(category); menuOpen = false },
                        leadingIcon = if (category == filters.category) {
                            { Icon(Icons.Default.Check, contentDescription = "Selected") }
                        } else null,
                    )
                }
            }
        }

        FilterChip(
            selected = filters.ingredient != null,
            onClick = onOpenIngredientPicker,
            label = { Text(filters.ingredient ?: "Main ingredient") },
            leadingIcon = if (filters.ingredient != null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose main ingredient") },
        )

        FilterChip(
            selected = filters.favouritesOnly,
            onClick = { actions.onFavouritesOnlyChange(!filters.favouritesOnly) },
            label = { Text("Favourites only") },
            leadingIcon = {
                Icon(
                    if (filters.favouritesOnly) Icons.Default.Check else Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )

        val sortLabel = if (filters.sort == SortOrder.NameAsc) "Name A–Z" else "Name Z–A"
        AssistChip(
            onClick = actions.onToggleSort,
            label = { Text(sortLabel) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            modifier = Modifier.semantics { contentDescription = "Sort order: $sortLabel. Double tap to reverse." },
        )
    }
}

/** Result count, removable active-filter chips and Clear all. */
@Composable
fun ResultSummary(shown: Int, total: Int, filters: FilterState, onClearAll: () -> Unit, actions: RecipesActions) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Showing $shown of $total recipes",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (filters.hasActiveFilters) {
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
        }
        if (filters.hasActiveFilters) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filters.query.isNotBlank()) {
                    ActiveChip("Search: “${filters.query.trim()}”", "search") { actions.onQueryChange("") }
                }
                filters.category?.let { ActiveChip("Category: $it", "category") { actions.onCategorySelected(null) } }
                filters.ingredient?.let { ActiveChip("Ingredient: $it", "ingredient") { actions.onIngredientSelected(null) } }
                if (filters.favouritesOnly) {
                    ActiveChip("Favourites only", "favourites") { actions.onFavouritesOnlyChange(false) }
                }
            }
        }
    }
}

@Composable
private fun ActiveChip(label: String, what: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $what filter", modifier = Modifier.size(InputChipIconSize)) },
    )
}

private val InputChipIconSize = 18.dp

@Composable
fun IngredientPickerDialog(
    ingredients: List<String>,
    loading: Boolean,
    error: String?,
    selected: String?,
    onSelect: (String?) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val visible = remember(ingredients, filter) {
        ingredients.filter { it.contains(filter.trim(), ignoreCase = true) }.take(200)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Main ingredient") },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Search ingredients") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Loading ingredients…")
                    }
                    error != null -> Column {
                        Text(error)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                    visible.isEmpty() -> Text("No ingredients match.")
                    else -> LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(visible, key = { it }) { name ->
                            ListItem(
                                headlineContent = { Text(name) },
                                trailingContent = if (name == selected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null,
                                modifier = Modifier.clickable { onSelect(name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = if (selected != null) {
            { TextButton(onClick = { onSelect(null) }) { Text("Remove filter") } }
        } else null,
    )
}
