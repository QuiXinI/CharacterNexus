package ru.quasaris.characternexus.tabs.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.model.DynamicContentBlock

data class CellCoord(val row: Int, val col: Int)
data class CellRange(val startRow: Int, val startCol: Int, val endRow: Int, val endCol: Int) {
    val minRow = minOf(startRow, endRow)
    val maxRow = maxOf(startRow, endRow)
    val minCol = minOf(startCol, endCol)
    val maxCol = maxOf(startCol, endCol)

    fun contains(r: Int, c: Int): Boolean = r in minRow..maxRow && c in minCol..maxCol
    val size: Int = (maxRow - minRow + 1) * (maxCol - minCol + 1)
}

@Composable
fun TableBlockComponent(
    table: DynamicContentBlock.Table,
    onUpdate: (DynamicContentBlock.Table) -> Unit,
    canEdit: Boolean,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
    activeCellCoord: CellCoord? = null,
    onActiveCellChange: (CellCoord?) -> Unit = {},
    selectedRange: CellRange? = null,
    onSelectedRangeChange: (CellRange?) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var rows by remember(table) { mutableStateOf(table.rows) }

    LaunchedEffect(table.rows) {
        if (rows != table.rows) {
            rows = table.rows
        }
    }

    fun emitUpdate(newRows: List<List<DynamicContentBlock.TableCell>>) {
        rows = newRows
        onUpdate(table.copy(rows = newRows))
    }

    val rowCount = rows.size
    val colCount = rows.maxOfOrNull { row -> row.sumOf { it.colSpan } } ?: 1

    val gridMap = remember(rows) {
        val map = mutableMapOf<CellCoord, Pair<Int, Int>>()
        val occupied = mutableSetOf<CellCoord>()

        for (r in rows.indices) {
            var c = 0
            for (colIdx in rows[r].indices) {
                while (occupied.contains(CellCoord(r, c))) c++
                val cell = rows[r][colIdx]
                for (dr in 0 until cell.rowSpan) {
                    for (dc in 0 until cell.colSpan) {
                        occupied.add(CellCoord(r + dr, c + dc))
                        map[CellCoord(r + dr, c + dc)] = Pair(r, colIdx)
                    }
                }
                c += cell.colSpan
            }
        }
        map
    }

    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    fun reqFor(r: Int, c: Int) = focusRequesters.getOrPut("$r,$c") { FocusRequester() }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier.wrapContentWidth()
            ) {
                for (r in 0 until rowCount) {
                    Row(modifier = Modifier.wrapContentWidth()) {
                        var c = 0
                        val rowCells = rows.getOrNull(r) ?: emptyList()
                        for (colIdx in rowCells.indices) {
                            val cell = rowCells[colIdx]
                            val coord = CellCoord(r, c)
                            val isSelected = selectedRange?.contains(r, c) == true
                            val isFocusedCell = activeCellCoord == coord

                            val cellBg = when {
                                isSelected -> colorScheme.primaryContainer.copy(alpha = 0.4f)
                                cell.isHeader -> colorScheme.surfaceVariant
                                else -> colorScheme.surface
                            }

                            val lineCount = 2 + cell.text.count { it == '\n' }

                            Box(
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 350.dp)
                                    .background(cellBg)
                                    .border(0.5.dp, colorScheme.outlineVariant)
                                    .padding(6.dp)
                                    .pointerInput(r, c) {
                                        detectTapGestures(
                                            onTap = {
                                                onActivate()
                                                onActiveCellChange(coord)
                                                onSelectedRangeChange(CellRange(r, c, r, c))
                                            }
                                        )
                                    }
                            ) {
                                var tfValue by remember(cell.text) {
                                    mutableStateOf(TextFieldValue(cell.text, TextRange(cell.text.length)))
                                }

                                BasicTextField(
                                    value = tfValue,
                                    onValueChange = { newVal ->
                                        tfValue = newVal
                                        val mutableRows = rows.map { it.toMutableList() }.toMutableList()
                                        mutableRows[r][colIdx] = cell.copy(text = newVal.text)
                                        emitUpdate(mutableRows)
                                    },
                                    enabled = canEdit,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurface,
                                        fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(colorScheme.primary),
                                    maxLines = lineCount,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(reqFor(r, c))
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                onActivate()
                                                onActiveCellChange(coord)
                                            }
                                        }
                                        .onPreviewKeyEvent { event ->
                                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                            when (event.key) {
                                                Key.Tab -> {
                                                    val nextCol = if (event.isShiftPressed) c - 1 else c + 1
                                                    val nextRow = if (nextCol < 0) r - 1 else if (nextCol >= colCount) r + 1 else r
                                                    val wrappedCol = if (nextCol < 0) colCount - 1 else if (nextCol >= colCount) 0 else nextCol
                                                    val wrappedRow = nextRow.coerceIn(0, rowCount - 1)

                                                    val targetActual = gridMap[CellCoord(wrappedRow, wrappedCol)]
                                                    if (targetActual != null) {
                                                        onActiveCellChange(CellCoord(wrappedRow, wrappedCol))
                                                        runCatching { reqFor(targetActual.first, targetActual.second).requestFocus() }
                                                    }
                                                    true
                                                }
                                                Key.Enter -> {
                                                    if (event.isShiftPressed) {
                                                        false
                                                    } else {
                                                        val nextRow = (r + 1).coerceAtMost(rowCount - 1)
                                                        val targetActual = gridMap[CellCoord(nextRow, c)]
                                                        if (targetActual != null) {
                                                            onActiveCellChange(CellCoord(nextRow, c))
                                                            runCatching { reqFor(targetActual.first, targetActual.second).requestFocus() }
                                                        }
                                                        true
                                                    }
                                                }
                                                else -> false
                                            }
                                        },
                                    decorationBox = { inner ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (tfValue.text.isEmpty()) {
                                                Text(
                                                    text = "",
                                                    style = TextStyle(fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }
                            c += cell.colSpan
                        }
                    }
                }
            }
        }
    }
}
