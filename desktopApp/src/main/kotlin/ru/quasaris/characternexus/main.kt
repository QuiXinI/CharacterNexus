package ru.quasaris.characternexus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Character Nexus",
    ) {
        App()
    }
}