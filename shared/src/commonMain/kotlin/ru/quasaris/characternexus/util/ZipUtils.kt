package ru.quasaris.characternexus.util

expect object ZipUtils {
    fun unzip(bytes: ByteArray): Map<String, ByteArray>
    fun zip(files: Map<String, ByteArray>): ByteArray
}
