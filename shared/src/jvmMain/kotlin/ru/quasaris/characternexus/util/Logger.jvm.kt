package ru.quasaris.characternexus.util

actual object Logger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("E/$tag: $message")
        throwable?.printStackTrace()
    }

    actual fun i(tag: String, message: String) {
        println("I/$tag: $message")
    }
}
