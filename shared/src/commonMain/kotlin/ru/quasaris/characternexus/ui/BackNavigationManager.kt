package ru.quasaris.characternexus.ui

/**
 * Manages a stack of back navigation callbacks.
 * This is used to coordinate between platform-specific back events (like Android Back or Desktop Esc)
 * and the application's internal state-based navigation.
 */
object BackNavigationManager {
    private val callbacks = mutableListOf<() -> Boolean>()

    /**
     * Registers a callback. If the callback returns true, it consumed the event.
     */
    fun register(callback: () -> Boolean) {
        callbacks.add(callback)
    }

    fun unregister(callback: () -> Boolean) {
        callbacks.remove(callback)
    }

    /**
     * Triggers the top-most callback. Returns true if the event was consumed.
     */
    fun pop(): Boolean {
        // Iterate backwards to trigger the most recently registered handler first
        for (i in callbacks.indices.reversed()) {
            if (callbacks[i]()) {
                return true
            }
        }
        return false
    }
}
