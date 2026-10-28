@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tasteindia.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasteindia.domain.Ingredient
import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.safeHttpUrl
import com.tasteindia.ui.components.ErrorView
import com.tasteindia.ui.components.FavouriteToggle
import com.tasteindia.ui.components.LoadingView
import com.tasteindia.ui.components.MealImage
import com.tasteindia.ui.theme.TasteIndiaTheme
import kotlinx.coroutines.launch

@Composable
fun DetailsRoute(viewModel: DetailsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Links are validated again here; opening can still fail if no app handles the intent.
    val openLink: (String) -> Unit = { url ->
        val safe = safeHttpUrl(url)
        val opened = safe != null && try {
            uriHandler.openUri(safe)
            true
        } catch (e: Exception) {
            false
        }
        if (!opened) scope.launch { snackbar.showSnackbar("No app available to open this link.") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state as? DetailsUiState.Content)?.detail?.name ?: "Recipe", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    (state as? DetailsUiState.Content)?.let {
                        FavouriteToggle(it.detail.name, it.isFavourite, viewModel::onToggleFavourite)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val s = state) {
            DetailsUiState.Loading -> LoadingView(Modifier.padding(padding), "Loading recipe…")
            is DetailsUiState.Error -> ErrorView(s.message, viewModel::onRetry, Modifier.padding(padding))
            is DetailsUiState.Content -> DetailsContent(s.detail, openLink, Modifier.padding(padding))
        }
    }
}

/** Stateless and previewable. Only non-empty fields are rendered. */
@Composable
fun DetailsContent(detail: MealDetail, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MealImage(
                url = detail.thumbUrl,
                contentDescription = "Photo of the finished dish",
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip16(),
            )
        }
        item { Text(detail.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }) }

        val meta = listOfNotNull(
            detail.category?.let { "Category: $it" },
            detail.area?.let { "Area: $it" },
        )
        if (meta.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meta.forEach { Pill(it) }
                }
            }
        }
        if (detail.tags.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.tags.forEach { Pill("Tag: $it") }
                }
            }
        }
        detail.alternateDrink?.let { drink -> item { Text("Serve with: $drink", style = MaterialTheme.typography.bodyLarge) } }

        if (detail.ingredients.isNotEmpty()) {
            item { SectionTitle("Ingredients") }
            items(detail.ingredients) { IngredientRow(it) }
        }
        if (detail.instructions.isNotEmpty()) {
            item { SectionTitle("Instructions") }
            items(detail.instructions.size) { index ->
                Text(detail.instructions[index], style = MaterialTheme.typography.bodyLarge)
            }
        }

        val hasLinks = detail.sourceUrl != null || detail.videoUrl != null || detail.imageSourceUrl != null
        if (hasLinks) {
            item { SectionTitle("Links") }
            detail.sourceUrl?.let { url ->
                item { OutlinedButton(onClick = { onOpenLink(url) }, modifier = Modifier.fillMaxWidth()) { Text("Read the original recipe") } }
            }
            detail.videoUrl?.let { url ->
                item {
                    OutlinedButton(onClick = { onOpenLink(url) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Watch the video tutorial")
                    }
                }
            }
            detail.imageSourceUrl?.let { url ->
                item { OutlinedButton(onClick = { onOpenLink(url) }, modifier = Modifier.fillMaxWidth()) { Text("View image source") } }
            }
        }
        item {
            Text(
                "Recipe data and images from TheMealDB (themealdb.com).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.clip16(): Modifier = this.then(Modifier.clip(RoundedCornerShape(16.dp)))

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun IngredientRow(ingredient: Ingredient) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(ingredient.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            ingredient.measure?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailsPreview() = TasteIndiaTheme {
    DetailsContent(
        MealDetail(
            id = "1", name = "Chicken Handi", thumbUrl = null, category = "Chicken", area = "Indian",
            ingredients = listOf(Ingredient("Chicken", "1.2 kg"), Ingredient("Onion", "5 thinly sliced")),
            instructions = List(6) { "Step ${it + 1}: a long instruction paragraph that wraps over several lines to check large text." },
            tags = listOf("Curry", "Spicy"), sourceUrl = "https://example.com", videoUrl = "https://youtube.com/watch?v=x",
            imageSourceUrl = null, alternateDrink = null,
        ),
        onOpenLink = {},
    )
}
