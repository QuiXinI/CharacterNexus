package ru.quasaris.characters.master.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.CharacterTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterNavigationShell(
    modifier: Modifier = Modifier,
    content: @Composable (CharacterTab) -> Unit
) {
    val tabs = CharacterTab.entries
    // Infinite pager trick: use a large number of pages and start in the middle
    val totalPages = 10000 
    val initialPage = totalPages / 2 - (totalPages / 2 % tabs.size)
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }
    val scope = rememberCoroutineScope()
    
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val currentTab = tabs[pagerState.currentPage % tabs.size]
    
    Column(modifier = modifier.fillMaxSize()) {
        // Clickable Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBottomSheet = true }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentTab.title.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1
        ) { page ->
            val tab = tabs[page % tabs.size]
            content(tab)
        }
    }
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Перейти к вкладке",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                tabs.forEachIndexed { index, tab ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                tab.title,
                                fontWeight = if (tab == currentTab) FontWeight.Bold else FontWeight.Normal,
                                color = if (tab == currentTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val currentP = pagerState.currentPage
                                val currentIdx = currentP % tabs.size
                                val diff = index - currentIdx
                                pagerState.animateScrollToPage(currentP + diff)
                                sheetState.hide()
                            }.invokeOnCompletion {
                                showBottomSheet = false
                            }
                        }
                    )
                }
            }
        }
    }
}
