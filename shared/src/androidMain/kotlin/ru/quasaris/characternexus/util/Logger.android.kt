package ru.quasaris.characternexus.util

import android.util.Log

actual object Logger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
        logToFlow("D", tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        logToFlow("E", tag, message, throwable)
    }

    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
        logToFlow("I", tag, message)
    }
}
