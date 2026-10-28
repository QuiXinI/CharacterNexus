package ru.quasaris.characternexus.util

actual object Logger {
    actual fun d(tag: String, message: String) {
        println("[$tag] $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR [$tag] $message: ${throwable?.message}")
    }

    actual fun i(tag: String, message: String) {
        println("INFO [$tag] $message")
    }
}
