package com.tasteindia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasteindia.domain.MealSummary
import com.tasteindia.ui.recipes.MealRowUi
import com.tasteindia.ui.theme.TasteIndiaTheme

/** Stateless, previewable list row: title = strMeal, image = strMealThumb, accessible favourite toggle. */
@Composable
fun MealRow(
    row: MealRowUi,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(onClickLabel = "Open recipe details", onClick = onClick)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decorative here: the visible title already names the dish.
                MealImage(row.meal.thumbUrl, contentDescription = null, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(row.meal.name, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    row.category?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FavouriteToggle(row.meal.name, row.isFavourite, onToggleFavourite)
        }
    }
}

/** Filled vs outlined heart (shape, not only colour) + explicit label. 48dp touch target. */
@Composable
fun FavouriteToggle(mealName: String, isFavourite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(checked = isFavourite, onCheckedChange = { onToggle() }, modifier = modifier) {
        Icon(
            imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavourite) "Remove $mealName from favourites" else "Add $mealName to favourites",
            tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealRowPreview() = TasteIndiaTheme {
    MealRow(MealRowUi(MealSummary("1", "Chicken Handi with a really long name that wraps", null), "Chicken", true), {}, {})
}
