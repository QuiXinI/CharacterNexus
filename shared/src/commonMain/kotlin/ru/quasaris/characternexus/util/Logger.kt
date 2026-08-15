package ru.quasaris.characternexus.util

expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
}

fun Throwable.log(tag: String = "Error", message: String = "An error occurred") {
    Logger.e(tag, message, this)
}
