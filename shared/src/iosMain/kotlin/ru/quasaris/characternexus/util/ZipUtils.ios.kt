package ru.quasaris.characternexus.util

actual object ZipUtils {
    actual fun unzip(bytes: ByteArray): Map<String, ByteArray> = emptyMap()
    actual fun zip(files: Map<String, ByteArray>): ByteArray = byteArrayOf()
}
