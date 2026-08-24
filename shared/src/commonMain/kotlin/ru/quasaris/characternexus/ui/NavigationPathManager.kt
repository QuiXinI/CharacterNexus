package ru.quasaris.characternexus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class NavNode(
    val id: String,
    val label: String,
    val level: Int = 0,
    val onClick: (() -> Unit)? = null
)

object NavigationPathManager {
    private val _path = mutableStateListOf<NavNode>()
    val path: List<NavNode> = _path
    
    var currentSection by mutableStateOf<String?>(null)
        private set

    fun updatePath(section: String?, nodes: List<NavNode>) {
        currentSection = section
        _path.clear()
        _path.addAll(nodes)
    }

    fun clear() {
        currentSection = null
        _path.clear()
    }
}
