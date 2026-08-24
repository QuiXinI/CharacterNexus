package ru.quasaris.characternexus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.*

@Composable
fun <T> ReorderableAdaptiveGrid(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    minSize: Dp = 340.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    isReorderEnabled: Boolean = true,
    itemContent: @Composable (T, Boolean, Modifier) -> Unit
) {
    val gridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val newList = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(newList)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minSize),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        items(items, key = key) { item ->
            ReorderableItem(reorderableState, key(item)) { isDragging ->
                val dragModifier = if (isReorderEnabled) {
                    Modifier.draggableHandle()
                } else {
                    Modifier
                }
                itemContent(item, isDragging, dragModifier)
            }
        }
    }
}
