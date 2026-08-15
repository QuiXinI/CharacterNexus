package ru.quasaris.characternexus.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object JsonConfig {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
    }
}
