package ru.quasaris.characternexus.ui

object GlobalActionRegistry {
    var onToggleDrawer: (() -> Unit)? = null

    fun toggleDrawer() {
        onToggleDrawer?.invoke()
    }
}
